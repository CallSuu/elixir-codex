package com.elixircodex.backend.alchemy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.function.IntSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatRollServiceTest {

    @Mock
    private IntSupplier percentRoll;

    private StatRollService service() {
        return new StatRollService(percentRoll);
    }

    @Test
    void 테마별로_정의된_스탯_이름이_그대로_나온다() {
        when(percentRoll.getAsInt()).thenReturn(50);

        assertThat(service().rollStats(ElixirGrade.COMMON, ThemeCategory.SKIN_ANTIOXIDANT).keySet())
                .containsExactly("피부 투명도", "항산화 방어", "장벽 결속력", "수분 보습도");
        assertThat(service().rollStats(ElixirGrade.COMMON, ThemeCategory.FATIGUE_ENERGY).keySet())
                .containsExactly("활력 마나량", "신속 순환력", "심장 박동력", "피로 무력화");
        assertThat(service().rollStats(ElixirGrade.COMMON, ThemeCategory.DIET_BLOODSUGAR).keySet())
                .containsExactly("당독소 봉인", "지방 연소열", "포만 유지력", "흡수 차단력");
        assertThat(service().rollStats(ElixirGrade.COMMON, ThemeCategory.SLEEP_REST).keySet())
                .containsExactly("스트레스 차단", "심연 수면도", "근육 이완도", "독소 정화력");
    }

    @Test
    void 굴림이_0이면_등급_최솟값이_나온다() {
        when(percentRoll.getAsInt()).thenReturn(0);

        Map<String, Integer> stats = service().rollStats(ElixirGrade.RARE, ThemeCategory.FATIGUE_ENERGY);

        assertThat(stats).containsValues(40, 40, 40, 40);
    }

    @Test
    void 굴림이_99이면_등급_최댓값이_나온다() {
        when(percentRoll.getAsInt()).thenReturn(99);

        Map<String, Integer> stats = service().rollStats(ElixirGrade.RARE, ThemeCategory.FATIGUE_ENERGY);

        assertThat(stats).containsValues(70, 70, 70, 70);
    }

    @Test
    void 굴림이_중간값이면_비례한_값이_나온다() {
        when(percentRoll.getAsInt()).thenReturn(50);

        Map<String, Integer> stats = service().rollStats(ElixirGrade.COMMON, ThemeCategory.DIET_BLOODSUGAR);

        // COMMON 범위 20~50(span 30), percent=50 -> 20 + round(30*50/99.0) = 20 + 15 = 35
        assertThat(stats).containsValues(35, 35, 35, 35);
    }

    @Test
    void 등급별_결과값이_정의된_범위_안에_들어온다() {
        for (ElixirGrade grade : ElixirGrade.values()) {
            int min = switch (grade) {
                case COMMON -> 20;
                case RARE -> 40;
                case EPIC -> 60;
                case PRISMATIC_LEGENDARY -> 85;
            };
            int max = switch (grade) {
                case COMMON -> 50;
                case RARE -> 70;
                case EPIC -> 90;
                case PRISMATIC_LEGENDARY -> 100;
            };

            for (int percent = 0; percent < 100; percent++) {
                when(percentRoll.getAsInt()).thenReturn(percent);
                Map<String, Integer> stats = service().rollStats(grade, ThemeCategory.SLEEP_REST);
                stats.values().forEach(value -> assertThat(value).isBetween(min, max));
            }
        }
    }
}
