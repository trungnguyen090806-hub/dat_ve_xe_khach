package com.example.booking.model.enums;

public enum TicketStatus {
    PENDING("Chờ thanh toán"),
    PAID("Đã thanh toán"),
    CANCELLED("Đã hủy"),
    EXPIRED("Hết hạn giữ chỗ"),
    COMPLETED("Đã hoàn thành");

    private final String label;

    TicketStatus(String label) { this.label = label; }

    public String getLabel() { return label; }
}
