package com.example.booking.controller.admin;

import com.example.booking.exception.BusinessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.stream.Collectors;

/** Lop cha cho cac controller admin: gom logic "chay thao tac -> bao Toast -> redirect". */
public abstract class AdminBaseController {

    protected static final int PAGE_SIZE = 10;

    /** Chay action; thanh cong -> toastSuccess, loi nghiep vu -> toastError. Luon redirect ve redirectUrl. */
    protected String execute(RedirectAttributes ra, String redirectUrl, String successMessage, Runnable action) {
        try {
            action.run();
            ra.addFlashAttribute("toastSuccess", successMessage);
        } catch (BusinessException ex) {
            ra.addFlashAttribute("toastError", ex.getMessage());
        } catch (DataIntegrityViolationException ex) {
            ra.addFlashAttribute("toastError", "Dữ liệu bị trùng hoặc vi phạm ràng buộc (ví dụ ghế đã có người đặt).");
        }
        return "redirect:" + redirectUrl;
    }

    /** Gop cac loi validate thanh mot chuoi de hien thi Toast. */
    protected String validationMessage(BindingResult result) {
        return result.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .distinct()
                .collect(Collectors.joining(" • "));
    }

    protected String statusParam(Enum<?> status) {
        return status == null ? "" : status.name();
    }
}
