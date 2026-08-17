package com.elixircodex.backend.attendance;

import java.time.LocalDateTime;
import java.util.List;

public record AttendanceStatusResponse(int currentStreak, List<FurnitureRewardSummary> rewards) {
    public record FurnitureRewardSummary(Long id, String itemName, LocalDateTime grantedAt) {
    }
}
