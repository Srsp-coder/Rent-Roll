package com.rentroll.service;

import com.rentroll.entity.Lease;
import com.rentroll.entity.LeaseStatus;
import com.rentroll.entity.PaymentStatus;
import com.rentroll.entity.RentPayment;
import com.rentroll.entity.Role;
import com.rentroll.entity.User;
import com.rentroll.exception.BusinessRuleException;
import com.rentroll.exception.ResourceNotFoundException;
import com.rentroll.exception.UnauthorizedAccessException;
import com.rentroll.repository.LeaseRepository;
import com.rentroll.repository.RentPaymentRepository;
import com.rentroll.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * Service managing lease agreements and automated rent schedule generation.
 */
@Service
@Transactional
public class LeaseService {

    private final LeaseRepository leaseRepository;
    private final RentPaymentRepository rentPaymentRepository;
    private final SecurityUtils securityUtils;

    public LeaseService(LeaseRepository leaseRepository,
                        RentPaymentRepository rentPaymentRepository,
                        SecurityUtils securityUtils) {
        this.leaseRepository = leaseRepository;
        this.rentPaymentRepository = rentPaymentRepository;
        this.securityUtils = securityUtils;
    }

    public List<Lease> getLeasesForCurrentUser() {
        User currentUser = securityUtils.getRequiredCurrentUser();
        if (currentUser.getRole() == Role.ADMIN) {
            return leaseRepository.findAll();
        } else if (currentUser.getRole() == Role.LANDLORD) {
            return leaseRepository.findByPropertyLandlordId(currentUser.getId());
        } else {
            return leaseRepository.findByTenantId(currentUser.getId());
        }
    }

    public Lease getLeaseById(Long id) {
        Lease lease = leaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lease not found with id: " + id));

        User currentUser = securityUtils.getRequiredCurrentUser();
        if (currentUser.getRole() == Role.LANDLORD &&
                !lease.getProperty().getLandlord().getId().equals(currentUser.getId())) {
            throw new UnauthorizedAccessException("You are not authorized to view this lease");
        }
        if (currentUser.getRole() == Role.TENANT &&
                !lease.getTenant().getId().equals(currentUser.getId())) {
            throw new UnauthorizedAccessException("You are not authorized to view this lease");
        }

        return lease;
    }

    /**
     * Creates a new lease after validating date logic and overlapping lease prevention,
     * then generates the monthly payment schedule.
     */
    public Lease createLease(Lease lease) {
        validateLeaseRules(lease);

        // Save the lease first so it obtains a generated ID
        Lease savedLease = leaseRepository.save(lease);

        // Generate scheduled rent payment ledger entries
        generateRentSchedule(savedLease);

        return savedLease;
    }

    /**
     * Validates business constraints for a lease:
     * 1. End date must be strictly after start date.
     * 2. Due day must be between 1 and 28.
     * 3. No other active lease can overlap these dates on the same property.
     */
    public void validateLeaseRules(Lease lease) {
        if (lease.getStartDate() == null || lease.getEndDate() == null) {
            throw new BusinessRuleException("Both lease start date and end date are required");
        }

        if (!lease.getEndDate().isAfter(lease.getStartDate())) {
            throw new BusinessRuleException("Lease end date (" + lease.getEndDate() +
                    ") must be strictly after start date (" + lease.getStartDate() + ")");
        }

        if (lease.getDueDayOfMonth() == null || lease.getDueDayOfMonth() < 1 || lease.getDueDayOfMonth() > 28) {
            throw new BusinessRuleException("Rent due day must be between 1 and 28 of the month");
        }

        if (lease.getMonthlyRent() == null || lease.getMonthlyRent().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Monthly rent must be a positive amount");
        }

        // Check for overlapping active leases on the same property
        List<Lease> overlapping = leaseRepository.findOverlappingActiveLeases(
                lease.getProperty().getId(),
                lease.getStartDate(),
                lease.getEndDate(),
                lease.getId()
        );

        if (!overlapping.isEmpty()) {
            Lease conflict = overlapping.get(0);
            throw new BusinessRuleException("Property already has an active lease agreement from " +
                    conflict.getStartDate() + " to " + conflict.getEndDate() +
                    " (Lease #" + conflict.getId() + "). Active leases cannot overlap.");
        }
    }

    /**
     * Generates monthly rent payment records across the duration of the lease.
     * Safe against duplicate generation.
     */
    public void generateRentSchedule(Lease lease) {
        LocalDate currentMonth = lease.getStartDate().withDayOfMonth(1);
        LocalDate endMonth = lease.getEndDate().withDayOfMonth(1);
        LocalDate today = LocalDate.now();

        while (!currentMonth.isAfter(endMonth)) {
            String rentalPeriod = currentMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                    + " " + currentMonth.getYear();

            // Guard against duplicate payments for the same lease and period
            boolean alreadyExists = rentPaymentRepository.existsByLeaseIdAndRentalPeriod(lease.getId(), rentalPeriod);

            if (!alreadyExists) {
                // Determine due date in this month, capping at 28 to safely fit all months (February included)
                int dueDay = lease.getDueDayOfMonth() != null ? lease.getDueDayOfMonth() : 5;
                LocalDate dueDate = currentMonth.withDayOfMonth(Math.min(dueDay, currentMonth.lengthOfMonth()));

                // Determine initial status based on due date relative to today
                PaymentStatus status;
                if (dueDate.isBefore(today)) {
                    status = PaymentStatus.LATE;
                } else if (!dueDate.isAfter(today.plusDays(7))) {
                    status = PaymentStatus.PENDING;
                } else {
                    status = PaymentStatus.UPCOMING;
                }

                RentPayment payment = new RentPayment(
                        lease,
                        lease.getProperty(),
                        lease.getTenant(),
                        rentalPeriod,
                        dueDate,
                        lease.getMonthlyRent(),
                        BigDecimal.ZERO,
                        lease.getMonthlyRent(),
                        status
                );

                rentPaymentRepository.save(payment);
            }

            // Move to next month
            currentMonth = currentMonth.plusMonths(1);
        }
    }

    /**
     * Terminates an active lease safely.
     * Cancels future unpaid payments while preserving all paid and past payments.
     */
    public void terminateLease(Long leaseId) {
        Lease lease = getLeaseById(leaseId);
        lease.setStatus(LeaseStatus.TERMINATED);
        leaseRepository.save(lease);

        LocalDate today = LocalDate.now();
        List<RentPayment> payments = rentPaymentRepository.findByLeaseIdOrderByDueDateAsc(leaseId);

        for (RentPayment payment : payments) {
            // Only cancel future unpaid dues; never touch already paid or historical records
            if (payment.getStatus() != PaymentStatus.PAID && payment.getDueDate().isAfter(today)) {
                payment.setStatus(PaymentStatus.CANCELLED);
                payment.setNotes("Cancelled due to early lease termination on " + today);
                rentPaymentRepository.save(payment);
            }
        }
    }

    public List<Lease> getExpiringLeasesForLandlord(Long landlordId, int withinDays) {
        LocalDate today = LocalDate.now();
        LocalDate expiryLimit = today.plusDays(withinDays);
        return leaseRepository.findExpiringLeasesForLandlord(landlordId, today, expiryLimit);
    }

    public List<Lease> getAllExpiringLeases(int withinDays) {
        LocalDate today = LocalDate.now();
        LocalDate expiryLimit = today.plusDays(withinDays);
        return leaseRepository.findAllExpiringLeases(today, expiryLimit);
    }

    public long countActiveLeases() {
        return leaseRepository.countByStatus(LeaseStatus.ACTIVE);
    }

    public long countActiveLeasesForLandlord(Long landlordId) {
        return leaseRepository.countByPropertyLandlordIdAndStatus(landlordId, LeaseStatus.ACTIVE);
    }
}

