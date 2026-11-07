package com.example.booking.service;

import com.example.booking.model.enums.MembershipType;
import com.example.booking.service.pricing.DefaultPricingStrategy;
import com.example.booking.service.pricing.PricingStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PricingStrategyTest {

    private PricingStrategy pricingStrategy;

    @BeforeEach
    void setUp() {
        pricingStrategy = new DefaultPricingStrategy();
    }

    @Test
    @DisplayName("Khách hàng REGULAR không được giảm giá (0%)")
    void testRegularMemberDiscount() {
        BigDecimal total = new BigDecimal("300000");
        BigDecimal discount = pricingStrategy.calculateMemberDiscount(MembershipType.REGULAR, total);

        assertEquals(new BigDecimal("0"), discount);
    }

    @Test
    @DisplayName("Khách hàng MEMBER được giảm giá 5%")
    void testMemberDiscount() {
        BigDecimal total = new BigDecimal("300000");
        BigDecimal discount = pricingStrategy.calculateMemberDiscount(MembershipType.MEMBER, total);

        // 300.000 * 5% = 15.000
        assertEquals(new BigDecimal("15000"), discount);
    }

    @Test
    @DisplayName("Khách hàng VIP đặt vé và được giảm giá 10%")
    void testVipMemberDiscount() {
        BigDecimal total = new BigDecimal("300000");
        BigDecimal discount = pricingStrategy.calculateMemberDiscount(MembershipType.VIP, total);

        // 300.000 * 10% = 30.000
        assertEquals(new BigDecimal("30000"), discount);
    }

    @Test
    @DisplayName("Khuyến mãi PERCENT có áp dụng mức giảm tối đa maxDiscount")
    void testPromotionDiscountPercentWithMax() {
        BigDecimal orderAmount = new BigDecimal("1000000");
        BigDecimal discountValue = new BigDecimal("10"); // 10% = 100.000d
        BigDecimal maxDiscount = new BigDecimal("50000"); // Tran 50.000d

        BigDecimal discount = pricingStrategy.calculatePromotionDiscount(
                orderAmount, discountValue, "PERCENT", maxDiscount);

        assertEquals(new BigDecimal("50000"), discount);
    }

    @Test
    @DisplayName("Khuyến mãi AMOUNT giảm số tiền cố định")
    void testPromotionDiscountAmount() {
        BigDecimal orderAmount = new BigDecimal("500000");
        BigDecimal discountValue = new BigDecimal("50000");

        BigDecimal discount = pricingStrategy.calculatePromotionDiscount(
                orderAmount, discountValue, "AMOUNT", null);

        assertEquals(new BigDecimal("50000"), discount);
    }
}
