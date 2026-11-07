package com.example.booking.dto;

import com.example.booking.model.BusCompany;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class BusCompanyResponse {

    private final Long id;
    private final String name;
    private final String hotline;
    private final String email;
    private final String address;
    private final String description;
    private final BigDecimal ratingAvg;
    private final int ratingCount;
    private final String status;
    private final String statusLabel;
    private final long busCount;
    private final long tripCount;

    public static BusCompanyResponse from(BusCompany c, long busCount, long tripCount) {
        return BusCompanyResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .hotline(c.getHotline())
                .email(c.getEmail())
                .address(c.getAddress())
                .description(c.getDescription())
                .ratingAvg(c.getRatingAvg())
                .ratingCount(c.getRatingCount())
                .status(c.getStatus().name())
                .statusLabel(c.getStatus().getLabel())
                .busCount(busCount)
                .tripCount(tripCount)
                .build();
    }
}
