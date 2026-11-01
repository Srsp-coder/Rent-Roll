package com.rentroll.service;

import com.rentroll.entity.Lease;
import com.rentroll.entity.LeaseStatus;
import com.rentroll.entity.PaymentStatus;
import com.rentroll.entity.Property;
import com.rentroll.entity.PropertyType;
import com.rentroll.entity.RentPayment;
import com.rentroll.entity.Role;
import com.rentroll.entity.User;
import com.rentroll.exception.BusinessRuleException;
import com.rentroll.exception.UnauthorizedAccessException;
import com.rentroll.repository.RentPaymentRepository;
import com.rentroll.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RentPaymentServiceTest {

    @Mock
    private RentPaymentRepository rentPaymentRepository;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private RentPaymentService paymentService;

    private User landlord;
    private User tenant;
    private Property property;
    private Lease lease;

    @BeforeEach
    void setUp() {
        landlord = new User("Rajesh Verma", "rajesh@rentroll.in", "pass", "+919876543210", Role.LANDLORD);
        landlord.setId(1L);

        tenant = new User("Amit Kumar", "amit@rentroll.in", "pass", "+919900112233", Role.TENANT);
        tenant.setId(2L);

        property = new Property("Prestige Flat 702", "Whitefield", "Bengaluru", "KA", "560066",
                PropertyType.APARTMENT, 2, new BigDecimal("40000.00"), "2BHK", landlord);
        property.setId(10L);

        lease = new Lease(property, tenant, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                new BigDecimal("40000.00"), 5, BigDecimal.ZERO, LeaseStatus.ACTIVE);
        lease.setId(100L);
    }

    @Test
    @DisplayName("Should correctly calculate 5% late fee using BigDecimal")
    void testCalculateLateFee() {
        BigDecimal baseRent = new BigDecimal("40000.00");
        BigDecimal expectedLateFee = new BigDecimal("2000.00"); // 5% of 40,000

        BigDecimal lateFee = paymentService.calculateLateFee(baseRent);

        assertEquals(0, expectedLateFee.compareTo(lateFee));
    }

    @Test
    @DisplayName("Should apply 5% late fee when overdue payment is marked as paid")
    void testApplyLateFeeWhenOverduePaymentIsPaid() {
        // Due 15 days ago -> overdue
        LocalDate pastDue = LocalDate.now().minusDays(15);
        RentPayment overduePayment = new RentPayment(
                lease, property, tenant, "Last Month", pastDue,
                new BigDecimal("40000.00"), BigDecimal.ZERO, new BigDecimal("40000.00"), PaymentStatus.LATE
        );
        overduePayment.setId(500L);

        when(rentPaymentRepository.findById(500L)).thenReturn(Optional.of(overduePayment));
        when(securityUtils.getRequiredCurrentUser()).thenReturn(landlord);
        when(rentPaymentRepository.save(any(RentPayment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RentPayment paid = paymentService.markPaymentAsPaid(500L, "UPI", "Ref #1234", LocalDate.now());

        assertNotNull(paid);
        assertEquals(PaymentStatus.PAID, paid.getStatus());
        assertEquals(0, new BigDecimal("2000.00").compareTo(paid.getLateFee())); // 5% applied
        assertEquals(0, new BigDecimal("42000.00").compareTo(paid.getTotalAmount())); // 40000 + 2000
    }

    @Test
    @DisplayName("Should not allow marking an already paid payment as paid again")
    void testPreventDuplicatePaymentSettlement() {
        RentPayment alreadyPaid = new RentPayment(
                lease, property, tenant, "January 2026", LocalDate.of(2026, 1, 5),
                new BigDecimal("40000.00"), BigDecimal.ZERO, new BigDecimal("40000.00"), PaymentStatus.PAID
        );
        alreadyPaid.setId(501L);
        alreadyPaid.setPaymentDate(LocalDate.of(2026, 1, 4));

        when(rentPaymentRepository.findById(501L)).thenReturn(Optional.of(alreadyPaid));
        when(securityUtils.getRequiredCurrentUser()).thenReturn(landlord);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> paymentService.markPaymentAsPaid(501L, "UPI", "Notes", LocalDate.now()));
        assertEquals(true, ex.getMessage().contains("already marked as PAID"));
    }

    @Test
    @DisplayName("Should strictly forbid tenants from marking their own payments as paid")
    void testTenantCannotMarkAsPaid() {
        RentPayment payment = new RentPayment(
                lease, property, tenant, "Current Month", LocalDate.now().plusDays(2),
                new BigDecimal("40000.00"), BigDecimal.ZERO, new BigDecimal("40000.00"), PaymentStatus.PENDING
        );
        payment.setId(502L);

        when(rentPaymentRepository.findById(502L)).thenReturn(Optional.of(payment));
        when(securityUtils.getRequiredCurrentUser()).thenReturn(tenant); // Tenant logged in

        assertThrows(UnauthorizedAccessException.class,
                () -> paymentService.markPaymentAsPaid(502L, "UPI", "Self-confirm", LocalDate.now()));
    }
}

