package com.elixircodex.backend.attendance;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

@Component
@Profile("!prod")
@RequiredArgsConstructor
public class AttendanceDummyDataRunner implements CommandLineRunner {

    private static final Long OWNER_ID_WITH_STREAK = 1L;
    private static final int STREAK_DAYS = 5;

    private final AttendanceLogRepository attendanceLogRepository;

    @Override
    public void run(String... args) {
        if (attendanceLogRepository.count() > 0) {
            return;
        }

        LocalDate yesterday = LocalDate.now().minusDays(1);

        List<AttendanceLog> logs = IntStream.rangeClosed(1, STREAK_DAYS)
                .mapToObj(streak -> AttendanceLog.builder()
                        .ownerId(OWNER_ID_WITH_STREAK)
                        .attendedDate(yesterday.minusDays(STREAK_DAYS - streak))
                        .streakAtCheckIn(streak)
                        .build())
                .toList();

        attendanceLogRepository.saveAll(logs);

        // ownerId=3은 의도적으로 출석 기록을 남기지 않음 — 첫 체크인 시나리오 테스트용
    }
}
