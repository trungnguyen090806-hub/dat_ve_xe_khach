package com.example.booking.dto;

import com.example.booking.model.Bus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BusResponse {

    private final Long id;
    private final Long companyId;
    private final String companyName;
    private final String licensePlate;
    private final String busTypeLabel;
    private final int totalSeats;
    private final int floors;
    private final String status;

    public static BusResponse from(Bus b) {
        return BusResponse.builder()
                .id(b.getId())
                .companyId(b.getCompany().getId())
                .companyName(b.getCompany().getName())
                .licensePlate(b.getLicensePlate())
                .busTypeLabel(b.getBusType().getLabel())
                .totalSeats(b.getTotalSeats())
                .floors(b.getFloors())
                .status(b.getStatus().name())
                .build();
    }

    /** Nhan hien thi trong dropdown chon xe khi tao chuyen. */
    public String getDisplayName() {
        return companyName + " – " + licensePlate + " (" + busTypeLabel + ", " + totalSeats + " ghế)";
    }
}
