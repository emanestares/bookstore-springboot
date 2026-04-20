package com.example.onlinebookstore.service;

import com.example.onlinebookstore.model.Book;

import java.util.List;

public interface BookService {

    List<Book> getAllBooks();

    Book getBookById(Long id);

    Book addBook(Book book);

    List<Book> searchByTitle(String title);

    List<Book> filterByCategory(String category);

    List<Book> searchByAuthor(String author);

    Book updateBook(Long id, Book book);

    void deleteBook(Long id);
}