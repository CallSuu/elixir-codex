package com.example.demo.Service;

import com.elixircodex.backend.stack.Grade;
import com.elixircodex.backend.stack.IngredientCard;
import com.elixircodex.backend.stack.IngredientCardRepository;
import com.example.demo.Dto.WeeklyQuestResponseDto;
import com.example.demo.Entity.QuestCategory;
import com.example.demo.Entity.QuestStatus;
import com.example.demo.Entity.User;
import com.example.demo.Entity.WeeklyQuest;
import com.example.demo.Entity.WeeklyQuestTemplate;
import com.example.demo.Repository.UserRepository;
import com.example.demo.Repository.WeeklyQuestRepository;
import com.example.demo.Repository.WeeklyQuestTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WeeklyQuestService {

    private static final int WEEKLY_QUEST_COUNT = 3;

    private final UserRepository userRepository;
    private final WeeklyQuestTemplateRepository weeklyQuestTemplateRepository;
    private final WeeklyQuestRepository weeklyQuestRepository;
    private final IngredientCardRepository ingredientCardRepository;

    @Transactional
    public List<WeeklyQuestResponseDto> getThisWeekQuests(String email) {
        User user = findUser(email);
        List<WeeklyQuest> weeklyQuests = getOrAssignThisWeekQuests(user);
        return weeklyQuests.stream().map(WeeklyQuestResponseDto::new).toList();
    }

    @Transactional
    public WeeklyQuestResponseDto completeWeeklyQuest(String email, Long weeklyQuestId) {
        User user = findUser(email);
        WeeklyQuest weeklyQuest = weeklyQuestRepository.findById(weeklyQuestId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주간 퀘스트입니다."));

        if (!weeklyQuest.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("본인의 퀘스트만 완료 처리할 수 있습니다.");
        }
        if (weeklyQuest.getStatus() == QuestStatus.COMPLETED) {
            throw new IllegalArgumentException("이미 완료된 퀘스트입니다.");
        }

        weeklyQuest.complete();
        grantReward(user, weeklyQuest.getWeeklyQuestTemplate());
        return new WeeklyQuestResponseDto(weeklyQuest);
    }

    private void grantReward(User user, WeeklyQuestTemplate template) {
        grantIngredientCard(user, template.getRewardMaterialName(), template.getRewardGrade(), template.getTitle());
        grantIngredientCard(user, template.getCommonRewardMaterialName(), Grade.COMMON, template.getTitle());
    }

    private void grantIngredientCard(User user, String materialName, Grade grade, String sourceQuestTitle) {
        ingredientCardRepository.findByOwnerIdAndNameAndGrade(user.getId(), materialName, grade)
                .ifPresentOrElse(
                        IngredientCard::increaseQuantity,
                        () -> ingredientCardRepository.save(IngredientCard.builder()
                                .ownerId(user.getId())
                                .name(materialName)
                                .grade(grade)
                                .sourceQuestTitle(sourceQuestTitle)
                                .build())
                );
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 이메일입니다."));
    }

    private List<WeeklyQuest> getOrAssignThisWeekQuests(User user) {
        LocalDate weekStartDate = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<WeeklyQuest> weeklyQuests = weeklyQuestRepository.findByUserAndWeekStartDate(user, weekStartDate);

        if (weeklyQuests.isEmpty()) {
            weeklyQuests = assignWeeklyQuests(user, weekStartDate);
        }

        return weeklyQuests;
    }

    private List<WeeklyQuest> assignWeeklyQuests(User user, LocalDate weekStartDate) {
        QuestCategory category = QuestCategory.fromLabel(user.getSelectedCategory());
        List<WeeklyQuestTemplate> candidates = new ArrayList<>(weeklyQuestTemplateRepository.findByCategory(category));
        Collections.shuffle(candidates);

        int pickCount = Math.min(WEEKLY_QUEST_COUNT, candidates.size());
        List<WeeklyQuest> assigned = candidates.subList(0, pickCount).stream()
                .map(template -> WeeklyQuest.builder()
                        .user(user)
                        .weeklyQuestTemplate(template)
                        .weekStartDate(weekStartDate)
                        .build())
                .toList();

        return weeklyQuestRepository.saveAll(assigned);
    }
}
