package com.elixircodex.backend.wizardroom;

import java.util.List;

public record WizardRoomResponse(List<PlacementItem> placements, boolean isPublic) {
}
