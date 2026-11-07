package com.example.booking.model.enums;

/** Trang thai dung chung cho nha xe (ACTIVE/INACTIVE). */
public enum CompanyStatus {
    ACTIVE("Hoạt động"), INACTIVE("Ngừng hoạt động");

    private final String label;

    CompanyStatus(String label) { this.label = label; }

    public String getLabel() { return label; }
}
