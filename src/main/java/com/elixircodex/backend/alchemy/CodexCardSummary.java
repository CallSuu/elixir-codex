package com.elixircodex.backend.alchemy;

public record CodexCardSummary(Long id, String name, ElixirGrade grade, String imageUrl, Long serialNumber,
                                boolean isMutated) {
}
