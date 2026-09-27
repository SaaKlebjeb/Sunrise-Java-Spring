package com.sunrise.recipeapi.controller;

import com.sunrise.recipeapi.domain.Recipe;
import com.sunrise.recipeapi.mapper.RecipeMapperImpl;
import com.sunrise.recipeapi.repository.RecipeRepository;
import com.sunrise.recipeapi.service.RecipeServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The slice, again. @WebMvcTest loads only the web layer: this controller, the
 * MVC infrastructure, the converters, and any @RestControllerAdvice you write.
 *
 * It does NOT load @Service classes, so @Import brings in the real
 * RecipeServiceImpl. The repository -- the thing that will one day be a
 * database -- is NOT a real bean here: it is a mock, supplied below, and each
 * test decides what it returns.
 *
 * @MockitoBean, not @MockBean. @MockBean was removed in Spring Boot 4.
 *
 * Six tests. 1, 2 and 3 go green at checkpoint 1. 4, 5 and 6 need deliberate
 * status codes, so they go green at checkpoint 2.
 */
@WebMvcTest(RecipeController.class)
@Import({RecipeServiceImpl.class, RecipeMapperImpl.class})
@DisplayName("The recipe collection, over HTTP")
class RecipeControllerWebTest {

    private static final Recipe AMOK = new Recipe(
            "1", "Fish Amok", "khmer", 45, 4,
            List.of("fish", "coconut milk", "kroeung"), "supplier: central market");

    private static final Recipe LOK_LAK = new Recipe(
            "2", "Lok Lak", "khmer", 25, 2,
            List.of("beef", "lettuce", "lime"), "supplier: central market");

    private static final Recipe CARBONARA = new Recipe(
            "3", "Carbonara", "italian", 20, 2,
            List.of("spaghetti", "guanciale", "egg"), "supplier: deli");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecipeRepository recipeRepository;

    @Test
    @DisplayName("1. GET /recipes returns the whole collection")
    void listsEveryRecipe() throws Exception {
        when(recipeRepository.findAll()).thenReturn(List.of(AMOK, LOK_LAK, CARBONARA));

        mockMvc.perform(get("/recipes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].title").value("Fish Amok"))
                .andExpect(jsonPath("$[2].cuisine").value("italian"));
    }

    @Test
    @DisplayName("2. GET /recipes/{id} returns one recipe")
    void returnsOneRecipe() throws Exception {
        when(recipeRepository.findById("1")).thenReturn(Optional.of(AMOK));

        mockMvc.perform(get("/recipes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.title").value("Fish Amok"))
                .andExpect(jsonPath("$.minutes").value(45));
    }

    @Test
    @DisplayName("3. GET /recipes?cuisine=khmer filters the collection")
    void filtersByCuisine() throws Exception {
        when(recipeRepository.findByCuisine("khmer")).thenReturn(List.of(AMOK, LOK_LAK));

        mockMvc.perform(get("/recipes").param("cuisine", "khmer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].cuisine").value("khmer"))
                .andExpect(jsonPath("$[1].cuisine").value("khmer"));
    }

    @Test
    @DisplayName("4. POST /recipes returns 201 and a Location header naming the new recipe")
    void createsARecipe() throws Exception {
        when(recipeRepository.save(any(Recipe.class))).thenReturn(AMOK);

        String body = """
                {
                  "title": "Fish Amok",
                  "cuisine": "khmer",
                  "minutes": 45,
                  "servings": 4,
                  "ingredients": ["fish", "coconut milk", "kroeung"]
                }
                """;

        mockMvc.perform(post("/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/recipes/1"))
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.title").value("Fish Amok"));
    }

    @Test
    @DisplayName("5. DELETE /recipes/{id} returns 204 and no body")
    void deletesARecipe() throws Exception {
        when(recipeRepository.deleteById("1")).thenReturn(true);

        mockMvc.perform(delete("/recipes/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    @DisplayName("6. GET /recipes/{id} for an id nobody has returns 404")
    void missingRecipeIs404() throws Exception {
        when(recipeRepository.findById(eq("does-not-exist"))).thenReturn(Optional.empty());

        mockMvc.perform(get("/recipes/does-not-exist"))
                .andExpect(status().isNotFound());
    }
}