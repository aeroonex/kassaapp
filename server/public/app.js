async function requireSession() {
  const res = await fetch("/api/session");
  const data = await res.json();
  if (!data.loggedIn) {
    window.location.href = "/login.html";
    throw new Error("not logged in");
  }
}

function escapeHtml(s) {
  return String(s).replace(/[&<>"']/g, (c) => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;",
  }[c]));
}

function timeAgo(ms) {
  if (!ms) return "hech qachon";
  const diff = Date.now() - ms;
  const mins = Math.floor(diff / 60000);
  if (mins < 1) return "hozir";
  if (mins < 60) return `${mins} daqiqa oldin`;
  const hours = Math.floor(mins / 60);
  if (hours < 24) return `${hours} soat oldin`;
  return `${Math.floor(hours / 24)} kun oldin`;
}

async function loadDevices() {
  const res = await fetch("/api/devices");
  const devices = await res.json();
  const grid = document.getElementById("grid");
  const online = devices.filter((d) => d.online).length;
  document.getElementById("summary").textContent =
    `${devices.length} qurilma, ${online} online`;

  grid.innerHTML = devices.map((d) => `
    <a class="card" href="/device.html?id=${encodeURIComponent(d.id)}">
      ${d.latestScreenshot
        ? `<img src="/api/devices/${encodeURIComponent(d.id)}/screenshots/${d.latestScreenshot}" loading="lazy" />`
        : `<img alt="rasm yo'q" />`}
      <div class="meta">
        <div class="id"><span class="status-dot ${d.online ? "online" : "offline"}"></span>${escapeHtml(d.label || d.id)}</div>
        <div>${timeAgo(d.lastSeen)}</div>
      </div>
    </a>
  `).join("");
}

(async () => {
  await requireSession();
  await loadDevices();
  setInterval(loadDevices, 5000);

  document.getElementById("logoutBtn").addEventListener("click", async () => {
    await fetch("/api/logout", { method: "POST" });
    window.location.href = "/login.html";
  });
})();
