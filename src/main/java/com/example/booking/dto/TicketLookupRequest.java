package com.example.booking.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TicketLookupRequest {

    @NotBlank(message = "Vui lòng nhập mã vé xe")
    private String ticketCode;

    @NotBlank(message = "Vui lòng nhập số điện thoại đặt vé")
    private String phone;
}
