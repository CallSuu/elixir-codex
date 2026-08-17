package com.elixircodex.backend.stack;

import java.util.List;

public record StackEvaluation(List<IngredientCard> ingredientCards, int totalScore, boolean affiliateBoost,
                                List<String> includedSupplements) {
}
