package com.example.booking.service.pricing;

import com.example.booking.model.enums.MembershipType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class DefaultPricingStrategy implements PricingStrategy {

    @Override
    public BigDecimal calculateMemberDiscount(MembershipType membershipType, BigDecimal subtotalWithSurcharge) {
        if (membershipType == null || subtotalWithSurcharge == null || subtotalWithSurcharge.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal rate = membershipType.getDiscountRate();
        return subtotalWithSurcharge.multiply(rate).setScale(0, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal calculatePromotionDiscount(BigDecimal orderAmount, BigDecimal discountValue,
                                                 String discountType, BigDecimal maxDiscount) {
        if (orderAmount == null || discountValue == null || orderAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount;
        if ("PERCENT".equalsIgnoreCase(discountType)) {
            // Giam theo %: (orderAmount * discountValue) / 100
            discount = orderAmount.multiply(discountValue)
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            if (maxDiscount != null && maxDiscount.compareTo(BigDecimal.ZERO) > 0) {
                discount = discount.min(maxDiscount);
            }
        } else {
            // Giam theo so tien co dinh
            discount = discountValue;
        }

        return discount.min(orderAmount);
    }
}
