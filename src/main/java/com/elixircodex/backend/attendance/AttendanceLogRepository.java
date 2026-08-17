package com.elixircodex.backend.attendance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface AttendanceLogRepository extends JpaRepository<AttendanceLog, Long> {

    Optional<AttendanceLog> findByOwnerIdAndAttendedDate(Long ownerId, LocalDate attendedDate);

    Optional<AttendanceLog> findFirstByOwnerIdOrderByAttendedDateDesc(Long ownerId);
}
