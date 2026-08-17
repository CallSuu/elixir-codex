package com.elixircodex.backend.attendance;

public record AttendanceCheckResponse(int currentStreak, boolean rewardGranted, String rewardItemName) {
}
