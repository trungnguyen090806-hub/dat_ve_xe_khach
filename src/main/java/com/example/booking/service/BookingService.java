package com.example.booking.service;

import com.example.booking.dto.BookingCheckoutForm;
import com.example.booking.dto.BookingCheckoutView;
import com.example.booking.model.Ticket;

import java.util.List;

public interface BookingService {

    /**
     * Chuan bi du lieu cho trang Checkout xac nhan dat ve va tinh toan tien truoc khi thanh toan
     */
    BookingCheckoutView prepareCheckout(Long tripId, List<Long> seatIds, String promoCode, Long userId);

    /**
     * Xu ly dat ve, tao giao dich thanh toan, xac nhan giu cho va tich diem nang hang
     */
    Ticket processBooking(BookingCheckoutForm form, Long userId);

    /**
     * Lay thong tin chi tiet ve xe theo ma ve (kiem tra quyen so huu)
     */
    Ticket getTicketByCode(String ticketCode, Long userId, boolean isAdmin);

    /**
     * Sinh ma QR dang Base64 PNG chua thong tin tra cuu ve
     */
    String generateTicketQrBase64(Ticket ticket);
}
