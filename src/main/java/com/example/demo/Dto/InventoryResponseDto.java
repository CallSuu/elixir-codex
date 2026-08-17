package com.example.demo.Dto;

import lombok.Getter;

import java.util.List;

@Getter
public class InventoryResponseDto {

    private final List<IngredientCardResponseDto> ingredientCards;
    private final List<RecipeScrollResponseDto> recipeScrolls;

    public InventoryResponseDto(List<IngredientCardResponseDto> ingredientCards, List<RecipeScrollResponseDto> recipeScrolls) {
        this.ingredientCards = ingredientCards;
        this.recipeScrolls = recipeScrolls;
    }
}
