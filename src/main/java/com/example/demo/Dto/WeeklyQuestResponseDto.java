package com.example.demo.Dto;

import com.example.demo.Entity.QuestCategory;
import com.example.demo.Entity.QuestStatus;
import com.example.demo.Entity.WeeklyQuest;
import lombok.Getter;

@Getter
public class WeeklyQuestResponseDto {

    private final Long weeklyQuestId;
    private final String title;
    private final String description;
    private final QuestCategory category;
    private final QuestStatus status;

    public WeeklyQuestResponseDto(WeeklyQuest weeklyQuest) {
        this.weeklyQuestId = weeklyQuest.getId();
        this.title = weeklyQuest.getWeeklyQuestTemplate().getTitle();
        this.description = weeklyQuest.getWeeklyQuestTemplate().getDescription();
        this.category = weeklyQuest.getWeeklyQuestTemplate().getCategory();
        this.status = weeklyQuest.getStatus();
    }
}
