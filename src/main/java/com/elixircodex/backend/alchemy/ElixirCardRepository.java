package com.elixircodex.backend.alchemy;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ElixirCardRepository extends JpaRepository<ElixirCard, Long> {

    long countByOwnerIdAndCreatedAtBetween(Long ownerId, LocalDateTime start, LocalDateTime end);

    List<ElixirCard> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    long countByGrade(ElixirGrade grade);
}
