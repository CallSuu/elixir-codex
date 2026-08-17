package com.example.demo.Service;

import com.elixircodex.backend.stack.Grade;
import com.elixircodex.backend.stack.IngredientCard;
import com.elixircodex.backend.stack.IngredientCardRepository;
import com.example.demo.Dto.QuestResponseDto;
import com.example.demo.Entity.DailyQuest;
import com.example.demo.Entity.QuestDataSource;
import com.example.demo.Entity.QuestStatus;
import com.example.demo.Entity.QuestTemplate;
import com.example.demo.Entity.User;
import com.example.demo.Repository.DailyQuestRepository;
import com.example.demo.Repository.QuestTemplateRepository;
import com.example.demo.Repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// QuestService.grantReward()가 팀원 쪽 IngredientCard 대신
// com.elixircodex.backend.stack.IngredientCard(Grade)를 정확히 사용하는지 확인하는 테스트.
@ExtendWith(MockitoExtension.class)
class QuestServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private QuestTemplateRepository questTemplateRepository;
    @Mock
    private DailyQuestRepository dailyQuestRepository;
    @Mock
    private IngredientCardRepository ingredientCardRepository;

    private QuestService service() {
        return new QuestService(userRepository, questTemplateRepository, dailyQuestRepository, ingredientCardRepository);
    }

    @Test
    void 수동_퀘스트_완료시_보상으로_ownerId를_가진_IngredientCard가_지급된다() {
        User user = User.builder().id(10L).email("user@example.com").build();
        QuestTemplate template = QuestTemplate.builder()
                .id(1L).title("물 1.5L 마시기").dataSource(QuestDataSource.MANUAL)
                .rewardMaterialName("탱탱 젤리").build();
        DailyQuest dailyQuest = DailyQuest.builder()
                .id(100L).user(user).questTemplate(template).status(QuestStatus.IN_PROGRESS).build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(dailyQuestRepository.findById(100L)).thenReturn(Optional.of(dailyQuest));
        when(ingredientCardRepository.findByOwnerIdAndNameAndGrade(eq(10L), anyString(), any(Grade.class)))
                .thenReturn(Optional.empty());

        QuestResponseDto response = service().completeManualQuest("user@example.com", 100L);

        assertThat(response).isNotNull();
        ArgumentCaptor<IngredientCard> captor = ArgumentCaptor.forClass(IngredientCard.class);
        verify(ingredientCardRepository).save(captor.capture());
        IngredientCard saved = captor.getValue();
        assertThat(saved.getOwnerId()).isEqualTo(10L);
        assertThat(saved.getName()).isEqualTo("탱탱 젤리");
        assertThat(saved.getSourceQuestTitle()).isEqualTo("물 1.5L 마시기");
        assertThat(saved.getGrade()).isIn(Grade.COMMON, Grade.RARE);
    }

    @Test
    void 이미_보유한_재료면_새로_만들지_않고_수량만_증가한다() {
        User user = User.builder().id(10L).email("user@example.com").build();
        QuestTemplate template = QuestTemplate.builder()
                .id(1L).title("물 1.5L 마시기").dataSource(QuestDataSource.MANUAL)
                .rewardMaterialName("탱탱 젤리").build();
        DailyQuest dailyQuest = DailyQuest.builder()
                .id(100L).user(user).questTemplate(template).status(QuestStatus.IN_PROGRESS).build();
        IngredientCard existing = IngredientCard.builder()
                .ownerId(10L).name("탱탱 젤리").grade(Grade.COMMON).quantity(1).build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(dailyQuestRepository.findById(100L)).thenReturn(Optional.of(dailyQuest));
        when(ingredientCardRepository.findByOwnerIdAndNameAndGrade(eq(10L), anyString(), any(Grade.class)))
                .thenReturn(Optional.of(existing));

        service().completeManualQuest("user@example.com", 100L);

        assertThat(existing.getQuantity()).isEqualTo(2);
        verify(ingredientCardRepository, never()).save(any());
    }
}
