package com.elixircodex.backend.wizardroom;

import com.elixircodex.backend.auth.AuthenticatedUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WizardRoomControllerTest {

    @Mock
    private WizardRoomService wizardRoomService;
    @Mock
    private AuthenticatedUserService authenticatedUserService;

    private WizardRoomController controller;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        controller = new WizardRoomController(wizardRoomService, authenticatedUserService);
    }

    @Test
    void 내_방_조회는_서비스_결과를_그대로_반환한다() {
        when(authenticatedUserService.getCurrentUserId()).thenReturn(1L);
        WizardRoomResponse expected = new WizardRoomResponse(List.of(new PlacementItem(1L, 100, 200, 15, 1)), true);
        when(wizardRoomService.getMyRoom(1L)).thenReturn(expected);

        WizardRoomResponse response = controller.getMyRoom();

        assertThat(response).isEqualTo(expected);
    }

    @Test
    void 배치_갱신_요청은_서비스로_그대로_위임된다() {
        when(authenticatedUserService.getCurrentUserId()).thenReturn(1L);
        List<PlacementItem> placements = List.of(new PlacementItem(1L, 100, 200, 15, 1));
        WizardRoomUpdateRequest request = new WizardRoomUpdateRequest(placements);

        controller.updatePlacements(request);

        verify(wizardRoomService).updatePlacements(1L, placements);
    }

    @Test
    void 공개설정_갱신_요청은_서비스로_그대로_위임된다() {
        when(authenticatedUserService.getCurrentUserId()).thenReturn(1L);
        WizardRoomVisibilityRequest request = new WizardRoomVisibilityRequest(true);

        controller.updateVisibility(request);

        verify(wizardRoomService).updateVisibility(1L, true);
    }

    @Test
    void 타인_공개방_조회는_서비스_결과를_그대로_반환한다() {
        List<PlacementItem> expected = List.of(new PlacementItem(5L, 10, 20, 0, 1));
        when(wizardRoomService.getPublicRoom(2L)).thenReturn(expected);

        List<PlacementItem> response = controller.getPublicRoom(2L);

        assertThat(response).isEqualTo(expected);
    }
}
