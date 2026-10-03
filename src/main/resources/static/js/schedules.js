(function () {
  const ui = window.SmartRoute;
  const cfg = window.SmartRouteConfig;

  function todayIso() {
    return new Date().toISOString().slice(0, 10);
  }

  function attachSearchForm(form) {
    if (!form) return;
    const dateInput = form.querySelector('[name="date"]');
    if (dateInput) dateInput.min = todayIso();
    form.addEventListener("submit", (event) => {
      event.preventDefault();
      const data = ui.formData(form);
      const errors = {};
      if (!ui.required(data.origin)) errors.origin = "Origin is required.";
      if (!ui.required(data.destination)) errors.destination = "Destination is required.";
      if (!ui.required(data.date)) errors.date = "Travel date is required.";
      ui.renderFieldErrors(errors);
      if (Object.keys(errors).length) return;
      window.location.assign(`/search-results.html${window.Api.toQuery({
        origin: data.origin.trim(),
        destination: data.destination.trim(),
        date: data.date
      })}`);
    });
  }

  async function availabilityFor(scheduleId) {
    try {
      return await window.Api.get(`/bookings/available-seats/${scheduleId}`, null, { redirectOnUnauthorized: false });
    } catch (error) {
      return null;
    }
  }

  async function renderSearchResults(page) {
    const params = new URLSearchParams(window.location.search);
    const origin = params.get("origin") || "";
    const destination = params.get("destination") || "";
    const date = params.get("date") || "";
    const list = ui.$("#resultsList");
    const summary = ui.$("#searchSummary");
    if (!origin || !destination || !date) {
      list.innerHTML = `<div class="empty-state">Enter an origin, destination and travel date to search schedules.</div>`;
      return;
    }
    summary.textContent = `${origin} to ${destination} on ${ui.date(date)}`;
    list.innerHTML = `<div class="empty-state"><span class="spinner"></span> Loading schedules...</div>`;
    try {
      const data = await window.Api.get("/schedules/search", {
        origin,
        destination,
        date,
        page: page || 0,
        size: cfg.defaultPageSize
      }, { redirectOnUnauthorized: false });
      if (!data.content || data.content.length === 0) {
        list.innerHTML = `<div class="empty-state">No schedules found for the selected route and date.</div>`;
        ui.pagination("#resultsPagination", data, renderSearchResults);
        return;
      }
      const availability = await Promise.all(data.content.map((schedule) => availabilityFor(schedule.scheduleId)));
      list.innerHTML = data.content.map((schedule, index) => {
        const availableCount = availability[index] ? availability[index].availableSeats.length : "Unavailable";
        return `
          <article class="card schedule-card">
            <div>
              <h2>${ui.escapeHtml(ui.routeLabel(schedule))}</h2>
              <div class="schedule-meta">
                <div class="meta-item"><span>Travel date</span><strong>${ui.date(schedule.travelDate)}</strong></div>
                <div class="meta-item"><span>Depart</span><strong>${ui.time(schedule.departureTime)}</strong></div>
                <div class="meta-item"><span>Arrive</span><strong>${ui.time(schedule.arrivalTime)}</strong></div>
                <div class="meta-item"><span>Bus type</span><strong>${ui.escapeHtml(ui.formatStatus(schedule.bus.busType))}</strong></div>
                <div class="meta-item"><span>Fare</span><strong>${ui.money(schedule.fare)}</strong></div>
              </div>
              <div class="schedule-meta">
                <div class="meta-item"><span>Available seats</span><strong>${ui.escapeHtml(availableCount)}</strong></div>
                <div class="meta-item"><span>Status</span>${ui.badge(schedule.status)}</div>
              </div>
            </div>
            <div class="actions">
              <a class="btn btn-primary" href="/seat-selection.html?scheduleId=${schedule.scheduleId}">Select seats</a>
            </div>
          </article>`;
      }).join("");
      ui.pagination("#resultsPagination", data, renderSearchResults);
    } catch (error) {
      list.innerHTML = "";
      ui.showAlert("#pageAlert", "error", ui.errorMessage(error));
    }
  }

  async function renderUpcomingSchedules() {
    const container = ui.$("#liveSchedules");
    if (!container) return;
    try {
      const data = await window.Api.get("/schedules/upcoming", { page: 0, size: 6 }, { redirectOnUnauthorized: false });
      if (!data.content || data.content.length === 0) {
        container.innerHTML = `<div class="empty-state">No upcoming schedules available.</div>`;
        return;
      }
      container.innerHTML = data.content.map((schedule) => `
        <article class="card schedule-card">
          <h3>${ui.escapeHtml(ui.routeLabel(schedule))}</h3>
          <p class="muted">${ui.date(schedule.travelDate)} · ${ui.time(schedule.departureTime)} - ${ui.time(schedule.arrivalTime)}</p>
          <p><strong>${ui.money(schedule.fare)}</strong> · ${ui.badge(schedule.status)}</p>
          <a class="btn btn-primary" href="/seat-selection.html?scheduleId=${schedule.scheduleId}">Select seats</a>
        </article>`).join("");
    } catch (error) {
      container.innerHTML = `<div class="empty-state">Live schedules are temporarily unavailable.</div>`;
    }
  }

  async function initSeatSelection() {
    if (!window.Auth.requireAuth({ roles: ["PASSENGER"] })) return;
    const scheduleId = new URLSearchParams(window.location.search).get("scheduleId");
    const container = ui.$("#seatMap");
    if (!scheduleId) {
      container.innerHTML = `<div class="empty-state">Schedule id is missing.</div>`;
      return;
    }
    const selected = new Set();

    function updateSummary(schedule) {
      const seats = Array.from(selected).sort((a, b) => a - b);
      ui.setText("#selectedSeats", seats.length ? seats.join(", ") : "None");
      ui.setText("#passengerCount", String(seats.length));
      ui.setText("#farePerSeat", ui.money(schedule.fare));
      ui.setText("#totalAmount", ui.money(Number(schedule.fare) * seats.length));
      const button = ui.$("#continueBtn");
      if (button) button.disabled = seats.length === 0;
    }

    try {
      const [schedule, available] = await Promise.all([
        window.Api.get(`/schedules/${scheduleId}`),
        window.Api.get(`/bookings/available-seats/${scheduleId}`)
      ]);
      ui.setText("#scheduleTitle", ui.routeLabel(schedule));
      ui.setText("#scheduleInfo", `${ui.date(schedule.travelDate)} at ${ui.time(schedule.departureTime)} | ${schedule.bus.registrationNo} | ${ui.formatStatus(schedule.bus.busType)}`);

      const availableNumbers = new Set((available.availableSeats || []).map((seat) => seat.seatNumber));
      const seats = [];
      for (let i = 1; i <= available.capacity; i += 1) {
        const availableSeat = availableNumbers.has(i);
        seats.push(`<button type="button" class="seat ${availableSeat ? "available" : "reserved"}" data-seat="${i}" ${availableSeat ? "" : "disabled"} aria-pressed="false" aria-label="Seat ${i} ${availableSeat ? "available" : "reserved"}">${ui.icon("seat")}<span class="seat-number">${i}</span></button>`);
      }
      container.innerHTML = seats.join("");
      ui.$all(".seat.available", container).forEach((button) => {
        button.addEventListener("click", () => {
          const seat = Number(button.dataset.seat);
          if (selected.has(seat)) {
            selected.delete(seat);
            button.classList.remove("selected");
          } else {
            selected.add(seat);
            button.classList.add("selected");
          }
          button.setAttribute("aria-pressed", String(selected.has(seat)));
          button.setAttribute("aria-label", `Seat ${seat} ${selected.has(seat) ? "selected" : "available"}`);
          updateSummary(schedule);
        });
      });
      updateSummary(schedule);
      ui.$("#continueBtn").addEventListener("click", () => {
        const seatsSelected = Array.from(selected).sort((a, b) => a - b);
        localStorage.setItem(cfg.checkoutKey, JSON.stringify({
          scheduleId: Number(scheduleId),
          schedule,
          seats: seatsSelected,
          fare: Number(schedule.fare),
          total: Number(schedule.fare) * seatsSelected.length
        }));
        window.location.assign("/checkout.html");
      });
    } catch (error) {
      container.innerHTML = "";
      ui.showAlert("#pageAlert", "error", ui.errorMessage(error));
    }
  }

  document.addEventListener("DOMContentLoaded", () => {
    const page = document.body.dataset.page;
    if (page === "home") {
      attachSearchForm(ui.$("#homeSearchForm"));
      renderUpcomingSchedules();
    }
    if (page === "search") {
      attachSearchForm(ui.$("#resultsSearchForm"));
      const params = new URLSearchParams(window.location.search);
      ["origin", "destination", "date"].forEach((name) => {
        const input = ui.$(`[name="${name}"]`);
        if (input) input.value = params.get(name) || "";
      });
      renderSearchResults(0);
    }
    if (page === "seat-selection") initSeatSelection();
  });
})();
