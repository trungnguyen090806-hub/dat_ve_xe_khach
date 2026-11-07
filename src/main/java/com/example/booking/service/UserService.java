package com.example.booking.service;

import com.example.booking.dto.RegisterRequest;
import com.example.booking.model.User;

import java.util.Optional;

public interface UserService {

    /** Dang ky khach hang moi (ROLE_CUSTOMER, hang REGULAR). Nem DuplicateResourceException neu trung email/SDT. */
    User register(RegisterRequest request);

    Optional<User> findByIdentifier(String usernameOrEmail);

    User getById(Long id);
}
