package com.example.booking.service.policy;

import com.example.booking.model.Ticket;
import com.example.booking.model.enums.TicketStatus;
import com.example.booking.model.enums.TripStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class StandardCancellationPolicy implements CancellationPolicy {

    private static final BigDecimal RATE_100 = new BigDecimal("1.00");
    private static final BigDecimal RATE_70 = new BigDecimal("0.70");
    private static final BigDecimal RATE_0 = BigDecimal.ZERO;

    @Override
    public boolean canCancel(Ticket ticket) {
        if (ticket == null || ticket.getTrip() == null) {
            return false;
        }
        if (ticket.getStatus() != TicketStatus.PAID && ticket.getStatus() != TicketStatus.PENDING) {
            return false;
        }
        if (ticket.getTrip().getStatus() != TripStatus.SCHEDULED) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime departure = ticket.getTrip().getDepartureTime();
        if (!departure.isAfter(now)) {
            return false;
        }

        Duration remaining = Duration.between(now, departure);
        return remaining.toHours() >= 6;
    }

    @Override
    public BigDecimal calculateRefundRate(Ticket ticket) {
        if (!canCancel(ticket)) {
            return RATE_0;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime departure = ticket.getTrip().getDepartureTime();
        Duration remaining = Duration.between(now, departure);

        long hours = remaining.toHours();
        if (hours >= 24) {
            return RATE_100;
        } else if (hours >= 6) {
            return RATE_70;
        } else {
            return RATE_0;
        }
    }

    @Override
    public BigDecimal calculateRefundAmount(Ticket ticket) {
        if (ticket == null || ticket.getTotalAmount() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal rate = calculateRefundRate(ticket);
        return ticket.getTotalAmount().multiply(rate).setScale(0, RoundingMode.HALF_UP);
    }

    @Override
    public String getPolicyDescription(Ticket ticket) {
        if (!canCancel(ticket)) {
            return "Không thể hủy vé (Chuyến xe khởi hành trong vòng dưới 6 giờ hoặc đã hoàn thành/hủy).";
        }
        BigDecimal rate = calculateRefundRate(ticket);
        if (rate.compareTo(RATE_100) == 0) {
            return "Hủy trước giờ khởi hành trên 24 giờ: Hoàn 100% tiền vé.";
        } else if (rate.compareTo(RATE_70) == 0) {
            return "Hủy trước giờ khởi hành từ 6 đến 24 giờ: Hoàn 70% tiền vé.";
        } else {
            return "Không áp dụng hoàn tiền.";
        }
    }
}
