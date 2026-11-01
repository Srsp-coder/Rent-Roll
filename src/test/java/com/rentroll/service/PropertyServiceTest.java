package com.rentroll.service;

import com.rentroll.entity.Property;
import com.rentroll.entity.PropertyType;
import com.rentroll.entity.Role;
import com.rentroll.entity.User;
import com.rentroll.exception.BusinessRuleException;
import com.rentroll.exception.UnauthorizedAccessException;
import com.rentroll.repository.LeaseRepository;
import com.rentroll.repository.PropertyRepository;
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
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PropertyServiceTest {

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private LeaseRepository leaseRepository;

    @Mock
    private RentPaymentRepository rentPaymentRepository;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private PropertyService propertyService;

    private User landlordUser;
    private Property sampleProperty;

    @BeforeEach
    void setUp() {
        landlordUser = new User("Rajesh Verma", "rajesh@rentroll.in", "pass", "+919876543210", Role.LANDLORD);
        landlordUser.setId(10L);

        sampleProperty = new Property(
                "Prestige Shantiniketan 702",
                "ITPL Main Road",
                "Bengaluru",
                "Karnataka",
                "560066",
                PropertyType.APARTMENT,
                3,
                new BigDecimal("42000.00"),
                "3BHK flat",
                landlordUser
        );
        sampleProperty.setId(100L);
    }

    @Test
    @DisplayName("Should create property and associate with landlord")
    void testCreateProperty() {
        when(propertyRepository.save(any(Property.class))).thenReturn(sampleProperty);

        Property created = propertyService.createProperty(sampleProperty, landlordUser);

        assertNotNull(created);
        assertEquals("Prestige Shantiniketan 702", created.getPropertyName());
        assertEquals(landlordUser, created.getLandlord());
        verify(propertyRepository).save(sampleProperty);
    }

    @Test
    @DisplayName("Should prevent another landlord from accessing property they do not own")
    void testLandlordAccessControl() {
        User otherLandlord = new User("Other Owner", "other@rentroll.in", "pass", "+919876543219", Role.LANDLORD);
        otherLandlord.setId(20L); // Different ID

        when(propertyRepository.findById(100L)).thenReturn(Optional.of(sampleProperty));
        when(securityUtils.getRequiredCurrentUser()).thenReturn(otherLandlord);

        assertThrows(UnauthorizedAccessException.class, () -> propertyService.getPropertyById(100L));
    }

    @Test
    @DisplayName("Should prevent deleting property when it has active leases")
    void testPreventDeleteWhenLeaseExists() {
        when(propertyRepository.findById(100L)).thenReturn(Optional.of(sampleProperty));
        when(securityUtils.getRequiredCurrentUser()).thenReturn(landlordUser);
        when(leaseRepository.findByPropertyId(100L)).thenReturn(Collections.singletonList(new com.rentroll.entity.Lease()));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> propertyService.deleteProperty(100L));
        assertEquals(true, ex.getMessage().contains("Cannot delete property"));
    }
}

