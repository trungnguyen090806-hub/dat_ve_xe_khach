package com.example.booking.model.enums;

public enum BusType {
    SLEEPER("Giường nằm"),
    LIMOUSINE("Limousine"),
    SEATER("Ghế ngồi");

    private final String label;

    BusType(String label) { this.label = label; }

    public String getLabel() { return label; }
}
