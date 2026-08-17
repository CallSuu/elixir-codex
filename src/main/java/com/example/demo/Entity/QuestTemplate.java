package com.example.demo.Entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;

    @Enumerated(EnumType.STRING)
    private QuestDataSource dataSource;

    private Integer targetValue;

    // 완료 보상 재료 이름 (기획팀 확정 15종 재료 목록 중 하나, QUEST_API_SPEC.md 참고)
    private String rewardMaterialName;
}
