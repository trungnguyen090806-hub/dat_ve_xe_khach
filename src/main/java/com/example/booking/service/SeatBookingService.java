package com.example.booking.service;

import com.example.booking.dto.TripSeatMapResponse;
import com.example.booking.model.SeatHold;

import java.util.List;

public interface SeatBookingService {

    /** Lay so do ghe 2 tang va trang thai thoi gian thuc cua mot chuyen */
    TripSeatMapResponse getSeatMap(Long tripId, Long currentUserId);

    /**
     * Giu ghe tam 10 phut cho khach hang.
     * Ap dung @Transactional va rang buoc du lieu de ngan chan 2 nguoi cung dat 1 ghe.
     */
    List<SeatHold> holdSeats(Long tripId, List<Long> seatIds, Long userId);

    /** Giai phong ghe dang giu cua khach khi ho thoat hoac chon lai */
    void releaseUserHolds(Long tripId, Long userId);

    /** Giai phong cac ghe giu qua 10 phut chua thanh toan */
    int cleanupExpiredHolds();
}
