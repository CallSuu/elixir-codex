package com.elixircodex.backend.alchemy;

import com.elixircodex.backend.stack.Grade;
import com.elixircodex.backend.stack.IngredientCard;
import com.elixircodex.backend.stack.StackEvaluation;
import com.elixircodex.backend.stack.StackRequest;
import com.elixircodex.backend.stack.StackService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.IntSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SynthesizeServiceTest {

    @Mock
    private StackService stackService;
    @Mock
    private ElixirCardRepository elixirCardRepository;
    @Mock
    private AlchemyNameService alchemyNameService;
    @Mock
    private ArtMatchingService artMatchingService;
    @Mock
    private ArtGenerationService artGenerationService;
    @Mock
    private StatRollService statRollService;
    @Mock
    private IntSupplier percentRoll;
    @Mock
    private IntSupplier mutationRoll;

    private final SynthesizeRequest request =
            new SynthesizeRequest(List.of(10L, 11L), ThemeCategory.SKIN_ANTIOXIDANT);

    // 아트 생성 자체는 이 테스트들의 관심사가 아니므로 기본적으로 비활성화해
    // 기존 등급/스탯 로직 검증에는 영향이 없도록 한다. 아트 생성/폴백 자체는 별도 테스트에서 검증한다.
    private SynthesizeService service() {
        return new SynthesizeService(stackService, elixirCardRepository, alchemyNameService, artMatchingService,
                artGenerationService, statRollService, false, percentRoll, mutationRoll);
    }

    private SynthesizeService serviceWithArtGeneration() {
        return new SynthesizeService(stackService, elixirCardRepository, alchemyNameService, artMatchingService,
                artGenerationService, statRollService, true, percentRoll, mutationRoll);
    }

    private void givenNoPriorSynthesisToday() {
        when(elixirCardRepository.countByOwnerIdAndCreatedAtBetween(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(0L);
        // 돌연변이 판정 자체는 이 테스트들의 관심사가 아니므로 기본적으로 실패(미판정)시켜
        // 기존 등급/스탯 로직 검증에는 영향이 없도록 한다. 돌연변이 자체는 별도 테스트에서 검증한다.
        when(mutationRoll.getAsInt()).thenReturn(50);
    }

    private void givenStackScore(int score, boolean affiliateBoost) {
        IngredientCard card = IngredientCard.builder().id(10L).name("천년삼").grade(Grade.EPIC).build();
        when(stackService.evaluate(eq(1L), any(StackRequest.class)))
                .thenReturn(new StackEvaluation(List.of(card), score, affiliateBoost, List.of("종합비타민")));
    }

    private void givenNameGenerationSucceeds() {
        when(alchemyNameService.generateName(any(ThemeCategory.class), any(), anyBoolean()))
                .thenReturn(new NameGenerationResponse("심해의 정화 오일", "좋은 조합입니다."));
    }

    private void givenArtMatchReturns(String imageUrl) {
        when(artMatchingService.findImageUrl(any(Grade.class), any(ThemeCategory.class))).thenReturn(imageUrl);
    }

    @Test
    void 오늘_이미_연성했으면_예외를_던진다() {
        givenStackScore(1, false);
        when(elixirCardRepository.countByOwnerIdAndCreatedAtBetween(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(1L);

        assertThatThrownBy(() -> service().synthesize(1L, request))
                .isInstanceOf(SynthesizeValidationException.class)
                .hasMessage("오늘 이미 연성을 완료했습니다");
    }

    @Test
    void 점수2는_COMMON업그레이드_실패시_COMMON으로_확정된다() {
        givenNoPriorSynthesisToday();
        givenStackScore(2, false);
        givenNameGenerationSucceeds();
        givenArtMatchReturns("url");
        when(percentRoll.getAsInt()).thenReturn(50);

        SynthesizeResponse response = service().synthesize(1L, request);

        assertThat(response.grade()).isEqualTo(ElixirGrade.COMMON);
        assertThat(response.serialNumber()).isNull();
    }

    @Test
    void 점수2에서_COMMON업그레이드_굴림이_0이면_EPIC이_된다() {
        givenNoPriorSynthesisToday();
        givenStackScore(2, false);
        givenNameGenerationSucceeds();
        givenArtMatchReturns("url");
        when(percentRoll.getAsInt()).thenReturn(0);

        SynthesizeResponse response = service().synthesize(1L, request);

        assertThat(response.grade()).isEqualTo(ElixirGrade.EPIC);
    }

    @Test
    void 점수2에서_COMMON업그레이드_굴림이_3이면_RARE가_된다() {
        givenNoPriorSynthesisToday();
        givenStackScore(2, false);
        givenNameGenerationSucceeds();
        givenArtMatchReturns("url");
        when(percentRoll.getAsInt()).thenReturn(3);

        SynthesizeResponse response = service().synthesize(1L, request);

        assertThat(response.grade()).isEqualTo(ElixirGrade.RARE);
    }

    @Test
    void 점수6은_RARE업그레이드_굴림이_5면_EPIC이_된다() {
        givenNoPriorSynthesisToday();
        givenStackScore(6, false);
        givenNameGenerationSucceeds();
        givenArtMatchReturns("url");
        when(percentRoll.getAsInt()).thenReturn(5);

        SynthesizeResponse response = service().synthesize(1L, request);

        assertThat(response.grade()).isEqualTo(ElixirGrade.EPIC);
    }

    @Test
    void 점수6은_RARE업그레이드_굴림이_6이면_RARE로_유지된다() {
        givenNoPriorSynthesisToday();
        givenStackScore(6, false);
        givenNameGenerationSucceeds();
        givenArtMatchReturns("url");
        when(percentRoll.getAsInt()).thenReturn(6);

        SynthesizeResponse response = service().synthesize(1L, request);

        assertThat(response.grade()).isEqualTo(ElixirGrade.RARE);
    }

    @Test
    void 점수7은_프리즈마틱_시도없이_EPIC으로_확정된다() {
        givenNoPriorSynthesisToday();
        givenStackScore(7, false);
        givenNameGenerationSucceeds();
        givenArtMatchReturns("url");

        SynthesizeResponse response = service().synthesize(1L, request);

        assertThat(response.grade()).isEqualTo(ElixirGrade.EPIC);
        assertThat(response.serialNumber()).isNull();
    }

    @Test
    void 점수10이상이고_제휴가_아니면_굴림이_4일때_프리즈마틱이_된다() {
        givenNoPriorSynthesisToday();
        givenStackScore(10, false);
        givenNameGenerationSucceeds();
        givenArtMatchReturns("url");
        when(elixirCardRepository.countByGrade(ElixirGrade.PRISMATIC_LEGENDARY)).thenReturn(3L);
        when(percentRoll.getAsInt()).thenReturn(4);

        SynthesizeResponse response = service().synthesize(1L, request);

        assertThat(response.grade()).isEqualTo(ElixirGrade.PRISMATIC_LEGENDARY);
        assertThat(response.serialNumber()).isEqualTo(4L);
    }

    @Test
    void 점수10이상이고_제휴이면_굴림이_20일때도_프리즈마틱이_된다() {
        givenNoPriorSynthesisToday();
        givenStackScore(10, true);
        givenNameGenerationSucceeds();
        givenArtMatchReturns("url");
        when(elixirCardRepository.countByGrade(ElixirGrade.PRISMATIC_LEGENDARY)).thenReturn(0L);
        when(percentRoll.getAsInt()).thenReturn(20);

        SynthesizeResponse response = service().synthesize(1L, request);

        assertThat(response.grade()).isEqualTo(ElixirGrade.PRISMATIC_LEGENDARY);
        assertThat(response.serialNumber()).isEqualTo(1L);
    }

    @Test
    void 점수10이상이어도_프리즈마틱_굴림에_실패하면_EPIC으로_남는다() {
        givenNoPriorSynthesisToday();
        givenStackScore(10, false);
        givenNameGenerationSucceeds();
        givenArtMatchReturns("url");
        when(percentRoll.getAsInt()).thenReturn(50);

        SynthesizeResponse response = service().synthesize(1L, request);

        assertThat(response.grade()).isEqualTo(ElixirGrade.EPIC);
        assertThat(response.serialNumber()).isNull();
    }

    @Test
    void 프리즈마틱_확정시_아트매칭은_EPIC등급으로_요청한다() {
        givenNoPriorSynthesisToday();
        givenStackScore(10, true);
        givenNameGenerationSucceeds();
        givenArtMatchReturns("url");
        when(elixirCardRepository.countByGrade(ElixirGrade.PRISMATIC_LEGENDARY)).thenReturn(0L);
        when(percentRoll.getAsInt()).thenReturn(0);

        service().synthesize(1L, request);

        verify(artMatchingService).findImageUrl(Grade.EPIC, ThemeCategory.SKIN_ANTIOXIDANT);
    }

    @Test
    void ingredientSummary는_재료이름을_콤마로_join한다() {
        givenNoPriorSynthesisToday();
        IngredientCard first = IngredientCard.builder().id(10L).name("천년삼").grade(Grade.EPIC).build();
        IngredientCard second = IngredientCard.builder().id(11L).name("영지버섯").grade(Grade.RARE).build();
        when(stackService.evaluate(eq(1L), any(StackRequest.class)))
                .thenReturn(new StackEvaluation(List.of(first, second), 7, false, List.of("종합비타민")));
        givenNameGenerationSucceeds();
        givenArtMatchReturns("url");

        service().synthesize(1L, request);

        verify(alchemyNameService).generateName(ThemeCategory.SKIN_ANTIOXIDANT, List.of("천년삼", "영지버섯"), false);
    }

    @Test
    void 최종등급과_테마로_산출된_스탯이_카드에_저장된다() {
        givenNoPriorSynthesisToday();
        givenStackScore(2, false);
        givenNameGenerationSucceeds();
        givenArtMatchReturns("url");
        when(percentRoll.getAsInt()).thenReturn(50); // COMMON 유지 (roll >= 7)

        Map<String, Integer> rolledStats = Map.of(
                "피부 투명도", 35, "항산화 방어", 40, "장벽 결속력", 30, "수분 보습도", 45);
        when(statRollService.rollStats(ElixirGrade.COMMON, ThemeCategory.SKIN_ANTIOXIDANT)).thenReturn(rolledStats);

        SynthesizeResponse response = service().synthesize(1L, request);

        ArgumentCaptor<ElixirCard> captor = ArgumentCaptor.forClass(ElixirCard.class);
        verify(elixirCardRepository).save(captor.capture());
        assertThat(captor.getValue().getStats()).isEqualTo(rolledStats);
        assertThat(response.stats()).isEqualTo(rolledStats);
    }

    @Test
    void 생성활성화_상태에서_실시간_생성이_성공하면_그_결과를_사용하고_템플릿매칭은_호출하지_않는다() {
        givenNoPriorSynthesisToday();
        givenStackScore(2, false);
        givenNameGenerationSucceeds();
        when(percentRoll.getAsInt()).thenReturn(50); // COMMON 유지
        when(artGenerationService.generate(any(Grade.class), any(ThemeCategory.class), any(String.class)))
                .thenReturn("https://generated.example.com/art.png");

        SynthesizeResponse response = serviceWithArtGeneration().synthesize(1L, request);

        assertThat(response.imageUrl()).isEqualTo("https://generated.example.com/art.png");
        verify(artMatchingService, org.mockito.Mockito.never()).findImageUrl(any(Grade.class), any(ThemeCategory.class));
    }

    @Test
    void 생성활성화_상태에서_실시간_생성이_실패하면_템플릿매칭으로_폴백한다() {
        givenNoPriorSynthesisToday();
        givenStackScore(2, false);
        givenNameGenerationSucceeds();
        givenArtMatchReturns("fallback-url");
        when(percentRoll.getAsInt()).thenReturn(50); // COMMON 유지
        when(artGenerationService.generate(any(Grade.class), any(ThemeCategory.class), any(String.class)))
                .thenThrow(new ArtGenerationException("실시간 아트 생성 요청이 실패했습니다"));

        SynthesizeResponse response = serviceWithArtGeneration().synthesize(1L, request);

        assertThat(response.imageUrl()).isEqualTo("fallback-url");
    }

    @Test
    void 생성비활성화_상태에서는_실시간_생성을_시도하지_않고_바로_템플릿매칭한다() {
        givenNoPriorSynthesisToday();
        givenStackScore(2, false);
        givenNameGenerationSucceeds();
        givenArtMatchReturns("fallback-url");
        when(percentRoll.getAsInt()).thenReturn(50); // COMMON 유지

        SynthesizeResponse response = service().synthesize(1L, request);

        assertThat(response.imageUrl()).isEqualTo("fallback-url");
        verify(artGenerationService, org.mockito.Mockito.never())
                .generate(any(Grade.class), any(ThemeCategory.class), any(String.class));
    }

    @Test
    void 돌연변이_판정에_성공하면_카드는_isMutated가_true로_저장되고_스탯은_한단계_위_등급으로_산출된다() {
        givenNoPriorSynthesisToday();
        givenStackScore(2, false); // baseGrade COMMON
        givenNameGenerationSucceeds();
        givenArtMatchReturns("url");
        when(percentRoll.getAsInt()).thenReturn(50); // COMMON 업그레이드 실패 → COMMON 유지
        when(mutationRoll.getAsInt()).thenReturn(0); // 4% 확률 성공 (0~3)

        Map<String, Integer> rolledStats = Map.of(
                "피부 투명도", 60, "항산화 방어", 65, "장벽 결속력", 55, "수분 보습도", 68);
        when(statRollService.rollStats(ElixirGrade.RARE, ThemeCategory.SKIN_ANTIOXIDANT)).thenReturn(rolledStats);

        SynthesizeResponse response = service().synthesize(1L, request);

        assertThat(response.grade()).isEqualTo(ElixirGrade.COMMON); // 확정 등급 자체는 그대로
        assertThat(response.stats()).isEqualTo(rolledStats); // 스탯만 한 단계 위(RARE) 범위로 산출

        ArgumentCaptor<ElixirCard> captor = ArgumentCaptor.forClass(ElixirCard.class);
        verify(elixirCardRepository).save(captor.capture());
        assertThat(captor.getValue().isMutated()).isTrue();
        verify(alchemyNameService).generateName(ThemeCategory.SKIN_ANTIOXIDANT, List.of("천년삼"), true);
    }

    @Test
    void 돌연변이_판정에_실패하면_카드는_isMutated가_false로_저장되고_스탯은_원래_등급으로_산출된다() {
        givenNoPriorSynthesisToday();
        givenStackScore(2, false);
        givenNameGenerationSucceeds();
        givenArtMatchReturns("url");
        when(percentRoll.getAsInt()).thenReturn(50); // COMMON 유지
        when(mutationRoll.getAsInt()).thenReturn(4); // 4% 확률 실패 경계값(0~3만 성공)

        Map<String, Integer> rolledStats = Map.of(
                "피부 투명도", 35, "항산화 방어", 40, "장벽 결속력", 30, "수분 보습도", 45);
        when(statRollService.rollStats(ElixirGrade.COMMON, ThemeCategory.SKIN_ANTIOXIDANT)).thenReturn(rolledStats);

        SynthesizeResponse response = service().synthesize(1L, request);

        assertThat(response.stats()).isEqualTo(rolledStats);
        ArgumentCaptor<ElixirCard> captor = ArgumentCaptor.forClass(ElixirCard.class);
        verify(elixirCardRepository).save(captor.capture());
        assertThat(captor.getValue().isMutated()).isFalse();
        verify(alchemyNameService).generateName(ThemeCategory.SKIN_ANTIOXIDANT, List.of("천년삼"), false);
    }

    @Test
    void EPIC등급_돌연변이는_이미_최상위이므로_스탯_등급_상승이_없다() {
        givenNoPriorSynthesisToday();
        givenStackScore(7, false); // baseGrade EPIC으로 바로 확정 (업그레이드 로직 없음)
        givenNameGenerationSucceeds();
        givenArtMatchReturns("url");
        when(mutationRoll.getAsInt()).thenReturn(0); // 돌연변이 성공

        service().synthesize(1L, request);

        verify(statRollService).rollStats(ElixirGrade.EPIC, ThemeCategory.SKIN_ANTIOXIDANT);
    }

    @Test
    void PRISMATIC_LEGENDARY_돌연변이도_이미_최상위이므로_스탯_등급_상승이_없다() {
        givenNoPriorSynthesisToday();
        givenStackScore(10, false);
        givenNameGenerationSucceeds();
        givenArtMatchReturns("url");
        when(elixirCardRepository.countByGrade(ElixirGrade.PRISMATIC_LEGENDARY)).thenReturn(0L);
        when(percentRoll.getAsInt()).thenReturn(0); // 프리즈마틱 확정
        when(mutationRoll.getAsInt()).thenReturn(0); // 돌연변이 성공

        service().synthesize(1L, request);

        verify(statRollService).rollStats(ElixirGrade.PRISMATIC_LEGENDARY, ThemeCategory.SKIN_ANTIOXIDANT);
    }

    @Test
    void 돌연변이여도_아트는_실제_확정등급_그대로_매칭한다() {
        givenNoPriorSynthesisToday();
        givenStackScore(2, false);
        givenNameGenerationSucceeds();
        givenArtMatchReturns("url");
        when(percentRoll.getAsInt()).thenReturn(50); // COMMON 유지
        when(mutationRoll.getAsInt()).thenReturn(0); // 돌연변이 성공 (스탯은 RARE, 아트는 여전히 COMMON)

        service().synthesize(1L, request);

        verify(artMatchingService).findImageUrl(Grade.COMMON, ThemeCategory.SKIN_ANTIOXIDANT);
    }
}
