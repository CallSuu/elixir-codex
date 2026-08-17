package com.example.demo.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecipeScroll {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String name;

    @Builder.Default
    private int quantity = 1;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public void increaseQuantity() {
        this.quantity += 1;
    }
}
