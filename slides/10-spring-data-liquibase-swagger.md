---
layout: section
---

# Spring Data JPA, Liquibase & Swagger

Delete the repository. Keep the tables. Publish the contract.
---
layout: default
---

# Learning Objectives

<v-clicks>

- Write a query in JPQL — and know when a method name stops being enough
- Use a projection, a native query and `@Modifying` — and say what each one costs
- Fix an N+1 with one annotation, and prove it in the log
- Put `@Transactional` on the right layer
- Move an application from H2 to PostgreSQL without changing a line of Java
- Hand the schema to Liquibase — and never edit a changeset that has run
- Map a many-to-many relationship, and say why it needs a third table
- Build one search whose filters are all optional, with Specifications
- Publish an API description that nobody writes

</v-clicks>
---
layout: section
---

# Who you are building for today
---
layout: default
---

# Five requests

<v-clicks>

- *"Every time you restart the server, every recipe we added is gone."* — still open
- *"We want to find every recipe that uses a given ingredient."*
- *"We want to label recipes — and see every recipe that carries a label."*
- *"Let us filter the menu — by cuisine, by time, by label. Any of them, or none."*
- *"The mobile team keeps asking what the API accepts. Can they read it somewhere?"*

</v-clicks>

<v-click>

<div class="mt-8 text-xl">

Last session the application moved to a database — an in-memory one. **Today: PostgreSQL.**

</div>

</v-click>
---
layout: default
---

# Where you left it

<v-clicks>

- `Recipe` and `Ingredient` are entities
- `RecipeRepository` is a Spring Data interface — the application reads and writes H2
- Restart, and every recipe is gone. H2 is in memory
- Derived queries, and `GET /recipes/page` — a page at a time
- `GET /recipes` sends one query per recipe
- `mvn test` green

</v-clicks>

<v-click>

<div class="mt-6 text-xl font-bold">

Today fixes the restart and the query count — and answers three new requests.

</div>

</v-click>
---
layout: default
---

# Today, in four steps

| Checkpoint | What you do | Database |
|---|---|---|
| 1 | A JPQL search, the N+1 fix, transactions | H2 |
| 2 | PostgreSQL, with Liquibase owning the tables | **PostgreSQL** |
| 3 | Tags — a many-to-many, added as a new changeset | PostgreSQL |
| 4 | One search, every filter optional — Specifications | PostgreSQL |

<v-click>

<div class="mt-6 text-lg">

**`mvn test` stays green at every checkpoint.** Swagger closes the session as a demo.

</div>

</v-click>
---
layout: default
---

# H2 first, then PostgreSQL

<div class="grid grid-cols-2 gap-8 mt-6">
<div class="p-6 rounded-lg border-2 border-gray-400">

### Checkpoint 1 — H2

Inside your application. In memory.

Nothing to install — and it forgets on restart.

</div>
<div class="p-6 rounded-lg border-2 border-blue-500">

### Checkpoints 2–4 — PostgreSQL

Its own server, on your machine.

**It keeps the data.** That is the client's first request.

</div>
</div>

<v-click>

<div class="mt-6 text-lg">

You need PostgreSQL **installed and running** before checkpoint 2.

</div>

</v-click>
---
layout: section
---

# Two ways to write a query
---
layout: default
---

# Last session: the name is the query

```java
List<Recipe> findByCuisineIgnoreCase(String cuisine);

List<Recipe> findByMinutesLessThanEqualOrderByMinutesAsc(int minutes);
```

<v-clicks>

- A **derived query** — Spring Data reads the method name and writes the SQL
- `LessThan` · `Between` · `Containing` · `In` · `OrderBy` · `Top3` · `countBy` · `existsBy`
- Misspell a field and the application does not start

</v-clicks>

<v-click>

<div class="mt-6 text-xl">

Perfect for one or two conditions on one table. Today: what to use when it is not enough.

</div>

</v-click>
---
layout: default
---

# When the name becomes a sentence

```java
List<Recipe> findDistinctByIngredientsNameIgnoreCase(String name);
```

<v-click>

<div class="mt-6 text-xl">

It works. Nobody wants to read it.

</div>

</v-click>

<v-click>

```java
@Query("""
        select distinct r from Recipe r
        join r.ingredients i
        where lower(i.name) = lower(:ingredient)
        """)
List<Recipe> findByIngredient(@Param("ingredient") String ingredient);
```

</v-click>
---
layout: default
---

# The JPQL, line by line

```java {all|2|3|4|6}
@Query("""
        select distinct r from Recipe r
        join r.ingredients i
        where lower(i.name) = lower(:ingredient)
        """)
List<Recipe> findByIngredient(@Param("ingredient") String ingredient);
```

- **Line 2** — recipes, each one **once**, even if it matches twice
- **Line 3** — follow the `ingredients` **field** to the other entity
- **Line 4** — compare names, ignoring upper and lower case
- **Line 6** — `@Param("ingredient")` fills `:ingredient`
---
layout: default
---

# JPQL is not SQL

| JPQL | SQL |
|---|---|
| `from Recipe r` — the **entity** | `from recipes` — the table |
| `join r.ingredients i` — follow the **field** | `join ... on ingredients.recipe_id = recipes.id` |
| `i.name` — a **field** | `name` — a column |

<v-clicks>

- You write about your classes. Hibernate writes the SQL for whichever database
- `:ingredient` is a **parameter**. Never glue strings into a query

</v-clicks>
---
layout: default
---

# What Hibernate sent

```sql
select r1_0.id, r1_0.cuisine, r1_0.internal_notes, r1_0.minutes, r1_0.servings, r1_0.title
from recipes r1_0
join ingredients i1_0 on r1_0.id=i1_0.recipe_id
where lower(i1_0.name)=lower(?)
```

<v-clicks>

- You wrote `join r.ingredients i`. It wrote the `on r1_0.id=i1_0.recipe_id`
- It knew the foreign key from your `@JoinColumn`
- On PostgreSQL it writes PostgreSQL's SQL. You change nothing

</v-clicks>
---
layout: default
---

# A parameter, never glue

```java
// never
"... where i.name = '" + ingredient + "'"
```

<v-click>

<div class="mt-4 text-lg">

Someone types `' or '1'='1` into your search box — and your query is now their query.
That is **SQL injection**.

</div>

</v-click>

<v-click>

```java
// always
"... where lower(i.name) = lower(:ingredient)"
```

<div class="mt-4 text-lg">

A parameter is sent **separately** from the query. It can only ever be a value.

</div>

</v-click>
---
layout: default
---

# Why the repository, not the service?

<v-click>

<div class="mt-8 text-2xl">

It is a question about **where the data is kept**.

</div>

</v-click>

<v-click>

<div class="mt-6 text-xl">

The service decides *whether* to ask. The repository knows *how*.

</div>

</v-click>
---
layout: default
---

# Through the layers

```java
// repository — how to ask the database
List<Recipe> findByIngredient(@Param("ingredient") String ingredient);
```

```java
// service — the application's vocabulary
public List<Recipe> findByIngredient(String ingredient) {
    return recipeRepository.findByIngredient(ingredient);
}
```

```java
// controller — HTTP
@GetMapping("/search")
public ResponseEntity<List<RecipeResponse>> searchByIngredient(@RequestParam String ingredient) {
    List<RecipeResponse> responses =
        recipeService.findByIngredient(ingredient).stream()
            .map(recipeMapper::toRecipeResponse)
            .toList();
    return ResponseEntity.ok(responses);
}
```
---
layout: default
---

# `/search` and `/{id}` do not fight

```java
@GetMapping("/search")     // GET /recipes/search?ingredient=fish
@GetMapping("/{id}")       // GET /recipes/1
```

<v-click>

<div class="mt-6 text-xl">

Spring picks the path with the **fixed word** over the one with a variable.

</div>

</v-click>

<v-click>

```bash
curl "http://localhost:8080/recipes/search?ingredient=FISH"
```

```json
[{"id":1,"title":"Fish Amok", ...},{"id":2,"title":"Nom Banh Chok", ...}]
```

</v-click>
---
layout: default
---

# Not the whole recipe — a projection

<div class="mt-2 text-lg">

A menu page shows a title and a time. Why load six columns and every ingredient?

</div>

```java
public record RecipeSummary(Long id, String title, int minutes) {
}
```

<v-click>

```java
@Query("""
        select new com.sunrise.recipeapi.dto.RecipeSummary(r.id, r.title, r.minutes)
        from Recipe r
        order by r.title
        """)
List<RecipeSummary> findSummaries();
```

</v-click>

<v-click>

```sql
select r1_0.id, r1_0.title, r1_0.minutes from recipes r1_0 order by r1_0.title
```

</v-click>
---
layout: default
---

# What a projection buys you

<v-clicks>

- **Three columns** instead of six — and no ingredients, so no N+1 at all
- `select new …(…)` calls the record's constructor, once per row
- The class name is written **in full**, package included. JPQL has no imports
- What comes back is **not an entity**. Change it and nothing is saved

</v-clicks>

<v-click>

<div class="mt-6 text-lg">

Use it for lists and reports. Use the entity when you are going to change something.

</div>

</v-click>
---
layout: default
---

# When you want the SQL itself — native

```java
@Query(value = "select * from recipes order by random() limit 3", nativeQuery = true)
List<Recipe> findThreeAtRandom();
```

<v-clicks>

- `nativeQuery = true` — this is **SQL**, sent exactly as you wrote it
- **Table** and **column** names now: `recipes`, `internal_notes` — not `Recipe`, `internalNotes`
- For what only your database can do, or SQL somebody hands you ready-made

</v-clicks>

<v-click>

<div class="mt-6 text-lg">

The price: it is written for **one** database. Move to another — or run your tests on H2 — and it may not work. Reach for JPQL first.

</div>

</v-click>
---
layout: default
---

# Paging works here too

```java
@Query("""
        select distinct r from Recipe r
        join r.ingredients i
        where lower(i.name) = lower(:ingredient)
        """)
Page<Recipe> findByIngredient(@Param("ingredient") String ingredient, Pageable pageable);
```

<v-clicks>

- A `Pageable` as the **last** parameter, a `Page` as the return type — as last session
- Spring Data writes the `count` query for you, from yours
- Sorting comes from the `Pageable`. Leave `order by` out of the JPQL

</v-clicks>
---
layout: default
---

# Last session's cliffhanger

<div class="mt-4 text-xl">

`GET /recipes` — two recipes, three `select`s. One more for every recipe you add.

</div>

<v-click>

```java
@Override
@EntityGraph(attributePaths = "ingredients")
List<Recipe> findAll();
```

</v-click>

<v-click>

```sql
select r1_0.id, r1_0.cuisine, i1_0.recipe_id, i1_0.id, i1_0.name, ...
from recipes r1_0
left join ingredients i1_0 on r1_0.id=i1_0.recipe_id
```

</v-click>

<v-click>

<div class="mt-4 text-xl font-bold">

Three became one. A hundred and one would also become one.

</div>

</v-click>
---
layout: default
---

# Why declare `findAll` again?

<v-clicks>

- `findAll()` already exists — you inherited it
- Declaring it again changes nothing about what it returns
- It gives you somewhere to **put an annotation**
- `@EntityGraph(attributePaths = "ingredients")` — *"when you load these, load that field too"*

</v-clicks>

<v-click>

<div class="mt-6 text-lg">

The ingredients are still `LAZY` everywhere else. Only this one query loads them eagerly — because
this one always needs them.

</div>

</v-click>
---
layout: default
---

# The same fix, written in JPQL

```java
@Query("select distinct r from Recipe r left join fetch r.ingredients")
List<Recipe> findAllWithIngredients();
```

<v-clicks>

- `join` — use the ingredients to **filter**
- `join fetch` — **load** the ingredients, in the same query
- `left` — keep a recipe that has no ingredients

</v-clicks>

<v-click>

<div class="mt-6 text-lg">

`@EntityGraph` for a query Spring Data wrote. `join fetch` for a query you wrote. Same result.

</div>

</v-click>
---
layout: default
---

# One rule: do not fetch a list onto a page

```java
@EntityGraph(attributePaths = "ingredients")
Page<Recipe> findAll(Pageable pageable);        // looks right. Is not
```

<v-clicks>

- One recipe with four ingredients is **four rows** in the joined result
- "Two rows" is no longer "two recipes" — the database cannot cut the page
- Hibernate loads **every** recipe, cuts in memory, and warns you in the log

</v-clicks>

<v-click>

```properties
spring.jpa.properties.hibernate.default_batch_fetch_size=20
```

For pages: the ingredients of every recipe on the page, in **one** extra query.

</v-click>
---
layout: section
---

# Where the transaction goes
---
layout: default
---

# What a transaction is

<div class="mt-4 text-xl">

A group of database changes that **all succeed, or none do.**

</div>

<v-click>

```sql
insert into recipes ...        -- 1
insert into ingredients ...    -- 2
insert into ingredients ...    -- 3   ← fails here
```

</v-click>

<v-clicks>

- **Without** a transaction: a recipe with one ingredient is left in the table
- **With** one: steps 1 and 2 are undone — a **rollback**. As if nothing happened
- When every step succeeds: a **commit**. Now it is permanent

</v-clicks>
---
layout: default
---

# All or nothing

```java
public Recipe replace(Long id, Recipe recipe) {
    if (!recipeRepository.existsById(id)) {
        throw new RecipeNotFoundException("recipe id: " + id + " not found");
    }
    recipe.setId(id);
    return recipeRepository.save(recipe);
}
```

<v-clicks>

- Two calls to the database
- `save` alone is several statements — the recipe, its old ingredients, its new ones
- One decision. It should succeed together, or not at all

</v-clicks>
---
layout: default
---

# On the service

```java
@Override
@Transactional
public Recipe replace(Long id, Recipe recipe) { ... }
```

<v-clicks>

- **Not the controller** — that is HTTP. A transaction is about data
- **Not only the repository** — each method has its own small one. Only the service knows which calls belong together
- On the methods that **write**: `create` · `replace` · `delete`

</v-clicks>
---
layout: default
---

# The three writes

```java {all|2|8|12}
@Override
@Transactional
public Recipe create(Recipe recipe) {
    return recipeRepository.save(recipe);
}

@Override
@Transactional
public Recipe replace(Long id, Recipe recipe) { ... }

@Override
@Transactional
public void delete(Long id) {
    recipeRepository.deleteById(id);
}
```

<v-click>

The reads — `findAll`, `findById`, `findByIngredient`, `findQuick`, `findPage` — work without it.

</v-click>
---
layout: default
---

# Reads can say so

```java
@Override
@Transactional(readOnly = true)
public List<Recipe> findAll(String cuisine) { ... }
```

<v-clicks>

- Not required — reads work without it
- It tells Hibernate nothing will be saved, so it skips checking every object for changes
- It tells the next developer too

</v-clicks>

<v-click>

<div class="mt-6 text-lg">

A common shape: `@Transactional(readOnly = true)` on the class, plain `@Transactional` on each write.

</div>

</v-click>
---
layout: default
---

# Changing many rows at once

<div class="mt-2 text-lg">

*"We spell it 'Khmer' now, not 'cambodian'."* — forty recipes.

</div>

```java {all|1|2|3}
@Modifying
@Query("update Recipe r set r.cuisine = :to where r.cuisine = :from")
int renameCuisine(@Param("from") String from, @Param("to") String to);
```

<v-clicks at="1">

- `@Modifying` — this `@Query` **changes** data. Without it Spring Data expects a `select`
- One `update` statement. Not forty `select`s and forty `save`s
- The `int` is how many rows changed

</v-clicks>
---
layout: default
---

# `@Modifying` has two conditions

```java
@Transactional
public int renameCuisine(String from, String to) {
    return recipeRepository.renameCuisine(from, to);
}
```

<v-clicks>

- **It must run inside a transaction.** Call it without one: `TransactionRequiredException`
- That is the service's `@Transactional` — the reason this slide is in this section
- **It goes straight to the database.** A `Recipe` already loaded in this transaction still shows the old cuisine
- `@Modifying(clearAutomatically = true)` makes Hibernate forget what it loaded

</v-clicks>
---
layout: default
---

# It is a proxy

<div class="mt-6 text-xl">

The proxy around `RecipeServiceImpl` opens a transaction before your method, commits after — or
rolls back if an exception escapes.

</div>

<v-click>

<div class="mt-8 text-xl">

Exactly how `ServiceCallAspect` wraps the same methods.

</div>

</v-click>

<v-click>

<div class="mt-8 text-lg opacity-80">

Propagation and rollback rules — `sunrise-springdatajpa`, *Transactions and performance*.

</div>

</v-click>
---
layout: default
---

# What the proxy does

<v-clicks>

1. The controller calls `recipeService.delete(1)`
2. The **proxy** opens a transaction
3. Your method runs — `existsById`, then `deleteById`
4. No exception → the proxy **commits**
5. An exception escapes → the proxy **rolls back**, then lets the exception continue

</v-clicks>

<v-click>

<div class="mt-6 text-lg">

Your method contains no transaction code at all. Same shape as `ServiceCallAspect`: something
around the call.

</div>

</v-click>
---
layout: image
image: /session-10-repository-proxy.svg
backgroundSize: contain
---
---
layout: default
---

# A warning you will see in the log

```
spring.jpa.open-in-view is enabled by default. Therefore, database queries
may be performed during view rendering.
```

<v-clicks>

- Boot keeps the database session open until the response is written
- That is why the **controller** can still read `recipe.getIngredients()` while mapping
- Fine for this course. The warning is Boot asking you to decide on purpose, one day

</v-clicks>
---
layout: default
---

# Checkpoint 1 — what you do

<v-clicks>

1. Replace `RecipeRepository` — the JPQL query, and `findAll` with `@EntityGraph`
2. Replace `RecipeService` and `RecipeServiceImpl` — `findByIngredient`, `@Transactional`
3. Add `search(...)` to the controller
4. `mvn test` — green
5. `curl` two recipes in, then the search
6. `GET /recipes` — and count the `select`s

</v-clicks>
---
layout: statement
---

# Checkpoint 1

The finished repository · `/recipes/search` · `@Transactional` · `recipe-api/README.md`

*`mvn test` green · count the `select`s for `GET /recipes` again*
---
layout: section
---

# Break — 10 minutes
---
layout: section
---

# `ddl-auto` is not a migration tool
---
layout: default
---

# With a real restaurant's data in it

<v-clicks>

- **`create-drop`** / **`create`** — every deploy deletes every recipe
- **`update`** — *guesses*. Rename a field: a new empty column, the old one keeps the data
- And nobody can see what changed, when, or on which database

</v-clicks>

<v-click>

<div class="mt-8 text-xl">

Every schema change **written down, in order, in Git** — and applied once to every database.

</div>

</v-click>
---
layout: default
---

# What `update` does to a rename

```java
private String internalNotes;     // you rename it…
private String kitchenNotes;      // …to this
```

<v-click>

```sql
alter table recipes add column kitchen_notes varchar(255)
```

</v-click>

<v-clicks>

- A **new, empty** column
- `internal_notes` is still there — with all the data in it
- No error. No warning. Every recipe just lost its notes

</v-clicks>
---
layout: default
---

# Three databases, drifting

<div class="grid grid-cols-3 gap-4 mt-6 text-center">
<div class="p-4 rounded-lg border-2 border-gray-400">

### Your laptop

Restarted fifty times

</div>
<div class="p-4 rounded-lg border-2 border-gray-400">

### The test server

Deployed last Tuesday

</div>
<div class="p-4 rounded-lg border-2 border-gray-400">

### Production

Deployed last month

</div>
</div>

<v-click>

<div class="mt-8 text-xl">

Which columns does each one have? **Nobody knows.** Nothing wrote it down.

</div>

</v-click>

<v-click>

<div class="mt-4 text-xl font-bold">

You keep code in Git. The schema deserves the same.

</div>

</v-click>
---
layout: default
---

# Liquibase in three words

| Word | Means |
|---|---|
| **changeset** | One change — a table, a column, some rows. Has an id and an author |
| **changelog** | The ordered list of changesets |
| **`DATABASECHANGELOG`** | A table in *your* database, recording every changeset that ran |

<v-click>

<div class="mt-6 text-xl">

At startup: run every changeset not in that table — in order, once each. **Then** Hibernate starts.

</div>

</v-click>
---
layout: default
---

# What happens at startup

<v-clicks>

1. Liquibase connects to the database
2. Creates `DATABASECHANGELOG` if it is not there
3. Reads your changelog, top to bottom
4. For each changeset: **already in the table? skip. Not there? run it, and write it down**
5. Only then does Hibernate start — and `validate` the result

</v-clicks>
---
layout: default
---

# The changelog

```yaml
databaseChangeLog:
  - include:
      file: db/changelog/001-create-recipes-and-ingredients.sql
  - include:
      file: db/changelog/002-seed-recipes.sql
```

<v-clicks>

- `src/main/resources/db/changelog/db.changelog-master.yaml` — where Boot looks
- New changes go at the **bottom**. The order is the history

</v-clicks>
---
layout: default
---

# Where the files go

```
src/main/resources/
├── application.properties
└── db/
    └── changelog/
        ├── db.changelog-master.yaml          ← the list
        ├── 001-create-recipes-and-ingredients.sql
        └── 002-seed-recipes.sql
```

<v-clicks>

- One file per change. Numbered, so the order is obvious
- The name says what it does
- Next month's change is `003-…`. Nobody opens `001` again

</v-clicks>
---
layout: default
---

# A changeset

```sql {all|1|3|4-11|13-17|18-19}
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
---
layout: default
---

# The header that names it

```sql
--changeset sunrise:001-create-recipes-and-ingredients
```

| Piece | Is |
|---|---|
| `sunrise` | The **author** |
| `001-create-recipes-and-ingredients` | The **id** |

<v-click>

<div class="mt-6 text-lg">

Author + id + file name = this changeset's identity, forever. Liquibase stores all three.

</div>

</v-click>

<v-click>

<div class="mt-4 text-lg">

To the database these lines are comments. To Liquibase they are instructions.

</div>

</v-click>
---
layout: statement
---

# You are writing down what Hibernate used to invent

Reviewed · versioned · the same on every database
---
layout: default
---

# The house recipes

```sql
--changeset sunrise:002-seed-recipes
insert into recipes (title, cuisine, minutes, servings, internal_notes)
    values ('Fish Amok', 'khmer', 45, 4, 'supplier: central market, pays late');

insert into ingredients (name, recipe_id)
    values ('fish', (select id from recipes where title = 'Fish Amok'));
```

<v-clicks>

- The job `@PostConstruct` did in the class you deleted
- Each ingredient finds its recipe **by title** — the database chose the id

</v-clicks>
---
layout: default
---

# Why not `values ('fish', 1)`?

```sql
insert into ingredients (name, recipe_id)
    values ('fish', (select id from recipes where title = 'Fish Amok'));
```

<v-clicks>

- The **database** hands out the ids. You do not know that Fish Amok will be `1`
- On a database that already has recipes, it will not be
- So each ingredient **looks its recipe up**

</v-clicks>
---
layout: default
---

# `validate` from here on

```properties
spring.jpa.hibernate.ddl-auto=validate
```

<v-click>

<div class="mt-6 text-xl">

Hibernate creates nothing. It compares entities with tables — and refuses to start:

</div>

</v-click>

<v-click>

```
Schema validation: missing column [difficulty] in table [recipes]
```

</v-click>

<v-click>

<div class="mt-4 text-lg">

Found at startup, on your machine. Not at three in the morning.

</div>

</v-click>
---
layout: default
---

# Who does what, from now on

<div class="grid grid-cols-2 gap-8 mt-6">
<div class="p-6 rounded-lg border-2 border-orange-500">

### Liquibase

**Builds** the tables.

From your changesets, in order, once each.

</div>
<div class="p-6 rounded-lg border-2 border-green-600">

### Hibernate

**Checks** the tables.

Entity and table disagree → the application does not start.

</div>
</div>

<v-click>

<div class="mt-6 text-lg">

Two tools, each doing the one thing it is good at.

</div>

</v-click>
---
layout: default
---

# Edit a changeset that has run

```
Validation Failed:
     1 changesets check sum
          db/changelog/002-seed-recipes.sql::002-seed-recipes::sunrise
          was: 9:b78109... but is now: 9:af6c64...
```

<v-click>

<div class="mt-8 text-2xl font-bold">

Why refuse? Your edit might be a good fix.

</div>

</v-click>

<v-click>

<div class="mt-4 text-xl">

Your laptop ran the old one. The test server ran it. Production ran it.

</div>

</v-click>
---
layout: statement
---

# A changeset that has run is never edited

A mistake in `002` is fixed by `003`
---
layout: default
---

# The right way to change the schema

<v-clicks>

- The client wants something that needs a new table, or a new column
- You do **not** open `001` and edit it
- You add a **new file** — `003-…` — and one line at the bottom of the master changelog
- Databases that ran `001` and `002` run only `003`
- A brand-new database runs all three. **They all end up identical**

</v-clicks>

<v-click>

<div class="mt-6 text-lg">

You do exactly this at checkpoint 3.

</div>

</v-click>
---
layout: section
---

# From H2 to PostgreSQL
---
layout: default
---

# Why leave H2?

<v-clicks>

- H2 was the right place to learn — nothing to install
- It lived **in memory**: stop the application, lose every recipe
- That is still the client's first complaint. Still unanswered

</v-clicks>

<v-click>

<div class="mt-8 text-xl">

A real application talks to a database **server** — its own program, running on its own, keeping
data on disk.

</div>

</v-click>

<v-click>

<div class="mt-4 text-2xl font-bold">

Ours is PostgreSQL.

</div>

</v-click>
---
layout: default
---

# One — an empty database

<div class="mt-4 text-xl">

PostgreSQL is running on your machine. In `psql`, pgAdmin or DataGrip:

</div>

```sql
create database recipe_db;
```

<v-click>

<div class="mt-8 text-2xl font-bold">

No tables.

</div>

</v-click>

<v-click>

<div class="mt-4 text-xl">

Liquibase builds those — from the two changesets you just read.

</div>

</v-click>
---
layout: default
---

# Two — the driver

```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

<v-clicks>

- The **driver** is the library that knows how to talk to one kind of database
- `runtime` — your code never imports it, exactly like H2
- No version: Spring Boot manages it

</v-clicks>
---
layout: default
---

# Three — three properties

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/recipe_db
spring.datasource.username=postgres
spring.datasource.password=password
```

| Piece | Means |
|---|---|
| `postgresql:` | Which database |
| `localhost` | Which machine — this one |
| `5432` | PostgreSQL's port |
| `recipe_db` | The database you created |

<v-click>

Use **your own** username and password.

</v-click>
---
layout: default
---

# H2 and PostgreSQL, side by side

| | H2, until now | PostgreSQL, from here |
|---|---|---|
| URL | `jdbc:h2:mem:recipes` | `jdbc:postgresql://localhost:5432/recipe_db` |
| Where it runs | Inside your application | Its own server |
| When the app stops | Everything is gone | Everything is kept |
| Who builds the tables | Hibernate, `create-drop` | Liquibase |
---
layout: default
---

# Pause

<div class="mt-8 text-3xl font-bold">

Which Java files change when you move from H2 to PostgreSQL?

</div>

<v-click>

<div class="mt-8 text-2xl">

None.

</div>

</v-click>

<v-click>

```
Database driver: PostgreSQL JDBC Driver
Database dialect: PostgreSQLDialect
```

Hibernate read the URL, worked out who it is talking to, and writes PostgreSQL's SQL now.

</v-click>
---
layout: statement
---

# Your code speaks JPA. Hibernate speaks to the database.

Last session's "program to the specification", paying off for real
---
layout: default
---

# H2 keeps one job

```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

<v-clicks>

- `runtime` → **`test`**. The application is not on H2 any more
- `contextLoads` starts the whole application — from here on, `mvn test` needs PostgreSQL running
- H2 is there for the **extra** tests: `@DataJpaTest` swaps in a fresh in-memory database
- The H2 console goes. Look inside PostgreSQL with `psql`, pgAdmin or DataGrip

</v-clicks>
---
layout: statement
---

# The same two changesets build both databases

H2 for the extra tests · PostgreSQL for the application
---
layout: default
---

# First start

```
Database driver: PostgreSQL JDBC Driver
Database version: 16.2
Running Changeset: db/changelog/001-create-recipes-and-ingredients.sql::...::sunrise
Running Changeset: db/changelog/002-seed-recipes.sql::002-seed-recipes::sunrise
UPDATE SUMMARY
Run:                          2
Previously run:               0
```

<v-click>

```bash
curl http://localhost:8080/recipes
```

Fish Amok. Lok Lak. Carbonara. **Back — and nobody wrote Java to put them there.**

</v-click>
---
layout: default
---

# Second start

<div class="mt-4 text-xl">

POST a recipe of your own. Stop the application. Start it again.

</div>

<v-click>

```
Run:                          0
Previously run:               2
```

</v-click>

<v-clicks>

- Liquibase checked `DATABASECHANGELOG`, found both, **skipped both**
- `GET /recipes` — your recipe is still there

</v-clicks>

<v-click>

<div class="mt-6 text-2xl font-bold">

The client's first request: done.

</div>

</v-click>
---
layout: default
---

# Look inside

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

<v-click>

Two tables of yours. Two of Liquibase's — its own record of what it has run.

</v-click>
---
layout: default
---

# Later: Docker

<v-clicks>

- PostgreSQL itself moves into a container
- The URL changes
- The changelog you wrote today builds the tables there too

</v-clicks>

<v-click>

<div class="mt-8 text-xl">

The application will not notice. It did not notice today either.

</div>

</v-click>
---
layout: default
---

# Past today's boundary

<v-clicks>

- **Rollback** — undoing a changeset
- **Contexts** — seed data on a laptop, never in production
- **Preconditions** — run only if…

</v-clicks>

<v-click>

<div class="mt-8 text-lg opacity-80">

All three: `sunrise-spring-migration`.

</div>

</v-click>
---
layout: default
---

# Checkpoint 2 — what you do

<v-clicks>

1. `create database recipe_db;`
2. POM: PostgreSQL driver and Liquibase starter in; H2 to `test`; H2 console out
3. Three changelog files under `db/changelog/`
4. Replace `application.properties` — your own username and password
5. `mvn test` — green
6. Start, read `Run: 2`. POST a recipe. Restart, read `Previously run: 2`

</v-clicks>
---
layout: default
---

# Then break it — twice

<div class="grid grid-cols-2 gap-8 mt-6">
<div class="p-6 rounded-lg border-2 border-red-500">

### One

Change Lok Lak's `25` minutes to `30` in `002-seed-recipes.sql`.

Restart. Read the error. Put it back.

</div>
<div class="p-6 rounded-lg border-2 border-red-500">

### Two

Add `private String difficulty;` to `Recipe`.

Restart. Read the error. Take it out.

</div>
</div>

<v-click>

<div class="mt-6 text-lg">

Predict each error before you restart. You have seen both on these slides.

</div>

</v-click>
---
layout: statement
---

# Checkpoint 2

PostgreSQL · Liquibase · `validate` · `recipe-api/README.md`

*`mvn test` green · restart and keep your data · then break it twice*
---
layout: section
---

# Many recipes, many tags
---
layout: statement
---

# *"We want to label recipes — spicy, vegetarian, quick — and see every recipe that carries a label."*

The client
---
layout: default
---

# Not the relationship you know

<div class="grid grid-cols-2 gap-8 mt-6">
<div class="p-6 rounded-lg border-2 border-gray-400">

### Ingredients — one-to-many

"lime" in Lok Lak belongs to **Lok Lak**. Only.

An ingredient row can carry one `recipe_id`.

</div>
<div class="p-6 rounded-lg border-2 border-blue-500">

### Tags — many-to-many

"spicy" is on Fish Amok **and** Lok Lak.

Fish Amok is "spicy" **and** "classic".

</div>
</div>
---
layout: default
---

# Pause — where does the foreign key go?

<v-clicks>

- `tag_id` on `recipes` → a recipe can have **one** tag
- `recipe_id` on `tags` → a tag can be on **one** recipe

</v-clicks>

<v-click>

<div class="mt-8 text-2xl font-bold">

A column holds one value. Neither table can hold the other's id.

</div>

</v-click>

<v-click>

<div class="mt-4 text-xl">

So the link gets a table of its own — one row per pair.

</div>

</v-click>
---
layout: default
---

# Three tables

<div class="grid grid-cols-3 gap-4 mt-4">
<div>

**recipes**

| id | title |
|---|---|
| 1 | Fish Amok |
| 2 | Lok Lak |

</div>
<div>

**recipe_tags**

| recipe_id | tag_id |
|---|---|
| 1 | 1 |
| 2 | 1 |
| 1 | 2 |

</div>
<div>

**tags**

| id | name |
|---|---|
| 1 | spicy |
| 2 | classic |

</div>
</div>

<v-click>

<div class="mt-4 text-xl">

**Two tags, three links.** "spicy" exists once, and is shared.

</div>

</v-click>
---
layout: statement
---

# You have seen a join table before

It is what Hibernate invented last session when you deleted `mappedBy`

*That time it was a mistake. This time it is the design.*
---
layout: default
---

# The changeset comes first

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
```

<v-clicks>

- `recipe_tags` has no id of its own — the **pair** is the primary key
- So the same recipe cannot carry the same tag twice

</v-clicks>
---
layout: default
---

# The tag

```java
@Entity
@Table(name = "tags")
@Getter @Setter @NoArgsConstructor
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;
}
```

<v-click>

No `recipe_id`. No reference to `Recipe` at all. A tag does not belong to anyone.

</v-click>
---
layout: default
---

# On the recipe

```java {all|1|2-5|6}
@ManyToMany
@JoinTable(
        name = "recipe_tags",
        joinColumns = @JoinColumn(name = "recipe_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id"))
private Set<Tag> tags = new LinkedHashSet<>();
```

| | Means |
|---|---|
| `@JoinTable` | The table that holds the pairs. No entity class for it |
| `joinColumns` | The column pointing at **this** entity |
| `inverseJoinColumns` | The column pointing at the **other** one |
| `Set` | A recipe has a tag or it does not — never twice |
---
layout: default
---

# No cascade — on purpose

<div class="grid grid-cols-2 gap-8 mt-6">
<div class="p-6 rounded-lg border-2 border-green-600">

### Ingredients

`cascade = ALL`

An ingredient is **part of** its recipe. Delete the recipe, delete them.

</div>
<div class="p-6 rounded-lg border-2 border-red-500">

### Tags

*(nothing)*

Delete Fish Amok and "spicy" must **survive** — Lok Lak uses it.

</div>
</div>

<v-click>

<div class="mt-6 text-lg">

Hibernate removes the recipe's rows in `recipe_tags`, and leaves `tags` alone.

</div>

</v-click>
---
layout: default
---

# Tagging is one line

```java
recipe.getTags().add(tag);
```

<v-click>

```sql
insert into recipe_tags (recipe_id, tag_id) values (?, ?)
```

</v-click>

<v-clicks>

- No row is written to `tags` — the tag already exists
- No row is changed in `recipes`
- One small row in the middle table. **That is the whole relationship**

</v-clicks>
---
layout: default
---

# Find the tag, or make it

```java
@Transactional
public void addTag(Long recipeId, String name) {
    Recipe recipe = recipeRepository.findById(recipeId)
            .orElseThrow(() -> new RecipeNotFoundException("recipe id: " + recipeId + " not found"));
    Tag tag = tagRepository.findByNameIgnoreCase(name)
            .orElseGet(() -> tagRepository.save(new Tag(name.toLowerCase())));
    recipe.getTags().add(tag);
    recipeRepository.save(recipe);
}
```

<v-clicks>

- "spicy" is created the **first** time, and reused every time after
- `@Transactional` — the new tag and the link are saved together, or not at all

</v-clicks>
---
layout: default
---

# Both directions

```java
recipe.getTags()                                      // recipe → its tags
recipeRepository.findByTagsNameIgnoreCase("spicy")    // tag → its recipes
```

<v-click>

| Word | Means |
|---|---|
| `Tags` | the `tags` field on `Recipe` |
| `Name` | the `name` field on `Tag` |
| `IgnoreCase` | `upper(...) = upper(?)` |

</v-click>

<v-click>

A derived query that **walks the relationship**. Spring Data writes both joins.

</v-click>
---
layout: default
---

# Three endpoints

| Request | Does |
|---|---|
| `PUT /recipes/1/tags/spicy` | Tag recipe 1. `204` |
| `GET /recipes/1/tags` | `["spicy", "classic"]` |
| `GET /tags/spicy/recipes` | Every recipe tagged spicy |

<v-clicks>

- **`PUT`, not `POST`** — tagging twice leaves it tagged once. Idempotent
- A separate `TagController` and `TagService` — so `RecipeServiceImpl` and `RecipeController` do not change

</v-clicks>
---
layout: default
---

# The mapper notices, again

```
RecipeMapper.java: warning: Unmapped target property: "tags".
```

<v-click>

```java
@Mapping(target = "tags", ignore = true)
```

</v-click>

<v-click>

<div class="mt-6 text-lg">

A new field on `Recipe`, and the build stops until you decide. The client does not send tags when
creating a recipe — say so in writing.

</div>

</v-click>
---
layout: statement
---

# *"Who put 'spicy' on Fish Amok — and when?"*

The client, six months from now · *an option — not in today's exercise*
---
layout: default
---

# A link with something to say

| recipe_id | tag_id | added_at | added_by |
|---|---|---|---|
| 1 | 1 | 2026-10-04 09:12 | dara |
| 2 | 1 | 2026-10-05 14:30 | sokha |

<v-clicks>

- The new facts are not about the recipe, and not about the tag — they are about **the pair**
- So the columns belong on `recipe_tags`
- But `Set<Tag> tags` has no class for that row. There is nowhere to write `addedAt`

</v-clicks>

<v-click>

<div class="mt-6 text-xl font-bold">

`@ManyToMany` hides the join table. Now you need to see it.

</div>

</v-click>
---
layout: default
---

# The option: the link becomes an entity

```java {all|1-3|8-14|16-17}
@Entity
@Table(name = "recipe_tags")
public class RecipeTag {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id")
    private Recipe recipe;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tag_id")
    private Tag tag;

    private Instant addedAt;
    private String addedBy;
}
```

The same table as before — with a class, an id, and room for its own fields.
---
layout: default
---

# One many-to-many, written as two one-to-manys

```java
// on Recipe — instead of @ManyToMany Set<Tag>
@OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
private Set<RecipeTag> recipeTags = new LinkedHashSet<>();
```

```java
// tagging is still one line — it just says more
recipe.getRecipeTags().add(new RecipeTag(recipe, tag, Instant.now(), "dara"));
```

<v-clicks>

- `Recipe` → many `RecipeTag` ← many `Tag`. Both arrows are the `@ManyToOne` you wrote for `Ingredient`
- The cascade is on the **link**, not on the tag — delete Fish Amok and "spicy" still survives
- In the changeset: keep `unique (recipe_id, tag_id)`, so a recipe is never tagged twice

</v-clicks>
---
layout: default
---

# Which one, when

<div class="grid grid-cols-2 gap-8 mt-6">
<div class="p-6 rounded-lg border-2 border-blue-500 bg-blue-500/10">

### `@ManyToMany`

The link is **only a link**.

"This recipe has this tag." Nothing more to know.

Less code. *Today's exercise.*

</div>
<div class="p-6 rounded-lg border-2 border-green-600 bg-green-500/10">

### A join entity

The link has **facts of its own**.

When, who, how many, in what order, at what price.

One more class — and you can query it.

</div>
</div>

<v-click>

<div class="mt-6 text-lg">

Order ↔ Product with a *quantity*. Student ↔ Course with a *grade*. In real projects the link
usually grows a column — so you will meet the join entity often.

</div>

</v-click>
---
layout: default
---

# The three kinds, side by side

| | Example | The foreign key lives |
|---|---|---|
| **One-to-many** | A recipe has many ingredients | On the "many" table — `ingredients.recipe_id` |
| **Many-to-many** | Recipes and tags | In a join table — `recipe_tags` |
| **One-to-one** | A recipe and its one nutrition label | On either table, with `unique` |

<v-click>

<div class="mt-6 text-lg">

One-to-one is the rare one — `@OneToOne` with `@JoinColumn`. Often the honest answer is *"those are
just more columns on the same table."*

</div>

</v-click>
---
layout: default
---

# Watch Liquibase run only one

```
Running Changeset: db/changelog/003-create-tags.sql::003-create-tags::sunrise
Run:                          1
Previously run:               2
```

<v-clicks>

- One changeset ran. Two were skipped
- **Every recipe is still there**
- That is the difference between a migration and `create-drop`

</v-clicks>
---
layout: default
---

# Checkpoint 3 — what you do

<v-clicks>

1. `003-create-tags.sql`, and one more line in the master changelog
2. `Tag`, and the `tags` field on `Recipe`
3. One line in the mapper; `TagRepository`; one derived query
4. `TagService` and `TagController`
5. `mvn test` — green. Start: `Run: 1 · Previously run: 2`
6. Tag two recipes "spicy" — then `select * from recipe_tags;`

</v-clicks>
---
layout: statement
---

# Checkpoint 3

Tags · `@ManyToMany` · a third changeset · `recipe-api/README.md`

*`mvn test` green · two tags, three links · delete a recipe and "spicy" survives*
---
layout: section
---

# Filters the client may, or may not, send
---
layout: statement
---

# *"Let us filter the menu — by cuisine, by time, by label. Any of them. All of them. Or none."*

One endpoint. Three filters. Every one optional
---
layout: default
---

# Count the combinations

| Sent | Method you would need |
|---|---|
| nothing | `findAll` |
| cuisine | `findByCuisineIgnoreCase` |
| minutes | `findByMinutesLessThanEqual` |
| cuisine + minutes | `findByCuisineIgnoreCaseAndMinutesLessThanEqual` |
| cuisine + tag | `findByCuisineIgnoreCaseAndTagsNameIgnoreCase` |
| … | … |

<v-click>

<div class="mt-4 text-xl">

Three filters: **8** methods — and an `if` chain in the service to pick one. Four filters: 16.

</div>

</v-click>
---
layout: default
---

# The trick that works — for a while

```java
@Query("""
        select r from Recipe r
        where (:cuisine is null or lower(r.cuisine) = lower(:cuisine))
          and (:maxMinutes is null or r.minutes <= :maxMinutes)
        """)
List<Recipe> filter(@Param("cuisine") String cuisine, @Param("maxMinutes") Integer maxMinutes);
```

<v-clicks>

- One query. Each line switches itself off when its parameter is `null`
- Fine for two filters. You will meet it in real projects
- Every new filter makes it longer — and a filter that needs a **join** cannot switch its join off

</v-clicks>
---
layout: default
---

# A `Specification` is one condition

```java
public static Specification<Recipe> takesAtMost(int minutes) {
    return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("minutes"), minutes);
}
```

<v-clicks>

- One `where` condition, as a Java object
- Small, named, and reusable — `takesAtMost(30)`
- A lambda. You have written these since the functional Java session

</v-clicks>

<v-click>

<div class="mt-6 text-xl">

Build the conditions you need. Join them with `and`. Hand the result to the repository.

</div>

</v-click>
---
layout: default
---

# Reading the lambda

```java
(root, query, cb) -> cb.lessThanOrEqualTo(root.get("minutes"), minutes)
```

| Piece | Is | In JPQL you wrote |
|---|---|---|
| `root` | the recipe being tested | `r` |
| `root.get("minutes")` | one of its **fields** | `r.minutes` |
| `cb` | the builder of comparisons | `<=`, `=`, `like` |
| `query` | the whole query — rarely needed | |

<v-click>

`r.minutes <= :minutes` — the same condition, built with method calls instead of text.

</v-click>
---
layout: default
---

# The comparisons `cb` can build

| You want | You call |
|---|---|
| `=` | `cb.equal(a, b)` |
| `<=` | `cb.lessThanOrEqualTo(a, b)` |
| `>=` | `cb.greaterThanOrEqualTo(a, b)` |
| `like` | `cb.like(a, "%am%")` |
| `between` | `cb.between(a, low, high)` |
| lower case | `cb.lower(a)` |

<v-click>

`cb` is short for **CriteriaBuilder** — part of JPA, not of Spring.

</v-click>
---
layout: default
---

# One small method for each filter

```java {all|3-5|7-9|11-13}
public final class RecipeSpecifications {

    public static Specification<Recipe> hasCuisine(String cuisine) {
        return (root, query, cb) -> cb.equal(cb.lower(root.get("cuisine")), cuisine.toLowerCase());
    }

    public static Specification<Recipe> takesAtMost(int minutes) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("minutes"), minutes);
    }

    public static Specification<Recipe> hasTag(String tag) {
        return (root, query, cb) -> cb.equal(cb.lower(root.join("tags").get("name")), tag.toLowerCase());
    }
}
```
---
layout: default
---

# The one with a join

```java
root.join("tags").get("name")
```

| Step | Means |
|---|---|
| `root` | the recipe |
| `.join("tags")` | follow the `tags` field, through `recipe_tags`, to the `Tag` |
| `.get("name")` | the tag's `name` |

<v-click>

<div class="mt-6 text-lg">

The join exists **only when this filter is used**. No tag in the request — no join in the SQL. That
is what the `:tag is null or …` trick could not do.

</div>

</v-click>
---
layout: default
---

# The repository learns one more word

```java
public interface RecipeRepository
        extends JpaRepository<Recipe, Long>, JpaSpecificationExecutor<Recipe> {
```

<v-click>

| You now also have | |
|---|---|
| `findAll(spec)` | every match, as a `List` |
| `findAll(spec, pageable)` | one page of matches — a `Page` |
| `count(spec)` | how many match |
| `exists(spec)` | does any match? |

</v-click>

<v-click>

No new method to declare. One word on the `extends` line.

</v-click>
---
layout: default
---

# Only the filters that were sent

```java {all|2|3-5|6|7}
public Page<Recipe> filter(String cuisine, Integer maxMinutes, String tag, int page, int size, String sortBy) {
    List<Specification<Recipe>> filters = new ArrayList<>();
    if (cuisine != null)    filters.add(hasCuisine(cuisine));
    if (maxMinutes != null) filters.add(takesAtMost(maxMinutes));
    if (tag != null)        filters.add(hasTag(tag));
    Specification<Recipe> all = Specification.allOf(filters);
    return recipeRepository.findAll(all, pageRequest(page, size, sortBy));
}
```

<v-clicks at="1">

- Start with no conditions
- Add one for each filter the client **sent**
- `allOf` — every one must be true. An empty list means "everything"
- Last session's page request, with its two rules, reused

</v-clicks>
---
layout: default
---

# `Integer`, not `int`

```java
@RequestParam(required = false) Integer maxMinutes
```

<v-clicks>

- An `int` cannot be "not sent". Spring fails the request rather than invent a number
- An `Integer` can be `null`. `null` means *the client did not ask*
- The same reason the entity's id is a `Long`

</v-clicks>
---
layout: default
---

# What Hibernate sent

`GET /recipes/filter?cuisine=khmer&maxMinutes=30`

```sql
select ... from recipes r1_0
where lower(r1_0.cuisine)=? and r1_0.minutes<=?
order by r1_0.id fetch first ? rows only
```

<v-click>

`GET /recipes/filter?tag=spicy`

```sql
select ... from recipes r1_0
join recipe_tags t1_0 on r1_0.id=t1_0.recipe_id
join tags t1_1 on t1_1.id=t1_0.tag_id
where lower(t1_1.name)=?
order by r1_0.id fetch first ? rows only
```

</v-click>

<v-click>

Two different queries, from the same six lines of Java.

</v-click>
---
layout: default
---

# Joining conditions

```java
hasCuisine("khmer").and(takesAtMost(30))          // both
hasCuisine("khmer").or(hasCuisine("thai"))        // either
Specification.not(hasTag("spicy"))                // the opposite

Specification.allOf(filters)                      // every one in a list
Specification.anyOf(filters)                      // at least one in a list
```

<v-click>

<div class="mt-6 text-lg">

Each one is still a `Specification<Recipe>` — so the result can be joined again, or paged.

</div>

</v-click>
---
layout: default
---

# The field names are text

```java
root.get("cuisin")        // compiles
```

<v-clicks>

- A derived query with a wrong field stops the application **at startup**
- A specification with a wrong field fails **when a request reaches it**
- So: one small class, one place for the names — and `curl` every filter once

</v-clicks>
---
layout: default
---

# Which tool, when

| The question | Reach for |
|---|---|
| One or two fixed conditions, one table | A **derived query** — the method name |
| Fixed, but long, or with a join | **`@Query`** with JPQL |
| Only a few columns | A **projection** |
| Change many rows in one statement | **`@Modifying`** `@Query` |
| Something only your database can do | A **native** query |
| Filters the client may or may not send | **Specifications** |

<v-click>

Start at the top. Move down only when the one above stops being readable.

</v-click>
---
layout: default
---

# Checkpoint 4 — what you do

<v-clicks>

1. `RecipeSpecifications` — three small methods
2. `JpaSpecificationExecutor<Recipe>` on the repository's `extends` line
3. `filter(...)` in the service, `GET /recipes/filter` in the controller
4. `mvn test` — green
5. `curl` it with no filter, one filter, all three
6. Read the log: the `where` changes with what you send

</v-clicks>
---
layout: statement
---

# Checkpoint 4

Optional filters · `Specification` · paged · `recipe-api/README.md`

*`mvn test` green · `/recipes/filter?cuisine=khmer&maxMinutes=30&tag=spicy` · watch the `where` change*
---
layout: section
---

# Documentation nobody writes
---
layout: default
---

# Written by hand, wrong by Friday

<div class="mt-6 text-xl">

A document describing the API is wrong the first time somebody adds a field.

</div>

<v-click>

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>${springdoc.version}</version>
</dependency>
```

</v-click>

<v-click>

<div class="mt-4 text-xl">

**http://localhost:8080/swagger-ui.html**

</div>

</v-click>
---
layout: statement
---

# A page nobody wrote

A starter on the classpath, and something appears — auto-configuration, again
---
layout: default
---

# What is on the page

<v-clicks>

- Every endpoint — method, URL, parameters
- Every request body and response, as a schema
- **Try it out** — send a real request from the browser
- **Schemas** — your DTOs, field by field

</v-clicks>

<v-click>

<div class="mt-6 text-lg">

All read from `RecipeController`, `CreateRecipeRequest` and `RecipeResponse`. The entity is not
on it — `internalNotes` still never leaves the building.

</div>

</v-click>
---
layout: default
---

# It read your validation rules

| On `CreateRecipeRequest` | On the page |
|---|---|
| `@NotBlank String title` | `title` is required |
| `@Min(1) int minutes` | `minutes`, minimum 1 |
| `@NotEmpty List<String> ingredients` | at least one item |

<v-click>

<div class="mt-6 text-xl">

The constraints that reject bad requests now **document** good ones. One source, two jobs.

</div>

</v-click>
---
layout: default
---

# The page is a viewer

<v-clicks>

- Behind it: **`/v3/api-docs`** — JSON, in a standard format called **OpenAPI**
- That file is what the mobile team imports
- They can even generate client code from it

</v-clicks>
---
layout: default
---

# Try it out

<v-clicks>

1. Open `GET /recipes/search`
2. **Try it out**
3. Type `fish`
4. **Execute**

</v-clicks>

<v-click>

<div class="mt-6 text-xl">

A real request, a real response, the real status code — and the `curl` command it would have been.

</div>

</v-click>

<v-click>

<div class="mt-4 text-lg opacity-80">

Now try `POST /recipes` with `"minutes": 0`. Your 400, with your error shape.

</div>

</v-click>
---
layout: default
---

# Say what the code cannot

```java
@Operation(summary = "Get one recipe by its id")
@ApiResponse(responseCode = "200", description = "The recipe")
@ApiResponse(responseCode = "404", description = "No recipe has this id")
@GetMapping("/{id}")
public ResponseEntity<RecipeResponse> getRecipeById(@PathVariable Long id) { ... }
```

<v-clicks>

- Changes nothing about what the endpoint does
- Document what other people call. Do not decorate every method out of duty

</v-clicks>
---
layout: default
---

# With and without

<div class="grid grid-cols-2 gap-8 mt-6">
<div class="p-6 rounded-lg border-2 border-green-600">

### `GET /recipes/{id}`

*Get one recipe by its id*

**200** — The recipe

**404** — No recipe has this id

</div>
<div class="p-6 rounded-lg border-2 border-gray-400">

### `DELETE /recipes/{id}`

No summary.

Only what the page could work out from the method signature.

</div>
</div>

<v-click>

<div class="mt-6 text-lg">

`DELETE` really answers `204`, or `404` the second time. **The code cannot say that by itself** —
which is exactly what the annotations are for.

</div>

</v-click>
---
layout: default
---

# Two warnings

<v-clicks>

- **springdoc 3.x for Spring Boot 4.** Every older tutorial shows 2.x — it does not work
- **The page is public.** The security session locks everything — then decides, on purpose, whether this page stays open

</v-clicks>
---
layout: default
---

# Demo — watch, do not type

<v-clicks>

1. `springdoc.version` 3.0.2 and the dependency into `pom.xml`
2. Three annotations on `getRecipeById`
3. Open **/swagger-ui.html**
4. **Schemas → CreateRecipeRequest** — the validation rules, read from your code
5. **`GET /recipes/filter`** — every optional filter is a box on the page. **Try it out**
6. Open **/v3/api-docs** — the JSON behind the page

</v-clicks>

<v-click>

<div class="mt-4 text-lg opacity-80">

The steps are in the README, after checkpoint 4 — do them at home in five minutes.

</div>

</v-click>
---
layout: section
---

# What you now know that you did not
---
layout: default
zoom: 0.85
---

# Before today, and now

| Before today | Now |
|---|---|
| The only query came from a method name | You also write your own, in JPQL — and native SQL when you must |
| Every query loaded whole recipes | A projection loads three columns |
| Optional filters meant a method for every combination | One `Specification` per filter, joined with `allOf` |
| One query per recipe | One query, with `@EntityGraph` |
| Transactions were invisible | `@Transactional` on the service's writes |
| The database forgot everything | PostgreSQL — and no Java changed to get there |
| Hibernate built and threw away the tables | Liquibase builds them from Git; Hibernate validates |
| One relationship: one-to-many | Many-to-many too — a join table, no cascade |
| The API lived in people's heads | Generated from the code |
---
layout: default
zoom: 0.8
---

# The words you now own

| Word | In one line |
|---|---|
| **Derived query** | A query written from the method's name |
| **JPQL** | A query language over entities, not tables |
| **Projection** | A query that returns a few fields, not the entity |
| **Native query** | SQL sent as you wrote it — tied to one database |
| **Specification** | One `where` condition as an object; join them with `and` / `or` |
| **Transaction** | Changes that all succeed, or none do |
| **Changeset** | One recorded change to the schema |
| **Join table** | The third table that holds a many-to-many's pairs |
| **Migration** | Moving a database from one version of the schema to the next |
| **OpenAPI** | A standard, machine-readable description of an API |
---
layout: default
zoom: 0.8
---

# When it goes wrong

| You see | It means |
|---|---|
| `No property 'kitchen' found for type 'Recipe'` | A derived query names a field that does not exist |
| `TransactionRequiredException` | A `@Modifying` query was called outside `@Transactional` |
| A filter request answers 500 | A field name inside a `Specification` is misspelt — it is only text |
| `/recipes/filter` with no `maxMinutes` answers 500 | `int maxMinutes` instead of `Integer`. An `int` cannot be "not sent" |
| `Connection to localhost:5432 refused` | PostgreSQL is not running |
| `database "recipe_db" does not exist` | The `create database` step was skipped |
| `password authentication failed` | Wrong username or password in the properties |
| `1 changesets check sum` | A changeset that already ran was edited |
| `Schema validation: missing column` | The entity and the table disagree |
| Swagger page is a 404 | Dependency missing — or a 2.x version |
---
layout: statement
---

# Who is allowed to delete a recipe?

Right now: anyone. The API has no idea who anyone is. That is where the security session starts.
---
layout: default
---

# Extra, for after class

<div class="mt-6 text-xl">

`recipe-api/EXTRA-tests.md` — six tests.

</div>

<v-clicks>

- `RecipeRepositoryTest` — the derived query and the JPQL query, against a real database
- `SearchWebTest` — the new endpoint through `MockMvc`
- `LiquibaseSeedTest` — saves nothing, and finds the three house recipes anyway
- `RecipeTagTest` — the many-to-many, from both ends
- **Swagger, hands on** — the README's last section: one dependency, three annotations, five minutes

</v-clicks>

<v-click>

<div class="mt-6 text-lg opacity-80">

Not needed for next session.

</div>

</v-click>
