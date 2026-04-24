package com.example.onlinebookstore.controller;

import com.example.onlinebookstore.model.User;
import com.example.onlinebookstore.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
     * Admin: fetch a single user by ID.
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
     * GET /api/auth/users
     * Admin: fetch all users (passwords never returned).
     */
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(
            @RequestHeader(value = "X-User-Admin", defaultValue = "false") String isAdmin) {
        if (!"true".equals(isAdmin))
            return ResponseEntity.status(403).body(Map.of("message", "Admin access required"));

        List<User> users = userService.getAllUsers();
        users.forEach(u -> u.setPassword(null));
        return ResponseEntity.ok(users);
    }

    /**
     * PUT /api/auth/users/{id}
     * Admin: update a user's name, email, role, and optionally password.
     */
    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(
            @PathVariable Long id,
            @RequestBody User updated,
            @RequestHeader(value = "X-User-Admin", defaultValue = "false") String isAdmin) {
        if (!"true".equals(isAdmin))
            return ResponseEntity.status(403).body(Map.of("message", "Admin access required"));
        try {
            User saved = userService.updateUser(id, updated);
            saved.setPassword(null);
            return ResponseEntity.ok(saved);
        } catch (RuntimeException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("not found"))
                return ResponseEntity.status(404).body(Map.of("message", msg));
            return ResponseEntity.badRequest().body(Map.of("message", msg));
        }
    }

    /**
     * POST /api/auth/admin/create-user
     * Admin: create a new user account with explicit role selection.
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
            User saved = userService.register(user);
            saved.setPassword(null);
            return ResponseEntity.ok(saved);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}