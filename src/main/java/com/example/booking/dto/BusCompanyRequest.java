package com.example.booking.dto;

import com.example.booking.model.enums.CompanyStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BusCompanyRequest {

    private Long id; // null = them moi

    @NotBlank(message = "Vui lòng nhập tên nhà xe")
    @Size(max = 120, message = "Tên nhà xe tối đa 120 ký tự")
    private String name;

    @NotBlank(message = "Vui lòng nhập hotline")
    @Pattern(regexp = "^[0-9 +]{8,20}$", message = "Hotline không hợp lệ")
    private String hotline;

    @Email(message = "Email không hợp lệ")
    @Size(max = 120, message = "Email tối đa 120 ký tự")
    private String email;

    @NotBlank(message = "Vui lòng nhập địa chỉ")
    @Size(max = 255, message = "Địa chỉ tối đa 255 ký tự")
    private String address;

    @Size(max = 2000, message = "Mô tả tối đa 2000 ký tự")
    private String description;

    @NotNull(message = "Vui lòng chọn trạng thái")
    private CompanyStatus status = CompanyStatus.ACTIVE;
}
