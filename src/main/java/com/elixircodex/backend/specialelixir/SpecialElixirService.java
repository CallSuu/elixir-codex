package com.elixircodex.backend.specialelixir;

import com.elixircodex.backend.alchemy.AlchemyNameService;
import com.elixircodex.backend.alchemy.ArtMatchingService;
import com.elixircodex.backend.alchemy.NameGenerationResponse;
import com.elixircodex.backend.alchemy.ThemeCategory;
import com.elixircodex.backend.onboarding.OnboardingClassificationService;
import com.elixircodex.backend.stack.Grade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SpecialElixirService {

    private static final int MAX_ELIXIRS_PER_OWNER = 3;

    private final SpecialElixirRepository specialElixirRepository;
    private final OnboardingClassificationService onboardingClassificationService;
    private final AlchemyNameService alchemyNameService;
    private final ArtMatchingService artMatchingService;

    public SpecialElixirResponse create(Long ownerId, String freeText) {
        if (specialElixirRepository.countByOwnerId(ownerId) >= MAX_ELIXIRS_PER_OWNER) {
            throw new SpecialElixirValidationException(
                    "스페셜 엘릭서는 최대 3개까지 저장할 수 있습니다. 기존 엘릭서를 삭제한 뒤 다시 시도해주세요.");
        }

        ThemeCategory themeCategory = onboardingClassificationService.classify(freeText);

        NameGenerationResponse nameResponse = alchemyNameService.generateName(themeCategory, List.of(freeText), false);

        String imageUrl = artMatchingService.findImageUrl(Grade.EPIC, themeCategory);

        SpecialElixir saved = specialElixirRepository.save(SpecialElixir.builder()
                .ownerId(ownerId)
                .name(nameResponse.name())
                .imageUrl(imageUrl)
                .adviserComment(nameResponse.adviserComment())
                .themeCategory(themeCategory)
                .build());

        return toResponse(saved);
    }

    public List<SpecialElixirResponse> list(Long ownerId) {
        return specialElixirRepository.findByOwnerId(ownerId).stream()
                .map(this::toResponse)
                .toList();
    }

    public void delete(Long ownerId, Long id) {
        SpecialElixir elixir = specialElixirRepository.findById(id)
                .orElseThrow(() -> new SpecialElixirValidationException("존재하지 않는 엘릭서입니다"));

        if (!elixir.getOwnerId().equals(ownerId)) {
            throw new SpecialElixirValidationException("본인의 엘릭서만 삭제할 수 있습니다");
        }

        specialElixirRepository.delete(elixir);
    }

    private SpecialElixirResponse toResponse(SpecialElixir elixir) {
        return new SpecialElixirResponse(elixir.getId(), elixir.getName(), elixir.getImageUrl(),
                elixir.getAdviserComment(), elixir.getThemeCategory(), elixir.getCreatedAt());
    }
}
