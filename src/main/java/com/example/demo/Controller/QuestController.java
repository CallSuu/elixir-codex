package com.example.demo.Controller;

import com.example.demo.Dto.HealthSyncRequestDto;
import com.example.demo.Dto.QuestResponseDto;
import com.example.demo.Service.QuestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/quests")
@RequiredArgsConstructor
public class QuestController {

    private final QuestService questService;

    // 오늘의 일일 퀘스트 조회 (없으면 새로 부여)
    @GetMapping("/daily")
    public ResponseEntity<List<QuestResponseDto>> getDailyQuests(Authentication authentication) {
        return ResponseEntity.ok(questService.getTodayQuests(authentication.getName()));
    }

    // Health 데이터(걸음수/수면시간) 동기화 → 기준 충족 퀘스트 자동 완료 처리
    @PostMapping("/health-sync")
    public ResponseEntity<List<QuestResponseDto>> syncHealthData(
            Authentication authentication, @RequestBody HealthSyncRequestDto request) {
        return ResponseEntity.ok(questService.syncHealthData(authentication.getName(), request));
    }

    // 수동 체크형(MANUAL) 퀘스트를 유저가 직접 완료 처리
    @PatchMapping("/{dailyQuestId}/complete")
    public ResponseEntity<QuestResponseDto> completeManualQuest(
            Authentication authentication, @PathVariable Long dailyQuestId) {
        return ResponseEntity.ok(questService.completeManualQuest(authentication.getName(), dailyQuestId));
    }
}
