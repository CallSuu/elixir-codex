package com.elixircodex.backend.attendance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private AttendanceLogRepository attendanceLogRepository;

    @Mock
    private FurnitureRewardRepository furnitureRewardRepository;

    private AttendanceService service;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        service = new AttendanceService(attendanceLogRepository, furnitureRewardRepository);
    }

    @Test
    void 오늘_이미_체크했으면_예외를_던진다() {
        LocalDate today = LocalDate.now();
        when(attendanceLogRepository.findByOwnerIdAndAttendedDate(1L, today))
                .thenReturn(Optional.of(AttendanceLog.builder().ownerId(1L).attendedDate(today).streakAtCheckIn(1).build()));

        assertThatThrownBy(() -> service.checkIn(1L))
                .isInstanceOf(AttendanceValidationException.class)
                .hasMessage("오늘 이미 출석체크를 완료했습니다");
    }

    @Test
    void 어제_출석했으면_연속일수가_증가한다() {
        LocalDate today = LocalDate.now();
        when(attendanceLogRepository.findByOwnerIdAndAttendedDate(1L, today)).thenReturn(Optional.empty());
        AttendanceLog yesterdayLog = AttendanceLog.builder()
                .ownerId(1L).attendedDate(today.minusDays(1)).streakAtCheckIn(5).build();
        when(attendanceLogRepository.findFirstByOwnerIdOrderByAttendedDateDesc(1L))
                .thenReturn(Optional.of(yesterdayLog));

        AttendanceCheckResponse response = service.checkIn(1L);

        assertThat(response.currentStreak()).isEqualTo(6);
        assertThat(response.rewardGranted()).isFalse();
        assertThat(response.rewardItemName()).isNull();

        ArgumentCaptor<AttendanceLog> captor = ArgumentCaptor.forClass(AttendanceLog.class);
        verify(attendanceLogRepository).save(captor.capture());
        assertThat(captor.getValue().getStreakAtCheckIn()).isEqualTo(6);
        assertThat(captor.getValue().getAttendedDate()).isEqualTo(today);
    }

    @Test
    void 하루_이상_공백이_있으면_연속일수가_1로_초기화된다() {
        LocalDate today = LocalDate.now();
        when(attendanceLogRepository.findByOwnerIdAndAttendedDate(1L, today)).thenReturn(Optional.empty());
        AttendanceLog oldLog = AttendanceLog.builder()
                .ownerId(1L).attendedDate(today.minusDays(3)).streakAtCheckIn(7).build();
        when(attendanceLogRepository.findFirstByOwnerIdOrderByAttendedDateDesc(1L))
                .thenReturn(Optional.of(oldLog));

        AttendanceCheckResponse response = service.checkIn(1L);

        assertThat(response.currentStreak()).isEqualTo(1);
        assertThat(response.rewardGranted()).isFalse();
    }

    @Test
    void 첫_체크인이면_연속일수는_1이다() {
        LocalDate today = LocalDate.now();
        when(attendanceLogRepository.findByOwnerIdAndAttendedDate(3L, today)).thenReturn(Optional.empty());
        when(attendanceLogRepository.findFirstByOwnerIdOrderByAttendedDateDesc(3L)).thenReturn(Optional.empty());

        AttendanceCheckResponse response = service.checkIn(3L);

        assertThat(response.currentStreak()).isEqualTo(1);
        assertThat(response.rewardGranted()).isFalse();
        verify(furnitureRewardRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void 연속7일이면_가구보상이_지급된다() {
        LocalDate today = LocalDate.now();
        when(attendanceLogRepository.findByOwnerIdAndAttendedDate(1L, today)).thenReturn(Optional.empty());
        AttendanceLog yesterdayLog = AttendanceLog.builder()
                .ownerId(1L).attendedDate(today.minusDays(1)).streakAtCheckIn(6).build();
        when(attendanceLogRepository.findFirstByOwnerIdOrderByAttendedDateDesc(1L))
                .thenReturn(Optional.of(yesterdayLog));

        AttendanceCheckResponse response = service.checkIn(1L);

        assertThat(response.currentStreak()).isEqualTo(7);
        assertThat(response.rewardGranted()).isTrue();
        assertThat(response.rewardItemName()).isEqualTo("연속 출석 보상 상자");

        ArgumentCaptor<FurnitureReward> captor = ArgumentCaptor.forClass(FurnitureReward.class);
        verify(furnitureRewardRepository).save(captor.capture());
        assertThat(captor.getValue().getOwnerId()).isEqualTo(1L);
        assertThat(captor.getValue().getItemName()).isEqualTo("연속 출석 보상 상자");
    }

    @Test
    void 상태조회는_최근_연속일수와_보상목록을_반환한다() {
        AttendanceLog latest = AttendanceLog.builder().ownerId(1L).attendedDate(LocalDate.now()).streakAtCheckIn(7).build();
        when(attendanceLogRepository.findFirstByOwnerIdOrderByAttendedDateDesc(1L)).thenReturn(Optional.of(latest));
        FurnitureReward reward = FurnitureReward.builder().id(1L).ownerId(1L).itemName("연속 출석 보상 상자").build();
        when(furnitureRewardRepository.findByOwnerId(1L)).thenReturn(List.of(reward));

        AttendanceStatusResponse status = service.getStatus(1L);

        assertThat(status.currentStreak()).isEqualTo(7);
        assertThat(status.rewards()).hasSize(1);
        assertThat(status.rewards().get(0).itemName()).isEqualTo("연속 출석 보상 상자");
    }

    @Test
    void 기록이_없으면_상태조회는_연속일수0을_반환한다() {
        when(attendanceLogRepository.findFirstByOwnerIdOrderByAttendedDateDesc(3L)).thenReturn(Optional.empty());
        when(furnitureRewardRepository.findByOwnerId(3L)).thenReturn(List.of());

        AttendanceStatusResponse status = service.getStatus(3L);

        assertThat(status.currentStreak()).isEqualTo(0);
        assertThat(status.rewards()).isEmpty();
    }
}
