package com.example.booking.controller;

import com.example.booking.config.CustomUserDetails;
import com.example.booking.dto.HoldSeatsRequest;
import com.example.booking.dto.TripSeatMapResponse;
import com.example.booking.exception.BusinessException;
import com.example.booking.model.SeatHold;
import com.example.booking.service.SeatBookingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/trips/{tripId}")
public class TripSeatController {

    private final SeatBookingService seatBookingService;

    public TripSeatController(SeatBookingService seatBookingService) {
        this.seatBookingService = seatBookingService;
    }

    /**
     * Xem so do ghe 2 tang cua chuyen xe.
     * Cho phep xem cong khai ca khi chua dang nhap.
     */
    @GetMapping("/seats")
    public String viewSeatMap(@PathVariable Long tripId,
                              @AuthenticationPrincipal CustomUserDetails userDetails,
                              Model model) {
        Long userId = userDetails != null ? userDetails.getId() : null;
        TripSeatMapResponse seatMap = seatBookingService.getSeatMap(tripId, userId);

        model.addAttribute("seatMap", seatMap);
        model.addAttribute("trip", seatMap.getTrip());
        model.addAttribute("isLoggedIn", userDetails != null);
        return "booking/seat-selection";
    }

    /**
     * API lay du lieu so do ghe dang JSON (dung cho AJAX refresh trang thai thoi gian thuc)
     */
    @GetMapping("/api/seats")
    @ResponseBody
    public ResponseEntity<TripSeatMapResponse> getSeatMapApi(@PathVariable Long tripId,
                                                             @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails != null ? userDetails.getId() : null;
        return ResponseEntity.ok(seatBookingService.getSeatMap(tripId, userId));
    }

    /**
     * Giu ghe tam 10 phut truoc khi sang buoc xac nhan dat ve & thanh toan.
     * Yeu cau dang nhap (ROLE_CUSTOMER hoac ADMIN).
     */
    @PostMapping("/api/hold-seats")
    @ResponseBody
    public ResponseEntity<?> holdSeatsApi(@PathVariable Long tripId,
                                          @Valid @RequestBody HoldSeatsRequest request,
                                          @AuthenticationPrincipal CustomUserDetails userDetails,
                                          HttpSession session) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "requireLogin", true,
                    "message", "Vui lòng đăng nhập để tiến hành giữ ghế và đặt vé."
            ));
        }

        try {
            List<SeatHold> holds = seatBookingService.holdSeats(tripId, request.getSeatIds(), userDetails.getId());

            // Luu tam thong tin giu ghe vao session de chuyen sang trang dat ve
            session.setAttribute("booking_trip_id", tripId);
            session.setAttribute("booking_seat_ids", request.getSeatIds());

            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.put("message", "Giữ chỗ thành công! Bạn có 10 phút để hoàn tất đặt vé.");
            resp.put("heldCount", holds.size());
            resp.put("expiresAt", holds.get(0).getExpiresAt().toString());
            resp.put("redirectUrl", "/booking/checkout?tripId=" + tripId);

            return ResponseEntity.ok(resp);
        } catch (BusinessException ex) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", ex.getMessage()
            ));
        }
    }
}
