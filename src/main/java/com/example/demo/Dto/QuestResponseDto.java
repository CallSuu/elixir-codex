package com.example.demo.Dto;

import com.example.demo.Entity.DailyQuest;
import com.example.demo.Entity.QuestDataSource;
import com.example.demo.Entity.QuestStatus;
import lombok.Getter;

@Getter
public class QuestResponseDto {

    private final Long dailyQuestId;
    private final String title;
    private final String description;
    private final QuestDataSource dataSource;
    private final Integer targetValue;
    private final QuestStatus status;

    public QuestResponseDto(DailyQuest dailyQuest) {
        this.dailyQuestId = dailyQuest.getId();
        this.title = dailyQuest.getQuestTemplate().getTitle();
        this.description = dailyQuest.getQuestTemplate().getDescription();
        this.dataSource = dailyQuest.getQuestTemplate().getDataSource();
        this.targetValue = dailyQuest.getQuestTemplate().getTargetValue();
        this.status = dailyQuest.getStatus();
    }
}
