package com.example.booking.controller.admin;

import com.example.booking.model.Notification;
import com.example.booking.model.Payment;
import com.example.booking.model.Ticket;
import com.example.booking.model.TicketDetail;
import com.example.booking.model.enums.NotificationType;
import com.example.booking.model.enums.PaymentMethod;
import com.example.booking.model.enums.PaymentStatus;
import com.example.booking.model.enums.TicketDetailStatus;
import com.example.booking.model.enums.TicketStatus;
import com.example.booking.repository.NotificationRepository;
import com.example.booking.repository.PaymentRepository;
import com.example.booking.repository.TicketRepository;
import com.example.booking.service.BookingService;
import com.example.booking.utils.CodeGenerator;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin/tickets")
public class AdminTicketController extends AdminBaseController {

    private final TicketRepository ticketRepository;
    private final PaymentRepository paymentRepository;
    private final NotificationRepository notificationRepository;
    private final BookingService bookingService;

    public AdminTicketController(TicketRepository ticketRepository,
                                 PaymentRepository paymentRepository,
                                 NotificationRepository notificationRepository,
                                 BookingService bookingService) {
        this.ticketRepository = ticketRepository;
        this.paymentRepository = paymentRepository;
        this.notificationRepository = notificationRepository;
        this.bookingService = bookingService;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "") String keyword,
                       @RequestParam(required = false) TicketStatus status,
                       Model model) {
        Specification<Ticket> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim().toLowerCase() + "%";
                ps.add(cb.or(
                        cb.like(cb.lower(root.get("ticketCode")), like),
                        cb.like(cb.lower(root.get("passengerName")), like),
                        cb.like(cb.lower(root.get("passengerPhone")), like)
                ));
            }
            if (status != null) {
                ps.add(cb.equal(root.get("status"), status));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };

        Page<Ticket> ticketPage = ticketRepository.findAll(spec, PageRequest.of(
                Math.max(0, page), PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt")));

        model.addAttribute("page", ticketPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("statusParam", status != null ? status.name() : "");
        model.addAttribute("statuses", TicketStatus.values());

        return "admin/tickets";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Ticket ticket = ticketRepository.findById(id).orElseThrow();
        String qrBase64 = bookingService.generateTicketQrBase64(ticket);
        List<Payment> payments = paymentRepository.findByTicketId(id);

        model.addAttribute("ticket", ticket);
        model.addAttribute("trip", ticket.getTrip());
        model.addAttribute("qrCode", qrBase64);
        model.addAttribute("payments", payments);

        return "admin/ticket-detail";
    }

    @PostMapping("/{id}/confirm-payment")
    public String confirmPayment(@PathVariable Long id, RedirectAttributes ra) {
        return execute(ra, "/admin/tickets/" + id, "Đã xác nhận thanh toán vé thành công", () -> {
            Ticket ticket = ticketRepository.findById(id).orElseThrow();
            ticket.setStatus(TicketStatus.PAID);
            ticketRepository.save(ticket);

            // Tao payment neu chua co
            Payment payment = new Payment();
            payment.setTicket(ticket);
            payment.setTransactionCode(CodeGenerator.generate("TXN", 10));
            payment.setMethod(PaymentMethod.CASH);
            payment.setAmount(ticket.getTotalAmount());
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setPaidAt(LocalDateTime.now());
            payment.setNote("Admin xác nhận thu tiền mặt");
            paymentRepository.save(payment);
        });
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id,
                         @RequestParam(defaultValue = "Admin hủy vé") String reason,
                         RedirectAttributes ra) {
        return execute(ra, "/admin/tickets", "Đã hủy vé và giải phóng ghế", () -> {
            Ticket ticket = ticketRepository.findById(id).orElseThrow();
            ticket.setStatus(TicketStatus.CANCELLED);
            ticket.setCancelReason(reason);
            ticket.setCancelledAt(LocalDateTime.now());

            for (TicketDetail d : ticket.getDetails()) {
                d.setStatus(TicketDetailStatus.CANCELLED);
            }
            ticketRepository.save(ticket);

            List<Payment> payments = paymentRepository.findByTicketId(id);
            for (Payment p : payments) {
                if (p.getStatus() == PaymentStatus.SUCCESS) {
                    p.setStatus(PaymentStatus.REFUNDED);
                    p.setNote("Admin hủy vé: " + reason);
                }
            }
            paymentRepository.saveAll(payments);

            Notification notif = new Notification();
            notif.setUser(ticket.getUser());
            notif.setTitle("Vé " + ticket.getTicketCode() + " đã bị hủy bởi Quản trị viên");
            notif.setContent("Lý do: " + reason);
            notif.setType(NotificationType.CANCEL);
            notificationRepository.save(notif);
        });
    }
}
