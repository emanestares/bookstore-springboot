package com.example.onlinebookstore.service;

import com.example.onlinebookstore.model.Book;
import com.example.onlinebookstore.model.Order;
import com.example.onlinebookstore.model.OrderItem;
import com.example.onlinebookstore.repository.BookRepository;
import com.example.onlinebookstore.repository.OrderRepository;
import com.example.onlinebookstore.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Places an order with full business logic:
     * 1. Validate user exists
     * 2. Validate each book exists and has enough stock
     * 3. Deduct stock from each book
     * 4. Snapshot price and title at time of purchase
     * 5. Calculate total and save order
     *
     * @Transactional ensures all steps succeed or all roll back together
     */
    @Override
    @Transactional
    public Order placeOrder(Long userId, List<OrderItem> items) {

        // 1. Validate user exists
        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // 2. Validate stock and enrich items with book data
        for (OrderItem item : items) {
            if (item.getQuantity() <= 0)
                throw new RuntimeException("Quantity must be at least 1 for each item");

            Book book = bookRepository.findById(item.getBookId())
                    .orElseThrow(() -> new RuntimeException("Book not found: " + item.getBookId()));

            if (book.getStock() < item.getQuantity())
                throw new RuntimeException("Not enough stock for \"" + book.getTitle() +
                        "\" — only " + book.getStock() + " left");

            // 3. Deduct stock
            book.setStock(book.getStock() - item.getQuantity());
            bookRepository.save(book);

            // 4. Snapshot price and title at time of purchase
            //    (so order history is preserved even if book is edited/deleted)
            item.setPriceAtPurchase(book.getPrice());
            item.setBookTitle(book.getTitle());
        }

        // 5. Build and save order
        Order order = new Order();
        order.setUserId(userId);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(Order.OrderStatus.CONFIRMED);

        double total = items.stream()
                .mapToDouble(i -> i.getPriceAtPurchase() * i.getQuantity())
                .sum();
        order.setTotalAmount(total);

        for (OrderItem item : items) {
            item.setOrder(order);
        }
        order.setItems(items);

        return orderRepository.save(order);
    }

    @Override
    public List<Order> getOrdersByUser(Long userId) {
        // Validate user exists first
        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return orderRepository.findByUserIdOrderByOrderDateDesc(userId);
    }

    @Override
    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }
}
