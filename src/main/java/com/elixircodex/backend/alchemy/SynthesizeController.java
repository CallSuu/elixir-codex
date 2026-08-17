package com.elixircodex.backend.alchemy;

import com.elixircodex.backend.auth.AuthenticatedUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/synthesize")
@RequiredArgsConstructor
public class SynthesizeController {

    private final SynthesizeService synthesizeService;
    private final AuthenticatedUserService authenticatedUserService;

    @PostMapping
    public ResponseEntity<?> synthesize(@RequestBody SynthesizeRequest request) {
        Long ownerId = authenticatedUserService.getCurrentUserId();
        return ResponseEntity.ok(synthesizeService.synthesize(ownerId, request));
    }
}
