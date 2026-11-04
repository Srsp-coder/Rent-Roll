package com.rentroll.service;

import com.rentroll.entity.PaymentStatus;
import com.rentroll.entity.RentPayment;
import com.rentroll.entity.Role;
import com.rentroll.entity.User;
import com.rentroll.exception.BusinessRuleException;
import com.rentroll.exception.ResourceNotFoundException;
import com.rentroll.exception.UnauthorizedAccessException;
import com.rentroll.repository.RentPaymentRepository;
import com.rentroll.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * Service managing monthly rent payments, late fees, receipt logging, and CSV exports.
 */
@Service
@Transactional
public class RentPaymentService {

    public static final BigDecimal LATE_FEE_RATE = new BigDecimal("0.05"); // 5% late fee

    private final RentPaymentRepository rentPaymentRepository;
    private final SecurityUtils securityUtils;

    public RentPaymentService(RentPaymentRepository rentPaymentRepository, SecurityUtils securityUtils) {
        this.rentPaymentRepository = rentPaymentRepository;
        this.securityUtils = securityUtils;
    }

    /**
     * Retrieve payments filtered by status and search keyword for the current user's role.
     */
    public List<RentPayment> getPaymentsForCurrentUser(PaymentStatus status, String search) {
        User currentUser = securityUtils.getRequiredCurrentUser();
        String query = (search != null && !search.trim().isEmpty()) ? search.trim().toLowerCase() : null;

        if (currentUser.getRole() == Role.ADMIN) {
            if (query != null) {
                return rentPaymentRepository.filterAllPayments(status, query);
            } else if (status != null) {
                return rentPaymentRepository.findByStatusOrderByDueDateDesc(status);
            } else {
                return rentPaymentRepository.findAllByOrderByDueDateDesc();
            }
        } else if (currentUser.getRole() == Role.LANDLORD) {
            if (query != null) {
                return rentPaymentRepository.filterLandlordPayments(currentUser.getId(), status, query);
            } else if (status != null) {
                return rentPaymentRepository.findByPropertyLandlordIdAndStatusOrderByDueDateDesc(currentUser.getId(), status);
            } else {
                return rentPaymentRepository.findByPropertyLandlordIdOrderByDueDateDesc(currentUser.getId());
            }
        } else {
            // Tenant sees their own payments
            return rentPaymentRepository.findByTenantIdOrderByDueDateDesc(currentUser.getId());
        }
    }

    public RentPayment getPaymentById(Long id) {
        RentPayment payment = rentPaymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rent payment record not found with id: " + id));

        User currentUser = securityUtils.getRequiredCurrentUser();
        if (currentUser.getRole() == Role.LANDLORD &&
                !payment.getProperty().getLandlord().getId().equals(currentUser.getId())) {
            throw new UnauthorizedAccessException("You are not authorized to view this payment");
        }
        if (currentUser.getRole() == Role.TENANT &&
                !payment.getTenant().getId().equals(currentUser.getId())) {
            throw new UnauthorizedAccessException("You are not authorized to view this payment");
        }

        return payment;
    }

    /**
     * Calculates 5% late fee on the base rent amount, rounded to 2 decimal places.
     */
    public BigDecimal calculateLateFee(BigDecimal baseRent) {
        if (baseRent == null) {
            return BigDecimal.ZERO;
        }
        return baseRent.multiply(LATE_FEE_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Records payment settlement by Landlord or Admin.
     * Enforces late fee rules and prevents duplicate settlements.
     */
    public RentPayment markPaymentAsPaid(Long paymentId, String paymentMethod, String notes, LocalDate paymentDate) {
        RentPayment payment = getPaymentById(paymentId);
        User currentUser = securityUtils.getRequiredCurrentUser();

        // Security check: Tenants cannot mark their own payments as paid
        if (currentUser.getRole() == Role.TENANT) {
            throw new UnauthorizedAccessException("Tenants are not permitted to mark payments as paid. " +
                    "Your landlord will confirm and record the payment.");
        }

        // Rule: Do not allow a payment to be marked paid twice
        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new BusinessRuleException("This payment was already marked as PAID on " + payment.getPaymentDate());
        }

        if (payment.getStatus() == PaymentStatus.CANCELLED) {
            throw new BusinessRuleException("Cannot mark a CANCELLED rent payment as paid.");
        }

        LocalDate effectivePaymentDate = paymentDate != null ? paymentDate : LocalDate.now();

        // Late fee rule: If overdue (status is LATE or due date has passed), apply 5% late fee if not already applied
        boolean isOverdue = payment.getStatus() == PaymentStatus.LATE ||
                (payment.getDueDate() != null && payment.getDueDate().isBefore(effectivePaymentDate));

        if (isOverdue && (payment.getLateFee() == null || payment.getLateFee().compareTo(BigDecimal.ZERO) == 0)) {
            BigDecimal fee = calculateLateFee(payment.getBaseRent());
            payment.setLateFee(fee);
            payment.setTotalAmount(payment.getBaseRent().add(fee));
        }

        payment.setStatus(PaymentStatus.PAID);
        payment.setPaymentDate(effectivePaymentDate);
        payment.setPaymentMethod(paymentMethod != null && !paymentMethod.isBlank() ? paymentMethod : "UPI / Direct Transfer");
        if (notes != null && !notes.isBlank()) {
            payment.setNotes(notes.trim());
        }

        return rentPaymentRepository.save(payment);
    }

    /**
     * Updates payment statuses where due date has passed and payment remains unpaid.
     */
    public void updateOverduePaymentStatuses() {
        LocalDate today = LocalDate.now();
        List<RentPayment> overdueList = rentPaymentRepository.findPaymentsNeedingLateStatus(today);
        for (RentPayment p : overdueList) {
            p.setStatus(PaymentStatus.LATE);
            rentPaymentRepository.save(p);
        }
    }

    /**
     * Generates CSV string for export of payments.
     */
    public String exportPaymentsCsv(List<RentPayment> payments) {
        StringBuilder csv = new StringBuilder();
        // CSV Header
        csv.append("Payment ID,Property,Tenant,Rental Period,Due Date,Base Rent (INR),Late Fee (INR),Total Amount (INR),Status,Payment Date,Payment Method,Notes\n");

        for (RentPayment p : payments) {
            csv.append(escapeCsv(String.valueOf(p.getId()))).append(",");
            csv.append(escapeCsv(p.getProperty().getPropertyName())).append(",");
            csv.append(escapeCsv(p.getTenant().getFullName())).append(",");
            csv.append(escapeCsv(p.getRentalPeriod())).append(",");
            csv.append(escapeCsv(p.getDueDate().toString())).append(",");
            csv.append(p.getBaseRent()).append(",");
            csv.append(p.getLateFee()).append(",");
            csv.append(p.getTotalAmount()).append(",");
            csv.append(escapeCsv(p.getStatus().name())).append(",");
            csv.append(escapeCsv(p.getPaymentDate() != null ? p.getPaymentDate().toString() : "")).append(",");
            csv.append(escapeCsv(p.getPaymentMethod() != null ? p.getPaymentMethod() : "")).append(",");
            csv.append(escapeCsv(p.getNotes() != null ? p.getNotes() : "")).append("\n");
        }

        return csv.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    public List<RentPayment> getRecentPaymentsForLandlord(Long landlordId) {
        return rentPaymentRepository.findTop5ByPropertyLandlordIdOrderByDueDateDesc(landlordId);
    }

    public List<RentPayment> getUpcomingDuesForLandlord(Long landlordId) {
        return rentPaymentRepository.findUpcomingDuesForLandlord(landlordId, LocalDate.now());
    }

    public long countOverduePaymentsForLandlord(Long landlordId) {
        return rentPaymentRepository.countOverduePaymentsForLandlord(landlordId, LocalDate.now());
    }

    public long countAllOverduePayments() {
        return rentPaymentRepository.countAllOverduePayments(LocalDate.now());
    }
}

