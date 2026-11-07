package com.example.booking.dto;

import com.example.booking.model.enums.PaymentMethod;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BookingCheckoutForm {

    @NotNull(message = "Thiếu thông tin chuyến xe")
    private Long tripId;

    @NotEmpty(message = "Vui lòng chọn ít nhất một ghế")
    private List<Long> seatIds;

    @NotBlank(message = "Vui lòng nhập họ tên hành khách")
    @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
    private String passengerName;

    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @Pattern(regexp = "^(0|\\+84)\\d{9}$", message = "Số điện thoại không hợp lệ (VD: 0912345678)")
    private String passengerPhone;

    @Email(message = "Email không hợp lệ")
    @Size(max = 120, message = "Email tối đa 120 ký tự")
    private String passengerEmail;

    private String promoCode;

    @NotNull(message = "Vui lòng chọn phương thức thanh toán")
    private PaymentMethod paymentMethod = PaymentMethod.BANK_TRANSFER;

    @Size(max = 255, message = "Ghi chú tối đa 255 ký tự")
    private String note;
}
