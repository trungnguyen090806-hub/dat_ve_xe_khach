package com.example.booking.exception;

/** Khong tim thay chuyen xe, ghe, ve, ... */
public class ResourceNotFoundException extends BusinessException {
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " không tồn tại (id/mã: " + id + ")");
    }
}
