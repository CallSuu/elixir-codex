package com.elixircodex.backend.attendance;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private static final String REWARD_ITEM_NAME = "연속 출석 보상 상자";
    private static final int REWARD_STREAK_INTERVAL = 7;

    private final AttendanceLogRepository attendanceLogRepository;
    private final FurnitureRewardRepository furnitureRewardRepository;

    public AttendanceCheckResponse checkIn(Long ownerId) {
        LocalDate today = LocalDate.now();
        if (attendanceLogRepository.findByOwnerIdAndAttendedDate(ownerId, today).isPresent()) {
            throw new AttendanceValidationException("오늘 이미 출석체크를 완료했습니다");
        }

        int streak = attendanceLogRepository.findFirstByOwnerIdOrderByAttendedDateDesc(ownerId)
                .filter(log -> log.getAttendedDate().equals(today.minusDays(1)))
                .map(log -> log.getStreakAtCheckIn() + 1)
                .orElse(1);

        attendanceLogRepository.save(AttendanceLog.builder()
                .ownerId(ownerId)
                .attendedDate(today)
                .streakAtCheckIn(streak)
                .build());

        boolean rewardGranted = streak % REWARD_STREAK_INTERVAL == 0;
        if (rewardGranted) {
            furnitureRewardRepository.save(FurnitureReward.builder()
                    .ownerId(ownerId)
                    .itemName(REWARD_ITEM_NAME)
                    .build());
        }

        return new AttendanceCheckResponse(streak, rewardGranted, rewardGranted ? REWARD_ITEM_NAME : null);
    }

    public AttendanceStatusResponse getStatus(Long ownerId) {
        int currentStreak = attendanceLogRepository.findFirstByOwnerIdOrderByAttendedDateDesc(ownerId)
                .map(AttendanceLog::getStreakAtCheckIn)
                .orElse(0);

        List<AttendanceStatusResponse.FurnitureRewardSummary> rewards = furnitureRewardRepository.findByOwnerId(ownerId)
                .stream()
                .map(reward -> new AttendanceStatusResponse.FurnitureRewardSummary(
                        reward.getId(), reward.getItemName(), reward.getGrantedAt()))
                .toList();

        return new AttendanceStatusResponse(currentStreak, rewards);
    }
}
