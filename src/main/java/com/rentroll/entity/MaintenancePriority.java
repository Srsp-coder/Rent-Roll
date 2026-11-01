package com.rentroll.entity;

/**
 * Urgency level for repair requests.
 */
public enum MaintenancePriority {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High / Emergency");

    private final String displayName;

    MaintenancePriority(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

