package com.rentroll.service;

import com.rentroll.entity.Property;
import com.rentroll.entity.PropertyType;
import com.rentroll.entity.Role;
import com.rentroll.entity.User;
import com.rentroll.exception.BusinessRuleException;
import com.rentroll.exception.ResourceNotFoundException;
import com.rentroll.exception.UnauthorizedAccessException;
import com.rentroll.repository.LeaseRepository;
import com.rentroll.repository.PropertyRepository;
import com.rentroll.repository.RentPaymentRepository;
import com.rentroll.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * Service managing real estate property listings and landlord authorization.
 */
@Service
@Transactional
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final LeaseRepository leaseRepository;
    private final RentPaymentRepository rentPaymentRepository;
    private final SecurityUtils securityUtils;

    public PropertyService(PropertyRepository propertyRepository,
                           LeaseRepository leaseRepository,
                           RentPaymentRepository rentPaymentRepository,
                           SecurityUtils securityUtils) {
        this.propertyRepository = propertyRepository;
        this.leaseRepository = leaseRepository;
        this.rentPaymentRepository = rentPaymentRepository;
        this.securityUtils = securityUtils;
    }

    /**
     * Retrieve properties based on user role and optional search query / type filter.
     */
    public List<Property> getPropertiesForCurrentUser(String search, PropertyType type) {
        User currentUser = securityUtils.getRequiredCurrentUser();

        String query = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        if (currentUser.getRole() == Role.ADMIN) {
            return propertyRepository.searchAllProperties(query, type);
        } else if (currentUser.getRole() == Role.LANDLORD) {
            return propertyRepository.searchLandlordProperties(currentUser.getId(), query, type);
        } else {
            // Tenant sees the property they are currently leasing
            return leaseRepository.findFirstByTenantIdAndStatus(currentUser.getId(), com.rentroll.entity.LeaseStatus.ACTIVE)
                    .map(lease -> Collections.singletonList(lease.getProperty()))
                    .orElse(Collections.emptyList());
        }
    }

    public Property getPropertyById(Long id) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + id));

        // Enforce ownership check for Landlords
        User currentUser = securityUtils.getRequiredCurrentUser();
        if (currentUser.getRole() == Role.LANDLORD && !property.getLandlord().getId().equals(currentUser.getId())) {
            throw new UnauthorizedAccessException("You do not have permission to access this property");
        }

        return property;
    }

    public Property createProperty(Property property, User landlord) {
        property.setLandlord(landlord);
        return propertyRepository.save(property);
    }

    public Property updateProperty(Long id, Property updatedData) {
        Property existing = getPropertyById(id);

        existing.setPropertyName(updatedData.getPropertyName());
        existing.setAddress(updatedData.getAddress());
        existing.setCity(updatedData.getCity());
        existing.setState(updatedData.getState());
        existing.setPinCode(updatedData.getPinCode());
        existing.setPropertyType(updatedData.getPropertyType());
        existing.setBedrooms(updatedData.getBedrooms());
        existing.setMonthlyRent(updatedData.getMonthlyRent());
        existing.setDescription(updatedData.getDescription());

        return propertyRepository.save(existing);
    }

    public void deleteProperty(Long id) {
        Property property = getPropertyById(id);

        // Safety rule: Prevent deletion if there are leases or payments attached
        long leaseCount = leaseRepository.findByPropertyId(id).size();
        if (leaseCount > 0) {
            throw new BusinessRuleException("Cannot delete property '" + property.getPropertyName() +
                    "' because it has " + leaseCount + " associated lease agreement(s). Terminate or remove leases first.");
        }

        long paymentCount = rentPaymentRepository.findByPropertyId(id).size();
        if (paymentCount > 0) {
            throw new BusinessRuleException("Cannot delete property '" + property.getPropertyName() +
                    "' because it has historical rent payment records.");
        }

        propertyRepository.delete(property);
    }

    public long countAllProperties() {
        return propertyRepository.count();
    }

    public long countPropertiesForLandlord(Long landlordId) {
        return propertyRepository.countByLandlordId(landlordId);
    }
}

