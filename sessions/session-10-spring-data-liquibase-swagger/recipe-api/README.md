# Exercise — The Recipe API, on PostgreSQL

Same project as last session. The application already talks to a database through a Spring Data
repository. Today you add a search that is real SQL, fix the N+1 you saw in the log, move from H2
to PostgreSQL with a migration tool owning the tables, give recipes tags — a many-to-many — and
build one search whose filters are all optional. A documentation page that nobody writes closes
the session as a demo.

**Java 25 · Spring Boot 4.1.1 · Maven · H2, then PostgreSQL.**

**You need PostgreSQL installed and running for checkpoint 2.** Checkpoint 1 still uses H2.

---

## Who you are building for today

**Sunrise Kitchen** asked last session: *"every time you restart the server, every recipe we added
is gone."* The application is on a database now — but it is H2, in memory, so the complaint still
stands. **Today it moves to PostgreSQL**, and by checkpoint 2 a recipe you add is still there after
a restart.

More requests came in with it:

- *"We want to find every recipe that uses a given ingredient."*
- *"We want to label recipes — spicy, vegetarian, quick — and see every recipe that carries a label."*
- *"Let us filter the menu — by cuisine, by time, by label. Any of them, or none."*
- *"The mobile team keeps asking what the API accepts. Can they read it somewhere?"*

## Start here

**This folder has no code in it — only this file.** You bring the code: today continues the project
you finished last session.

1. Copy your Session 9 project into this folder — everything in
   `../../session-09-hibernate-and-jpa/recipe-api/` **except** its `README.md` and `EXTRA-tests.md`.
   From this folder:

   ```bash
   cp -r ../../session-09-hibernate-and-jpa/recipe-api/{pom.xml,src,mvnw,mvnw.cmd,.mvn} .
   ```

   On Windows, copy `pom.xml`, `src`, `mvnw`, `mvnw.cmd` and `.mvn` across in File Explorer.
2. In the copied `pom.xml`, change `<artifactId>recipe-api-jpa</artifactId>` to
   `<artifactId>recipe-api-data</artifactId>`, so IntelliJ does not confuse it with last session's
   project.
3. **Open the `pom.xml`, not the folder**, when you import it into IntelliJ.
4. Run `mvn test` and start the application once before you change anything. It should behave
   exactly as it did at the end of last session.

Your Session 9 folder stays as it is. Everything today goes into the copy.

**Today builds on all four of last session's checkpoints** — including the last one, quick recipes
and paging (`/recipes/quick`, `/recipes/page`, `PageResponse`). Today's checkpoint 4 reuses
`findPage`. If your project does not have it yet, finish that checkpoint first.

---

## What you check today

**`mvn test` stays green at the end of every checkpoint, and no test you already have changes.**

| Checkpoint | What you do | How you check it |
|---|---|---|
| 1 | A JPQL search, the N+1 fix, transactions — on H2 | `mvn test` green · `curl` · count the `select`s |
| 2 | PostgreSQL, with Liquibase owning the tables | `mvn test` green · restart and keep your data · two deliberate breaks |
| 3 | Tags — a many-to-many, added as a new changeset | `mvn test` green · `Run: 1` in the log · `curl` · three tables |
| 4 | One search, every filter optional — Specifications | `mvn test` green · `curl` · the `where` in the log |

No new tests are written in class. **`EXTRA-tests.md`**, next to this file, has six tests for the
repository, the search endpoint, the migration and the tags — for afterwards, if you want them.

### Pace

The slowest is checkpoint 2, because it has two deliberate breaks in it. Swagger is no longer a
checkpoint: it is shown as a demo, and its steps are at the end of this file to do at home.

---

## Checkpoint 1 — Queries, and where the transaction goes

**What the client asked for:** *"We want to find every recipe that uses a given ingredient."*
`GET /recipes/search?ingredient=fish` returns every recipe with fish in it, each one once.

### The repository

Replace `repository/RecipeRepository.java`. Your two derived queries stay; two methods are new:

```java
package com.sunrise.recipeapi.repository;

import com.sunrise.recipeapi.domain.Recipe;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    // Derived queries: Spring Data reads the method NAME and writes the SQL
    List<Recipe> findByCuisineIgnoreCase(String cuisine);

    List<Recipe> findByMinutesLessThanEqualOrderByMinutesAsc(int minutes);

    // A JPQL query: you write it, against the entities -- Recipe, not the recipes table
    @Query("""
            select distinct r from Recipe r
            join r.ingredients i
            where lower(i.name) = lower(:ingredient)
            """)
    List<Recipe> findByIngredient(@Param("ingredient") String ingredient);

    // The N+1 fix: load every recipe AND its ingredients in one SELECT
    @Override
    @EntityGraph(attributePaths = "ingredients")
    List<Recipe> findAll();
}
```

| Method | What it is |
|---|---|
| `findByIngredient` | **JPQL** you wrote. It queries *entities* — `Recipe r join r.ingredients` — not tables. `distinct` because a recipe with fish twice would otherwise come back twice |
| `findAll()` with `@EntityGraph` | The same method `JpaRepository` already has, re-declared to add one instruction: *load the ingredients in the same `select`*. That is the N+1 fix |
| The two `findBy...` methods | Unchanged — still queries written from the method name |

### The service

**`service/RecipeService.java` — add one method:**

```java
List<Recipe> findByIngredient(String ingredient);
```

**`service/RecipeServiceImpl.java` — add that method, and put `@Transactional` on the three methods
that write.** Nothing else in the class changes:

```java
  @Override
  public List<Recipe> findByIngredient(String ingredient) {
    return recipeRepository.findByIngredient(ingredient);
  }

  @Override
  @Transactional
  public Recipe create(Recipe recipe) {
    return recipeRepository.save(recipe);
  }

  @Override
  @Transactional
  public Recipe replace(Long id, Recipe recipe) {
    if (!recipeRepository.existsById(id)) {
      throw new RecipeNotFoundException("recipe id: " + id + " not found");
    }
    recipe.setId(id);
    return recipeRepository.save(recipe);
  }

  @Override
  @Transactional
  public void delete(Long id) {
    recipeRepository.deleteById(id);
  }
```

One new import — **Spring's `@Transactional`, not the `jakarta` one** IntelliJ also offers:

```java
import org.springframework.transaction.annotation.Transactional;
```

`@Transactional` goes on the methods that **write**: everything inside one of them is saved
together, or not at all. The reads — `findAll`, `findById`, `findByIngredient`, `findQuick`,
`findPage` — work without it.

### The endpoint

Add this method to `RecipeController`, above `getRecipeById`:

```java
  // Every recipe that uses an ingredient -- the JPQL query in RecipeRepository
  @GetMapping("/search")
  public ResponseEntity<List<RecipeResponse>> searchByIngredient(@RequestParam String ingredient) {
    List<RecipeResponse> responses =
        recipeService.findByIngredient(ingredient).stream()
            .map(recipeMapper::toRecipeResponse)
            .toList();
    return ResponseEntity.ok(responses);
  }
```

`/recipes/search` and `/recipes/{id}` do not fight: Spring picks the path with the fixed word over
the one with a variable.

### Your turn

1. `mvn test` — **green.**
2. Start the application and add two recipes (the database is still in memory, so it starts empty):

   ```bash
   curl -X POST http://localhost:8080/recipes -H "Content-Type: application/json" \
     -d '{"title":"Fish Amok","cuisine":"khmer","minutes":45,"servings":4,"ingredients":["fish","coconut milk","kroeung"]}'

   curl -X POST http://localhost:8080/recipes -H "Content-Type: application/json" \
     -d '{"title":"Nom Banh Chok","cuisine":"khmer","minutes":60,"servings":4,"ingredients":["rice noodles","fish","lemongrass"]}'
   ```

3. Search:

   ```bash
   curl "http://localhost:8080/recipes/search?ingredient=FISH"
   ```

   Both recipes, once each. In the log, the `select` has `join ingredients` and
   `where lower(i1_0.name)=lower(?)` in it — Hibernate's translation of your JPQL.
4. `curl "http://localhost:8080/recipes?cuisine=KHMER"` and find `where upper(r1_0.cuisine)=upper(?)`
   in the log. You never wrote that. Spring Data read the words `CuisineIgnoreCase` and wrote it.
5. `curl http://localhost:8080/recipes` and count the `select`s. **One**, with a `left join
   ingredients` in it. Last session's three became one.

---

## Checkpoint 2 — PostgreSQL, and the tables belong to Liquibase now

**What the client asked for:** *"Recipes we add must still be there after a restart — and the three
house recipes should be there from the start."*

Two things have been true until now, and both end here:

- **H2 lived in memory.** Perfect for learning — nothing to install — and it forgets everything
  when the application stops. A real application needs a real database server: **PostgreSQL**.
- **Hibernate created the tables** at every start (`create-drop`) and threw them away at every
  stop. From here on, **the tables are built by numbered SQL files, run once each, in order** —
  and Hibernate only checks that they match.

### Before you start: a database to connect to

PostgreSQL must be installed and running on your machine. Create an empty database for the
project — in `psql`, pgAdmin or DataGrip:

```sql
create database recipe_db;
```

That is all. **No tables.** Liquibase builds those.

### The dependencies

Add PostgreSQL's driver and the Liquibase starter:

```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-liquibase</artifactId>
</dependency>
```

**Change H2's scope from `runtime` to `test`**, and move it down next to the test starters:

```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

and **delete the `spring-boot-h2console` dependency**. The application no longer runs on H2, so
there is no console to open.

H2 stays for one job: **the extra tests** in `EXTRA-tests.md`. `@DataJpaTest` swaps the real
database for a fresh in-memory one, so those tests never need PostgreSQL running and never touch
your real data. `contextLoads` is different: it starts the whole application, so from this
checkpoint on it needs PostgreSQL running.

### The changelog

Three files under `src/main/resources/db/changelog/`. The master list:

`db.changelog-master.yaml`

```yaml
# The list of changes, in the order they run. Liquibase reads this file at startup.
# Add a new file at the bottom. Never edit a file that has already run.
databaseChangeLog:
  - include:
      file: db/changelog/001-create-recipes-and-ingredients.sql
  - include:
      file: db/changelog/002-seed-recipes.sql
```

The first changeset — the same tables Hibernate printed last session, now written down and owned
by you:

`001-create-recipes-and-ingredients.sql`

```sql
--liquibase formatted sql

--changeset sunrise:001-create-recipes-and-ingredients
create table recipes (
    id             bigint generated by default as identity primary key,
    title          varchar(100) not null,
    cuisine        varchar(50)  not null,
    minutes        integer      not null,
    servings       integer      not null,
    internal_notes varchar(255)
);

create table ingredients (
    id        bigint generated by default as identity primary key,
    name      varchar(100) not null,
    recipe_id bigint       not null references recipes (id)
);
--rollback drop table ingredients;
--rollback drop table recipes;
```

The second changeset — the three house recipes, which used to be seeded by the class you deleted:

`002-seed-recipes.sql`

```sql
--liquibase formatted sql

--changeset sunrise:002-seed-recipes
insert into recipes (title, cuisine, minutes, servings, internal_notes) values ('Fish Amok', 'khmer',   45, 4, 'supplier: central market, pays late');
insert into recipes (title, cuisine, minutes, servings, internal_notes) values ('Lok Lak',   'khmer',   25, 2, 'supplier: central market');
insert into recipes (title, cuisine, minutes, servings, internal_notes) values ('Carbonara', 'italian', 20, 2, 'supplier: deli, margin 12%');

insert into ingredients (name, recipe_id) values ('fish',          (select id from recipes where title = 'Fish Amok'));
insert into ingredients (name, recipe_id) values ('coconut milk',  (select id from recipes where title = 'Fish Amok'));
insert into ingredients (name, recipe_id) values ('kroeung',       (select id from recipes where title = 'Fish Amok'));
insert into ingredients (name, recipe_id) values ('beef',          (select id from recipes where title = 'Lok Lak'));
insert into ingredients (name, recipe_id) values ('lettuce',       (select id from recipes where title = 'Lok Lak'));
insert into ingredients (name, recipe_id) values ('lime',          (select id from recipes where title = 'Lok Lak'));
insert into ingredients (name, recipe_id) values ('kampot pepper', (select id from recipes where title = 'Lok Lak'));
insert into ingredients (name, recipe_id) values ('spaghetti',     (select id from recipes where title = 'Carbonara'));
insert into ingredients (name, recipe_id) values ('guanciale',     (select id from recipes where title = 'Carbonara'));
insert into ingredients (name, recipe_id) values ('egg',           (select id from recipes where title = 'Carbonara'));
insert into ingredients (name, recipe_id) values ('pecorino',      (select id from recipes where title = 'Carbonara'));
--rollback delete from ingredients;
--rollback delete from recipes;
```

Each ingredient finds its recipe by title (`select id from recipes where title = ...`), because the
database hands out the ids and you do not know them in advance.

### The properties

Replace `src/main/resources/application.properties`:

```properties
spring.application.name=recipe-api

# Spring MVC own exceptions answer as RFC 9457 Problem Details.
# Exceptions you wrote need the @RestControllerAdvice.
spring.mvc.problemdetails.enabled=true

# --- The database: PostgreSQL, running on your machine. It keeps the data when the app stops ---
spring.datasource.url=jdbc:postgresql://localhost:5432/recipe_db
spring.datasource.username=postgres
spring.datasource.password=password

# --- Hibernate ---
# validate: Hibernate no longer creates anything. It checks, at startup, that the
# tables Liquibase built match the entities -- and refuses to start if they do not.
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# --- Liquibase ---
# This is the default location; written out so you can see where it looks.
spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.yaml

# --- The connection pool (HikariCP, Boot's default) ---
spring.datasource.hikari.maximum-pool-size=5
spring.datasource.hikari.connection-timeout=5000
```

**Use your own PostgreSQL username and password** if they are not `postgres` / `password`.

Three things changed:

- **`spring.datasource.url`** — `jdbc:postgresql://localhost:5432/recipe_db`: PostgreSQL, on this
  machine, port 5432, the database you just created. Compare it with `jdbc:h2:mem:recipes`. That
  line, the driver in the POM, and nothing else is what it takes to change database. **Not one
  line of Java moved.**
- **`ddl-auto=validate`** — Hibernate creates nothing any more. It compares the tables to the
  entities at startup and refuses to start if they differ.
- **The H2 console line is gone.**

### Your turn

1. `mvn test` — **green**, with **PostgreSQL running**: `contextLoads` starts the whole application,
   so it connects to the database — and runs Liquibase — exactly as the application does.
2. Start the application. In the log:

   ```
   Database driver: PostgreSQL JDBC Driver
   Database version: 16.2
   Running Changeset: db/changelog/001-create-recipes-and-ingredients.sql::001-create-recipes-and-ingredients::sunrise
   Running Changeset: db/changelog/002-seed-recipes.sql::002-seed-recipes::sunrise
   UPDATE SUMMARY
   Run:                          2
   Previously run:               0
   ```

   (Your version number may differ.)
3. `curl http://localhost:8080/recipes` — Fish Amok, Lok Lak, Carbonara. POST one of your own.
4. **Stop the application and start it again.** Now the log says:

   ```
   Run:                          0
   Previously run:               2
   ```

   and `GET /recipes` still has **your** recipe in it. The client's request is done.
5. Look inside the database with `psql` (or pgAdmin / DataGrip):

   ```
   recipe_db=# \dt
    public | databasechangelog     | table | postgres
    public | databasechangeloglock | table | postgres
    public | ingredients           | table | postgres
    public | recipes               | table | postgres

   recipe_db=# select id, author, filename from databasechangelog;
    001-create-recipes-and-ingredients | sunrise | db/changelog/001-create-recipes-and-ingredients.sql
    002-seed-recipes                   | sunrise | db/changelog/002-seed-recipes.sql
   ```

   Two tables of yours, and two of Liquibase's. `databasechangelog` is its own record of what it
   has run — that is how it knew to skip both changesets on the second start.

### Break it on purpose — twice

**First: edit a changeset that has already run.** In `002-seed-recipes.sql`, change Lok Lak's `25`
minutes to `30`. Restart. The application refuses to start:

```
Validation Failed:
     1 changesets check sum
          db/changelog/002-seed-recipes.sql::002-seed-recipes::sunrise was: 9:b78109d6ac20a3492e38a772dc2193c8 but is now: 9:af6c64b55abb01bff6653b06df785f50
```

Liquibase stored a fingerprint of every changeset it ran. Edit one, and the fingerprint no longer
matches — because the databases that already ran the old version would now disagree with the ones
that run the new one. **A changeset that has run is never edited. You add a new one.** Put the `25`
back and restart.

**Second: change an entity without a changeset.** Add one field to `Recipe`:

```java
private String difficulty;
```

Restart:

```
Schema validation: missing column [difficulty] in table [recipes]
```

That is `ddl-auto=validate` doing its job: the entity and the table disagree, so nothing starts.
The fix, in a real project, is a new changeset that adds the column — which is exactly how
checkpoint 3 adds its tables. **Remove the field.**

---

## Checkpoint 3 — Tags: a many-to-many, added the right way

**What the client asked for:** *"We want to label recipes — spicy, vegetarian, quick — and see every
recipe that carries a label."*

One recipe has many tags. One tag is on many recipes. Neither table can hold the other's id, so the
link is a **third table** with one row per pair. And because the database already exists, the new
tables arrive as a **third changeset** — you do not touch `001`.

### The changeset

Create `src/main/resources/db/changelog/003-create-tags.sql`:

```sql
--liquibase formatted sql

--changeset sunrise:003-create-tags
create table tags (
    id   bigint generated by default as identity primary key,
    name varchar(50) not null unique
);

create table recipe_tags (
    recipe_id bigint not null references recipes (id),
    tag_id    bigint not null references tags (id),
    primary key (recipe_id, tag_id)
);
--rollback drop table recipe_tags;
--rollback drop table tags;
```

and add it at the bottom of `db.changelog-master.yaml`:

```yaml
# The list of changes, in the order they run. Liquibase reads this file at startup.
# Add a new file at the bottom. Never edit a file that has already run.
databaseChangeLog:
  - include:
      file: db/changelog/001-create-recipes-and-ingredients.sql
  - include:
      file: db/changelog/002-seed-recipes.sql
  - include:
      file: db/changelog/003-create-tags.sql
```

`recipe_tags` has no id of its own. Its two columns together are the primary key — which also means
the same recipe cannot carry the same tag twice.

### The entity

Create `domain/Tag.java`:

```java
package com.sunrise.recipeapi.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A label a recipe can carry: "spicy", "vegetarian", "quick".
 *
 * A tag does not belong to one recipe. Many recipes share it, and one recipe
 * has many of them -- which is why there is no recipe_id column here. The link
 * lives in a third table, recipe_tags.
 */
@Entity
@Table(name = "tags")
@Getter
@Setter
@NoArgsConstructor
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    public Tag(String name) {
        this.name = name;
    }
}
```

**`domain/Recipe.java` — add one field.** Nothing else in the class changes:

```java
  // Many recipes, many tags. Neither table can hold the other's id, so the
  // link is a row in a third table: recipe_tags (recipe_id, tag_id)
  @ManyToMany
  @JoinTable(
      name = "recipe_tags",
      joinColumns = @JoinColumn(name = "recipe_id"),
      inverseJoinColumns = @JoinColumn(name = "tag_id"))
  private Set<Tag> tags = new LinkedHashSet<>();
```

Two new imports (the annotations are already covered by `jakarta.persistence.*`):

```java
import java.util.LinkedHashSet;
import java.util.Set;
```

| On the `tags` field | Means |
|---|---|
| `@ManyToMany` | Many recipes, many tags |
| `@JoinTable(name = "recipe_tags", ...)` | The table that holds the pairs |
| `joinColumns = @JoinColumn(name = "recipe_id")` | The column pointing at *this* entity |
| `inverseJoinColumns = @JoinColumn(name = "tag_id")` | The column pointing at the *other* one |
| `Set<Tag>` | A recipe has a tag or it does not — never twice |

There is no `cascade` here, on purpose. Deleting a recipe must not delete "spicy" — other recipes
use it. Hibernate removes the recipe's rows in `recipe_tags` and leaves the tag alone.

### The mapper — one line

`Recipe` has a new field, and the client does not send tags when creating a recipe. MapStruct
notices: the build now prints `Unmapped target property: "tags"` for `toRecipe`. Say so in writing —
one more line above `toRecipe` in `mapper/RecipeMapper.java`:

```java
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ingredients", ignore = true)
    @Mapping(target = "tags", ignore = true)
    Recipe toRecipe(CreateRecipeRequest request);
```

### The repositories

Create `repository/TagRepository.java`:

```java
package com.sunrise.recipeapi.repository;

import com.sunrise.recipeapi.domain.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByNameIgnoreCase(String name);
}
```

**`repository/RecipeRepository.java` — add one derived query.** Read the name: `Tags` is the field
on `Recipe`, `Name` is the field on `Tag`. Spring Data walks the relationship and writes both joins:

```java
    // Derived again -- through the relationship: Recipe.tags, then Tag.name
    List<Recipe> findByTagsNameIgnoreCase(String name);
```

### The services

**`service/RecipeServiceImpl.java` — only `replace` changes.** A `PUT` carries no tags, so the recipe
must keep the ones it has. Until now `replace` only asked whether the recipe exists; now it loads
it, to copy its tags across:

```java
  @Override
  @Transactional
  public Recipe replace(Long id, Recipe recipe) {
    // 404 if it is not there -- and we need the tags it already has
    Recipe existing = findById(id);
    recipe.setId(id);
    // The client does not send tags, so the recipe keeps its own
    recipe.setTags(new LinkedHashSet<>(existing.getTags()));
    return recipeRepository.save(recipe);
  }
```

One new import: `java.util.LinkedHashSet`.

Create `service/TagService.java`:

```java
package com.sunrise.recipeapi.service;

import com.sunrise.recipeapi.domain.Recipe;
import com.sunrise.recipeapi.domain.Tag;
import com.sunrise.recipeapi.repository.RecipeRepository;
import com.sunrise.recipeapi.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Tagging recipes. Its own service, so RecipeServiceImpl keeps one job: recipes.
 */
@Service
@RequiredArgsConstructor
public class TagService {

    private final RecipeRepository recipeRepository;
    private final TagRepository tagRepository;

    /** Find the tag or create it, then link it. One transaction: both happen, or neither. */
    @Transactional
    public void addTag(Long recipeId, String name) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RecipeNotFoundException("recipe id: " + recipeId + " not found"));
        Tag tag = tagRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> tagRepository.save(new Tag(name.toLowerCase())));
        recipe.getTags().add(tag);
        recipeRepository.save(recipe);
    }

    /** From the recipe to its tags. */
    @Transactional(readOnly = true)
    public List<String> tagsOf(Long recipeId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RecipeNotFoundException("recipe id: " + recipeId + " not found"));
        Set<Tag> tags = recipe.getTags();
        return tags.stream().map(Tag::getName).toList();
    }

    /** From the tag to its recipes -- the other direction of the same relationship. */
    public List<Recipe> recipesTagged(String name) {
        return recipeRepository.findByTagsNameIgnoreCase(name);
    }
}
```

Tagging gets its own service and controller so that `RecipeServiceImpl` keeps its one collaborator
and `RecipeController` stays as it is.

### The controller

Create `controller/TagController.java`:

```java
package com.sunrise.recipeapi.controller;

import com.sunrise.recipeapi.dto.RecipeResponse;
import com.sunrise.recipeapi.mapper.RecipeMapper;
import com.sunrise.recipeapi.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Tags over HTTP. A separate controller, so RecipeController stays as it is.
 */
@RestController
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;
    private final RecipeMapper recipeMapper;

    /** PUT, not POST: tagging the same recipe twice leaves it tagged once. */
    @PutMapping("/recipes/{id}/tags/{name}")
    public ResponseEntity<Void> addTag(@PathVariable Long id, @PathVariable String name) {
        tagService.addTag(id, name);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/recipes/{id}/tags")
    public List<String> tagsOf(@PathVariable Long id) {
        return tagService.tagsOf(id);
    }

    @GetMapping("/tags/{name}/recipes")
    public List<RecipeResponse> recipesTagged(@PathVariable String name) {
        return tagService.recipesTagged(name).stream().map(recipeMapper::toRecipeResponse).toList();
    }
}
```

### Your turn

1. `mvn test` — **green.**
2. Start the application. Liquibase runs **only the new changeset**:

   ```
   Running Changeset: db/changelog/003-create-tags.sql::003-create-tags::sunrise
   Run:                          1
   Previously run:               2
   ```

   Your recipes are untouched. That is the difference between a migration and `create-drop`.
3. Tag two recipes with the same tag, and one of them with a second:

   ```bash
   curl -X PUT http://localhost:8080/recipes/1/tags/spicy
   curl -X PUT http://localhost:8080/recipes/2/tags/spicy
   curl -X PUT http://localhost:8080/recipes/1/tags/classic
   ```

   Each answers `204`. In the log, the link is one small `insert`:

   ```sql
   insert into recipe_tags (recipe_id, tag_id) values (?, ?)
   ```

4. Both directions of the same relationship:

   ```bash
   curl http://localhost:8080/recipes/1/tags         # ["spicy","classic"]
   curl http://localhost:8080/tags/spicy/recipes     # Fish Amok and Lok Lak
   ```

5. Look at the three tables:

   ```
   recipe_db=# select * from tags;
     1 | spicy
     2 | classic

   recipe_db=# select * from recipe_tags;
     1 | 1
     2 | 1
     1 | 2
   ```

   **Two tags, three links.** "spicy" exists once and is shared — that is what many-to-many means.
6. Run the first `curl` again. Still `204`, still three rows in `recipe_tags`. A `Set`, and a
   primary key on the pair: tagging twice changes nothing.
7. `curl -X DELETE http://localhost:8080/recipes/2`, then `select * from tags;`. The recipe and its
   link are gone. **"spicy" is still there.**

---

## Checkpoint 4 — One search, every filter optional

**What the client asked for:** *"Let us filter the menu — by cuisine, by how long it takes, by
label. Any of them, all of them, or none. And a page at a time, like the list."*

One endpoint:

| Request | Answers |
|---|---|
| `GET /recipes/filter` | every recipe, first page |
| `GET /recipes/filter?cuisine=khmer` | Khmer recipes |
| `GET /recipes/filter?maxMinutes=30` | 30 minutes or less |
| `GET /recipes/filter?tag=spicy` | carrying the tag "spicy" |
| `GET /recipes/filter?cuisine=khmer&maxMinutes=30&tag=spicy` | all three at once |

Each filter is used **only if the client sent it**. `page`, `size` and `sort` work exactly as they
do on `/recipes/page`.

### The code

**1. New file `repository/RecipeSpecifications.java`** — one small method for each filter. Each
returns one `where` condition:

```java
package com.sunrise.recipeapi.repository;

import com.sunrise.recipeapi.domain.Recipe;
import org.springframework.data.jpa.domain.Specification;

/**
 * One method per filter. Each returns ONE where-condition; the service joins
 * the ones the client asked for.
 *
 * root = the recipe being tested ("r" in JPQL). cb builds the comparison.
 * The names in quotes are FIELDS of the entity, not columns.
 */
public final class RecipeSpecifications {

    private RecipeSpecifications() {
    }

    /** lower(r.cuisine) = ? */
    public static Specification<Recipe> hasCuisine(String cuisine) {
        return (root, query, cb) -> cb.equal(cb.lower(root.get("cuisine")), cuisine.toLowerCase());
    }

    /** r.minutes <= ? */
    public static Specification<Recipe> takesAtMost(int minutes) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("minutes"), minutes);
    }

    /** join r.tags t ... lower(t.name) = ?   -- the join exists only when this filter is used */
    public static Specification<Recipe> hasTag(String tag) {
        return (root, query, cb) -> cb.equal(cb.lower(root.join("tags").get("name")), tag.toLowerCase());
    }
}
```

**2. `repository/RecipeRepository.java` — one more word on the `extends` line.** Nothing else in
the file changes:

```java
public interface RecipeRepository
        extends JpaRepository<Recipe, Long>, JpaSpecificationExecutor<Recipe> {
```

with the import `org.springframework.data.jpa.repository.JpaSpecificationExecutor`. That one word
gives the repository `findAll(specification, pageable)`.

**3. `service/RecipeService.java` — add one method:**

```java
Page<Recipe> filter(String cuisine, Integer maxMinutes, String tag, int page, int size, String sortBy);
```

`Integer`, not `int`: an `Integer` can be `null`, and `null` is how the service knows the client did
not send that filter.

**4. `service/RecipeServiceImpl.java`.** Last session's `findPage` built its page request in three
lines. The filter needs the same three lines, so they move into one private method that both use:

```java
    @Override
    public Page<Recipe> findPage(int page, int size, String sortBy) {
        return recipeRepository.findAll(pageRequest(page, size, sortBy));
    }

    /** Only the filters the client sent are added. An empty list means "everything". */
    @Override
    public Page<Recipe> filter(String cuisine, Integer maxMinutes, String tag,
                               int page, int size, String sortBy) {
        List<Specification<Recipe>> filters = new ArrayList<>();
        if (cuisine != null) {
            filters.add(RecipeSpecifications.hasCuisine(cuisine));
        }
        if (maxMinutes != null) {
            filters.add(RecipeSpecifications.takesAtMost(maxMinutes));
        }
        if (tag != null) {
            filters.add(RecipeSpecifications.hasTag(tag));
        }
        return recipeRepository.findAll(Specification.allOf(filters), pageRequest(page, size, sortBy));
    }

    /** The two paging rules, in one place: cap the size, and sort only by an allowed field. */
    private Pageable pageRequest(int page, int size, String sortBy) {
        int safeSize = Math.min(size, MAX_PAGE_SIZE);
        String property = SORTABLE.contains(sortBy) ? sortBy : "id";
        Sort sort = Sort.by(property).and(Sort.by("id"));
        return PageRequest.of(page, safeSize, sort);
    }
```

New imports:

```java
import com.sunrise.recipeapi.repository.RecipeSpecifications;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
```

**5. `controller/RecipeController.java` — add one endpoint.** `required = false` on each filter:
leaving one out is not an error.

```java
  // Every filter is optional. The service uses only the ones that were sent
  @GetMapping("/filter")
  public ResponseEntity<PageResponse<RecipeResponse>> filterRecipes(
      @RequestParam(required = false) String cuisine,
      @RequestParam(required = false) Integer maxMinutes,
      @RequestParam(required = false) String tag,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "id") String sort) {
    Page<Recipe> result = recipeService.filter(cuisine, maxMinutes, tag, page, size, sort);
    List<RecipeResponse> content =
        result.getContent().stream().map(recipeMapper::toRecipeResponse).toList();
    return ResponseEntity.ok(
        new PageResponse<>(
            content,
            result.getNumber(),
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages()));
  }
```

### Your turn

1. `mvn test` — **green.**
2. Start the application, and make sure there is something to filter. Add two recipes, and tag
   Lok Lak "spicy" — **use the `id` that the second `curl` answers with** in place of `<id>`:

   ```bash
   curl -X POST http://localhost:8080/recipes -H "Content-Type: application/json" \
     -d '{"title":"Nom Banh Chok","cuisine":"khmer","minutes":60,"servings":4,"ingredients":["rice noodles","fish","lemongrass"]}'

   curl -X POST http://localhost:8080/recipes -H "Content-Type: application/json" \
     -d '{"title":"Lok Lak","cuisine":"khmer","minutes":25,"servings":2,"ingredients":["beef","lettuce","lime","kampot pepper"]}'

   curl -X PUT http://localhost:8080/recipes/<id>/tags/spicy
   ```

3. No filter at all — everything, as a page. **Keep the quotes around every URL from here on:**

   ```bash
   curl "http://localhost:8080/recipes/filter"
   ```

   In the log, the `select ... from recipes` has **no `where`**.
4. One filter, then two:

   ```bash
   curl "http://localhost:8080/recipes/filter?cuisine=KHMER"
   curl "http://localhost:8080/recipes/filter?cuisine=khmer&maxMinutes=30"
   ```

   The first answers every Khmer recipe, in any letter case. The second answers Lok Lak — Fish Amok
   takes 45 minutes. Read the two `where` clauses in the log: the second one grew an `and`.
5. The filter that needs a join:

   ```bash
   curl "http://localhost:8080/recipes/filter?tag=spicy"
   ```

   Fish Amok and Lok Lak. In the log there are now two `join`s — to `recipe_tags`, then to `tags`.
   They were not in the last two queries. **The join is only there when the filter is.**
6. All three, sorted, one per page:

   ```bash
   curl "http://localhost:8080/recipes/filter?cuisine=khmer&maxMinutes=50&tag=spicy&sort=minutes&size=1"
   ```

   Lok Lak, with `"totalElements": 2` and `"totalPages": 2`. Add `&page=1` for Fish Amok.
7. **Count the `select`s for step 5.** The page, the `count`, and one more per recipe for its
   ingredients: the N+1 is back, because this query is not the `findAll()` that carries
   `@EntityGraph`. Add one line to `application.properties`, restart, and count again:

   ```properties
   spring.jpa.properties.hibernate.default_batch_fetch_size=20
   ```

   The ingredients of every recipe on the page now arrive in **one** query. Do not put
   `@EntityGraph` on a paged query to fix this — the lecture says why.

**If you finish early:** add a fourth filter, `ingredient`. The condition is
`cb.equal(cb.lower(root.join("ingredients").get("name")), ingredient.toLowerCase())` — and a recipe
that lists the ingredient twice would come back twice, so that method also needs
`query.distinct(true);` on the line before its `return`.

---

## After class — Documentation nobody writes

*Shown as a demo at the end of the session. Not a checkpoint: do it at home, it takes five minutes.*

**What the client asked for:** *"The mobile team keeps asking what the API accepts. Can they read it
somewhere?"*

### The dependency

Add a version to `<properties>`:

```xml
<springdoc.version>3.0.2</springdoc.version>
```

and the dependency:

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>${springdoc.version}</version>
</dependency>
```

Spring Boot does not manage springdoc's version, so it is written out. **3.x is the line for Spring
Boot 4**; a 2.x version from an older tutorial will not work.

### Document one endpoint properly

In `RecipeController`, only `getRecipeById` changes — three annotations above it:

```java
  @Operation(summary = "Get one recipe by its id")
  @ApiResponse(responseCode = "200", description = "The recipe")
  @ApiResponse(responseCode = "404", description = "No recipe has this id")
  @GetMapping("/{id}")
  public ResponseEntity<RecipeResponse> getRecipeById(@PathVariable Long id) {
    Recipe recipe = recipeService.findById(id);
    return ResponseEntity.ok(recipeMapper.toRecipeResponse(recipe));
  }
```

and two imports:

```java
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
```

Only `getRecipeById` gets the annotations. They are from `io.swagger.v3.oas.annotations`, and they
change nothing about what the endpoint *does* — only what the page says about it.

### Your turn

1. `mvn test` — **still green.** Documentation has no test.
2. Start the application and open **http://localhost:8080/swagger-ui.html**. Every endpoint is
   listed. You wrote none of this page.
3. Open **Schemas → CreateRecipeRequest**. Your validation rules from the REST session are already
   there: `minutes` and `servings` with a minimum of 1, `ingredients` needing at least one item,
   `title` and `cuisine` marked required. The page read your `@Min`, `@NotEmpty` and `@NotBlank`.
4. Open `GET /recipes/{id}` — your summary and your two responses. Compare with
   `DELETE /recipes/{id}`, which has none.
5. Open `GET /recipes/filter`. Every optional filter from checkpoint 4 is a box on the page, and
   none is marked required. Press **Try it out**, type `khmer` into `cuisine`, **Execute**. A real
   request, from the page.
6. The raw description the page is built from is at **http://localhost:8080/v3/api-docs** — JSON,
   in a standard format called OpenAPI. That is what the mobile team would actually import.

---

## If you get stuck

**`No property 'kitchen' found for type 'Recipe'`** — a derived query's name does not match a
field. The word after `findBy` must be a field of `Recipe`, spelled exactly: `findByCuisine...`, not
`findByKitchen`.

**`Validation Failed: 1 changesets check sum`** — a changeset that already ran was edited. Undo the
edit. If you really are experimenting and want to start over, stop the application, then
`drop database recipe_db;` and `create database recipe_db;`.

**`Schema validation: missing column [...]`** or **`wrong column type`** — the entity and the
changeset disagree. The changeset is the truth; change the entity, or add a new changeset.

**`db.changelog-master.yaml does not exist`** — the master changelog is not where Boot looks. It
must be `src/main/resources/db/changelog/db.changelog-master.yaml`, spelled exactly.

**`Connection to localhost:5432 refused`** — PostgreSQL is not running. Start it.

**`FATAL: database "recipe_db" does not exist`** — the `create database recipe_db;` step was skipped.

**`FATAL: password authentication failed for user "postgres"`** — the username or password in
`application.properties` is not the one your PostgreSQL uses.

**`relation "recipes" already exists`** when Liquibase runs — the database is not empty: an earlier
run with `ddl-auto` created the tables. Drop and re-create `recipe_db`.

**The H2 console is a 404 after checkpoint 2** — correct. The application runs on PostgreSQL now;
use `psql`, pgAdmin or DataGrip.

**`LazyInitializationException`** — something read `getIngredients()` after the database session
closed. Keep the mapping in the controller, where it is; Boot keeps the session open until the
response is written, which is also what the `spring.jpa.open-in-view is enabled by default` warning
in your log is about.

**MapStruct warns `Unmapped target property: "tags"`** — the `@Mapping(target = "tags", ignore = true)` line
is missing from `toRecipe`. Checkpoint 3.

**A `PUT /recipes/{id}` removes the recipe's tags** — `replace` is still last session's version. It
must copy the existing tags across before it saves. Checkpoint 3.

**`mvn test` fails with `Connection to localhost:5432 refused`** — from checkpoint 2 on,
`contextLoads` needs PostgreSQL running.

**`Schema validation: missing table [recipe_tags]`** — `003-create-tags.sql` is not listed in
`db.changelog-master.yaml`, so Liquibase never ran it.

**`No property 'tags' found for type 'Recipe'`** — the `tags` field is missing from `Recipe`, but
`findByTagsNameIgnoreCase` is already in the repository.

**`/recipes/filter` answers 500 when `maxMinutes` is left out** — it is declared `int`. It must be
`Integer` in the controller *and* in the service, so that "not sent" can be `null`.

**`/recipes/filter` answers 500 only when one particular filter is used** — the field name inside
that `Specification` is misspelt. It is text, so the compiler cannot check it: `"cuisine"`,
`"minutes"`, `"tags"`, `"name"`.

**`reference to findAll is ambiguous`** — `findAll(null, pageable)` was called with a literal
`null`. Pass `Specification.allOf(filters)`; an empty list already means "no condition".

**`curl` prints only part of the answer, or `[1] 12345`** — the URL has an `&` in it and no quotes
around it.

**The Swagger page is a 404** — the dependency is missing, or it is a 2.x version. Use 3.0.2.

**`/recipes/search` returns 400** — the `ingredient` parameter is missing from the URL.
