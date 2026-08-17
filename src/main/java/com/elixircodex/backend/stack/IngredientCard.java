package com.elixircodex.backend.stack;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class IngredientCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long ownerId;

    private String name;

    @Enumerated(EnumType.STRING)
    private Grade grade;

    private String sourceQuestTitle;

    @Builder.Default
    private int quantity = 1;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public void increaseQuantity() {
        this.quantity += 1;
    }
}
