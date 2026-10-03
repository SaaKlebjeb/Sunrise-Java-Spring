# Exercise — The Recipe API, in a real database

Same project as last session. Today `Recipe` stops being a record in a `Map` and becomes something
a database can store — two tables, one relationship — and by the end the application reads and
writes that database instead of the map.

**Java 25 · Spring Boot 4.1.1 · Maven · H2.**

---

## Who you are building for today

**Sunrise Kitchen** again. The client has noticed something: *"every time you restart the server,
every recipe we added is gone."* They are right. The recipes live in a `LinkedHashMap`, and a map
lives in memory.

Fixing that is two sessions of work. **Today** you teach Hibernate what a recipe *is* — which
table, which columns, how its ingredients hang off it — and switch the application from the map to
a database. Today's database is H2, in memory: it still forgets on restart. **Next session** it
becomes PostgreSQL, and the data stays.

## Start here

**This folder is where you work.** It is last session's finished Recipe API, copied here and
renamed: the Maven artifact is `recipe-api-jpa`, so IntelliJ can have it open next to last
session's project without mixing the two up. The Java package is still `com.sunrise.recipeapi`.

Nothing from today is in it yet. There is no Hibernate, no database and no entity — **you add
all of it.**

1. Open this folder's **`pom.xml`** in IntelliJ — the `pom.xml`, not the folder.
2. Run `mvn test` and confirm **twelve green** before you change anything: your eleven numbered
   tests, plus `contextLoads`.

---

## What you check today

**Your twelve tests stay green at the end of every checkpoint.** They are what proves that
changing everything underneath did not change what the API does.

| Checkpoint | What you do | How you check it |
|---|---|---|
| 1 | Hibernate, H2 and the connection pool in the build | twelve green · the pool's log lines · the H2 console |
| 2 | `Recipe` and `Ingredient` become entities | twelve green · the `create table` log |
| 3 | Spring Data writes the repository | twelve green · `curl` · rows in the H2 console |
| 4 | Quick recipes, and a page at a time | twelve green · `curl` · two queries in the log |

No new tests are written in class. **`EXTRA-tests.md`**, next to this file, has five tests that
talk to the database directly — for afterwards, if you want them.

### Pace

Checkpoint 2 is the long one: changing what a `Recipe` *is* touches every layer, and your build is
red until the last file is in. The big files are printed in full below — paste those whole. The
small changes — a `String` that becomes a `Long` — you make by hand, and each one is listed.
Checkpoint 3 is the short one — and the payoff. Checkpoint 4 only adds: nothing you already have
changes.

---

## Checkpoint 1 — Hibernate in the build

**What the client asked for:** nothing you can see yet. This checkpoint puts a database and
Hibernate into the project and proves they start.

### The code

Add these to `<dependencies>` in `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-h2console</artifactId>
</dependency>
```

and this one next to the other test starters:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa-test</artifactId>
    <scope>test</scope>
</dependency>
```

| Dependency | What it brings |
|---|---|
| `spring-boot-starter-data-jpa` | Hibernate, the JPA annotations, HikariCP (the connection pool) and Spring Data |
| `h2` | The database itself — a Java library that runs inside your application. `runtime`: your code never imports it |
| `spring-boot-h2console` | A web page for looking inside H2. In Spring Boot 4 it is its own module; without it the property below does nothing |
| `spring-boot-starter-data-jpa-test` | `@DataJpaTest`, used by the tests in `EXTRA-tests.md` |

Replace `src/main/resources/application.properties` with:

```properties
spring.application.name=recipe-api

# Spring MVC own exceptions answer as RFC 9457 Problem Details.
# Exceptions you wrote need the @RestControllerAdvice.
spring.mvc.problemdetails.enabled=true

# --- The database: H2, in memory, gone when the application stops ---
spring.datasource.url=jdbc:h2:mem:recipes
spring.datasource.username=sa
spring.datasource.password=

# --- Hibernate ---
# create-drop: build the tables from the entities at startup, drop them at shutdown.
# Fine for today. Next session it is switched off, and Liquibase owns the tables.
spring.jpa.hibernate.ddl-auto=create-drop
# Print every SQL statement Hibernate sends, formatted.
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# --- The connection pool (HikariCP, Boot's default) ---
spring.datasource.hikari.maximum-pool-size=5
spring.datasource.hikari.connection-timeout=5000

# --- A browser window onto the database: http://localhost:8080/h2-console ---
spring.h2.console.enabled=true
```

### Your turn

1. Reload Maven (the elephant icon), then `mvn test` — **twelve green.** Nothing about your code
   changed.
2. Start the application. In the log, find these lines:

   ```
   HikariPool-1 - Starting...
   HikariPool-1 - Added connection ... url=jdbc:h2:mem:recipes user=SA
   HikariPool-1 - Start completed.
   ```

   That is the connection pool opening its first connection. You never wrote a line of JDBC.
3. Open **http://localhost:8080/h2-console**. Set **JDBC URL** to `jdbc:h2:mem:recipes`, user
   `sa`, empty password, **Connect**. The database is there — and empty. Hibernate has no
   entities to make tables for yet.
4. *Optional — skip it in class, try it afterwards.* **See the pool's settings.** Add this line to
   `application.properties` and restart:

   ```properties
   logging.level.com.zaxxer.hikari=DEBUG
   ```

   Near the top of the log Hikari now prints every setting it is using. Find your two —
   `maximumPoolSize.................5` and `connectionTimeout...............5000` — and three you
   never set: `minimumIdle` (5, the same as the maximum), `maxLifetime` (1800000, thirty minutes)
   and `keepaliveTime` (120000, two minutes). **Take the line out again**; it is noisy.

---

## Checkpoint 2 — Recipe and Ingredient become entities

**What the client asked for:** *"A recipe has a list of ingredients. Every ingredient belongs to
exactly one recipe. If we delete a recipe, its ingredients go with it."*

That is two tables and one relationship: `recipes`, and `ingredients` with a `recipe_id` column
pointing back at its recipe.

### The two entities

Replace `domain/Recipe.java`:

```java
package com.sunrise.recipeapi.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * What we store -- and from today, what Hibernate stores.
 *
 * A class, not a record: Hibernate needs a no-argument constructor and fields
 * it can set after the object exists. A record has neither.
 *
 * @Getter and @Setter, never @Data. @Data writes equals/hashCode/toString over
 * every field, including the ingredients list -- which is a trap on an entity.
 */
@Entity
@Table(name = "recipes")
@Getter
@Setter
@NoArgsConstructor
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 50)
    private String cuisine;

    private int minutes;

    private int servings;

    /** Still never leaves the building -- see dto/RecipeResponse. */
    private String internalNotes;

    /**
     * mappedBy = "recipe": the Ingredient side owns the relationship -- its
     * recipe_id column is the foreign key. This list is the mirror.
     */
    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<Ingredient> ingredients = new ArrayList<>();

    /** The one way to add an ingredient: it sets BOTH sides, so the foreign key is never null. */
    public void addIngredient(String name) {
        ingredients.add(new Ingredient(name, this));
    }
}
```

Replace `domain/Ingredient.java`. The class has been sitting in the project since last session — a
plain `@Data` class that nothing uses. Now it becomes the second entity. `type` and `amount` go:
the client only ever sends an ingredient's name.

```java
package com.sunrise.recipeapi.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One line of a recipe's ingredient list, in its own table.
 *
 * This is the OWNING side: the ingredients table has the recipe_id column, so
 * whatever is in this `recipe` field is what gets written to the database.
 */
@Entity
@Table(name = "ingredients")
@Getter
@Setter
@NoArgsConstructor
public class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id")
    private Recipe recipe;

    public Ingredient(String name, Recipe recipe) {
        this.name = name;
        this.recipe = recipe;
    }
}
```

### The build is now red — and that is expected

`Recipe` used to be a record: `recipe.id()`, a `String` id, a seven-argument constructor,
`withId(...)`. None of that exists any more. It is a class with getters and setters, and the id is
a `Long` handed out by the database. **Every file that touched a `Recipe` has to follow.** Work
down this list in order; the build goes green at the end of it.

| # | File | What you do |
|---|---|---|
| 1 | `repository/RecipeRepository.java` | Replace it |
| 2 | `repository/InMemoryRecipeRepository.java` | Replace it |
| 3 | `service/RecipeService.java` | Two words, by hand |
| 4 | `service/RecipeServiceImpl.java` | Three lines, by hand |
| 5 | `dto/RecipeResponse.java` | One word, by hand |
| 6 | `mapper/RecipeMapper.java` | Replace it |
| 7 | `controller/RecipeController.java` | Three lines, by hand |
| 8 | the tests | One new helper, two files replaced |

`GlobalExceptionHandler`, `RecipeNotFoundException` and `CreateRecipeRequest` do not change.

**1. Replace `repository/RecipeRepository.java`** — `Long` ids, and two methods changed to match
the words Spring Data uses. Checkpoint 3 shows why:

```java
package com.sunrise.recipeapi.repository;

import com.sunrise.recipeapi.domain.Recipe;

import java.util.List;
import java.util.Optional;

/**
 * Where recipes are kept. The method names are the ones Spring Data uses --
 * findById, deleteById, findByCuisineIgnoreCase -- on purpose.
 */
public interface RecipeRepository {
    List<Recipe> findAll();
    Optional<Recipe> findById(Long id);                       // was String
    List<Recipe> findByCuisineIgnoreCase(String cuisine);     // was findByCuisine
    Recipe save(Recipe recipe);
    void deleteById(Long id);                                 // was String, and returned boolean
}
```

**2. Replace `repository/InMemoryRecipeRepository.java`** — still a map, now holding entities:

```java
package com.sunrise.recipeapi.repository;

import com.sunrise.recipeapi.domain.Recipe;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Still a Map. The entity is ready for a database; this class is not -- yet.
 */
@Repository
public class InMemoryRecipeRepository implements RecipeRepository {

    private final Map<Long, Recipe> shelf = new LinkedHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    @PostConstruct
    void seed() {
        save(recipe("Fish Amok", "khmer", 45, 4, "supplier: central market, pays late",
                "fish", "coconut milk", "kroeung"));
        save(recipe("Lok Lak", "khmer", 25, 2, "supplier: central market",
                "beef", "lettuce", "lime", "kampot pepper"));
        save(recipe("Carbonara", "italian", 20, 2, "supplier: deli, margin 12%",
                "spaghetti", "guanciale", "egg", "pecorino"));
    }

    @Override
    public List<Recipe> findAll() {
        return new ArrayList<>(shelf.values());
    }

    @Override
    public Optional<Recipe> findById(Long id) {
        return Optional.ofNullable(shelf.get(id));
    }

    @Override
    public List<Recipe> findByCuisineIgnoreCase(String cuisine) {
        return shelf.values().stream()
                .filter(recipe -> recipe.getCuisine().equalsIgnoreCase(cuisine))
                .toList();
    }

    @Override
    public Recipe save(Recipe recipe) {
        if (recipe.getId() == null) {
            recipe.setId(nextId.getAndIncrement());
        }
        shelf.put(recipe.getId(), recipe);
        return recipe;
    }

    @Override
    public void deleteById(Long id) {
        shelf.remove(id);
    }

    private static Recipe recipe(String title, String cuisine, int minutes, int servings,
                                 String internalNotes, String... ingredients) {
        Recipe recipe = new Recipe();
        recipe.setTitle(title);
        recipe.setCuisine(cuisine);
        recipe.setMinutes(minutes);
        recipe.setServings(servings);
        recipe.setInternalNotes(internalNotes);
        for (String ingredient : ingredients) {
            recipe.addIngredient(ingredient);
        }
        return recipe;
    }
}
```

**3. `service/RecipeService.java` — by hand.** The id is a `Long` in two methods:

```java
Recipe findById(Long id);      // was String
void delete(Long id);          // was String
```

**4. `service/RecipeServiceImpl.java` — by hand.** The same two, and the renamed repository method:

```java
public Recipe findById(Long id) {          // was String
```

```java
public void delete(Long id) {              // was String
```

```java
return cuisine == null ? recipeRepository.findAll() : recipeRepository.findByCuisineIgnoreCase(cuisine);
//                                                                     ^ was findByCuisine
```

The `"recipe id: " + id + " not found"` message stays as it is — a `Long` joins a string the same
way a `String` does.

**5. `dto/RecipeResponse.java` — by hand.** One word, `String id` becomes `Long id`:

```java
public record RecipeResponse(
    Long id, String title, String cuisine, int minutes, int servings, List<String> ingredients) {}
```

`ingredients` is still a `List<String>`. The client does not learn that there is a second table.

**6. Replace `mapper/RecipeMapper.java`** — the client still sends and receives ingredients as
plain strings, so the mapper now converts between `"fish"` and an `Ingredient`:

```java
package com.sunrise.recipeapi.mapper;

import com.sunrise.recipeapi.domain.Ingredient;
import com.sunrise.recipeapi.domain.Recipe;
import com.sunrise.recipeapi.dto.CreateRecipeRequest;
import com.sunrise.recipeapi.dto.RecipeResponse;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface RecipeMapper {

    RecipeResponse toRecipeResponse(Recipe recipe);

    /** The client still receives ["fish", "lime"]: each Ingredient becomes its name on the way out. */
    default String toName(Ingredient ingredient) {
        return ingredient.getName();
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ingredients", ignore = true)
    Recipe toRecipe(CreateRecipeRequest request);

    /** Runs after toRecipe. addIngredient sets both sides of the relationship. */
    @AfterMapping
    default void addIngredients(CreateRecipeRequest request, @MappingTarget Recipe recipe) {
        request.ingredients().forEach(recipe::addIngredient);
    }
}
```

Two new things in the mapper, both small:

- **`toName(Ingredient)`** — MapStruct needs to turn a `List<Ingredient>` into a `List<String>`.
  It finds this method by its types and calls it once per ingredient. You never call it yourself.
- **`@AfterMapping addIngredients(...)`** — runs after `toRecipe` has copied the plain fields. It
  goes through `addIngredient`, so every ingredient points back at its recipe.

The two `ignore = true` lines tell MapStruct to leave those fields alone: the database hands out
the `id`, and `addIngredients` fills the `ingredients`.

**7. `controller/RecipeController.java` — by hand.** Three lines:

```java
public ResponseEntity<RecipeResponse> getRecipeById(@PathVariable Long id) {     // was String
```

```java
return ResponseEntity.created(URI.create("/recipes/" + savedRecipe.getId()))     // was savedRecipe.id()
```

```java
public ResponseEntity<Void> deleteRecipe(@PathVariable Long id) {                // was String
```

Spring turns the `1` in `/recipes/1` into a `Long` for you.

### The tests follow the same change

**8.** The tests build `Recipe` objects, and there is no seven-argument constructor any more.
Create one helper that every test shares, in
`src/test/java/com/sunrise/recipeapi/TestRecipes.java`:

```java
package com.sunrise.recipeapi;

import com.sunrise.recipeapi.domain.Recipe;

/**
 * Builds a Recipe for a test in one line. Recipe is an entity now -- a class with
 * setters, not a record with one constructor -- so the tests share this instead
 * of repeating six setters each.
 */
public final class TestRecipes {

    private TestRecipes() {
    }

    public static Recipe recipe(Long id, String title, String cuisine, int minutes, int servings,
                                String internalNotes, String... ingredients) {
        Recipe recipe = new Recipe();
        recipe.setId(id);
        recipe.setTitle(title);
        recipe.setCuisine(cuisine);
        recipe.setMinutes(minutes);
        recipe.setServings(servings);
        recipe.setInternalNotes(internalNotes);
        for (String ingredient : ingredients) {
            recipe.addIngredient(ingredient);
        }
        return recipe;
    }
}
```

Then replace these two test files. **What they check over HTTP has not changed** — the same URLs,
the same status codes, the same JSON. What changed is how each test builds its recipes, the ids
(`1L` instead of `"1"`, and `"id": 1` in the JSON instead of `"id": "1"`), and the two repository
methods that changed.

`controller/RecipeControllerWebTest.java`:

```java
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

import static com.sunrise.recipeapi.TestRecipes.recipe;
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

    private static final Recipe AMOK = recipe(1L, "Fish Amok", "khmer", 45, 4,
            "supplier: central market", "fish", "coconut milk", "kroeung");

    private static final Recipe LOK_LAK = recipe(2L, "Lok Lak", "khmer", 25, 2,
            "supplier: central market", "beef", "lettuce", "lime");

    private static final Recipe CARBONARA = recipe(3L, "Carbonara", "italian", 20, 2,
            "supplier: deli", "spaghetti", "guanciale", "egg");

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
        when(recipeRepository.findById(1L)).thenReturn(Optional.of(AMOK));

        mockMvc.perform(get("/recipes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Fish Amok"))
                .andExpect(jsonPath("$.minutes").value(45));
    }

    @Test
    @DisplayName("3. GET /recipes?cuisine=khmer filters the collection")
    void filtersByCuisine() throws Exception {
        when(recipeRepository.findByCuisineIgnoreCase("khmer")).thenReturn(List.of(AMOK, LOK_LAK));

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
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Fish Amok"));
    }

    @Test
    @DisplayName("5. DELETE /recipes/{id} returns 204 and no body")
    void deletesARecipe() throws Exception {
        mockMvc.perform(delete("/recipes/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    @DisplayName("6. GET /recipes/{id} for an id nobody has returns 404")
    void missingRecipeIs404() throws Exception {
        when(recipeRepository.findById(eq(99L))).thenReturn(Optional.empty());

        mockMvc.perform(get("/recipes/99"))
                .andExpect(status().isNotFound());
    }
}
```

`controller/RecipeRepresentationTest.java`:

```java
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static com.sunrise.recipeapi.TestRecipes.recipe;
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
@Import({RecipeServiceImpl.class, RecipeMapperImpl.class})
@DisplayName("What the client actually sees")
class RecipeRepresentationTest {

    private static final Recipe AMOK = recipe(1L, "Fish Amok", "khmer", 45, 4,
            "supplier: central market, pays late", "fish", "coconut milk", "kroeung");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecipeRepository recipeRepository;

    @Test
    @DisplayName("7. the response never carries internalNotes")
    void doesNotLeakInternalNotes() throws Exception {
        when(recipeRepository.findById(1L)).thenReturn(Optional.of(AMOK));

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
```

`ValidationWebTest` and `RecipeApiApplicationTests` do not change.

### Your turn

1. `mvn test` — **twelve green.**
2. Start the application and scroll up to where Hibernate started. It printed the tables it built
   from your two classes:

   ```sql
   create table ingredients (
       id bigint generated by default as identity,
       recipe_id bigint not null,
       name varchar(100) not null,
       primary key (id)
   )
   create table recipes (
       minutes integer not null,
       servings integer not null,
       id bigint generated by default as identity,
       cuisine varchar(50) not null,
       title varchar(100) not null,
       internal_notes varchar(255),
       primary key (id)
   )
   alter table if exists ingredients
      add constraint FK7p08vcn6wf7fd6qp79yy2jrwg
      foreign key (recipe_id)
      references recipes
   ```

   Match each line to an annotation: `length = 100` → `varchar(100)`, `nullable = false` →
   `not null`, `internalNotes` → `internal_notes`, `@JoinColumn(name = "recipe_id")` → the foreign
   key.
3. Open the H2 console again and connect. **`RECIPES` and `INGREDIENTS` are there, and both are
   empty.** `curl http://localhost:8080/recipes` still returns three recipes — because the running
   application still reads from the map. The tables are ready; nothing uses them yet. Checkpoint 3
   changes that.

### Now break it on purpose

In `Recipe.java`, delete `mappedBy = "recipe", ` from the `@OneToMany` line. Restart and read the
`create table` output again. There is now a **third** table:

```sql
create table recipes_ingredients (
    ingredients_id bigint not null unique,
    recipe_id bigint not null
)
```

Without `mappedBy`, Hibernate thinks the list and the `recipe` field are two *different*
relationships, and builds a join table for the second one. `mappedBy` is how you say *"this list is
the same relationship as `Ingredient.recipe`, seen from the other end."* **Put it back.**

---

## Checkpoint 3 — Spring Data writes the repository

**What the client asked for:** *"use the database."* Nothing else about the API changes: same URLs,
same JSON, same status codes.

### The code

**Delete `repository/InMemoryRecipeRepository.java`.** The whole file.

Replace `repository/RecipeRepository.java`:

```java
package com.sunrise.recipeapi.repository;

import com.sunrise.recipeapi.domain.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * No class implements this interface. Spring Data writes the implementation at
 * startup and hands it to the service.
 *
 * findAll, findById, save, deleteById: inherited from JpaRepository.
 */
public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    /** A derived query: Spring Data reads the method NAME and writes the SQL. */
    List<Recipe> findByCuisineIgnoreCase(String cuisine);
}
```

That is the entire change. Nothing else in `src/main` is touched — not the service, not the
controller, not the mapper.

### Your turn

1. `mvn test` — **twelve green**, and no test file was touched. Every controller test still mocks
   `RecipeRepository`, and a mock does not care whether the interface extends `JpaRepository`. The
   five methods the service calls — `findAll`, `findById`, `findByCuisineIgnoreCase`, `save`,
   `deleteById` — have exactly the names you gave them at checkpoint 2. That is why you changed
   them.
2. Start the application. `curl http://localhost:8080/recipes` returns `[]` — the three seeded
   recipes lived in the class you just deleted. Next session brings them back, properly.
3. Add two recipes:

   ```bash
   curl -X POST http://localhost:8080/recipes -H "Content-Type: application/json" \
     -d '{"title":"Fish Amok","cuisine":"khmer","minutes":45,"servings":4,"ingredients":["fish","coconut milk","kroeung"]}'

   curl -X POST http://localhost:8080/recipes -H "Content-Type: application/json" \
     -d '{"title":"Nom Banh Chok","cuisine":"khmer","minutes":60,"servings":4,"ingredients":["rice noodles","fish","lemongrass"]}'
   ```

   Read the log while you do: one `insert into recipes`, three `insert into ingredients`, for each.
   You saved a recipe; Hibernate saved its ingredients with it.
4. Open the H2 console (`jdbc:h2:mem:recipes`) and run `select * from ingredients`. Six rows, each
   with a `recipe_id`. **The data is in the database now.**
5. `curl http://localhost:8080/recipes` and read the log. Count the `select`s: one for the recipes,
   then one more per recipe for its ingredients. Two recipes, three queries. That is the **N+1
   problem** — the answer is right, and the number of queries grows with the data. Next session
   fixes it with one annotation.
6. Stop the application and start it again. `curl http://localhost:8080/recipes` — `[]`. H2 is in
   memory. **That** is what next session's PostgreSQL is for.

---

## Checkpoint 4 — Quick recipes, and a page at a time

**What the client asked for:** *"Show us the quick recipes — anything that takes half an hour or
less, quickest first. And the menu is growing: do not send the whole list at once. Send it a page
at a time, and tell us how many pages there are."*

Two new endpoints. `GET /recipes` itself does not change — clients already read it as a plain list.

| Endpoint | Answers |
|---|---|
| `GET /recipes/quick?maxMinutes=30` | every recipe of 30 minutes or less, quickest first |
| `GET /recipes/page?page=0&size=2&sort=minutes` | one page of recipes, and the totals |

### The code

**1. `repository/RecipeRepository.java` — add one method.** A derived query: the name is the query.
Paging needs nothing here — `findAll(Pageable)` is inherited from `JpaRepository`.

```java
public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    /** A derived query: Spring Data reads the method NAME and writes the SQL. */
    List<Recipe> findByCuisineIgnoreCase(String cuisine);

    /** Derived again: where minutes <= ? order by minutes. */
    List<Recipe> findByMinutesLessThanEqualOrderByMinutesAsc(int minutes);
}
```

**2. New file `dto/PageResponse.java`** — what one page looks like on the wire:

```java
package com.sunrise.recipeapi.dto;

import java.util.List;

/**
 * One page of anything, plus the totals a client needs to draw "page 1 of 2".
 * Our own record, not Spring's Page: we decide what the JSON looks like.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
```

**3. `service/RecipeService.java` — add two methods:**

```java
List<Recipe> findQuick(int maxMinutes);

Page<Recipe> findPage(int page, int size, String sortBy);
```

with one new import: `org.springframework.data.domain.Page`.

**4. `service/RecipeServiceImpl.java` — add a constant and the two methods.** The two rules about
paging live here, because they are decisions about recipes, not about HTTP:

```java
    /** The biggest page a client may ask for. */
    private static final int MAX_PAGE_SIZE = 50;

    /** The fields a client may sort by. internalNotes is not one of them. */
    private static final Set<String> SORTABLE = Set.of("title", "cuisine", "minutes", "servings");

    @Override
    public List<Recipe> findQuick(int maxMinutes) {
        return recipeRepository.findByMinutesLessThanEqualOrderByMinutesAsc(maxMinutes);
    }

    @Override
    public Page<Recipe> findPage(int page, int size, String sortBy) {
        int safeSize = Math.min(size, MAX_PAGE_SIZE);
        String property = SORTABLE.contains(sortBy) ? sortBy : "id";
        Sort sort = Sort.by(property).and(Sort.by("id"));     // id breaks ties, so pages never overlap
        return recipeRepository.findAll(PageRequest.of(page, safeSize, sort));
    }
```

New imports:

```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Set;
```

**5. `controller/RecipeController.java` — add two endpoints.** The controller reads the URL, calls
the service, and turns the result into DTOs. Nothing else:

```java
    @GetMapping("/quick")
    public ResponseEntity<List<RecipeResponse>> quickRecipes(
            @RequestParam(defaultValue = "30") int maxMinutes) {
        List<RecipeResponse> responses =
                recipeService.findQuick(maxMinutes).stream().map(recipeMapper::toRecipeResponse).toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/page")
    public ResponseEntity<PageResponse<RecipeResponse>> pageOfRecipes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sort) {
        Page<Recipe> result = recipeService.findPage(page, size, sort);
        List<RecipeResponse> content =
                result.getContent().stream().map(recipeMapper::toRecipeResponse).toList();
        return ResponseEntity.ok(new PageResponse<>(content, result.getNumber(),
                result.getSize(), result.getTotalElements(), result.getTotalPages()));
    }
```

New imports:

```java
import com.sunrise.recipeapi.dto.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestParam;
```

`/recipes/quick` and `/recipes/page` do not collide with `/recipes/{id}`: when a URL matches an
exact path and a pattern, Spring picks the exact path.

### Your turn

1. `mvn test` — **twelve green**, and again no test file was touched. You only added methods.
2. Start the application. H2 starts empty, so put four recipes in:

   ```bash
   curl -X POST http://localhost:8080/recipes -H "Content-Type: application/json" \
     -d '{"title":"Fish Amok","cuisine":"khmer","minutes":45,"servings":4,"ingredients":["fish","coconut milk","kroeung"]}'

   curl -X POST http://localhost:8080/recipes -H "Content-Type: application/json" \
     -d '{"title":"Nom Banh Chok","cuisine":"khmer","minutes":60,"servings":4,"ingredients":["rice noodles","fish","lemongrass"]}'

   curl -X POST http://localhost:8080/recipes -H "Content-Type: application/json" \
     -d '{"title":"Lok Lak","cuisine":"khmer","minutes":25,"servings":2,"ingredients":["beef","lettuce","lime","kampot pepper"]}'

   curl -X POST http://localhost:8080/recipes -H "Content-Type: application/json" \
     -d '{"title":"Carbonara","cuisine":"italian","minutes":20,"servings":2,"ingredients":["spaghetti","guanciale","egg","pecorino"]}'
   ```

3. The quick ones:

   ```bash
   curl "http://localhost:8080/recipes/quick?maxMinutes=30"
   ```

   Carbonara (20), then Lok Lak (25). In the log, find `where r1_0.minutes<=? order by r1_0.minutes`
   — SQL you never wrote, from a method name.
4. The first page, two at a time, quickest first. **The quotes around the URL matter** — without
   them your shell treats `&` as the end of the command:

   ```bash
   curl "http://localhost:8080/recipes/page?size=2&sort=minutes"
   ```

   ```json
   {
     "content": [
       { "id": 4, "title": "Carbonara", "cuisine": "italian", "minutes": 20, "servings": 2, "ingredients": ["spaghetti", "guanciale", "egg", "pecorino"] },
       { "id": 3, "title": "Lok Lak", "cuisine": "khmer", "minutes": 25, "servings": 2, "ingredients": ["beef", "lettuce", "lime", "kampot pepper"] }
     ],
     "page": 0,
     "size": 2,
     "totalElements": 4,
     "totalPages": 2
   }
   ```

5. The second page — `page=1`, because the first page is page 0:

   ```bash
   curl "http://localhost:8080/recipes/page?page=1&size=2&sort=minutes"
   ```

   Fish Amok (45), then Nom Banh Chok (60).
6. **Read the log for that last request.** Two queries did the paging: a `select ... from recipes`
   that ends in an `offset` and a `fetch first`, and a `select count(...)`. The database cut the
   list; your Java code never saw the other two recipes. Then one more `select ... from ingredients`
   for each recipe on the page — the N+1 from checkpoint 3 is still there, only smaller.
7. Try the two rules:

   ```bash
   curl "http://localhost:8080/recipes/page?size=100000"
   curl "http://localhost:8080/recipes/page?sort=internalNotes"
   ```

   The first answers with `"size": 50`. The second sorts by `id` — the client asked to sort by a
   field it is not allowed to see, and the service quietly said no.

---

## If you get stuck

**`cannot find symbol: method id()`, `title()`, `ingredients()`** — code still calling the old
record accessors. The entity has getters: `getId()`, `getTitle()`. Find the file in the error and
go back to its step in the checkpoint 2 list.

**`incompatible types: String cannot be converted to Long`** — an id still written as `"1"`, or a
method still declared with `String id`. It is `1L` and `Long id` now.

**`RecipeServiceImpl is not abstract and does not override abstract method`** — the interface and
the implementation disagree. Both `findById` and `delete` take a `Long` in both files.

**`No default (no-argument) constructor for entity`** — `@NoArgsConstructor` is missing from an
entity. Hibernate creates the object empty first, then fills the fields.

**`Not a managed type: class ...Recipe`** or no `create table` in the log — `@Entity` is missing,
or the class is outside the `com.sunrise.recipeapi` package.

**`ingredients` comes back empty, or `recipe_id` is null** — an ingredient was added with
`getIngredients().add(...)` instead of `addIngredient(...)`. Only `addIngredient` sets both sides.

**`APPLICATION FAILED TO START` … `Consider defining a bean of type 'RecipeRepository'`** — at
checkpoint 3, the interface does not extend `JpaRepository<Recipe, Long>`, or it is outside the
`com.sunrise.recipeapi` package.

**`GET /recipes` returns `[]` after checkpoint 3** — correct. The seeded recipes were in the class you
deleted, and H2 starts empty. POST some.

**`No property 'minute' found for type 'Recipe'`** and the application will not start — a derived
query's name uses a field the entity does not have. Check the spelling in `RecipeRepository`: it is
`findByMinutesLessThanEqualOrderByMinutesAsc`, with `Minutes` twice.

**`curl` prints only part of the answer, or `[1] 12345`** — the URL has an `&` in it and no quotes
around it. Put the whole URL in double quotes.

**`Page index must not be less than zero`** — `?page=-1`. The first page is page 0.

**`GET /recipes/page` answers 400 with `Failed to convert 'page'`** — the request went to
`/recipes/{id}`: the `@GetMapping("/page")` method is missing or misspelt.

**H2 console says `Database "mem:recipes" not found`** — the JDBC URL on the login page is still
the default. Type `jdbc:h2:mem:recipes`, exactly as in `application.properties`. The application
must be running.

**The H2 console page is a 404** — `spring-boot-h2console` is missing from `pom.xml`. Spring
Boot 4 moved the console into its own module.

**`JPA metamodel must not be empty`** in a controller test — `@EnableJpaAuditing` has been put on
`RecipeApiApplication`. See the auditing section of the lecture; it belongs on its own
`@Configuration` class.
