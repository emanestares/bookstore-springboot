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

    @Override
    @Transactional
    public Order placeOrder(Long userId, List<OrderItem> items) {

        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        for (OrderItem item : items) {
            if (item.getQuantity() <= 0)
                throw new RuntimeException("Quantity must be at least 1 for each item");

            Book book = bookRepository.findById(item.getBookId())
                    .orElseThrow(() -> new RuntimeException("Book not found: " + item.getBookId()));

            if (book.getStock() < item.getQuantity())
                throw new RuntimeException("Not enough stock for \"" + book.getTitle() +
                        "\" — only " + book.getStock() + " left");

            book.setStock(book.getStock() - item.getQuantity());
            bookRepository.save(book);

            item.setPriceAtPurchase(book.getPrice());
            item.setBookTitle(book.getTitle());
        }

        Order order = new Order();
        order.setUserId(userId);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(Order.OrderStatus.PREPARING);

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
        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return orderRepository.findByUserIdOrderByOrderDateDesc(userId);
    }

    @Override
    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }

    @Override
    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByOrderDateDesc();
    }

    @Override
    @Transactional
    public Order updateOrderStatus(Long orderId, Order.OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        Order.OrderStatus previousStatus = order.getStatus();

        // Restore stock when transitioning TO declined from a non-declined state
        if (status == Order.OrderStatus.DECLINED && previousStatus != Order.OrderStatus.DECLINED) {
            for (OrderItem item : order.getItems()) {
                bookRepository.findById(item.getBookId()).ifPresent(book -> {
                    book.setStock(book.getStock() + item.getQuantity());
                    bookRepository.save(book);
                });
            }
        }

        // Re-deduct stock when transitioning FROM declined back to an active state
        if (previousStatus == Order.OrderStatus.DECLINED && status != Order.OrderStatus.DECLINED) {
            for (OrderItem item : order.getItems()) {
                Book book = bookRepository.findById(item.getBookId())
                        .orElseThrow(() -> new RuntimeException("Book not found: " + item.getBookId()));
                if (book.getStock() < item.getQuantity()) {
                    throw new RuntimeException("Not enough stock to reactivate order for \"" + book.getTitle() +
                            "\" — only " + book.getStock() + " left");
                }
                book.setStock(book.getStock() - item.getQuantity());
                bookRepository.save(book);
            }
        }

        order.setStatus(status);
        return orderRepository.save(order);
    }
}
