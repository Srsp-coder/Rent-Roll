package com.rentroll.entity;

/**
 * Lifecycle statuses for a monthly rent payment.
 * - UPCOMING: Due date is more than 7 days in the future.
 * - PENDING: Due date is approaching or due today.
 * - LATE: Due date has passed and payment has not been received.
 * - PAID: Payment has been settled and verified by the landlord.
 * - CANCELLED: Lease ended early or period was voided.
 */
public enum PaymentStatus {
    UPCOMING("Upcoming"),
    PENDING("Pending"),
    LATE("Overdue / Late"),
    PAID("Paid"),
    CANCELLED("Cancelled");

    private final String displayName;

    PaymentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

