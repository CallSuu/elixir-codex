package com.example.demo.Controller;

import com.example.demo.Dto.WeeklyQuestResponseDto;
import com.example.demo.Service.WeeklyQuestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/quests/weekly")
@RequiredArgsConstructor
public class WeeklyQuestController {

    private final WeeklyQuestService weeklyQuestService;

    // 이번 주 퀘스트 조회 (없으면 카테고리 기반으로 새로 부여)
    @GetMapping
    public ResponseEntity<List<WeeklyQuestResponseDto>> getWeeklyQuests(Authentication authentication) {
        return ResponseEntity.ok(weeklyQuestService.getThisWeekQuests(authentication.getName()));
    }

    // 주간 퀘스트 수동 완료 (전부 수동체크 기반)
    @PatchMapping("/{weeklyQuestId}/complete")
    public ResponseEntity<WeeklyQuestResponseDto> completeWeeklyQuest(
            Authentication authentication, @PathVariable Long weeklyQuestId) {
        return ResponseEntity.ok(weeklyQuestService.completeWeeklyQuest(authentication.getName(), weeklyQuestId));
    }
}
