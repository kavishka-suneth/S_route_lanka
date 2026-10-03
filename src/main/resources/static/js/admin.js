(function () {
  const ui = window.SmartRoute;
  const cfg = window.SmartRouteConfig;

  function pageName() {
    return document.body.dataset.adminPage;
  }

  function pageSize() {
    return cfg.defaultPageSize;
  }

  function unsupported(container, message) {
    ui.$(container).innerHTML = `<div class="empty-state">${ui.escapeHtml(message)}</div>`;
  }

  async function safeMetric(selector, label, loader, formatter) {
    try {
      const data = await loader();
      ui.$(selector).innerHTML = `<span>${ui.escapeHtml(label)}</span><strong>${ui.escapeHtml(formatter(data))}</strong>`;
    } catch (error) {
      ui.$(selector).innerHTML = `<span>${ui.escapeHtml(label)}</span><strong>Unavailable</strong>`;
    }
  }

  async function initDashboard() {
    await Promise.all([
      safeMetric("#metricUsers", "Total users", () => window.Api.get("/reports/users"), (d) => d.totalUsers),
      safeMetric("#metricBookings", "Total bookings", () => window.Api.get("/reports/bookings/count"), (d) => d.totalBookings),
      safeMetric("#metricRevenue", "Total revenue", () => window.Api.get("/reports/revenue/total"), (d) => ui.money(d.totalRevenue)),
      safeMetric("#metricPayments", "Successful payments", () => window.Api.get("/reports/payments"), (d) => d.successful),
      safeMetric("#metricSchedules", "Schedules", () => window.Api.get("/schedules", { size: 1 }), (d) => d.totalElements),
      safeMetric("#metricSupport", "Open support tickets", () => window.Api.get("/support-tickets", { status: "OPEN", size: 1 }), (d) => d.totalElements),
      safeMetric("#metricOccupancy", "Average occupancy", () => window.Api.get("/reports/schedule-occupancy", { size: 50 }), (d) => {
        const rows = d.content || [];
        if (!rows.length) return "0%";
        return (rows.reduce((sum, row) => sum + Number(row.occupancyPercent || 0), 0) / rows.length).toFixed(1) + "%";
      })
    ]);
  }

  function filterRows(rows, keyword, values) {
    if (!keyword) return rows;
    const needle = keyword.toLowerCase();
    return rows.filter((row) => values(row).join(" ").toLowerCase().includes(needle));
  }

  async function initUsers() {
    let currentPage = 0;
    let lastRows = [];

    async function load(page) {
      currentPage = page || 0;
      const role = ui.$("#roleFilter").value;
      const active = ui.$("#activeFilter").value;
      const data = await window.Api.get("/users", {
        role,
        active,
        page: currentPage,
        size: pageSize()
      });
      lastRows = data.content || [];
      render(data);
      ui.pagination("#adminPagination", data, load);
    }

    function render(data) {
      const keyword = ui.$("#userSearch").value.trim();
      const rows = filterRows(lastRows, keyword, (user) => [user.firstName, user.lastName, user.email, user.role]);
      if (!rows.length) {
        ui.$("#adminTable").innerHTML = `<div class="empty-state">No users found on this page.</div>`;
        return;
      }
      const canRole = window.Auth.hasRole("SYSTEM_ADMIN");
      const canActive = window.Auth.hasAny(["SYSTEM_ADMIN", "IT_SUPPORT"]);
      ui.$("#adminTable").innerHTML = `
        <div class="table-wrap card flush">
          <table>
            <thead><tr><th>User</th><th>Email</th><th>Phone</th><th>Role</th><th>Active</th><th>Actions</th></tr></thead>
            <tbody>${rows.map((user) => `
              <tr>
                <td>${ui.escapeHtml(ui.fullName(user))}</td>
                <td>${ui.escapeHtml(user.email)}</td>
                <td>${ui.escapeHtml(user.phone || "-")}</td>
                <td>
                  <select data-role="${user.userId}" ${canRole ? "" : "disabled"}>
                    ${cfg.roles.map((role) => `<option value="${role}"${role === user.role ? " selected" : ""}>${ui.formatStatus(role)}</option>`).join("")}
                  </select>
                </td>
                <td>${ui.badge(user.active ? "ACTIVE" : "INACTIVE")}</td>
                <td>
                  <div class="actions">
                    <button class="btn btn-small btn-ghost" data-view="${user.userId}" type="button">View</button>
                    <button class="btn btn-small btn-danger" data-active="${user.userId}" data-next="${!user.active}" type="button" ${canActive ? "" : "disabled"}>${user.active ? "Deactivate" : "Activate"}</button>
                  </div>
                </td>
              </tr>`).join("")}</tbody>
          </table>
        </div>`;
      ui.$all("[data-role]").forEach((select) => {
        select.addEventListener("change", async () => {
          if (!confirm("Change this user's role?")) return;
          try {
            await window.Api.patch(`/users/${select.dataset.role}/role`, { role: select.value });
            ui.showAlert("#pageAlert", "success", "Role updated.");
            load(currentPage);
          } catch (error) {
            ui.showAlert("#pageAlert", "error", ui.errorMessage(error));
          }
        });
      });
      ui.$all("[data-active]").forEach((button) => {
        button.addEventListener("click", async () => {
          if (!confirm("Update this account active status?")) return;
          try {
            await window.Api.patch(`/users/${button.dataset.active}/active`, { active: button.dataset.next === "true" });
            ui.showAlert("#pageAlert", "success", "Account status updated.");
            load(currentPage);
          } catch (error) {
            ui.showAlert("#pageAlert", "error", ui.errorMessage(error));
          }
        });
      });
      ui.$all("[data-view]").forEach((button) => {
        button.addEventListener("click", async () => {
          try {
            const user = await window.Api.get(`/users/${button.dataset.view}`);
            ui.$("#detailsPanel").innerHTML = `
              <h3>User details</h3>
              <div class="detail-grid">
                <div class="detail"><span>Name</span><strong>${ui.escapeHtml(ui.fullName(user))}</strong></div>
                <div class="detail"><span>Email</span><strong>${ui.escapeHtml(user.email)}</strong></div>
                <div class="detail"><span>Role</span><strong>${ui.escapeHtml(ui.formatStatus(user.role))}</strong></div>
                <div class="detail"><span>Passenger ID</span><strong>${ui.escapeHtml(user.passengerId || "-")}</strong></div>
                <div class="detail"><span>Staff ID</span><strong>${ui.escapeHtml(user.staffId || "-")}</strong></div>
                <div class="detail"><span>Created</span><strong>${ui.datetime(user.createdAt)}</strong></div>
              </div>`;
          } catch (error) {
            ui.showAlert("#pageAlert", "error", ui.errorMessage(error));
          }
        });
      });
    }

    ui.$("#userSearch").addEventListener("input", () => render({ content: lastRows }));
    ui.$("#roleFilter").innerHTML = ui.selectOptions(cfg.roles, "", "All roles");
    ui.$("#roleFilter").addEventListener("change", () => load(0));
    ui.$("#activeFilter").addEventListener("change", () => load(0));
    load(0).catch((error) => ui.showAlert("#pageAlert", "error", ui.errorMessage(error)));
  }

  function crudTable(config) {
    let currentPage = 0;
    let rows = [];

    async function load(page) {
      currentPage = page || 0;
      const params = { page: currentPage, size: pageSize() };
      if (config.activeFilter && ui.$("#activeFilter")) {
        const active = ui.$("#activeFilter").value;
        if (active !== "") params.active = active;
      }
      const availability = ui.$("#availabilityFilter");
      if (availability && availability.value) params.availabilityStatus = availability.value;
      const data = await window.Api.get(config.path, params);
      rows = data.content || [];
      render(data);
      ui.pagination("#adminPagination", data, load);
    }

    function render(data) {
      const keywordNode = ui.$("#entitySearch");
      const keyword = keywordNode ? keywordNode.value.trim() : "";
      const visible = filterRows(rows, keyword, config.searchValues);
      if (!visible.length) {
        ui.$("#adminTable").innerHTML = `<div class="empty-state">No records found.</div>`;
        return;
      }
      ui.$("#adminTable").innerHTML = `
        <div class="table-wrap card flush">
          <table>
            <thead><tr>${config.columns.map((col) => `<th>${ui.escapeHtml(col.label)}</th>`).join("")}<th>Actions</th></tr></thead>
            <tbody>${visible.map((row) => `
              <tr>
                ${config.columns.map((col) => `<td>${col.render(row)}</td>`).join("")}
                <td>
                  <div class="actions">
                    <button class="btn btn-small btn-ghost" data-edit="${row[config.id]}">Edit</button>
                    <button class="btn btn-small btn-danger" data-delete="${row[config.id]}">${config.deleteLabel || "Delete"}</button>
                  </div>
                </td>
              </tr>`).join("")}</tbody>
          </table>
        </div>`;
      ui.$all("[data-edit]").forEach((button) => {
        button.addEventListener("click", () => {
          const row = rows.find((item) => String(item[config.id]) === button.dataset.edit);
          config.fill(row);
        });
      });
      ui.$all("[data-delete]").forEach((button) => {
        button.addEventListener("click", async () => {
          if (!confirm(config.confirmDelete || "Delete this record?")) return;
          try {
            await window.Api.delete(`${config.path}/${button.dataset.delete}`);
            ui.showAlert("#pageAlert", "success", config.deletedMessage || "Record updated.");
            load(currentPage);
          } catch (error) {
            ui.showAlert("#pageAlert", "error", ui.errorMessage(error));
          }
        });
      });
    }

    const form = ui.$("#entityForm");
    form.addEventListener("submit", async (event) => {
      event.preventDefault();
      const submit = form.querySelector('button[type="submit"]');
      if (submit && submit.disabled) return;
      const data = ui.formData(form);
      const payload = config.payload(data);
      const id = data[config.id];
      try {
        ui.setLoading(submit, true, "Saving");
        if (id) await window.Api.put(`${config.path}/${id}`, payload);
        else await window.Api.post(config.path, payload);
        form.reset();
        ui.$(`[name="${config.id}"]`).value = "";
        ui.showAlert("#pageAlert", "success", id ? "Record updated." : "Record created.");
        load(currentPage);
      } catch (error) {
        ui.showAlert("#pageAlert", "error", ui.errorMessage(error));
      } finally {
        ui.setLoading(submit, false);
      }
    });
    const clear = ui.$("#clearFormBtn");
    if (clear) clear.addEventListener("click", () => {
      form.reset();
      const idField = form.querySelector(`[name="${config.id}"]`);
      if (idField) idField.value = "";
    });
    const search = ui.$("#entitySearch");
    if (search) search.addEventListener("input", () => render({ content: rows }));
    const active = ui.$("#activeFilter");
    if (active) active.addEventListener("change", () => load(0));
    const availability = ui.$("#availabilityFilter");
    if (availability) availability.addEventListener("change", () => load(0));
    load(0).catch((error) => ui.showAlert("#pageAlert", "error", ui.errorMessage(error)));
  }

  function initRoutes() {
    crudTable({
      path: "/routes",
      id: "routeId",
      activeFilter: true,
      deleteLabel: "Deactivate",
      columns: [
        { label: "ID", render: (r) => r.routeId },
        { label: "Origin", render: (r) => ui.escapeHtml(r.origin) },
        { label: "Destination", render: (r) => ui.escapeHtml(r.destination) },
        { label: "Active", render: (r) => ui.badge(r.active ? "ACTIVE" : "INACTIVE") }
      ],
      searchValues: (r) => [r.origin, r.destination, r.active ? "active" : "inactive"],
      fill: (r) => {
        ui.$('[name="routeId"]').value = r.routeId;
        ui.$('[name="origin"]').value = r.origin;
        ui.$('[name="destination"]').value = r.destination;
        ui.$('[name="active"]').checked = Boolean(r.active);
      },
      payload: (d) => ({ origin: d.origin.trim(), destination: d.destination.trim(), active: d.active === "on" }),
      confirmDelete: "Deactivate this route? Existing schedules are preserved but passengers cannot book inactive routes.",
      deletedMessage: "Route deactivated."
    });
  }

  function assignmentText(resource) {
    const assignment = resource.nextAssignment;
    if (!assignment) return resource.assignedNow ? "Assigned now" : "No active assignment";
    return `${resource.assignedNow ? "Assigned now" : "Next"} ${assignment.route} ${assignment.travelDate} ${assignment.departureTime}`;
  }

  function assignmentLabel(resource) {
    const assignment = resource.nextAssignment;
    if (!assignment) return `<span class="muted">No active assignment</span>`;
    return `<div>${resource.assignedNow ? ui.badge("ASSIGNED") : ui.badge("SCHEDULED")}</div><small class="muted">${ui.escapeHtml(assignment.route)}<br>${ui.date(assignment.travelDate)} ${ui.time(assignment.departureTime)} - ${ui.time(assignment.arrivalTime)}</small>`;
  }

  function initBuses() {
    ui.$('[name="busType"]').innerHTML = ui.selectOptions(cfg.busTypes, "STANDARD");
    ui.$('[name="availabilityStatus"]').innerHTML = ui.selectOptions(cfg.availabilityStatuses, "AVAILABLE");
    ui.$('[name="status"]').innerHTML = ui.selectOptions(cfg.busStatuses, "ACTIVE");
    crudTable({
      path: "/buses",
      id: "busId",
      activeFilter: true,
      deleteLabel: "Deactivate",
      columns: [
        { label: "Registration", render: (r) => ui.escapeHtml(r.registrationNo) },
        { label: "Capacity", render: (r) => `${r.capacity} seats` },
        { label: "Type", render: (r) => ui.formatStatus(r.busType) },
        { label: "Operational", render: (r) => ui.badge(r.availabilityStatus) },
        { label: "Lifecycle", render: (r) => ui.badge(r.status) },
        { label: "Assignment", render: (r) => assignmentLabel(r) }
      ],
      searchValues: (r) => [r.registrationNo, r.busType, r.status, r.availabilityStatus, assignmentText(r)],
      fill: (r) => {
        ["busId", "registrationNo", "capacity", "busType", "availabilityStatus", "status"].forEach((name) => {
          ui.$(`[name="${name}"]`).value = r[name];
        });
      },
      payload: (d) => ({
        registrationNo: d.registrationNo.trim(),
        capacity: Number(d.capacity),
        busType: d.busType,
        availabilityStatus: d.availabilityStatus,
        status: d.status
      }),
      confirmDelete: "Deactivate this bus?"
    });
  }

  function initDrivers() {
    ui.$('[name="availabilityStatus"]').innerHTML = ui.selectOptions(["AVAILABLE", "UNAVAILABLE"], "AVAILABLE");
    crudTable({
      path: "/drivers",
      id: "driverId",
      deleteLabel: "Make unavailable",
      columns: [
        { label: "Name", render: (r) => ui.escapeHtml(`${r.firstName} ${r.lastName}`) },
        { label: "License", render: (r) => ui.escapeHtml(r.licenseNo) },
        { label: "Phone", render: (r) => ui.escapeHtml(r.phone || "-") },
        { label: "Operational", render: (r) => ui.badge(r.availabilityStatus) },
        { label: "Assignment", render: (r) => assignmentLabel(r) }
      ],
      searchValues: (r) => [r.firstName, r.lastName, r.licenseNo, r.phone, r.availabilityStatus, assignmentText(r)],
      fill: (r) => {
        ["driverId", "firstName", "lastName", "licenseNo", "phone", "availabilityStatus"].forEach((name) => {
          ui.$(`[name="${name}"]`).value = r[name] || "";
        });
      },
      payload: (d) => ({
        firstName: d.firstName.trim(),
        lastName: d.lastName.trim(),
        licenseNo: d.licenseNo.trim(),
        phone: d.phone.trim(),
        availabilityStatus: d.availabilityStatus
      }),
      confirmDelete: "Mark this driver unavailable?"
    });
  }

  async function initSchedules() {
    let currentPage = 0;
    let editingSchedule = null;
    let availabilityRequest = 0;
    async function loadOptions() {
      const routes = await window.Api.get("/routes", { size: 100 });
      ui.$('[name="routeId"]').innerHTML = `<option value="">Select route</option>` + (routes.content || []).map((r) => `<option value="${r.routeId}">${ui.escapeHtml(r.origin)} to ${ui.escapeHtml(r.destination)}</option>`).join("");
      resetAvailabilitySelects();
    }
    async function load(page) {
      currentPage = page || 0;
      const data = await window.Api.get("/schedules", { page: currentPage, size: pageSize() });
      if (!data.content.length) {
        ui.$("#adminTable").innerHTML = `<div class="empty-state">No schedules found.</div>`;
      } else {
        ui.$("#adminTable").innerHTML = `
          <div class="table-wrap card flush"><table>
            <thead><tr><th>Route</th><th>Date</th><th>Time</th><th>Bus</th><th>Driver</th><th>Fare</th><th>Status</th><th>Actions</th></tr></thead>
            <tbody>${data.content.map((s) => `
              <tr>
                <td>${ui.escapeHtml(ui.routeLabel(s))}</td>
                <td>${ui.date(s.travelDate)}</td>
                <td>${ui.time(s.departureTime)} - ${ui.time(s.arrivalTime)}</td>
                <td>${ui.escapeHtml(s.bus.registrationNo)}</td>
                <td>${ui.escapeHtml(s.driver.firstName)} ${ui.escapeHtml(s.driver.lastName)}</td>
                <td>${ui.money(s.fare)}</td>
                <td>${ui.badge(s.status)}</td>
                <td><div class="actions">
                  <button class="btn btn-small btn-ghost" data-edit="${s.scheduleId}">Edit</button>
                  <button class="btn btn-small btn-ghost" data-delay="${s.scheduleId}">Delay</button>
                  <button class="btn btn-small btn-danger" data-cancel="${s.scheduleId}">Cancel</button>
                </div></td>
              </tr>`).join("")}</tbody></table></div>`;
        ui.$all("[data-edit]").forEach((button) => {
          button.addEventListener("click", async () => {
            const s = data.content.find((item) => String(item.scheduleId) === button.dataset.edit);
            editingSchedule = s;
            ui.$('[name="scheduleId"]').value = s.scheduleId;
            ui.$('[name="routeId"]').value = s.route.routeId;
            ui.$('[name="travelDate"]').value = s.travelDate;
            ui.$('[name="departureTime"]').value = s.departureTime;
            ui.$('[name="arrivalTime"]').value = s.arrivalTime;
            ui.$('[name="fare"]').value = s.fare;
            ui.$('[name="status"]').value = s.status;
            await loadAvailability(s);
          });
        });
        async function statusUpdate(id, status) {
          try {
            await window.Api.patch(`/schedules/${id}/status`, { status });
            ui.showAlert("#pageAlert", "success", `Schedule ${ui.formatStatus(status).toLowerCase()}.`);
            load(currentPage);
          } catch (error) {
            ui.showAlert("#pageAlert", "error", ui.errorMessage(error));
          }
        }
        ui.$all("[data-delay]").forEach((b) => b.addEventListener("click", () => statusUpdate(b.dataset.delay, "DELAYED")));
        ui.$all("[data-cancel]").forEach((b) => b.addEventListener("click", () => {
          if (confirm("Cancel this schedule and active bookings?")) statusUpdate(b.dataset.cancel, "CANCELLED");
        }));
      }
      ui.pagination("#adminPagination", data, load);
    }
    ui.$('[name="status"]').innerHTML = ui.selectOptions(cfg.scheduleStatuses, "SCHEDULED");
    await loadOptions();

    function availabilityParams() {
      const travelDate = ui.$('[name="travelDate"]').value;
      const departureTime = ui.$('[name="departureTime"]').value;
      const arrivalTime = ui.$('[name="arrivalTime"]').value;
      if (!travelDate || !departureTime || !arrivalTime || arrivalTime <= departureTime) return null;
      const params = { travelDate, departureTime, arrivalTime };
      const id = ui.$('[name="scheduleId"]').value;
      if (id) params.excludeScheduleId = id;
      return params;
    }

    function resetAvailabilitySelects() {
      ui.$('[name="busId"]').innerHTML = `<option value="">Select date and time first</option>`;
      ui.$('[name="driverId"]').innerHTML = `<option value="">Select date and time first</option>`;
      ui.$('[name="busId"]').disabled = true;
      ui.$('[name="driverId"]').disabled = true;
      ui.$("#busAvailabilityState").textContent = "";
      ui.$("#driverAvailabilityState").textContent = "";
    }

    function renderBusOptions(items, current) {
      const rows = [...items];
      if (current && !rows.some((bus) => String(bus.busId) === String(current.busId))) rows.unshift(current);
      ui.$('[name="busId"]').innerHTML = `<option value="">Select bus</option>` + rows.map((b) => `<option value="${b.busId}">${ui.escapeHtml(b.registrationNo)} (${b.capacity} seats)${current && String(b.busId) === String(current.busId) ? " - current" : ""}</option>`).join("");
      ui.$('[name="busId"]').disabled = rows.length === 0;
      ui.$('[name="busId"]').value = current ? current.busId : "";
      ui.$("#busAvailabilityState").textContent = rows.length ? "" : "No buses available for this time";
    }

    function renderDriverOptions(items, current) {
      const rows = [...items];
      if (current && !rows.some((driver) => String(driver.driverId) === String(current.driverId))) rows.unshift(current);
      ui.$('[name="driverId"]').innerHTML = `<option value="">Select driver</option>` + rows.map((d) => `<option value="${d.driverId}">${ui.escapeHtml(d.firstName)} ${ui.escapeHtml(d.lastName)}${current && String(d.driverId) === String(current.driverId) ? " - current" : ""}</option>`).join("");
      ui.$('[name="driverId"]').disabled = rows.length === 0;
      ui.$('[name="driverId"]').value = current ? current.driverId : "";
      ui.$("#driverAvailabilityState").textContent = rows.length ? "" : "No drivers available for this time";
    }

    async function loadAvailability(currentSchedule) {
      const params = availabilityParams();
      availabilityRequest += 1;
      const requestId = availabilityRequest;
      if (!params) {
        resetAvailabilitySelects();
        if (ui.$('[name="arrivalTime"]').value && ui.$('[name="departureTime"]').value && ui.$('[name="arrivalTime"]').value <= ui.$('[name="departureTime"]').value) {
          ui.$("#busAvailabilityState").textContent = "Arrival time must be after departure time";
          ui.$("#driverAvailabilityState").textContent = "Arrival time must be after departure time";
        }
        return;
      }
      ui.$('[name="busId"]').disabled = true;
      ui.$('[name="driverId"]').disabled = true;
      ui.$('[name="busId"]').innerHTML = `<option value="">Loading buses...</option>`;
      ui.$('[name="driverId"]').innerHTML = `<option value="">Loading drivers...</option>`;
      ui.$("#busAvailabilityState").textContent = "";
      ui.$("#driverAvailabilityState").textContent = "";
      try {
        const [buses, drivers] = await Promise.all([
          window.Api.get("/buses/available", params),
          window.Api.get("/drivers/available", params)
        ]);
        if (requestId !== availabilityRequest) return;
        renderBusOptions(buses || [], currentSchedule ? currentSchedule.bus : null);
        renderDriverOptions(drivers || [], currentSchedule ? currentSchedule.driver : null);
      } catch (error) {
        if (requestId !== availabilityRequest) return;
        resetAvailabilitySelects();
        ui.$("#busAvailabilityState").textContent = ui.errorMessage(error);
        ui.$("#driverAvailabilityState").textContent = ui.errorMessage(error);
      }
    }

    ["travelDate", "departureTime", "arrivalTime"].forEach((name) => {
      ui.$(`[name="${name}"]`).addEventListener("change", () => {
        editingSchedule = null;
        loadAvailability(null);
      });
    });

    ui.$("#clearScheduleFormBtn").addEventListener("click", () => {
      ui.$("#entityForm").reset();
      ui.$('[name="scheduleId"]').value = "";
      editingSchedule = null;
      resetAvailabilitySelects();
    });

    ui.$("#entityForm").addEventListener("submit", async (event) => {
      event.preventDefault();
      const submit = event.currentTarget.querySelector('button[type="submit"]');
      if (submit && submit.disabled) return;
      const d = ui.formData(event.currentTarget);
      const payload = {
        routeId: Number(d.routeId),
        busId: Number(d.busId),
        driverId: Number(d.driverId),
        travelDate: d.travelDate,
        departureTime: d.departureTime,
        arrivalTime: d.arrivalTime,
        fare: Number(d.fare),
        status: d.status
      };
      try {
        ui.setLoading(submit, true, "Saving");
        if (d.scheduleId) await window.Api.put(`/schedules/${d.scheduleId}`, payload);
        else await window.Api.post("/schedules", payload);
        event.currentTarget.reset();
        editingSchedule = null;
        resetAvailabilitySelects();
        ui.showAlert("#pageAlert", "success", "Schedule saved.");
        load(currentPage);
      } catch (error) {
        ui.showAlert("#pageAlert", "error", ui.errorMessage(error));
      } finally {
        ui.setLoading(submit, false);
      }
    });
    load(0).catch((error) => ui.showAlert("#pageAlert", "error", ui.errorMessage(error)));
  }

  function renderBooking(booking, onChange) {
    const passengerDetails = (booking.passengerDetails || []).map((passenger) => `Seat ${ui.escapeHtml(passenger.seatNumber)}: ${ui.escapeHtml(passenger.fullName)}${passenger.email ? ` (${ui.escapeHtml(passenger.email)})` : ""}`).join("<br>");
    ui.$("#lookupResult").innerHTML = `
      <div class="card">
        <h3>${ui.escapeHtml(booking.bookingReference)}</h3>
        <div class="detail-grid">
          <div class="detail"><span>Passenger</span><strong>${ui.escapeHtml(ui.fullName(booking.passengerUser))}</strong></div>
          <div class="detail"><span>Route</span><strong>${ui.escapeHtml(ui.routeLabel(booking.schedule))}</strong></div>
          <div class="detail"><span>Date</span><strong>${ui.date(booking.schedule.travelDate)} ${ui.time(booking.schedule.departureTime)}</strong></div>
          <div class="detail"><span>Seats</span><strong>${ui.escapeHtml(ui.seatsLabel(booking.seats))}</strong></div>
          <div class="detail"><span>Total</span><strong>${ui.money(booking.totalAmount)}</strong></div>
          <div class="detail"><span>Status</span>${ui.badge(booking.bookingStatus)}</div>
          ${passengerDetails ? `<div class="detail"><span>Seat passengers</span><strong>${passengerDetails}</strong></div>` : ""}
        </div>
        <div class="actions" style="margin-top:16px">
          <button class="btn btn-danger" id="adminCancelBooking" type="button">Cancel booking</button>
        </div>
      </div>`;
    ui.$("#adminCancelBooking").addEventListener("click", async () => {
      if (!confirm("Cancel this booking?")) return;
      try {
        const cancelled = await window.Api.patch(`/bookings/${booking.bookingId}/cancel`, null);
        renderBooking(cancelled, onChange);
        ui.showAlert("#pageAlert", "success", "Booking cancelled.");
        if (onChange) onChange(cancelled);
      } catch (error) {
        ui.showAlert("#pageAlert", "error", ui.errorMessage(error));
      }
    });
  }

  function initBookingsAdmin() {
    let currentPage = 0;
    let rows = [];

    async function load(page) {
      currentPage = page || 0;
      const data = await window.Api.get("/bookings", {
        keyword: ui.$("#bookingKeyword").value.trim(),
        status: ui.$("#bookingStatusFilter").value,
        page: currentPage,
        size: pageSize()
      });
      rows = data.content || [];
      if (!rows.length) {
        ui.$("#adminTable").innerHTML = `<div class="empty-state">No bookings found.</div>`;
      } else {
        ui.$("#adminTable").innerHTML = `<div class="table-wrap card flush"><table>
          <thead><tr><th>ID</th><th>Reference</th><th>Passenger</th><th>Route</th><th>Date</th><th>Seats</th><th>Total</th><th>Status</th><th>Actions</th></tr></thead>
          <tbody>${rows.map((booking) => `<tr>
            <td>#${booking.bookingId}</td>
            <td>${ui.escapeHtml(booking.bookingReference)}</td>
            <td>${ui.escapeHtml(ui.fullName(booking.passengerUser))}</td>
            <td>${ui.escapeHtml(ui.routeLabel(booking.schedule))}</td>
            <td>${ui.date(booking.schedule.travelDate)} ${ui.time(booking.schedule.departureTime)}</td>
            <td>${ui.escapeHtml(ui.seatsLabel(booking.seats))}</td>
            <td>${ui.money(booking.totalAmount)}</td>
            <td>${ui.badge(booking.bookingStatus)}</td>
            <td><div class="actions">
              <button class="btn btn-small btn-ghost" data-view-booking="${booking.bookingId}" type="button">View</button>
              <button class="btn btn-small btn-danger" data-cancel-booking="${booking.bookingId}" type="button" ${["CANCELLED", "COMPLETED"].includes(booking.bookingStatus) ? "disabled" : ""}>Cancel</button>
            </div></td>
          </tr>`).join("")}</tbody></table></div>`;
        ui.$all("[data-view-booking]").forEach((button) => {
          button.addEventListener("click", () => {
            const booking = rows.find((item) => String(item.bookingId) === button.dataset.viewBooking);
            if (booking) renderBooking(booking, () => load(currentPage));
          });
        });
        ui.$all("[data-cancel-booking]").forEach((button) => {
          button.addEventListener("click", async () => {
            if (!confirm("Cancel this booking?")) return;
            try {
              const cancelled = await window.Api.patch(`/bookings/${button.dataset.cancelBooking}/cancel`, null);
              renderBooking(cancelled);
              ui.showAlert("#pageAlert", "success", "Booking cancelled.");
              load(currentPage);
            } catch (error) {
              ui.showAlert("#pageAlert", "error", ui.errorMessage(error));
            }
          });
        });
      }
      ui.pagination("#adminPagination", data, load);
    }

    ui.$("#bookingStatusFilter").innerHTML = ui.selectOptions(cfg.bookingStatuses, "", "All statuses");
    ui.$("#bookingKeyword").addEventListener("input", () => load(0));
    ui.$("#bookingStatusFilter").addEventListener("change", () => load(0));
    ui.$("#refreshBookingsBtn").addEventListener("click", () => load(currentPage));
    load(0).catch((error) => ui.showAlert("#pageAlert", "error", ui.errorMessage(error)));

    ui.$("#bookingLookupForm").addEventListener("submit", async (event) => {
      event.preventDefault();
      const id = ui.formData(event.currentTarget).bookingId;
      try {
        renderBooking(await window.Api.get(`/bookings/${id}`), () => load(currentPage));
      } catch (error) {
        ui.showAlert("#pageAlert", "error", ui.errorMessage(error));
      }
    });
  }

  function initPaymentsAdmin() {
    let currentPage = 0;
    let rows = [];

    function renderPaymentDetails(payment) {
      ui.$("#paymentResult").innerHTML = `<div class="card"><h3>Payment #${payment.paymentId}</h3><div class="detail-grid">
        <div class="detail"><span>Booking</span><strong>${ui.escapeHtml(payment.bookingReference)}</strong></div>
        <div class="detail"><span>Method</span><strong>${ui.escapeHtml(payment.paymentMethod)}</strong></div>
        <div class="detail"><span>Reference</span><strong>${ui.escapeHtml(payment.transactionReference)}</strong></div>
        <div class="detail"><span>Date</span><strong>${ui.datetime(payment.paymentDate)}</strong></div>
        <div class="detail"><span>Amount</span><strong>${ui.money(payment.amount)}</strong></div>
        <div class="detail"><span>Status</span>${ui.badge(payment.paymentStatus)}</div>
      </div><div class="actions" style="margin-top:16px"><button class="btn btn-danger" id="refundBtn" ${payment.paymentStatus === "SUCCESS" ? "" : "disabled"}>Refund</button></div></div>`;
      ui.$("#refundBtn").addEventListener("click", async () => {
        if (!confirm("Refund this successful payment?")) return;
        try {
          const refunded = await window.Api.post(`/payments/${payment.paymentId}/refund`, null);
          renderPaymentDetails(refunded);
          ui.showAlert("#pageAlert", "success", "Refund processed.");
          load(currentPage);
        } catch (error) {
          ui.showAlert("#pageAlert", "error", ui.errorMessage(error));
        }
      });
    }

    async function load(page) {
      currentPage = page || 0;
      const data = await window.Api.get("/payments", {
        keyword: ui.$("#paymentKeyword").value.trim(),
        status: ui.$("#paymentStatusFilter").value,
        page: currentPage,
        size: pageSize()
      });
      rows = data.content || [];
      if (!rows.length) {
        ui.$("#adminTable").innerHTML = `<div class="empty-state">No payments found.</div>`;
      } else {
        ui.$("#adminTable").innerHTML = `<div class="table-wrap card flush"><table>
          <thead><tr><th>ID</th><th>Booking</th><th>Method</th><th>Reference</th><th>Date</th><th>Amount</th><th>Status</th><th>Actions</th></tr></thead>
          <tbody>${rows.map((payment) => `<tr>
            <td>#${payment.paymentId}</td>
            <td>${ui.escapeHtml(payment.bookingReference)}</td>
            <td>${ui.escapeHtml(payment.paymentMethod)}</td>
            <td>${ui.escapeHtml(payment.transactionReference)}</td>
            <td>${ui.datetime(payment.paymentDate)}</td>
            <td>${ui.money(payment.amount)}</td>
            <td>${ui.badge(payment.paymentStatus)}</td>
            <td><div class="actions">
              <button class="btn btn-small btn-ghost" data-view-payment="${payment.paymentId}" type="button">View</button>
              <button class="btn btn-small btn-danger" data-refund-payment="${payment.paymentId}" type="button" ${payment.paymentStatus === "SUCCESS" ? "" : "disabled"}>Refund</button>
            </div></td>
          </tr>`).join("")}</tbody></table></div>`;
        ui.$all("[data-view-payment]").forEach((button) => {
          button.addEventListener("click", () => {
            const payment = rows.find((item) => String(item.paymentId) === button.dataset.viewPayment);
            if (payment) renderPaymentDetails(payment);
          });
        });
        ui.$all("[data-refund-payment]").forEach((button) => {
          button.addEventListener("click", async () => {
            if (!confirm("Refund this successful payment?")) return;
            try {
              const refunded = await window.Api.post(`/payments/${button.dataset.refundPayment}/refund`, null);
              renderPaymentDetails(refunded);
              ui.showAlert("#pageAlert", "success", "Refund processed.");
              load(currentPage);
            } catch (error) {
              ui.showAlert("#pageAlert", "error", ui.errorMessage(error));
            }
          });
        });
      }
      ui.pagination("#adminPagination", data, load);
    }

    ui.$("#paymentStatusFilter").innerHTML = ui.selectOptions(cfg.paymentStatuses, "", "All statuses");
    ui.$("#paymentKeyword").addEventListener("input", () => load(0));
    ui.$("#paymentStatusFilter").addEventListener("change", () => load(0));
    ui.$("#refreshPaymentsBtn").addEventListener("click", () => load(currentPage));
    load(0).catch((error) => ui.showAlert("#pageAlert", "error", ui.errorMessage(error)));

    ui.$("#paymentVerifyForm").addEventListener("submit", async (event) => {
      event.preventDefault();
      const id = ui.formData(event.currentTarget).paymentId;
      try {
        const payment = await window.Api.get(`/payments/${id}/verify`);
        renderPaymentDetails(payment);
      } catch (error) {
        ui.showAlert("#pageAlert", "error", ui.errorMessage(error));
      }
    });
    ui.$("#receiptLookupForm").addEventListener("submit", async (event) => {
      event.preventDefault();
      const id = ui.formData(event.currentTarget).bookingId;
      try {
        const receipt = await window.Api.get(`/payments/bookings/${id}/receipt`);
        const passengerDetails = (receipt.passengerDetails || []).map((passenger) => `Seat ${ui.escapeHtml(passenger.seatNumber)}: ${ui.escapeHtml(passenger.fullName)}`).join("<br>");
        ui.$("#receiptResult").innerHTML = `<div class="card"><h3>${ui.escapeHtml(receipt.receiptNumber)}</h3><div class="detail-grid">
          <div class="detail"><span>Booking</span><strong>${ui.escapeHtml(receipt.bookingReference)}</strong></div>
          <div class="detail"><span>Passenger</span><strong>${ui.escapeHtml(receipt.passengerName)}</strong></div>
          <div class="detail"><span>Route</span><strong>${ui.escapeHtml(receipt.route)}</strong></div>
          <div class="detail"><span>Seats</span><strong>${ui.escapeHtml(ui.seatsLabel(receipt.seats))}</strong></div>
          ${passengerDetails ? `<div class="detail"><span>Seat passengers</span><strong>${passengerDetails}</strong></div>` : ""}
          <div class="detail"><span>Amount</span><strong>${ui.money(receipt.amount)}</strong></div>
          <div class="detail"><span>Transaction</span><strong>${ui.escapeHtml(receipt.transactionReference)}</strong></div>
        </div></div>`;
      } catch (error) {
        ui.showAlert("#pageAlert", "error", ui.errorMessage(error));
      }
    });
  }

  async function initSupportAdmin() {
    async function load(page) {
      const data = await window.Api.get("/support-tickets", {
        status: ui.$("#statusFilter").value,
        unassigned: ui.$("#unassignedFilter").value,
        keyword: ui.$("#ticketKeyword").value.trim(),
        page: page || 0,
        size: pageSize()
      });
      if (!data.content.length) {
        ui.$("#adminTable").innerHTML = `<div class="empty-state">No support tickets found.</div>`;
      } else {
        ui.$("#adminTable").innerHTML = `<div class="table-wrap card flush"><table>
          <thead><tr><th>ID</th><th>Subject</th><th>Description</th><th>Status</th><th>Assign staff ID</th><th>Response</th><th>Actions</th></tr></thead>
          <tbody>${data.content.map((t) => `<tr>
            <td>#${t.ticketId}</td>
            <td>${ui.escapeHtml(t.subject)}</td>
            <td><div style="white-space:pre-wrap">${ui.escapeHtml(t.description)}</div></td>
            <td><select data-status="${t.ticketId}">${cfg.supportStatuses.map((s) => `<option value="${s}"${s === t.status ? " selected" : ""}>${ui.formatStatus(s)}</option>`).join("")}</select></td>
            <td><input data-staff="${t.ticketId}" type="number" min="1" value="${t.handledByStaffId || ""}" placeholder="Current staff"></td>
            <td><textarea data-response="${t.ticketId}" placeholder="Staff response"></textarea></td>
            <td><button class="btn btn-small btn-primary" data-respond="${t.ticketId}" type="button">Save response</button></td>
          </tr>`).join("")}</tbody></table></div>`;
        ui.$all("[data-respond]").forEach((button) => {
          button.addEventListener("click", async () => {
            const id = button.dataset.respond;
            try {
              await window.Api.patch(`/support-tickets/${id}/response`, {
                response: ui.$(`[data-response="${id}"]`).value.trim(),
                status: ui.$(`[data-status="${id}"]`).value,
                handledByStaffId: ui.$(`[data-staff="${id}"]`).value ? Number(ui.$(`[data-staff="${id}"]`).value) : null
              });
              ui.showAlert("#pageAlert", "success", "Ticket updated.");
              load(page || 0);
            } catch (error) {
              ui.showAlert("#pageAlert", "error", ui.errorMessage(error));
            }
          });
        });
      }
      ui.pagination("#adminPagination", data, load);
    }
    ui.$("#statusFilter").innerHTML = ui.selectOptions(cfg.supportStatuses, "", "All statuses");
    ["statusFilter", "unassignedFilter"].forEach((id) => ui.$("#" + id).addEventListener("change", () => load(0)));
    ui.$("#ticketKeyword").addEventListener("input", () => load(0));
    load(0).catch((error) => ui.showAlert("#pageAlert", "error", ui.errorMessage(error)));
  }

  async function initNotificationsAdmin() {
    const categories = ["BOOKING_UPDATES", "PAYMENT_UPDATES", "SCHEDULE_CHANGES", "TRIP_REMINDERS", "WALLET_UPDATES", "SUPPORT_MESSAGES", "SECURITY_ALERTS", "PROMOTIONAL", "ADMIN"];
    const types = ["ADMIN_BROADCAST", "SCHEDULE_UPDATED", "SCHEDULE_DELAYED", "PAYMENT_SUCCESS", "PAYMENT_FAILED", "REFUND_COMPLETED", "BOOKING_REMINDER", "SUPPORT_REPLY", "WALLET_REFUND"];
    const targets = ["ALL_PASSENGERS", "PASSENGER", "SELECTED_PASSENGERS", "SCHEDULE", "ROUTE"];
    const targetLabels = {
      ALL_PASSENGERS: "All Passengers",
      PASSENGER: "Single Passenger",
      SELECTED_PASSENGERS: "Selected Passengers",
      SCHEDULE: "Schedule Passengers",
      ROUTE: "Route Passengers"
    };
    const targetPanel = ui.$("#notificationTargetPanel");
    const targetError = ui.$("#notificationTargetError");
    const sendButton = ui.$("#sendNotificationButton");
    const recipientState = {
      passengerOptions: [],
      scheduleOptions: [],
      routeOptions: [],
      passengersLoaded: false,
      schedulesLoaded: false,
      routesLoaded: false,
      loading: null,
      query: "",
      selectedPassenger: null,
      selectedPassengers: [],
      selectedSchedule: null,
      selectedRoute: null
    };
    let currentPage = 0;

    ui.$('[name="notificationType"]').innerHTML = ui.selectOptions(types, "ADMIN_BROADCAST");
    ui.$('[name="targetType"]').innerHTML = targets.map((target) => `<option value="${target}">${ui.escapeHtml(targetLabels[target])}</option>`).join("");
    ui.$('[name="category"]').innerHTML = ui.selectOptions(categories, "ADMIN");

    function optionText(parts) {
      return parts.filter((part) => part !== undefined && part !== null && String(part).trim() !== "").join(" ");
    }

    function passengerName(passenger) {
      return optionText([passenger.firstName, passenger.lastName]) || passenger.email || "Passenger";
    }

    function passengerSearchText(passenger) {
      return optionText([passengerName(passenger), passenger.email, passenger.phone, passenger.passengerId, passenger.userId]).toLowerCase();
    }

    function scheduleRoute(schedule) {
      return schedule.route ? `${schedule.route.origin} \u2192 ${schedule.route.destination}` : "Route unavailable";
    }

    function scheduleLabel(schedule) {
      const bus = schedule.bus && schedule.bus.registrationNo ? `Bus ${schedule.bus.registrationNo}` : "Bus unassigned";
      return `${scheduleRoute(schedule)} | ${ui.date(schedule.travelDate)} | ${ui.time(schedule.departureTime)} | ${bus}`;
    }

    function scheduleSearchText(schedule) {
      const bus = schedule.bus && schedule.bus.registrationNo ? schedule.bus.registrationNo : "";
      return optionText([schedule.scheduleId, scheduleRoute(schedule), schedule.travelDate, schedule.departureTime, bus]).toLowerCase();
    }

    function routeLabel(route) {
      return `${route.origin} \u2192 ${route.destination}`;
    }

    function routeSearchText(route) {
      return optionText([route.routeId, route.origin, route.destination, routeLabel(route)]).toLowerCase();
    }

    function filterOptions(options, query, textFactory, limit) {
      const needle = String(query || "").trim().toLowerCase();
      const rows = needle ? options.filter((option) => textFactory(option).includes(needle)) : options;
      return rows.slice(0, limit || 8);
    }

    function syncHiddenRecipientFields() {
      const targetType = ui.$('[name="targetType"]').value;
      const targetIdInput = ui.$('[name="targetId"]');
      const targetIdsInput = ui.$('[name="targetIds"]');
      targetIdInput.value = "";
      targetIdsInput.value = "";
      if (targetType === "PASSENGER" && recipientState.selectedPassenger) {
        targetIdInput.value = recipientState.selectedPassenger.passengerId;
      } else if (targetType === "SELECTED_PASSENGERS" && recipientState.selectedPassengers.length) {
        targetIdsInput.value = recipientState.selectedPassengers.map((passenger) => passenger.passengerId).join(",");
      } else if (targetType === "SCHEDULE" && recipientState.selectedSchedule) {
        targetIdInput.value = recipientState.selectedSchedule.scheduleId;
      } else if (targetType === "ROUTE" && recipientState.selectedRoute) {
        targetIdInput.value = recipientState.selectedRoute.routeId;
      }
    }

    function clearTargetValues() {
      recipientState.query = "";
      recipientState.selectedPassenger = null;
      recipientState.selectedPassengers = [];
      recipientState.selectedSchedule = null;
      recipientState.selectedRoute = null;
      syncHiddenRecipientFields();
    }

    function validateRecipient(showMessage) {
      const targetType = ui.$('[name="targetType"]').value;
      let message = "";
      if (targetType === "PASSENGER" && !recipientState.selectedPassenger) {
        message = "Select one passenger.";
      } else if (targetType === "SELECTED_PASSENGERS" && recipientState.selectedPassengers.length === 0) {
        message = "Select at least one passenger.";
      } else if (targetType === "SCHEDULE" && !recipientState.selectedSchedule) {
        message = "Select one schedule.";
      } else if (targetType === "ROUTE" && !recipientState.selectedRoute) {
        message = "Select one route.";
      }
      if (targetError) targetError.textContent = showMessage || message ? message : "";
      if (sendButton) sendButton.disabled = Boolean(message);
      return !message;
    }

    function selectedPassengerHtml(passenger, removable) {
      return `<span class="recipient-chip">
        <span>${ui.escapeHtml(passengerName(passenger))}</span>
        <small>${ui.escapeHtml(passenger.email || ("Passenger #" + passenger.passengerId))}</small>
        ${removable ? `<button type="button" data-remove-passenger="${passenger.passengerId}" aria-label="Remove ${ui.escapeHtml(passengerName(passenger))}">x</button>` : ""}
      </span>`;
    }

    function resultButtonHtml(kind, id, title, meta) {
      return `<button class="recipient-result" type="button" data-select-${kind}="${ui.escapeHtml(id)}">
        <strong>${ui.escapeHtml(title)}</strong>
        ${meta ? `<span>${ui.escapeHtml(meta)}</span>` : ""}
      </button>`;
    }

    function renderPassengerResults(multiple) {
      if (recipientState.loading === "passengers") return `<div class="recipient-empty"><span class="spinner" aria-hidden="true"></span>Loading passengers...</div>`;
      if (!recipientState.passengersLoaded) return `<div class="recipient-empty">Preparing passenger search...</div>`;
      const selectedIds = new Set(recipientState.selectedPassengers.map((passenger) => passenger.passengerId));
      const rows = filterOptions(recipientState.passengerOptions, recipientState.query, passengerSearchText, 8)
        .filter((passenger) => !multiple || !selectedIds.has(passenger.passengerId));
      if (!rows.length) return `<div class="recipient-empty">No results found.</div>`;
      return rows.map((passenger) => resultButtonHtml(
        "passenger",
        passenger.passengerId,
        passengerName(passenger),
        [passenger.email, passenger.phone, `Passenger #${passenger.passengerId}`].filter(Boolean).join(" | ")
      )).join("");
    }

    function renderScheduleResults() {
      if (recipientState.loading === "schedules") return `<div class="recipient-empty"><span class="spinner" aria-hidden="true"></span>Loading schedules...</div>`;
      if (!recipientState.schedulesLoaded) return `<div class="recipient-empty">Preparing schedule search...</div>`;
      const rows = filterOptions(recipientState.scheduleOptions, recipientState.query, scheduleSearchText, 8);
      if (!rows.length) return `<div class="recipient-empty">No results found.</div>`;
      return rows.map((schedule) => resultButtonHtml("schedule", schedule.scheduleId, scheduleLabel(schedule), `Schedule #${schedule.scheduleId}`)).join("");
    }

    function renderRouteResults() {
      if (recipientState.loading === "routes") return `<div class="recipient-empty"><span class="spinner" aria-hidden="true"></span>Loading routes...</div>`;
      if (!recipientState.routesLoaded) return `<div class="recipient-empty">Preparing route search...</div>`;
      const rows = filterOptions(recipientState.routeOptions, recipientState.query, routeSearchText, 8);
      if (!rows.length) return `<div class="recipient-empty">No results found.</div>`;
      return rows.map((route) => resultButtonHtml("route", route.routeId, routeLabel(route), `Route #${route.routeId}`)).join("");
    }

    function renderTargetPanel() {
      const targetType = ui.$('[name="targetType"]').value;
      targetPanel.className = `target-panel target-panel-${targetType.toLowerCase()}`;
      if (targetType === "ALL_PASSENGERS") {
        targetPanel.innerHTML = `<div class="target-info" role="note">This notification will be sent to all active passengers.</div>`;
      } else if (targetType === "PASSENGER") {
        targetPanel.innerHTML = `
          <label class="recipient-label">Select Passenger
            <input class="recipient-search-input" autocomplete="off" value="${ui.escapeHtml(recipientState.query)}" placeholder="Search by name, email, phone or ID...">
          </label>
          <div class="recipient-results">${renderPassengerResults(false)}</div>
          ${recipientState.selectedPassenger ? `<div class="recipient-selected"><span>Selected:</span>${selectedPassengerHtml(recipientState.selectedPassenger, false)}</div>` : ""}
        `;
      } else if (targetType === "SELECTED_PASSENGERS") {
        targetPanel.innerHTML = `
          <label class="recipient-label">Select Passengers
            <input class="recipient-search-input" autocomplete="off" value="${ui.escapeHtml(recipientState.query)}" placeholder="Search passengers...">
          </label>
          <div class="recipient-chip-list">${recipientState.selectedPassengers.map((passenger) => selectedPassengerHtml(passenger, true)).join("")}</div>
          <div class="recipient-results">${renderPassengerResults(true)}</div>
        `;
      } else if (targetType === "SCHEDULE") {
        targetPanel.innerHTML = `
          <label class="recipient-label">Select Schedule
            <input class="recipient-search-input" autocomplete="off" value="${ui.escapeHtml(recipientState.query)}" placeholder="Search schedules...">
          </label>
          <div class="recipient-results">${renderScheduleResults()}</div>
          ${recipientState.selectedSchedule ? `<div class="recipient-selected"><span>Selected:</span><strong>${ui.escapeHtml(scheduleLabel(recipientState.selectedSchedule))}</strong></div>` : ""}
        `;
      } else if (targetType === "ROUTE") {
        targetPanel.innerHTML = `
          <label class="recipient-label">Select Route
            <input class="recipient-search-input" autocomplete="off" value="${ui.escapeHtml(recipientState.query)}" placeholder="Search routes...">
          </label>
          <div class="recipient-results">${renderRouteResults()}</div>
          ${recipientState.selectedRoute ? `<div class="recipient-selected"><span>Selected:</span><strong>${ui.escapeHtml(routeLabel(recipientState.selectedRoute))}</strong></div>` : ""}
        `;
      }
      syncHiddenRecipientFields();
      validateRecipient(false);
    }

    async function loadPassengerOptions() {
      if (recipientState.passengersLoaded || recipientState.loading === "passengers") return;
      recipientState.loading = "passengers";
      renderTargetPanel();
      try {
        const data = await window.Api.get("/users", { role: "PASSENGER", active: true, page: 0, size: 1000 });
        recipientState.passengerOptions = (data.content || []).filter((user) => user.passengerId);
        recipientState.passengersLoaded = true;
      } finally {
        recipientState.loading = null;
        renderTargetPanel();
      }
    }

    async function loadScheduleOptions() {
      if (recipientState.schedulesLoaded || recipientState.loading === "schedules") return;
      recipientState.loading = "schedules";
      renderTargetPanel();
      try {
        const data = await window.Api.get("/schedules", { page: 0, size: 1000 });
        recipientState.scheduleOptions = data.content || [];
        recipientState.schedulesLoaded = true;
      } finally {
        recipientState.loading = null;
        renderTargetPanel();
      }
    }

    async function loadRouteOptions() {
      if (recipientState.routesLoaded || recipientState.loading === "routes") return;
      recipientState.loading = "routes";
      renderTargetPanel();
      try {
        const data = await window.Api.get("/routes", { active: true, page: 0, size: 1000 });
        recipientState.routeOptions = data.content || [];
        recipientState.routesLoaded = true;
      } finally {
        recipientState.loading = null;
        renderTargetPanel();
      }
    }

    function loadOptionsForTarget() {
      const targetType = ui.$('[name="targetType"]').value;
      const loader = targetType === "PASSENGER" || targetType === "SELECTED_PASSENGERS"
        ? loadPassengerOptions
        : targetType === "SCHEDULE"
          ? loadScheduleOptions
          : targetType === "ROUTE"
            ? loadRouteOptions
            : null;
      if (loader) loader().catch((error) => ui.showAlert("#pageAlert", "error", ui.errorMessage(error)));
    }

    async function load(page) {
      currentPage = page || 0;
      const data = await window.Api.get("/notifications", {
        status: ui.$("#notificationStatusFilter").value,
        page: currentPage,
        size: pageSize()
      });
      ui.$("#adminTable").innerHTML = data.content && data.content.length ? `<div class="table-wrap card flush"><table>
        <thead><tr><th>Title</th><th>Type</th><th>User</th><th>Channels</th><th>Status</th><th>Sent</th></tr></thead>
        <tbody>${data.content.map((n) => `<tr>
          <td><strong>${ui.escapeHtml(n.title || "Notification")}</strong><br><span class="muted">${ui.escapeHtml(n.message || "")}</span></td>
          <td>${ui.escapeHtml(ui.formatStatus(n.notificationType))}</td>
          <td>#${ui.escapeHtml(n.userId)}</td>
          <td>${ui.escapeHtml(n.deliveryChannels || "IN_APP")}</td>
          <td>${ui.badge(n.status)}</td>
          <td>${ui.datetime(n.sentDateTime)}</td>
        </tr>`).join("")}</tbody></table></div>` : `<div class="empty-state">No notifications found.</div>`;
      ui.pagination("#adminPagination", data, load);
    }

    function payloadFrom(form) {
      const data = ui.formData(form);
      const payload = {
        title: data.title.trim(),
        message: data.message.trim(),
        notificationType: data.notificationType,
        targetType: data.targetType,
        targetId: null,
        targetIds: [],
        category: data.category,
        priority: data.priority,
        actionUrl: data.actionUrl.trim() || null
      };
      if (data.targetType === "PASSENGER" && recipientState.selectedPassenger) {
        payload.targetId = Number(recipientState.selectedPassenger.passengerId);
      } else if (data.targetType === "SELECTED_PASSENGERS") {
        payload.targetIds = recipientState.selectedPassengers.map((passenger) => Number(passenger.passengerId)).filter(Boolean);
      } else if (data.targetType === "SCHEDULE" && recipientState.selectedSchedule) {
        payload.targetId = Number(recipientState.selectedSchedule.scheduleId);
      } else if (data.targetType === "ROUTE" && recipientState.selectedRoute) {
        payload.targetId = Number(recipientState.selectedRoute.routeId);
      }
      return payload;
    }

    targetPanel.addEventListener("input", (event) => {
      if (!event.target.classList.contains("recipient-search-input")) return;
      recipientState.query = event.target.value;
      renderTargetPanel();
      const input = targetPanel.querySelector(".recipient-search-input");
      if (input) {
        input.focus();
        input.setSelectionRange(input.value.length, input.value.length);
      }
    });

    targetPanel.addEventListener("focusin", (event) => {
      if (event.target.classList.contains("recipient-search-input")) loadOptionsForTarget();
    });

    targetPanel.addEventListener("click", (event) => {
      const passengerButton = event.target.closest("[data-select-passenger]");
      const scheduleButton = event.target.closest("[data-select-schedule]");
      const routeButton = event.target.closest("[data-select-route]");
      const removePassengerButton = event.target.closest("[data-remove-passenger]");
      if (passengerButton) {
        const id = Number(passengerButton.dataset.selectPassenger);
        const passenger = recipientState.passengerOptions.find((item) => Number(item.passengerId) === id);
        if (passenger) {
          if (ui.$('[name="targetType"]').value === "SELECTED_PASSENGERS") {
            if (!recipientState.selectedPassengers.some((item) => Number(item.passengerId) === id)) {
              recipientState.selectedPassengers.push(passenger);
            }
          } else {
            recipientState.selectedPassenger = passenger;
          }
          recipientState.query = "";
          renderTargetPanel();
        }
      } else if (scheduleButton) {
        const id = Number(scheduleButton.dataset.selectSchedule);
        recipientState.selectedSchedule = recipientState.scheduleOptions.find((item) => Number(item.scheduleId) === id) || null;
        recipientState.query = "";
        renderTargetPanel();
      } else if (routeButton) {
        const id = Number(routeButton.dataset.selectRoute);
        recipientState.selectedRoute = recipientState.routeOptions.find((item) => Number(item.routeId) === id) || null;
        recipientState.query = "";
        renderTargetPanel();
      } else if (removePassengerButton) {
        const id = Number(removePassengerButton.dataset.removePassenger);
        recipientState.selectedPassengers = recipientState.selectedPassengers.filter((passenger) => Number(passenger.passengerId) !== id);
        renderTargetPanel();
      }
    });

    ui.$('[name="targetType"]').addEventListener("change", () => {
      clearTargetValues();
      renderTargetPanel();
      loadOptionsForTarget();
    });

    renderTargetPanel();

    ui.$("#manualNotificationForm").addEventListener("submit", async (event) => {
      event.preventDefault();
      if (!validateRecipient(true)) return;
      const form = event.currentTarget;
      const button = form.querySelector('button[type="submit"]');
      try {
        ui.setLoading(button, true, "Sending");
        const result = await window.Api.post("/notifications/manual", payloadFrom(form));
        ui.showAlert("#pageAlert", "success", `Notification sent to ${result.totalElements || 0} passenger account(s).`);
        form.reset();
        clearTargetValues();
        ui.$('[name="notificationType"]').value = "ADMIN_BROADCAST";
        ui.$('[name="targetType"]').value = "ALL_PASSENGERS";
        ui.$('[name="category"]').value = "ADMIN";
        renderTargetPanel();
        load(currentPage);
      } catch (error) {
        ui.showAlert("#pageAlert", "error", ui.errorMessage(error));
      } finally {
        ui.setLoading(button, false);
        validateRecipient(false);
      }
    });
    ui.$("#notificationStatusFilter").addEventListener("change", () => load(0));
    load(0).catch((error) => ui.showAlert("#pageAlert", "error", ui.errorMessage(error)));
  }

  async function initReviews() {
    async function load(page) {
      try {
        const data = await window.Api.get("/reviews", { page: page || 0, size: pageSize() });
        ui.$("#adminTable").innerHTML = data.content.length ? `<div class="table-wrap card flush"><table><thead><tr><th>Review</th><th>Passenger</th><th>Booking</th><th>Rating</th><th>Feedback</th><th>Date</th></tr></thead><tbody>${data.content.map((r) => `<tr><td>#${r.reviewId}</td><td>#${r.passengerId}</td><td>#${r.bookingId}</td><td>${"★".repeat(r.rating)}${"☆".repeat(5 - r.rating)}</td><td>${ui.escapeHtml(r.comment || "No comment")}</td><td>${ui.datetime(r.reviewDate)}</td></tr>`).join("")}</tbody></table></div>` : '<div class="empty-state">No reviews found.</div>';
        ui.pagination("#adminPagination", data, load);
      } catch (error) { ui.showAlert("#pageAlert", "error", ui.errorMessage(error)); }
    }
    load(0);
  }

  async function initReports() {
    async function downloadCsv(path, params, filename, button) {
      const query = new URLSearchParams();
      Object.entries(params || {}).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== "") query.set(key, value);
      });
      const url = `${cfg.apiBase}${path}${query.toString() ? "?" + query.toString() : ""}`;
      try {
        if (button) ui.setLoading(button, true, "Exporting");
        const response = await fetch(url, { headers: { Authorization: "Bearer " + window.Auth.getToken() } });
        if (!response.ok) throw new Error("Report export failed.");
        const blob = await response.blob();
        const objectUrl = URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = objectUrl;
        link.download = filename;
        document.body.appendChild(link);
        link.click();
        link.remove();
        URL.revokeObjectURL(objectUrl);
      } catch (error) {
        ui.showAlert("#pageAlert", "error", ui.errorMessage(error));
      } finally {
        if (button) ui.setLoading(button, false);
      }
    }

    function paramsFromForm(form, dateTime) {
      const data = ui.formData(form);
      const params = {};
      ["from", "to"].forEach((field) => {
        if (data[field]) params[field] = dateTime ? new Date(data[field]).toISOString() : data[field];
      });
      ["route", "travelDate"].forEach((field) => {
        if (data[field]) params[field] = data[field];
      });
      ["routeId", "scheduleId"].forEach((field) => {
        if (data[field]) params[field] = Number(data[field]);
      });
      return params;
    }

    function metricCards(items) {
      return `<div class="summary-grid">${items.map((item) => `<div class="card metric"><span>${ui.escapeHtml(item.label)}</span><strong>${ui.escapeHtml(item.value)}</strong></div>`).join("")}</div>`;
    }

    function reportTable(headers, rows, empty) {
      if (!rows.length) return `<div class="empty-state">${ui.escapeHtml(empty || "No report data found.")}</div>`;
      return `<div class="table-wrap"><table><thead><tr>${headers.map((h) => `<th>${ui.escapeHtml(h)}</th>`).join("")}</tr></thead><tbody>${rows.join("")}</tbody></table></div>`;
    }

    function renderPaymentMethods(rows) {
      return reportTable(["Payment method", "Revenue", "Successful payments"], (rows || []).map((row) => `<tr><td>${ui.escapeHtml(row.paymentMethod)}</td><td>${ui.money(row.revenue)}</td><td>${row.successfulPayments}</td></tr>`), "No payment method revenue found.");
    }

    function renderTicketSales(report) {
      const rows = (report.schedules && report.schedules.content) || [];
      ui.$("#ticketSalesReport").innerHTML = `
        ${metricCards([
          { label: "Tickets sold", value: report.summary.totalTicketsSold },
          { label: "Bookings represented", value: report.summary.totalBookings },
          { label: "Ticket sales amount", value: ui.money(report.summary.totalTicketSalesAmount) }
        ])}
        ${reportTable(["Schedule", "Route", "Travel date", "Departure", "Bus", "Capacity", "Sold", "Remaining", "Occupancy", "Amount"], rows.map((row) => `<tr>
          <td>#${row.scheduleId}</td>
          <td>${ui.escapeHtml(row.route)}</td>
          <td>${ui.date(row.travelDate)}</td>
          <td>${ui.time(row.departureTime)}</td>
          <td>${ui.escapeHtml(row.busRegistrationNo || "-")}</td>
          <td>${row.busCapacity}</td>
          <td>${row.ticketsSold}</td>
          <td>${row.remainingSeats}</td>
          <td>${row.occupancyPercent}%</td>
          <td>${ui.money(row.ticketSalesAmount)}</td>
        </tr>`), "No ticket sales found for this range.")}
      `;
    }

    function renderOccupancyDetails(report) {
      const rows = (report.schedules && report.schedules.content) || [];
      ui.$("#occupancyDetailsReport").innerHTML = `
        ${metricCards([
          { label: "Schedules", value: report.summary.schedules },
          { label: "Average occupancy", value: report.summary.averageOccupancyPercent + "%" }
        ])}
        ${reportTable(["Schedule", "Route", "Travel date", "Departure", "Bus", "Capacity", "Occupied", "Available", "Occupancy"], rows.map((row) => `<tr>
          <td>#${row.scheduleId}</td>
          <td>${ui.escapeHtml(row.route)}</td>
          <td>${ui.date(row.travelDate)}</td>
          <td>${ui.time(row.departureTime)}</td>
          <td>${ui.escapeHtml(row.busRegistrationNo || "-")}</td>
          <td>${row.busCapacity}</td>
          <td>${row.occupiedSeats}</td>
          <td>${row.availableSeats}</td>
          <td>${row.occupancyPercent}%</td>
        </tr>`), "No occupancy data found for this range.")}
      `;
    }

    function renderManagement(report) {
      ui.$("#managementReport").innerHTML = `
        ${metricCards([
          { label: "Total bookings", value: report.totalBookings },
          { label: "Pending payment", value: report.pendingPaymentBookings },
          { label: "Confirmed", value: report.confirmedBookings },
          { label: "Completed", value: report.completedBookings },
          { label: "Cancelled", value: report.cancelledBookings },
          { label: "Tickets sold", value: report.totalTicketsSold },
          { label: "Schedules", value: report.totalSchedules },
          { label: "Average occupancy", value: report.averageOccupancyPercent + "%" },
          { label: "Successful revenue", value: ui.money(report.successfulRevenue) }
        ])}
        ${reportTable(["Route", "Bookings", "Tickets sold", "Revenue", "Avg occupancy"], (report.routePerformance || []).map((row) => `<tr>
          <td>${ui.escapeHtml(row.route)}</td>
          <td>${row.bookings}</td>
          <td>${row.ticketsSold}</td>
          <td>${ui.money(row.revenue)}</td>
          <td>${row.averageOccupancyPercent}%</td>
        </tr>`), "No route performance data found.")}
      `;
    }

    function renderFinancial(report) {
      ui.$("#financialReport").innerHTML = `
        ${metricCards([
          { label: "Successful revenue", value: ui.money(report.successfulRevenue) },
          { label: "Successful payments", value: report.successfulPaymentCount },
          { label: "Pending payments", value: report.pendingPaymentCount },
          { label: "Failed payments", value: report.failedPaymentCount },
          { label: "Refunded payments", value: report.refundedPaymentCount },
          { label: "Refunded amount", value: ui.money(report.refundedAmount) }
        ])}
        ${renderPaymentMethods(report.revenueByPaymentMethod)}
      `;
    }

    const exportButton = ui.$("#exportSummaryBtn");
    if (exportButton) exportButton.addEventListener("click", () => downloadCsv("/reports/export/summary.csv", {}, "smartroute-report-summary.csv", exportButton));

    async function loadReports() {
      try {
        const [revenue, revenueDetails, bookings, payments, users, cancelled, sales, occupancy] = await Promise.all([
          window.Api.get("/reports/revenue/total"),
          window.Api.get("/reports/revenue/details"),
          window.Api.get("/reports/bookings/count"),
          window.Api.get("/reports/payments"),
          window.Api.get("/reports/users"),
          window.Api.get("/reports/cancelled-bookings"),
          window.Api.get("/reports/ticket-sales"),
          window.Api.get("/reports/schedule-occupancy", { size: 10 })
        ]);
        ui.$("#reportsSummary").innerHTML = `
          <div class="summary-grid">
            <div class="card metric"><span>Total revenue</span><strong>${ui.money(revenue.totalRevenue)}</strong></div>
            <div class="card metric"><span>Total bookings</span><strong>${bookings.totalBookings}</strong></div>
            <div class="card metric"><span>Successful payments</span><strong>${payments.successful}</strong></div>
            <div class="card metric"><span>Users</span><strong>${users.totalUsers}</strong></div>
            <div class="card metric"><span>Cancelled bookings</span><strong>${cancelled.cancelledBookings}</strong></div>
            <div class="card metric"><span>Reserved seats</span><strong>${sales.activeReservedSeats}</strong></div>
            <div class="card metric"><span>Refunded amount</span><strong>${ui.money(revenueDetails.refundedAmount)}</strong></div>
            <div class="card metric"><span>Refunded transactions</span><strong>${revenueDetails.refundedTransactions}</strong></div>
          </div>`;
        const max = Math.max(...(occupancy.content || []).map((row) => row.occupancyPercent), 1);
        ui.$("#occupancyReport").innerHTML = `<div class="card"><h3>Schedule occupancy</h3><div class="bar-list">
          ${(occupancy.content || []).map((row) => `<div class="bar-row">
            <strong>${ui.escapeHtml(row.route)} (${ui.date(row.travelDate)} ${ui.time(row.departureTime)})</strong>
            <div class="bar-track"><div class="bar-fill" style="width:${Math.min(100, (row.occupancyPercent / max) * 100)}%"></div></div>
            <span class="muted">${row.reservedSeats}/${row.capacity} seats, ${row.occupancyPercent}%</span>
          </div>`).join("") || `<div class="empty-state">No occupancy data.</div>`}
        </div></div>`;
      } catch (error) {
        ui.showAlert("#pageAlert", "error", ui.errorMessage(error));
      }
    }

    async function runTicketSales(params) {
      ui.$("#ticketSalesReport").innerHTML = `<div class="empty-state">Loading ticket sales report...</div>`;
      renderTicketSales(await window.Api.get("/reports/ticket-sales/details", { ...params, size: 25 }));
    }

    async function runOccupancyDetails(params) {
      ui.$("#occupancyDetailsReport").innerHTML = `<div class="empty-state">Loading occupancy report...</div>`;
      renderOccupancyDetails(await window.Api.get("/reports/schedule-occupancy/details", { ...params, size: 25 }));
    }

    async function runManagement(params) {
      ui.$("#managementReport").innerHTML = `<div class="empty-state">Loading management report...</div>`;
      renderManagement(await window.Api.get("/reports/management", params));
    }

    async function runFinancial(params) {
      ui.$("#financialReport").innerHTML = `<div class="empty-state">Loading financial report...</div>`;
      renderFinancial(await window.Api.get("/reports/financial", params));
    }

    ui.$("#revenueRangeForm").addEventListener("submit", async (event) => {
      event.preventDefault();
      const d = ui.formData(event.currentTarget);
      try {
        const report = await window.Api.get("/reports/revenue", {
          from: new Date(d.from).toISOString(),
          to: new Date(d.to).toISOString()
        });
        ui.$("#rangeRevenue").innerHTML = `<div class="alert alert-info">Revenue in range: ${ui.money(report.totalRevenue)} from ${report.successfulPayments} successful payments.</div>`;
      } catch (error) {
        ui.showAlert("#pageAlert", "error", ui.errorMessage(error));
      }
    });
    ui.$("#ticketSalesForm").addEventListener("submit", async (event) => {
      event.preventDefault();
      try { await runTicketSales(paramsFromForm(event.currentTarget, false)); }
      catch (error) { ui.showAlert("#pageAlert", "error", ui.errorMessage(error)); }
    });
    ui.$("#occupancyDetailsForm").addEventListener("submit", async (event) => {
      event.preventDefault();
      try { await runOccupancyDetails(paramsFromForm(event.currentTarget, false)); }
      catch (error) { ui.showAlert("#pageAlert", "error", ui.errorMessage(error)); }
    });
    ui.$("#managementReportForm").addEventListener("submit", async (event) => {
      event.preventDefault();
      try { await runManagement(paramsFromForm(event.currentTarget, true)); }
      catch (error) { ui.showAlert("#pageAlert", "error", ui.errorMessage(error)); }
    });
    ui.$("#financialReportForm").addEventListener("submit", async (event) => {
      event.preventDefault();
      try { await runFinancial(paramsFromForm(event.currentTarget, true)); }
      catch (error) { ui.showAlert("#pageAlert", "error", ui.errorMessage(error)); }
    });
    ui.$("#exportTicketSalesBtn").addEventListener("click", () => downloadCsv("/reports/export/ticket-sales.csv", paramsFromForm(ui.$("#ticketSalesForm"), false), "smartroute-ticket-sales.csv", ui.$("#exportTicketSalesBtn")));
    ui.$("#exportManagementBtn").addEventListener("click", () => downloadCsv("/reports/export/management.csv", paramsFromForm(ui.$("#managementReportForm"), true), "smartroute-management-report.csv", ui.$("#exportManagementBtn")));
    ui.$("#exportFinancialBtn").addEventListener("click", () => downloadCsv("/reports/export/financial.csv", paramsFromForm(ui.$("#financialReportForm"), true), "smartroute-financial-report.csv", ui.$("#exportFinancialBtn")));
    loadReports();
    try {
      await Promise.all([
        runTicketSales({ size: 25 }),
        runOccupancyDetails({ size: 25 }),
        runManagement({}),
        runFinancial({})
      ]);
    } catch (error) {
      ui.showAlert("#pageAlert", "error", ui.errorMessage(error));
    }
  }

  document.addEventListener("DOMContentLoaded", () => {
    const adminPage = pageName();
    if (!adminPage) return;
    if (!window.Auth.requireAuth({ roles: cfg.staffRoles })) return;
    const init = {
      dashboard: initDashboard,
      users: initUsers,
      routes: initRoutes,
      buses: initBuses,
      drivers: initDrivers,
      schedules: initSchedules,
      bookings: initBookingsAdmin,
      payments: initPaymentsAdmin,
      "support-tickets": initSupportAdmin,
      notifications: initNotificationsAdmin,
      reviews: initReviews,
      reports: initReports
    }[adminPage];
    if (init) init();
  });
})();
