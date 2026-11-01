package com.rentroll.entity;

/**
 * Categories of maintenance and repair requests.
 */
public enum MaintenanceCategory {
    PLUMBING("Plumbing & Water"),
    ELECTRICAL("Electrical & Power"),
    APPLIANCE("Appliance Repair"),
    STRUCTURAL("Carpentry & Structural"),
    PAINTING("Painting & Seepage"),
    OTHER("Other Maintenance");

    private final String displayName;

    MaintenanceCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

