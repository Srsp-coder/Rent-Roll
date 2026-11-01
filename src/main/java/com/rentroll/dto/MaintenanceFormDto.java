package com.rentroll.dto;

import com.rentroll.entity.MaintenanceCategory;
import com.rentroll.entity.MaintenancePriority;
import com.rentroll.entity.MaintenanceStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Form DTO for submitting and updating maintenance requests.
 */
public class MaintenanceFormDto {

    private Long id;

    @NotNull(message = "Please select the property")
    private Long propertyId;

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 150, message = "Title must be between 3 and 150 characters")
    private String title;

    @NotBlank(message = "Please describe the problem")
    @Size(min = 10, max = 2000, message = "Description must be at least 10 characters")
    private String description;

    @NotNull(message = "Please select a category")
    private MaintenanceCategory category;

    @NotNull(message = "Please select priority")
    private MaintenancePriority priority = MaintenancePriority.MEDIUM;

    private MaintenanceStatus status;
    private String resolutionNotes;

    public MaintenanceFormDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(Long propertyId) {
        this.propertyId = propertyId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public MaintenanceCategory getCategory() {
        return category;
    }

    public void setCategory(MaintenanceCategory category) {
        this.category = category;
    }

    public MaintenancePriority getPriority() {
        return priority;
    }

    public void setPriority(MaintenancePriority priority) {
        this.priority = priority;
    }

    public MaintenanceStatus getStatus() {
        return status;
    }

    public void setStatus(MaintenanceStatus status) {
        this.status = status;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }
}

