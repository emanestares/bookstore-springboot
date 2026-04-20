package com.example.onlinebookstore.service;

import com.example.onlinebookstore.model.OrderItem;

import java.util.List;

public interface OrderItemService {

    List<OrderItem> getItemsByOrderId(Long orderId);
}