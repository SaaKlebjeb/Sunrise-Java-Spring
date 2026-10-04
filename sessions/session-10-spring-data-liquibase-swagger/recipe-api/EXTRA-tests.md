# Extra — tests for the repository, the search and the migration

**Not part of the session.** Do these afterwards if you want them — six tests, numbered 22 to 27,
carrying on from last session's extra tests.

**Two things they need, both from last session's `README.md` and `EXTRA-tests.md`:** the
`spring-boot-starter-data-jpa-test` dependency in `pom.xml`, and the `TestRecipes` helper class
under `src/test/java/com/sunrise/recipeapi/`. Add those first if your project does not have them.

They need no PostgreSQL: `@DataJpaTest` swaps in a fresh in-memory H2 database, runs your two
Liquibase changesets against it, and rolls every test back.

---

## The repository Spring Data wrote — tests 22, 23

Create `src/test/java/com/sunrise/recipeapi/repository/RecipeRepositoryTest.java`:

```java
package com.sunrise.recipeapi.repository;

import com.sunrise.recipeapi.domain.Recipe;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;

import static com.sunrise.recipeapi.TestRecipes.recipe;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The real RecipeRepository -- the one Spring Data writes -- against H2.
 *
 * Each test saves its own Thai recipes, so it does not care what else is in
 * the database. That matters from checkpoint 3 on, when Liquibase seeds three
 * recipes before any test runs.
 */
@DataJpaTest
@DisplayName("The repository Spring Data wrote")
class RecipeRepositoryTest {

    @Autowired
    private RecipeRepository recipeRepository;

    @Test
    @DisplayName("22. findByCuisineIgnoreCase -- a query written from the method name")
    void derivedQuery() {
        recipeRepository.save(recipe(null, "Tom Yum", "thai", 30, 2, null, "shrimp", "galangal"));
        recipeRepository.save(recipe(null, "Pad Thai", "thai", 25, 2, null, "rice noodles", "egg"));

        List<Recipe> thai = recipeRepository.findByCuisineIgnoreCase("THAI");

        assertThat(thai)
                .extracting(Recipe::getTitle)
                .containsExactlyInAnyOrder("Tom Yum", "Pad Thai");
    }

    @Test
    @DisplayName("23. findByIngredient -- a JPQL query, each recipe once")
    void jpqlQuery() {
        recipeRepository.save(recipe(null, "Tom Yum", "thai", 30, 2, null, "shrimp", "galangal", "lime"));
        recipeRepository.save(recipe(null, "Tom Kha", "thai", 35, 4, null, "chicken", "galangal", "coconut milk"));
        recipeRepository.save(recipe(null, "Pad Thai", "thai", 25, 2, null, "rice noodles", "egg"));

        List<Recipe> withGalangal = recipeRepository.findByIngredient("Galangal");

        assertThat(withGalangal)
                .extracting(Recipe::getTitle)
                .containsExactlyInAnyOrder("Tom Yum", "Tom Kha");
    }
}
```

Each test saves its own Thai recipes and asks only about those, so it passes whether or not
Liquibase has already seeded the three house recipes.

## The search endpoint, over HTTP — test 24

Create `src/test/java/com/sunrise/recipeapi/controller/SearchWebTest.java`:

```java
package com.sunrise.recipeapi.controller;

import com.sunrise.recipeapi.mapper.RecipeMapperImpl;
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

import static com.sunrise.recipeapi.TestRecipes.recipe;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The new endpoint, over HTTP. Same slice as every controller test: the
 * repository is a mock, so this test says nothing about the SQL -- test 24 does.
 */
@WebMvcTest(RecipeController.class)
@Import({RecipeServiceImpl.class, RecipeMapperImpl.class})
@DisplayName("Searching by ingredient, over HTTP")
class SearchWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecipeRepository recipeRepository;

    @Test
    @DisplayName("24. GET /recipes/search?ingredient=fish returns the recipes that use it")
    void searchByIngredient() throws Exception {
        when(recipeRepository.findByIngredient("fish")).thenReturn(List.of(
                recipe(1L, "Fish Amok", "khmer", 45, 4, null, "fish", "coconut milk")));

        mockMvc.perform(get("/recipes/search").param("ingredient", "fish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Fish Amok"))
                .andExpect(jsonPath("$[0].ingredients[0]").value("fish"));
    }
}
```

The same slice as every controller test you already have: the repository is a mock, so this says
nothing about the SQL. Test 23 does that.

## The schema Liquibase built — test 25

Create `src/test/java/com/sunrise/recipeapi/migration/LiquibaseSeedTest.java`:

```java
package com.sunrise.recipeapi.migration;

import com.sunrise.recipeapi.domain.Ingredient;
import com.sunrise.recipeapi.domain.Recipe;
import com.sunrise.recipeapi.repository.RecipeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Nothing in this test saves anything. If the three recipes are there, the only
 * thing that can have put them there is Liquibase, running your two changesets
 * before the test started. And if the tables did not match the entities,
 * ddl-auto=validate would have stopped the test from starting at all.
 */
@DataJpaTest
@DisplayName("The schema Liquibase built")
class LiquibaseSeedTest {

    @Autowired
    private RecipeRepository recipeRepository;

    @Test
    @DisplayName("25. Liquibase built the tables and seeded Fish Amok, Lok Lak and Carbonara")
    void liquibaseSeededTheRecipes() {
        assertThat(recipeRepository.findAll())
                .extracting(Recipe::getTitle)
                .contains("Fish Amok", "Lok Lak", "Carbonara");

        Recipe amok = recipeRepository.findByCuisineIgnoreCase("khmer").stream()
                .filter(recipe -> recipe.getTitle().equals("Fish Amok"))
                .findFirst()
                .orElseThrow();
        assertThat(amok.getIngredients())
                .extracting(Ingredient::getName)
                .containsExactly("fish", "coconut milk", "kroeung");
    }
}
```

Nothing in this test saves anything. If the three recipes are there, Liquibase put them there —
and if the tables did not match the entities, `ddl-auto=validate` would have stopped the test from
starting at all. **This one only passes after checkpoint 2.**

## Recipes and tags, many to many — tests 26, 27

Create `src/test/java/com/sunrise/recipeapi/repository/RecipeTagTest.java`:

```java
package com.sunrise.recipeapi.repository;

import com.sunrise.recipeapi.domain.Recipe;
import com.sunrise.recipeapi.domain.Tag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.List;

import static com.sunrise.recipeapi.TestRecipes.recipe;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Many-to-many, from both ends: one tag on two recipes, and two tags on one recipe.
 */
@DataJpaTest
@DisplayName("Recipes and tags, many to many")
class RecipeTagTest {

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("26. one tag on two recipes -- findByTagsNameIgnoreCase finds both")
    void oneTagManyRecipes() {
        Tag spicy = tagRepository.save(new Tag("spicy-test"));
        Recipe tomYum = recipe(null, "Tom Yum", "thai", 30, 2, null, "shrimp");
        Recipe padKrapow = recipe(null, "Pad Krapow", "thai", 15, 2, null, "basil");
        tomYum.getTags().add(spicy);
        padKrapow.getTags().add(spicy);
        recipeRepository.save(tomYum);
        recipeRepository.save(padKrapow);
        entityManager.flush();
        entityManager.clear();

        List<Recipe> tagged = recipeRepository.findByTagsNameIgnoreCase("SPICY-TEST");

        assertThat(tagged)
                .extracting(Recipe::getTitle)
                .containsExactlyInAnyOrder("Tom Yum", "Pad Krapow");
    }

    @Test
    @DisplayName("27. two tags on one recipe -- and the tag rows are shared, not copied")
    void oneRecipeManyTags() {
        Tag quick = tagRepository.save(new Tag("quick-test"));
        Tag soup = tagRepository.save(new Tag("soup-test"));
        long tagsBefore = tagRepository.count();
        Recipe tomYum = recipe(null, "Tom Yum", "thai", 30, 2, null, "shrimp");
        tomYum.getTags().add(quick);
        tomYum.getTags().add(soup);
        Long id = recipeRepository.save(tomYum).getId();
        entityManager.flush();
        entityManager.clear();

        Recipe found = recipeRepository.findById(id).orElseThrow();

        assertThat(found.getTags())
                .extracting(Tag::getName)
                .containsExactlyInAnyOrder("quick-test", "soup-test");
        assertThat(tagRepository.count()).isEqualTo(tagsBefore);
    }
}
```

Test 26 is one tag on two recipes; test 27 is two tags on one recipe — the two halves of
"many-to-many". The last line of test 27 is the important one: tagging a recipe adds rows to
`recipe_tags`, never to `tags`. **These two only pass after checkpoint 3.**
