package com.example.booking.dto;

import com.example.booking.model.enums.BusType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BusRequest {

    @NotNull(message = "Thiếu nhà xe")
    private Long companyId;

    @NotBlank(message = "Vui lòng nhập biển số xe")
    @Size(max = 20, message = "Biển số tối đa 20 ký tự")
    private String licensePlate;

    @NotNull(message = "Vui lòng chọn loại xe")
    private BusType busType;

    @NotNull(message = "Vui lòng nhập tổng số ghế")
    @Min(value = 4, message = "Xe tối thiểu 4 ghế")
    @Max(value = 60, message = "Xe tối đa 60 ghế")
    private Integer totalSeats;

    @NotNull(message = "Vui lòng chọn số tầng")
    @Min(value = 1, message = "Số tầng là 1 hoặc 2")
    @Max(value = 2, message = "Số tầng là 1 hoặc 2")
    private Integer floors = 2;
}
