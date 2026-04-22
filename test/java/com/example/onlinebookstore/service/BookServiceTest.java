package com.example.onlinebookstore.service;

import com.example.onlinebookstore.model.Book;
import com.example.onlinebookstore.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookService Tests")
class BookServiceTest {

    @Mock BookRepository bookRepository;
    @InjectMocks BookServiceImpl bookService;

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
    @DisplayName("getAllBooks returns all books from repository")
    void getAllBooks_returnsAllBooks() {
        when(bookRepository.findAll()).thenReturn(List.of(sampleBook));
        List<Book> result = bookService.getAllBooks();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Clean Code");
    }

    @Test
    @DisplayName("getAllBooks returns empty list when catalog is empty")
    void getAllBooks_emptyRepository_returnsEmptyList() {
        when(bookRepository.findAll()).thenReturn(List.of());
        assertThat(bookService.getAllBooks()).isEmpty();
    }

    @Test
    @DisplayName("getBookById returns book when found")
    void getBookById_found() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(sampleBook));
        Book result = bookService.getBookById(1L);
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getAuthor()).isEqualTo("Robert C. Martin");
    }

    @Test
    @DisplayName("getBookById throws RuntimeException when not found")
    void getBookById_notFound_throws() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> bookService.getBookById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Book not found");
    }

    @Test
    @DisplayName("addBook persists and returns the saved book")
    void addBook_savesAndReturns() {
        when(bookRepository.save(sampleBook)).thenReturn(sampleBook);
        Book result = bookService.addBook(sampleBook);
        assertThat(result.getTitle()).isEqualTo("Clean Code");
        verify(bookRepository).save(sampleBook);
    }

    @Test
    @DisplayName("addBook does not modify the book before saving")
    void addBook_noMutationBeforeSave() {
        when(bookRepository.save(sampleBook)).thenReturn(sampleBook);
        bookService.addBook(sampleBook);
        assertThat(sampleBook.getPrice()).isEqualTo(499.00);
        assertThat(sampleBook.getStock()).isEqualTo(10);
    }

    @Test
    @DisplayName("updateBook updates all fields and saves")
    void updateBook_updatesFields() {
        Book updated = new Book();
        updated.setTitle("Refactoring");
        updated.setAuthor("Martin Fowler");
        updated.setPrice(599.00);
        updated.setCategory("Programming");
        updated.setDescription("A guide to refactoring");
        updated.setStock(5);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(sampleBook));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        Book result = bookService.updateBook(1L, updated);
        assertThat(result.getTitle()).isEqualTo("Refactoring");
        assertThat(result.getAuthor()).isEqualTo("Martin Fowler");
        assertThat(result.getPrice()).isEqualTo(599.00);
        assertThat(result.getStock()).isEqualTo(5);
        assertThat(result.getDescription()).isEqualTo("A guide to refactoring");
    }

    @Test
    @DisplayName("updateBook throws when book not found")
    void updateBook_notFound_throws() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> bookService.updateBook(99L, sampleBook))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Book not found");
    }

    @Test
    @DisplayName("deleteBook calls deleteById when book exists")
    void deleteBook_exists_deletes() {
        when(bookRepository.existsById(1L)).thenReturn(true);
        bookService.deleteBook(1L);
        verify(bookRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteBook throws when book does not exist")
    void deleteBook_notFound_throws() {
        when(bookRepository.existsById(99L)).thenReturn(false);
        assertThatThrownBy(() -> bookService.deleteBook(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Book not found");
    }

    @Test
    @DisplayName("searchByTitle delegates to repository with title param")
    void searchByTitle_delegatesToRepository() {
        when(bookRepository.findByTitleContainingIgnoreCase("clean")).thenReturn(List.of(sampleBook));
        List<Book> result = bookService.searchByTitle("clean");
        assertThat(result).hasSize(1);
        verify(bookRepository).findByTitleContainingIgnoreCase("clean");
    }

    @Test
    @DisplayName("filterByCategory delegates to repository")
    void filterByCategory_delegatesToRepository() {
        when(bookRepository.findByCategoryIgnoreCase("Programming")).thenReturn(List.of(sampleBook));
        List<Book> result = bookService.filterByCategory("Programming");
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("searchByAuthor delegates to repository")
    void searchByAuthor_delegatesToRepository() {
        when(bookRepository.findByAuthorContainingIgnoreCase("martin")).thenReturn(List.of(sampleBook));
        List<Book> result = bookService.searchByAuthor("martin");
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("getAllCategories returns distinct sorted categories")
    void getAllCategories_distinctSorted() {
        Book b2 = new Book(); b2.setCategory("Fiction");
        Book b3 = new Book(); b3.setCategory("Programming"); // duplicate
        when(bookRepository.findAll()).thenReturn(List.of(sampleBook, b2, b3));
        List<String> cats = bookService.getAllCategories();
        assertThat(cats).containsExactly("Fiction", "Programming");
    }

    @Test
    @DisplayName("getAllCategories filters out null and blank categories")
    void getAllCategories_filtersNullAndBlank() {
        Book b2 = new Book(); b2.setCategory(null);
        Book b3 = new Book(); b3.setCategory("  ");
        when(bookRepository.findAll()).thenReturn(List.of(sampleBook, b2, b3));
        List<String> cats = bookService.getAllCategories();
        assertThat(cats).containsExactly("Programming");
    }
}
