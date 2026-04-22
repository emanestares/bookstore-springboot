package com.example.onlinebookstore.controller;

import com.example.onlinebookstore.model.Order;
import com.example.onlinebookstore.model.OrderItem;
import com.example.onlinebookstore.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@DisplayName("OrderController Tests")
class OrderControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean  OrderService orderService;

    private Order sampleOrder() {
        OrderItem item = new OrderItem();
        item.setId(1L); item.setBookId(10L);
        item.setBookTitle("Clean Code");
        item.setPriceAtPurchase(499.00);
        item.setQuantity(2);

        Order o = new Order();
        o.setId(1L); o.setUserId(1L);
        o.setOrderDate(LocalDateTime.now());
        o.setStatus(Order.OrderStatus.PREPARING);
        o.setTotalAmount(998.00);
        o.setItems(List.of(item));
        return o;
    }

    // ── POST /api/orders ─────────────────────────────────

    @Test
    @DisplayName("POST /api/orders places order successfully")
    void placeOrder_success_returns200() throws Exception {
        when(orderService.placeOrder(anyLong(), anyList())).thenReturn(sampleOrder());

        var body = Map.of(
            "userId", 1,
            "items", List.of(Map.of("bookId", 10, "quantity", 2))
        );

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(998.00))
                .andExpect(jsonPath("$.status").value("PREPARING"))
                .andExpect(jsonPath("$.userId").value(1));
    }

    @Test
    @DisplayName("POST /api/orders returns 400 when userId is missing")
    void placeOrder_missingUserId_returns400() throws Exception {
        var body = Map.of("items", List.of(Map.of("bookId", 10, "quantity", 1)));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("userId is required"));
    }

    @Test
    @DisplayName("POST /api/orders returns 400 when items list is empty")
    void placeOrder_emptyItems_returns400() throws Exception {
        var body = Map.of("userId", 1, "items", List.of());

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Order must have at least one item"));
    }

    @Test
    @DisplayName("POST /api/orders returns 400 when service throws (e.g. insufficient stock)")
    void placeOrder_serviceThrows_returns400() throws Exception {
        when(orderService.placeOrder(anyLong(), anyList()))
                .thenThrow(new RuntimeException("Not enough stock for \"Clean Code\""));

        var body = Map.of(
            "userId", 1,
            "items", List.of(Map.of("bookId", 10, "quantity", 999))
        );

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Not enough stock")));
    }

    // ── GET /api/orders?userId ───────────────────────────

    @Test
    @DisplayName("GET /api/orders?userId=1 returns orders for user")
    void getOrdersByUser_returns200() throws Exception {
        when(orderService.getOrdersByUser(1L)).thenReturn(List.of(sampleOrder()));

        mockMvc.perform(get("/api/orders").param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(1))
                .andExpect(jsonPath("$[0].status").value("PREPARING"))
                .andExpect(jsonPath("$[0].items[0].bookTitle").value("Clean Code"));
    }

    @Test
    @DisplayName("GET /api/orders?userId returns 400 when user not found")
    void getOrdersByUser_userNotFound_returns400() throws Exception {
        when(orderService.getOrdersByUser(99L)).thenThrow(new RuntimeException("User not found"));

        mockMvc.perform(get("/api/orders").param("userId", "99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    // ── GET /api/orders/all ──────────────────────────────

    @Test
    @DisplayName("GET /api/orders/all with admin header returns all orders")
    void getAllOrders_adminHeader_returns200() throws Exception {
        when(orderService.getAllOrders()).thenReturn(List.of(sampleOrder()));

        mockMvc.perform(get("/api/orders/all").header("X-User-Admin", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].totalAmount").value(998.00));
    }

    @Test
    @DisplayName("GET /api/orders/all without admin header returns 403")
    void getAllOrders_noAdminHeader_returns403() throws Exception {
        mockMvc.perform(get("/api/orders/all"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Admin access required"));
    }

    @Test
    @DisplayName("GET /api/orders/all with admin=false returns 403")
    void getAllOrders_adminFalse_returns403() throws Exception {
        mockMvc.perform(get("/api/orders/all").header("X-User-Admin", "false"))
                .andExpect(status().isForbidden());
    }

    // ── PATCH /api/orders/{id}/status ────────────────────

    @Test
    @DisplayName("PATCH /api/orders/{id}/status sets TO_DELIVER with admin header")
    void updateStatus_toDeliver_returns200() throws Exception {
        Order updated = sampleOrder();
        updated.setStatus(Order.OrderStatus.TO_DELIVER);
        when(orderService.updateOrderStatus(1L, Order.OrderStatus.TO_DELIVER)).thenReturn(updated);

        mockMvc.perform(patch("/api/orders/1/status")
                        .header("X-User-Admin", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"TO_DELIVER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TO_DELIVER"));
    }

    @Test
    @DisplayName("PATCH /api/orders/{id}/status sets DECLINED with admin header")
    void updateStatus_toDeclined_returns200() throws Exception {
        Order updated = sampleOrder();
        updated.setStatus(Order.OrderStatus.DECLINED);
        when(orderService.updateOrderStatus(1L, Order.OrderStatus.DECLINED)).thenReturn(updated);

        mockMvc.perform(patch("/api/orders/1/status")
                        .header("X-User-Admin", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DECLINED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DECLINED"));
    }

    @Test
    @DisplayName("PATCH /api/orders/{id}/status returns 400 for invalid status value")
    void updateStatus_invalidStatus_returns400() throws Exception {
        mockMvc.perform(patch("/api/orders/1/status")
                        .header("X-User-Admin", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INVALID_STATUS\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid status value"));
    }

    @Test
    @DisplayName("PATCH /api/orders/{id}/status without admin header returns 403")
    void updateStatus_noAdminHeader_returns403() throws Exception {
        mockMvc.perform(patch("/api/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"TO_DELIVER\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH /api/orders/{id}/status returns 400 when order not found")
    void updateStatus_orderNotFound_returns400() throws Exception {
        when(orderService.updateOrderStatus(eq(99L), any()))
                .thenThrow(new RuntimeException("Order not found: 99"));

        mockMvc.perform(patch("/api/orders/99/status")
                        .header("X-User-Admin", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DECLINED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Order not found")));
    }
}
