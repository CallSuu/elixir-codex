package com.elixircodex.backend.stack;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IngredientCardRepository extends JpaRepository<IngredientCard, Long> {

    Optional<IngredientCard> findByOwnerIdAndNameAndGrade(Long ownerId, String name, Grade grade);

    List<IngredientCard> findByOwnerIdOrderByGradeDescCreatedAtDesc(Long ownerId);
}
