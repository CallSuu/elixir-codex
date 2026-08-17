package com.elixircodex.backend.alchemy;

import com.elixircodex.backend.stack.Grade;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/alchemy")
@RequiredArgsConstructor
public class AlchemyArtController {

    private final ArtMatchingService artMatchingService;

    @GetMapping("/art")
    public ResponseEntity<?> getArt(@RequestParam Grade grade, @RequestParam ThemeCategory themeCategory) {
        String imageUrl = artMatchingService.findImageUrl(grade, themeCategory);
        return ResponseEntity.ok(Map.of("imageUrl", imageUrl));
    }
}
