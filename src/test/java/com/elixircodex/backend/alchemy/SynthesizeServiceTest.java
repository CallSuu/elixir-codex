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
import java.util.LinkedHashMap;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SynthesizeServiceTest {

    @Mock
    private StackService stackService;
    @Mock
    private ElixirCardRepository elixirCardRepository;
    @Mock
    private FixedRecipeRepository fixedRecipeRepository;
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
        return new SynthesizeService(stackService, elixirCardRepository, fixedRecipeRepository, alchemyNameService,
                artMatchingService, artGenerationService, statRollService, false, percentRoll, mutationRoll);
    }

    private SynthesizeService serviceWithArtGeneration() {
        return new SynthesizeService(stackService, elixirCardRepository, fixedRecipeRepository, alchemyNameService,
                artMatchingService, artGenerationService, statRollService, true, percentRoll, mutationRoll);
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

    private FixedRecipe fixedRecipe(String name, ThemeCategory theme, List<String> ingredients,
                                     List<String> bonusStats, int bonusPercent) {
        return FixedRecipe.builder()
                .id(1L).name(name).themeCategory(theme).grade(ElixirGrade.EPIC)
                .requiredIngredientNames(ingredients).bonusStatNames(bonusStats).bonusPercent(bonusPercent)
                .cardDescription("카드 설명").adviserComment("늘해랑 조언").scientificExplanation("과학적 설명")
                .build();
    }

    // 고정 레시피 매칭 테스트 전용: 매칭되면 mutationRoll/percentRoll이 전혀 호출되지 않아야 하므로,
    // givenNoPriorSynthesisToday()의 mutationRoll 기본 스텁까지 걸면 UnnecessaryStubbingException이 난다.
    private void givenNoPriorSynthesisTodayOnly() {
        when(elixirCardRepository.countByOwnerIdAndCreatedAtBetween(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(0L);
    }

    private List<IngredientCard> ingredientCards(List<String> names) {
        return names.stream()
                .map(name -> IngredientCard.builder().id((long) name.hashCode()).name(name).grade(Grade.COMMON).build())
                .toList();
    }

    @Test
    void 레시피1_재료가_정확히_일치하면_탱글한_백옥_엘릭서로_고정되고_절차형_판정을_건너뛴다() {
        FixedRecipe recipe = fixedRecipe("탱글한 백옥 엘릭서", ThemeCategory.SKIN_ANTIOXIDANT,
                List.of("황금 레몬", "탱탱 젤리", "백옥 진주"), List.of("피부 투명도", "장벽 결속력"), 20);
        when(fixedRecipeRepository.findAll()).thenReturn(List.of(recipe));
        when(elixirCardRepository.countByOwnerIdAndCreatedAtBetween(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(0L);
        List<IngredientCard> ingredients = ingredientCards(List.of("황금 레몬", "탱탱 젤리", "백옥 진주"));
        // totalScore=15는 절차형 로직이었다면 프리즈마틱 판정 대상(>=10)이고, percentRoll 기본값(0)은 그 판정을 성공시킨다.
        // 그런데도 최종 등급이 recipe.grade(EPIC) 그대로 나온다면 프리즈마틱/업그레이드 판정 자체가 통째로 스킵된 것이다.
        when(stackService.evaluate(eq(1L), any(StackRequest.class)))
                .thenReturn(new StackEvaluation(ingredients, 15, false, List.of()));
        Map<String, Integer> rolledStats = new LinkedHashMap<>(Map.of(
                "피부 투명도", 50, "항산화 방어", 40, "장벽 결속력", 45, "수분 보습도", 60));
        when(statRollService.rollStats(ElixirGrade.EPIC, ThemeCategory.SKIN_ANTIOXIDANT)).thenReturn(rolledStats);
        givenArtMatchReturns("url");

        SynthesizeResponse response = service().synthesize(1L,
                new SynthesizeRequest(List.of(1L, 2L, 3L), ThemeCategory.FATIGUE_ENERGY));

        assertThat(response.name()).isEqualTo("탱글한 백옥 엘릭서");
        assertThat(response.grade()).isEqualTo(ElixirGrade.EPIC);
        assertThat(response.serialNumber()).isNull();
        assertThat(response.adviserComment()).isEqualTo("늘해랑 조언");
        assertThat(response.cardDescription()).isEqualTo("카드 설명");
        assertThat(response.scientificExplanation()).isEqualTo("과학적 설명");
        assertThat(response.stats().get("피부 투명도")).isEqualTo(60); // 50 * 1.2
        assertThat(response.stats().get("장벽 결속력")).isEqualTo(54); // 45 * 1.2
        assertThat(response.stats().get("항산화 방어")).isEqualTo(40); // 보너스 대상 아님, 그대로
        verifyNoInteractions(alchemyNameService);
        verify(artMatchingService).findImageUrl(Grade.EPIC, ThemeCategory.SKIN_ANTIOXIDANT);
    }

    @Test
    void 레시피2_재료가_정확히_일치하면_불타는_태양_엘릭서로_고정된다() {
        FixedRecipe recipe = fixedRecipe("불타는 태양 엘릭서", ThemeCategory.FATIGUE_ENERGY,
                List.of("활력초", "심장 태엽", "마룡 뿔"), List.of("활력 마나량", "신속 순환력"), 25);
        when(fixedRecipeRepository.findAll()).thenReturn(List.of(recipe));
        givenNoPriorSynthesisTodayOnly();
        List<IngredientCard> ingredients = ingredientCards(List.of("활력초", "심장 태엽", "마룡 뿔"));
        when(stackService.evaluate(eq(1L), any(StackRequest.class)))
                .thenReturn(new StackEvaluation(ingredients, 5, false, List.of()));
        when(statRollService.rollStats(ElixirGrade.EPIC, ThemeCategory.FATIGUE_ENERGY))
                .thenReturn(new LinkedHashMap<>(Map.of("활력 마나량", 60, "신속 순환력", 50, "심장 박동력", 40, "피로 무력화", 45)));
        givenArtMatchReturns("url");

        SynthesizeResponse response = service().synthesize(1L,
                new SynthesizeRequest(List.of(1L, 2L, 3L), ThemeCategory.SLEEP_REST));

        assertThat(response.name()).isEqualTo("불타는 태양 엘릭서");
        assertThat(response.grade()).isEqualTo(ElixirGrade.EPIC);
    }

    @Test
    void 레시피3_재료가_정확히_일치하면_가뿐한_칠흑_엘릭서로_고정된다() {
        FixedRecipe recipe = fixedRecipe("가뿐한 칠흑 엘릭서", ThemeCategory.DIET_BLOODSUGAR,
                List.of("홀쭉 열매", "녹차잎", "바나바잎"), List.of("당독소 봉인", "지방 연소열"), 20);
        when(fixedRecipeRepository.findAll()).thenReturn(List.of(recipe));
        givenNoPriorSynthesisTodayOnly();
        List<IngredientCard> ingredients = ingredientCards(List.of("홀쭉 열매", "녹차잎", "바나바잎"));
        when(stackService.evaluate(eq(1L), any(StackRequest.class)))
                .thenReturn(new StackEvaluation(ingredients, 5, false, List.of()));
        when(statRollService.rollStats(ElixirGrade.EPIC, ThemeCategory.DIET_BLOODSUGAR))
                .thenReturn(new LinkedHashMap<>(Map.of("당독소 봉인", 60, "지방 연소열", 50, "포만 유지력", 40, "흡수 차단력", 45)));
        givenArtMatchReturns("url");

        SynthesizeResponse response = service().synthesize(1L,
                new SynthesizeRequest(List.of(1L, 2L, 3L), ThemeCategory.SKIN_ANTIOXIDANT));

        assertThat(response.name()).isEqualTo("가뿐한 칠흑 엘릭서");
        assertThat(response.grade()).isEqualTo(ElixirGrade.EPIC);
    }

    @Test
    void 레시피4_재료가_정확히_일치하면_은은한_달빛_엘릭서로_고정된다() {
        FixedRecipe recipe = fixedRecipe("은은한 달빛 엘릭서", ThemeCategory.SLEEP_REST,
                List.of("안정석", "평온초", "해독 엉겅퀴"), List.of("스트레스 차단", "심연 수면도"), 25);
        when(fixedRecipeRepository.findAll()).thenReturn(List.of(recipe));
        givenNoPriorSynthesisTodayOnly();
        List<IngredientCard> ingredients = ingredientCards(List.of("안정석", "평온초", "해독 엉겅퀴"));
        when(stackService.evaluate(eq(1L), any(StackRequest.class)))
                .thenReturn(new StackEvaluation(ingredients, 5, false, List.of()));
        when(statRollService.rollStats(ElixirGrade.EPIC, ThemeCategory.SLEEP_REST))
                .thenReturn(new LinkedHashMap<>(Map.of("스트레스 차단", 60, "심연 수면도", 50, "근육 이완도", 40, "독소 정화력", 45)));
        givenArtMatchReturns("url");

        SynthesizeResponse response = service().synthesize(1L,
                new SynthesizeRequest(List.of(1L, 2L, 3L), ThemeCategory.DIET_BLOODSUGAR));

        assertThat(response.name()).isEqualTo("은은한 달빛 엘릭서");
        assertThat(response.grade()).isEqualTo(ElixirGrade.EPIC);
    }

    @Test
    void 레시피5_재료4개가_정확히_일치하면_온전한_조화_엘릭서로_고정된다() {
        FixedRecipe recipe = fixedRecipe("온전한 조화 엘릭서", ThemeCategory.FATIGUE_ENERGY,
                List.of("황금 레몬", "심해 오일", "안정석", "황금 포자"),
                List.of("활력 마나량", "항산화 방어", "스트레스 차단"), 25);
        when(fixedRecipeRepository.findAll()).thenReturn(List.of(recipe));
        givenNoPriorSynthesisTodayOnly();
        List<IngredientCard> ingredients = ingredientCards(List.of("황금 레몬", "심해 오일", "안정석", "황금 포자"));
        when(stackService.evaluate(eq(1L), any(StackRequest.class)))
                .thenReturn(new StackEvaluation(ingredients, 7, false, List.of()));
        when(statRollService.rollStats(ElixirGrade.EPIC, ThemeCategory.FATIGUE_ENERGY))
                .thenReturn(new LinkedHashMap<>(Map.of("활력 마나량", 60, "신속 순환력", 50, "심장 박동력", 40, "피로 무력화", 45)));
        givenArtMatchReturns("url");

        SynthesizeResponse response = service().synthesize(1L,
                new SynthesizeRequest(List.of(1L, 2L, 3L, 4L), ThemeCategory.SKIN_ANTIOXIDANT));

        assertThat(response.name()).isEqualTo("온전한 조화 엘릭서");
        assertThat(response.grade()).isEqualTo(ElixirGrade.EPIC);
        // 보너스 스탯명 중 "항산화 방어"/"스트레스 차단"은 FATIGUE_ENERGY 스탯 목록에 없어 조용히 무시되고,
        // 목록에 있는 "활력 마나량"만 실제로 보너스가 적용된다.
        assertThat(response.stats().get("활력 마나량")).isEqualTo(75); // 60 * 1.25
    }

    @Test
    void 필요재료보다_하나_많으면_매칭되지_않고_절차형_로직으로_진행된다() {
        FixedRecipe recipe = fixedRecipe("탱글한 백옥 엘릭서", ThemeCategory.SKIN_ANTIOXIDANT,
                List.of("황금 레몬", "탱탱 젤리", "백옥 진주"), List.of("피부 투명도"), 20);
        when(fixedRecipeRepository.findAll()).thenReturn(List.of(recipe));
        givenNoPriorSynthesisToday();
        List<IngredientCard> ingredients = ingredientCards(List.of("황금 레몬", "탱탱 젤리", "백옥 진주", "이슬 한 방울"));
        when(stackService.evaluate(eq(1L), any(StackRequest.class)))
                .thenReturn(new StackEvaluation(ingredients, 4, false, List.of()));
        givenNameGenerationSucceeds();
        givenArtMatchReturns("url");
        when(percentRoll.getAsInt()).thenReturn(50);

        SynthesizeResponse response = service().synthesize(1L,
                new SynthesizeRequest(List.of(1L, 2L, 3L, 4L), ThemeCategory.SKIN_ANTIOXIDANT));

        assertThat(response.name()).isEqualTo("심해의 정화 오일");
        assertThat(response.scientificExplanation()).isNull();
        assertThat(response.cardDescription()).isNull();
    }

    @Test
    void 필요재료보다_하나_적으면_매칭되지_않고_절차형_로직으로_진행된다() {
        FixedRecipe recipe = fixedRecipe("탱글한 백옥 엘릭서", ThemeCategory.SKIN_ANTIOXIDANT,
                List.of("황금 레몬", "탱탱 젤리", "백옥 진주"), List.of("피부 투명도"), 20);
        when(fixedRecipeRepository.findAll()).thenReturn(List.of(recipe));
        givenNoPriorSynthesisToday();
        List<IngredientCard> ingredients = ingredientCards(List.of("황금 레몬", "탱탱 젤리"));
        when(stackService.evaluate(eq(1L), any(StackRequest.class)))
                .thenReturn(new StackEvaluation(ingredients, 2, false, List.of()));
        givenNameGenerationSucceeds();
        givenArtMatchReturns("url");
        when(percentRoll.getAsInt()).thenReturn(50);

        SynthesizeResponse response = service().synthesize(1L,
                new SynthesizeRequest(List.of(1L, 2L), ThemeCategory.SKIN_ANTIOXIDANT));

        assertThat(response.name()).isEqualTo("심해의 정화 오일");
        assertThat(response.scientificExplanation()).isNull();
        assertThat(response.cardDescription()).isNull();
    }

    @Test
    void 보너스_적용후_스탯값이_100을_넘으면_100으로_고정된다() {
        FixedRecipe recipe = fixedRecipe("탱글한 백옥 엘릭서", ThemeCategory.SKIN_ANTIOXIDANT,
                List.of("황금 레몬", "탱탱 젤리", "백옥 진주"), List.of("피부 투명도"), 25);
        when(fixedRecipeRepository.findAll()).thenReturn(List.of(recipe));
        givenNoPriorSynthesisTodayOnly();
        List<IngredientCard> ingredients = ingredientCards(List.of("황금 레몬", "탱탱 젤리", "백옥 진주"));
        when(stackService.evaluate(eq(1L), any(StackRequest.class)))
                .thenReturn(new StackEvaluation(ingredients, 5, false, List.of()));
        when(statRollService.rollStats(ElixirGrade.EPIC, ThemeCategory.SKIN_ANTIOXIDANT))
                .thenReturn(new LinkedHashMap<>(Map.of(
                        "피부 투명도", 90, "항산화 방어", 40, "장벽 결속력", 45, "수분 보습도", 60)));
        givenArtMatchReturns("url");

        SynthesizeResponse response = service().synthesize(1L,
                new SynthesizeRequest(List.of(1L, 2L, 3L), ThemeCategory.SKIN_ANTIOXIDANT));

        // 90 * 1.25 = 112.5 -> 100으로 캡핑
        assertThat(response.stats().get("피부 투명도")).isEqualTo(100);
    }
}
