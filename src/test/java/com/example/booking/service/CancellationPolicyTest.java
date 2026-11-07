package com.example.booking.service;

import com.example.booking.model.BusTrip;
import com.example.booking.model.Ticket;
import com.example.booking.model.enums.TicketStatus;
import com.example.booking.model.enums.TripStatus;
import com.example.booking.service.policy.CancellationPolicy;
import com.example.booking.service.policy.StandardCancellationPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CancellationPolicyTest {

    private CancellationPolicy cancellationPolicy;
    private Ticket ticket;
    private BusTrip trip;

    @BeforeEach
    void setUp() {
        cancellationPolicy = new StandardCancellationPolicy();

        trip = new BusTrip();
        trip.setStatus(TripStatus.SCHEDULED);

        ticket = new Ticket();
        ticket.setStatus(TicketStatus.PAID);
        ticket.setTrip(trip);
        ticket.setTotalAmount(new BigDecimal("200000"));
    }

    @Test
    @DisplayName("Hủy vé trước giờ khởi hành trên 24 giờ -> Hoàn 100% tiền vé")
    void testCancelBefore24Hours() {
        // Chuyen xe khoi hanh sau 30 gio nua
        trip.setDepartureTime(LocalDateTime.now().plusHours(30));

        assertTrue(cancellationPolicy.canCancel(ticket));
        assertEquals(new BigDecimal("1.00"), cancellationPolicy.calculateRefundRate(ticket));
        assertEquals(new BigDecimal("200000"), cancellationPolicy.calculateRefundAmount(ticket));
    }

    @Test
    @DisplayName("Hủy vé trước giờ khởi hành từ 6 đến 24 giờ -> Hoàn 70% tiền vé")
    void testCancelBetween6And24Hours() {
        // Chuyen xe khoi hanh sau 10 gio nua
        trip.setDepartureTime(LocalDateTime.now().plusHours(10));

        assertTrue(cancellationPolicy.canCancel(ticket));
        assertEquals(new BigDecimal("0.70"), cancellationPolicy.calculateRefundRate(ticket));
        // 200.000 * 70% = 140.000
        assertEquals(new BigDecimal("140000"), cancellationPolicy.calculateRefundAmount(ticket));
    }

    @Test
    @DisplayName("Hủy vé khi thời gian còn dưới 6 giờ -> Không cho phép hủy")
    void testCancelUnder6Hours() {
        // Chuyen xe khoi hanh sau 3 gio nua
        trip.setDepartureTime(LocalDateTime.now().plusHours(3));

        assertFalse(cancellationPolicy.canCancel(ticket));
        assertEquals(BigDecimal.ZERO, cancellationPolicy.calculateRefundAmount(ticket));
    }

    @Test
    @DisplayName("Vé đã bị hủy trước đó -> Không thể tiếp tục hủy")
    void testAlreadyCancelledTicket() {
        trip.setDepartureTime(LocalDateTime.now().plusHours(30));
        ticket.setStatus(TicketStatus.CANCELLED);

        assertFalse(cancellationPolicy.canCancel(ticket));
    }
}
