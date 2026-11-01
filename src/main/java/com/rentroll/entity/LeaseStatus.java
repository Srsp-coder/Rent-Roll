package com.rentroll.entity;

/**
 * Status of a rental lease agreement.
 */
public enum LeaseStatus {
    ACTIVE("Active"),
    EXPIRED("Expired"),
    TERMINATED("Terminated");

    private final String displayName;

    LeaseStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

