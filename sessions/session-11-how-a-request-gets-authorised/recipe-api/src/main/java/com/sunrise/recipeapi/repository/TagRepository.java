package com.sunrise.recipeapi.repository;

import com.sunrise.recipeapi.domain.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {
  Optional<Tag> findByNameIgnoreCase(String name);
}
