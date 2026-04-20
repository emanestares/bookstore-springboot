package com.example.onlinebookstore.repository;

import com.example.onlinebookstore.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookRepository extends JpaRepository<Book, Long> {

    // Search by title (case-insensitive)
    List<Book> findByTitleContainingIgnoreCase(String title);

    // Filter by category
    List<Book> findByCategoryIgnoreCase(String category);

    // Search by author
    List<Book> findByAuthorContainingIgnoreCase(String author);
}