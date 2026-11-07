package com.example.booking.controller.admin;

import com.example.booking.dto.TripSeatMapResponse;
import com.example.booking.model.BusTrip;
import com.example.booking.repository.BusTripRepository;
import com.example.booking.service.SeatBookingService;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/admin/seats")
public class AdminSeatMapController extends AdminBaseController {

    private final BusTripRepository tripRepository;
    private final SeatBookingService seatBookingService;

    public AdminSeatMapController(BusTripRepository tripRepository,
                                  SeatBookingService seatBookingService) {
        this.tripRepository = tripRepository;
        this.seatBookingService = seatBookingService;
    }

    @GetMapping
    public String viewSeats(@RequestParam(required = false) Long tripId, Model model) {
        List<BusTrip> trips = tripRepository.findAll(Sort.by(Sort.Direction.DESC, "departureTime"));
        model.addAttribute("trips", trips);

        if (tripId == null && !trips.isEmpty()) {
            tripId = trips.get(0).getId();
        }

        if (tripId != null) {
            TripSeatMapResponse seatMap = seatBookingService.getSeatMap(tripId, null);
            model.addAttribute("selectedTripId", tripId);
            model.addAttribute("seatMap", seatMap);
            model.addAttribute("trip", seatMap.getTrip());
        }

        return "admin/seats";
    }
}
