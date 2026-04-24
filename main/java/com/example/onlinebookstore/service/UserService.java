package com.example.onlinebookstore.service;

import com.example.onlinebookstore.model.User;
import java.util.List;

public interface UserService {

    User register(User user);

    User login(String email, String password);

    User getUserById(Long id);

    List<User> getAllUsers();

    User updateUser(Long id, User updated);
}