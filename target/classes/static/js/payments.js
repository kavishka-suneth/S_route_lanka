(function () {
  function transactionReference() {
    return "WEB-" + Date.now() + "-" + Math.random().toString(36).slice(2, 7).toUpperCase();
  }

  async function payBooking(bookingId, options) {
    const opts = options || {};
    return window.Api.post(`/payments/bookings/${bookingId}`, {
      transactionReference: opts.transactionReference || transactionReference(),
      paymentMethod: opts.paymentMethod || "CARD",
      simulateFailure: Boolean(opts.simulateFailure)
    });
  }

  async function verify(paymentId) {
    return window.Api.get(`/payments/${paymentId}/verify`);
  }

  async function receipt(bookingId) {
    return window.Api.get(`/payments/bookings/${bookingId}/receipt`);
  }

  window.Payments = {
    transactionReference,
    payBooking,
    verify,
    receipt
  };
})();
