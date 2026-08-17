package com.example.demo.Dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class RoomPlacementRequestDto {

    private List<PlacementItem> placements;

    @Getter
    @NoArgsConstructor
    public static class PlacementItem {
        private Long elixirCardId;
        private int x;
        private int y;
        private int rotation;
        private int zIndex;
    }
}
