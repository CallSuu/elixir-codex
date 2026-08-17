package com.elixircodex.backend.specialelixir;

import com.elixircodex.backend.auth.AuthenticatedUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/special-elixirs")
@RequiredArgsConstructor
public class SpecialElixirController {

    private final SpecialElixirService specialElixirService;
    private final AuthenticatedUserService authenticatedUserService;

    @PostMapping
    public SpecialElixirResponse create(@RequestBody SpecialElixirCreateRequest request) {
        Long ownerId = authenticatedUserService.getCurrentUserId();
        return specialElixirService.create(ownerId, request.freeText());
    }

    @GetMapping
    public List<SpecialElixirResponse> list() {
        return specialElixirService.list(authenticatedUserService.getCurrentUserId());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        specialElixirService.delete(authenticatedUserService.getCurrentUserId(), id);
    }
}
