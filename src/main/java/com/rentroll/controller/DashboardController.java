package com.rentroll.controller;

import com.rentroll.dto.AdminDashboardDto;
import com.rentroll.dto.LandlordDashboardDto;
import com.rentroll.dto.TenantDashboardDto;
import com.rentroll.entity.User;
import com.rentroll.security.SecurityUtils;
import com.rentroll.service.DashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Routes the authenticated user to their role-specific dashboard.
 */
@Controller
public class DashboardController {

    private final DashboardService dashboardService;
    private final SecurityUtils securityUtils;

    public DashboardController(DashboardService dashboardService, SecurityUtils securityUtils) {
        this.dashboardService = dashboardService;
        this.securityUtils = securityUtils;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        User currentUser = securityUtils.getRequiredCurrentUser();
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("activeNav", "dashboard");

        switch (currentUser.getRole()) {
            case ADMIN -> {
                AdminDashboardDto stats = dashboardService.getAdminDashboard();
                model.addAttribute("stats", stats);
                model.addAttribute("pageTitle", "Admin Control Centre");
                return "dashboard/admin";
            }
            case LANDLORD -> {
                LandlordDashboardDto stats = dashboardService.getLandlordDashboard(currentUser.getId());
                model.addAttribute("stats", stats);
                model.addAttribute("pageTitle", "Landlord Financial Dashboard");
                return "dashboard/landlord";
            }
            case TENANT -> {
                TenantDashboardDto stats = dashboardService.getTenantDashboard(currentUser.getId());
                model.addAttribute("stats", stats);
                model.addAttribute("pageTitle", "Tenant Portal");
                return "dashboard/tenant";
            }
            default -> throw new IllegalStateException("Unexpected role: " + currentUser.getRole());
        }
    }
}

