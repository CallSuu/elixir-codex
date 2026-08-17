package com.elixircodex.backend.stack;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StackServiceTest {

    @Mock
    private SupplementLogRepository supplementLogRepository;

    @Mock
    private IngredientCardRepository ingredientCardRepository;

    private StackService service;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        service = new StackService(supplementLogRepository, ingredientCardRepository);
    }

    @Test
    void 오늘_인증된_영양제가_없으면_예외를_던진다() {
        when(supplementLogRepository.findByOwnerIdAndConsumedDateAndIsVerifiedTrue(1L, LocalDate.now()))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.evaluate(1L, new StackRequest(List.of(10L))))
                .isInstanceOf(StackValidationException.class)
                .hasMessage("오늘 인증된 영양제가 없습니다");
    }

    @Test
    void 투입할_재료가_없으면_예외를_던진다() {
        SupplementLog log = SupplementLog.builder()
                .id(1L).ownerId(1L).productName("종합비타민")
                .isVerified(true).consumedDate(LocalDate.now()).build();
        when(supplementLogRepository.findByOwnerIdAndConsumedDateAndIsVerifiedTrue(1L, LocalDate.now()))
                .thenReturn(List.of(log));
        when(ingredientCardRepository.findAllById(List.of(10L))).thenReturn(List.of());

        assertThatThrownBy(() -> service.evaluate(1L, new StackRequest(List.of(10L))))
                .isInstanceOf(StackValidationException.class)
                .hasMessage("투입할 재료 카드가 없습니다");
    }

    @Test
    void 자동_편입된_영양제_중_하나라도_제휴상품이면_제휴부스트가_적용된다() {
        SupplementLog notAffiliate = SupplementLog.builder()
                .id(1L).ownerId(1L).productName("오메가3")
                .isAffiliateProduct(false).isVerified(true).consumedDate(LocalDate.now()).build();
        SupplementLog affiliate = SupplementLog.builder()
                .id(2L).ownerId(1L).productName("종합비타민")
                .isAffiliateProduct(true).isVerified(true).consumedDate(LocalDate.now()).build();
        when(supplementLogRepository.findByOwnerIdAndConsumedDateAndIsVerifiedTrue(1L, LocalDate.now()))
                .thenReturn(List.of(notAffiliate, affiliate));

        IngredientCard epic = IngredientCard.builder().id(10L).name("천년삼").grade(Grade.EPIC).build();
        IngredientCard common = IngredientCard.builder().id(11L).name("들꽃").grade(Grade.COMMON).build();
        when(ingredientCardRepository.findAllById(List.of(10L, 11L))).thenReturn(List.of(epic, common));

        StackEvaluation evaluation = service.evaluate(1L, new StackRequest(List.of(10L, 11L)));

        assertThat(evaluation.totalScore()).isEqualTo(8);
        assertThat(evaluation.affiliateBoost()).isTrue();
        assertThat(evaluation.ingredientCards()).hasSize(2);
        assertThat(evaluation.includedSupplements()).containsExactly("오메가3", "종합비타민");
    }

    @Test
    void 제휴상품이_하나도_없으면_제휴부스트는_false다() {
        SupplementLog notAffiliate = SupplementLog.builder()
                .id(1L).ownerId(1L).productName("오메가3")
                .isAffiliateProduct(false).isVerified(true).consumedDate(LocalDate.now()).build();
        when(supplementLogRepository.findByOwnerIdAndConsumedDateAndIsVerifiedTrue(1L, LocalDate.now()))
                .thenReturn(List.of(notAffiliate));

        IngredientCard common = IngredientCard.builder().id(11L).name("들꽃").grade(Grade.COMMON).build();
        when(ingredientCardRepository.findAllById(List.of(11L))).thenReturn(List.of(common));

        StackEvaluation evaluation = service.evaluate(1L, new StackRequest(List.of(11L)));

        assertThat(evaluation.affiliateBoost()).isFalse();
        assertThat(evaluation.includedSupplements()).containsExactly("오메가3");
    }
}
