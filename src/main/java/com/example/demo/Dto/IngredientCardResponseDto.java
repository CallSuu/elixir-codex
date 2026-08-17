package com.example.demo.Dto;

import com.elixircodex.backend.stack.Grade;
import com.elixircodex.backend.stack.IngredientCard;
import lombok.Getter;

@Getter
public class IngredientCardResponseDto {

    private final String name;
    private final Grade grade;
    private final int quantity;
    private final String sourceQuestTitle;

    public IngredientCardResponseDto(IngredientCard ingredientCard) {
        this.name = ingredientCard.getName();
        this.grade = ingredientCard.getGrade();
        this.quantity = ingredientCard.getQuantity();
        this.sourceQuestTitle = ingredientCard.getSourceQuestTitle();
    }
}
