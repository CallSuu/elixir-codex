package com.elixircodex.backend.alchemy;

import java.util.Map;

public record SynthesizeResponse(Long elixirCardId, String name, ElixirGrade grade, String imageUrl,
                                  String adviserComment, Long serialNumber, Map<String, Integer> stats,
                                  String scientificExplanation, String cardDescription) {
}
