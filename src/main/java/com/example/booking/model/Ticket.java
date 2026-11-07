package com.example.booking.model;

import com.example.booking.model.enums.TicketStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tickets")
@Getter
@Setter
@NoArgsConstructor
public class Ticket extends BaseEntity {

    @Column(name = "ticket_code", nullable = false, unique = true, length = 20)
    private String ticketCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private BusTrip trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id")
    private Promotion promotion;

    @Column(name = "passenger_name", nullable = false, length = 100)
    private String passengerName;

    @Column(name = "passenger_phone", nullable = false, length = 15)
    private String passengerPhone;

    @Column(name = "passenger_email", length = 120)
    private String passengerEmail;

    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal subtotal;

    @Column(name = "vip_surcharge_total", nullable = false, precision = 12, scale = 0)
    private BigDecimal vipSurchargeTotal = BigDecimal.ZERO;

    @Column(name = "member_discount", nullable = false, precision = 12, scale = 0)
    private BigDecimal memberDiscount = BigDecimal.ZERO;

    @Column(name = "promo_discount", nullable = false, precision = 12, scale = 0)
    private BigDecimal promoDiscount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 0)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status = TicketStatus.PENDING;

    @Column(name = "cancel_reason", length = 255)
    private String cancelReason;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TicketDetail> details = new ArrayList<>();

    /** Giu quan he hai chieu nhat quan. */
    public void addDetail(TicketDetail detail) {
        detail.setTicket(this);
        details.add(detail);
    }
}
