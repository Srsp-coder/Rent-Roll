package com.rentroll.controller;

import com.rentroll.dto.MaintenanceFormDto;
import com.rentroll.entity.MaintenanceCategory;
import com.rentroll.entity.MaintenancePriority;
import com.rentroll.entity.MaintenanceRequest;
import com.rentroll.entity.MaintenanceStatus;
import com.rentroll.entity.Role;
import com.rentroll.entity.User;
import com.rentroll.exception.BusinessRuleException;
import com.rentroll.security.SecurityUtils;
import com.rentroll.service.MaintenanceRequestService;
import com.rentroll.service.PropertyService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controller managing tenant maintenance requests and landlord resolution workflows.
 */
@Controller
@RequestMapping("/maintenance")
public class MaintenanceController {

    private final MaintenanceRequestService maintenanceService;
    private final PropertyService propertyService;
    private final SecurityUtils securityUtils;

    public MaintenanceController(MaintenanceRequestService maintenanceService,
                                 PropertyService propertyService,
                                 SecurityUtils securityUtils) {
        this.maintenanceService = maintenanceService;
        this.propertyService = propertyService;
        this.securityUtils = securityUtils;
    }

    @GetMapping
    public String listRequests(@RequestParam(value = "status", required = false) MaintenanceStatus status,
                               @RequestParam(value = "priority", required = false) MaintenancePriority priority,
                               Model model) {
        User currentUser = securityUtils.getRequiredCurrentUser();
        List<MaintenanceRequest> requests = maintenanceService.getRequestsForCurrentUser(status, priority);

        model.addAttribute("requests", requests);
        model.addAttribute("statuses", MaintenanceStatus.values());
        model.addAttribute("priorities", MaintenancePriority.values());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedPriority", priority);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("activeNav", "maintenance");
        model.addAttribute("pageTitle", "Maintenance & Repairs");

        return "maintenance/list";
    }

    @GetMapping("/{id}")
    public String viewRequest(@PathVariable("id") Long id, Model model) {
        MaintenanceRequest request = maintenanceService.getRequestById(id);
        User currentUser = securityUtils.getRequiredCurrentUser();

        model.addAttribute("request", request);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("statuses", MaintenanceStatus.values());
        model.addAttribute("activeNav", "maintenance");
        model.addAttribute("pageTitle", "Ticket #" + request.getId() + " - " + request.getTitle());

        return "maintenance/details";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        User currentUser = securityUtils.getRequiredCurrentUser();
        MaintenanceFormDto form = new MaintenanceFormDto();

        model.addAttribute("maintenanceForm", form);
        model.addAttribute("properties", propertyService.getPropertiesForCurrentUser(null, null));
        model.addAttribute("categories", MaintenanceCategory.values());
        model.addAttribute("priorities", MaintenancePriority.values());
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("activeNav", "maintenance");
        model.addAttribute("pageTitle", "Raise Maintenance Request");

        return "maintenance/form";
    }

    @PostMapping("/new")
    public String createRequest(@Valid @ModelAttribute("maintenanceForm") MaintenanceFormDto form,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        User currentUser = securityUtils.getRequiredCurrentUser();

        if (bindingResult.hasErrors()) {
            model.addAttribute("properties", propertyService.getPropertiesForCurrentUser(null, null));
            model.addAttribute("categories", MaintenanceCategory.values());
            model.addAttribute("priorities", MaintenancePriority.values());
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("activeNav", "maintenance");
            model.addAttribute("pageTitle", "Raise Maintenance Request");
            return "maintenance/form";
        }

        try {
            MaintenanceRequest created = maintenanceService.createRequest(
                    form.getPropertyId(),
                    form.getTitle(),
                    form.getDescription(),
                    form.getCategory(),
                    form.getPriority()
            );

            redirectAttributes.addFlashAttribute("successMessage",
                    "Maintenance ticket #" + created.getId() + " submitted successfully.");
            return "redirect:/maintenance/" + created.getId();
        } catch (BusinessRuleException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("properties", propertyService.getPropertiesForCurrentUser(null, null));
            model.addAttribute("categories", MaintenanceCategory.values());
            model.addAttribute("priorities", MaintenancePriority.values());
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("activeNav", "maintenance");
            model.addAttribute("pageTitle", "Raise Maintenance Request");
            return "maintenance/form";
        }
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable("id") Long id,
                               @RequestParam("status") MaintenanceStatus status,
                               @RequestParam(value = "resolutionNotes", required = false) String resolutionNotes,
                               RedirectAttributes redirectAttributes) {
        User currentUser = securityUtils.getRequiredCurrentUser();
        if (currentUser.getRole() == Role.TENANT) {
            redirectAttributes.addFlashAttribute("errorMessage", "Tenants cannot change maintenance ticket status.");
            return "redirect:/maintenance/" + id;
        }

        maintenanceService.updateStatus(id, status, resolutionNotes);
        redirectAttributes.addFlashAttribute("successMessage", "Ticket #" + id + " updated to " + status.getDisplayName());

        return "redirect:/maintenance/" + id;
    }
}

