package com.rentroll.controller;

import com.rentroll.dto.PropertyFormDto;
import com.rentroll.entity.Property;
import com.rentroll.entity.PropertyType;
import com.rentroll.entity.Role;
import com.rentroll.entity.User;
import com.rentroll.exception.BusinessRuleException;
import com.rentroll.security.SecurityUtils;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controller managing property listings, additions, modifications, and removals.
 */
@Controller
@RequestMapping("/properties")
public class PropertyController {

    private final PropertyService propertyService;
    private final UserService userService;
    private final SecurityUtils securityUtils;

    public PropertyController(PropertyService propertyService,
                              UserService userService,
                              SecurityUtils securityUtils) {
        this.propertyService = propertyService;
        this.userService = userService;
        this.securityUtils = securityUtils;
    }

    @GetMapping
    public String listProperties(@RequestParam(value = "search", required = false) String search,
                                 @RequestParam(value = "type", required = false) PropertyType propertyType,
                                 Model model) {
        User currentUser = securityUtils.getRequiredCurrentUser();
        List<Property> properties = propertyService.getPropertiesForCurrentUser(search, propertyType);

        model.addAttribute("properties", properties);
        model.addAttribute("propertyTypes", PropertyType.values());
        model.addAttribute("search", search);
        model.addAttribute("selectedType", propertyType);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("activeNav", "properties");
        model.addAttribute("pageTitle", "Properties");

        return "properties/list";
    }

    @GetMapping("/{id}")
    public String viewProperty(@PathVariable("id") Long id, Model model) {
        Property property = propertyService.getPropertyById(id);
        User currentUser = securityUtils.getRequiredCurrentUser();

        model.addAttribute("property", property);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("activeNav", "properties");
        model.addAttribute("pageTitle", property.getPropertyName());

        return "properties/details";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        User currentUser = securityUtils.getRequiredCurrentUser();
        if (currentUser.getRole() == Role.TENANT) {
            return "redirect:/properties";
        }

        PropertyFormDto form = new PropertyFormDto();
        model.addAttribute("propertyForm", form);
        model.addAttribute("propertyTypes", PropertyType.values());
        model.addAttribute("landlords", userService.getLandlords());
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("activeNav", "properties");
        model.addAttribute("pageTitle", "Add New Property");

        return "properties/form";
    }

    @PostMapping("/new")
    public String createProperty(@Valid @ModelAttribute("propertyForm") PropertyFormDto form,
                                 BindingResult bindingResult,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        User currentUser = securityUtils.getRequiredCurrentUser();
        if (currentUser.getRole() == Role.TENANT) {
            return "redirect:/properties";
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("propertyTypes", PropertyType.values());
            model.addAttribute("landlords", userService.getLandlords());
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("activeNav", "properties");
            model.addAttribute("pageTitle", "Add New Property");
            return "properties/form";
        }

        User landlord;
        if (currentUser.getRole() == Role.ADMIN && form.getLandlordId() != null) {
            landlord = userService.getUserById(form.getLandlordId());
        } else {
            landlord = currentUser;
        }

        Property property = new Property(
                form.getPropertyName(),
                form.getAddress(),
                form.getCity(),
                form.getState(),
                form.getPinCode(),
                form.getPropertyType(),
                form.getBedrooms(),
                form.getMonthlyRent(),
                form.getDescription(),
                landlord
        );

        Property saved = propertyService.createProperty(property, landlord);
        redirectAttributes.addFlashAttribute("successMessage",
                "Property '" + saved.getPropertyName() + "' added successfully!");

        return "redirect:/properties/" + saved.getId();
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        User currentUser = securityUtils.getRequiredCurrentUser();
        if (currentUser.getRole() == Role.TENANT) {
            return "redirect:/properties";
        }

        Property property = propertyService.getPropertyById(id);

        PropertyFormDto form = new PropertyFormDto();
        form.setId(property.getId());
        form.setPropertyName(property.getPropertyName());
        form.setAddress(property.getAddress());
        form.setCity(property.getCity());
        form.setState(property.getState());
        form.setPinCode(property.getPinCode());
        form.setPropertyType(property.getPropertyType());
        form.setBedrooms(property.getBedrooms());
        form.setMonthlyRent(property.getMonthlyRent());
        form.setDescription(property.getDescription());

        model.addAttribute("propertyForm", form);
        model.addAttribute("propertyTypes", PropertyType.values());
        model.addAttribute("isEdit", true);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("activeNav", "properties");
        model.addAttribute("pageTitle", "Edit " + property.getPropertyName());

        return "properties/form";
    }

    @PostMapping("/{id}/edit")
    public String updateProperty(@PathVariable("id") Long id,
                                 @Valid @ModelAttribute("propertyForm") PropertyFormDto form,
                                 BindingResult bindingResult,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        User currentUser = securityUtils.getRequiredCurrentUser();
        if (currentUser.getRole() == Role.TENANT) {
            return "redirect:/properties";
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("propertyTypes", PropertyType.values());
            model.addAttribute("isEdit", true);
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("activeNav", "properties");
            model.addAttribute("pageTitle", "Edit Property");
            return "properties/form";
        }

        Property updatedData = new Property();
        updatedData.setPropertyName(form.getPropertyName());
        updatedData.setAddress(form.getAddress());
        updatedData.setCity(form.getCity());
        updatedData.setState(form.getState());
        updatedData.setPinCode(form.getPinCode());
        updatedData.setPropertyType(form.getPropertyType());
        updatedData.setBedrooms(form.getBedrooms());
        updatedData.setMonthlyRent(form.getMonthlyRent());
        updatedData.setDescription(form.getDescription());

        propertyService.updateProperty(id, updatedData);
        redirectAttributes.addFlashAttribute("successMessage", "Property details updated successfully!");

        return "redirect:/properties/" + id;
    }

    @PostMapping("/{id}/delete")
    public String deleteProperty(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            propertyService.deleteProperty(id);
            redirectAttributes.addFlashAttribute("successMessage", "Property deleted successfully.");
        } catch (BusinessRuleException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/properties/" + id;
        }
        return "redirect:/properties";
    }
}

