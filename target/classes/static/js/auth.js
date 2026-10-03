(function () {
  const cfg = window.SmartRouteConfig;

  function decodeJwt(token) {
    if (!token || !token.includes(".")) return null;
    try {
      const payload = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
      const padded = payload.padEnd(payload.length + ((4 - payload.length % 4) % 4), "=");
      return JSON.parse(atob(padded));
    } catch (error) {
      return null;
    }
  }

  function safeUser(user) {
    if (!user) return null;
    return {
      userId: user.userId,
      firstName: user.firstName,
      lastName: user.lastName,
      email: user.email,
      phone: user.phone,
      userType: user.userType,
      role: user.role,
      staffType: user.staffType,
      passengerId: user.passengerId,
      staffId: user.staffId,
      emailVerified: user.emailVerified,
      active: user.active,
      createdAt: user.createdAt,
      updatedAt: user.updatedAt
    };
  }

  function getToken() {
    return localStorage.getItem(cfg.tokenKey);
  }

  function getUser() {
    const raw = localStorage.getItem(cfg.userKey);
    if (!raw) return null;
    try {
      return JSON.parse(raw);
    } catch (error) {
      return null;
    }
  }

  function tokenExpired(token) {
    const decoded = decodeJwt(token);
    if (!decoded || !decoded.exp) return false;
    return decoded.exp * 1000 <= Date.now();
  }

  function writeTokenCookie(token, expiresInMs) {
    const maxAge = Math.max(0, Math.floor((expiresInMs || 86400000) / 1000));
    document.cookie = `SRL_AUTH=${token}; Path=/; Max-Age=${maxAge}; SameSite=Lax`;
  }

  function isAuthenticated() {
    const token = getToken();
    if (!token) return false;
    if (tokenExpired(token)) {
      clearSession();
      return false;
    }
    const decoded = decodeJwt(token);
    if (decoded && decoded.exp) {
      writeTokenCookie(token, decoded.exp * 1000 - Date.now());
    }
    return true;
  }

  function saveSession(response) {
    localStorage.setItem(cfg.tokenKey, response.token);
    localStorage.setItem(cfg.userKey, JSON.stringify(safeUser(response.user)));
    writeTokenCookie(response.token, response.expiresInMs);
  }

  function clearSession() {
    localStorage.removeItem(cfg.tokenKey);
    localStorage.removeItem(cfg.userKey);
    document.cookie = "SRL_AUTH=; Path=/; Max-Age=0; SameSite=Lax";
  }

  function logout(redirectTo) {
    clearSession();
    const target = redirectTo || "/login.html";
    window.location.assign(target);
  }

  function currentRole() {
    const user = getUser();
    if (user && user.role) return user.role;
    const decoded = decodeJwt(getToken());
    return decoded && decoded.role ? decoded.role : null;
  }

  function hasRole() {
    const role = currentRole();
    return Array.from(arguments).includes(role);
  }

  function hasAny(roles) {
    return roles.includes(currentRole());
  }

  function isStaff() {
    return cfg.staffRoles.includes(currentRole());
  }

  function redirectAfterLogin() {
    const params = new URLSearchParams(window.location.search);
    const explicit = params.get("redirect");
    if (explicit && explicit.startsWith("/")) return explicit;
    return isStaff() ? "/admin/dashboard.html" : "/my-bookings.html";
  }

  function requireAuth(options) {
    const opts = options || {};
    if (!isAuthenticated()) {
      const current = window.location.pathname + window.location.search;
      window.location.assign("/login.html?redirect=" + encodeURIComponent(current));
      return false;
    }
    if (opts.roles && opts.roles.length && !hasAny(opts.roles)) {
      window.location.assign(isStaff() ? "/admin/dashboard.html?forbidden=1" : "/index.html?forbidden=1");
      return false;
    }
    return true;
  }

  window.Auth = {
    decodeJwt,
    saveSession,
    clearSession,
    logout,
    getToken,
    getUser,
    currentRole,
    hasRole,
    hasAny,
    isStaff,
    isAuthenticated,
    requireAuth,
    redirectAfterLogin
  };

  function initLoginPage() {
    const form = document.querySelector("#loginForm");
    if (!form) return;
    if (isAuthenticated()) {
      window.location.assign(redirectAfterLogin());
      return;
    }
    if (new URLSearchParams(window.location.search).get("verified")) {
      window.SmartRoute.showAlert("#pageAlert", "success", "Email verified. Please log in.");
    }
    form.addEventListener("submit", async (event) => {
      event.preventDefault();
      const data = window.SmartRoute.formData(form);
      const errors = {};
      if (!window.SmartRoute.validateEmail(data.email)) errors.email = "Enter a valid email address.";
      if (!window.SmartRoute.required(data.password)) errors.password = "Password is required.";
      window.SmartRoute.renderFieldErrors(errors);
      if (Object.keys(errors).length) return;
      const button = form.querySelector("button[type=submit]");
      window.SmartRoute.setLoading(button, true, "Signing in");
      try {
        const response = await window.Api.post("/auth/login", {
          email: data.email.trim(),
          password: data.password
        }, { redirectOnUnauthorized: false });
        saveSession(response);
        window.location.assign(redirectAfterLogin());
      } catch (error) {
        const message = error.status === 403
          ? "This account is inactive or not allowed to sign in."
          : window.SmartRoute.errorMessage(error);
        window.SmartRoute.showAlert("#pageAlert", "error", message);
      } finally {
        window.SmartRoute.setLoading(button, false);
      }
    });
  }

  function initRegisterPage() {
    const form = document.querySelector("#registerForm");
    if (!form) return;
    const otpDialog = document.querySelector("#registrationOtpDialog");
    const otpForm = document.querySelector("#registrationOtpForm");
    const resendButton = document.querySelector("#resendRegistrationOtp");
    const otpEmail = document.querySelector("#registrationOtpEmail");
    let registrationEmail = "";

    form.addEventListener("submit", async (event) => {
      event.preventDefault();
      const data = window.SmartRoute.formData(form);
      const errors = {};
      if (!window.SmartRoute.required(data.firstName)) errors.firstName = "First name is required.";
      if (!window.SmartRoute.required(data.lastName)) errors.lastName = "Last name is required.";
      if (!window.SmartRoute.validateEmail(data.email)) errors.email = "Enter a valid email address.";
      if (!window.SmartRoute.validatePhone(data.phone)) errors.phone = "Use a valid phone number.";
      if (!window.SmartRoute.validateStrongPassword(data.password)) errors.password = "Use upper and lower case letters, a number, and a special character.";
      if (data.password !== data.confirmPassword) errors.confirmPassword = "Passwords do not match.";
      window.SmartRoute.renderFieldErrors(errors);
      if (Object.keys(errors).length) return;
      const button = form.querySelector("button[type=submit]");
      window.SmartRoute.setLoading(button, true, "Creating account");
      try {
        const response = await window.Api.post("/auth/register", {
          firstName: data.firstName.trim(),
          lastName: data.lastName.trim(),
          email: data.email.trim(),
          password: data.password,
          phone: data.phone.trim()
        }, { redirectOnUnauthorized: false });
        registrationEmail = response.email || data.email.trim();
        if (otpEmail) otpEmail.textContent = registrationEmail;
        window.SmartRoute.showAlert("#pageAlert", "success", response.message || "Check your email for the verification code.");
        if (otpDialog && typeof otpDialog.showModal === "function") otpDialog.showModal();
      } catch (error) {
        window.SmartRoute.showAlert("#pageAlert", "error", window.SmartRoute.errorMessage(error));
      } finally {
        window.SmartRoute.setLoading(button, false);
      }
    });

    if (otpForm) {
      otpForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        const data = window.SmartRoute.formData(otpForm);
        const button = otpForm.querySelector("button[type=submit]");
        window.SmartRoute.setLoading(button, true, "Verifying");
        try {
          const response = await window.Api.post("/auth/register/verify", {
            email: registrationEmail,
            otp: data.otp
          }, { redirectOnUnauthorized: false });
          if (otpDialog && otpDialog.open) otpDialog.close();
          window.SmartRoute.showAlert("#pageAlert", "success", response.message || "Email verified. Please log in.");
          window.setTimeout(() => window.location.assign("/login.html?verified=1"), 1100);
        } catch (error) {
          window.SmartRoute.showAlert("#registrationOtpAlert", "error", window.SmartRoute.errorMessage(error));
        } finally {
          window.SmartRoute.setLoading(button, false);
        }
      });
    }

    if (resendButton) {
      resendButton.addEventListener("click", async () => {
        if (!registrationEmail) return;
        window.SmartRoute.setLoading(resendButton, true, "Sending");
        try {
          const response = await window.Api.post("/auth/register/resend", {
            email: registrationEmail
          }, { redirectOnUnauthorized: false });
          window.SmartRoute.showAlert("#registrationOtpAlert", "success", response.message || "Verification code sent.");
        } catch (error) {
          window.SmartRoute.showAlert("#registrationOtpAlert", "error", window.SmartRoute.errorMessage(error));
        } finally {
          window.SmartRoute.setLoading(resendButton, false);
        }
      });
    }
  }

  document.addEventListener("DOMContentLoaded", () => {
    if (document.body.dataset.page === "login") initLoginPage();
    if (document.body.dataset.page === "register") initRegisterPage();
  });
})();
