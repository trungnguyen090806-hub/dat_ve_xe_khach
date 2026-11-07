package com.example.booking.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class TripSeatMapResponse {

    private final BusTripResponse trip;
    private final List<SeatDto> floor1Seats;
    private final List<SeatDto> floor2Seats;
    private final int totalFloors;
    private final BigDecimal basePrice;
    private final BigDecimal vipSurcharge;
    private final int holdMinutes;
    private final LocalDateTime currentHoldExpiresAt;
    private final List<Long> myHeldSeatIds;
}
