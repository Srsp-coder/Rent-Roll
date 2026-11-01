package com.rentroll.controller;

import com.rentroll.dto.LeaseFormDto;
import com.rentroll.entity.Lease;
import com.rentroll.entity.LeaseStatus;
import com.rentroll.entity.Property;
import com.rentroll.entity.RentPayment;
import com.rentroll.entity.Role;
import com.rentroll.entity.User;
import com.rentroll.exception.BusinessRuleException;
import com.rentroll.repository.RentPaymentRepository;
import com.rentroll.security.SecurityUtils;
import com.rentroll.service.LeaseService;
import com.rentroll.service.PropertyService;
import com.rentroll.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controller managing lease agreements and payment schedule reviews.
 */
@Controller
@RequestMapping("/leases")
public class LeaseController {

    private final LeaseService leaseService;
    private final PropertyService propertyService;
    private final UserService userService;
    private final RentPaymentRepository rentPaymentRepository;
    private final SecurityUtils securityUtils;

    public LeaseController(LeaseService leaseService,
                           PropertyService propertyService,
                           UserService userService,
                           RentPaymentRepository rentPaymentRepository,
                           SecurityUtils securityUtils) {
        this.leaseService = leaseService;
        this.propertyService = propertyService;
        this.userService = userService;
        this.rentPaymentRepository = rentPaymentRepository;
        this.securityUtils = securityUtils;
    }

    @GetMapping
    public String listLeases(Model model) {
        User currentUser = securityUtils.getRequiredCurrentUser();
        List<Lease> leases = leaseService.getLeasesForCurrentUser();

        model.addAttribute("leases", leases);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("activeNav", "leases");
        model.addAttribute("pageTitle", "Lease Agreements");

        return "leases/list";
    }

    @GetMapping("/{id}")
    public String viewLease(@PathVariable("id") Long id, Model model) {
        Lease lease = leaseService.getLeaseById(id);
        User currentUser = securityUtils.getRequiredCurrentUser();
        List<RentPayment> payments = rentPaymentRepository.findByLeaseIdOrderByDueDateAsc(id);

        model.addAttribute("lease", lease);
        model.addAttribute("payments", payments);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("activeNav", "leases");
        model.addAttribute("pageTitle", "Lease #" + lease.getId() + " - " + lease.getProperty().getPropertyName());

        return "leases/details";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        User currentUser = securityUtils.getRequiredCurrentUser();
        if (currentUser.getRole() == Role.TENANT) {
            return "redirect:/leases";
        }

        LeaseFormDto form = new LeaseFormDto();
        model.addAttribute("leaseForm", form);
        model.addAttribute("properties", propertyService.getPropertiesForCurrentUser(null, null));
        model.addAttribute("tenants", userService.getTenants());
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("activeNav", "leases");
        model.addAttribute("pageTitle", "Create Lease Agreement");

        return "leases/form";
    }

    @PostMapping("/new")
    public String createLease(@Valid @ModelAttribute("leaseForm") LeaseFormDto form,
                              BindingResult bindingResult,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        User currentUser = securityUtils.getRequiredCurrentUser();
        if (currentUser.getRole() == Role.TENANT) {
            return "redirect:/leases";
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("properties", propertyService.getPropertiesForCurrentUser(null, null));
            model.addAttribute("tenants", userService.getTenants());
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("activeNav", "leases");
            model.addAttribute("pageTitle", "Create Lease Agreement");
            return "leases/form";
        }

        try {
            Property property = propertyService.getPropertyById(form.getPropertyId());
            User tenant = userService.getUserById(form.getTenantId());

            Lease lease = new Lease(
                    property,
                    tenant,
                    form.getStartDate(),
                    form.getEndDate(),
                    form.getMonthlyRent(),
                    form.getDueDayOfMonth(),
                    form.getSecurityDeposit(),
                    LeaseStatus.ACTIVE
            );

            Lease saved = leaseService.createLease(lease);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Lease #" + saved.getId() + " created successfully with automated rent payment schedule!");

            return "redirect:/leases/" + saved.getId();
        } catch (BusinessRuleException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("properties", propertyService.getPropertiesForCurrentUser(null, null));
            model.addAttribute("tenants", userService.getTenants());
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("activeNav", "leases");
            model.addAttribute("pageTitle", "Create Lease Agreement");
            return "leases/form";
        }
    }

    @PostMapping("/{id}/terminate")
    public String terminateLease(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        User currentUser = securityUtils.getRequiredCurrentUser();
        if (currentUser.getRole() == Role.TENANT) {
            redirectAttributes.addFlashAttribute("errorMessage", "Tenants cannot terminate leases directly.");
            return "redirect:/leases/" + id;
        }

        leaseService.terminateLease(id);
        redirectAttributes.addFlashAttribute("successMessage",
                "Lease #" + id + " has been terminated. Future unpaid dues have been safely cancelled.");

        return "redirect:/leases/" + id;
    }
}

