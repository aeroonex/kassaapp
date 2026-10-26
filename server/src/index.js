require("dotenv").config();
const path = require("path");
const express = require("express");
const session = require("express-session");

const { startCleanupSchedule } = require("./cleanup");
const authRoutes = require("./routes/auth");
const deviceRoutes = require("./routes/devices");
const screenshotRoutes = require("./routes/screenshots");

const app = express();
app.use(express.json());
app.use(
  session({
    secret: process.env.SESSION_SECRET || "dev-secret-change-me",
    resave: false,
    saveUninitialized: false,
    cookie: { maxAge: 12 * 60 * 60 * 1000 },
  })
);

app.use(authRoutes);
app.use(deviceRoutes);
app.use(screenshotRoutes);

// Static assets (HTML/CSS/JS) are public; every data-bearing call underneath
// (/api/devices, /api/devices/:id/screenshots, ...) is session-protected by
// requireLogin in its own route, and the panel's own JS bounces an
// unauthenticated viewer to /login.html via a failed /api/session check.
app.use("/", express.static(path.join(__dirname, "..", "public")));

const port = process.env.PORT || 3000;
app.listen(port, () => {
  console.log(`kassaapp monitoring server listening on :${port}`);
  startCleanupSchedule();
});
