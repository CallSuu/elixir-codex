package com.example.demo.Service;

import com.elixircodex.backend.stack.IngredientCardRepository;
import com.example.demo.Dto.IngredientCardResponseDto;
import com.example.demo.Dto.InventoryResponseDto;
import com.example.demo.Dto.RecipeScrollResponseDto;
import com.example.demo.Entity.User;
import com.example.demo.Repository.RecipeScrollRepository;
import com.example.demo.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final UserRepository userRepository;
    private final IngredientCardRepository ingredientCardRepository;
    private final RecipeScrollRepository recipeScrollRepository;

    @Transactional(readOnly = true)
    public InventoryResponseDto getInventory(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 이메일입니다."));

        var ingredientCards = ingredientCardRepository.findByOwnerIdOrderByGradeDescCreatedAtDesc(user.getId()).stream()
                .map(IngredientCardResponseDto::new)
                .toList();

        var recipeScrolls = recipeScrollRepository.findByUser(user).stream()
                .map(RecipeScrollResponseDto::new)
                .toList();

        return new InventoryResponseDto(ingredientCards, recipeScrolls);
    }
}
