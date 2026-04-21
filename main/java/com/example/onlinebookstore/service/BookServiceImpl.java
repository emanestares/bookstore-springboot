package com.example.onlinebookstore.service;

import com.example.onlinebookstore.model.Book;
import com.example.onlinebookstore.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookServiceImpl implements BookService {

    @Autowired
    private BookRepository bookRepository;

    @Override
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    @Override
    public Book getBookById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Book not found with id: " + id));
    }

    @Override
    public Book addBook(Book book) {
        return bookRepository.save(book);
    }

    @Override
    public List<Book> searchByTitle(String title) {
        return bookRepository.findByTitleContainingIgnoreCase(title);
    }

    @Override
    public List<Book> filterByCategory(String category) {
        return bookRepository.findByCategoryIgnoreCase(category);
    }

    @Override
    public List<Book> searchByAuthor(String author) {
        return bookRepository.findByAuthorContainingIgnoreCase(author);
    }

    @Override
    public Book updateBook(Long id, Book updated) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Book not found with id: " + id));
        book.setTitle(updated.getTitle());
        book.setAuthor(updated.getAuthor());
        book.setPrice(updated.getPrice());
        book.setCategory(updated.getCategory());
        book.setDescription(updated.getDescription());
        book.setStock(updated.getStock());
        book.setCoverImage(updated.getCoverImage());
        return bookRepository.save(book);
    }

    @Override
    public void deleteBook(Long id) {
        if (!bookRepository.existsById(id))
            throw new RuntimeException("Book not found with id: " + id);
        bookRepository.deleteById(id);
    }

    @Override
    public List<String> getAllCategories() {
        return bookRepository.findAll().stream()
                .map(Book::getCategory)
                .filter(c -> c != null && !c.isBlank())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }
}
