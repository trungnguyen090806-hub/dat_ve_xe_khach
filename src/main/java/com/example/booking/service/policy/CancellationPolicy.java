package com.example.booking.service.policy;

import com.example.booking.model.Ticket;

import java.math.BigDecimal;

/**
 * Interface ap dung OOP (Polymorphism & Abstraction) cho chinh sach huy ve xe.
 */
public interface CancellationPolicy {

    /**
     * Kiem tra ve co du dieu kien de huy hay khong
     */
    boolean canCancel(Ticket ticket);

    /**
     * Tinh ty le hoan tien (1.0 = 100%, 0.7 = 70%, 0.0 = khong hoan)
     */
    BigDecimal calculateRefundRate(Ticket ticket);

    /**
     * Tinh so tien thuc te duoc hoan
     */
    BigDecimal calculateRefundAmount(Ticket ticket);

    /**
     * Mo ta chinh sach ap dung cho ve nay
     */
    String getPolicyDescription(Ticket ticket);
}
