package com.rentroll.entity;

/**
 * Roles for authorization in RentRoll.
 * - ADMIN: System administrator who can manage all users and oversee the system.
 * - LANDLORD: Property owner who manages their properties, leases, rent collection, and repairs.
 * - TENANT: Resident who rents a property, tracks dues, and submits maintenance requests.
 */
public enum Role {
    ADMIN("Administrator"),
    LANDLORD("Landlord"),
    TENANT("Tenant");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

