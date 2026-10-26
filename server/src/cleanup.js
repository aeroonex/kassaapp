const fs = require("fs");
const path = require("path");
const { db, dataDir } = require("./db");

const RETENTION_DAYS = Number(process.env.RETENTION_DAYS || 30);

/** Deletes screenshots (file + row) older than RETENTION_DAYS. */
function cleanupOldScreenshots() {
  const cutoff = Date.now() - RETENTION_DAYS * 24 * 60 * 60 * 1000;
  const old = db
    .prepare("SELECT id, device_id, filename FROM screenshots WHERE captured_at < ?")
    .all(cutoff);

  for (const row of old) {
    const filePath = path.join(dataDir, "screenshots", row.device_id, row.filename);
    fs.rm(filePath, { force: true }, () => {});
  }

  if (old.length > 0) {
    db.prepare("DELETE FROM screenshots WHERE captured_at < ?").run(cutoff);
    console.log(`[cleanup] removed ${old.length} screenshot(s) older than ${RETENTION_DAYS} days`);
  }
}

function startCleanupSchedule() {
  cleanupOldScreenshots();
  // Once an hour is plenty for a daily retention window.
  setInterval(cleanupOldScreenshots, 60 * 60 * 1000);
}

module.exports = { startCleanupSchedule, cleanupOldScreenshots, RETENTION_DAYS };
