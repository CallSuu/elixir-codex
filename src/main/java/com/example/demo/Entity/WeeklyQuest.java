package com.example.demo.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklyQuest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "weekly_quest_template_id")
    private WeeklyQuestTemplate weeklyQuestTemplate;

    private LocalDate weekStartDate;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private QuestStatus status = QuestStatus.IN_PROGRESS;

    public void complete() {
        this.status = QuestStatus.COMPLETED;
    }
}
