package com.example.booking.service.impl;

import com.example.booking.dto.RegisterRequest;
import com.example.booking.exception.BusinessException;
import com.example.booking.exception.DuplicateResourceException;
import com.example.booking.exception.ResourceNotFoundException;
import com.example.booking.model.Role;
import com.example.booking.model.User;
import com.example.booking.model.enums.MembershipType;
import com.example.booking.repository.RoleRepository;
import com.example.booking.repository.UserRepository;
import com.example.booking.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public User register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        String phone = request.getPhone().trim();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("email", "Email đã được sử dụng");
        }
        if (userRepository.existsByPhone(phone)) {
            throw new DuplicateResourceException("phone", "Số điện thoại đã được sử dụng");
        }

        Role customerRole = roleRepository.findByName(Role.CUSTOMER)
                .orElseThrow(() -> new BusinessException("Chưa khởi tạo vai trò ROLE_CUSTOMER. Hãy chạy file SQL."));

        User user = new User();
        user.setFullName(request.getFullName().trim());
        user.setEmail(email);
        user.setPhone(phone);
        user.setUsername(generateUsername(email));
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setMembershipType(MembershipType.REGULAR);
        user.getRoles().add(customerRole);
        try {
            return userRepository.save(user);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            // Truong hop 2 request dang ky cung luc vuot qua buoc kiem tra o tren
            throw new DuplicateResourceException("email", "Email hoặc số điện thoại đã được sử dụng");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByIdentifier(String usernameOrEmail) {
        return userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(usernameOrEmail, usernameOrEmail);
    }

    @Override
    @Transactional(readOnly = true)
    public User getById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Người dùng", id));
    }

    /** Username = phan truoc @ cua email, bo dau/ky tu la; them so neu bi trung. */
    private String generateUsername(String email) {
        String base = email.substring(0, email.indexOf('@'));
        base = Normalizer.normalize(base, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase().replaceAll("[^a-z0-9._]", "");
        if (base.length() < 3) base = "user" + base;
        if (base.length() > 40) base = base.substring(0, 40);
        String candidate = base;
        int i = 1;
        while (userRepository.existsByUsernameIgnoreCase(candidate)) {
            candidate = base + (i++);
        }
        return candidate;
    }
}
