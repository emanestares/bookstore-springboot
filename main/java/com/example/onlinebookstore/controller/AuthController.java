package com.example.onlinebookstore.controller;

import com.example.onlinebookstore.model.User;
import com.example.onlinebookstore.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {

    @Autowired
    private UserService userService;

    // REGISTER
    @PostMapping("/register")
    public User register(@RequestBody User user) {
        User savedUser = userService.register(user);

        // hide password before returning
        savedUser.setPassword(null);

        return savedUser;
    }

    // LOGIN
    @PostMapping("/login")
    public User login(@RequestBody User user) {
        User loggedInUser = userService.login(user.getEmail(), user.getPassword());

        // hide password before returning
        loggedInUser.setPassword(null);

        return loggedInUser;
    }
}