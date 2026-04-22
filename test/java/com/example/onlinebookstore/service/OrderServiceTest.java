package com.example.onlinebookstore.service;

import com.example.onlinebookstore.model.Book;
import com.example.onlinebookstore.model.Order;
import com.example.onlinebookstore.model.OrderItem;
import com.example.onlinebookstore.model.User;
import com.example.onlinebookstore.repository.BookRepository;
import com.example.onlinebookstore.repository.OrderRepository;
import com.example.onlinebookstore.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService Tests")
class OrderServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock BookRepository  bookRepository;
    @Mock UserRepository  userRepository;
    @InjectMocks OrderServiceImpl orderService;

    private User   sampleUser;
    private Book   sampleBook;
    private Order  sampleOrder;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setName("Juan dela Cruz");
        sampleUser.setEmail("juan@example.com");

        sampleBook = new Book();
        sampleBook.setId(10L);
        sampleBook.setTitle("Clean Code");
        sampleBook.setAuthor("Robert C. Martin");
        sampleBook.setPrice(499.00);
        sampleBook.setStock(10);
        sampleBook.setCategory("Programming");

        // Always initialize items to avoid NullPointerException in updateOrderStatus
        OrderItem item = new OrderItem();
        item.setBookId(10L);
        item.setBookTitle("Clean Code");
        item.setPriceAtPurchase(499.00);
        item.setQuantity(2);

        sampleOrder = new Order();
        sampleOrder.setId(1L);
        sampleOrder.setUserId(1L);
        sampleOrder.setOrderDate(LocalDateTime.now());
        sampleOrder.setStatus(Order.OrderStatus.PREPARING);
        sampleOrder.setTotalAmount(998.00);
        sampleOrder.setItems(new ArrayList<>(List.of(item)));
    }

    // ── placeOrder ───────────────────────────────────────

    @Test
    @DisplayName("placeOrder succeeds and saves order with correct total")
    void placeOrder_success_savesOrder() {
        OrderItem item = new OrderItem();
        item.setBookId(10L);
        item.setQuantity(2);

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(bookRepository.findById(10L)).thenReturn(Optional.of(sampleBook));
        when(bookRepository.save(any(Book.class))).thenReturn(sampleBook);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(1L);
            return o;
        });

        Order result = orderService.placeOrder(1L, List.of(item));

        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo(Order.OrderStatus.PREPARING);
        assertThat(result.getTotalAmount()).isEqualTo(998.00);
        verify(bookRepository).save(any(Book.class));
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("placeOrder deducts stock from book")
    void placeOrder_deductsStock() {
        OrderItem item = new OrderItem();
        item.setBookId(10L);
        item.setQuantity(3);

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(bookRepository.findById(10L)).thenReturn(Optional.of(sampleBook));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        orderService.placeOrder(1L, List.of(item));

        assertThat(sampleBook.getStock()).isEqualTo(7); // 10 - 3
    }

    @Test
    @DisplayName("placeOrder snaps book title and price onto each item")
    void placeOrder_snapshotsTitleAndPrice() {
        OrderItem item = new OrderItem();
        item.setBookId(10L);
        item.setQuantity(1);

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(bookRepository.findById(10L)).thenReturn(Optional.of(sampleBook));
        when(bookRepository.save(any())).thenReturn(sampleBook);
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        orderService.placeOrder(1L, List.of(item));

        assertThat(item.getBookTitle()).isEqualTo("Clean Code");
        assertThat(item.getPriceAtPurchase()).isEqualTo(499.00);
    }

    @Test
    @DisplayName("placeOrder throws when user not found")
    void placeOrder_userNotFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.placeOrder(99L, List.of(new OrderItem())))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("placeOrder throws when book not found")
    void placeOrder_bookNotFound_throws() {
        OrderItem item = new OrderItem();
        item.setBookId(999L);
        item.setQuantity(1);

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(bookRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.placeOrder(1L, List.of(item)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Book not found");
    }

    @Test
    @DisplayName("placeOrder throws when stock is insufficient")
    void placeOrder_insufficientStock_throws() {
        OrderItem item = new OrderItem();
        item.setBookId(10L);
        item.setQuantity(50);

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(bookRepository.findById(10L)).thenReturn(Optional.of(sampleBook));

        assertThatThrownBy(() -> orderService.placeOrder(1L, List.of(item)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Not enough stock");
    }

    @Test
    @DisplayName("placeOrder throws when quantity is zero or negative")
    void placeOrder_zeroQuantity_throws() {
        OrderItem item = new OrderItem();
        item.setBookId(10L);
        item.setQuantity(0);

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        assertThatThrownBy(() -> orderService.placeOrder(1L, List.of(item)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Quantity must be at least 1");
    }

    // ── getOrdersByUser ──────────────────────────────────

    @Test
    @DisplayName("getOrdersByUser returns orders for valid user")
    void getOrdersByUser_returnsOrders() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(orderRepository.findByUserIdOrderByOrderDateDesc(1L)).thenReturn(List.of(sampleOrder));

        List<Order> result = orderService.getOrdersByUser(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getOrdersByUser throws when user not found")
    void getOrdersByUser_userNotFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrdersByUser(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    // ── getAllOrders ─────────────────────────────────────

    @Test
    @DisplayName("getAllOrders returns all orders sorted by date")
    void getAllOrders_returnsAllOrders() {
        when(orderRepository.findAllByOrderByOrderDateDesc()).thenReturn(List.of(sampleOrder));

        List<Order> result = orderService.getAllOrders();

        assertThat(result).hasSize(1);
        verify(orderRepository).findAllByOrderByOrderDateDesc();
    }

    // ── updateOrderStatus ────────────────────────────────

    @Test
    @DisplayName("updateOrderStatus saves new status correctly")
    void updateOrderStatus_savesStatus() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = orderService.updateOrderStatus(1L, Order.OrderStatus.TO_DELIVER);

        assertThat(result.getStatus()).isEqualTo(Order.OrderStatus.TO_DELIVER);
        verify(orderRepository).save(sampleOrder);
    }

    @Test
    @DisplayName("updateOrderStatus can set DECLINED status and restores stock")
    void updateOrderStatus_toDeclined() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(bookRepository.findById(10L)).thenReturn(Optional.of(sampleBook));

        Order result = orderService.updateOrderStatus(1L, Order.OrderStatus.DECLINED);

        assertThat(result.getStatus()).isEqualTo(Order.OrderStatus.DECLINED);
        // Stock should be restored (10 + 2 = 12)
        assertThat(sampleBook.getStock()).isEqualTo(12);
    }

    @Test
    @DisplayName("updateOrderStatus throws when order not found")
    void updateOrderStatus_orderNotFound_throws() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.updateOrderStatus(99L, Order.OrderStatus.TO_DELIVER))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Order not found");
    }
}