package com.example.booking.service;

import com.example.booking.dto.RegisterRequest;
import com.example.booking.exception.DuplicateResourceException;
import com.example.booking.model.Role;
import com.example.booking.model.User;
import com.example.booking.model.enums.MembershipType;
import com.example.booking.repository.RoleRepository;
import com.example.booking.repository.UserRepository;
import com.example.booking.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setFullName("Lê Văn Thử Nghiệm");
        registerRequest.setEmail("test@busgo.vn");
        registerRequest.setPhone("0987654321");
        registerRequest.setPassword("password123");
        registerRequest.setConfirmPassword("password123");
    }

    @Test
    @DisplayName("Đăng ký tài khoản thành công")
    void testRegisterSuccess() {
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(userRepository.existsByPhone(anyString())).thenReturn(false);
        when(roleRepository.findByName(Role.CUSTOMER)).thenReturn(Optional.of(new Role(Role.CUSTOMER)));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = userService.register(registerRequest);

        assertNotNull(user);
        assertEquals("Lê Văn Thử Nghiệm", user.getFullName());
        assertEquals("test@busgo.vn", user.getEmail());
        assertEquals("0987654321", user.getPhone());
        assertEquals("hashed_password", user.getPassword());
        assertEquals(MembershipType.REGULAR, user.getMembershipType());
        assertTrue(user.hasRole(Role.CUSTOMER));
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Đăng ký email đã tồn tại -> Ném DuplicateResourceException")
    void testRegisterDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("test@busgo.vn")).thenReturn(true);

        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class, () -> {
            userService.register(registerRequest);
        });

        assertEquals("email", ex.getField());
        assertTrue(ex.getMessage().contains("Email đã được sử dụng"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Đăng ký số điện thoại đã tồn tại -> Ném DuplicateResourceException")
    void testRegisterDuplicatePhone() {
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(userRepository.existsByPhone("0987654321")).thenReturn(true);

        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class, () -> {
            userService.register(registerRequest);
        });

        assertEquals("phone", ex.getField());
        assertTrue(ex.getMessage().contains("Số điện thoại đã được sử dụng"));
        verify(userRepository, never()).save(any(User.class));
    }
}
