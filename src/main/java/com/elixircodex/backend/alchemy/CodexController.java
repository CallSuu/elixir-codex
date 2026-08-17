package com.elixircodex.backend.alchemy;

import com.elixircodex.backend.auth.AuthenticatedUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/codex")
@RequiredArgsConstructor
public class CodexController {

    private final ElixirCardRepository elixirCardRepository;
    private final AuthenticatedUserService authenticatedUserService;

    @GetMapping
    public List<CodexCardSummary> getCodex() {
        Long ownerId = authenticatedUserService.getCurrentUserId();
        return elixirCardRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId).stream()
                .filter(card -> !card.isMutated())
                .map(this::toSummary)
                .toList();
    }

    @GetMapping("/mutations")
    public List<CodexCardSummary> getMutations() {
        Long ownerId = authenticatedUserService.getCurrentUserId();
        return elixirCardRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId).stream()
                .filter(ElixirCard::isMutated)
                .map(this::toSummary)
                .toList();
    }

    private CodexCardSummary toSummary(ElixirCard card) {
        return new CodexCardSummary(card.getId(), card.getName(), card.getGrade(),
                card.getImageUrl(), card.getSerialNumber(), card.isMutated());
    }

    @GetMapping("/{elixirCardId}")
    public CodexCardDetailResponse getCodexDetail(@PathVariable Long elixirCardId) {
        Long ownerId = authenticatedUserService.getCurrentUserId();
        ElixirCard card = elixirCardRepository.findById(elixirCardId)
                .filter(c -> c.getOwnerId().equals(ownerId))
                .orElseThrow(() -> new CodexValidationException("존재하지 않는 카드입니다"));

        return new CodexCardDetailResponse(card.getId(), card.getName(), card.getGrade(), card.getImageUrl(),
                card.getSerialNumber(), card.getIngredientSummary(), card.getAdviserComment(), card.getStats());
    }
}
