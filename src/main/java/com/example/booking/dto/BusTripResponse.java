package com.example.booking.dto;

import com.example.booking.model.BusTrip;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

@Getter
@Builder
public class BusTripResponse {

    private final Long id;
    private final String tripCode;
    private final Long busId;
    private final Long companyId;
    private final String companyName;
    private final String companyHotline;
    private final BigDecimal companyRatingAvg;
    private final int companyRatingCount;
    private final String licensePlate;
    private final String busType;
    private final String busTypeLabel;
    private final int totalSeats;
    private final long bookedSeats;
    private final long availableSeats;
    private final String origin;
    private final String destination;
    private final String pickupPoint;
    private final String dropoffPoint;
    private final LocalDateTime departureTime;
    private final LocalDateTime arrivalTime;
    private final BigDecimal basePrice;
    private final BigDecimal vipSurcharge;
    private final String status;
    private final String statusLabel;

    public static BusTripResponse from(BusTrip t, long occupiedSeats) {
        long available = Math.max(0, t.getBus().getTotalSeats() - occupiedSeats);
        return BusTripResponse.builder()
                .id(t.getId())
                .tripCode(t.getTripCode())
                .busId(t.getBus().getId())
                .companyId(t.getBus().getCompany().getId())
                .companyName(t.getBus().getCompany().getName())
                .companyHotline(t.getBus().getCompany().getHotline())
                .companyRatingAvg(t.getBus().getCompany().getRatingAvg())
                .companyRatingCount(t.getBus().getCompany().getRatingCount())
                .licensePlate(t.getBus().getLicensePlate())
                .busType(t.getBus().getBusType().name())
                .busTypeLabel(t.getBus().getBusType().getLabel())
                .totalSeats(t.getBus().getTotalSeats())
                .bookedSeats(occupiedSeats)
                .availableSeats(available)
                .origin(t.getOrigin())
                .destination(t.getDestination())
                .pickupPoint(t.getPickupPoint())
                .dropoffPoint(t.getDropoffPoint())
                .departureTime(t.getDepartureTime())
                .arrivalTime(t.getArrivalTime())
                .basePrice(t.getBasePrice())
                .vipSurcharge(t.getVipSurcharge())
                .status(t.getStatus().name())
                .statusLabel(t.getStatus().getLabel())
                .build();
    }

    public String getDurationText() {
        Duration d = Duration.between(departureTime, arrivalTime);
        long minutes = d.toMinutes();
        return (minutes / 60) + "h" + String.format("%02d", minutes % 60);
    }

    public boolean isEditable() {
        return "SCHEDULED".equals(status) || "RUNNING".equals(status);
    }
}
