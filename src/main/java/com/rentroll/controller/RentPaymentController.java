package com.rentroll.controller;

import com.rentroll.dto.PaymentActionDto;
import com.rentroll.entity.PaymentStatus;
import com.rentroll.entity.RentPayment;
import com.rentroll.entity.Role;
import com.rentroll.entity.User;
import com.rentroll.exception.BusinessRuleException;
import com.rentroll.security.SecurityUtils;
import com.rentroll.service.RentPaymentService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

/**
 * Controller managing rent collection, receipt logging, and CSV exports.
 */
@Controller
@RequestMapping("/payments")
public class RentPaymentController {

    private final RentPaymentService rentPaymentService;
    private final SecurityUtils securityUtils;

    public RentPaymentController(RentPaymentService rentPaymentService, SecurityUtils securityUtils) {
        this.rentPaymentService = rentPaymentService;
        this.securityUtils = securityUtils;
    }

    @GetMapping
    public String listPayments(@RequestParam(value = "status", required = false) PaymentStatus status,
                               @RequestParam(value = "search", required = false) String search,
                               Model model) {
        // First synchronize overdue statuses dynamically
        rentPaymentService.updateOverduePaymentStatuses();

        User currentUser = securityUtils.getRequiredCurrentUser();
        List<RentPayment> payments = rentPaymentService.getPaymentsForCurrentUser(status, search);

        model.addAttribute("payments", payments);
        model.addAttribute("statuses", PaymentStatus.values());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("search", search);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("activeNav", "payments");
        model.addAttribute("pageTitle", "Rent Payments Ledger");
        model.addAttribute("paymentAction", new PaymentActionDto());

        return "payments/list";
    }

    @GetMapping("/{id}")
    public String viewPayment(@PathVariable("id") Long id, Model model) {
        RentPayment payment = rentPaymentService.getPaymentById(id);
        User currentUser = securityUtils.getRequiredCurrentUser();

        model.addAttribute("payment", payment);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("activeNav", "payments");
        model.addAttribute("pageTitle", "Payment Details - " + payment.getRentalPeriod());
        model.addAttribute("paymentAction", new PaymentActionDto());

        return "payments/details";
    }

    @PostMapping("/{id}/pay")
    public String markAsPaid(@PathVariable("id") Long id,
                             @ModelAttribute("paymentAction") PaymentActionDto action,
                             RedirectAttributes redirectAttributes) {
        User currentUser = securityUtils.getRequiredCurrentUser();

        // Enforce role check
        if (currentUser.getRole() == Role.TENANT) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Tenants are not permitted to record payments as paid. Your landlord will confirm receipt.");
            return "redirect:/payments/" + id;
        }

        try {
            LocalDate payDate = action.getPaymentDate() != null ? action.getPaymentDate() : LocalDate.now();
            RentPayment paid = rentPaymentService.markPaymentAsPaid(id, action.getPaymentMethod(), action.getNotes(), payDate);

            if (paid.getLateFee().compareTo(java.math.BigDecimal.ZERO) > 0) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Payment marked as PAID! Note: A 5% late fee of ₹" + paid.getLateFee() +
                        " was automatically added because payment was overdue.");
            } else {
                redirectAttributes.addFlashAttribute("successMessage", "Payment successfully marked as PAID!");
            }
        } catch (BusinessRuleException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/payments/" + id;
    }

    @GetMapping("/export")
    public void exportCsv(@RequestParam(value = "status", required = false) PaymentStatus status,
                          @RequestParam(value = "search", required = false) String search,
                          HttpServletResponse response) throws IOException {
        List<RentPayment> payments = rentPaymentService.getPaymentsForCurrentUser(status, search);
        String csvContent = rentPaymentService.exportPaymentsCsv(payments);

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"rentroll_payments.csv\"");
        response.getOutputStream().write(csvContent.getBytes(StandardCharsets.UTF_8));
        response.flushBuffer();
    }
}

