(function () {
  const ui = window.SmartRoute;
  const cfg = window.SmartRouteConfig;

  function renderTickets(data) {
    const node = ui.$("#supportTickets");
    if (!data.content || data.content.length === 0) {
      node.innerHTML = `<div class="empty-state">No support tickets yet.</div>`;
      return;
    }
    node.innerHTML = `
      <div class="table-wrap card flush">
        <table>
          <thead><tr><th>Ticket</th><th>Subject</th><th>Description and response</th><th>Status</th><th>Created</th></tr></thead>
          <tbody>
            ${data.content.map((ticket) => `
              <tr>
                <td>#${ticket.ticketId}</td>
                <td>${ui.escapeHtml(ticket.subject)}</td>
                <td><div style="white-space:pre-wrap">${ui.escapeHtml(ticket.description)}</div></td>
                <td>${ui.badge(ticket.status)}</td>
                <td>${ui.datetime(ticket.createdDate)}</td>
              </tr>`).join("")}
          </tbody>
        </table>
      </div>`;
  }

  async function loadTickets(page) {
    try {
      const data = await window.Api.get("/support-tickets/my", { page: page || 0, size: cfg.defaultPageSize });
      renderTickets(data);
      ui.pagination("#supportPagination", data, loadTickets);
    } catch (error) {
      ui.showAlert("#pageAlert", "error", ui.errorMessage(error));
    }
  }

  function initSupportPage() {
    if (!window.Auth.requireAuth()) return;
    const form = ui.$("#supportForm");
    form.addEventListener("submit", async (event) => {
      event.preventDefault();
      const data = ui.formData(form);
      const errors = {};
      if (!ui.required(data.subject)) errors.subject = "Subject is required.";
      if (!ui.required(data.description)) errors.description = "Description is required.";
      ui.renderFieldErrors(errors);
      if (Object.keys(errors).length) return;
      try {
        await window.Api.post("/support-tickets", {
          subject: data.subject.trim(),
          description: data.description.trim()
        });
        form.reset();
        ui.showAlert("#pageAlert", "success", "Support ticket created.");
        loadTickets(0);
      } catch (error) {
        ui.showAlert("#pageAlert", "error", ui.errorMessage(error));
      }
    });
    loadTickets(0);
  }

  document.addEventListener("DOMContentLoaded", () => {
    if (document.body.dataset.page === "support") initSupportPage();
  });
})();
