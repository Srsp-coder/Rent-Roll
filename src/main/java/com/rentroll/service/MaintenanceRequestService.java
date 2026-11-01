package com.rentroll.service;

import com.rentroll.entity.Lease;
import com.rentroll.entity.LeaseStatus;
import com.rentroll.entity.MaintenanceCategory;
import com.rentroll.entity.MaintenancePriority;
import com.rentroll.entity.MaintenanceRequest;
import com.rentroll.entity.MaintenanceStatus;
import com.rentroll.entity.Property;
import com.rentroll.entity.Role;
import com.rentroll.entity.User;
import com.rentroll.exception.BusinessRuleException;
import com.rentroll.exception.ResourceNotFoundException;
import com.rentroll.exception.UnauthorizedAccessException;
import com.rentroll.repository.LeaseRepository;
import com.rentroll.repository.MaintenanceRequestRepository;
import com.rentroll.repository.PropertyRepository;
import com.rentroll.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service managing property maintenance and tenant repair requests.
 */
@Service
@Transactional
public class MaintenanceRequestService {

    private final MaintenanceRequestRepository maintenanceRepository;
    private final PropertyRepository propertyRepository;
    private final LeaseRepository leaseRepository;
    private final SecurityUtils securityUtils;

    public MaintenanceRequestService(MaintenanceRequestRepository maintenanceRepository,
                                     PropertyRepository propertyRepository,
                                     LeaseRepository leaseRepository,
                                     SecurityUtils securityUtils) {
        this.maintenanceRepository = maintenanceRepository;
        this.propertyRepository = propertyRepository;
        this.leaseRepository = leaseRepository;
        this.securityUtils = securityUtils;
    }

    public List<MaintenanceRequest> getRequestsForCurrentUser(MaintenanceStatus status, MaintenancePriority priority) {
        User currentUser = securityUtils.getRequiredCurrentUser();

        if (currentUser.getRole() == Role.ADMIN) {
            return maintenanceRepository.filterAllRequests(status, priority);
        } else if (currentUser.getRole() == Role.LANDLORD) {
            return maintenanceRepository.filterLandlordRequests(currentUser.getId(), status, priority);
        } else {
            return maintenanceRepository.findByTenantIdOrderByCreatedAtDesc(currentUser.getId());
        }
    }

    public MaintenanceRequest getRequestById(Long id) {
        MaintenanceRequest request = maintenanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance request not found with id: " + id));

        User currentUser = securityUtils.getRequiredCurrentUser();
        if (currentUser.getRole() == Role.LANDLORD &&
                !request.getProperty().getLandlord().getId().equals(currentUser.getId())) {
            throw new UnauthorizedAccessException("You are not authorized to view this maintenance request");
        }
        if (currentUser.getRole() == Role.TENANT &&
                !request.getTenant().getId().equals(currentUser.getId())) {
            throw new UnauthorizedAccessException("You are not authorized to view this maintenance request");
        }

        return request;
    }

    /**
     * Creates a new maintenance ticket.
     * Enforces that tenants can only raise requests for properties they actively lease.
     */
    public MaintenanceRequest createRequest(Long propertyId, String title, String description,
                                            MaintenanceCategory category, MaintenancePriority priority) {
        User tenant = securityUtils.getRequiredCurrentUser();

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + propertyId));

        // If user is a tenant, verify they hold an active lease for this property
        if (tenant.getRole() == Role.TENANT) {
            boolean hasActiveLease = leaseRepository.findByPropertyId(propertyId).stream()
                    .anyMatch(l -> l.getTenant().getId().equals(tenant.getId()) && l.getStatus() == LeaseStatus.ACTIVE);

            if (!hasActiveLease) {
                throw new BusinessRuleException("You can only raise maintenance requests for properties where you hold an active lease agreement.");
            }
        }

        MaintenanceRequest request = new MaintenanceRequest(title, description, property, tenant, category, priority);
        return maintenanceRepository.save(request);
    }

    /**
     * Updates ticket status and logs resolution notes when resolved.
     */
    public MaintenanceRequest updateStatus(Long id, MaintenanceStatus newStatus, String resolutionNotes) {
        MaintenanceRequest request = getRequestById(id);
        User currentUser = securityUtils.getRequiredCurrentUser();

        // Only landlords and admins can update ticket progress or resolve tickets
        if (currentUser.getRole() == Role.TENANT) {
            throw new UnauthorizedAccessException("Tenants cannot alter ticket resolution status");
        }

        request.setStatus(newStatus);
        if (resolutionNotes != null && !resolutionNotes.isBlank()) {
            request.setResolutionNotes(resolutionNotes.trim());
        }

        if (newStatus == MaintenanceStatus.RESOLVED) {
            request.setResolvedAt(LocalDateTime.now());
        }

        return maintenanceRepository.save(request);
    }

    public long countOpenRequestsForLandlord(Long landlordId) {
        return maintenanceRepository.countByPropertyLandlordIdAndStatus(landlordId, MaintenanceStatus.OPEN);
    }

    public long countOpenRequestsForTenant(Long tenantId) {
        return maintenanceRepository.countByTenantIdAndStatus(tenantId, MaintenanceStatus.OPEN);
    }

    public long countAllOpenRequests() {
        return maintenanceRepository.countByStatus(MaintenanceStatus.OPEN);
    }
}

