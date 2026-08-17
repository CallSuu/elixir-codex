package com.elixircodex.backend.stack;

import java.util.List;

public record StackResponse(
        List<IngredientCardSummary> ingredientCards,
        int totalScore,
        boolean affiliateBoost,
        boolean canSynthesize,
        List<String> includedSupplements
) {
    public record IngredientCardSummary(Long id, String name, Grade grade) {
    }
}
