(function () {
  const ui = window.SmartRoute;
  const cfg = window.SmartRouteConfig;

  function checkoutState() {
    try {
      return JSON.parse(localStorage.getItem(cfg.checkoutKey));
    } catch (error) {
      return null;
    }
  }

  function bookingFuture(booking) {
    const schedule = booking.schedule;
    if (!schedule) return false;
    return new Date(`${schedule.travelDate}T${schedule.departureTime}`) > new Date();
  }

  function canCancel(booking) {
    return ["PENDING_PAYMENT", "CONFIRMED"].includes(booking.bookingStatus) && bookingFuture(booking);
  }

  function canReview(booking) {
    return ["CONFIRMED", "COMPLETED"].includes(booking.bookingStatus);
  }

  function summaryRows(booking, payment, receipt) {
    const schedule = booking.schedule;
    const passengerDetails = booking.passengerDetails || (receipt && receipt.passengerDetails) || [];
    const passengerRows = passengerDetails.length ? `
      <div class="detail full">
        <span>Seat passengers</span>
        <strong>${passengerDetails.map((passenger) => `Seat ${ui.escapeHtml(passenger.seatNumber)}: ${ui.escapeHtml(passenger.fullName)}${passenger.email ? ` (${ui.escapeHtml(passenger.email)})` : ""}`).join("<br>")}</strong>
      </div>` : "";
    return `
      <div class="detail-grid">
        <div class="detail"><span>Booking reference</span><strong>${ui.escapeHtml(booking.bookingReference)}</strong></div>
        <div class="detail"><span>Passenger</span><strong>${ui.escapeHtml(ui.fullName(booking.passengerUser))}</strong></div>
        <div class="detail"><span>Route</span><strong>${ui.escapeHtml(ui.routeLabel(schedule))}</strong></div>
        <div class="detail"><span>Travel date</span><strong>${ui.date(schedule.travelDate)}</strong></div>
        <div class="detail"><span>Departure</span><strong>${ui.time(schedule.departureTime)}</strong></div>
        <div class="detail"><span>Arrival</span><strong>${ui.time(schedule.arrivalTime)}</strong></div>
        <div class="detail"><span>Bus registration</span><strong>${ui.escapeHtml(schedule.bus.registrationNo)}</strong></div>
        <div class="detail"><span>Seats</span><strong>${ui.escapeHtml(ui.seatsLabel(booking.seats))}</strong></div>
        <div class="detail"><span>Total amount</span><strong>${ui.money(booking.totalAmount)}</strong></div>
        <div class="detail"><span>Payment method</span><strong>${ui.escapeHtml((payment && payment.paymentMethod) || (receipt && receipt.paymentMethod) || "Not available")}</strong></div>
        <div class="detail"><span>Booking status</span>${ui.badge(booking.bookingStatus)}</div>
        <div class="detail"><span>Payment status</span>${payment ? ui.badge(payment.paymentStatus) : "<strong>Not available</strong>"}</div>
        ${passengerRows}
      </div>`;
  }

  function renderPassengerDetails(seats) {
    return `<h3 class="form-row full">Passenger details</h3>` + seats.map((seat) => `
      <div class="form-row full passenger-seat" data-seat="${ui.escapeHtml(seat)}">
        <label class="passenger-seat-toggle">
          <span>Seat ${ui.escapeHtml(seat)}</span>
          <span><input name="passenger_${seat}_other" type="checkbox" style="width:auto;min-height:auto"> Book this seat for another passenger</span>
        </label>
        <div class="friend-passenger-fields form-grid" data-friend-fields="${ui.escapeHtml(seat)}" hidden>
          <div class="form-row full">
            <label for="passenger_${seat}_name">Full name</label>
            <input id="passenger_${seat}_name" name="passenger_${seat}_name" maxlength="160">
            <span class="field-error" data-error-for="passenger_${seat}_name"></span>
          </div>
          <div class="form-row">
            <label for="passenger_${seat}_nic">NIC / passport</label>
            <input id="passenger_${seat}_nic" name="passenger_${seat}_nic" maxlength="40">
          </div>
          <div class="form-row">
            <label for="passenger_${seat}_phone">Phone</label>
            <input id="passenger_${seat}_phone" name="passenger_${seat}_phone" maxlength="30">
          </div>
          <div class="form-row full">
            <label for="passenger_${seat}_email">Email</label>
            <input id="passenger_${seat}_email" name="passenger_${seat}_email" type="email" maxlength="160">
            <span class="field-error" data-error-for="passenger_${seat}_email"></span>
          </div>
        </div>
      </div>`).join("");
  }

  function clearFriendFields(fields) {
    ui.$all("input", fields).forEach((input) => {
      if (input.type !== "checkbox") input.value = "";
    });
    ui.$all(".field-error", fields).forEach((error) => {
      error.textContent = "";
    });
  }

  function setFriendFieldsOpen(fields, open) {
    if (!fields) return;
    if (open) {
      fields.hidden = false;
      window.requestAnimationFrame(() => fields.classList.add("is-open"));
      return;
    }
    fields.classList.remove("is-open");
    clearFriendFields(fields);
    window.setTimeout(() => {
      if (!fields.classList.contains("is-open")) fields.hidden = true;
    }, 240);
  }

  function collectPassengerDetails(seats) {
    const errors = {};
    const details = [];
    seats.forEach((seat) => {
      const other = ui.$(`[name="passenger_${seat}_other"]`).checked;
      if (!other) {
        return;
      }
      const fullName = ui.$(`[name="passenger_${seat}_name"]`).value.trim();
      const email = ui.$(`[name="passenger_${seat}_email"]`).value.trim();
      if (!fullName) errors[`passenger_${seat}_name`] = "Full name is required.";
      if (email && !ui.validateEmail(email)) errors[`passenger_${seat}_email`] = "Enter a valid email address.";
      details.push({
        seatNumber: Number(seat),
        bookingForAnotherPassenger: true,
        fullName,
        nicOrPassport: ui.$(`[name="passenger_${seat}_nic"]`).value.trim(),
        phone: ui.$(`[name="passenger_${seat}_phone"]`).value.trim(),
        email
      });
    });
    return { details, errors };
  }

  function initCheckout() {
    if (!window.Auth.requireAuth({ roles: ["PASSENGER"] })) return;
    const state = checkoutState();
    if (!state || !state.scheduleId || !state.seats || !state.seats.length) {
      ui.showAlert("#pageAlert", "warning", "Select seats before checkout.");
      ui.$("#checkoutContent").innerHTML = `<div class="empty-state"><a class="btn btn-primary" href="/index.html">Search schedules</a></div>`;
      return;
    }
    const schedule = state.schedule;
    ui.$("#checkoutSummary").innerHTML = `
      <div class="detail-grid">
        <div class="detail"><span>Route</span><strong>${ui.escapeHtml(ui.routeLabel(schedule))}</strong></div>
        <div class="detail"><span>Travel date</span><strong>${ui.date(schedule.travelDate)}</strong></div>
        <div class="detail"><span>Departure</span><strong>${ui.time(schedule.departureTime)}</strong></div>
        <div class="detail"><span>Selected seats</span><strong>${ui.escapeHtml(state.seats.join(", "))}</strong></div>
        <div class="detail"><span>Passengers</span><strong>${state.seats.length}</strong></div>
        <div class="detail"><span>Total payment</span><strong>${ui.money(state.total)}</strong></div>
      </div>`;
    ui.$("#passengerDetails").innerHTML = renderPassengerDetails(state.seats);
    ui.$all('[name$="_other"]', ui.$("#passengerDetails")).forEach((checkbox) => {
      checkbox.addEventListener("change", () => {
        const seat = checkbox.name.replace("passenger_", "").replace("_other", "");
        const fields = ui.$(`[data-friend-fields="${seat}"]`);
        setFriendFieldsOpen(fields, checkbox.checked);
      });
    });

    const refInput = ui.$('[name="transactionReference"]');
    if (refInput) refInput.value = window.Payments.transactionReference();
    let walletBalance = null;
    const methodSelect = ui.$('#paymentMethod');
    const walletBox = ui.$('#walletCheckoutBox');
    async function refreshWallet() {
      try { const w = await window.Api.get('/wallet'); walletBalance = Number(w.balance || 0); renderWallet(); } catch (_) { walletBalance = null; }
    }
    function renderWallet() {
      const isWallet = methodSelect && methodSelect.value === 'SMARTROUTE_WALLET';
      if (walletBox) {
        walletBox.hidden = !isWallet;
        if (isWallet) {
          const total = Number(state.total || 0); const remaining = walletBalance == null ? null : walletBalance - total;
          walletBox.innerHTML = walletBalance == null ? '<span class="muted">Unable to load wallet balance.</span>' :
            `<div class="detail-grid"><div class="detail"><span>Wallet balance</span><strong>${ui.money(walletBalance)}</strong></div><div class="detail"><span>Booking total</span><strong>${ui.money(total)}</strong></div><div class="detail"><span>Remaining balance</span><strong>${ui.money(Math.max(0, remaining))}</strong></div></div>${remaining < 0 ? '<div class="alert alert-warning">Insufficient wallet balance. <a href="/wallet.html">Top up wallet</a>.</div>' : ''}`;
        }
      }
      const wallet = isWallet;
      const refRow = ui.$('#transactionReferenceRow'), failRow = ui.$('#simulateFailureRow');
      if (refRow) refRow.hidden = wallet; if (failRow) failRow.hidden = wallet;
    }
    if (methodSelect) methodSelect.addEventListener('change', renderWallet);
    refreshWallet();
    const form = ui.$("#checkoutForm");
    form.addEventListener("submit", async (event) => {
      event.preventDefault();
      ui.showAlert("#pageAlert", "", "");
      const passengers = collectPassengerDetails(state.seats);
      ui.renderFieldErrors(passengers.errors);
      if (Object.keys(passengers.errors).length) {
        ui.showAlert("#pageAlert", "error", "Please fix the passenger details before continuing.");
        return;
      }
      const button = form.querySelector("button[type=submit]");
      ui.setLoading(button, true, "Processing");
      try {
        const data = ui.formData(form);
        if (data.paymentMethod === 'SMARTROUTE_WALLET' && walletBalance != null && walletBalance < Number(state.total || 0)) {
          throw new Error('Insufficient wallet balance. Top up your wallet before paying.');
        }
        const booking = await window.Api.post("/bookings", {
          scheduleId: state.scheduleId,
          seatNumbers: state.seats,
          passengerDetails: passengers.details
        });
        let payment;
        try {
          if (data.paymentMethod === 'SMARTROUTE_WALLET') {
            const walletResult = await window.Api.post(`/wallet/bookings/${booking.bookingId}/pay`, {});
            payment = walletResult.payment; walletBalance = Number(walletResult.balance || 0);
          } else {
            payment = await window.Payments.payBooking(booking.bookingId, { transactionReference: data.transactionReference, paymentMethod: data.paymentMethod, simulateFailure: data.simulateFailure === "on" });
          }
        } catch (paymentError) {
          try { await window.Api.patch(`/bookings/${booking.bookingId}/cancel`, null); } catch (_) {}
          throw paymentError;
        }
        localStorage.setItem(cfg.bookingKey, JSON.stringify({ booking, payment }));
        localStorage.removeItem(cfg.checkoutKey);
        if (payment.paymentStatus === "SUCCESS") {
          window.location.assign(`/ticket.html?bookingId=${booking.bookingId}&paymentId=${payment.paymentId}`);
        } else {
          ui.showAlert("#pageAlert", "warning", "Payment did not succeed. The unpaid booking was cancelled and the selected seats were released.");
        }
      } catch (error) {
        ui.showAlert("#pageAlert", "error", ui.errorMessage(error));
      } finally {
        ui.setLoading(button, false);
      }
    });
  }

  async function initTicket() {
    if (!window.Auth.requireAuth()) return;
    const params = new URLSearchParams(window.location.search);
    const saved = (() => {
      try { return JSON.parse(localStorage.getItem(cfg.bookingKey)); } catch (error) { return null; }
    })();
    const bookingId = params.get("bookingId") || (saved && saved.booking && saved.booking.bookingId);
    const paymentId = params.get("paymentId") || (saved && saved.payment && saved.payment.paymentId);
    if (!bookingId) {
      ui.$("#ticketContent").innerHTML = `<div class="empty-state">No booking id was provided.</div>`;
      return;
    }
    try {
      const booking = await window.Api.get(`/bookings/${bookingId}`);
      let payment = null;
      let receipt = null;
      if (paymentId) {
        try {
          payment = await window.Payments.verify(paymentId);
        } catch (error) {
          payment = saved && saved.payment ? saved.payment : null;
        }
      }
      try {
        receipt = await window.Payments.receipt(bookingId);
      } catch (error) {
        receipt = null;
      }
      ui.$("#ticketContent").innerHTML = `
        <article class="ticket">
          <div class="ticket-head">
            <p class="eyebrow">SmartRoute Lanka</p>
            <h1>${ui.escapeHtml(booking.bookingReference)}</h1>
          </div>
          <div class="ticket-body">${summaryRows(booking, payment, receipt)}</div>
          <div class="ticket-qr" id="ticketQr"></div>
        </article>`;
      try { const qr = await window.Api.get(`/payments/bookings/${bookingId}/ticket-qr`); ui.$("#ticketQr").innerHTML = `<img src="${qr.qrCodeDataUri}" alt="QR e-ticket" width="220" height="220">`; } catch (_) {}
      ui.$("#printBtn").addEventListener("click", () => window.print());
    } catch (error) {
      ui.showAlert("#pageAlert", "error", ui.errorMessage(error));
    }
  }

  async function loadReviewsMap() {
    try {
      const data = await window.Api.get("/reviews/my", { size: 100 });
      return new Map((data.content || []).map((review) => [review.bookingId, review]));
    } catch (error) {
      return new Map();
    }
  }

  async function loadMyBookings(page) {
    const list = ui.$("#bookingsList");
    list.innerHTML = `<div class="empty-state"><span class="spinner"></span> Loading bookings...</div>`;
    try {
      const [data, reviews] = await Promise.all([
        window.Api.get("/bookings/my", { page: page || 0, size: cfg.defaultPageSize }),
        loadReviewsMap()
      ]);
      if (!data.content || data.content.length === 0) {
        list.innerHTML = `<div class="empty-state">You do not have any bookings yet.</div>`;
        ui.pagination("#bookingsPagination", data, loadMyBookings);
        return;
      }
      list.innerHTML = `<div class="premium-bookings">${data.content.map((booking) => `
        <article class="card premium-booking ${booking.bookingStatus === "CANCELLED" ? "cancelled" : ""}">
          <div>
            <div class="premium-booking-top">${ui.badge(booking.bookingStatus)}<span class="premium-booking-ref">${ui.escapeHtml(booking.bookingReference)}</span><span class="premium-booking-date">${ui.date(booking.schedule.travelDate)}</span></div>
            <div class="premium-route">
              <div class="premium-route-stop"><strong>${ui.time(booking.schedule.departureTime)}</strong><span>${ui.escapeHtml(booking.schedule.route.origin)}</span></div>
              <div class="premium-route-line" aria-hidden="true">${ui.icon("bus")}</div>
              <div class="premium-route-stop"><strong>${ui.time(booking.schedule.arrivalTime)}</strong><span>${ui.escapeHtml(booking.schedule.route.destination)}</span></div>
            </div>
            <div class="premium-booking-meta"><span>${ui.icon("seat")} Seats ${ui.escapeHtml(ui.seatsLabel(booking.seats))}</span><span>${ui.icon("bus")} ${ui.escapeHtml(ui.formatStatus(booking.schedule.bus.busType))}</span><span>${ui.icon("users")} ${(booking.seats || []).length} passenger(s)</span></div>
          </div>
          <div class="premium-booking-side"><strong class="premium-booking-total">${ui.money(booking.totalAmount)}</strong>
            <div class="actions"><a class="btn btn-primary" href="/ticket.html?bookingId=${booking.bookingId}">View ticket</a></div>
            <div class="premium-minor-actions">
              ${canCancel(booking) ? `<button class="btn btn-small btn-danger" data-cancel="${booking.bookingId}" type="button">Cancel</button>` : ""}
              ${canReview(booking) && !reviews.has(booking.bookingId) ? `<a class="btn btn-small btn-ghost" href="/review.html?bookingId=${booking.bookingId}">Review</a>` : ""}
              ${reviews.has(booking.bookingId) ? `<span class="status-pill status-success">Reviewed</span>` : ""}
            </div>
          </div>
        </article>`).join("")}</div>`;
      ui.$all("[data-cancel]").forEach((button) => {
        button.addEventListener("click", async () => {
          if (!confirm("Cancel this booking? Reserved seats will be released.")) return;
          try {
            await window.Api.patch(`/bookings/${button.dataset.cancel}/cancel`, null);
            ui.showAlert("#pageAlert", "success", "Booking cancelled.");
            loadMyBookings(page || 0);
          } catch (error) {
            ui.showAlert("#pageAlert", "error", ui.errorMessage(error));
          }
        });
      });
      ui.pagination("#bookingsPagination", data, loadMyBookings);
    } catch (error) {
      list.innerHTML = "";
      ui.showAlert("#pageAlert", "error", ui.errorMessage(error));
    }
  }

  document.addEventListener("DOMContentLoaded", () => {
    const page = document.body.dataset.page;
    if (page === "checkout") initCheckout();
    if (page === "ticket") initTicket();
    if (page === "my-bookings" && window.Auth.requireAuth({ roles: ["PASSENGER"] })) loadMyBookings(0);
  });
})();
