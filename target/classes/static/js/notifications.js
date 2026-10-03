(function () {
  const ui = window.SmartRoute;
  const categories = ["BOOKING_UPDATES", "PAYMENT_UPDATES", "SCHEDULE_CHANGES", "TRIP_REMINDERS", "WALLET_UPDATES", "SUPPORT_MESSAGES", "SECURITY_ALERTS", "PROMOTIONAL", "ADMIN"];
  let currentPage = 0;

  function actionUrl(n) {
    if (n.actionUrl) return n.actionUrl;
    if (String(n.category).includes("PAYMENT") || String(n.category).includes("BOOKING") || String(n.category).includes("SCHEDULE")) return "/my-bookings.html";
    if (String(n.category).includes("WALLET")) return "/wallet.html";
    if (String(n.category).includes("SUPPORT")) return "/support.html";
    return null;
  }

  function render(data) {
    const node = ui.$("#notificationsList");
    if (!data.content || !data.content.length) { node.innerHTML = '<div class="empty-state">No notifications yet.</div>'; return; }
    node.innerHTML = `<div class="notification-list">${data.content.map((n) => {
      const url = actionUrl(n);
      return `<article class="notification-item ${n.status === "SENT" ? "unread" : ""}">
        <div class="notification-icon" aria-hidden="true">${ui.escapeHtml((n.category || "ADMIN").slice(0, 1))}</div>
        <div class="notification-copy">
          <div class="notification-head"><strong>${ui.escapeHtml(n.title || "Notification")}</strong>${ui.badge(n.status)}</div>
          <p>${ui.escapeHtml(n.message)}</p>
          <small>${ui.escapeHtml(ui.formatStatus(n.category || ""))} · ${ui.escapeHtml(n.deliveryChannels || "IN_APP")} · ${ui.datetime(n.sentDateTime)}</small>
        </div>
        <div class="actions">
          ${url ? `<a class="btn btn-small btn-ghost" href="${ui.escapeHtml(url)}" data-open="${n.notificationId}">Open</a>` : ""}
          ${n.status !== "READ" ? `<button class="btn btn-small btn-ghost" data-read="${n.notificationId}">Mark read</button>` : ""}
          <button class="btn btn-small btn-danger" data-archive="${n.notificationId}">Archive</button>
        </div>
      </article>`;
    }).join("")}</div>`;
    ui.$all("[data-open]").forEach((link) => link.addEventListener("click", async () => {
      try { await window.Api.patch(`/notifications/${link.dataset.open}/read`, null); } catch (_) { /* Navigation still works. */ }
    }));
    ui.$all("[data-read]").forEach((button) => button.addEventListener("click", async () => {
      try { await window.Api.patch(`/notifications/${button.dataset.read}/read`, null); await load(data.page); await window.SmartRoute.refreshNotificationBadge(); }
      catch (error) { ui.showAlert("#pageAlert", "error", ui.errorMessage(error)); }
    }));
    ui.$all("[data-archive]").forEach((button) => button.addEventListener("click", async () => {
      try { await window.Api.delete(`/notifications/${button.dataset.archive}`); await load(data.page); await window.SmartRoute.refreshNotificationBadge(); }
      catch (error) { ui.showAlert("#pageAlert", "error", ui.errorMessage(error)); }
    }));
  }
  async function load(page) {
    currentPage = page || 0;
    const data = await window.Api.get("/notifications/my", {
      page: currentPage,
      size: 10,
      status: ui.$("#readFilter").value,
      category: ui.$("#categoryFilter").value
    });
    render(data); ui.pagination("#notificationsPagination", data, load);
  }

  async function loadPreferences() {
    const p = await window.Api.get("/notification-preferences");
    const form = ui.$("#preferencesForm");
    Object.keys(p).forEach((key) => {
      const field = form.elements[key];
      if (field && field.type === "checkbox") field.checked = Boolean(p[key]);
    });
    form.elements.securityAlertsEnabled.checked = true;
  }

  function startStream() {
    if (!window.EventSource) return;
    try {
      const source = new EventSource("/api/notifications/stream");
      source.addEventListener("notification", async () => {
        await window.SmartRoute.refreshNotificationBadge();
        if (document.visibilityState === "visible") load(currentPage).catch(() => {});
      });
      source.onerror = () => source.close();
    } catch (_) {
      /* REST polling remains available. */
    }
  }

  document.addEventListener("DOMContentLoaded", () => {
    if (!window.Auth.requireAuth()) return;
    ui.$("#categoryFilter").innerHTML = `<option value="">All categories</option>` + categories.map((c) => `<option value="${c}">${ui.escapeHtml(ui.formatStatus(c))}</option>`).join("");
    ui.$("#readFilter").addEventListener("change", () => load(0));
    ui.$("#categoryFilter").addEventListener("change", () => load(0));
    ui.$("#refreshNotificationsBtn").addEventListener("click", () => load(currentPage));
    ui.$("#markAllReadBtn").addEventListener("click", async () => {
      try { await window.Api.patch("/notifications/my/read-all", null); await load(currentPage); await window.SmartRoute.refreshNotificationBadge(); }
      catch (error) { ui.showAlert("#pageAlert", "error", ui.errorMessage(error)); }
    });
    ui.$("#preferencesForm").addEventListener("submit", async (event) => {
      event.preventDefault();
      const form = event.currentTarget;
      const payload = Object.fromEntries(Array.from(form.elements).filter((el) => el.name).map((el) => [el.name, el.checked]));
      payload.securityAlertsEnabled = true;
      try { await window.Api.put("/notification-preferences", payload); ui.showAlert("#pageAlert", "success", "Preferences saved."); }
      catch (error) { ui.showAlert("#pageAlert", "error", ui.errorMessage(error)); }
    });
    Promise.all([load(0), loadPreferences()]).catch((e) => ui.showAlert("#pageAlert", "error", ui.errorMessage(e)));
    startStream();
  });
})();
