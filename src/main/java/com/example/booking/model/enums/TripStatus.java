package com.example.booking.model.enums;

public enum TripStatus {
    SCHEDULED("Sắp khởi hành"),
    RUNNING("Đang chạy"),
    COMPLETED("Hoàn thành"),
    CANCELLED("Đã hủy");

    private final String label;

    TripStatus(String label) { this.label = label; }

    public String getLabel() { return label; }
}
