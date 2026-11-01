package com.rentroll.dto;

import com.rentroll.entity.Lease;
import com.rentroll.entity.RentPayment;

import java.math.BigDecimal;
import java.util.List;

/**
 * Data transfer object aggregating metrics for the Landlord dashboard.
 */
public class LandlordDashboardDto {

    private long totalProperties;
    private long activeLeases;
    private BigDecimal expectedRentCurrentMonth;
    private BigDecimal collectedRentCurrentMonth;
    private BigDecimal outstandingRent;
    private long overduePaymentsCount;
    private List<Lease> expiringLeases;
    private List<RentPayment> recentPayments;
    private List<RentPayment> upcomingDues;

    // Chart.js data points
    private List<String> chartLabels;
    private List<BigDecimal> chartCollectedData;
    private List<BigDecimal> chartExpectedData;

    public LandlordDashboardDto() {
    }

    public long getTotalProperties() {
        return totalProperties;
    }

    public void setTotalProperties(long totalProperties) {
        this.totalProperties = totalProperties;
    }

    public long getActiveLeases() {
        return activeLeases;
    }

    public void setActiveLeases(long activeLeases) {
        this.activeLeases = activeLeases;
    }

    public BigDecimal getExpectedRentCurrentMonth() {
        return expectedRentCurrentMonth;
    }

    public void setExpectedRentCurrentMonth(BigDecimal expectedRentCurrentMonth) {
        this.expectedRentCurrentMonth = expectedRentCurrentMonth;
    }

    public BigDecimal getCollectedRentCurrentMonth() {
        return collectedRentCurrentMonth;
    }

    public void setCollectedRentCurrentMonth(BigDecimal collectedRentCurrentMonth) {
        this.collectedRentCurrentMonth = collectedRentCurrentMonth;
    }

    public BigDecimal getOutstandingRent() {
        return outstandingRent;
    }

    public void setOutstandingRent(BigDecimal outstandingRent) {
        this.outstandingRent = outstandingRent;
    }

    public long getOverduePaymentsCount() {
        return overduePaymentsCount;
    }

    public void setOverduePaymentsCount(long overduePaymentsCount) {
        this.overduePaymentsCount = overduePaymentsCount;
    }

    public List<Lease> getExpiringLeases() {
        return expiringLeases;
    }

    public void setExpiringLeases(List<Lease> expiringLeases) {
        this.expiringLeases = expiringLeases;
    }

    public List<RentPayment> getRecentPayments() {
        return recentPayments;
    }

    public void setRecentPayments(List<RentPayment> recentPayments) {
        this.recentPayments = recentPayments;
    }

    public List<RentPayment> getUpcomingDues() {
        return upcomingDues;
    }

    public void setUpcomingDues(List<RentPayment> upcomingDues) {
        this.upcomingDues = upcomingDues;
    }

    public List<String> getChartLabels() {
        return chartLabels;
    }

    public void setChartLabels(List<String> chartLabels) {
        this.chartLabels = chartLabels;
    }

    public List<BigDecimal> getChartCollectedData() {
        return chartCollectedData;
    }

    public void setChartCollectedData(List<BigDecimal> chartCollectedData) {
        this.chartCollectedData = chartCollectedData;
    }

    public List<BigDecimal> getChartExpectedData() {
        return chartExpectedData;
    }

    public void setChartExpectedData(List<BigDecimal> chartExpectedData) {
        this.chartExpectedData = chartExpectedData;
    }
}

