const express = require("express");
const { db } = require("../db");
const { requireLogin } = require("../auth");

const router = express.Router();

// List all known devices with their last-seen time and latest screenshot.
router.get("/api/devices", requireLogin, (req, res) => {
  const devices = db
    .prepare("SELECT id, label, last_seen AS lastSeen FROM devices ORDER BY last_seen DESC")
    .all();

  const latestStmt = db.prepare(
    "SELECT filename, captured_at AS capturedAt FROM screenshots WHERE device_id = ? ORDER BY captured_at DESC LIMIT 1"
  );

  const withLatest = devices.map((d) => {
    const latest = latestStmt.get(d.id);
    const onlineThresholdMs = 30 * 1000; // ~6 missed 5s captures
    return {
      ...d,
      online: Date.now() - d.lastSeen < onlineThresholdMs,
      latestScreenshot: latest ? latest.filename : null,
      latestCapturedAt: latest ? latest.capturedAt : null,
    };
  });

  res.json(withLatest);
});

module.exports = router;
