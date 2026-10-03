package lk.smartroute.model;

public enum Role {
    PASSENGER,
    CUSTOMER_SERVICE,
    TRANSPORT_COORDINATOR,
    FINANCE_OFFICER,
    OPERATIONS_MANAGER,
    SYSTEM_ADMIN,
    IT_SUPPORT;

    public String authority() {
        return "ROLE_" + name();
    }

    public static Role fromStaffType(StaffType staffType) {
        return switch (staffType) {
            case CUSTOMER_SERVICE_STAFF -> CUSTOMER_SERVICE;
            case TRANSPORT_COORDINATOR -> TRANSPORT_COORDINATOR;
            case FINANCE_OFFICER -> FINANCE_OFFICER;
            case OPERATIONS_MANAGER -> OPERATIONS_MANAGER;
            case SYSTEM_ADMINISTRATOR -> SYSTEM_ADMIN;
            case IT_SUPPORT_STAFF -> IT_SUPPORT;
        };
    }

    public StaffType toStaffType() {
        return switch (this) {
            case CUSTOMER_SERVICE -> StaffType.CUSTOMER_SERVICE_STAFF;
            case TRANSPORT_COORDINATOR -> StaffType.TRANSPORT_COORDINATOR;
            case FINANCE_OFFICER -> StaffType.FINANCE_OFFICER;
            case OPERATIONS_MANAGER -> StaffType.OPERATIONS_MANAGER;
            case SYSTEM_ADMIN -> StaffType.SYSTEM_ADMINISTRATOR;
            case IT_SUPPORT -> StaffType.IT_SUPPORT_STAFF;
            case PASSENGER -> throw new IllegalStateException("Passenger has no staff type");
        };
    }
}
