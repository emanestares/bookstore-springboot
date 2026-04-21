package com.example.onlinebookstore.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // userId kept simple as per your original design
    private Long userId;

    private LocalDateTime orderDate;

    // Added: order status for order history display
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.CONFIRMED;

    // Added: total amount snapshot at time of order
    private double totalAmount;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<OrderItem> items;

    public enum OrderStatus {
        CONFIRMED, SHIPPED, DELIVERED, CANCELLED
    }
}
