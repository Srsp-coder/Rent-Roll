package com.rentroll.service;

import com.rentroll.entity.Lease;
import com.rentroll.entity.LeaseStatus;
import com.rentroll.entity.MaintenanceCategory;
import com.rentroll.entity.MaintenancePriority;
import com.rentroll.entity.MaintenanceRequest;
import com.rentroll.entity.MaintenanceStatus;
import com.rentroll.entity.Property;
import com.rentroll.entity.PropertyType;
import com.rentroll.entity.Role;
import com.rentroll.entity.User;
import com.rentroll.exception.BusinessRuleException;
import com.rentroll.exception.UnauthorizedAccessException;
import com.rentroll.repository.LeaseRepository;
import com.rentroll.repository.MaintenanceRequestRepository;
import com.rentroll.repository.PropertyRepository;
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
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaintenanceRequestServiceTest {

    @Mock
    private MaintenanceRequestRepository maintenanceRepository;

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private LeaseRepository leaseRepository;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private MaintenanceRequestService maintenanceService;

    private User landlord;
    private User tenant;
    private Property property;
    private Lease activeLease;

    @BeforeEach
    void setUp() {
        landlord = new User("Rajesh Verma", "rajesh@rentroll.in", "pass", "+919876543210", Role.LANDLORD);
        landlord.setId(1L);

        tenant = new User("Amit Kumar", "amit@rentroll.in", "pass", "+919900112233", Role.TENANT);
        tenant.setId(2L);

        property = new Property("Prestige Flat 702", "Whitefield", "Bengaluru", "KA", "560066",
                PropertyType.APARTMENT, 2, new BigDecimal("40000.00"), "2BHK", landlord);
        property.setId(10L);

        activeLease = new Lease(property, tenant, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                new BigDecimal("40000.00"), 5, BigDecimal.ZERO, LeaseStatus.ACTIVE);
    }

    @Test
    @DisplayName("Tenant holding an active lease can submit maintenance ticket")
    void testTenantCreateMaintenanceRequestSuccess() {
        when(securityUtils.getRequiredCurrentUser()).thenReturn(tenant);
        when(propertyRepository.findById(10L)).thenReturn(Optional.of(property));
        when(leaseRepository.findByPropertyId(10L)).thenReturn(Collections.singletonList(activeLease));

        MaintenanceRequest mockSaved = new MaintenanceRequest(
                "Leaking kitchen sink", "Water dripping constantly", property, tenant,
                MaintenanceCategory.PLUMBING, MaintenancePriority.MEDIUM
        );
        mockSaved.setId(1L);

        when(maintenanceRepository.save(any(MaintenanceRequest.class))).thenReturn(mockSaved);

        MaintenanceRequest result = maintenanceService.createRequest(
                10L, "Leaking kitchen sink", "Water dripping constantly",
                MaintenanceCategory.PLUMBING, MaintenancePriority.MEDIUM
        );

        assertNotNull(result);
        assertEquals("Leaking kitchen sink", result.getTitle());
        verify(maintenanceRepository).save(any(MaintenanceRequest.class));
    }

    @Test
    @DisplayName("Tenant cannot submit ticket for property they do not lease")
    void testTenantCannotSubmitForUnleasedProperty() {
        when(securityUtils.getRequiredCurrentUser()).thenReturn(tenant);
        when(propertyRepository.findById(10L)).thenReturn(Optional.of(property));
        // No active leases for this tenant on this property
        when(leaseRepository.findByPropertyId(10L)).thenReturn(Collections.emptyList());

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> maintenanceService.createRequest(
                        10L, "Broken Door", "Door latch broken",
                        MaintenanceCategory.STRUCTURAL, MaintenancePriority.LOW
                ));
        assertEquals(true, ex.getMessage().contains("active lease agreement"));
    }

    @Test
    @DisplayName("Landlord can update ticket status and log resolution notes")
    void testLandlordUpdateStatus() {
        MaintenanceRequest req = new MaintenanceRequest(
                "Leaking tap", "Mixer is dripping", property, tenant,
                MaintenanceCategory.PLUMBING, MaintenancePriority.LOW
        );
        req.setId(77L);

        when(maintenanceRepository.findById(77L)).thenReturn(Optional.of(req));
        when(securityUtils.getRequiredCurrentUser()).thenReturn(landlord);
        when(maintenanceRepository.save(any(MaintenanceRequest.class))).thenAnswer(i -> i.getArgument(0));

        MaintenanceRequest updated = maintenanceService.updateStatus(77L, MaintenanceStatus.RESOLVED, "Plumber changed valve");

        assertEquals(MaintenanceStatus.RESOLVED, updated.getStatus());
        assertEquals("Plumber changed valve", updated.getResolutionNotes());
        assertNotNull(updated.getResolvedAt());
    }

    @Test
    @DisplayName("Tenant cannot alter ticket status")
    void testTenantCannotAlterStatus() {
        MaintenanceRequest req = new MaintenanceRequest(
                "Leaking tap", "Mixer is dripping", property, tenant,
                MaintenanceCategory.PLUMBING, MaintenancePriority.LOW
        );
        req.setId(77L);

        when(maintenanceRepository.findById(77L)).thenReturn(Optional.of(req));
        when(securityUtils.getRequiredCurrentUser()).thenReturn(tenant);

        assertThrows(UnauthorizedAccessException.class,
                () -> maintenanceService.updateStatus(77L, MaintenanceStatus.RESOLVED, "Self resolved"));
    }
}

