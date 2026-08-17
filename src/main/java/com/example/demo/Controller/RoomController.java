package com.example.demo.Controller;

import com.example.demo.Dto.RoomCardPlacementResponseDto;
import com.example.demo.Dto.RoomPlacementRequestDto;
import com.example.demo.Service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/room")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    // 비밀 서재 배치 전체 조회
    @GetMapping
    public ResponseEntity<List<RoomCardPlacementResponseDto>> getPlacements(@RequestParam Long ownerId) {
        return ResponseEntity.ok(roomService.getPlacements(ownerId));
    }

    // 비밀 서재 배치 전체 덮어쓰기 저장
    @PutMapping
    public ResponseEntity<List<RoomCardPlacementResponseDto>> savePlacements(
            @RequestParam Long ownerId,
            @RequestBody RoomPlacementRequestDto request) {
        return ResponseEntity.ok(roomService.savePlacements(ownerId, request));
    }
}
