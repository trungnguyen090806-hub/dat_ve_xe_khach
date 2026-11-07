package com.example.booking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String REMEMBER_ME_KEY = "busgo-remember-me-secret-key";

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, CustomUserDetailsService userDetailsService)
            throws Exception {
        http
            .csrf(csrf -> csrf
                // Bo qua CSRF cho cac API endpoint JSON dung Fetch / AJAX
                .ignoringRequestMatchers("/api/**", "/trips/*/api/**"))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/js/**", "/images/**", "/uploads/**", "/favicon.ico", "/error/**").permitAll()
                .requestMatchers("/", "/login", "/register", "/forgot-password", "/lookup", "/403").permitAll()
                // Khach vang lai duoc tim chuyen va xem so do ghe (giai doan 5, 6)
                .requestMatchers("/search", "/trips/**", "/companies/**", "/api/public/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .successHandler(successHandler())
                .failureHandler(failureHandler())
                .permitAll())
            .rememberMe(rm -> rm
                .key(REMEMBER_ME_KEY)
                .rememberMeParameter("remember-me")
                .tokenValiditySeconds(14 * 24 * 60 * 60)
                .userDetailsService(userDetailsService))
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .deleteCookies("JSESSIONID", "remember-me")
                .permitAll())
            .exceptionHandling(ex -> ex.accessDeniedPage("/403"));
        return http.build();
    }

    /** Admin -> dashboard; khach hang -> trang dang xem truoc khi bi chuyen toi login (neu co), nguoc lai "/". */
    @Bean
    public AuthenticationSuccessHandler successHandler() {
        SavedRequestAwareAuthenticationSuccessHandler customer = new SavedRequestAwareAuthenticationSuccessHandler();
        customer.setDefaultTargetUrl("/");
        return (request, response, authentication) -> {
            boolean admin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            if (admin) {
                response.sendRedirect(request.getContextPath() + "/admin/dashboard");
            } else {
                customer.onAuthenticationSuccess(request, response, authentication);
            }
        };
    }

    @Bean
    public AuthenticationFailureHandler failureHandler() {
        return (request, response, exception) -> {
            String target = exception instanceof DisabledException ? "/login?disabled" : "/login?error";
            response.sendRedirect(request.getContextPath() + target);
        };
    }
}
