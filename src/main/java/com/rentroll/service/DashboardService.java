package com.rentroll.service;

import com.rentroll.dto.AdminDashboardDto;
import com.rentroll.dto.LandlordDashboardDto;
import com.rentroll.dto.ReportSummaryDto;
import com.rentroll.dto.TenantDashboardDto;
import com.rentroll.entity.Lease;
import com.rentroll.entity.LeaseStatus;
import com.rentroll.entity.MaintenanceStatus;
import com.rentroll.entity.PaymentStatus;
import com.rentroll.entity.RentPayment;
import com.rentroll.entity.Role;
import com.rentroll.entity.User;
import com.rentroll.repository.LeaseRepository;
import com.rentroll.repository.MaintenanceRequestRepository;
import com.rentroll.repository.PropertyRepository;
import com.rentroll.repository.RentPaymentRepository;
import com.rentroll.repository.UserRepository;
import com.rentroll.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Service aggregating analytics, KPIs, and monthly financial summaries for dashboards.
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final LeaseRepository leaseRepository;
    private final RentPaymentRepository rentPaymentRepository;
    private final MaintenanceRequestRepository maintenanceRepository;
    private final SecurityUtils securityUtils;

    public DashboardService(UserRepository userRepository,
                            PropertyRepository propertyRepository,
                            LeaseRepository leaseRepository,
                            RentPaymentRepository rentPaymentRepository,
                            MaintenanceRequestRepository maintenanceRepository,
                            SecurityUtils securityUtils) {
        this.userRepository = userRepository;
        this.propertyRepository = propertyRepository;
        this.leaseRepository = leaseRepository;
        this.rentPaymentRepository = rentPaymentRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.securityUtils = securityUtils;
    }

    /**
     * Builds data for Landlord dashboard.
     */
    public LandlordDashboardDto getLandlordDashboard(Long landlordId) {
        LandlordDashboardDto dto = new LandlordDashboardDto();
        LocalDate today = LocalDate.now();
        YearMonth currentYearMonth = YearMonth.from(today);
        LocalDate startOfMonth = currentYearMonth.atDay(1);
        LocalDate endOfMonth = currentYearMonth.atEndOfMonth();

        dto.setTotalProperties(propertyRepository.countByLandlordId(landlordId));
        dto.setActiveLeases(leaseRepository.countByPropertyLandlordIdAndStatus(landlordId, LeaseStatus.ACTIVE));

        BigDecimal expected = rentPaymentRepository.sumExpectedRentForLandlord(landlordId, startOfMonth, endOfMonth);
        dto.setExpectedRentCurrentMonth(expected != null ? expected : BigDecimal.ZERO);

        BigDecimal collected = rentPaymentRepository.sumCollectedRentForLandlord(landlordId, startOfMonth, endOfMonth);
        dto.setCollectedRentCurrentMonth(collected != null ? collected : BigDecimal.ZERO);

        BigDecimal outstanding = rentPaymentRepository.sumOutstandingRentForLandlord(landlordId);
        dto.setOutstandingRent(outstanding != null ? outstanding : BigDecimal.ZERO);

        dto.setOverduePaymentsCount(rentPaymentRepository.countOverduePaymentsForLandlord(landlordId, today));
        dto.setExpiringLeases(leaseRepository.findExpiringLeasesForLandlord(landlordId, today, today.plusDays(60)));
        dto.setRecentPayments(rentPaymentRepository.findTop5ByPropertyLandlordIdOrderByDueDateDesc(landlordId));
        dto.setUpcomingDues(rentPaymentRepository.findUpcomingDuesForLandlord(landlordId, today));

        // Prepare 6-month historical/projected trend for Chart.js
        List<String> labels = new ArrayList<>();
        List<BigDecimal> collectedData = new ArrayList<>();
        List<BigDecimal> expectedData = new ArrayList<>();

        for (int i = 5; i >= 0; i--) {
            YearMonth ym = currentYearMonth.minusMonths(i);
            String monthLabel = ym.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            labels.add(monthLabel);

            BigDecimal mCollected = rentPaymentRepository.sumCollectedRentForLandlord(
                    landlordId, ym.atDay(1), ym.atEndOfMonth());
            BigDecimal mExpected = rentPaymentRepository.sumExpectedRentForLandlord(
                    landlordId, ym.atDay(1), ym.atEndOfMonth());

            collectedData.add(mCollected != null ? mCollected : BigDecimal.ZERO);
            expectedData.add(mExpected != null ? mExpected : BigDecimal.ZERO);
        }

        dto.setChartLabels(labels);
        dto.setChartCollectedData(collectedData);
        dto.setChartExpectedData(expectedData);

        return dto;
    }

    /**
     * Builds data for Tenant dashboard.
     */
    public TenantDashboardDto getTenantDashboard(Long tenantId) {
        TenantDashboardDto dto = new TenantDashboardDto();
        LocalDate today = LocalDate.now();

        Lease activeLease = leaseRepository.findFirstByTenantIdAndStatus(tenantId, LeaseStatus.ACTIVE)
                .orElse(null);
        dto.setActiveLease(activeLease);

        if (activeLease != null) {
            dto.setMonthlyRent(activeLease.getMonthlyRent());
        } else {
            dto.setMonthlyRent(BigDecimal.ZERO);
        }

        List<RentPayment> payments = rentPaymentRepository.findByTenantIdOrderByDueDateDesc(tenantId);
        dto.setPaymentHistory(payments);

        // Find next rent due date and calculate outstanding balance
        LocalDate nextDue = null;
        BigDecimal balance = BigDecimal.ZERO;

        for (RentPayment p : payments) {
            if (p.getStatus() == PaymentStatus.LATE || p.getStatus() == PaymentStatus.PENDING) {
                balance = balance.add(p.getTotalAmount());
            }
            if (p.getStatus() != PaymentStatus.PAID && p.getStatus() != PaymentStatus.CANCELLED) {
                if (nextDue == null || (p.getDueDate().isAfter(today.minusDays(1)) && p.getDueDate().isBefore(nextDue))) {
                    nextDue = p.getDueDate();
                }
            }
        }

        dto.setNextRentDueDate(nextDue);
        dto.setOutstandingBalance(balance);

        dto.setMaintenanceRequests(maintenanceRepository.findByTenantIdOrderByCreatedAtDesc(tenantId));
        dto.setOpenRequestsCount(maintenanceRepository.countByTenantIdAndStatus(tenantId, MaintenanceStatus.OPEN));

        return dto;
    }

    /**
     * Builds data for Administrator overview dashboard.
     */
    public AdminDashboardDto getAdminDashboard() {
        AdminDashboardDto dto = new AdminDashboardDto();
        LocalDate today = LocalDate.now();

        dto.setTotalUsers(userRepository.count());
        dto.setTotalLandlords(userRepository.countByRole(Role.LANDLORD));
        dto.setTotalTenants(userRepository.countByRole(Role.TENANT));
        dto.setTotalProperties(propertyRepository.count());
        dto.setTotalActiveLeases(leaseRepository.countByStatus(LeaseStatus.ACTIVE));

        BigDecimal collected = rentPaymentRepository.sumAllCollectedRent();
        dto.setTotalCollectedRent(collected != null ? collected : BigDecimal.ZERO);

        BigDecimal outstanding = rentPaymentRepository.sumAllOutstandingRent();
        dto.setTotalOutstandingRent(outstanding != null ? outstanding : BigDecimal.ZERO);

        dto.setTotalOverduePayments(rentPaymentRepository.countAllOverduePayments(today));
        dto.setTotalOpenMaintenanceTickets(maintenanceRepository.countByStatus(MaintenanceStatus.OPEN));

        return dto;
    }

    /**
     * Aggregates detailed monthly financial report.
     */
    public ReportSummaryDto getReportSummary(Integer year, Integer month, Long landlordId) {
        LocalDate today = LocalDate.now();
        int targetYear = year != null ? year : today.getYear();
        int targetMonth = month != null ? month : today.getMonthValue();

        YearMonth ym = YearMonth.of(targetYear, targetMonth);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        ReportSummaryDto dto = new ReportSummaryDto();
        dto.setSelectedYear(targetYear);
        dto.setSelectedMonth(targetMonth);
        dto.setMonthName(Month.of(targetMonth).getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + targetYear);

        BigDecimal expected;
        BigDecimal collected;
        long overdueCount;
        List<Lease> expiringLeases;
        List<RentPayment> payments;

        if (landlordId != null) {
            expected = rentPaymentRepository.sumExpectedRentForLandlord(landlordId, start, end);
            collected = rentPaymentRepository.sumCollectedRentForLandlord(landlordId, start, end);
            overdueCount = rentPaymentRepository.countOverduePaymentsForLandlord(landlordId, today);
            expiringLeases = leaseRepository.findExpiringLeasesForLandlord(landlordId, today, today.plusDays(60));
            payments = rentPaymentRepository.findByPropertyLandlordIdOrderByDueDateDesc(landlordId);
        } else {
            expected = rentPaymentRepository.sumAllCollectedRent();
            collected = rentPaymentRepository.sumAllCollectedRent();
            overdueCount = rentPaymentRepository.countAllOverduePayments(today);
            expiringLeases = leaseRepository.findAllExpiringLeases(today, today.plusDays(60));
            payments = rentPaymentRepository.findAll();
        }

        expected = expected != null ? expected : BigDecimal.ZERO;
        collected = collected != null ? collected : BigDecimal.ZERO;
        BigDecimal outstanding = expected.subtract(collected);
        if (outstanding.compareTo(BigDecimal.ZERO) < 0) {
            outstanding = BigDecimal.ZERO;
        }

        dto.setExpectedRent(expected);
        dto.setCollectedRent(collected);
        dto.setOutstandingRent(outstanding);
        dto.setOverduePaymentCount(overdueCount);

        double pct = 0.0;
        if (expected.compareTo(BigDecimal.ZERO) > 0) {
            pct = collected.multiply(BigDecimal.valueOf(100))
                    .divide(expected, 1, RoundingMode.HALF_UP)
                    .doubleValue();
        }
        dto.setCollectionPercentage(pct);
        dto.setExpiringLeases(expiringLeases);

        // Filter payments belonging to this specific month for detailed display
        List<RentPayment> monthPayments = new ArrayList<>();
        for (RentPayment p : payments) {
            if (p.getDueDate() != null && !p.getDueDate().isBefore(start) && !p.getDueDate().isAfter(end)) {
                monthPayments.add(p);
            }
        }
        dto.setMonthPayments(monthPayments);

        return dto;
    }
}

