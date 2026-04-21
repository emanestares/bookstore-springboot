package com.example.onlinebookstore.controller;

import com.example.onlinebookstore.model.Order;
import com.example.onlinebookstore.model.OrderItem;
import com.example.onlinebookstore.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * POST /api/orders
     * Body: { "userId": 1, "items": [{"bookId":1,"bookTitle":"...","priceAtPurchase":29.99,"quantity":2}] }
     */
    @PostMapping
    public ResponseEntity<?> placeOrder(@RequestBody OrderRequest request) {
        try {
            Order order = orderService.placeOrder(request.getUserId(), request.getItems());
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/orders?userId=1
     * Returns all orders for a specific user.
     */
    @GetMapping
    public List<Order> getOrdersByUser(@RequestParam Long userId) {
        return orderService.getOrdersByUser(userId);
    }

    // ─── Inner DTO ────────────────────────────────────────
    @lombok.Data
    public static class OrderRequest {
        private Long userId;
        private List<OrderItem> items;
    }
}
