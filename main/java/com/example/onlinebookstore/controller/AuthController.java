package com.example.onlinebookstore.controller;

import com.example.onlinebookstore.model.User;
import com.example.onlinebookstore.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {

    @Autowired
    private UserService userService;

    /**
     * POST /api/auth/register
     * Body: { "name": "Juan", "email": "juan@email.com", "password": "pass123" }
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        try {
            if (user.getName() == null || user.getName().isBlank())
                return ResponseEntity.badRequest().body(Map.of("message", "Name is required"));
            if (user.getEmail() == null || user.getEmail().isBlank())
                return ResponseEntity.badRequest().body(Map.of("message", "Email is required"));
            if (user.getPassword() == null || user.getPassword().length() < 6)
                return ResponseEntity.badRequest().body(Map.of("message", "Password must be at least 6 characters"));

            User saved = userService.register(user);
            saved.setPassword(null);
            return ResponseEntity.ok(saved);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * POST /api/auth/login
     * Body: { "email": "juan@email.com", "password": "pass123" }
     * Returns the User object (password hidden) — client stores this in localStorage.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User user) {
        try {
            User loggedIn = userService.login(user.getEmail(), user.getPassword());
            loggedIn.setPassword(null);
            return ResponseEntity.ok(loggedIn);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(Map.of("message", e.getMessage()));
        }
    }
}
