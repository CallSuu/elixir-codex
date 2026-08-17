package com.example.demo.Repository;

import com.example.demo.Entity.RoomCardPlacement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomCardPlacementRepository extends JpaRepository<RoomCardPlacement, Long> {

    List<RoomCardPlacement> findByOwnerId(Long ownerId);

    void deleteByOwnerId(Long ownerId);
}
