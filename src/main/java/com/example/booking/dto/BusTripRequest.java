package com.example.booking.dto;

import com.example.booking.model.enums.TripStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class BusTripRequest {

    private Long id; // null = them moi

    @Size(max = 20, message = "Mã chuyến tối đa 20 ký tự")
    private String tripCode; // de trong -> he thong tu sinh

    @NotNull(message = "Vui lòng chọn xe")
    private Long busId;

    @NotBlank(message = "Vui lòng nhập điểm đi")
    @Size(max = 100)
    private String origin;

    @NotBlank(message = "Vui lòng nhập điểm đến")
    @Size(max = 100)
    private String destination;

    @Size(max = 255)
    private String pickupPoint;

    @Size(max = 255)
    private String dropoffPoint;

    @NotNull(message = "Vui lòng chọn ngày giờ khởi hành")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime departureTime;

    @NotNull(message = "Vui lòng chọn ngày giờ đến dự kiến")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime arrivalTime;

    @NotNull(message = "Vui lòng nhập giá vé")
    @Positive(message = "Giá vé phải lớn hơn 0")
    private BigDecimal basePrice;

    @NotNull(message = "Vui lòng nhập phụ phí ghế VIP")
    @PositiveOrZero(message = "Phụ phí VIP không được âm")
    private BigDecimal vipSurcharge;

    private TripStatus status = TripStatus.SCHEDULED;
}
