const path = require("path");
const fs = require("fs");
const Database = require("better-sqlite3");

const dataDir = path.join(__dirname, "..", "data");
fs.mkdirSync(dataDir, { recursive: true });
fs.mkdirSync(path.join(dataDir, "screenshots"), { recursive: true });

const db = new Database(path.join(dataDir, "kassaapp.db"));
db.pragma("journal_mode = WAL");

db.exec(`
  CREATE TABLE IF NOT EXISTS devices (
    id TEXT PRIMARY KEY,
    label TEXT,
    first_seen INTEGER NOT NULL,
    last_seen INTEGER NOT NULL
  );

  CREATE TABLE IF NOT EXISTS screenshots (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    device_id TEXT NOT NULL,
    filename TEXT NOT NULL,
    captured_at INTEGER NOT NULL,
    received_at INTEGER NOT NULL
  );

  CREATE INDEX IF NOT EXISTS idx_screenshots_device_time
    ON screenshots (device_id, captured_at DESC);
`);

module.exports = { db, dataDir };
