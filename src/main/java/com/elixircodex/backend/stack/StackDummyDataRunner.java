package com.elixircodex.backend.stack;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@Profile("!prod")
@RequiredArgsConstructor
public class StackDummyDataRunner implements CommandLineRunner {

    private static final Long OWNER_ID = 1L;
    private static final Long OWNER_ID_WITHOUT_TODAY_VERIFICATION = 2L;

    private final IngredientCardRepository ingredientCardRepository;
    private final SupplementLogRepository supplementLogRepository;

    @Override
    public void run(String... args) {
        if (ingredientCardRepository.count() > 0) {
            return;
        }

        ingredientCardRepository.saveAll(List.of(
                IngredientCard.builder()
                        .ownerId(OWNER_ID).name("천년삼").grade(Grade.EPIC)
                        .sourceQuestTitle("quest-001").quantity(2).build(),
                IngredientCard.builder()
                        .ownerId(OWNER_ID).name("영지버섯").grade(Grade.RARE)
                        .sourceQuestTitle("quest-002").quantity(3).build(),
                IngredientCard.builder()
                        .ownerId(OWNER_ID).name("들꽃").grade(Grade.COMMON)
                        .sourceQuestTitle("quest-003").quantity(5).build()
        ));

        supplementLogRepository.saveAll(List.of(
                SupplementLog.builder()
                        .ownerId(OWNER_ID).productName("종합비타민")
                        .isAffiliateProduct(true).isVerified(true)
                        .consumedDate(LocalDate.now()).build(),
                SupplementLog.builder()
                        .ownerId(OWNER_ID).productName("오메가3")
                        .isAffiliateProduct(false).isVerified(true)
                        .consumedDate(LocalDate.now()).build(),
                SupplementLog.builder()
                        .ownerId(OWNER_ID).productName("유산균")
                        .isAffiliateProduct(false).isVerified(false)
                        .consumedDate(LocalDate.now()).build(),
                SupplementLog.builder()
                        .ownerId(OWNER_ID_WITHOUT_TODAY_VERIFICATION).productName("어제자 비타민")
                        .isAffiliateProduct(false).isVerified(true)
                        .consumedDate(LocalDate.now().minusDays(1)).build()
        ));
    }
}
