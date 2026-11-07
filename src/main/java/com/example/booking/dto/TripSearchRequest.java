package com.example.booking.dto;

import com.example.booking.model.enums.BusType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class TripSearchRequest {

    private String origin;
    private String destination;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate departureDate;

    private Integer passengers = 1;

    private BigDecimal minPrice;
    private BigDecimal maxPrice;

    private BusType busType;
    private Long companyId;

    /**
     * Khung gio khoi hanh:
     * - EARLY_MORNING (00:00 - 06:00)
     * - MORNING       (06:00 - 12:00)
     * - AFTERNOON     (12:00 - 18:00)
     * - EVENING       (18:00 - 24:00)
     */
    private String timeRange;

    /**
     * Sap xep:
     * - price_asc   : Gia thap den cao
     * - price_desc  : Gia cao den thap
     * - time_asc    : Gio khoi hanh som nhat (mac dinh)
     * - rating_desc : Danh gia cao nhat
     */
    private String sortBy = "time_asc";

    private int page = 0;
    private int size = 8;
}
