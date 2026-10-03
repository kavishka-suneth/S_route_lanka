(function () {
  window.SmartRouteConfig = {
    apiBase: "/api",
    tokenKey: "srl.jwt",
    userKey: "srl.user",
    checkoutKey: "srl.checkout",
    bookingKey: "srl.lastBooking",
    defaultPageSize: 10,
    roles: [
      "PASSENGER",
      "CUSTOMER_SERVICE",
      "TRANSPORT_COORDINATOR",
      "FINANCE_OFFICER",
      "OPERATIONS_MANAGER",
      "SYSTEM_ADMIN",
      "IT_SUPPORT"
    ],
    staffRoles: [
      "CUSTOMER_SERVICE",
      "TRANSPORT_COORDINATOR",
      "FINANCE_OFFICER",
      "OPERATIONS_MANAGER",
      "SYSTEM_ADMIN",
      "IT_SUPPORT"
    ],
    transportRoles: ["TRANSPORT_COORDINATOR", "OPERATIONS_MANAGER", "SYSTEM_ADMIN"],
    supportRoles: ["CUSTOMER_SERVICE", "OPERATIONS_MANAGER", "SYSTEM_ADMIN"],
    financeRoles: ["FINANCE_OFFICER", "SYSTEM_ADMIN"],
    reportRoles: ["FINANCE_OFFICER", "OPERATIONS_MANAGER", "SYSTEM_ADMIN"],
    notificationRoles: ["CUSTOMER_SERVICE", "TRANSPORT_COORDINATOR", "OPERATIONS_MANAGER", "SYSTEM_ADMIN"],
    userAdminRoles: ["CUSTOMER_SERVICE", "OPERATIONS_MANAGER", "SYSTEM_ADMIN", "IT_SUPPORT"],
    busTypes: ["STANDARD", "SEMI_LUXURY", "LUXURY", "AC"],
    availabilityStatuses: ["AVAILABLE", "UNAVAILABLE", "MAINTENANCE"],
    busStatuses: ["ACTIVE", "INACTIVE", "MAINTENANCE"],
    scheduleStatuses: ["SCHEDULED", "DELAYED", "CANCELLED", "COMPLETED"],
    bookingStatuses: ["PENDING_PAYMENT", "CONFIRMED", "CANCELLED", "COMPLETED"],
    paymentStatuses: ["PENDING", "SUCCESS", "FAILED", "REFUNDED"],
    supportStatuses: ["OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED"]
  };
})();
