package com.example.booking.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class AdminDashboardStats {

    private final BigDecimal totalRevenue;
    private final long totalTrips;
    private final long totalSoldTickets;
    private final long totalCustomers;
    private final long pendingTickets;
    private final double occupancyRate;

    // Du lieu Chart 1: Doanh thu 6 thang gan nhat
    private final List<String> monthlyRevenueLabels;
    private final List<BigDecimal> monthlyRevenueData;

    // Du lieu Chart 2: So luong ve ban trong 7 ngay gan nhat
    private final List<String> dailyTicketsLabels;
    private final List<Long> dailyTicketsData;

    // Du lieu Chart 3: Doanh thu theo tuyen xe
    private final List<String> routeRevenueLabels;
    private final List<BigDecimal> routeRevenueData;

    // Du lieu Chart 4: Ty le lap day ghe
    private final long totalSeatsInSystem;
    private final long totalBookedSeats;
}
