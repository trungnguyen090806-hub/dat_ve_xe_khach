package com.example.booking.service.impl;

import com.example.booking.exception.BusinessException;
import com.example.booking.exception.ResourceNotFoundException;
import com.example.booking.model.Notification;
import com.example.booking.model.Payment;
import com.example.booking.model.Ticket;
import com.example.booking.model.TicketDetail;
import com.example.booking.model.enums.NotificationType;
import com.example.booking.model.enums.PaymentStatus;
import com.example.booking.model.enums.TicketDetailStatus;
import com.example.booking.model.enums.TicketStatus;
import com.example.booking.repository.NotificationRepository;
import com.example.booking.repository.PaymentRepository;
import com.example.booking.repository.TicketRepository;
import com.example.booking.service.CustomerTicketService;
import com.example.booking.service.policy.CancellationPolicy;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class CustomerTicketServiceImpl implements CustomerTicketService {

    private final TicketRepository ticketRepository;
    private final PaymentRepository paymentRepository;
    private final NotificationRepository notificationRepository;
    private final CancellationPolicy cancellationPolicy;

    public CustomerTicketServiceImpl(TicketRepository ticketRepository,
                                     PaymentRepository paymentRepository,
                                     NotificationRepository notificationRepository,
                                     CancellationPolicy cancellationPolicy) {
        this.ticketRepository = ticketRepository;
        this.paymentRepository = paymentRepository;
        this.notificationRepository = notificationRepository;
        this.cancellationPolicy = cancellationPolicy;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Ticket> getMyTickets(Long userId, TicketStatus status, int page, int size) {
        Specification<Ticket> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("user").get("id"), userId));
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return ticketRepository.findAll(spec, PageRequest.of(
                Math.max(0, page), size > 0 ? size : 10,
                Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @Override
    @Transactional(readOnly = true)
    public Ticket getTicketDetail(Long ticketId, Long userId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Vé xe", ticketId));

        if (!ticket.getUser().getId().equals(userId)) {
            throw new BusinessException("Bạn không có quyền xem thông tin vé này");
        }
        return ticket;
    }

    @Override
    @Transactional(readOnly = true)
    public Ticket lookupTicket(String ticketCode, String phone) {
        if (ticketCode == null || ticketCode.isBlank() || phone == null || phone.isBlank()) {
            throw new BusinessException("Vui lòng nhập đầy đủ mã vé và số điện thoại");
        }

        Ticket ticket = ticketRepository.findByTicketCode(ticketCode.trim().toUpperCase())
                .orElseThrow(() -> new BusinessException("Không tìm thấy vé với mã: " + ticketCode));

        String cleanInputPhone = phone.replaceAll("[^0-9]", "");
        String cleanTicketPhone = ticket.getPassengerPhone().replaceAll("[^0-9]", "");

        if (!cleanTicketPhone.endsWith(cleanInputPhone) && !cleanInputPhone.endsWith(cleanTicketPhone)) {
            throw new BusinessException("Số điện thoại không khớp với thông tin vé");
        }

        return ticket;
    }

    @Override
    @Transactional
    public void cancelTicket(Long ticketId, String reason, Long userId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Vé xe", ticketId));

        if (!ticket.getUser().getId().equals(userId)) {
            throw new BusinessException("Bạn không có quyền thực hiện hủy vé này");
        }

        if (!cancellationPolicy.canCancel(ticket)) {
            throw new BusinessException("Vé này không đủ điều kiện để hủy (chỉ cho phép hủy trước giờ khởi hành tối thiểu 6 giờ).");
        }

        BigDecimal refundAmount = cancellationPolicy.calculateRefundAmount(ticket);

        ticket.setStatus(TicketStatus.CANCELLED);
        ticket.setCancelReason(reason != null && !reason.isBlank() ? reason.trim() : "Khách hàng yêu cầu hủy");
        ticket.setCancelledAt(LocalDateTime.now());

        // Giai phong ghe: Chuyen status ve CANCELLED -> active_key thanh NULL
        for (TicketDetail detail : ticket.getDetails()) {
            detail.setStatus(TicketDetailStatus.CANCELLED);
        }

        ticketRepository.save(ticket);

        // Cap nhat Payment
        List<Payment> payments = paymentRepository.findByTicketId(ticketId);
        for (Payment p : payments) {
            if (p.getStatus() == PaymentStatus.SUCCESS) {
                p.setStatus(PaymentStatus.REFUNDED);
                p.setNote("Đã hoàn tiền " + refundAmount + "đ do khách hủy vé (" + ticket.getCancelReason() + ")");
            }
        }
        if (!payments.isEmpty()) {
            paymentRepository.saveAll(payments);
        }

        // Gui thong bao
        Notification notif = new Notification();
        notif.setUser(ticket.getUser());
        notif.setTitle("Đã hủy vé " + ticket.getTicketCode());
        notif.setContent("Vé " + ticket.getTicketCode() + " đã được hủy thành công. Số tiền hoàn lại dự kiến: "
                + new java.text.DecimalFormat("#,###đ").format(refundAmount) + ".");
        notif.setType(NotificationType.CANCEL);
        notif.setRead(false);
        notificationRepository.save(notif);
    }

    @Override
    public CancellationPolicy getCancellationPolicy() {
        return cancellationPolicy;
    }
}
