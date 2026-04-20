package com.example.onlinebookstore.service;

import com.example.onlinebookstore.model.User;

public interface UserService {

    User register(User user);

    User login(String email, String password);

    User getUserById(Long id);
}