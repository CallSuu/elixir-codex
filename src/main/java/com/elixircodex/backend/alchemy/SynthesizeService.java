package com.elixircodex.backend.alchemy;

import com.elixircodex.backend.stack.Grade;
import com.elixircodex.backend.stack.IngredientCard;
import com.elixircodex.backend.stack.StackEvaluation;
import com.elixircodex.backend.stack.StackRequest;
import com.elixircodex.backend.stack.StackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntSupplier;

@Service
public class SynthesizeService {

    private static final Logger log = LoggerFactory.getLogger(SynthesizeService.class);

    private static final int PRISMATIC_MIN_SCORE = 10;
    private static final int PRISMATIC_CHANCE_BASE = 5;
    private static final int PRISMATIC_CHANCE_AFFILIATE = 25;
    private static final int MUTATION_CHANCE = 4;

    private final StackService stackService;
    private final ElixirCardRepository elixirCardRepository;
    private final FixedRecipeRepository fixedRecipeRepository;
    private final AlchemyNameService alchemyNameService;
    private final ArtMatchingService artMatchingService;
    private final ArtGenerationService artGenerationService;
    private final StatRollService statRollService;
    private final boolean artGenerationEnabled;
    private final IntSupplier percentRoll;
    private final IntSupplier mutationRoll;

    @Autowired
    public SynthesizeService(StackService stackService, ElixirCardRepository elixirCardRepository,
                              FixedRecipeRepository fixedRecipeRepository, AlchemyNameService alchemyNameService,
                              ArtMatchingService artMatchingService, ArtGenerationService artGenerationService,
                              StatRollService statRollService,
                              @Value("${elixir.art.generation-enabled}") boolean artGenerationEnabled) {
        this(stackService, elixirCardRepository, fixedRecipeRepository, alchemyNameService, artMatchingService,
                artGenerationService, statRollService, artGenerationEnabled,
                () -> ThreadLocalRandom.current().nextInt(100), () -> ThreadLocalRandom.current().nextInt(100));
    }

    SynthesizeService(StackService stackService, ElixirCardRepository elixirCardRepository,
                       FixedRecipeRepository fixedRecipeRepository, AlchemyNameService alchemyNameService,
                       ArtMatchingService artMatchingService, ArtGenerationService artGenerationService,
                       StatRollService statRollService, boolean artGenerationEnabled, IntSupplier percentRoll,
                       IntSupplier mutationRoll) {
        this.stackService = stackService;
        this.elixirCardRepository = elixirCardRepository;
        this.fixedRecipeRepository = fixedRecipeRepository;
        this.alchemyNameService = alchemyNameService;
        this.artMatchingService = artMatchingService;
        this.artGenerationService = artGenerationService;
        this.statRollService = statRollService;
        this.artGenerationEnabled = artGenerationEnabled;
        this.percentRoll = percentRoll;
        this.mutationRoll = mutationRoll;
    }

    public SynthesizeResponse synthesize(Long ownerId, SynthesizeRequest request) {
        StackEvaluation evaluation = stackService.evaluate(ownerId, new StackRequest(request.ingredientCardIds()));

        ensureNotAlreadySynthesizedToday(ownerId);

        List<String> ingredientNames = evaluation.ingredientCards().stream().map(IngredientCard::getName).toList();
        Optional<FixedRecipe> matchedRecipe = findMatchingRecipe(ingredientNames);

        ElixirCard card = matchedRecipe
                .map(recipe -> buildFixedRecipeCard(ownerId, recipe, ingredientNames))
                .orElseGet(() -> buildProceduralCard(ownerId, request, evaluation, ingredientNames));
        elixirCardRepository.save(card);

        return new SynthesizeResponse(card.getId(), card.getName(), card.getGrade(), card.getImageUrl(),
                card.getAdviserComment(), card.getSerialNumber(), card.getStats(),
                card.getScientificExplanation(), card.getCardDescription());
    }

    private Optional<FixedRecipe> findMatchingRecipe(List<String> ingredientNames) {
        Set<String> ingredientNameSet = Set.copyOf(ingredientNames);
        return fixedRecipeRepository.findAll().stream()
                .filter(recipe -> recipe.getRequiredIngredientNames().size() == ingredientNames.size())
                .filter(recipe -> Set.copyOf(recipe.getRequiredIngredientNames()).equals(ingredientNameSet))
                .findFirst();
    }

    private ElixirCard buildFixedRecipeCard(Long ownerId, FixedRecipe recipe, List<String> ingredientNames) {
        Map<String, Integer> stats = rollBonusedStats(recipe);
        String imageUrl = resolveImageUrl(toArtGrade(recipe.getGrade()), recipe.getThemeCategory(), recipe.getName());

        return ElixirCard.builder()
                .ownerId(ownerId)
                .name(recipe.getName())
                .grade(recipe.getGrade())
                .themeCategory(recipe.getThemeCategory())
                .imageUrl(imageUrl)
                .adviserComment(recipe.getAdviserComment())
                .ingredientSummary(String.join(", ", ingredientNames))
                .stats(stats)
                .scientificExplanation(recipe.getScientificExplanation())
                .cardDescription(recipe.getCardDescription())
                .build();
    }

    private Map<String, Integer> rollBonusedStats(FixedRecipe recipe) {
        Map<String, Integer> stats = statRollService.rollStats(recipe.getGrade(), recipe.getThemeCategory());
        double multiplier = 1 + recipe.getBonusPercent() / 100.0;
        for (String bonusStatName : recipe.getBonusStatNames()) {
            stats.computeIfPresent(bonusStatName,
                    (name, value) -> Math.min(100, (int) Math.round(value * multiplier)));
        }
        return stats;
    }

    private ElixirCard buildProceduralCard(Long ownerId, SynthesizeRequest request, StackEvaluation evaluation,
                                            List<String> ingredientNames) {
        int totalScore = evaluation.totalScore();
        ElixirGrade baseGrade = resolveBaseGrade(totalScore);

        Long serialNumber = null;
        ElixirGrade finalGrade = baseGrade;

        boolean isPrismatic = totalScore >= PRISMATIC_MIN_SCORE && rollPrismatic(evaluation.affiliateBoost());
        if (isPrismatic) {
            finalGrade = ElixirGrade.PRISMATIC_LEGENDARY;
            serialNumber = elixirCardRepository.countByGrade(ElixirGrade.PRISMATIC_LEGENDARY) + 1;
        } else if (baseGrade == ElixirGrade.COMMON) {
            finalGrade = rollCommonUpgrade();
        } else if (baseGrade == ElixirGrade.RARE) {
            finalGrade = rollRareUpgrade();
        }

        boolean isMutated = mutationRoll.getAsInt() < MUTATION_CHANCE;
        ElixirGrade statGrade = isMutated ? upgradeForMutation(finalGrade) : finalGrade;

        Map<String, Integer> stats = statRollService.rollStats(statGrade, request.themeCategory());

        NameGenerationResponse nameResponse =
                alchemyNameService.generateName(request.themeCategory(), ingredientNames, isMutated);

        String imageUrl = resolveImageUrl(toArtGrade(finalGrade), request.themeCategory(), nameResponse.name());

        return ElixirCard.builder()
                .ownerId(ownerId)
                .name(nameResponse.name())
                .grade(finalGrade)
                .themeCategory(request.themeCategory())
                .imageUrl(imageUrl)
                .adviserComment(nameResponse.adviserComment())
                .serialNumber(serialNumber)
                .ingredientSummary(String.join(", ", ingredientNames))
                .stats(stats)
                .isMutated(isMutated)
                .build();
    }

    private void ensureNotAlreadySynthesizedToday(Long ownerId) {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime tomorrowStart = todayStart.plusDays(1);
        long todayCount = elixirCardRepository.countByOwnerIdAndCreatedAtBetween(ownerId, todayStart, tomorrowStart);
        if (todayCount >= 1) {
            throw new SynthesizeValidationException("오늘 이미 연성을 완료했습니다");
        }
    }

    private ElixirGrade resolveBaseGrade(int totalScore) {
        if (totalScore <= 2) {
            return ElixirGrade.COMMON;
        }
        if (totalScore <= 6) {
            return ElixirGrade.RARE;
        }
        return ElixirGrade.EPIC;
    }

    private boolean rollPrismatic(boolean affiliateBoost) {
        int chance = affiliateBoost ? PRISMATIC_CHANCE_AFFILIATE : PRISMATIC_CHANCE_BASE;
        return percentRoll.getAsInt() < chance;
    }

    private ElixirGrade rollCommonUpgrade() {
        int roll = percentRoll.getAsInt();
        if (roll < 1) {
            return ElixirGrade.EPIC;
        }
        if (roll < 7) {
            return ElixirGrade.RARE;
        }
        return ElixirGrade.COMMON;
    }

    private ElixirGrade rollRareUpgrade() {
        int roll = percentRoll.getAsInt();
        return roll < 6 ? ElixirGrade.EPIC : ElixirGrade.RARE;
    }

    private ElixirGrade upgradeForMutation(ElixirGrade grade) {
        return switch (grade) {
            case COMMON -> ElixirGrade.RARE;
            case RARE -> ElixirGrade.EPIC;
            case EPIC, PRISMATIC_LEGENDARY -> grade;
        };
    }

    private String resolveImageUrl(Grade grade, ThemeCategory themeCategory, String elixirName) {
        if (artGenerationEnabled) {
            try {
                return artGenerationService.generate(grade, themeCategory, elixirName);
            } catch (ArtGenerationException e) {
                log.warn("실시간 아트 생성 실패, 템플릿 매칭으로 폴백합니다: {}", e.getMessage());
            }
        }
        return artMatchingService.findImageUrl(grade, themeCategory);
    }

    private Grade toArtGrade(ElixirGrade finalGrade) {
        if (finalGrade == ElixirGrade.PRISMATIC_LEGENDARY) {
            return Grade.EPIC;
        }
        return Grade.valueOf(finalGrade.name());
    }
}
