package com.rentroll.dto;

import java.math.BigDecimal;

/**
 * Data transfer object aggregating system-wide metrics for the Admin dashboard.
 */
public class AdminDashboardDto {

    private long totalUsers;
    private long totalLandlords;
    private long totalTenants;
    private long totalProperties;
    private long totalActiveLeases;
    private BigDecimal totalCollectedRent;
    private BigDecimal totalOutstandingRent;
    private long totalOverduePayments;
    private long totalOpenMaintenanceTickets;

    public AdminDashboardDto() {
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getTotalLandlords() {
        return totalLandlords;
    }

    public void setTotalLandlords(long totalLandlords) {
        this.totalLandlords = totalLandlords;
    }

    public long getTotalTenants() {
        return totalTenants;
    }

    public void setTotalTenants(long totalTenants) {
        this.totalTenants = totalTenants;
    }

    public long getTotalProperties() {
        return totalProperties;
    }

    public void setTotalProperties(long totalProperties) {
        this.totalProperties = totalProperties;
    }

    public long getTotalActiveLeases() {
        return totalActiveLeases;
    }

    public void setTotalActiveLeases(long totalActiveLeases) {
        this.totalActiveLeases = totalActiveLeases;
    }

    public BigDecimal getTotalCollectedRent() {
        return totalCollectedRent;
    }

    public void setTotalCollectedRent(BigDecimal totalCollectedRent) {
        this.totalCollectedRent = totalCollectedRent;
    }

    public BigDecimal getTotalOutstandingRent() {
        return totalOutstandingRent;
    }

    public void setTotalOutstandingRent(BigDecimal totalOutstandingRent) {
        this.totalOutstandingRent = totalOutstandingRent;
    }

    public long getTotalOverduePayments() {
        return totalOverduePayments;
    }

    public void setTotalOverduePayments(long totalOverduePayments) {
        this.totalOverduePayments = totalOverduePayments;
    }

    public long getTotalOpenMaintenanceTickets() {
        return totalOpenMaintenanceTickets;
    }

    public void setTotalOpenMaintenanceTickets(long totalOpenMaintenanceTickets) {
        this.totalOpenMaintenanceTickets = totalOpenMaintenanceTickets;
    }
}

