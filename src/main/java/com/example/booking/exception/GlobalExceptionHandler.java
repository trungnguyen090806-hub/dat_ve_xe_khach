package com.example.booking.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

/**
 * Xu ly loi tap trung. Request AJAX/JSON nhan ve JSON {success:false, message}, request thuong nhan trang loi.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public Object handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return respond(ex.getMessage(), HttpStatus.NOT_FOUND, "error/404", request);
    }

    @ExceptionHandler(BusinessException.class)
    public Object handleBusiness(BusinessException ex, HttpServletRequest request) {
        return respond(ex.getMessage(), HttpStatus.BAD_REQUEST, "error/error", request);
    }

    @ExceptionHandler(Exception.class)
    public Object handleUnexpected(Exception ex, HttpServletRequest request) throws Exception {
        // De Spring Security xu ly 403/401 va cac exception framework dac biet
        if (ex instanceof org.springframework.security.access.AccessDeniedException
                || ex instanceof org.springframework.security.core.AuthenticationException
                || ex instanceof org.springframework.web.ErrorResponse) { // 404 tai nguyen, 405, 400... de Spring xu ly
            throw ex;
        }
        log.error("Loi khong mong doi tai {}", request.getRequestURI(), ex);
        return respond("Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau.", HttpStatus.INTERNAL_SERVER_ERROR,
                "error/5xx", request);
    }

    private Object respond(String message, HttpStatus status, String view, HttpServletRequest request) {
        if (isAjax(request)) {
            return ResponseEntity.status(status).body(Map.of("success", false, "message", message));
        }
        ModelAndView mav = new ModelAndView(view);
        mav.setStatus(status);
        mav.addObject("message", message);
        return mav;
    }

    private boolean isAjax(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        return "XMLHttpRequest".equals(request.getHeader("X-Requested-With"))
                || (accept != null && accept.contains("application/json"))
                || request.getRequestURI().startsWith("/api/");
    }
}
