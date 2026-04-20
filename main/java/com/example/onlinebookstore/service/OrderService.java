package com.example.onlinebookstore.service;

import com.example.onlinebookstore.model.Order;
import com.example.onlinebookstore.model.OrderItem;

import java.util.List;

public interface OrderService {

    Order placeOrder(Long userId, List<OrderItem> items);

    List<Order> getOrdersByUser(Long userId);

    Order getOrderById(Long id);
}