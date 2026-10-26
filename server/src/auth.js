function requireLogin(req, res, next) {
  if (req.session && req.session.loggedIn) {
    return next();
  }
  if (req.path.startsWith("/api/")) {
    return res.status(401).json({ error: "login required" });
  }
  return res.redirect("/login.html");
}

function requireDeviceToken(req, res, next) {
  const expected = process.env.UPLOAD_TOKEN;
  if (!expected) {
    // No token configured: allow, but this should only happen in local dev.
    return next();
  }
  const got = req.header("X-Upload-Token");
  if (got !== expected) {
    return res.status(401).json({ error: "invalid upload token" });
  }
  next();
}

module.exports = { requireLogin, requireDeviceToken };
