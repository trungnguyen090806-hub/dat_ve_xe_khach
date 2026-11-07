package com.example.booking.exception;

/** Du lieu trung (email, so dien thoai, ...). Mang theo ten truong de gan loi vao form. */
public class DuplicateResourceException extends BusinessException {

    private final String field;

    public DuplicateResourceException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
