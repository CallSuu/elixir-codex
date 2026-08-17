package com.example.demo.Entity;

import com.elixircodex.backend.stack.Grade;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklyQuestTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;

    @Enumerated(EnumType.STRING)
    private QuestCategory category;

    @Enumerated(EnumType.STRING)
    private Grade rewardGrade;

    private String rewardMaterialName;
    private String commonRewardMaterialName;
    private String recipeScrollName;
}
