package com.example.booking.controller;

import com.example.booking.dto.BusTripResponse;
import com.example.booking.dto.TripSearchRequest;
import com.example.booking.model.enums.BusType;
import com.example.booking.service.BusCompanyService;
import com.example.booking.service.BusTripService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;

@Controller
@RequestMapping("/search")
public class TripSearchController {

    private final BusTripService tripService;
    private final BusCompanyService companyService;

    public TripSearchController(BusTripService tripService, BusCompanyService companyService) {
        this.tripService = tripService;
        this.companyService = companyService;
    }

    @GetMapping
    public String search(@ModelAttribute("searchReq") TripSearchRequest request, Model model) {
        Page<BusTripResponse> page = tripService.searchCustomerTrips(request);

        model.addAttribute("page", page);
        model.addAttribute("origins", tripService.getDistinctOrigins());
        model.addAttribute("destinations", tripService.getDistinctDestinations());
        model.addAttribute("companies", companyService.getActiveCompanies());
        model.addAttribute("busTypes", BusType.values());
        model.addAttribute("today", LocalDate.now().toString());

        return "search-results";
    }
}
