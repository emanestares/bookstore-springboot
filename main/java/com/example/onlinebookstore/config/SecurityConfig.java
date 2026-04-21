package com.example.onlinebookstore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

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

                        // AUTH
                        .requestMatchers("/api/auth/**").permitAll()

                        // BOOKS (public)
                        .requestMatchers(HttpMethod.GET, "/api/books/**").permitAll()

                        // 🔒 ORDERS (MUST BE AUTHENTICATED)
                        .requestMatchers("/api/orders/**").authenticated()

                        // STATIC FILES (frontend pages must be accessible)
                        .requestMatchers(
                                "/", "/index.html", "/login.html", "/signup.html",
                                "/orders.html", "/cart.html",
                                "/*.html",
                                "/css/**", "/js/**", "/images/**"
                        ).permitAll()

                        // PREFLIGHT
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        .anyRequest().authenticated()
                );

        return http.build();
    }
}