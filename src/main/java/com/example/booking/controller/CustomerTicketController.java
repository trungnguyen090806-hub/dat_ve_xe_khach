package com.example.booking.controller;

import com.example.booking.config.CustomUserDetails;
import com.example.booking.dto.TicketLookupRequest;
import com.example.booking.exception.BusinessException;
import com.example.booking.model.Ticket;
import com.example.booking.model.enums.TicketStatus;
import com.example.booking.service.BookingService;
import com.example.booking.service.CustomerTicketService;
import com.example.booking.service.policy.CancellationPolicy;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CustomerTicketController {

    private final CustomerTicketService customerTicketService;
    private final BookingService bookingService;

    public CustomerTicketController(CustomerTicketService customerTicketService,
                                    BookingService bookingService) {
        this.customerTicketService = customerTicketService;
        this.bookingService = bookingService;
    }

    /**
     * Danh sach ve cua toi (yeu cau dang nhap)
     */
    @GetMapping("/my-tickets")
    public String myTickets(@AuthenticationPrincipal CustomUserDetails userDetails,
                            @RequestParam(required = false) TicketStatus status,
                            @RequestParam(defaultValue = "0") int page,
                            Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        Page<Ticket> ticketPage = customerTicketService.getMyTickets(
                userDetails.getId(), status, page, 8);

        model.addAttribute("page", ticketPage);
        model.addAttribute("statusParam", status != null ? status.name() : "");
        model.addAttribute("statuses", TicketStatus.values());
        return "user/my-tickets";
    }

    /**
     * Chi tiet ve xe
     */
    @GetMapping("/my-tickets/{id}")
    public String ticketDetail(@PathVariable Long id,
                               @AuthenticationPrincipal CustomUserDetails userDetails,
                               Model model,
                               RedirectAttributes ra) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        try {
            Ticket ticket = customerTicketService.getTicketDetail(id, userDetails.getId());
            CancellationPolicy policy = customerTicketService.getCancellationPolicy();
            String qrBase64 = bookingService.generateTicketQrBase64(ticket);

            model.addAttribute("ticket", ticket);
            model.addAttribute("trip", ticket.getTrip());
            model.addAttribute("policy", policy);
            model.addAttribute("canCancel", policy.canCancel(ticket));
            model.addAttribute("refundAmount", policy.calculateRefundAmount(ticket));
            model.addAttribute("policyDesc", policy.getPolicyDescription(ticket));
            model.addAttribute("qrCode", qrBase64);

            return "user/ticket-detail";
        } catch (BusinessException ex) {
            ra.addFlashAttribute("toastError", ex.getMessage());
            return "redirect:/my-tickets";
        }
    }

    /**
     * Khach hang huy ve theo chinh sach
     */
    @PostMapping("/my-tickets/{id}/cancel")
    public String cancelTicket(@PathVariable Long id,
                               @RequestParam(defaultValue = "Khách hàng tự hủy") String reason,
                               @AuthenticationPrincipal CustomUserDetails userDetails,
                               RedirectAttributes ra) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        try {
            customerTicketService.cancelTicket(id, reason, userDetails.getId());
            ra.addFlashAttribute("toastSuccess", "Hủy vé thành công! Ghế đã được giải phóng và tiến trình hoàn tiền đang được xử lý.");
        } catch (BusinessException ex) {
            ra.addFlashAttribute("toastError", ex.getMessage());
        }
        return "redirect:/my-tickets/" + id;
    }

    /**
     * Trang tra cuu ma ve cong khai
     */
    @GetMapping("/lookup")
    public String lookupPage(Model model) {
        model.addAttribute("lookupRequest", new TicketLookupRequest());
        return "user/ticket-lookup";
    }

    /**
     * Xu ly tra cuu ma ve cong khai
     */
    @PostMapping("/lookup")
    public String handleLookup(@Valid @ModelAttribute("lookupRequest") TicketLookupRequest request,
                               BindingResult result,
                               Model model) {
        if (result.hasErrors()) {
            return "user/ticket-lookup";
        }

        try {
            Ticket ticket = customerTicketService.lookupTicket(request.getTicketCode(), request.getPhone());
            String qrBase64 = bookingService.generateTicketQrBase64(ticket);

            model.addAttribute("ticket", ticket);
            model.addAttribute("trip", ticket.getTrip());
            model.addAttribute("qrCode", qrBase64);
            model.addAttribute("found", true);
        } catch (BusinessException ex) {
            model.addAttribute("lookupError", ex.getMessage());
        }
        return "user/ticket-lookup";
    }
}
