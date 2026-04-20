package com.example.onlinebookstore.controller;

import com.example.onlinebookstore.model.Order;
import com.example.onlinebookstore.model.OrderItem;
import com.example.onlinebookstore.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin
public class OrderController {

    @Autowired
    private OrderRepository orderRepository;

    @PostMapping
    public String placeOrder(@RequestBody List<OrderItem> items) {

        Order order = new Order();
        order.setOrderDate(LocalDateTime.now());

        // link items to order
        for (OrderItem item : items) {
            item.setOrder(order);   // ✅ FIXED (NO orderId anymore)
        }

        order.setItems(items);

        orderRepository.save(order); // ✅ cascade saves items if configured

        return "Order placed!";
    }
}