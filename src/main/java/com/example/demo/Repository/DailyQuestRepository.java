package com.example.demo.Repository;

import com.example.demo.Entity.DailyQuest;
import com.example.demo.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface DailyQuestRepository extends JpaRepository<DailyQuest, Long> {

    List<DailyQuest> findByUserAndAssignedDate(User user, LocalDate assignedDate);
}
