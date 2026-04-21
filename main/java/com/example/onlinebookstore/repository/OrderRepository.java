package com.example.onlinebookstore.repository;

import com.example.onlinebookstore.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(Long userId);
    // Returns orders newest first
    List<Order> findByUserIdOrderByOrderDateDesc(Long userId);
}
