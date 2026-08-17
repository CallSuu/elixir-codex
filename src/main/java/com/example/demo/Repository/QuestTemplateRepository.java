package com.example.demo.Repository;

import com.example.demo.Entity.QuestDataSource;
import com.example.demo.Entity.QuestTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestTemplateRepository extends JpaRepository<QuestTemplate, Long> {

    List<QuestTemplate> findByDataSource(QuestDataSource dataSource);

    List<QuestTemplate> findByDataSourceNot(QuestDataSource dataSource);
}
