package com.example.booking.exception;

/** Loi nghiep vu (vi pham quy tac): dat ghe da ban, thanh toan 2 lan, ... */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
