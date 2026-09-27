package com.sunrise.recipeapi.controller;

import com.sunrise.recipeapi.domain.Recipe;
import com.sunrise.recipeapi.repository.RecipeRepository;
import com.sunrise.recipeapi.service.RecipeServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * What you send is not what you store.
 *
 * Two tests, both green at checkpoint 3. The first is the one that matters:
 * `internalNotes` is a real field on Recipe, and it must never leave the
 * building. It is the reason RecipeResponse exists.
 */
@WebMvcTest(RecipeController.class)
@Import(RecipeServiceImpl.class)
@DisplayName("What the client actually sees")
class RecipeRepresentationTest {

    private static final Recipe AMOK = new Recipe(
            "1", "Fish Amok", "khmer", 45, 4,
            List.of("fish", "coconut milk", "kroeung"),
            "supplier: central market, pays late");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecipeRepository recipeRepository;

    @Test
    @DisplayName("7. the response never carries internalNotes")
    void doesNotLeakInternalNotes() throws Exception {
        when(recipeRepository.findById("1")).thenReturn(Optional.of(AMOK));

        mockMvc.perform(get("/recipes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Fish Amok"))
                .andExpect(jsonPath("$.internalNotes").doesNotExist());
    }

    @Test
    @DisplayName("8. GET /recipes with nothing stored returns 200 and an empty array")
    void emptyCollectionIsAnEmptyArray() throws Exception {
        when(recipeRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/recipes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }
}