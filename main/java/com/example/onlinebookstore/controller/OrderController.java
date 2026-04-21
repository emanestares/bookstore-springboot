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
     * Places an order.
     */
    @PostMapping
    public ResponseEntity<?> placeOrder(@RequestBody OrderRequest request) {
        try {
            if (request.getUserId() == null)
                return ResponseEntity.badRequest().body(Map.of("message", "userId is required"));
            if (request.getItems() == null || request.getItems().isEmpty())
                return ResponseEntity.badRequest().body(Map.of("message", "Order must have at least one item"));

            Order order = orderService.placeOrder(request.getUserId(), request.getItems());
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * GET /api/orders?userId=1
     * Returns all orders for a user, newest first.
     */
    @GetMapping
    public ResponseEntity<?> getOrdersByUser(@RequestParam Long userId) {
        try {
            return ResponseEntity.ok(orderService.getOrdersByUser(userId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * GET /api/orders/all
     * Admin: returns all orders across all users.
     */
    @GetMapping("/all")
    public ResponseEntity<?> getAllOrders(
            @RequestHeader(value = "X-User-Admin", defaultValue = "false") String isAdmin) {
        if (!"true".equals(isAdmin))
            return ResponseEntity.status(403).body(Map.of("message", "Admin access required"));
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    /**
     * GET /api/orders/{id}
     * Returns a specific order by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(orderService.getOrderById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * PATCH /api/orders/{id}/status
     * Admin: update order status.
     * Body: { "status": "TO_DELIVER" }
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "X-User-Admin", defaultValue = "false") String isAdmin) {
        if (!"true".equals(isAdmin))
            return ResponseEntity.status(403).body(Map.of("message", "Admin access required"));
        try {
            Order.OrderStatus status = Order.OrderStatus.valueOf(body.get("status"));
            Order updated = orderService.updateOrderStatus(id, status);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid status value"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ── Inner DTO ─────────────────────────────────────────
    @lombok.Data
    public static class OrderRequest {
        private Long userId;
        private List<OrderItem> items;
    }
}
