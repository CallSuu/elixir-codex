package com.example.demo.Dto;

import com.example.demo.Entity.RecipeScroll;
import lombok.Getter;

@Getter
public class RecipeScrollResponseDto {

    private final String name;
    private final int quantity;

    public RecipeScrollResponseDto(RecipeScroll recipeScroll) {
        this.name = recipeScroll.getName();
        this.quantity = recipeScroll.getQuantity();
    }
}
