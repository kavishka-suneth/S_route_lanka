(function () {
  const ui = window.SmartRoute;

  async function initReviewPage() {
    if (!window.Auth.requireAuth({ roles: ["PASSENGER"] })) return;
    const bookingId = Number(new URLSearchParams(window.location.search).get("bookingId"));
    const form = ui.$("#reviewForm");
    if (!bookingId) {
      ui.$("#reviewContent").innerHTML = `<div class="empty-state">Choose an eligible booking from booking history.</div>`;
      form.style.display = "none";
      return;
    }
    try {
      const [booking, mine] = await Promise.all([
        window.Api.get(`/bookings/${bookingId}`),
        window.Api.get("/reviews/my", { size: 100 })
      ]);
      ui.$("#reviewBooking").innerHTML = `
        <div class="detail-grid">
          <div class="detail"><span>Booking</span><strong>${ui.escapeHtml(booking.bookingReference)}</strong></div>
          <div class="detail"><span>Route</span><strong>${ui.escapeHtml(ui.routeLabel(booking.schedule))}</strong></div>
          <div class="detail"><span>Travel date</span><strong>${ui.date(booking.schedule.travelDate)}</strong></div>
          <div class="detail"><span>Status</span>${ui.badge(booking.bookingStatus)}</div>
        </div>`;
      const existing = (mine.content || []).find((review) => Number(review.bookingId) === bookingId);
      if (existing) {
        form.style.display = "none";
        ui.$("#existingReview").innerHTML = `
          <div class="alert alert-info">
            Existing review: ${existing.rating}/5<br>${ui.escapeHtml(existing.comment || "No comment")}
          </div>`;
        return;
      }
      if (!["CONFIRMED", "COMPLETED"].includes(booking.bookingStatus)) {
        form.style.display = "none";
        ui.showAlert("#pageAlert", "warning", "Only confirmed or completed bookings can be reviewed.");
        return;
      }
      ui.$('[name="bookingId"]').value = bookingId;
    } catch (error) {
      form.style.display = "none";
      ui.showAlert("#pageAlert", "error", ui.errorMessage(error));
      return;
    }

    form.addEventListener("submit", async (event) => {
      event.preventDefault();
      const data = ui.formData(form);
      const rating = Number(data.rating);
      const errors = {};
      if (!rating || rating < 1 || rating > 5) errors.rating = "Select a rating from 1 to 5.";
      ui.renderFieldErrors(errors);
      if (Object.keys(errors).length) return;
      try {
        const review = await window.Api.post("/reviews", {
          bookingId: Number(data.bookingId),
          rating,
          comment: data.comment.trim()
        });
        form.style.display = "none";
        ui.showAlert("#pageAlert", "success", "Review submitted.");
        ui.$("#existingReview").innerHTML = `<div class="alert alert-info">Review: ${review.rating}/5<br>${ui.escapeHtml(review.comment || "No comment")}</div>`;
      } catch (error) {
        ui.showAlert("#pageAlert", "error", ui.errorMessage(error));
      }
    });
  }

  document.addEventListener("DOMContentLoaded", () => {
    if (document.body.dataset.page === "review") initReviewPage();
  });
})();
