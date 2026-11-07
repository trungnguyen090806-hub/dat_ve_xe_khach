package com.example.booking.service.impl;

import com.example.booking.dto.ChangePasswordRequest;
import com.example.booking.dto.ProfileUpdateRequest;
import com.example.booking.exception.BusinessException;
import com.example.booking.exception.DuplicateResourceException;
import com.example.booking.exception.ResourceNotFoundException;
import com.example.booking.model.Payment;
import com.example.booking.model.User;
import com.example.booking.repository.PaymentRepository;
import com.example.booking.repository.UserRepository;
import com.example.booking.service.CustomerProfileService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CustomerProfileServiceImpl implements CustomerProfileService {

    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomerProfileServiceImpl(UserRepository userRepository,
                                      PaymentRepository paymentRepository,
                                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.paymentRepository = paymentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public User getProfile(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", userId));
    }

    @Override
    @Transactional
    public void updateProfile(Long userId, ProfileUpdateRequest request) {
        User user = getProfile(userId);

        String email = request.getEmail().trim().toLowerCase();
        String phone = request.getPhone().trim();

        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, userId)) {
            throw new DuplicateResourceException("email", "Email này đã được sử dụng bởi tài khoản khác");
        }
        if (userRepository.existsByPhoneAndIdNot(phone, userId)) {
            throw new DuplicateResourceException("phone", "Số điện thoại này đã được sử dụng bởi tài khoản khác");
        }

        user.setFullName(request.getFullName().trim());
        user.setEmail(email);
        user.setPhone(phone);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = getProfile(userId);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException("Mật khẩu hiện tại không chính xác");
        }

        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new BusinessException("Mật khẩu mới và xác nhận mật khẩu không khớp");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BusinessException("Mật khẩu mới không được trùng với mật khẩu hiện tại");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> getPaymentHistory(Long userId) {
        return paymentRepository.findByTicketUserIdOrderByCreatedAtDesc(userId);
    }
}
