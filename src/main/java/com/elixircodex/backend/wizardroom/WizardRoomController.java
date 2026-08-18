package com.elixircodex.backend.wizardroom;

import com.elixircodex.backend.auth.AuthenticatedUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/wizard-room")
@RequiredArgsConstructor
public class WizardRoomController {

    private final WizardRoomService wizardRoomService;
    private final AuthenticatedUserService authenticatedUserService;

    @GetMapping
    public WizardRoomResponse getMyRoom() {
        return wizardRoomService.getMyRoom(authenticatedUserService.getCurrentUserId());
    }

    @PutMapping
    public void updatePlacements(@RequestBody WizardRoomUpdateRequest request) {
        wizardRoomService.updatePlacements(authenticatedUserService.getCurrentUserId(), request.placements());
    }

    @PutMapping("/visibility")
    public void updateVisibility(@RequestBody WizardRoomVisibilityRequest request) {
        wizardRoomService.updateVisibility(authenticatedUserService.getCurrentUserId(), request.isPublic());
    }

    @GetMapping("/{ownerId}")
    public List<PlacementItem> getPublicRoom(@PathVariable Long ownerId) {
        return wizardRoomService.getPublicRoom(ownerId);
    }
}
