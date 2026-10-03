(function () {
  const cfg = window.SmartRouteConfig;

  // Presentation only: bounded loader lifetime, safe on cached/back navigation.
  function initPageLoader() {
    const loader = document.getElementById("srPageLoader");
    if (!loader) return;
    const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    const startedAt = performance.now();
    let returning = false;
    try { returning = sessionStorage.getItem("srl.ui.visited") === "1"; sessionStorage.setItem("srl.ui.visited", "1"); } catch (_) { /* Storage may be disabled. */ }
    const minimumVisibleMs = reduceMotion ? 0 : returning ? 120 : 520;
    let dismissed = false;
    const dismiss = () => {
      if (dismissed) return;
      dismissed = true;
      window.setTimeout(() => {
        loader.classList.add("is-leaving");
        loader.setAttribute("aria-hidden", "true");
        window.setTimeout(() => loader.remove(), reduceMotion ? 0 : 320);
      }, Math.max(0, minimumVisibleMs - (performance.now() - startedAt)));
    };
    if (document.readyState === "complete") dismiss();
    else window.addEventListener("load", dismiss, { once: true });
    window.setTimeout(dismiss, 2500);
    window.addEventListener("pageshow", (event) => { if (event.persisted) loader.remove(); });
  }
  initPageLoader();

  function $(selector, root) {
    return (root || document).querySelector(selector);
  }

  function $all(selector, root) {
    return Array.from((root || document).querySelectorAll(selector));
  }

  function escapeHtml(value) {
    return String(value ?? "")
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#039;");
  }

  function formatStatus(value) {
    return String(value || "")
      .toLowerCase()
      .split("_")
      .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
      .join(" ");
  }

  function statusClass(value) {
    const normalized = String(value || "").toLowerCase();
    if (["success", "confirmed", "scheduled", "available", "active", "resolved", "completed"].some((word) => normalized.includes(word))) return "status-success";
    if (["pending", "delayed", "progress", "assigned"].some((word) => normalized.includes(word))) return "status-warning";
    if (["failed", "cancelled", "inactive", "unavailable", "maintenance", "closed", "refunded"].some((word) => normalized.includes(word))) return "status-danger";
    return "status-muted";
  }

  function badge(value) {
    return `<span class="status-pill ${statusClass(value)}">${escapeHtml(formatStatus(value))}</span>`;
  }

  function money(value) {
    const n = Number(value || 0);
    return "LKR " + n.toLocaleString("en-LK", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  function date(value) {
    if (!value) return "-";
    return new Date(value + (String(value).includes("T") ? "" : "T00:00:00")).toLocaleDateString("en-LK", {
      year: "numeric",
      month: "short",
      day: "2-digit"
    });
  }

  function time(value) {
    if (!value) return "-";
    const [hh, mm] = String(value).split(":");
    const d = new Date();
    d.setHours(Number(hh || 0), Number(mm || 0), 0, 0);
    return d.toLocaleTimeString("en-LK", { hour: "2-digit", minute: "2-digit" });
  }

  function datetime(value) {
    if (!value) return "-";
    return new Date(value).toLocaleString("en-LK", {
      year: "numeric",
      month: "short",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit"
    });
  }

  function fullName(user) {
    if (!user) return "-";
    return [user.firstName, user.lastName].filter(Boolean).join(" ") || user.email || "-";
  }

  function routeLabel(schedule) {
    if (!schedule || !schedule.route) return "-";
    return `${schedule.route.origin} to ${schedule.route.destination}`;
  }

  function seatsLabel(seats) {
    return (seats || []).map((seat) => seat.seatNumber || seat).sort((a, b) => Number(a) - Number(b)).join(", ");
  }

  function setText(selector, text, root) {
    const node = $(selector, root);
    if (node) node.textContent = text;
  }

  function showAlert(target, type, message) {
    const node = typeof target === "string" ? $(target) : target;
    if (!node) return;
    node.innerHTML = message
      ? `<div class="alert alert-${type}" role="alert">${escapeHtml(message)}</div>`
      : "";
  }

  function errorMessage(error) {
    return error && error.message ? error.message : "Something went wrong. Please try again.";
  }

  function setLoading(button, loading, text) {
    if (!button) return;
    if (loading) {
      button.dataset.originalText = button.textContent;
      button.disabled = true;
      button.innerHTML = `<span class="spinner" aria-hidden="true"></span>${escapeHtml(text || "Loading")}`;
    } else {
      button.disabled = false;
      button.textContent = button.dataset.originalText || text || "Submit";
    }
  }

  function required(value) {
    return value !== undefined && value !== null && String(value).trim() !== "";
  }

  function validateEmail(value) {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(String(value || "").trim());
  }

  function validatePhone(value) {
    return !value || /^[0-9+()\-\s]{7,30}$/.test(String(value).trim());
  }

  function validatePassword(value) {
    return /^(?=.*[A-Za-z])(?=.*\d).{8,100}$/.test(String(value || ""));
  }

  function validateStrongPassword(value) {
    return /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,100}$/.test(String(value || ""));
  }

  function renderFieldErrors(errors) {
    $all(".field-error").forEach((node) => (node.textContent = ""));
    Object.entries(errors || {}).forEach(([field, message]) => {
      const target = document.querySelector(`[data-error-for="${field}"]`);
      if (target) target.textContent = message;
    });
  }

  function formData(form) {
    return Object.fromEntries(new FormData(form).entries());
  }

  function selectOptions(values, selected, includeBlank) {
    const blank = includeBlank ? `<option value="">${typeof includeBlank === "string" ? includeBlank : "Select"}</option>` : "";
    return blank + values.map((value) => `<option value="${escapeHtml(value)}"${value === selected ? " selected" : ""}>${escapeHtml(formatStatus(value))}</option>`).join("");
  }

  function pagination(container, page, onChange) {
    const node = typeof container === "string" ? $(container) : container;
    if (!node || !page || page.totalPages <= 1) {
      if (node) node.innerHTML = "";
      return;
    }
    const current = Number(page.page || 0);
    const last = Number(page.totalPages || 1) - 1;
    const pages = [];
    const start = Math.max(0, current - 2);
    const end = Math.min(last, current + 2);
    pages.push(`<button class="page-btn" data-page="${current - 1}" ${current === 0 ? "disabled" : ""}>Previous</button>`);
    for (let i = start; i <= end; i++) {
      pages.push(`<button class="page-btn ${i === current ? "active" : ""}" data-page="${i}">${i + 1}</button>`);
    }
    pages.push(`<button class="page-btn" data-page="${current + 1}" ${current >= last ? "disabled" : ""}>Next</button>`);
    node.innerHTML = pages.join("");
    $all("button[data-page]", node).forEach((button) => {
      button.addEventListener("click", () => {
        const next = Number(button.dataset.page);
        if (!Number.isNaN(next) && next >= 0 && next <= last) onChange(next);
      });
    });
  }

  function navLink(path, label, active) {
    return `<a class="nav-link ${active === path ? "active" : ""}" href="${path}"${active === path ? ' aria-current="page"' : ''}>${label}</a>`;
  }

  async function refreshNotificationBadge() {
    const badge = $("#notificationBadge");
    if (!badge || !window.Auth || !window.Auth.isAuthenticated()) return;
    try {
      const count = await window.Api.get("/notifications/unread-count");
      badge.textContent = count > 99 ? "99+" : String(count);
      badge.hidden = !count;
    } catch (_) {
      badge.hidden = true;
    }
  }

  function initShell() {
    const page = document.body.dataset.page || "";
    const nav = $("#siteNav");
    if (nav) {
      const user = window.Auth.getUser();
      const authenticated = window.Auth.isAuthenticated();
      const staff = window.Auth.isStaff();
      nav.innerHTML = `
        <nav class="navbar" aria-label="Main navigation">
          <a class="brand" href="/index.html"><span class="brand-mark"><img src="/assets/smartroute-mark.svg" alt="" width="42" height="42"></span><span class="brand-name">SmartRoute<small>LANKA</small></span></a>
          <button class="nav-toggle" type="button" aria-label="Toggle navigation" aria-expanded="false" aria-controls="mainNavigation"><svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" aria-hidden="true"><path d="M4 6h16M4 12h16M4 18h16"/></svg>Menu</button>
          <div class="nav-menu" id="mainNavigation">
            ${navLink("/index.html", "Home", page === "home" ? "/index.html" : "")}
            ${navLink("/search-results.html", "Schedules", page === "search" ? "/search-results.html" : "")}
            ${authenticated && !staff ? navLink("/my-bookings.html", "My bookings", page === "my-bookings" ? "/my-bookings.html" : "") : ""}
            ${authenticated && !staff ? navLink("/wallet.html", "Wallet", page === "wallet" ? "/wallet.html" : "") : ""}
            ${authenticated ? navLink("/support.html", "Support", page === "support" ? "/support.html" : "") : ""}
            ${authenticated ? `<a class="nav-link notification-nav-link ${page === "notifications" ? "active" : ""}" href="/notifications.html"${page === "notifications" ? ' aria-current="page"' : ''}>Notifications<span id="notificationBadge" class="notification-badge" hidden>0</span></a>` : ""}
            ${staff ? navLink("/admin/dashboard.html", "Admin", "/admin/" + page + ".html") : ""}
          </div>
          <div class="nav-actions">
            ${authenticated ? `<a class="btn btn-ghost" href="/profile.html">${escapeHtml(fullName(user))}</a><button class="btn btn-primary" id="logoutBtn" type="button">Logout</button>` : `<a class="btn btn-ghost" href="/login.html">Login</a><a class="btn btn-primary" href="/register.html">Register</a>`}
          </div>
        </nav>`;
      const toggle = $(".nav-toggle", nav);
      const menu = $(".nav-menu", nav);
      toggle.addEventListener("click", () => {
        const open = menu.classList.toggle("open");
        toggle.setAttribute("aria-expanded", String(open));
      });
      nav.addEventListener("keydown", (event) => {
        if (event.key === "Escape" && menu.classList.contains("open")) {
          menu.classList.remove("open");
          toggle.setAttribute("aria-expanded", "false");
          toggle.focus();
        }
      });
      const logout = $("#logoutBtn", nav);
      if (logout) logout.addEventListener("click", () => window.Auth.logout("/index.html"));
      if (authenticated) {
        refreshNotificationBadge();
        window.setInterval(refreshNotificationBadge, 60000);
      }
    }

    const adminNav = $("#adminNav");
    if (adminNav) {
      const role = window.Auth.currentRole();
      const links = [
        { href: "/admin/dashboard.html", label: "Dashboard", roles: cfg.staffRoles },
        { href: "/admin/users.html", label: "Users", roles: cfg.userAdminRoles },
        { href: "/admin/routes.html", label: "Routes", roles: cfg.transportRoles },
        { href: "/admin/buses.html", label: "Buses", roles: cfg.transportRoles },
        { href: "/admin/drivers.html", label: "Drivers", roles: cfg.transportRoles },
        { href: "/admin/schedules.html", label: "Schedules", roles: cfg.transportRoles },
        { href: "/admin/bookings.html", label: "Bookings", roles: cfg.supportRoles },
        { href: "/admin/payments.html", label: "Payments", roles: cfg.financeRoles },
        { href: "/admin/support-tickets.html", label: "Support tickets", roles: cfg.supportRoles },
        { href: "/admin/notifications.html", label: "Notifications", roles: cfg.notificationRoles },
        { href: "/admin/reviews.html", label: "Reviews & feedback", roles: cfg.supportRoles },
        { href: "/admin/reports.html", label: "Reports", roles: cfg.reportRoles }
      ].filter((link) => link.roles.includes(role));
      adminNav.innerHTML = `
        <aside class="admin-sidebar">
          <a class="brand compact" href="/admin/dashboard.html"><span class="brand-mark"><img src="/assets/smartroute-mark.svg" alt="" width="36" height="36"></span><span class="brand-name">SmartRoute<small>ADMIN WORKSPACE</small></span></a>
          ${links.map((link) => `<a href="${link.href}">${link.label}</a>`).join("")}
          <a href="/index.html">Public site</a>
        </aside>`;
      const iconPaths = {
        dashboard: '<rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/>',
        users: '<circle cx="9" cy="8" r="3"/><path d="M3 21v-3a6 6 0 0 1 12 0v3m0-16a3 3 0 0 1 0 6m3 4a5 5 0 0 1 3 5"/>',
        routes: '<circle cx="6" cy="5" r="2"/><circle cx="18" cy="19" r="2"/><path d="M8 5h8a4 4 0 0 1 0 8H8a3 3 0 0 0 0 6h8"/>',
        buses: '<rect x="4" y="3" width="16" height="17" rx="3"/><path d="M4 11h16M7 15h2m6 0h2M7 20v2m10-2v2"/>',
        schedules: '<rect x="3" y="5" width="18" height="16" rx="2"/><path d="M7 3v4m10-4v4M3 11h18m-12 4h5"/>',
        bookings: '<rect x="4" y="3" width="16" height="18" rx="2"/><path d="M8 8h8m-8 4h8m-8 4h5"/>',
        payments: '<rect x="2" y="5" width="20" height="14" rx="2"/><path d="M2 10h20M6 15h4"/>',
        reports: '<path d="M4 3v18h17M8 17v-5m5 5V7m5 10V4"/>',
        reviews: '<path d="m12 3 3 6 6 1-4.5 4.5L18 21l-6-3-6 3 1.5-6.5L3 10l6-1 3-6Z"/>',
        support: '<path d="M4 13V11a8 8 0 0 1 16 0v2M4 12H2v8h5v-8H4m16 0h2v8h-5v-8h3"/>',
        notifications: '<path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4"/>',
        home: '<path d="m3 10 9-7 9 7v11h-7v-7h-4v7H3V10Z"/>'
      };
      $all("a:not(.brand)", adminNav).forEach((link) => {
        const file = link.getAttribute("href").split("/").pop().replace(".html", "");
        const key = file === "drivers" ? "users" : file === "support-tickets" ? "support" : file === "index" ? "home" : file;
        link.insertAdjacentHTML("afterbegin", `<svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${iconPaths[key] || iconPaths.bookings}</svg>`);
      });
      const current = window.location.pathname;
      $all("a:not(.brand)", adminNav).forEach((link) => {
        if (link.getAttribute("href") === current) { link.classList.add("active"); link.setAttribute("aria-current", "page"); }
      });
    }

    const footer = $("#siteFooter");
    if (footer) {
      footer.innerHTML = `<footer class="site-footer">
        <div class="footer-col">
          <div class="brand compact"><span class="brand-mark"><img src="/assets/smartroute-mark.svg" alt="" width="42" height="42"></span><span class="brand-name">SmartRoute<small>LANKA</small></span></div>
          <p>From the everyday commute to your next adventure. Your next stop, made simple.</p>
        </div>
        <div class="footer-col">
          <strong>Your journey</strong>
          <a href="/search-results.html">Search schedules</a>
          <a href="/my-bookings.html">My bookings</a>
          <a href="/support.html">Support</a>
        </div>
        <div class="footer-col">
          <strong>Account</strong>
          <a href="/login.html">Login</a>
          <a href="/register.html">Register</a>
          <a href="/profile.html">Profile</a>
        </div>
      </footer><div class="footer-bottom"><span>&copy; ${new Date().getFullYear()} SmartRoute Lanka</span><span>A little less planning. A little more journey.</span></div>`;
    }

    if (new URLSearchParams(window.location.search).get("forbidden")) {
      showAlert("#pageAlert", "warning", "You do not have permission to open that page.");
    }
  }

  function initProfessionalUi() {
    const reduceMotion = window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches;

    // Reveal important sections only when they enter the viewport. API-rendered
    // cards are picked up by the MutationObserver below as well.
    let revealObserver = null;
    if (!reduceMotion && "IntersectionObserver" in window) {
      revealObserver = new IntersectionObserver((entries) => {
        entries.forEach((entry) => {
          if (!entry.isIntersecting) return;
          entry.target.classList.add("is-visible");
          revealObserver.unobserve(entry.target);
        });
      }, { threshold: 0.08, rootMargin: "0px 0px -36px 0px" });
    }

    const enhance = (root) => {
      const scope = root && root.querySelectorAll ? root : document;
      const targets = [];
      if (root && root.nodeType === 1 && root.matches(".card, .page-title, .empty-state, .ticket, .seat-map, .table-wrap, .destination-card, .how-step, .promo-banner")) {
        targets.push(root);
      }
      targets.push(...Array.from(scope.querySelectorAll(".card, .page-title, .empty-state, .ticket, .seat-map, .table-wrap, .destination-card, .how-step, .promo-banner")));

      targets.forEach((node, index) => {
        if (node.dataset.uiEnhanced === "true") return;
        node.dataset.uiEnhanced = "true";
        node.classList.add("ui-reveal");
        node.style.setProperty("--reveal-delay", `${Math.min(index % 4, 3) * 55}ms`);
        if (reduceMotion || !revealObserver) {
          node.classList.add("is-visible");
        } else {
          revealObserver.observe(node);
        }
      });
    };

    enhance(document);
    const updateScroll = () => document.body.classList.toggle("is-scrolled", window.scrollY > 8);
    updateScroll();
    window.addEventListener("scroll", updateScroll, { passive: true });

    const mutationObserver = new MutationObserver((mutations) => {
      mutations.forEach((mutation) => {
        mutation.addedNodes.forEach((node) => {
          if (node.nodeType === 1) enhance(node);
        });
      });
    });
    mutationObserver.observe(document.body, { childList: true, subtree: true });

    // Lightweight click ripple for buttons without changing any existing handlers.
    document.addEventListener("pointerdown", (event) => {
      if (reduceMotion) return;
      const button = event.target.closest && event.target.closest(".btn, .page-btn");
      if (!button || button.disabled) return;
      const rect = button.getBoundingClientRect();
      const ripple = document.createElement("span");
      ripple.className = "ui-ripple";
      ripple.style.left = `${event.clientX - rect.left}px`;
      ripple.style.top = `${event.clientY - rect.top}px`;
      button.appendChild(ripple);
      window.setTimeout(() => ripple.remove(), 650);
    });
  }

  document.addEventListener("DOMContentLoaded", () => {
    initShell();
    initProfessionalUi();
  });

  window.SmartRoute = {
    $,
    $all,
    escapeHtml,
    formatStatus,
    statusClass,
    badge,
    money,
    date,
    time,
    datetime,
    fullName,
    routeLabel,
    seatsLabel,
    setText,
    showAlert,
    errorMessage,
    setLoading,
    required,
    validateEmail,
    validatePhone,
    validatePassword,
    validateStrongPassword,
    renderFieldErrors,
    formData,
    selectOptions,
    pagination,
    refreshNotificationBadge
  };
})();
