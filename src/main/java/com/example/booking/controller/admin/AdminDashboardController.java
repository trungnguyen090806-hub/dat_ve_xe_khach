package com.example.booking.controller.admin;

import com.example.booking.dto.AdminDashboardStats;
import com.example.booking.service.AdminDashboardService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController extends AdminBaseController {

    private final AdminDashboardService dashboardService;

    public AdminDashboardController(AdminDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model) {
        AdminDashboardStats stats = dashboardService.getDashboardStats();
        model.addAttribute("stats", stats);
        return "admin/dashboard";
    }

    @GetMapping("/reports")
    public String reportsPage(Model model) {
        AdminDashboardStats stats = dashboardService.getDashboardStats();
        model.addAttribute("stats", stats);
        return "admin/reports";
    }

    @GetMapping("/reports/export-excel")
    public void exportExcel(HttpServletResponse response) throws IOException {
        dashboardService.exportRevenueExcel(response);
    }
}
