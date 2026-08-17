package com.example.demo.Controller;

import com.example.demo.Dto.InventoryResponseDto;
import com.example.demo.Service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    // 보유 재료 카드 + 레시피 스크롤 목록 조회
    @GetMapping
    public ResponseEntity<InventoryResponseDto> getInventory(Authentication authentication) {
        return ResponseEntity.ok(inventoryService.getInventory(authentication.getName()));
    }
}
