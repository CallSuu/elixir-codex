package com.elixircodex.backend.specialelixir;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpecialElixirRepository extends JpaRepository<SpecialElixir, Long> {

    long countByOwnerId(Long ownerId);

    List<SpecialElixir> findByOwnerId(Long ownerId);
}
