package com.elixircodex.backend.wizardroom;

import com.elixircodex.backend.attendance.FurnitureReward;
import com.elixircodex.backend.attendance.FurnitureRewardRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WizardRoomServiceTest {

    @Mock
    private WizardRoomPlacementRepository placementRepository;
    @Mock
    private WizardRoomSettingsRepository settingsRepository;
    @Mock
    private FurnitureRewardRepository furnitureRewardRepository;

    private WizardRoomService service() {
        return new WizardRoomService(placementRepository, settingsRepository, furnitureRewardRepository);
    }

    @Test
    void 본인이_받은_가구면_정상적으로_배치가_저장된다() {
        FurnitureReward furniture = FurnitureReward.builder().id(1L).ownerId(1L).itemName("연속 출석 보상 상자").build();
        when(furnitureRewardRepository.findById(1L)).thenReturn(Optional.of(furniture));
        PlacementItem item = new PlacementItem(1L, 100, 200, 15, 1);

        service().updatePlacements(1L, List.of(item));

        verify(placementRepository).deleteByOwnerId(1L);
        ArgumentCaptor<List<WizardRoomPlacement>> captor = ArgumentCaptor.forClass(List.class);
        verify(placementRepository).saveAll(captor.capture());
        WizardRoomPlacement saved = captor.getValue().get(0);
        assertThat(saved.getOwnerId()).isEqualTo(1L);
        assertThat(saved.getFurnitureRewardId()).isEqualTo(1L);
        assertThat(saved.getX()).isEqualTo(100);
        assertThat(saved.getY()).isEqualTo(200);
        assertThat(saved.getRotation()).isEqualTo(15);
        assertThat(saved.getZIndex()).isEqualTo(1);
    }

    @Test
    void 본인_소유가_아닌_가구를_배치하려하면_예외를_던지고_저장하지_않는다() {
        FurnitureReward othersFurniture = FurnitureReward.builder().id(1L).ownerId(2L).itemName("연속 출석 보상 상자").build();
        when(furnitureRewardRepository.findById(1L)).thenReturn(Optional.of(othersFurniture));
        PlacementItem item = new PlacementItem(1L, 100, 200, 15, 1);

        assertThatThrownBy(() -> service().updatePlacements(1L, List.of(item)))
                .isInstanceOf(WizardRoomValidationException.class)
                .hasMessage("본인이 받은 가구만 배치할 수 있습니다");

        verify(placementRepository, never()).deleteByOwnerId(any());
        verify(placementRepository, never()).saveAll(any());
    }

    @Test
    void 존재하지_않는_가구를_배치하려하면_예외를_던진다() {
        when(furnitureRewardRepository.findById(999L)).thenReturn(Optional.empty());
        PlacementItem item = new PlacementItem(999L, 100, 200, 15, 1);

        assertThatThrownBy(() -> service().updatePlacements(1L, List.of(item)))
                .isInstanceOf(WizardRoomValidationException.class)
                .hasMessage("본인이 받은 가구만 배치할 수 있습니다");
    }

    @Test
    void 공개설정이_처음이면_새로_생성해서_저장한다() {
        when(settingsRepository.findByOwnerId(1L)).thenReturn(Optional.empty());

        service().updateVisibility(1L, true);

        ArgumentCaptor<WizardRoomSettings> captor = ArgumentCaptor.forClass(WizardRoomSettings.class);
        verify(settingsRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isNull();
        assertThat(captor.getValue().getOwnerId()).isEqualTo(1L);
        assertThat(captor.getValue().isPublic()).isTrue();
    }

    @Test
    void 공개설정이_이미_있으면_id를_유지한채_갱신한다() {
        WizardRoomSettings existing = WizardRoomSettings.builder().id(10L).ownerId(1L).isPublic(false).build();
        when(settingsRepository.findByOwnerId(1L)).thenReturn(Optional.of(existing));

        service().updateVisibility(1L, true);

        ArgumentCaptor<WizardRoomSettings> captor = ArgumentCaptor.forClass(WizardRoomSettings.class);
        verify(settingsRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(10L);
        assertThat(captor.getValue().isPublic()).isTrue();
    }

    @Test
    void 설정_자체가_없으면_공개되지_않은_방_예외를_던진다() {
        when(settingsRepository.findByOwnerId(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().getPublicRoom(2L))
                .isInstanceOf(WizardRoomValidationException.class)
                .hasMessage("공개되지 않은 방입니다");
    }

    @Test
    void isPublic이_false면_공개되지_않은_방_예외를_던진다() {
        WizardRoomSettings settings = WizardRoomSettings.builder().id(10L).ownerId(2L).isPublic(false).build();
        when(settingsRepository.findByOwnerId(2L)).thenReturn(Optional.of(settings));

        assertThatThrownBy(() -> service().getPublicRoom(2L))
                .isInstanceOf(WizardRoomValidationException.class)
                .hasMessage("공개되지 않은 방입니다");
    }

    @Test
    void 공개된_방은_요청자와_무관하게_배치_목록을_반환한다() {
        WizardRoomSettings settings = WizardRoomSettings.builder().id(10L).ownerId(2L).isPublic(true).build();
        when(settingsRepository.findByOwnerId(2L)).thenReturn(Optional.of(settings));
        WizardRoomPlacement placement = WizardRoomPlacement.builder()
                .id(1L).ownerId(2L).furnitureRewardId(5L).x(10).y(20).rotation(0).zIndex(1).build();
        when(placementRepository.findByOwnerId(2L)).thenReturn(List.of(placement));

        List<PlacementItem> result = service().getPublicRoom(2L);

        assertThat(result).containsExactly(new PlacementItem(5L, 10, 20, 0, 1));
    }
}
