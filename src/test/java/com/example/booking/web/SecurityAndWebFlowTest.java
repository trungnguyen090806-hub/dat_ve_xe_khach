package com.example.booking.web;

import com.example.booking.config.CustomUserDetailsService;
import com.example.booking.config.SecurityConfig;
import com.example.booking.controller.CustomerTicketController;
import com.example.booking.controller.HomeController;
import com.example.booking.controller.admin.AdminDashboardController;
import com.example.booking.dto.AdminDashboardStats;
import com.example.booking.service.AdminDashboardService;
import com.example.booking.service.BookingService;
import com.example.booking.service.BusCompanyService;
import com.example.booking.service.BusTripService;
import com.example.booking.service.CustomerTicketService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {
        HomeController.class,
        CustomerTicketController.class,
        AdminDashboardController.class
})
@Import(SecurityConfig.class)
class SecurityAndWebFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BusTripService busTripService;

    @MockBean
    private BusCompanyService busCompanyService;

    @MockBean
    private CustomerTicketService customerTicketService;

    @MockBean
    private BookingService bookingService;

    @MockBean
    private AdminDashboardService adminDashboardService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("Khách vãng lai truy cập Trang chủ -> 200 OK")
    void testPublicAccessHome() throws Exception {
        when(busTripService.getDistinctOrigins()).thenReturn(List.of("Hà Nội"));
        when(busTripService.getDistinctDestinations()).thenReturn(List.of("Hải Phòng"));
        when(companyServiceMock()).thenReturn(List.of());
        when(busTripService.getRecommendedTrips()).thenReturn(List.of());

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    private List<?> companyServiceMock() {
        return busCompanyService.getFeaturedCompanies();
    }

    @Test
    @DisplayName("Khách vãng lai truy cập Tra cứu vé -> 200 OK")
    void testPublicAccessLookup() throws Exception {
        mockMvc.perform(get("/lookup"))
                .andExpect(status().isOk())
                .andExpect(view().name("user/ticket-lookup"));
    }

    @Test
    @DisplayName("Chưa đăng nhập truy cập Admin -> Chuyển hướng tới trang Đăng nhập")
    void testUnauthenticatedAccessAdminRedirectsLogin() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "customer", roles = {"CUSTOMER"})
    @DisplayName("Tài khoản CUSTOMER cố tình truy cập /admin/dashboard -> Bị chặn 403 Forbidden")
    void testCustomerCannotAccessAdmin() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/403"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Tài khoản ADMIN truy cập /admin/dashboard -> Thành công 200 OK")
    void testAdminAccessDashboardSuccess() throws Exception {
        when(adminDashboardService.getDashboardStats()).thenReturn(
                AdminDashboardStats.builder()
                        .totalRevenue(java.math.BigDecimal.ZERO)
                        .totalTrips(0)
                        .totalSoldTickets(0)
                        .totalCustomers(0)
                        .pendingTickets(0)
                        .occupancyRate(0.0)
                        .monthlyRevenueLabels(List.of())
                        .monthlyRevenueData(List.of())
                        .dailyTicketsLabels(List.of())
                        .dailyTicketsData(List.of())
                        .routeRevenueLabels(List.of())
                        .routeRevenueData(List.of())
                        .totalSeatsInSystem(0)
                        .totalBookedSeats(0)
                        .build()
        );

        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"));
    }

    @Test
    @DisplayName("Đăng nhập sai mật khẩu -> Chuyển hướng /login?error")
    void testLoginWithWrongPassword() throws Exception {
        when(customUserDetailsService.loadUserByUsername(anyString()))
                .thenThrow(new UsernameNotFoundException("Không tìm thấy tài khoản"));

        mockMvc.perform(formLogin("/login").user("username", "admin").password("password", "sai_mat_khau"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }
}
