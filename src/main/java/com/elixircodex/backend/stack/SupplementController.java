package com.elixircodex.backend.stack;

import com.elixircodex.backend.auth.AuthenticatedUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/supplements")
@RequiredArgsConstructor
public class SupplementController {

    private final SupplementVerificationService supplementVerificationService;
    private final AuthenticatedUserService authenticatedUserService;

    @PostMapping(value = "/verify", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SupplementVerifyResponse verify(@RequestParam MultipartFile image) {
        return supplementVerificationService.verify(authenticatedUserService.getCurrentUserId(), image);
    }

    @GetMapping
    public List<SupplementVerifyResponse> getMyLogs() {
        return supplementVerificationService.getLogs(authenticatedUserService.getCurrentUserId());
    }
}
