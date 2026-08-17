package com.example.demo.Repository;

import com.example.demo.Entity.QuestCategory;
import com.example.demo.Entity.WeeklyQuestTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WeeklyQuestTemplateRepository extends JpaRepository<WeeklyQuestTemplate, Long> {

    List<WeeklyQuestTemplate> findByCategory(QuestCategory category);
}
