package com.sunrise.recipeapi.controller;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Three tests.
 *
 * 9 and 10 go green at checkpoint 4, as soon as the constraints are on the record
 * and @Valid is on the parameter. 11 needs the advice, so it goes green at
 * checkpoint 5.
 *
 * If 9 and 10 stay red after you have added the annotations: check for @Valid on
 * the controller parameter. That is the answer nine times out of ten.
 */
@WebMvcTest(RecipeController.class)
@Import(RecipeServiceImpl.class)
@DisplayName("A request that should not be accepted")
class ValidationWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecipeRepository recipeRepository;

    @Test
    @DisplayName("9. POST /recipes with a blank title is rejected")
    void blankTitleIsRejected() throws Exception {
        String body = """
                {
                  "title": "   ",
                  "cuisine": "khmer",
                  "minutes": 45,
                  "servings": 4,
                  "ingredients": ["fish"]
                }
                """;

        mockMvc.perform(post("/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("10. POST /recipes with minutes below 1 is rejected")
    void zeroMinutesIsRejected() throws Exception {
        String body = """
                {
                  "title": "Fish Amok",
                  "cuisine": "khmer",
                  "minutes": 0,
                  "servings": 4,
                  "ingredients": ["fish"]
                }
                """;

        mockMvc.perform(post("/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("11. the rejection names every field that was wrong")
    void theProblemDetailNamesEveryBadField() throws Exception {
        String body = """
                {
                  "title": "   ",
                  "cuisine": "khmer",
                  "minutes": 0,
                  "servings": 4,
                  "ingredients": ["fish"]
                }
                """;

        mockMvc.perform(post("/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.minutes").exists());
    }
}