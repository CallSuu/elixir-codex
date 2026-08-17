package com.elixircodex.backend.stack;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface SupplementLogRepository extends JpaRepository<SupplementLog, Long> {

    List<SupplementLog> findByOwnerIdAndConsumedDateAndIsVerifiedTrue(Long ownerId, LocalDate consumedDate);

    List<SupplementLog> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
}
