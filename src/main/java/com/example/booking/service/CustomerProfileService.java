package com.example.booking.service;

import com.example.booking.dto.ChangePasswordRequest;
import com.example.booking.dto.ProfileUpdateRequest;
import com.example.booking.model.Payment;
import com.example.booking.model.User;

import java.util.List;

public interface CustomerProfileService {

    User getProfile(Long userId);

    void updateProfile(Long userId, ProfileUpdateRequest request);

    void changePassword(Long userId, ChangePasswordRequest request);

    List<Payment> getPaymentHistory(Long userId);
}
