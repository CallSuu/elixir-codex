package com.elixircodex.backend.alchemy;

import com.elixircodex.backend.stack.Grade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArtTemplateRepository extends JpaRepository<ArtTemplate, Long> {

    List<ArtTemplate> findByGradeAndThemeCategory(Grade grade, ThemeCategory themeCategory);
}
