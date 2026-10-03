# Extra — tests that talk to the database

**Not part of the session.** Do these afterwards if you want to see how persistence code is tested.
The lecture's last section, *Extra learning material*, explains every idea used here.

All five run with `mvn test` and need nothing installed: `@DataJpaTest` starts Hibernate against a
fresh in-memory H2 database, and rolls each test back when it finishes. With them, `mvn test` shows
**seventeen green** instead of twelve.

---

## Prove the mapping — tests 12, 13, 14

The controller tests use a *mock* repository — they never touch a database. These are the
opposite: no web, no service, just Hibernate and H2, saving real rows and reading them back.

Create `src/test/java/com/sunrise/recipeapi/domain/RecipeMappingTest.java`:

```java
package com.sunrise.recipeapi.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import static com.sunrise.recipeapi.TestRecipes.recipe;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @DataJpaTest starts the persistence slice only: Hibernate, an in-memory H2
 * database, and a TestEntityManager. No controllers, no services, no web.
 * Every test runs in a transaction that is rolled back at the end.
 *
 * The pattern in every test: persist, flush (send the SQL), clear (forget
 * everything Hibernate remembers), then read back. Without clear(), find()
 * would hand you the object you already have, and you would prove nothing.
 *
 * Watch the console while these run -- spring.jpa.show-sql=true prints every
 * statement Hibernate sends.
 */
@DataJpaTest
@DisplayName("A recipe, in a real database")
class RecipeMappingTest {

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("12. a saved recipe gets an id from the database and reads back field for field")
    void savedRecipeReadsBack() {
        Recipe amok = recipe(null, "Fish Amok", "khmer", 45, 4, "supplier pays late", "fish");

        Long id = entityManager.persistAndGetId(amok, Long.class);
        entityManager.flush();
        entityManager.clear();

        Recipe found = entityManager.find(Recipe.class, id);

        assertThat(id).isNotNull();
        assertThat(found).isNotSameAs(amok);
        assertThat(found.getTitle()).isEqualTo("Fish Amok");
        assertThat(found.getCuisine()).isEqualTo("khmer");
        assertThat(found.getMinutes()).isEqualTo(45);
        assertThat(found.getInternalNotes()).isEqualTo("supplier pays late");
    }

    @Test
    @DisplayName("13. its ingredients are saved with it, in order, each pointing back at it")
    void ingredientsAreSavedWithTheRecipe() {
        Recipe lokLak = recipe(null, "Lok Lak", "khmer", 25, 2, null,
                "beef", "lettuce", "lime");

        Long id = entityManager.persistAndGetId(lokLak, Long.class);
        entityManager.flush();
        entityManager.clear();

        Recipe found = entityManager.find(Recipe.class, id);

        assertThat(found.getIngredients())
                .extracting(Ingredient::getName)
                .containsExactly("beef", "lettuce", "lime");
        assertThat(found.getIngredients())
                .allSatisfy(ingredient -> assertThat(ingredient.getRecipe().getId()).isEqualTo(id));
    }

    @Test
    @DisplayName("14. deleting a recipe deletes its ingredients too")
    void deletingARecipeDeletesItsIngredients() {
        Recipe carbonara = recipe(null, "Carbonara", "italian", 20, 2, null,
                "spaghetti", "egg");
        Long id = entityManager.persistAndGetId(carbonara, Long.class);
        entityManager.flush();
        entityManager.clear();

        entityManager.remove(entityManager.find(Recipe.class, id));
        entityManager.flush();

        Long ingredientsLeft = entityManager.getEntityManager()
                .createQuery("select count(i) from Ingredient i where i.recipe.id = :id", Long.class)
                .setParameter("id", id)
                .getSingleResult();
        assertThat(ingredientsLeft).isZero();
    }
}
```

The same four steps in every test:

| Step | What it does |
|---|---|
| `persist` | Hand the object to Hibernate. It gets an id |
| `flush` | Send the SQL to the database *now*, instead of whenever Hibernate chooses |
| `clear` | Make Hibernate forget every object it is holding |
| `find` | Read it back — which now has to come from the database, because Hibernate forgot |

**Skip `clear()` and you prove nothing:** `find` hands back the object you already had, straight
from memory, and the test passes whether the mapping works or not.

Read the console while it runs. Test 13 is worth reading slowly — one `insert` into `recipes`,
three into `ingredients`, and later one `select` for the recipe and a *separate* `select` for its
ingredients.

---

## Watch Hibernate work — tests 15, 16

Two behaviours you cannot see in Java code, only in the SQL log.

Create `src/test/java/com/sunrise/recipeapi/domain/HibernateAtWorkTest.java`:

```java
package com.sunrise.recipeapi.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.List;

import static com.sunrise.recipeapi.TestRecipes.recipe;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Two tests whose real result is in the CONSOLE, not in the assertion.
 *
 * Both pass. What you are here for is the number of SELECT statements
 * Hibernate prints while each one runs. Count them.
 */
@DataJpaTest
@DisplayName("Hibernate at work -- count the SELECTs")
class HibernateAtWorkTest {

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("15. asking for the same recipe twice returns the same object -- and one SELECT")
    void firstLevelCache() {
        Long id = entityManager.persistAndGetId(
                recipe(null, "Fish Amok", "khmer", 45, 4, null, "fish"), Long.class);
        entityManager.flush();
        entityManager.clear();

        Recipe first = entityManager.find(Recipe.class, id);
        Recipe second = entityManager.find(Recipe.class, id);

        // Look at the console: one select for two find() calls.
        assertThat(second).isSameAs(first);
    }

    @Test
    @DisplayName("16. reading every recipe's ingredients -- count the SELECTs in the console")
    void nPlusOne() {
        entityManager.persist(recipe(null, "Fish Amok", "khmer", 45, 4, null, "fish", "coconut milk"));
        entityManager.persist(recipe(null, "Lok Lak", "khmer", 25, 2, null, "beef", "lime"));
        entityManager.persist(recipe(null, "Carbonara", "italian", 20, 2, null, "spaghetti", "egg"));
        entityManager.flush();
        entityManager.clear();

        List<Recipe> recipes = entityManager.getEntityManager()
                .createQuery("select r from Recipe r", Recipe.class)
                .getResultList();

        long ingredientCount = 0;
        for (Recipe recipe : recipes) {
            ingredientCount += recipe.getIngredients().size();   // one more SELECT per recipe
        }

        // 1 select for the recipes + 1 select per recipe for its ingredients. That is N+1.
        Long ingredientsInTheTable = entityManager.getEntityManager()
                .createQuery("select count(i) from Ingredient i", Long.class)
                .getSingleResult();
        assertThat(ingredientCount).isEqualTo(ingredientsInTheTable);
    }
}
```

Both pass. **The lesson is in the console.** Run the class on its own (the green arrow next to the
class name) so the log is short.

**Test 15 — two `find()` calls, one `select`.** The second `find` never reached the database.
Hibernate remembered the object from the first one. That memory is the **first-level cache**, and
it lasts as long as one transaction.

**Test 16 — count the `select`s.** One for the recipes, then one more for each recipe. That is the
same N+1 you saw in the log at checkpoint 3, pinned down in a test.
