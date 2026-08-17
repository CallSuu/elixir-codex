package com.example.demo.Dto;

import com.example.demo.Entity.RoomCardPlacement;
import lombok.Getter;

@Getter
public class RoomCardPlacementResponseDto {

    private final Long elixirCardId;
    private final int x;
    private final int y;
    private final int rotation;
    private final int zIndex;

    public RoomCardPlacementResponseDto(RoomCardPlacement placement) {
        this.elixirCardId = placement.getElixirCardId();
        this.x = placement.getX();
        this.y = placement.getY();
        this.rotation = placement.getRotation();
        this.zIndex = placement.getZIndex();
    }
}
