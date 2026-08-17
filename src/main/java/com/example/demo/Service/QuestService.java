package com.example.demo.Service;

import com.elixircodex.backend.stack.Grade;
import com.elixircodex.backend.stack.IngredientCard;
import com.elixircodex.backend.stack.IngredientCardRepository;
import com.example.demo.Dto.HealthSyncRequestDto;
import com.example.demo.Dto.QuestResponseDto;
import com.example.demo.Entity.DailyQuest;
import com.example.demo.Entity.QuestDataSource;
import com.example.demo.Entity.QuestStatus;
import com.example.demo.Entity.QuestTemplate;
import com.example.demo.Entity.User;
import com.example.demo.Repository.DailyQuestRepository;
import com.example.demo.Repository.QuestTemplateRepository;
import com.example.demo.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class QuestService {

    private static final int DAILY_QUEST_COUNT = 5;
    // QST-04: 일일 퀘스트는 일반~희귀 등급 보상 — 15% 확률로 희귀 등급 승급
    private static final double RARE_UPGRADE_CHANCE = 0.15;

    private final UserRepository userRepository;
    private final QuestTemplateRepository questTemplateRepository;
    private final DailyQuestRepository dailyQuestRepository;
    private final IngredientCardRepository ingredientCardRepository;

    @Transactional
    public List<QuestResponseDto> getTodayQuests(String email) {
        User user = findUser(email);
        List<DailyQuest> dailyQuests = getOrAssignTodayQuests(user);
        return dailyQuests.stream().map(QuestResponseDto::new).toList();
    }

    @Transactional
    public List<QuestResponseDto> syncHealthData(String email, HealthSyncRequestDto request) {
        User user = findUser(email);
        List<DailyQuest> dailyQuests = getOrAssignTodayQuests(user);

        for (DailyQuest dailyQuest : dailyQuests) {
            if (dailyQuest.getStatus() == QuestStatus.COMPLETED) {
                continue;
            }

            QuestTemplate template = dailyQuest.getQuestTemplate();
            boolean achieved = switch (template.getDataSource()) {
                case STEPS -> request.getSteps() != null && request.getSteps() >= template.getTargetValue();
                case SLEEP -> request.getSleepHours() != null && request.getSleepHours() >= template.getTargetValue();
                case MANUAL -> false;
            };

            if (achieved) {
                dailyQuest.complete();
                grantReward(user, dailyQuest.getQuestTemplate());
            }
        }

        return dailyQuests.stream().map(QuestResponseDto::new).toList();
    }

    @Transactional
    public QuestResponseDto completeManualQuest(String email, Long dailyQuestId) {
        User user = findUser(email);
        DailyQuest dailyQuest = dailyQuestRepository.findById(dailyQuestId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 퀘스트입니다."));

        if (!dailyQuest.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("본인의 퀘스트만 완료 처리할 수 있습니다.");
        }
        if (dailyQuest.getQuestTemplate().getDataSource() != QuestDataSource.MANUAL) {
            throw new IllegalArgumentException("자동 판정 대상 퀘스트는 수동으로 완료할 수 없습니다.");
        }
        if (dailyQuest.getStatus() == QuestStatus.COMPLETED) {
            throw new IllegalArgumentException("이미 완료된 퀘스트입니다.");
        }

        dailyQuest.complete();
        grantReward(user, dailyQuest.getQuestTemplate());
        return new QuestResponseDto(dailyQuest);
    }

    // 일일 퀘스트 완료 보상: 일반 등급 재료 1개 지급, 15% 확률로 희귀 등급 승급 (QST-04: 일일=일반~희귀)
    private void grantReward(User user, QuestTemplate template) {
        Grade grade = ThreadLocalRandom.current().nextDouble() < RARE_UPGRADE_CHANCE
                ? Grade.RARE
                : Grade.COMMON;

        ingredientCardRepository.findByOwnerIdAndNameAndGrade(user.getId(), template.getRewardMaterialName(), grade)
                .ifPresentOrElse(
                        IngredientCard::increaseQuantity,
                        () -> ingredientCardRepository.save(IngredientCard.builder()
                                .ownerId(user.getId())
                                .name(template.getRewardMaterialName())
                                .grade(grade)
                                .sourceQuestTitle(template.getTitle())
                                .build())
                );
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 이메일입니다."));
    }

    private List<DailyQuest> getOrAssignTodayQuests(User user) {
        LocalDate today = LocalDate.now();
        List<DailyQuest> dailyQuests = dailyQuestRepository.findByUserAndAssignedDate(user, today);

        if (dailyQuests.isEmpty()) {
            dailyQuests = assignDailyQuests(user, today);
        }

        return dailyQuests;
    }

    private List<DailyQuest> assignDailyQuests(User user, LocalDate today) {
        List<QuestTemplate> candidates = user.isHealthDataEnabled()
                ? questTemplateRepository.findAll()
                : questTemplateRepository.findByDataSource(QuestDataSource.MANUAL);

        List<QuestTemplate> shuffled = new ArrayList<>(candidates);
        Collections.shuffle(shuffled);

        // STEPS/SLEEP처럼 같은 지표를 기준만 다르게 재는 미션이 하루에 중복 배정되지 않도록 데이터소스당 1개로 제한
        Set<QuestDataSource> usedAutoSources = EnumSet.noneOf(QuestDataSource.class);
        List<QuestTemplate> picked = new ArrayList<>();

        for (QuestTemplate template : shuffled) {
            if (picked.size() >= DAILY_QUEST_COUNT) {
                break;
            }
            if (template.getDataSource() != QuestDataSource.MANUAL
                    && !usedAutoSources.add(template.getDataSource())) {
                continue;
            }
            picked.add(template);
        }

        List<DailyQuest> assigned = picked.stream()
                .map(template -> DailyQuest.builder()
                        .user(user)
                        .questTemplate(template)
                        .assignedDate(today)
                        .build())
                .toList();

        return dailyQuestRepository.saveAll(assigned);
    }
}
