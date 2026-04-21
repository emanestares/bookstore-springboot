package com.example.onlinebookstore.service;

import com.example.onlinebookstore.model.User;
import com.example.onlinebookstore.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    // Injected as @Bean from SecurityConfig — NOT manually created with "new"
    // This is the correct Spring way and avoids BCrypt version mismatches
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public User register(User user) {
        if (userRepository.findByEmail(user.getEmail()) != null)
            throw new RuntimeException("Email already in use");

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    @Override
    public User login(String email, String password) {
        User user = userRepository.findByEmail(email);
        if (user == null)
            throw new RuntimeException("No account found with that email");
        if (!passwordEncoder.matches(password, user.getPassword()))
            throw new RuntimeException("Incorrect password");
        return user;
    }

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
