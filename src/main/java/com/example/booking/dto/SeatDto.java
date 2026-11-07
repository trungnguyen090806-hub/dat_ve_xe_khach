package com.example.booking.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class SeatDto {

    private final Long id;
    private final String seatCode;
    private final int floorNo;
    private final String seatType;
    private final String seatTypeLabel;
    private final BigDecimal price;
    private final BigDecimal surcharge;

    /**
     * Trang thai ghe:
     * - AVAILABLE    : Xanh la (Con trong)
     * - HELD_BY_ME   : Vang (Ban dang giu)
     * - HELD_BY_OTHER: Vang nhat (Nguoi khac dang giu)
     * - BOOKED       : Do (Da ban / Da dat)
     */
    private final String status;
    private final String statusLabel;
    private final boolean canSelect;
}
