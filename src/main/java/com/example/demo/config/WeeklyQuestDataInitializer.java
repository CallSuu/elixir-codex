package com.example.demo.config;

import com.elixircodex.backend.stack.Grade;
import com.example.demo.Entity.QuestCategory;
import com.example.demo.Entity.WeeklyQuestTemplate;
import com.example.demo.Repository.WeeklyQuestTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class WeeklyQuestDataInitializer implements CommandLineRunner {

    private final WeeklyQuestTemplateRepository weeklyQuestTemplateRepository;

    @Override
    public void run(String... args) {
        if (weeklyQuestTemplateRepository.count() > 0) {
            return;
        }

        List<WeeklyQuestTemplate> pool = List.of(
                // 혈당/다이어트 (홀쭉 열매, 바나바잎, 녹차잎 / 공통 재료: 포만 이끼)
                WeeklyQuestTemplate.builder().title("주 5회 10,000보 걷기").description("이번 주 5일 이상 10,000보 이상 걷기").category(QuestCategory.BLOOD_SUGAR_DIET).rewardGrade(Grade.RARE).rewardMaterialName("홀쭉 열매").commonRewardMaterialName("포만 이끼").recipeScrollName("혈당/다이어트의 비법 레시피 스크롤").build(),
                WeeklyQuestTemplate.builder().title("주 4회 당류 제한 식단 유지하기").description("이번 주 4일 이상 당류 섭취를 제한한 식단 지키기").category(QuestCategory.BLOOD_SUGAR_DIET).rewardGrade(Grade.RARE).rewardMaterialName("바나바잎").commonRewardMaterialName("포만 이끼").recipeScrollName("혈당/다이어트의 비법 레시피 스크롤").build(),
                WeeklyQuestTemplate.builder().title("매 끼니 채소 포함해서 먹기").description("이번 주 5일 이상 매 끼니 채소를 포함해서 먹기").category(QuestCategory.BLOOD_SUGAR_DIET).rewardGrade(Grade.EPIC).rewardMaterialName("녹차잎").commonRewardMaterialName("포만 이끼").recipeScrollName("혈당/다이어트의 비법 레시피 스크롤").build(),

                // 피부/항산화 (이슬 한 방울, 탱탱 젤리, 황금 레몬 / 공통 재료: 백옥 진주)
                WeeklyQuestTemplate.builder().title("매일 자외선 차단제 바르기").description("이번 주 7일 모두 자외선 차단제 바르기").category(QuestCategory.SKIN_ANTIOXIDANT).rewardGrade(Grade.RARE).rewardMaterialName("이슬 한 방울").commonRewardMaterialName("백옥 진주").recipeScrollName("피부/항산화의 비법 레시피 스크롤").build(),
                WeeklyQuestTemplate.builder().title("주 3회 마스크팩 하기").description("이번 주 3회 이상 마스크팩 하기").category(QuestCategory.SKIN_ANTIOXIDANT).rewardGrade(Grade.RARE).rewardMaterialName("탱탱 젤리").commonRewardMaterialName("백옥 진주").recipeScrollName("피부/항산화의 비법 레시피 스크롤").build(),
                WeeklyQuestTemplate.builder().title("밤 10시 이전 세안 후 취침하기").description("이번 주 5일 이상 밤 10시 이전 세안 후 취침하기").category(QuestCategory.SKIN_ANTIOXIDANT).rewardGrade(Grade.EPIC).rewardMaterialName("황금 레몬").commonRewardMaterialName("백옥 진주").recipeScrollName("피부/항산화의 비법 레시피 스크롤").build(),

                // 피로/에너지 (활력초, 천년 뿌리, 마룡 뿔 / 공통 재료: 심장 태엽)
                WeeklyQuestTemplate.builder().title("주 3회 근력운동 30분 이상").description("이번 주 3회 이상 근력운동 30분 이상 하기").category(QuestCategory.FATIGUE_ENERGY).rewardGrade(Grade.RARE).rewardMaterialName("활력초").commonRewardMaterialName("심장 태엽").recipeScrollName("피로/에너지의 비법 레시피 스크롤").build(),
                WeeklyQuestTemplate.builder().title("주 4회 유산소 20분 이상").description("이번 주 4회 이상 유산소 운동 20분 이상 하기").category(QuestCategory.FATIGUE_ENERGY).rewardGrade(Grade.RARE).rewardMaterialName("천년 뿌리").commonRewardMaterialName("심장 태엽").recipeScrollName("피로/에너지의 비법 레시피 스크롤").build(),
                WeeklyQuestTemplate.builder().title("매일 물 2L 마시기").description("이번 주 7일 모두 물 2L 이상 마시기").category(QuestCategory.FATIGUE_ENERGY).rewardGrade(Grade.EPIC).rewardMaterialName("마룡 뿔").commonRewardMaterialName("심장 태엽").recipeScrollName("피로/에너지의 비법 레시피 스크롤").build(),

                // 수면/휴식 (평온초, 안정석, 해독 엉겅퀴 / 공통 재료는 같은 3개를 서로 겹치지 않게 순환 배정)
                WeeklyQuestTemplate.builder().title("5일 연속 동일 시간 수면하기").description("이번 주 5일 연속 동일한 시간에 잠들기").category(QuestCategory.SLEEP_REST).rewardGrade(Grade.RARE).rewardMaterialName("평온초").commonRewardMaterialName("안정석").recipeScrollName("수면/휴식의 비법 레시피 스크롤").build(),
                WeeklyQuestTemplate.builder().title("주 5회 7시간 이상 수면하기").description("이번 주 5일 이상 7시간 이상 수면하기").category(QuestCategory.SLEEP_REST).rewardGrade(Grade.RARE).rewardMaterialName("안정석").commonRewardMaterialName("해독 엉겅퀴").recipeScrollName("수면/휴식의 비법 레시피 스크롤").build(),
                WeeklyQuestTemplate.builder().title("주 4회 낮잠 20분 이내로 제한하기").description("이번 주 4일 이상 낮잠을 20분 이내로 제한하기").category(QuestCategory.SLEEP_REST).rewardGrade(Grade.EPIC).rewardMaterialName("해독 엉겅퀴").commonRewardMaterialName("평온초").recipeScrollName("수면/휴식의 비법 레시피 스크롤").build()
        );

        weeklyQuestTemplateRepository.saveAll(pool);
    }
}
