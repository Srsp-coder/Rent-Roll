package com.rentroll.dto;

import com.rentroll.entity.Lease;
import com.rentroll.entity.MaintenanceRequest;
import com.rentroll.entity.RentPayment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Data transfer object aggregating metrics for Tenant dashboard.
 */
public class TenantDashboardDto {

    private Lease activeLease;
    private BigDecimal monthlyRent;
    private LocalDate nextRentDueDate;
    private BigDecimal outstandingBalance;
    private List<RentPayment> paymentHistory;
    private List<MaintenanceRequest> maintenanceRequests;
    private long openRequestsCount;

    public TenantDashboardDto() {
    }

    public Lease getActiveLease() {
        return activeLease;
    }

    public void setActiveLease(Lease activeLease) {
        this.activeLease = activeLease;
    }

    public BigDecimal getMonthlyRent() {
        return monthlyRent;
    }

    public void setMonthlyRent(BigDecimal monthlyRent) {
        this.monthlyRent = monthlyRent;
    }

    public LocalDate getNextRentDueDate() {
        return nextRentDueDate;
    }

    public void setNextRentDueDate(LocalDate nextRentDueDate) {
        this.nextRentDueDate = nextRentDueDate;
    }

    public BigDecimal getOutstandingBalance() {
        return outstandingBalance;
    }

    public void setOutstandingBalance(BigDecimal outstandingBalance) {
        this.outstandingBalance = outstandingBalance;
    }

    public List<RentPayment> getPaymentHistory() {
        return paymentHistory;
    }

    public void setPaymentHistory(List<RentPayment> paymentHistory) {
        this.paymentHistory = paymentHistory;
    }

    public List<MaintenanceRequest> getMaintenanceRequests() {
        return maintenanceRequests;
    }

    public void setMaintenanceRequests(List<MaintenanceRequest> maintenanceRequests) {
        this.maintenanceRequests = maintenanceRequests;
    }

    public long getOpenRequestsCount() {
        return openRequestsCount;
    }

    public void setOpenRequestsCount(long openRequestsCount) {
        this.openRequestsCount = openRequestsCount;
    }
}

