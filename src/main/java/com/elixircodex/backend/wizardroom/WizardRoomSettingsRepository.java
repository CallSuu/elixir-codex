package com.elixircodex.backend.wizardroom;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WizardRoomSettingsRepository extends JpaRepository<WizardRoomSettings, Long> {

    Optional<WizardRoomSettings> findByOwnerId(Long ownerId);
}
