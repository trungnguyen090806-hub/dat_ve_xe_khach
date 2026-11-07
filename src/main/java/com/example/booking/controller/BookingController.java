package com.example.booking.controller;

import com.example.booking.config.CustomUserDetails;
import com.example.booking.dto.BookingCheckoutForm;
import com.example.booking.dto.BookingCheckoutView;
import com.example.booking.exception.BusinessException;
import com.example.booking.model.Ticket;
import com.example.booking.service.BookingService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/booking")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     * Hien thi trang Checkout xac nhan dat ve va chon phuong thuc thanh toan
     */
    @GetMapping("/checkout")
    @SuppressWarnings("unchecked")
    public String checkoutPage(@RequestParam(required = false) Long tripId,
                               @RequestParam(required = false) List<Long> seatIds,
                               @RequestParam(required = false) String promoCode,
                               @AuthenticationPrincipal CustomUserDetails userDetails,
                               HttpSession session,
                               Model model,
                               RedirectAttributes ra) {
        if (userDetails == null) {
            ra.addFlashAttribute("toastError", "Vui lòng đăng nhập để tiến hành đặt vé.");
            return "redirect:/login";
        }

        // Lay tu session neu khong co tren query params
        if (tripId == null) {
            tripId = (Long) session.getAttribute("booking_trip_id");
        }
        if (seatIds == null || seatIds.isEmpty()) {
            seatIds = (List<Long>) session.getAttribute("booking_seat_ids");
        }

        if (tripId == null || seatIds == null || seatIds.isEmpty()) {
            ra.addFlashAttribute("toastError", "Phiên chọn ghế đã hết hạn hoặc không tìm thấy ghế. Vui lòng chọn lại!");
            return "redirect:/search";
        }

        try {
            BookingCheckoutView checkoutView = bookingService.prepareCheckout(
                    tripId, seatIds, promoCode, userDetails.getId());

            BookingCheckoutForm form = new BookingCheckoutForm();
            form.setTripId(tripId);
            form.setSeatIds(seatIds);
            form.setPassengerName(checkoutView.getDefaultPassengerName());
            form.setPassengerPhone(checkoutView.getDefaultPassengerPhone());
            form.setPassengerEmail(checkoutView.getDefaultPassengerEmail());
            form.setPromoCode(promoCode);

            model.addAttribute("view", checkoutView);
            model.addAttribute("form", form);
            model.addAttribute("trip", checkoutView.getTrip());
            model.addAttribute("seats", checkoutView.getSeats());

            return "booking/checkout";
        } catch (BusinessException ex) {
            ra.addFlashAttribute("toastError", ex.getMessage());
            return "redirect:/trips/" + tripId + "/seats";
        }
    }

    /**
     * Xu ly submit thanh toan dat ve
     */
    @PostMapping("/checkout")
    public String processCheckout(@Valid @ModelAttribute("form") BookingCheckoutForm form,
                                  BindingResult result,
                                  @AuthenticationPrincipal CustomUserDetails userDetails,
                                  HttpSession session,
                                  Model model,
                                  RedirectAttributes ra) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        if (result.hasErrors()) {
            ra.addFlashAttribute("toastError", "Vui lòng kiểm tra lại thông tin hành khách!");
            return "redirect:/booking/checkout?tripId=" + form.getTripId();
        }

        try {
            Ticket ticket = bookingService.processBooking(form, userDetails.getId());

            // Xoa thong tin khoi session
            session.removeAttribute("booking_trip_id");
            session.removeAttribute("booking_seat_ids");

            ra.addFlashAttribute("toastSuccess", "Đặt vé và thanh toán thành công!");
            return "redirect:/booking/success?ticketCode=" + ticket.getTicketCode();
        } catch (BusinessException ex) {
            ra.addFlashAttribute("toastError", ex.getMessage());
            return "redirect:/trips/" + form.getTripId() + "/seats";
        }
    }

    /**
     * Trang Dat ve thanh cong & Hien thi ma QR, thong tin ve
     */
    @GetMapping("/success")
    public String bookingSuccess(@RequestParam String ticketCode,
                                 @AuthenticationPrincipal CustomUserDetails userDetails,
                                 Model model,
                                 RedirectAttributes ra) {
        try {
            Long userId = userDetails != null ? userDetails.getId() : null;
            boolean isAdmin = userDetails != null && userDetails.isAdmin();

            Ticket ticket = bookingService.getTicketByCode(ticketCode, userId, isAdmin);
            String qrBase64 = bookingService.generateTicketQrBase64(ticket);

            model.addAttribute("ticket", ticket);
            model.addAttribute("trip", ticket.getTrip());
            model.addAttribute("qrCode", qrBase64);

            return "booking/success";
        } catch (BusinessException ex) {
            ra.addFlashAttribute("toastError", ex.getMessage());
            return "redirect:/";
        }
    }
}
