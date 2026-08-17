package com.elixircodex.backend.attendance;

import com.elixircodex.backend.auth.AuthenticatedUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final AuthenticatedUserService authenticatedUserService;

    @PostMapping("/check")
    public AttendanceCheckResponse check() {
        return attendanceService.checkIn(authenticatedUserService.getCurrentUserId());
    }

    @GetMapping
    public AttendanceStatusResponse status() {
        return attendanceService.getStatus(authenticatedUserService.getCurrentUserId());
    }
}
