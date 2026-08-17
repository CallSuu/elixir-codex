package com.elixircodex.backend.attendance;

import com.elixircodex.backend.auth.AuthenticatedUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceControllerTest {

    @Mock
    private AttendanceService attendanceService;
    @Mock
    private AuthenticatedUserService authenticatedUserService;

    private AttendanceController controller;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        controller = new AttendanceController(attendanceService, authenticatedUserService);
        when(authenticatedUserService.getCurrentUserId()).thenReturn(1L);
    }

    @Test
    void 정상_체크시_결과를_그대로_반환한다() {
        AttendanceCheckResponse expected = new AttendanceCheckResponse(6, false, null);
        when(attendanceService.checkIn(1L)).thenReturn(expected);

        AttendanceCheckResponse response = controller.check();

        assertThat(response).isEqualTo(expected);
    }

    @Test
    void 중복_체크시_예외가_그대로_전파된다() {
        when(attendanceService.checkIn(1L))
                .thenThrow(new AttendanceValidationException("오늘 이미 출석체크를 완료했습니다"));

        assertThatThrownBy(() -> controller.check())
                .isInstanceOf(AttendanceValidationException.class)
                .hasMessage("오늘 이미 출석체크를 완료했습니다");
    }

    @Test
    void 상태조회는_결과를_그대로_반환한다() {
        AttendanceStatusResponse expected = new AttendanceStatusResponse(7, java.util.List.of());
        when(attendanceService.getStatus(1L)).thenReturn(expected);

        AttendanceStatusResponse response = controller.status();

        assertThat(response).isEqualTo(expected);
    }
}
