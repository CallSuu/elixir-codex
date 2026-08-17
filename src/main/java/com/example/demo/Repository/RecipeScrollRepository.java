package com.example.demo.Repository;

import com.example.demo.Entity.RecipeScroll;
import com.example.demo.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecipeScrollRepository extends JpaRepository<RecipeScroll, Long> {

    Optional<RecipeScroll> findByUserAndName(User user, String name);

    List<RecipeScroll> findByUser(User user);
}
