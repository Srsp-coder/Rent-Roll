package com.rentroll.controller;

import com.rentroll.dto.ReportSummaryDto;
import com.rentroll.entity.Role;
import com.rentroll.entity.User;
import com.rentroll.security.SecurityUtils;
import com.rentroll.service.DashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * Controller displaying financial reports, collection KPIs, and lease expiries.
 */
@Controller
@RequestMapping("/reports")
public class ReportController {

    private final DashboardService dashboardService;
    private final SecurityUtils securityUtils;

    public ReportController(DashboardService dashboardService, SecurityUtils securityUtils) {
        this.dashboardService = dashboardService;
        this.securityUtils = securityUtils;
    }

    @GetMapping
    public String viewReports(@RequestParam(value = "year", required = false) Integer year,
                              @RequestParam(value = "month", required = false) Integer month,
                              Model model) {
        User currentUser = securityUtils.getRequiredCurrentUser();
        if (currentUser.getRole() == Role.TENANT) {
            return "redirect:/dashboard";
        }

        LocalDate now = LocalDate.now();
        int targetYear = year != null ? year : now.getYear();
        int targetMonth = month != null ? month : now.getMonthValue();

        Long landlordId = (currentUser.getRole() == Role.LANDLORD) ? currentUser.getId() : null;
        ReportSummaryDto summary = dashboardService.getReportSummary(targetYear, targetMonth, landlordId);

        List<Integer> availableYears = Arrays.asList(now.getYear() - 1, now.getYear(), now.getYear() + 1);

        model.addAttribute("summary", summary);
        model.addAttribute("selectedYear", targetYear);
        model.addAttribute("selectedMonth", targetMonth);
        model.addAttribute("availableYears", availableYears);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("activeNav", "reports");
        model.addAttribute("pageTitle", "Financial Reports & Expiry Watch");

        return "reports/index";
    }
}

