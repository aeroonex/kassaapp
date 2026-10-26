const express = require("express");
const router = express.Router();

router.post("/api/login", (req, res) => {
  const { username, password } = req.body || {};
  if (username === process.env.ADMIN_USER && password === process.env.ADMIN_PASSWORD) {
    req.session.loggedIn = true;
    return res.json({ ok: true });
  }
  res.status(401).json({ error: "login yoki parol xato" });
});

router.post("/api/logout", (req, res) => {
  req.session.destroy(() => res.json({ ok: true }));
});

router.get("/api/session", (req, res) => {
  res.json({ loggedIn: !!(req.session && req.session.loggedIn) });
});

module.exports = router;
