package com.rentroll.entity;

/**
 * Common property types in India.
 */
public enum PropertyType {
    APARTMENT("Apartment / Flat"),
    INDEPENDENT_HOUSE("Independent House / Villa"),
    STUDIO("Studio / 1RK"),
    COMMERCIAL("Commercial Shop / Office"),
    ROOM("Single Room / PG");

    private final String displayName;

    PropertyType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

