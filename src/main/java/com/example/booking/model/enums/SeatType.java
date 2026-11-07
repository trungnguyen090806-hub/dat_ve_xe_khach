package com.example.booking.model.enums;

public enum SeatType {
    NORMAL("Ghế thường"),
    VIP("Ghế VIP");

    private final String label;

    SeatType(String label) { this.label = label; }

    public String getLabel() { return label; }
}
