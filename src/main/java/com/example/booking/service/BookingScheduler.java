package com.example.booking.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Task dinh ky giai phong ghe giu tam va ve PENDING qua 10 phut.
 */
@Component
public class BookingScheduler {

    private static final Logger log = LoggerFactory.getLogger(BookingScheduler.class);

    private final SeatBookingService seatBookingService;

    public BookingScheduler(SeatBookingService seatBookingService) {
        this.seatBookingService = seatBookingService;
    }

    @Scheduled(fixedRateString = "${app.booking.hold-cleanup-rate-ms:60000}")
    public void scheduleHoldCleanup() {
        try {
            int cleaned = seatBookingService.cleanupExpiredHolds();
            if (cleaned > 0) {
                log.debug("Scheduler: Da giai phong {} ghe het han", cleaned);
            }
        } catch (Exception ex) {
            log.error("Loi khi quet giai phong ghe giu tam", ex);
        }
    }
}
