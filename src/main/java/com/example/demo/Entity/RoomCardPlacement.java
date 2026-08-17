package com.example.demo.Entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomCardPlacement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long ownerId;

    // com.elixircodex.backend.alchemy.ElixirCard의 id를 참조하되 FK 제약은 걸지 않음
    private Long elixirCardId;

    private int x;
    private int y;
    private int rotation;
    private int zIndex;
}
