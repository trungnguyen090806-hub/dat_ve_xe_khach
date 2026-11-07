package com.example.booking.model.enums;

import java.math.BigDecimal;

/** Hang thanh vien. Moi hang co ty le giam gia rieng (dong goi chinh sach vao enum). */
public enum MembershipType {
    REGULAR("Khách thường", "0.00"),
    MEMBER("Thành viên", "0.05"),
    VIP("VIP", "0.10");

    private final String label;
    private final BigDecimal discountRate;

    MembershipType(String label, String discountRate) {
        this.label = label;
        this.discountRate = new BigDecimal(discountRate);
    }

    public String getLabel() { return label; }

    public BigDecimal getDiscountRate() { return discountRate; }
}
