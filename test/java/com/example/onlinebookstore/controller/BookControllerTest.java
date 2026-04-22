package com.example.onlinebookstore.controller;

import com.example.onlinebookstore.model.Book;
import com.example.onlinebookstore.service.BookService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("BookController Tests")
class BookControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean BookService bookService;

    private Book sampleBook;

    @BeforeEach
    void setUp() {
        sampleBook = new Book();
        sampleBook.setId(1L);
        sampleBook.setTitle("Clean Code");
        sampleBook.setAuthor("Robert C. Martin");
        sampleBook.setPrice(499.00);
        sampleBook.setCategory("Programming");
        sampleBook.setStock(10);
    }

    @Test
    @DisplayName("GET /api/books returns all books")
    void getAllBooks_returns200WithList() throws Exception {
        when(bookService.getAllBooks()).thenReturn(List.of(sampleBook));
        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Clean Code"))
                .andExpect(jsonPath("$[0].author").value("Robert C. Martin"))
                .andExpect(jsonPath("$[0].price").value(499.00));
    }

    @Test
    @DisplayName("GET /api/books returns empty array when no books")
    void getAllBooks_empty_returns200EmptyList() throws Exception {
        when(bookService.getAllBooks()).thenReturn(List.of());
        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("GET /api/books/{id} returns book when found")
    void getBookById_found_returns200() throws Exception {
        when(bookService.getBookById(1L)).thenReturn(sampleBook);
        mockMvc.perform(get("/api/books/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Clean Code"));
    }

    @Test
    @DisplayName("GET /api/books/{id} returns 404 when not found")
    void getBookById_notFound_returns404() throws Exception {
        when(bookService.getBookById(99L)).thenThrow(new RuntimeException("Book not found"));
        mockMvc.perform(get("/api/books/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/books/search returns matching books")
    void searchBooks_returnsMatches() throws Exception {
        when(bookService.searchByTitle("clean")).thenReturn(List.of(sampleBook));
        mockMvc.perform(get("/api/books/search").param("title", "clean"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Clean Code"));
    }

    @Test
    @DisplayName("GET /api/books/category filters by category")
    void filterByCategory_returnsFiltered() throws Exception {
        when(bookService.filterByCategory("Programming")).thenReturn(List.of(sampleBook));
        mockMvc.perform(get("/api/books/category").param("category", "Programming"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("Programming"));
    }

    @Test
    @DisplayName("POST /api/books with admin header creates book")
    void addBook_adminHeader_returns200() throws Exception {
        when(bookService.addBook(any(Book.class))).thenReturn(sampleBook);
        mockMvc.perform(post("/api/books")
                        .header("X-User-Admin", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleBook)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Clean Code"));
    }

    @Test
    @DisplayName("POST /api/books without admin header returns 403")
    void addBook_noAdminHeader_returns403() throws Exception {
        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleBook)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Admin access required"));
    }

    @Test
    @DisplayName("POST /api/books returns 400 when title is missing")
    void addBook_missingTitle_returns400() throws Exception {
        sampleBook.setTitle("");
        mockMvc.perform(post("/api/books")
                        .header("X-User-Admin", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleBook)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Title is required"));
    }

    @Test
    @DisplayName("POST /api/books returns 400 when author is missing")
    void addBook_missingAuthor_returns400() throws Exception {
        sampleBook.setAuthor("");
        mockMvc.perform(post("/api/books")
                        .header("X-User-Admin", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleBook)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Author is required"));
    }

    @Test
    @DisplayName("POST /api/books returns 400 when price is negative")
    void addBook_negativePrice_returns400() throws Exception {
        sampleBook.setPrice(-10.0);
        mockMvc.perform(post("/api/books")
                        .header("X-User-Admin", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleBook)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Price cannot be negative"));
    }

    @Test
    @DisplayName("PUT /api/books/{id} with admin header updates book")
    void updateBook_adminHeader_returns200() throws Exception {
        when(bookService.updateBook(eq(1L), any(Book.class))).thenReturn(sampleBook);
        mockMvc.perform(put("/api/books/1")
                        .header("X-User-Admin", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleBook)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Clean Code"));
    }

    @Test
    @DisplayName("PUT /api/books/{id} without admin header returns 403")
    void updateBook_noAdminHeader_returns403() throws Exception {
        mockMvc.perform(put("/api/books/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleBook)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/books/{id} returns 404 when book not found")
    void updateBook_notFound_returns404() throws Exception {
        when(bookService.updateBook(eq(99L), any())).thenThrow(new RuntimeException("not found"));
        mockMvc.perform(put("/api/books/99")
                        .header("X-User-Admin", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleBook)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/books/{id} with admin header deletes book")
    void deleteBook_adminHeader_returns200() throws Exception {
        doNothing().when(bookService).deleteBook(1L);
        mockMvc.perform(delete("/api/books/1").header("X-User-Admin", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Book deleted"));
    }

    @Test
    @DisplayName("DELETE /api/books/{id} without admin header returns 403")
    void deleteBook_noAdminHeader_returns403() throws Exception {
        mockMvc.perform(delete("/api/books/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/books/{id} returns 404 when book not found")
    void deleteBook_notFound_returns404() throws Exception {
        doThrow(new RuntimeException("not found")).when(bookService).deleteBook(99L);
        mockMvc.perform(delete("/api/books/99").header("X-User-Admin", "true"))
                .andExpect(status().isNotFound());
    }
}