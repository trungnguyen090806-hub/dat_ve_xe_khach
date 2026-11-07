package com.example.booking.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class HoldSeatsRequest {

    @NotNull(message = "Thiếu mã chuyến xe")
    private Long tripId;

    @NotEmpty(message = "Vui lòng chọn ít nhất một ghế")
    private List<Long> seatIds;
}
