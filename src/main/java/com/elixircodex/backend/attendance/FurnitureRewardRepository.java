package com.elixircodex.backend.attendance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FurnitureRewardRepository extends JpaRepository<FurnitureReward, Long> {

    List<FurnitureReward> findByOwnerId(Long ownerId);
}
