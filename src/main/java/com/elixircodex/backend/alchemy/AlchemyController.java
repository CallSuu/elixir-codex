package com.elixircodex.backend.alchemy;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/alchemy")
@RequiredArgsConstructor
public class AlchemyController {

    private final AlchemyNameService alchemyNameService;

    @PostMapping("/generate-name")
    public ResponseEntity<?> generateName(@RequestBody NameGenerationRequest request) {
        if (request.ingredientNames() == null || request.ingredientNames().size() < 2) {
            return ResponseEntity.badRequest().body(Map.of("message", "재료가 최소 2개 이상 필요합니다"));
        }

        return ResponseEntity.ok(alchemyNameService.generate(request));
    }
}
