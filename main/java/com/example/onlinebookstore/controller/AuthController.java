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

    /**
     * GET /api/auth/users/{id}
     * Admin: fetch a user's name and email by ID (password never returned).
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUserById(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Admin", defaultValue = "false") String isAdmin) {
        if (!"true".equals(isAdmin))
            return ResponseEntity.status(403).body(Map.of("message", "Admin access required"));
        try {
            User user = userService.getUserById(id);
            user.setPassword(null);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * POST /api/auth/admin/create-user
     * Admin: create a new user account with an explicit role selection.
     * Body: { name, email, password, admin: true/false }
     * Header: X-User-Admin: true
     */
    @PostMapping("/admin/create-user")
    public ResponseEntity<?> adminCreateUser(
            @RequestBody User user,
            @RequestHeader(value = "X-User-Admin", defaultValue = "false") String isAdmin) {

        if (!"true".equals(isAdmin))
            return ResponseEntity.status(403).body(Map.of("message", "Admin access required"));

        if (user.getName() == null || user.getName().isBlank())
            return ResponseEntity.badRequest().body(Map.of("message", "Name is required"));
        if (user.getEmail() == null || user.getEmail().isBlank())
            return ResponseEntity.badRequest().body(Map.of("message", "Email is required"));
        if (user.getPassword() == null || user.getPassword().length() < 6)
            return ResponseEntity.badRequest().body(Map.of("message", "Password must be at least 6 characters"));

        try {
            User saved = userService.register(user); // register handles BCrypt + duplicate check
            saved.setPassword(null);
            return ResponseEntity.ok(saved);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}