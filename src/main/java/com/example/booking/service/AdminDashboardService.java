package com.example.booking.service;

import com.example.booking.dto.AdminDashboardStats;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public interface AdminDashboardService {

    /** Lay so lieu tong quan va du lieu cho 4 bieu do Chart.js */
    AdminDashboardStats getDashboardStats();

    /** Xuat bao cao doanh thu ra file Excel (.xlsx) bang Apache POI */
    void exportRevenueExcel(HttpServletResponse response) throws IOException;
}
