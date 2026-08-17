package com.example.demo.Repository;

import com.example.demo.Entity.User;
import com.example.demo.Entity.WeeklyQuest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface WeeklyQuestRepository extends JpaRepository<WeeklyQuest, Long> {

    List<WeeklyQuest> findByUserAndWeekStartDate(User user, LocalDate weekStartDate);
}
