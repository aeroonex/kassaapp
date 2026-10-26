const path = require("path");
const fs = require("fs");
const express = require("express");
const multer = require("multer");
const { db, dataDir } = require("../db");
const { requireDeviceToken, requireLogin } = require("../auth");

const router = express.Router();

const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 5 * 1024 * 1024 },
});

// Called by the Android app (ScreenshotUploadService). Authenticated with a
// shared device token, not a login session — 300 phones, one secret.
router.post(
  "/api/devices/:deviceId/screenshots",
  requireDeviceToken,
  upload.single("screenshot"),
  (req, res) => {
    const { deviceId } = req.params;
    if (!req.file) {
      return res.status(400).json({ error: "screenshot file missing" });
    }

    const capturedAt = Number(req.body.timestamp) || Date.now();
    const receivedAt = Date.now();
    const filename = `shot_${capturedAt}.jpg`;

    const deviceDir = path.join(dataDir, "screenshots", deviceId);
    fs.mkdirSync(deviceDir, { recursive: true });
    fs.writeFileSync(path.join(deviceDir, filename), req.file.buffer);

    const now = Date.now();
    db.prepare(
      `INSERT INTO devices (id, label, first_seen, last_seen) VALUES (?, ?, ?, ?)
       ON CONFLICT(id) DO UPDATE SET last_seen = excluded.last_seen`
    ).run(deviceId, deviceId, now, now);

    db.prepare(
      "INSERT INTO screenshots (device_id, filename, captured_at, received_at) VALUES (?, ?, ?, ?)"
    ).run(deviceId, filename, capturedAt, receivedAt);

    res.json({ ok: true });
  }
);

// Panel-facing: list recent screenshots for a device.
router.get("/api/devices/:deviceId/screenshots", requireLogin, (req, res) => {
  const { deviceId } = req.params;
  const limit = Math.min(Number(req.query.limit) || 50, 500);
  const rows = db
    .prepare(
      "SELECT filename, captured_at AS capturedAt FROM screenshots WHERE device_id = ? ORDER BY captured_at DESC LIMIT ?"
    )
    .all(deviceId, limit);
  res.json(rows);
});

// Serves one screenshot image file. Session-authenticated like the rest of the panel.
router.get("/api/devices/:deviceId/screenshots/:filename", requireLogin, (req, res) => {
  const { deviceId, filename } = req.params;
  if (!/^shot_\d+\.jpg$/.test(filename)) {
    return res.status(400).end();
  }
  const filePath = path.join(dataDir, "screenshots", deviceId, filename);
  if (!fs.existsSync(filePath)) {
    return res.status(404).end();
  }
  res.sendFile(filePath);
});

module.exports = router;
