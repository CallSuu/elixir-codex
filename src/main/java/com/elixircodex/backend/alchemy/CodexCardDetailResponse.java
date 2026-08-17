package com.elixircodex.backend.alchemy;

import java.util.Map;

public record CodexCardDetailResponse(Long id, String name, ElixirGrade grade, String imageUrl, Long serialNumber,
                                       String ingredientSummary, String adviserComment, Map<String, Integer> stats) {
}
