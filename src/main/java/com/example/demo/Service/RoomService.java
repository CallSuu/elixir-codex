package com.example.demo.Service;

import com.example.demo.Dto.RoomCardPlacementResponseDto;
import com.example.demo.Dto.RoomPlacementRequestDto;
import com.example.demo.Entity.RoomCardPlacement;
import com.example.demo.Repository.RoomCardPlacementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomCardPlacementRepository roomCardPlacementRepository;

    @Transactional(readOnly = true)
    public List<RoomCardPlacementResponseDto> getPlacements(Long ownerId) {
        return roomCardPlacementRepository.findByOwnerId(ownerId).stream()
                .map(RoomCardPlacementResponseDto::new)
                .toList();
    }

    // 캔버스 저장 시점의 배치 목록 전체로 덮어쓴다 (부분 수정 API 없음)
    @Transactional
    public List<RoomCardPlacementResponseDto> savePlacements(Long ownerId, RoomPlacementRequestDto request) {
        roomCardPlacementRepository.deleteByOwnerId(ownerId);

        List<RoomPlacementRequestDto.PlacementItem> items = request.getPlacements() != null
                ? request.getPlacements()
                : List.of();

        List<RoomCardPlacement> placements = items.stream()
                .map(item -> RoomCardPlacement.builder()
                        .ownerId(ownerId)
                        .elixirCardId(item.getElixirCardId())
                        .x(item.getX())
                        .y(item.getY())
                        .rotation(item.getRotation())
                        .zIndex(item.getZIndex())
                        .build())
                .toList();

        roomCardPlacementRepository.saveAll(placements);

        return placements.stream()
                .map(RoomCardPlacementResponseDto::new)
                .toList();
    }
}
