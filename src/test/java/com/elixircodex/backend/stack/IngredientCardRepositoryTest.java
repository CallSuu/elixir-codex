package com.elixircodex.backend.stack;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = IngredientCardRepositoryTest.TestConfig.class)
class IngredientCardRepositoryTest {

    // @DataJpaTest도 @WebMvcTest와 동일하게 패키지 상위 탐색으로 @SpringBootConfiguration을 찾는데,
    // 이 클래스가 com.elixircodex.backend.stack에 있어 같은 패키지의 엔티티/리포지토리는 기본으로 잡힌다.
    @SpringBootConfiguration
    @AutoConfigurationPackage
    static class TestConfig {
    }

    @Autowired
    private IngredientCardRepository ingredientCardRepository;

    @Test
    void findByOwnerIdAndNameAndGrade는_동일한_이름과_등급의_카드를_찾는다() {
        ingredientCardRepository.save(IngredientCard.builder()
                .ownerId(1L).name("정제된 수분 결정").grade(Grade.COMMON).build());

        Optional<IngredientCard> found =
                ingredientCardRepository.findByOwnerIdAndNameAndGrade(1L, "정제된 수분 결정", Grade.COMMON);

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("정제된 수분 결정");
    }

    @Test
    void findByOwnerIdAndNameAndGrade는_등급이_다르면_찾지_못한다() {
        ingredientCardRepository.save(IngredientCard.builder()
                .ownerId(1L).name("정제된 수분 결정").grade(Grade.COMMON).build());

        Optional<IngredientCard> found =
                ingredientCardRepository.findByOwnerIdAndNameAndGrade(1L, "정제된 수분 결정", Grade.RARE);

        assertThat(found).isEmpty();
    }

    @Test
    void increaseQuantity_호출후_저장하면_수량이_1_증가한다() {
        IngredientCard saved = ingredientCardRepository.save(IngredientCard.builder()
                .ownerId(1L).name("정제된 수분 결정").grade(Grade.COMMON).quantity(1).build());

        IngredientCard found = ingredientCardRepository.findById(saved.getId()).orElseThrow();
        found.increaseQuantity();
        ingredientCardRepository.saveAndFlush(found);

        IngredientCard reloaded = ingredientCardRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getQuantity()).isEqualTo(2);
    }

    @Test
    void 빌더로_생성시_quantity_기본값은_1이다() {
        IngredientCard card = IngredientCard.builder()
                .ownerId(1L).name("정제된 수분 결정").grade(Grade.COMMON).build();

        assertThat(card.getQuantity()).isEqualTo(1);
    }

    @Test
    void findByOwnerIdOrderByGradeDescCreatedAtDesc는_등급_내림차순으로_정렬한다() {
        ingredientCardRepository.save(IngredientCard.builder()
                .ownerId(1L).name("들꽃").grade(Grade.COMMON).build());
        ingredientCardRepository.save(IngredientCard.builder()
                .ownerId(1L).name("천년삼").grade(Grade.EPIC).build());
        ingredientCardRepository.save(IngredientCard.builder()
                .ownerId(1L).name("영지버섯").grade(Grade.RARE).build());

        List<IngredientCard> result = ingredientCardRepository.findByOwnerIdOrderByGradeDescCreatedAtDesc(1L);

        assertThat(result).extracting(IngredientCard::getGrade)
                .containsExactly(Grade.RARE, Grade.EPIC, Grade.COMMON);
    }
}
