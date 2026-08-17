package com.elixircodex.backend.alchemy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntSupplier;

@Service
public class StatRollService {

    private static final Map<ThemeCategory, List<String>> STAT_NAMES_BY_THEME = Map.of(
            ThemeCategory.SKIN_ANTIOXIDANT, List.of("피부 투명도", "항산화 방어", "장벽 결속력", "수분 보습도"),
            ThemeCategory.FATIGUE_ENERGY, List.of("활력 마나량", "신속 순환력", "심장 박동력", "피로 무력화"),
            ThemeCategory.DIET_BLOODSUGAR, List.of("당독소 봉인", "지방 연소열", "포만 유지력", "흡수 차단력"),
            ThemeCategory.SLEEP_REST, List.of("스트레스 차단", "심연 수면도", "근육 이완도", "독소 정화력")
    );

    private static final Map<ElixirGrade, ValueRange> VALUE_RANGE_BY_GRADE = Map.of(
            ElixirGrade.COMMON, new ValueRange(20, 50),
            ElixirGrade.RARE, new ValueRange(40, 70),
            ElixirGrade.EPIC, new ValueRange(60, 90),
            ElixirGrade.PRISMATIC_LEGENDARY, new ValueRange(85, 100)
    );

    private final IntSupplier percentRoll;

    @Autowired
    public StatRollService() {
        this(() -> ThreadLocalRandom.current().nextInt(100));
    }

    StatRollService(IntSupplier percentRoll) {
        this.percentRoll = percentRoll;
    }

    public Map<String, Integer> rollStats(ElixirGrade grade, ThemeCategory themeCategory) {
        List<String> statNames = STAT_NAMES_BY_THEME.get(themeCategory);
        ValueRange range = VALUE_RANGE_BY_GRADE.get(grade);

        Map<String, Integer> stats = new LinkedHashMap<>();
        for (String statName : statNames) {
            stats.put(statName, rollValue(range));
        }
        return stats;
    }

    private int rollValue(ValueRange range) {
        int percent = percentRoll.getAsInt();
        int span = range.max() - range.min();
        return range.min() + Math.round(span * (percent / 99.0f));
    }

    private record ValueRange(int min, int max) {
    }
}
