package com.example.booking.controller;

import com.example.booking.dto.ForgotPasswordRequest;
import com.example.booking.dto.RegisterRequest;
import com.example.booking.exception.DuplicateResourceException;
import com.example.booking.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Controller chi dieu huong; nghiep vu nam o UserService. */
@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginPage(Authentication authentication) {
        return isLoggedIn(authentication) ? "redirect:/" : "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Authentication authentication, Model model) {
        if (isLoggedIn(authentication)) return "redirect:/";
        model.addAttribute("registerRequest", new RegisterRequest());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerRequest") RegisterRequest request,
                           BindingResult bindingResult, RedirectAttributes redirect) {
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }
        try {
            userService.register(request);
        } catch (DuplicateResourceException ex) {
            bindingResult.rejectValue(ex.getField(), "duplicate", ex.getMessage());
            return "auth/register";
        }
        redirect.addFlashAttribute("toastSuccess", "Đăng ký thành công! Vui lòng đăng nhập.");
        return "redirect:/login";
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage(Model model) {
        model.addAttribute("forgotPasswordRequest", new ForgotPasswordRequest());
        return "auth/forgot-password";
    }

    /** DEMO: chua gui email that; luon tra thong bao chung de khong lo viec email co ton tai hay khong. */
    @PostMapping("/forgot-password")
    public String forgotPassword(@Valid @ModelAttribute("forgotPasswordRequest") ForgotPasswordRequest request,
                                 BindingResult bindingResult, RedirectAttributes redirect) {
        if (bindingResult.hasErrors()) {
            return "auth/forgot-password";
        }
        redirect.addFlashAttribute("toastSuccess",
                "Nếu email tồn tại, hướng dẫn đặt lại mật khẩu đã được gửi (chế độ DEMO – chưa gửi email thật).");
        return "redirect:/login";
    }

    private boolean isLoggedIn(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
