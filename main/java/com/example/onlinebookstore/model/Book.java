package com.example.onlinebookstore.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "books")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String title;

    @NotBlank
    @Column(nullable = false)
    private String author;

    @Min(0)
    private double price;

    @Column(length = 1000)
    private String description;

    private String category;

    @Min(0)
    @Column(nullable = false)
    private int stock = 0;
}
