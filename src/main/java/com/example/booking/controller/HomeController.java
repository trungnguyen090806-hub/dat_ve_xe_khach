package com.example.booking.controller;

import com.example.booking.service.BusCompanyService;
import com.example.booking.service.BusTripService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
public class HomeController {

    private final BusTripService tripService;
    private final BusCompanyService companyService;

    public HomeController(BusTripService tripService, BusCompanyService companyService) {
        this.tripService = tripService;
        this.companyService = companyService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("origins", tripService.getDistinctOrigins());
        model.addAttribute("destinations", tripService.getDistinctDestinations());
        model.addAttribute("featuredCompanies", companyService.getFeaturedCompanies());
        model.addAttribute("recommendedTrips", tripService.getRecommendedTrips());
        model.addAttribute("today", LocalDate.now().toString());

        // Danh sach tuyen xe pho bien de khach hang bam chon nhanh
        List<Map<String, String>> popularRoutes = List.of(
                Map.of(
                        "origin", "Hà Nội",
                        "destination", "Hải Phòng",
                        "image", "https://images.unsplash.com/photo-1509042239860-f550ce710b93?auto=format&fit=crop&w=600&q=80",
                        "price", "110.000đ",
                        "description", "Cao tốc 5B hiện đại, chạy liên tục 30 phút/chuyến"
                ),
                Map.of(
                        "origin", "Hà Nội",
                        "destination", "Đà Nẵng",
                        "image", "https://images.unsplash.com/photo-1559592413-7cec4d0cae2b?auto=format&fit=crop&w=600&q=80",
                        "price", "450.000đ",
                        "description", "Xe giường nằm cao cấp, tiện nghi sang trọng"
                ),
                Map.of(
                        "origin", "TP. Hồ Chí Minh",
                        "destination", "Đà Lạt",
                        "image", "https://images.unsplash.com/photo-1583417319070-4a69db38a482?auto=format&fit=crop&w=600&q=80",
                        "price", "300.000đ",
                        "description", "Khởi hành hàng ngày, xe Limousine phòng nằm êm ái"
                ),
                Map.of(
                        "origin", "TP. Hồ Chí Minh",
                        "destination", "Vũng Tàu",
                        "image", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=600&q=80",
                        "price", "140.000đ",
                        "description", "Chỉ 2 tiếng di chuyển, đưa đón tận nơi"
                )
        );
        model.addAttribute("popularRoutes", popularRoutes);

        return "index";
    }

    @GetMapping("/403")
    public String accessDenied() {
        return "error/403";
    }
}
