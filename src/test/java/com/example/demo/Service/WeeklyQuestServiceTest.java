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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// WeeklyQuestService.grantReward()가 팀원 쪽 IngredientCard 대신
// com.elixircodex.backend.stack.IngredientCard(Grade)를 정확히 사용하는지,
// 그리고 완료 보상이 (테마 등급 재료 1개 + commonRewardMaterialName의 Common 등급 재료 1개)
// 총 2개로 지급되고 RecipeScroll은 더 이상 지급되지 않는지 확인하는 테스트.
@ExtendWith(MockitoExtension.class)
class WeeklyQuestServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private WeeklyQuestTemplateRepository weeklyQuestTemplateRepository;
    @Mock
    private WeeklyQuestRepository weeklyQuestRepository;
    @Mock
    private IngredientCardRepository ingredientCardRepository;

    private WeeklyQuestService service() {
        return new WeeklyQuestService(userRepository, weeklyQuestTemplateRepository, weeklyQuestRepository,
                ingredientCardRepository);
    }

    private WeeklyQuestTemplate template() {
        return WeeklyQuestTemplate.builder()
                .id(1L).title("매 끼니 채소 포함해서 먹기").category(QuestCategory.BLOOD_SUGAR_DIET)
                .rewardGrade(Grade.EPIC).rewardMaterialName("녹차잎").commonRewardMaterialName("포만 이끼")
                .recipeScrollName("혈당/다이어트의 비법 레시피 스크롤").build();
    }

    @Test
    void 주간_퀘스트_완료시_템플릿에_지정된_등급으로_IngredientCard가_지급된다() {
        User user = User.builder().id(10L).email("user@example.com").build();
        WeeklyQuestTemplate template = template();
        WeeklyQuest weeklyQuest = WeeklyQuest.builder()
                .id(200L).user(user).weeklyQuestTemplate(template).status(QuestStatus.IN_PROGRESS).build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(weeklyQuestRepository.findById(200L)).thenReturn(Optional.of(weeklyQuest));
        when(ingredientCardRepository.findByOwnerIdAndNameAndGrade(10L, "녹차잎", Grade.EPIC))
                .thenReturn(Optional.empty());
        when(ingredientCardRepository.findByOwnerIdAndNameAndGrade(10L, "포만 이끼", Grade.COMMON))
                .thenReturn(Optional.empty());

        WeeklyQuestResponseDto response = service().completeWeeklyQuest("user@example.com", 200L);

        assertThat(response).isNotNull();
        ArgumentCaptor<IngredientCard> captor = ArgumentCaptor.forClass(IngredientCard.class);
        verify(ingredientCardRepository, times(2)).save(captor.capture());
        IngredientCard themeCard = captor.getAllValues().get(0);
        assertThat(themeCard.getOwnerId()).isEqualTo(10L);
        assertThat(themeCard.getName()).isEqualTo("녹차잎");
        assertThat(themeCard.getGrade()).isEqualTo(Grade.EPIC);
        assertThat(themeCard.getSourceQuestTitle()).isEqualTo("매 끼니 채소 포함해서 먹기");
    }

    @Test
    void 완료_보상으로_commonRewardMaterialName의_Common_등급_재료도_함께_지급된다() {
        User user = User.builder().id(10L).email("user@example.com").build();
        WeeklyQuestTemplate template = template();
        WeeklyQuest weeklyQuest = WeeklyQuest.builder()
                .id(200L).user(user).weeklyQuestTemplate(template).status(QuestStatus.IN_PROGRESS).build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(weeklyQuestRepository.findById(200L)).thenReturn(Optional.of(weeklyQuest));
        when(ingredientCardRepository.findByOwnerIdAndNameAndGrade(10L, "녹차잎", Grade.EPIC))
                .thenReturn(Optional.empty());
        when(ingredientCardRepository.findByOwnerIdAndNameAndGrade(10L, "포만 이끼", Grade.COMMON))
                .thenReturn(Optional.empty());

        service().completeWeeklyQuest("user@example.com", 200L);

        ArgumentCaptor<IngredientCard> captor = ArgumentCaptor.forClass(IngredientCard.class);
        verify(ingredientCardRepository, times(2)).save(captor.capture());
        List<IngredientCard> saved = captor.getAllValues();
        assertThat(saved).hasSize(2);
        IngredientCard commonCard = saved.get(1);
        assertThat(commonCard.getOwnerId()).isEqualTo(10L);
        assertThat(commonCard.getName()).isEqualTo("포만 이끼");
        assertThat(commonCard.getGrade()).isEqualTo(Grade.COMMON);
        assertThat(commonCard.getSourceQuestTitle()).isEqualTo("매 끼니 채소 포함해서 먹기");
    }

    @Test
    void 이미_보유한_재료면_새로_만들지_않고_두_재료_모두_수량만_증가한다() {
        User user = User.builder().id(10L).email("user@example.com").build();
        WeeklyQuestTemplate template = template();
        WeeklyQuest weeklyQuest = WeeklyQuest.builder()
                .id(200L).user(user).weeklyQuestTemplate(template).status(QuestStatus.IN_PROGRESS).build();
        IngredientCard existingTheme = IngredientCard.builder()
                .ownerId(10L).name("녹차잎").grade(Grade.EPIC).quantity(1).build();
        IngredientCard existingCommon = IngredientCard.builder()
                .ownerId(10L).name("포만 이끼").grade(Grade.COMMON).quantity(1).build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(weeklyQuestRepository.findById(200L)).thenReturn(Optional.of(weeklyQuest));
        when(ingredientCardRepository.findByOwnerIdAndNameAndGrade(10L, "녹차잎", Grade.EPIC))
                .thenReturn(Optional.of(existingTheme));
        when(ingredientCardRepository.findByOwnerIdAndNameAndGrade(10L, "포만 이끼", Grade.COMMON))
                .thenReturn(Optional.of(existingCommon));

        service().completeWeeklyQuest("user@example.com", 200L);

        assertThat(existingTheme.getQuantity()).isEqualTo(2);
        assertThat(existingCommon.getQuantity()).isEqualTo(2);
        verify(ingredientCardRepository, never()).save(any());
    }
}
