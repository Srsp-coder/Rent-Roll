package com.rentroll.dto;

import com.rentroll.entity.Lease;
import com.rentroll.entity.RentPayment;

import java.math.BigDecimal;
import java.util.List;

/**
 * Summary metrics for financial reports and lease expiries.
 */
public class ReportSummaryDto {

    private int selectedYear;
    private int selectedMonth;
    private String monthName;
    private BigDecimal expectedRent;
    private BigDecimal collectedRent;
    private BigDecimal outstandingRent;
    private long overduePaymentCount;
    private double collectionPercentage;
    private List<Lease> expiringLeases;
    private List<RentPayment> monthPayments;

    public ReportSummaryDto() {
    }

    public int getSelectedYear() {
        return selectedYear;
    }

    public void setSelectedYear(int selectedYear) {
        this.selectedYear = selectedYear;
    }

    public int getSelectedMonth() {
        return selectedMonth;
    }

    public void setSelectedMonth(int selectedMonth) {
        this.selectedMonth = selectedMonth;
    }

    public String getMonthName() {
        return monthName;
    }

    public void setMonthName(String monthName) {
        this.monthName = monthName;
    }

    public BigDecimal getExpectedRent() {
        return expectedRent;
    }

    public void setExpectedRent(BigDecimal expectedRent) {
        this.expectedRent = expectedRent;
    }

    public BigDecimal getCollectedRent() {
        return collectedRent;
    }

    public void setCollectedRent(BigDecimal collectedRent) {
        this.collectedRent = collectedRent;
    }

    public BigDecimal getOutstandingRent() {
        return outstandingRent;
    }

    public void setOutstandingRent(BigDecimal outstandingRent) {
        this.outstandingRent = outstandingRent;
    }

    public long getOverduePaymentCount() {
        return overduePaymentCount;
    }

    public void setOverduePaymentCount(long overduePaymentCount) {
        this.overduePaymentCount = overduePaymentCount;
    }

    public double getCollectionPercentage() {
        return collectionPercentage;
    }

    public void setCollectionPercentage(double collectionPercentage) {
        this.collectionPercentage = collectionPercentage;
    }

    public List<Lease> getExpiringLeases() {
        return expiringLeases;
    }

    public void setExpiringLeases(List<Lease> expiringLeases) {
        this.expiringLeases = expiringLeases;
    }

    public List<RentPayment> getMonthPayments() {
        return monthPayments;
    }

    public void setMonthPayments(List<RentPayment> monthPayments) {
        this.monthPayments = monthPayments;
    }
}

