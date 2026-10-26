async function requireSession() {
  const res = await fetch("/api/session");
  const data = await res.json();
  if (!data.loggedIn) {
    window.location.href = "/login.html";
    throw new Error("not logged in");
  }
}

function formatTime(ms) {
  return new Date(ms).toLocaleString("uz-UZ");
}

const deviceId = new URLSearchParams(window.location.search).get("id");

async function loadHistory() {
  document.getElementById("deviceId").textContent = deviceId;
  const res = await fetch(`/api/devices/${encodeURIComponent(deviceId)}/screenshots?limit=60`);
  const shots = await res.json();

  if (shots.length > 0) {
    const latest = shots[0];
    document.getElementById("liveImage").src =
      `/api/devices/${encodeURIComponent(deviceId)}/screenshots/${latest.filename}`;
    document.getElementById("liveCaption").textContent = formatTime(latest.capturedAt);
  }

  document.getElementById("history").innerHTML = shots.map((s) => `
    <a class="card" href="/api/devices/${encodeURIComponent(deviceId)}/screenshots/${s.filename}" target="_blank">
      <img src="/api/devices/${encodeURIComponent(deviceId)}/screenshots/${s.filename}" loading="lazy" />
      <div class="meta">${formatTime(s.capturedAt)}</div>
    </a>
  `).join("");
}

(async () => {
  await requireSession();
  await loadHistory();
  setInterval(loadHistory, 5000);

  document.getElementById("logoutBtn").addEventListener("click", async () => {
    await fetch("/api/logout", { method: "POST" });
    window.location.href = "/login.html";
  });
})();
