package com.example.booking.controller;

import com.example.booking.config.CustomUserDetails;
import com.example.booking.dto.ChangePasswordRequest;
import com.example.booking.dto.ProfileUpdateRequest;
import com.example.booking.exception.BusinessException;
import com.example.booking.exception.DuplicateResourceException;
import com.example.booking.model.Payment;
import com.example.booking.model.User;
import com.example.booking.model.enums.MembershipType;
import com.example.booking.service.CustomerProfileService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Controller
@RequestMapping("/profile")
public class CustomerProfileController {

    private final CustomerProfileService profileService;

    public CustomerProfileController(CustomerProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public String profilePage(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        User user = profileService.getProfile(userDetails.getId());
        List<Payment> payments = profileService.getPaymentHistory(userDetails.getId());

        ProfileUpdateRequest updateReq = new ProfileUpdateRequest();
        updateReq.setFullName(user.getFullName());
        updateReq.setPhone(user.getPhone());
        updateReq.setEmail(user.getEmail());

        model.addAttribute("user", user);
        model.addAttribute("payments", payments);
        model.addAttribute("profileReq", updateReq);
        model.addAttribute("passwordReq", new ChangePasswordRequest());

        // Tinh toan tien trinh tich luy hang thanh vien
        BigDecimal spent = user.getTotalSpent();
        BigDecimal nextTarget = BigDecimal.ZERO;
        int progressPercent = 100;
        String nextRankLabel = "Đã đạt hạng cao nhất";

        if (user.getMembershipType() == MembershipType.REGULAR) {
            nextTarget = new BigDecimal("2000000"); // 2 trieu de len MEMBER
            nextRankLabel = "Thành viên (MEMBER)";
            progressPercent = spent.multiply(BigDecimal.valueOf(100))
                    .divide(nextTarget, 0, RoundingMode.FLOOR).intValue();
            progressPercent = Math.min(100, progressPercent);
        } else if (user.getMembershipType() == MembershipType.MEMBER) {
            nextTarget = new BigDecimal("10000000"); // 10 trieu de len VIP
            nextRankLabel = "Khách hàng VIP";
            progressPercent = spent.multiply(BigDecimal.valueOf(100))
                    .divide(nextTarget, 0, RoundingMode.FLOOR).intValue();
            progressPercent = Math.min(100, progressPercent);
        }

        model.addAttribute("spent", spent);
        model.addAttribute("nextTarget", nextTarget);
        model.addAttribute("progressPercent", progressPercent);
        model.addAttribute("nextRankLabel", nextRankLabel);

        return "user/profile";
    }

    @PostMapping("/update")
    public String updateProfile(@Valid @ModelAttribute("profileReq") ProfileUpdateRequest request,
                                BindingResult result,
                                @AuthenticationPrincipal CustomUserDetails userDetails,
                                RedirectAttributes ra) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        if (result.hasErrors()) {
            ra.addFlashAttribute("toastError", "Thông tin nhập không hợp lệ. Vui lòng kiểm tra lại.");
            return "redirect:/profile";
        }

        try {
            profileService.updateProfile(userDetails.getId(), request);
            ra.addFlashAttribute("toastSuccess", "Cập nhật thông tin cá nhân thành công!");
        } catch (DuplicateResourceException ex) {
            ra.addFlashAttribute("toastError", ex.getMessage());
        } catch (BusinessException ex) {
            ra.addFlashAttribute("toastError", ex.getMessage());
        }
        return "redirect:/profile";
    }

    @PostMapping("/change-password")
    public String changePassword(@Valid @ModelAttribute("passwordReq") ChangePasswordRequest request,
                                 BindingResult result,
                                 @AuthenticationPrincipal CustomUserDetails userDetails,
                                 RedirectAttributes ra) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        if (result.hasErrors()) {
            ra.addFlashAttribute("toastError", "Mật khẩu mới phải có tối thiểu 6 ký tự.");
            return "redirect:/profile";
        }

        try {
            profileService.changePassword(userDetails.getId(), request);
            ra.addFlashAttribute("toastSuccess", "Đổi mật khẩu thành công! Mật khẩu mới đã được cập nhật.");
        } catch (BusinessException ex) {
            ra.addFlashAttribute("toastError", ex.getMessage());
        }
        return "redirect:/profile";
    }
}
