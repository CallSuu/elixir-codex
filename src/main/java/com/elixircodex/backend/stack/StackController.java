package com.elixircodex.backend.stack;

import com.elixircodex.backend.auth.AuthenticatedUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stack")
@RequiredArgsConstructor
public class StackController {

    private final StackService stackService;
    private final AuthenticatedUserService authenticatedUserService;

    @PostMapping
    public ResponseEntity<?> stack(@RequestBody StackRequest request) {
        Long ownerId = authenticatedUserService.getCurrentUserId();
        StackEvaluation evaluation = stackService.evaluate(ownerId, request);
        List<StackResponse.IngredientCardSummary> summaries = evaluation.ingredientCards().stream()
                .map(card -> new StackResponse.IngredientCardSummary(card.getId(), card.getName(), card.getGrade()))
                .toList();
        StackResponse response = new StackResponse(summaries, evaluation.totalScore(), evaluation.affiliateBoost(), true,
                evaluation.includedSupplements());
        return ResponseEntity.ok(response);
    }
}
