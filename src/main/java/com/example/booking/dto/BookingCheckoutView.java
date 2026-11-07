package com.example.booking.dto;

import com.example.booking.model.enums.MembershipType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class BookingCheckoutView {

    private final BusTripResponse trip;
    private final List<SeatDto> seats;

    private final String defaultPassengerName;
    private final String defaultPassengerPhone;
    private final String defaultPassengerEmail;

    private final BigDecimal subtotal;
    private final BigDecimal vipSurchargeTotal;
    private final MembershipType membershipType;
    private final BigDecimal memberDiscountRate;
    private final BigDecimal memberDiscount;

    private final String promoCode;
    private final BigDecimal promoDiscount;
    private final String promoMessage;

    private final BigDecimal totalAmount;
}
