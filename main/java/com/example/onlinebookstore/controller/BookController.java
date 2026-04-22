package com.example.onlinebookstore.controller;

import com.example.onlinebookstore.model.Book;
import com.example.onlinebookstore.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/books")
@CrossOrigin
public class BookController {

    @Autowired
    private BookService bookService;

    // ── Public Endpoints ──────────────────────────────────

    @GetMapping
    public List<Book> getAllBooks() {
        return bookService.getAllBooks();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getBookById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(bookService.getBookById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/search")
    public List<Book> searchBooks(@RequestParam String title) {
        return bookService.searchByTitle(title);
    }

    @GetMapping("/category")
    public List<Book> filterByCategory(@RequestParam String category) {
        return bookService.filterByCategory(category);
    }

    @GetMapping("/author")
    public List<Book> searchByAuthor(@RequestParam String author) {
        return bookService.searchByAuthor(author);
    }

    @GetMapping("/categories")
    public List<String> getCategories() {
        return bookService.getAllCategories();
    }

    // ── Admin Endpoints ──────────────────────────────────

    @PostMapping
    public ResponseEntity<?> addBook(@RequestBody Book book,
                                     @RequestHeader(value = "X-User-Admin", defaultValue = "false") String isAdmin) {
        if (!"true".equals(isAdmin))
            return ResponseEntity.status(403).body(Map.of("message", "Admin access required"));
        if (book.getTitle() == null || book.getTitle().isBlank())
            return ResponseEntity.badRequest().body(Map.of("message", "Title is required"));
        if (book.getAuthor() == null || book.getAuthor().isBlank())
            return ResponseEntity.badRequest().body(Map.of("message", "Author is required"));
        if (book.getPrice() < 0)
            return ResponseEntity.badRequest().body(Map.of("message", "Price cannot be negative"));

        return ResponseEntity.ok(bookService.addBook(book));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateBook(@PathVariable Long id,
                                        @RequestBody Book book,
                                        @RequestHeader(value = "X-User-Admin", defaultValue = "false") String isAdmin) {
        if (!"true".equals(isAdmin))
            return ResponseEntity.status(403).body(Map.of("message", "Admin access required"));
        try {
            return ResponseEntity.ok(bookService.updateBook(id, book));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBook(@PathVariable Long id,
                                        @RequestHeader(value = "X-User-Admin", defaultValue = "false") String isAdmin) {
        if (!"true".equals(isAdmin))
            return ResponseEntity.status(403).body(Map.of("message", "Admin access required"));
        try {
            bookService.deleteBook(id);
            return ResponseEntity.ok(Map.of("message", "Book deleted"));
        } catch (RuntimeException e) {
            // Only return 404 for genuine "not found" — other errors return 500
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("not found")) {
                return ResponseEntity.status(404).body(Map.of("message", msg));
            }
            return ResponseEntity.status(500).body(Map.of("message", "Could not delete book: " + msg));
        }
    }
}