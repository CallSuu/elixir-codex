package com.elixircodex.backend.wizardroom;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WizardRoomPlacementRepository extends JpaRepository<WizardRoomPlacement, Long> {

    List<WizardRoomPlacement> findByOwnerId(Long ownerId);

    void deleteByOwnerId(Long ownerId);
}
