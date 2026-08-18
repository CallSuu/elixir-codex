package com.elixircodex.backend.alchemy;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Base64;
import java.util.List;

@Component
@Profile("!prod")
@RequiredArgsConstructor
public class FixedRecipeDataInitializer implements CommandLineRunner {

    private static final String ART_DIRECTORY = "fixed-recipe-art/";

    private final FixedRecipeRepository fixedRecipeRepository;

    @Override
    public void run(String... args) {
        if (fixedRecipeRepository.count() > 0) {
            return;
        }

        List<FixedRecipe> pool = List.of(
                FixedRecipe.builder()
                        .name("탱글한 백옥 엘릭서")
                        .themeCategory(ThemeCategory.SKIN_ANTIOXIDANT)
                        .grade(ElixirGrade.EPIC)
                        .requiredIngredientNames(List.of("황금 레몬", "탱탱 젤리", "백옥 진주"))
                        .bonusStatNames(List.of("피부 투명도", "장벽 결속력"))
                        .bonusPercent(20)
                        .imageUrl(loadImageDataUri("1-tangle-baekok.png"))
                        .cardDescription("가마솥에서 눈부신 은백색 거품이 솟구치며 신비로운 향이 퍼집니다. 황금 레몬의 산뜻한 햇살 기운과 백옥 진주의 정화력이 탱탱 젤리의 결속 마력과 완벽히 녹아들었습니다.")
                        .adviserComment("피부 속 깊은 어둠을 걷어내고 세월의 풍파에도 흔들리지 않는 찬란한 백옥빛 결계를 둘러줄게. 아주 피부가 맑고 깨끗해질 것 같은 완벽한 배합이야!")
                        .scientificExplanation("황금 레몬(비타민 C)은 콜라겐 합성 효소의 필수 조효소로 작용하며 활성산소를 제거합니다. 탱탱 젤리(콜라겐)는 진피층 섬유아세포를 활성화해 무너지지 않는 탄력망을 복원합니다. 백옥 진주(글루타치온)는 멜라닌 생성을 억제하고 산화된 비타민 C를 환원시켜 재생 회로를 가동합니다. 종합적으로, 비타민 C가 콜라겐 합성을 촉진하고 글루타치온이 멜라닌을 억제하며 산화된 비타민 C를 지속적으로 환원시킵니다. 이 세 성분이 맞물려 콜라겐 장벽 형성과 비타민 C 재생 회로를 가동해 피부 탄력 및 미백 시너지를 극대화합니다.")
                        .build(),
                FixedRecipe.builder()
                        .name("불타는 태양 엘릭서")
                        .themeCategory(ThemeCategory.FATIGUE_ENERGY)
                        .grade(ElixirGrade.EPIC)
                        .requiredIngredientNames(List.of("활력초", "심장 태엽", "마룡 뿔"))
                        .bonusStatNames(List.of("활력 마나량", "신속 순환력"))
                        .bonusPercent(25)
                        .imageUrl(loadImageDataUri("2-burning-sun.png"))
                        .cardDescription("가마솥 안에서 붉은 용의 맥박 같은 맹렬한 열기가 요동칩니다. 심장 태엽이 거세게 회전하며 마룡 뿔의 폭발적인 혈류 마력과 활력초의 생기를 빨아들였습니다.")
                        .adviserComment("지친 용사의 영혼에 꺼지지 않는 태양 마나 엔진을 장착해 줄게. 더 폭발적인 대사열을 원한다면 씁쓸한 녹차잎을 한 꼬집 더해봐도 좋아!")
                        .scientificExplanation("활력초(비타민 B군)는 세포 내 TCA 회로의 필수 조효소로 작동해 즉각적인 생체 에너지를 충전합니다. 마룡 뿔(L-아르기닌)은 산화질소(NO)를 생성해 혈관을 확장하고 전신 산소 공급 속도를 높입니다. 심장 태엽(코엔자임Q10)은 미토콘드리아 전자전달계에서 생체 에너지 단위인 ATP 생성을 촉진합니다. 종합적으로, 아르기닌의 혈류 확장이 산소와 비타민 B군의 대사 연료 공급을 가속하고, 코엔자임Q10이 이를 미토콘드리아에서 ATP 에너지로 즉시 전환합니다. 산소 공급-대사 촉진-에너지 생성이 삼위일체를 이루어 피로를 원천 봉쇄합니다.")
                        .build(),
                FixedRecipe.builder()
                        .name("가뿐한 칠흑 엘릭서")
                        .themeCategory(ThemeCategory.DIET_BLOODSUGAR)
                        .grade(ElixirGrade.EPIC)
                        .requiredIngredientNames(List.of("홀쭉 열매", "녹차잎", "바나바잎"))
                        .bonusStatNames(List.of("당독소 봉인", "지방 연소열"))
                        .bonusPercent(20)
                        .imageUrl(loadImageDataUri("3-light-jilheuk.png"))
                        .cardDescription("짙은 비취빛 안개가 가마솥을 휘감으며 몸을 무겁게 짓누르던 탁기를 순식간에 흡수합니다. 바나바잎의 고대 결계가 식후 당독소를 가두고, 홀쭉 열매와 녹차잎의 정화열이 불필요한 지방의 족쇄를 모조리 불태웠습니다.")
                        .adviserComment("식후에 찾아오는 나태한 졸음을 쫓아내고 몸을 깃털처럼 가볍게 만들어 줄게. 가짜 허기까지 꽉 잡고 싶다면 수분을 머금는 포만 이끼를 추가해봐!")
                        .scientificExplanation("바나바잎은 포도당 수송체를 활성화해 혈중 당분을 세포로 이동시켜 식후 혈당 스파이크를 막습니다. 홀쭉 열매(가르시니아)는 지방 합성 효소를 차단해 잉여 탄수화물이 체지방으로 축적되는 경로를 억제합니다. 녹차잎(카테킨)은 교감신경을 자극해 기초 대사열을 발생시키고 저장된 체지방 연소를 유도합니다. 종합적으로, 바나바잎이 포도당 흡수를 도와 식후 혈당 스파이크를 막고, 가르시니아가 잉여 탄수화물의 지방 합성을 차단합니다. 여기에 카테킨의 대사열 발생이 더해져 당 흡수 억제-지방 합성 차단-저장 지방 연소의 3중 대사 방어선을 완성합니다.")
                        .build(),
                FixedRecipe.builder()
                        .name("은은한 달빛 엘릭서")
                        .themeCategory(ThemeCategory.SLEEP_REST)
                        .grade(ElixirGrade.EPIC)
                        .requiredIngredientNames(List.of("안정석", "평온초", "해독 엉겅퀴"))
                        .bonusStatNames(List.of("스트레스 차단", "심연 수면도"))
                        .bonusPercent(25)
                        .imageUrl(loadImageDataUri("4-moonlight.png"))
                        .cardDescription("가마솥 수면 위로 은은한 달빛 호수가 잔잔하게 차오르며 고요한 밤의 향기가 피어납니다. 안정석의 묵직한 파동과 평온초의 안식 기운이 신경을 감싸고, 해독 엉겅퀴가 잠든 사이 체내 독소를 맑게 정화합니다.")
                        .adviserComment("오늘 밤은 모든 근심을 내려놓고 가장 깊고 평화로운 꿈의 심연으로 여행을 떠나봐. 뒤척임 없이 깊은 잠에 빠져들 수 있는 최고의 나이트 루틴이야.")
                        .scientificExplanation("평온초(L-테아닌)는 흥분 신경계를 진정시키고 안정 뇌파인 알파(α)파 방출을 유도합니다. 안정석(마그네슘)은 근육 이완과 신경계 평정을 도와 깊은 서파 수면에 머물게 합니다. 해독 엉겅퀴(밀크씨슬)는 수면 중 간세포의 글루타치온 합성과 야간 독소 해독을 진행합니다. 종합적으로, 테아닌의 정신적 이완과 마그네슘의 근육 이완으로 깊은 숙면에 도달하며, 수면 시간 동안 밀크씨슬의 실리마린이 간세포 해독 및 재생을 진행하여 기상 시 피로를 상쾌하게 정화합니다.")
                        .build(),
                FixedRecipe.builder()
                        .name("온전한 조화 엘릭서")
                        .themeCategory(ThemeCategory.FATIGUE_ENERGY)
                        .grade(ElixirGrade.EPIC)
                        .requiredIngredientNames(List.of("황금 레몬", "심해 오일", "안정석", "황금 포자"))
                        .bonusStatNames(List.of("활력 마나량", "항산화 방어", "스트레스 차단"))
                        .bonusPercent(25)
                        .imageUrl(loadImageDataUri("5-harmony.png"))
                        .cardDescription("황금 레몬의 상큼한 빛과 심해 오일의 푸른 윤슬이 가마솥 안에서 소용돌이칩니다. 안정석의 차분한 기운과 황금 포자의 생명력이 융합되자, 은은한 에메랄드빛 광채와 함께 온몸의 긴장을 녹이는 맑고 깊은 향기가 피어오릅니다.")
                        .adviserComment("체내 장벽부터 세포 끝까지 빈틈없이 채워주는 완벽한 올인원 배합이야! 잔병치레나 지친 피로 따윈 얼씬도 못 하겠는걸? 이 루틴 그대로 매일 유지해봐!")
                        .scientificExplanation("황금 레몬(비타민 C)은 수용성 항산화 조효소로 작용해 체내 유해 활성산소를 제거하고 면역 세포 활성을 지원합니다. 심해 오일(오메가3)은 세포막 인지질 구조의 유동성을 높이고 염증성 사이토카인 생성을 억제해 전신 미세 염증을 완화합니다. 안정석(마그네슘)은 300종 이상의 생체 효소 반응을 보조하며 신경 흥분과 근육 경직을 풀어 심신을 안정화합니다. 황금 포자(유산균)는 장내 유익균 총을 형성하여 체내 면역세포의 70%를 담당하는 장벽을 강화하고 영양소 흡수율을 극대화합니다. 종합적으로, 유산균이 장벽을 튼튼히 다져 영양 흡수율을 높이면, 오메가3가 세포막 유동성을 개선해 비타민 C와 마그네슘의 세포 내 흡수를 가속합니다. 비타민 C의 수용성 항산화와 오메가3의 지용성 항염 작용이 결합하고, 마그네슘이 신경·근육 긴장을 완화해 장-뇌 축과 전신 면역 방어선을 동시에 완성합니다.")
                        .build()
        );

        fixedRecipeRepository.saveAll(pool);
    }

    private String loadImageDataUri(String filename) {
        try {
            byte[] bytes = new ClassPathResource(ART_DIRECTORY + filename).getInputStream().readAllBytes();
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (IOException e) {
            throw new UncheckedIOException("고정 레시피 아트 이미지를 읽지 못했습니다: " + filename, e);
        }
    }
}
