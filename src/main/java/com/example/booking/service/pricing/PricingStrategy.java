package com.example.booking.service.pricing;

import com.example.booking.model.enums.MembershipType;

import java.math.BigDecimal;

/**
 * Strategy Pattern (OOP - Polymorphism & Abstraction):
 * Tinh toan giam gia theo hang thanh vien va khuyen mai.
 */
public interface PricingStrategy {

    /**
     * Tinh so tien duoc giam theo hang thanh vien:
     * - REGULAR: 0%
     * - MEMBER : 5%
     * - VIP    : 10%
     * Tinh tren tong tien ve goc + phu phi VIP.
     */
    BigDecimal calculateMemberDiscount(MembershipType membershipType, BigDecimal subtotalWithSurcharge);

    /**
     * Kiem tra va tinh muc giam gia khuyen mai neu co ma hop le
     */
    BigDecimal calculatePromotionDiscount(BigDecimal orderAmount, BigDecimal discountValue,
                                          String discountType, BigDecimal maxDiscount);
}
