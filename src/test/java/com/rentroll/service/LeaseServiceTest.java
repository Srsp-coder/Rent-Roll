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
import com.rentroll.repository.LeaseRepository;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaseServiceTest {

    @Mock
    private LeaseRepository leaseRepository;

    @Mock
    private RentPaymentRepository rentPaymentRepository;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private LeaseService leaseService;

    private Property property;
    private User tenant;
    private User landlord;

    @BeforeEach
    void setUp() {
        landlord = new User("Rajesh Verma", "rajesh@rentroll.in", "pass", "+919876543210", Role.LANDLORD);
        landlord.setId(1L);

        tenant = new User("Amit Kumar", "amit@rentroll.in", "pass", "+919900112233", Role.TENANT);
        tenant.setId(2L);

        property = new Property("Prestige Flat 702", "Whitefield", "Bengaluru", "KA", "560066",
                PropertyType.APARTMENT, 2, new BigDecimal("40000.00"), "2BHK", landlord);
        property.setId(10L);
    }

    @Test
    @DisplayName("Should reject lease if end date is before or equal to start date")
    void testValidateLeaseDates() {
        Lease invalidLease = new Lease(
                property, tenant,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 5, 1), // Invalid: before start date
                new BigDecimal("40000.00"), 5, BigDecimal.ZERO, LeaseStatus.ACTIVE
        );

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> leaseService.validateLeaseRules(invalidLease));
        assertEquals(true, ex.getMessage().contains("must be strictly after start date"));
    }

    @Test
    @DisplayName("Should reject lease if due day is not between 1 and 28")
    void testValidateDueDay() {
        Lease invalidDayLease = new Lease(
                property, tenant,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                new BigDecimal("40000.00"), 30, // Invalid: day 30
                BigDecimal.ZERO, LeaseStatus.ACTIVE
        );

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> leaseService.validateLeaseRules(invalidDayLease));
        assertEquals(true, ex.getMessage().contains("Rent due day must be between 1 and 28"));
    }

    @Test
    @DisplayName("Should prevent overlapping active leases on the same property")
    void testPreventOverlappingLeases() {
        Lease existingLease = new Lease(property, tenant,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30),
                new BigDecimal("40000.00"), 5, BigDecimal.ZERO, LeaseStatus.ACTIVE);
        existingLease.setId(55L);

        when(leaseRepository.findOverlappingActiveLeases(eq(10L), any(LocalDate.class), any(LocalDate.class), any()))
                .thenReturn(Collections.singletonList(existingLease));

        Lease newOverlappingLease = new Lease(property, tenant,
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 10, 31), // Overlaps April-June
                new BigDecimal("40000.00"), 5, BigDecimal.ZERO, LeaseStatus.ACTIVE);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> leaseService.validateLeaseRules(newOverlappingLease));
        assertEquals(true, ex.getMessage().contains("Property already has an active lease agreement"));
    }

    @Test
    @DisplayName("Should generate monthly rent schedule for 6 months duration")
    void testMonthlyRentScheduleGeneration() {
        Lease lease = new Lease(property, tenant,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30),
                new BigDecimal("40000.00"), 5, BigDecimal.ZERO, LeaseStatus.ACTIVE);
        lease.setId(100L);

        when(rentPaymentRepository.existsByLeaseIdAndRentalPeriod(anyLong(), anyString())).thenReturn(false);

        leaseService.generateRentSchedule(lease);

        // 6 months between Jan 2026 and June 2026 inclusive -> 6 saves
        verify(rentPaymentRepository, times(6)).save(any(RentPayment.class));
    }

    @Test
    @DisplayName("Should prevent duplicate payments when schedule generation is repeated")
    void testDuplicatePaymentPrevention() {
        Lease lease = new Lease(property, tenant,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31),
                new BigDecimal("40000.00"), 5, BigDecimal.ZERO, LeaseStatus.ACTIVE);
        lease.setId(100L);

        // First month already exists, next 2 don't
        when(rentPaymentRepository.existsByLeaseIdAndRentalPeriod(100L, "January 2026")).thenReturn(true);
        when(rentPaymentRepository.existsByLeaseIdAndRentalPeriod(100L, "February 2026")).thenReturn(false);
        when(rentPaymentRepository.existsByLeaseIdAndRentalPeriod(100L, "March 2026")).thenReturn(false);

        leaseService.generateRentSchedule(lease);

        // Only 2 new records should be saved, Jan is skipped
        verify(rentPaymentRepository, times(2)).save(any(RentPayment.class));
    }

    @Test
    @DisplayName("Safe termination: cancels only future unpaid payments and preserves paid records")
    void testSafeLeaseTermination() {
        Lease lease = new Lease(property, tenant,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                new BigDecimal("40000.00"), 5, BigDecimal.ZERO, LeaseStatus.ACTIVE);
        lease.setId(200L);

        when(leaseRepository.findById(200L)).thenReturn(Optional.of(lease));
        when(securityUtils.getRequiredCurrentUser()).thenReturn(landlord);

        // 1 past paid payment, 1 future unpaid payment
        RentPayment paidPayment = new RentPayment(lease, property, tenant, "January 2026",
                LocalDate.of(2026, 1, 5), new BigDecimal("40000.00"), BigDecimal.ZERO, new BigDecimal("40000.00"), PaymentStatus.PAID);

        RentPayment futurePayment = new RentPayment(lease, property, tenant, "December 2026",
                LocalDate.now().plusMonths(2), new BigDecimal("40000.00"), BigDecimal.ZERO, new BigDecimal("40000.00"), PaymentStatus.UPCOMING);

        List<RentPayment> list = new ArrayList<>();
        list.add(paidPayment);
        list.add(futurePayment);

        when(rentPaymentRepository.findByLeaseIdOrderByDueDateAsc(200L)).thenReturn(list);

        leaseService.terminateLease(200L);

        assertEquals(LeaseStatus.TERMINATED, lease.getStatus());
        assertEquals(PaymentStatus.PAID, paidPayment.getStatus()); // Untouched
        assertEquals(PaymentStatus.CANCELLED, futurePayment.getStatus()); // Cancelled
    }
}

