package com.example.booking.model;

import com.example.booking.model.enums.TripStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Table(name = "bus_trips")
@Getter
@Setter
@NoArgsConstructor
public class BusTrip extends BaseEntity {

    @Column(name = "trip_code", nullable = false, unique = true, length = 20)
    private String tripCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bus_id", nullable = false)
    private Bus bus;

    @Column(nullable = false, length = 100)
    private String origin;

    @Column(nullable = false, length = 100)
    private String destination;

    @Column(name = "pickup_point", length = 255)
    private String pickupPoint;

    @Column(name = "dropoff_point", length = 255)
    private String dropoffPoint;

    @Column(name = "departure_time", nullable = false)
    private LocalDateTime departureTime;

    @Column(name = "arrival_time", nullable = false)
    private LocalDateTime arrivalTime;

    @Column(name = "base_price", nullable = false, precision = 12, scale = 0)
    private BigDecimal basePrice;

    @Column(name = "vip_surcharge", nullable = false, precision = 12, scale = 0)
    private BigDecimal vipSurcharge = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TripStatus status = TripStatus.SCHEDULED;

    /** Chuyen con dat ve duoc khong: chua huy va chua khoi hanh. */
    public boolean isBookable() {
        return status == TripStatus.SCHEDULED && departureTime.isAfter(LocalDateTime.now());
    }

    public Duration getDuration() {
        return Duration.between(departureTime, arrivalTime);
    }
}
