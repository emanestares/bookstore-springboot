package com.example.onlinebookstore.service;

import com.example.onlinebookstore.model.User;
import com.example.onlinebookstore.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

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

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public User updateUser(Long id, User updated) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        // Update name
        if (updated.getName() != null && !updated.getName().isBlank())
            user.setName(updated.getName());

        // Update email — check for duplicates on other accounts
        if (updated.getEmail() != null && !updated.getEmail().isBlank()) {
            User existing = userRepository.findByEmail(updated.getEmail());
            if (existing != null && !existing.getId().equals(id))
                throw new RuntimeException("Email already in use by another account");
            user.setEmail(updated.getEmail());
        }

        // Update role
        user.setAdmin(updated.isAdmin());

        // Only update password if a new one was provided
        if (updated.getPassword() != null && !updated.getPassword().isBlank()) {
            if (updated.getPassword().length() < 6)
                throw new RuntimeException("Password must be at least 6 characters");
            user.setPassword(passwordEncoder.encode(updated.getPassword()));
        }

        return userRepository.save(user);
    }
}