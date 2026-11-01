package com.rentroll.controller;

import com.rentroll.dto.UserFormDto;
import com.rentroll.entity.Role;
import com.rentroll.entity.User;
import com.rentroll.exception.BusinessRuleException;
import com.rentroll.security.SecurityUtils;
import com.rentroll.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
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
 * Controller allowing administrators to manage user accounts and system access.
 */
@Controller
@RequestMapping("/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;
    private final SecurityUtils securityUtils;

    public UserController(UserService userService, SecurityUtils securityUtils) {
        this.userService = userService;
        this.securityUtils = securityUtils;
    }

    @GetMapping
    public String listUsers(Model model) {
        User currentUser = securityUtils.getRequiredCurrentUser();
        List<User> users = userService.getAllUsers();

        model.addAttribute("users", users);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("activeNav", "users");
        model.addAttribute("pageTitle", "User Accounts Management");

        return "users/list";
    }

    @GetMapping("/{id}")
    public String viewUser(@PathVariable("id") Long id, Model model) {
        User user = userService.getUserById(id);
        User currentUser = securityUtils.getRequiredCurrentUser();

        model.addAttribute("user", user);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("activeNav", "users");
        model.addAttribute("pageTitle", "User Profile - " + user.getFullName());

        return "users/details";
    }

    @GetMapping("/new")
    public String showCreateUserForm(Model model) {
        User currentUser = securityUtils.getRequiredCurrentUser();
        UserFormDto form = new UserFormDto();

        model.addAttribute("userForm", form);
        model.addAttribute("roles", Role.values());
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("activeNav", "users");
        model.addAttribute("pageTitle", "Create User Account");

        return "users/form";
    }

    @PostMapping("/new")
    public String createUser(@Valid @ModelAttribute("userForm") UserFormDto form,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        User currentUser = securityUtils.getRequiredCurrentUser();

        if (bindingResult.hasErrors()) {
            model.addAttribute("roles", Role.values());
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("activeNav", "users");
            model.addAttribute("pageTitle", "Create User Account");
            return "users/form";
        }

        try {
            User newUser = new User(
                    form.getFullName(),
                    form.getEmail(),
                    "", // password will be encoded in service
                    form.getPhoneNumber(),
                    form.getRole()
            );

            User saved = userService.createUser(newUser, form.getPassword());
            redirectAttributes.addFlashAttribute("successMessage",
                    "User account for " + saved.getFullName() + " created successfully!");

            return "redirect:/users/" + saved.getId();
        } catch (BusinessRuleException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("roles", Role.values());
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("activeNav", "users");
            model.addAttribute("pageTitle", "Create User Account");
            return "users/form";
        }
    }

    @PostMapping("/{id}/toggle-active")
    public String toggleUserStatus(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        User currentUser = securityUtils.getRequiredCurrentUser();

        // Safety: Prevent admin from deactivating themselves
        if (currentUser.getId().equals(id)) {
            redirectAttributes.addFlashAttribute("errorMessage", "You cannot deactivate your own administrative account.");
            return "redirect:/users/" + id;
        }

        userService.toggleUserActive(id);
        redirectAttributes.addFlashAttribute("successMessage", "User account status updated successfully.");

        return "redirect:/users/" + id;
    }
}

