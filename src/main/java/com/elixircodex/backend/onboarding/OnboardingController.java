package com.elixircodex.backend.onboarding;

import com.elixircodex.backend.auth.AuthenticatedUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingClassificationService onboardingClassificationService;
    private final AuthenticatedUserService authenticatedUserService;

    @PostMapping("/classify")
    public OnboardingClassifyResponse classify(@RequestBody OnboardingClassifyRequest request) {
        Long ownerId = authenticatedUserService.getCurrentUserId();
        return onboardingClassificationService.classifyAndUpdateUser(ownerId, request.freeText());
    }
}
