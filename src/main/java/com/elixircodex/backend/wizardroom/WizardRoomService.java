package com.elixircodex.backend.wizardroom;

import com.elixircodex.backend.attendance.FurnitureRewardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WizardRoomService {

    private final WizardRoomPlacementRepository placementRepository;
    private final WizardRoomSettingsRepository settingsRepository;
    private final FurnitureRewardRepository furnitureRewardRepository;

    public WizardRoomResponse getMyRoom(Long ownerId) {
        List<PlacementItem> placements = toItems(placementRepository.findByOwnerId(ownerId));
        boolean isPublic = settingsRepository.findByOwnerId(ownerId)
                .map(WizardRoomSettings::isPublic)
                .orElse(false);
        return new WizardRoomResponse(placements, isPublic);
    }

    @Transactional
    public void updatePlacements(Long ownerId, List<PlacementItem> placements) {
        for (PlacementItem item : placements) {
            boolean ownsFurniture = furnitureRewardRepository.findById(item.furnitureRewardId())
                    .map(furniture -> furniture.getOwnerId().equals(ownerId))
                    .orElse(false);
            if (!ownsFurniture) {
                throw new WizardRoomValidationException("본인이 받은 가구만 배치할 수 있습니다");
            }
        }

        placementRepository.deleteByOwnerId(ownerId);
        List<WizardRoomPlacement> entities = placements.stream()
                .map(item -> WizardRoomPlacement.builder()
                        .ownerId(ownerId)
                        .furnitureRewardId(item.furnitureRewardId())
                        .x(item.x())
                        .y(item.y())
                        .rotation(item.rotation())
                        .zIndex(item.zIndex())
                        .build())
                .toList();
        placementRepository.saveAll(entities);
    }

    public void updateVisibility(Long ownerId, boolean isPublic) {
        Long existingId = settingsRepository.findByOwnerId(ownerId).map(WizardRoomSettings::getId).orElse(null);
        settingsRepository.save(WizardRoomSettings.builder()
                .id(existingId)
                .ownerId(ownerId)
                .isPublic(isPublic)
                .build());
    }

    public List<PlacementItem> getPublicRoom(Long targetOwnerId) {
        WizardRoomSettings settings = settingsRepository.findByOwnerId(targetOwnerId)
                .orElseThrow(() -> new WizardRoomValidationException("공개되지 않은 방입니다"));

        if (!settings.isPublic()) {
            throw new WizardRoomValidationException("공개되지 않은 방입니다");
        }

        return toItems(placementRepository.findByOwnerId(targetOwnerId));
    }

    private List<PlacementItem> toItems(List<WizardRoomPlacement> placements) {
        return placements.stream()
                .map(p -> new PlacementItem(p.getFurnitureRewardId(), p.getX(), p.getY(), p.getRotation(), p.getZIndex()))
                .toList();
    }
}
