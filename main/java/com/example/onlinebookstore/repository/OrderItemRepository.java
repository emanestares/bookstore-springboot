package com.example.onlinebookstore.repository;

import com.example.onlinebookstore.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // Get all items for a specific order
    List<OrderItem> findByOrder_Id(Long orderId);

    // Null out book_id for all order items referencing a deleted book
    @Modifying
    @Query("UPDATE OrderItem oi SET oi.bookId = null WHERE oi.bookId = :bookId")
    void clearBookIdByBookId(@Param("bookId") Long bookId);
}