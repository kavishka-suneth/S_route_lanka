(function () {
  const ui = window.SmartRoute;

  async function loadProfile() {
    if (!window.Auth.requireAuth()) return;
    try {
      const user = await window.Api.get("/users/me");
      localStorage.setItem(window.SmartRouteConfig.userKey, JSON.stringify({
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
        active: user.active,
        createdAt: user.createdAt,
        updatedAt: user.updatedAt
      }));
      ui.$("#profileSummary").innerHTML = `
        <div class="detail-grid">
          <div class="detail"><span>Name</span><strong>${ui.escapeHtml(ui.fullName(user))}</strong></div>
          <div class="detail"><span>Email</span><strong>${ui.escapeHtml(user.email)}</strong></div>
          <div class="detail"><span>Phone</span><strong>${ui.escapeHtml(user.phone || "-")}</strong></div>
          <div class="detail"><span>Role</span><strong>${ui.escapeHtml(ui.formatStatus(user.role))}</strong></div>
        </div>`;
      ui.$('[name="firstName"]').value = user.firstName || "";
      ui.$('[name="lastName"]').value = user.lastName || "";
      ui.$('[name="phone"]').value = user.phone || "";
    } catch (error) {
      ui.showAlert("#pageAlert", "error", ui.errorMessage(error));
    }
  }

  function initForms() {
    const profileForm = ui.$("#profileForm");
    profileForm.addEventListener("submit", async (event) => {
      event.preventDefault();
      const data = ui.formData(profileForm);
      const errors = {};
      if (!ui.required(data.firstName)) errors.firstName = "First name is required.";
      if (!ui.required(data.lastName)) errors.lastName = "Last name is required.";
      if (!ui.validatePhone(data.phone)) errors.phone = "Use a valid phone number.";
      ui.renderFieldErrors(errors);
      if (Object.keys(errors).length) return;
      try {
        await window.Api.put("/users/me", {
          firstName: data.firstName.trim(),
          lastName: data.lastName.trim(),
          phone: data.phone.trim()
        });
        ui.showAlert("#pageAlert", "success", "Profile updated.");
        loadProfile();
      } catch (error) {
        ui.showAlert("#pageAlert", "error", ui.errorMessage(error));
      }
    });

    const passwordForm = ui.$("#passwordForm");
    passwordForm.addEventListener("submit", async (event) => {
      event.preventDefault();
      const data = ui.formData(passwordForm);
      const errors = {};
      if (!ui.required(data.currentPassword)) errors.currentPassword = "Current password is required.";
      if (!ui.validatePassword(data.newPassword)) errors.newPassword = "Use at least 8 characters with letters and numbers.";
      if (data.newPassword !== data.confirmPassword) errors.confirmPassword = "Passwords do not match.";
      ui.renderFieldErrors(errors);
      if (Object.keys(errors).length) return;
      try {
        await window.Api.patch("/users/me/password", {
          currentPassword: data.currentPassword,
          newPassword: data.newPassword
        });
        passwordForm.reset();
        ui.showAlert("#pageAlert", "success", "Password changed.");
      } catch (error) {
        ui.showAlert("#pageAlert", "error", ui.errorMessage(error));
      }
    });

    ui.$("#profileLogoutBtn").addEventListener("click", () => window.Auth.logout("/index.html"));
  }

  document.addEventListener("DOMContentLoaded", () => {
    if (document.body.dataset.page === "profile") {
      initForms();
      loadProfile();
    }
  });
})();
