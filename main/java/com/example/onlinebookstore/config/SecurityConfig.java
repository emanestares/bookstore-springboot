package com.example.onlinebookstore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security Configuration
 *
 * ROOT CAUSE OF CHECKOUT BUG:
 * The previous config marked /api/orders/** as .authenticated()
 * but the app never issues JWT tokens — it only stores user data
 * in localStorage on the frontend. So every POST /api/orders
 * was blocked with 401 Unauthorized before it even reached the controller.
 *
 * FIX: Since this app uses a simple userId-based approach (not JWT),
 * we permit all API endpoints and handle authorization in the service layer
 * (e.g. verifying userId exists before placing an order).
 *
 * If you want real JWT auth in the future, that requires a full
 * JWT filter chain — see the capstone tutorial files for that.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // All API endpoints — open (auth is handled in service layer via userId)
                .requestMatchers("/api/**").permitAll()
                // All static frontend files
                .requestMatchers("/**").permitAll()
            );

        return http.build();
    }
}
