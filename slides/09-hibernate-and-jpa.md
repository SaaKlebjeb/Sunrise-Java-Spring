---
layout: section
---

# Hibernate & JPA Mapping

Teach Java what a table is — and teach the database what a recipe is.
---
layout: default
---

# Learning Objectives

<v-clicks>

- Say what JPA is, what Hibernate is, and why there are two names for one job
- Configure a database, Hibernate and a connection pool with a few properties
- Turn a class into an entity — and explain why it cannot be a record
- Map a one-to-many relationship, and say which side owns it
- Replace a hand-written repository with an interface — and say what writes the class
- Ask the repository a question with a method name — and get the answer a page at a time
- Read the SQL log, and spot the N+1 problem in it

</v-clicks>
---
layout: section
---

# Who you are building for today
---
layout: statement
---

# *"Every time you restart the server, every recipe we added is gone."*

Sunrise Kitchen
---
layout: default
---

# They are right

```java
@Repository
public class InMemoryRecipeRepository implements RecipeRepository {

    private final Map<String, Recipe> shelf = new LinkedHashMap<>();
```

<v-click>

<div class="mt-6 text-xl">

A map lives in memory. Memory is wiped when the program stops.

</div>

</v-click>

<v-click>

<div class="mt-6 text-xl">

What they need is a **database** — something that keeps data whether your program runs or not.

</div>

</v-click>
---
layout: default
---

# Watch it happen

```bash
curl -X POST http://localhost:8080/recipes -H "Content-Type: application/json" \
  -d '{"title":"Nom Banh Chok","cuisine":"khmer","minutes":60,"servings":4,"ingredients":["rice noodles"]}'

curl http://localhost:8080/recipes      # four recipes
```

<v-click>

<div class="mt-4 text-xl">

Stop the application. Start it again.

</div>

</v-click>

<v-click>

```bash
curl http://localhost:8080/recipes      # three recipes
```

</v-click>

<v-click>

<div class="mt-4 text-xl font-bold">

Nom Banh Chok is gone. Nobody deleted it.

</div>

</v-click>
---
layout: default
---

# Two sessions, one thing at a time

<div class="grid grid-cols-2 gap-8 mt-8">
<div class="p-6 rounded-lg border-2 border-blue-500">

### Today

Teach Hibernate what a recipe **is** — tables, columns, the ingredients.

Then switch the application from the map to a database.

</div>
<div class="p-6 rounded-lg border-2 border-gray-400">

### Next session

PostgreSQL — so the data survives a restart — and a migration tool that owns the tables.

</div>
</div>
---
layout: default
---

# Today, in four steps

| Checkpoint | What you do | How you check it |
|---|---|---|
| 1 | Hibernate, H2 and a connection pool in the build | the pool's log lines · the H2 console |
| 2 | `Recipe` and `Ingredient` become entities | the `create table` log |
| 3 | Spring Data writes the repository | `curl` · rows in the H2 console |
| 4 | Quick recipes, and a page at a time | `curl` · two queries in the log |

<v-click>

<div class="mt-6 text-lg">

**Your twelve tests stay green at every checkpoint.** They prove that changing everything
underneath did not change what the API does.

</div>

</v-click>
---
layout: default
---

# Today's database: H2

<v-clicks>

- A real SQL database — tables, rows, `select`, `insert`
- Written in Java. It runs **inside your application**
- Nothing to install, nothing to start
- It lives in memory, so it also forgets on restart — for now

</v-clicks>

<v-click>

<div class="mt-8 text-xl">

H2 is where you learn. **Next session the application moves to PostgreSQL** — have it installed.

</div>

</v-click>
---
layout: section
---

# Objects and tables do not fit
---
layout: default
---

# Two shapes

| In Java | In a database |
|---|---|
| An object | A row |
| A field | A column |
| A reference to another object | A foreign key |
| `new Recipe()` — exists immediately | Exists after an `INSERT` |
| Identity: `==` | Identity: the primary key |
---
layout: default
---

# The hard one

```java
public class Recipe {
    private List<Ingredient> ingredients;   // a list, inside the object
}
```

<v-click>

<div class="mt-6 text-2xl font-bold">

A table has no lists.

</div>

</v-click>

<v-click>

<div class="mt-4 text-xl">

The ingredients are rows in **another table**, each carrying the id of its recipe.

</div>

</v-click>

<v-click>

<div class="mt-8 text-lg opacity-80">

Translating between the two by hand — for every class, forever — is called **object–relational
mapping. ORM.**

</div>

</v-click>
---
layout: default
---

# By hand, it looks like this

```java
PreparedStatement statement = connection.prepareStatement(
        "select id, title, cuisine, minutes, servings from recipes where id = ?");
statement.setLong(1, id);
ResultSet row = statement.executeQuery();
if (row.next()) {
    Recipe recipe = new Recipe();
    recipe.setId(row.getLong("id"));
    recipe.setTitle(row.getString("title"));
    recipe.setCuisine(row.getString("cuisine"));
    recipe.setMinutes(row.getInt("minutes"));
    recipe.setServings(row.getInt("servings"));
}
```

<v-click>

That reads **one** recipe. Without its ingredients.

</v-click>
---
layout: statement
---

# Now write it again for save, update, delete and list

For every class. Forever.
---
layout: default
---

# What an ORM does instead

<v-clicks>

- You describe the mapping **once**, with annotations on the class
- It writes the `insert`, `select`, `update`, `delete`
- It turns rows back into objects
- It follows the foreign keys and rebuilds the lists

</v-clicks>

<v-click>

<div class="mt-8 text-xl">

You stopped typing constructors and mapping methods last session. Today you stop typing SQL.

</div>

</v-click>
---
layout: default
---

# JPA and Hibernate

<div class="grid grid-cols-2 gap-8 mt-6">
<div class="p-6 rounded-lg bg-blue-500/10 border-2 border-blue-500">

### JPA

**The rulebook.** A specification: annotations and interfaces.

`@Entity` · `@Id` · `@OneToMany` · `EntityManager`

You cannot run it.

</div>
<div class="p-6 rounded-lg bg-green-500/10 border-2 border-green-600">

### Hibernate

**The worker.** The library that reads the annotations, writes the SQL, and runs it.

</div>
</div>

<v-click>

<div class="mt-8 text-xl">

Every annotation today is `import jakarta.persistence.*` — **not** Hibernate.

</div>

</v-click>
---
layout: default
---

# Look at your imports

```java
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
```

<v-click>

<div class="mt-6 text-xl">

Every one is `jakarta.persistence` — the **specification**.

</div>

</v-click>

<v-click>

<div class="mt-4 text-xl">

Not one line says `org.hibernate`.

</div>

</v-click>
---
layout: default
---

# Pause

<div class="mt-8 text-2xl">

Hibernate is not the only JPA implementation. **EclipseLink** is another.

</div>

<v-click>

<div class="mt-8 text-2xl font-bold">

If your project switched next year — which of today's files would change?

</div>

</v-click>

<v-click>

<div class="mt-6 text-xl">

None of the entities. That is what the specification buys you.

</div>

</v-click>
---
layout: default
---

# The whole stack, top to bottom

<div class="mt-4 flex flex-col gap-2 text-center text-lg">
<div class="p-3 rounded-lg bg-blue-500/10 border-2 border-blue-500"><b>Your code</b> — entities, with JPA annotations</div>
<div class="p-3 rounded-lg bg-blue-500/10 border-2 border-blue-300"><b>JPA</b> — the rulebook your code is written against</div>
<div class="p-3 rounded-lg bg-green-500/10 border-2 border-green-600"><b>Hibernate</b> — writes the SQL</div>
<div class="p-3 rounded-lg bg-orange-500/10 border-2 border-orange-500"><b>HikariCP</b> — lends it a connection</div>
<div class="p-3 rounded-lg bg-gray-500/10 border-2 border-gray-500"><b>H2</b> — the database</div>
</div>

<v-click>

<div class="mt-6 text-lg">

Spring Boot sets up all four lower layers from one starter and a few properties.

</div>

</v-click>
---
layout: statement
---

# Your code speaks JPA. Hibernate is the one listening.

The specification-and-implementation idea from the first session, paid off
---
layout: section
---

# Hibernate in a Spring Boot application
---
layout: default
---

# Two dependencies

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
```

<v-clicks>

- **data-jpa** — Hibernate, the JPA annotations, a connection pool
- **h2** — the database itself, running *inside* your application
- `runtime` — your code never imports it

</v-clicks>
---
layout: default
---

# A few properties

```properties
spring.datasource.url=jdbc:h2:mem:recipes
spring.datasource.username=sa
spring.datasource.password=

spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

<v-clicks>

- `spring.datasource.*` — **where** the database is
- `spring.jpa.*` — **how Hibernate behaves**
- `show-sql` — print every statement. Leave it on while you learn

</v-clicks>
---
layout: default
---

# Reading the URL

```properties
spring.datasource.url=jdbc:h2:mem:recipes
```

| Piece | Means |
|---|---|
| `jdbc:` | A database address, for Java |
| `h2:` | Which database |
| `mem:` | Keep it in memory |
| `recipes` | The database's name |

<v-click>

<div class="mt-6 text-lg">

For PostgreSQL — next session — this line names a server and a port instead. Nothing else moves.

</div>

</v-click>
---
layout: default
---

# What `show-sql` gives you

```sql
Hibernate:
    insert
    into
        recipes
        (cuisine, internal_notes, minutes, servings, title, id)
    values
        (?, ?, ?, ?, ?, default)
```

<v-clicks>

- Every statement Hibernate sends, as it sends it
- `?` — the values go in separately. You never see them here
- **The log is where you find out what Hibernate actually does**

</v-clicks>
---
layout: default
---

# `ddl-auto` — who creates the tables?

| Value | At startup Hibernate… |
|---|---|
| `create-drop` | creates the tables, drops them at shutdown — **today** |
| `create` | drops and re-creates. Every row gone |
| `update` | *alters* tables to match, and guesses |
| `validate` | creates nothing — checks, and refuses to start on a mismatch |
| `none` | does nothing |

<v-click>

<div class="mt-6 text-lg">

`create-drop` throws the data away on every restart. **Next session: `validate`**, and a real
migration tool builds the tables.

</div>

</v-click>
---
layout: statement
---

# `update` exists. Do not use it on a database you care about.

It never deletes, never renames, and never tells you what it guessed
---
layout: default
---

# The connection pool

<v-clicks>

- Opening a database connection is **slow** — a network handshake, a login
- A **pool** opens a few at startup and keeps them
- A request **borrows** one, uses it, gives it back

</v-clicks>

<v-click>

<div class="mt-8 text-xl">

Boot's pool is **HikariCP**. Already on your classpath. Nothing to configure to make it work.

</div>

</v-click>
---
layout: default
---

# Borrow, use, give back

<v-clicks>

1. At startup the pool opens some connections — and keeps them
2. A request needs the database: it **borrows** one
3. It runs its queries
4. It **gives it back**. The connection stays open, ready for the next request

</v-clicks>

<v-click>

<div class="mt-6 text-lg">

Your code never sees this. When Hibernate "closes" a connection, it goes back to the pool.

</div>

</v-click>
---
layout: statement
---

# A pool is also a limit

With a pool of ten, your application can never use more than ten connections

*The eleventh request waits its turn — instead of pushing the database over*
---
layout: default
---

# The four settings that matter

`spring.datasource.hikari.…` — times in milliseconds

| Setting | Default | Means |
|---|---|---|
| `maximum-pool-size` | 10 | The most connections the pool will ever hold |
| `minimum-idle` | same as maximum | How many stay open when nobody is asking |
| `connection-timeout` | 30000 | How long a request waits for a free one before failing |
| `max-lifetime` | 1800000 | No connection lives longer. Retired and replaced, quietly |

<v-click>

`maximum-pool-size` is the one that matters most.

</v-click>
---
layout: default
---

# The two you set today

```properties
spring.datasource.hikari.maximum-pool-size=5
spring.datasource.hikari.connection-timeout=5000
```

<v-clicks>

- At most **five** connections
- A request waits at most **five seconds** for one, then fails with a clear error
- A fast, clear failure is kinder than a request that hangs for thirty seconds

</v-clicks>
---
layout: default
---

# How big should the pool be?

<v-click>

<div class="mt-6 text-3xl font-bold">

Smaller than you think.

</div>

</v-click>

<v-clicks>

- Ten connections doing real work beat a hundred fighting over the same disk
- Past a point, every extra connection makes the database **slower**
- Start with the default of 10
- Hikari's rule of thumb: about **twice the database server's cores, plus one** — then measure

</v-clicks>
---
layout: default
---

# When the pool runs out

```
java.sql.SQLTransientConnectionException: HikariPool-1 - Connection is not available,
request timed out after 5000ms (total=5, active=5, idle=0, waiting=0)
```

| Number | Says |
|---|---|
| `total=5` | The pool is at its maximum |
| `active=5` | Every one is in use |
| `idle=0` | None is free |
| `waiting=0` | Nobody else is queueing |

<v-click>

It almost never means "make the pool bigger". Something is **holding** connections: a slow query, or code that never gave one back.

</v-click>
---
layout: default
---

# In your log

```
HikariPool-1 - Starting...
HikariPool-1 - Added connection ... url=jdbc:h2:mem:recipes user=SA
HikariPool-1 - Start completed.
```

<v-click>

<div class="mt-8 text-xl">

That is the pool opening its first connection.

</div>

</v-click>

<v-click>

<div class="mt-4 text-xl font-bold">

You never wrote a line of JDBC.

</div>

</v-click>
---
layout: default
---

# A window into the database

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-h2console</artifactId>
</dependency>
```

```properties
spring.h2.console.enabled=true
```

<v-click>

<div class="mt-6 text-xl">

**http://localhost:8080/h2-console** — look at tables, run SQL by hand.

</div>

</v-click>

<v-click>

<div class="mt-4 text-lg opacity-80">

Spring Boot 4 moved it into its own module. Without the dependency: a 404.

</div>

</v-click>
---
layout: default
---

# Logging in to the console

| Field | Type this |
|---|---|
| **JDBC URL** | `jdbc:h2:mem:recipes` |
| **User Name** | `sa` |
| **Password** | *(leave empty)* |

<v-clicks>

- The URL on the page is **not** filled in for you — it shows a default. Type yours
- The application must be running: the database lives inside it
- `Database "mem:recipes" not found` → the URL is wrong

</v-clicks>
---
layout: default
---

# Checkpoint 1 — what you do

<v-clicks>

1. Four dependencies into `pom.xml` — `data-jpa`, `h2`, `h2console`, `data-jpa-test`
2. Replace `application.properties`
3. Reload Maven, `mvn test` — twelve green
4. Start the application, find the three `HikariPool-1` lines
5. Open the H2 console and connect

</v-clicks>

<v-click>

<div class="mt-6 text-lg">

The database is there — and **empty**. Hibernate has no entities to make tables for yet.

</div>

</v-click>
---
layout: statement
---

# Checkpoint 1

Hibernate, H2 and the pool in the build · `recipe-api/README.md`

*Twelve green · `HikariPool-1 - Start completed` · an empty database in the console*
---
layout: section
---

# An entity
---
layout: default
---

# One class, one table

```java {all|1|2|8-10|12-13|18|20}
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
    private String internalNotes;
}
```
---
layout: default
---

# What each one means

| Annotation | Means |
|---|---|
| `@Entity` | This class is stored |
| `@Table(name = "recipes")` | The table's name. Default: `recipe` |
| `@Id` | The primary key |
| `@GeneratedValue(IDENTITY)` | The **database** hands out the id |
| `@Column(nullable = false, length = 100)` | `varchar(100) not null` |
| *(nothing)* | Still a column |

<v-click>

<div class="mt-6 text-lg">

`internalNotes` becomes `internal_notes` — Boot turns `camelCase` into `snake_case` for you.

</div>

</v-click>
---
layout: default
---

# Annotation → SQL

| You wrote | Hibernate built |
|---|---|
| `@Table(name = "recipes")` | `create table recipes (` |
| `@Id @GeneratedValue(IDENTITY) Long id` | `id bigint generated by default as identity` |
| `@Column(nullable = false, length = 100) String title` | `title varchar(100) not null` |
| `int minutes` | `minutes integer not null` |
| `String internalNotes` | `internal_notes varchar(255)` |

<v-click>

<div class="mt-6 text-lg">

No annotation → still a column. `int` cannot be null, so `not null`. A `String` can, so it is allowed.

</div>

</v-click>
---
layout: default
---

# What `IDENTITY` does

```sql
insert into recipes
    (cuisine, internal_notes, minutes, servings, title, id)
values
    (?, ?, ?, ?, ?, default)
```

<v-clicks>

- `default` — *"database, you choose the id"*
- The database's own counter fills it in: 1, 2, 3…
- Hibernate reads the new id back and sets it on **your object**

</v-clicks>
---
layout: default
---

# The id is a `Long` now

<v-clicks>

- Last session the map made ids: `"1"`, `"2"` — strings
- Now the **database** counts: insert without an id, it fills one in
- `Long`, not `long` — a new recipe has **no id yet**, and `null` says that

</v-clicks>

<v-click>

<div class="mt-8 text-xl font-bold">

`String` → `Long` is why checkpoint 2 touches every layer.

</div>

</v-click>
---
layout: default
---

# `Long`, not `long`

<div class="grid grid-cols-2 gap-8 mt-6">
<div class="p-6 rounded-lg border-2 border-red-500">

### `long id`

A new recipe has id `0`.

`0` looks like a real id.

</div>
<div class="p-6 rounded-lg border-2 border-green-600">

### `Long id`

A new recipe has id `null`.

`null` means *"not saved yet"* — and Hibernate reads it exactly that way.

</div>
</div>
---
layout: default
---

# Why an entity cannot be a record

Hibernate builds an object from a row in two steps:

<v-clicks>

1. **Create it empty** — needs a no-argument constructor. A record has none
2. **Fill in the fields** — needs fields it can set. A record's are `final`
3. Sometimes: hand you a generated **subclass** that loads later. A record is `final`

</v-clicks>

<v-click>

<div class="mt-8 text-xl">

An ordinary class: `@NoArgsConstructor` for Hibernate, `@Getter` `@Setter` for everyone.

</div>

</v-click>
---
layout: default
---

# The DTOs stay records

<div class="mt-6 text-xl">

`CreateRecipeRequest` · `RecipeResponse`

</div>

<v-click>

<div class="mt-8 text-2xl font-bold">

Hibernate never sees them.

</div>

</v-click>

<v-click>

<div class="mt-4 text-lg">

Records are still right for data built once and never changed. An entity is neither.

</div>

</v-click>
---
layout: default
---

# The `@Data` trap, now you can see it

`@Data` writes `equals`, `hashCode`, `toString` over **every** field — including `ingredients`.

<v-clicks>

- `toString()` → prints the ingredients → each prints its recipe → prints its ingredients → **stack overflow**
- `hashCode()` changes when the database assigns the id → lost inside a `HashSet`

</v-clicks>

<v-click>

<div class="mt-8 text-2xl font-bold">

`@Getter` · `@Setter` · `@NoArgsConstructor`. Nothing that does everything.

</div>

</v-click>
---
layout: default
---

# What goes on an entity

```java
@Entity
@Table(name = "recipes")
@Getter
@Setter
@NoArgsConstructor
public class Recipe {
```

| Annotation | For whom |
|---|---|
| `@NoArgsConstructor` | Hibernate — it creates the object empty |
| `@Setter` | Hibernate fills the fields; so does MapStruct |
| `@Getter` | Everyone who reads a recipe |
---
layout: default
---

# An aside: enums

```java
@Enumerated(EnumType.STRING)
private Cuisine cuisine;
```

<v-clicks>

- **Always `STRING`.** The default stores the *position*: `0`, `1`, `2`
- Someone adds `FRENCH` at the top of the enum → every row changes meaning, silently
- Our cuisine stays a `String` — the client sends free text

</v-clicks>
---
layout: section
---

# Two tables, one relationship
---
layout: statement
---

# *"A recipe has a list of ingredients. Every ingredient belongs to exactly one recipe. Delete a recipe and its ingredients go with it."*

The client
---
layout: default
---

# Start from the database

<div class="mt-4 text-xl">

One recipe, many ingredients. In tables, that is always the same shape:

</div>

<v-clicks>

- A `recipes` table
- An `ingredients` table
- A **`recipe_id`** column on `ingredients`, holding the id of its recipe

</v-clicks>

<v-click>

<div class="mt-8 text-2xl font-bold">

The "many" side always carries the foreign key.

</div>

</v-click>

<v-click>

<div class="mt-4 text-lg opacity-80">

A recipe row cannot hold a list of ingredient ids. An ingredient row can hold one recipe id.

</div>

</v-click>
---
layout: default
---

# The ingredient

```java {all|13-15}
@Entity
@Table(name = "ingredients")
@Getter @Setter @NoArgsConstructor
public class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id")
    private Recipe recipe;
}
```

<v-click>

Many ingredients → one recipe. **This field is the foreign key.**

</v-click>
---
layout: default
---

# The recipe's side

```java {all|1|5-7}
@OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
@OrderBy("id")
private List<Ingredient> ingredients = new ArrayList<>();

public void addIngredient(String name) {
    ingredients.add(new Ingredient(name, this));
}
```

<v-click>

<div class="mt-6 text-xl">

`mappedBy = "recipe"` — *"store nothing for me. Look at the `recipe` field on `Ingredient`."*

</div>

</v-click>
---
layout: image
image: /session-09-recipe-tables.svg
backgroundSize: contain
---
---
layout: default
---

# One fact, two ends

<div class="grid grid-cols-2 gap-8 mt-6">
<div class="p-6 rounded-lg bg-red-500/10 border-2 border-red-600">

### `Ingredient.recipe`

`@ManyToOne` — the **owning side**.

What is in this field is what gets written to `recipe_id`.

</div>
<div class="p-6 rounded-lg bg-blue-500/10 border-2 border-blue-500">

### `Recipe.ingredients`

`@OneToMany(mappedBy)` — the **mirror**.

Convenient to read. Never written.

</div>
</div>
---
layout: statement
---

# Add to the list only, and nothing useful is stored

The list is the mirror. The mirror is not written.

*That is what `addIngredient` is for: it sets both ends in one call.*
---
layout: default
---

# The bug, and the fix

```java
// compiles, runs, and the ingredient has no recipe
recipe.getIngredients().add(new Ingredient("lime", null));
```

<v-click>

```java
// sets both ends
recipe.addIngredient("lime");
```

```java
public void addIngredient(String name) {
    ingredients.add(new Ingredient(name, this));   // `this` fills in the owning side
}
```

</v-click>

<v-click>

<div class="mt-4 text-xl font-bold">

Make `addIngredient` the only way anyone adds one.

</div>

</v-click>
---
layout: default
---

# The rest of that line

<v-clicks>

- **`cascade = ALL`** — save a recipe, its ingredients are saved. Delete it, they go too
- **`orphanRemoval = true`** — take an ingredient *out* of the list, its row is deleted
- **`@OrderBy("id")`** — a table has no order. This asks for the order they were added

</v-clicks>
---
layout: default
---

# `cascade`, in the log

```java
entityManager.persist(lokLak);   // one call, one recipe
```

<v-click>

```sql
insert into recipes     (cuisine, internal_notes, minutes, servings, title, id) values (?, ?, ?, ?, ?, default)
insert into ingredients (name, recipe_id, id) values (?, ?, default)
insert into ingredients (name, recipe_id, id) values (?, ?, default)
insert into ingredients (name, recipe_id, id) values (?, ?, default)
```

</v-click>

<v-click>

<div class="mt-4 text-xl">

You saved a recipe. Hibernate saved its three ingredients. **You never save an ingredient yourself.**

</div>

</v-click>
---
layout: default
---

# A table has no order

<v-clicks>

- A Java `List` keeps the order you added things in
- A table does not. Rows come back in whatever order the database likes
- `["fish", "coconut milk", "kroeung"]` could come back shuffled

</v-clicks>

<v-click>

```java
@OrderBy("id")
private List<Ingredient> ingredients = new ArrayList<>();
```

```sql
select ... from ingredients i1_0 where i1_0.recipe_id=? order by i1_0.id
```

</v-click>
---
layout: default
---

# `LAZY` — loaded when you touch it

<v-clicks>

- Load an ingredient → its recipe is **not** loaded until `getRecipe()`
- Load a recipe → its ingredients are **not** loaded until something reads the list
- The right default: showing one title should not drag in half the database

</v-clicks>

<v-click>

<div class="mt-8 text-xl">

It has a price. You will see it in the log before the session ends.

</div>

</v-click>
---
layout: default
---

# Lazy, in the log

```java
Recipe found = entityManager.find(Recipe.class, id);
```

```sql
select ... from recipes r1_0 where r1_0.id=?
```

<v-click>

```java
found.getIngredients().size();     // only now
```

```sql
select ... from ingredients i1_0 where i1_0.recipe_id=? order by i1_0.id
```

</v-click>

<v-click>

<div class="mt-4 text-xl">

Two statements, and the second only happens **if somebody asks**.

</div>

</v-click>
---
layout: default
---

# Before you delete `mappedBy`…

<div class="mt-8 text-2xl">

Checkpoint 2 has you delete `mappedBy = "recipe"` and restart.

</div>

<v-click>

<div class="mt-8 text-3xl font-bold">

What will Hibernate build?

</div>

</v-click>

<v-click>

<div class="mt-6 text-lg opacity-80">

Write your answer down. You will read the real one in the log.

</div>

</v-click>
---
layout: section
---

# Break — 10 minutes
---
layout: section
---

# Every layer follows
---
layout: default
---

# Your build is red — expected

`Recipe` is not a record any more:

<v-clicks>

- `recipe.id()` → `recipe.getId()`
- the seven-argument constructor → gone
- `withId(...)` → gone
- `String id` → `Long id`

</v-clicks>

<v-click>

<div class="mt-6 text-xl">

Repository · service · mapper · one line of the DTO · three lines of the controller · the tests.

</div>

</v-click>
---
layout: default
---

# The files that change

| Layer | File | What changes |
|---|---|---|
| domain | `Recipe`, `Ingredient` | The entities |
| repository | `RecipeRepository`, `InMemoryRecipeRepository` | `Long` ids, two methods changed |
| service | `RecipeService`, `RecipeServiceImpl` | `Long` ids, one renamed call |
| dto | `RecipeResponse` | One word |
| mapper | `RecipeMapper` | Ingredients |
| controller | `RecipeController` | Three lines |
| tests | two classes + `TestRecipes` | How a recipe is built |

<v-click>

The big files are printed whole in the README. The small changes you make **by hand**.

</v-click>
---
layout: default
---

# The repository: two changes

```java {all|5|9}
public interface RecipeRepository {

    List<Recipe> findAll();
    Optional<Recipe> findById(Long id);
    List<Recipe> findByCuisineIgnoreCase(String cuisine);   // was findByCuisine

    Recipe save(Recipe recipe);

    void deleteById(Long id);                               // returned boolean
}
```

<v-click>

These are the words Spring Data uses. Checkpoint 3 shows why that matters.

</v-click>
---
layout: default
---

# Still a map — holding entities

```java
@Override
public Recipe save(Recipe recipe) {
    if (recipe.getId() == null) {
        recipe.setId(nextId.getAndIncrement());
    }
    shelf.put(recipe.getId(), recipe);
    return recipe;
}
```

<v-clicks>

- `Map<Long, Recipe>` now, not `Map<String, Recipe>`
- The map still hands out the ids **for the running application**
- At checkpoint 3 the map goes, and the database takes over

</v-clicks>
---
layout: default
---

# The service: `String` becomes `Long`

```java
public Recipe findById(Long id) {                       // was String
    return recipeRepository
        .findById(id)
        .orElseThrow(() -> new RecipeNotFoundException("recipe id: " + id + " not found"));
}

public void delete(Long id) {                           // was String
    recipeRepository.deleteById(id);
}
```

<v-click>

<div class="mt-6 text-lg">

Two words here, and one renamed call in `findAll`. The logic does not move. **You make these
changes by hand.**

</div>

</v-click>
---
layout: default
---

# Controller and DTO: almost nothing

```java
public ResponseEntity<RecipeResponse> getRecipeById(
        @PathVariable Long id) {                              // was String
```

```java
.created(URI.create("/recipes/" + savedRecipe.getId()))       // was savedRecipe.id()
```

```java
public record RecipeResponse(
    Long id,                                                  // was String
    String title, ...
```

<v-click>

<div class="mt-4 text-xl">

Spring turns `/recipes/1` into the `Long` for you.

</div>

</v-click>
---
layout: default
---

# The mapper learns ingredients

```java {all|1-3|5-7|9-12}
default String toName(Ingredient ingredient) {
    return ingredient.getName();
}

@Mapping(target = "id", ignore = true)
@Mapping(target = "ingredients", ignore = true)
Recipe toRecipe(CreateRecipeRequest request);

@AfterMapping
default void addIngredients(CreateRecipeRequest request, @MappingTarget Recipe recipe) {
    request.ingredients().forEach(recipe::addIngredient);
}
```

<v-click>

The client still sends `["fish", "lime"]`. The mapper turns names into ingredients — through
`addIngredient`.

</v-click>
---
layout: default
---

# The tests: one helper

```java
public static Recipe recipe(Long id, String title, String cuisine, int minutes, int servings,
                            String internalNotes, String... ingredients) {
    Recipe recipe = new Recipe();
    recipe.setId(id);
    recipe.setTitle(title);
    // ... the other setters
    for (String ingredient : ingredients) {
        recipe.addIngredient(ingredient);
    }
    return recipe;
}
```

<v-click>

```java
private static final Recipe AMOK = recipe(1L, "Fish Amok", "khmer", 45, 4,
        "supplier: central market", "fish", "coconut milk", "kroeung");
```

</v-click>
---
layout: default
---

# The JSON, before and after

```json
{
  "id": 1,
  "title": "Fish Amok",
  "cuisine": "khmer",
  "minutes": 45,
  "servings": 4,
  "ingredients": ["fish", "coconut milk", "kroeung"]
}
```

<v-clicks>

- `"id": 1` — a number now. It was `"1"`
- Everything else: **identical**
- Two tables and a foreign key underneath, and the client sees a list of strings

</v-clicks>
---
layout: default
---

# What does **not** change

<div class="mt-6 text-xl">

What the tests check over HTTP.

</div>

<v-clicks>

- Same URLs
- Same status codes
- Same JSON — `"ingredients": ["fish", "coconut milk", "kroeung"]`

</v-clicks>

<v-click>

<div class="mt-8 text-2xl font-bold">

The client cannot tell. That is what the DTOs were for.

</div>

</v-click>
---
layout: default
---

# Checkpoint 2 — what you do

<v-clicks>

1. Replace `Recipe` and `Ingredient` — the build goes red
2. Work down the README's list — big files replaced, small changes by hand
3. `mvn test` — twelve green again
4. Start the application, read the `create table` output
5. H2 console: `RECIPES` and `INGREDIENTS` exist — and are empty
6. Delete `mappedBy`, restart, read the log. Put it back

</v-clicks>
---
layout: statement
---

# Checkpoint 2

The two entities, then every file that used the record · `recipe-api/README.md`

*Twelve green · read the `create table` log · two empty tables in the console*
---
layout: default
---

# What you should see

```sql
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

<v-click>

And without `mappedBy`: a **third** table, `recipes_ingredients`. Two relationships where you meant
one.

</v-click>
---
layout: default
---

# Without `mappedBy`

```sql
create table recipes_ingredients (
    ingredients_id bigint not null unique,
    recipe_id bigint not null
)
```

<v-clicks>

- A **third** table nobody asked for
- Hibernate saw two separate relationships: the list, and the `recipe` field
- `mappedBy = "recipe"` says they are **the same one**, seen from the other end

</v-clicks>
---
layout: default
---

# Two tables — and both empty

```bash
curl http://localhost:8080/recipes       # three recipes
```

```sql
select * from recipes;                   -- no rows
```

<v-click>

<div class="mt-6 text-xl">

The running application still reads **the map**.

</div>

</v-click>

<v-click>

<div class="mt-4 text-xl">

The tables are ready. Nothing uses them — until checkpoint 3.

</div>

</v-click>
---
layout: section
---

# The repository you stop writing
---
layout: default
---

# Every entity, every project

```java
public Optional<Recipe> findById(Long id) {
    return Optional.ofNullable(entityManager.find(Recipe.class, id));
}

public Recipe save(Recipe recipe) {
    if (recipe.getId() == null) {
        entityManager.persist(recipe);
        return recipe;
    }
    return entityManager.merge(recipe);
}

public boolean existsById(Long id) { ... }
public void deleteById(Long id) { ... }
public List<Recipe> findAll() { ... }
```

<v-click>

Typing that carries no decision. The same smell as last session's constructors and mappers.

</v-click>
---
layout: default
---

# An interface, and nothing else

```java
public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    List<Recipe> findByCuisineIgnoreCase(String cuisine);
}
```

<v-click>

<div class="mt-6 text-xl">

`JpaRepository<Recipe, Long>` — a repository of `Recipe`s, whose id is a `Long`.

</div>

</v-click>

<v-click>

<div class="mt-4 text-lg">

`findAll` · `findById` · `save` · `existsById` · `deleteById` · `count` · and a dozen more. Free.

</div>

</v-click>
---
layout: default
---

# Reading that one line

```java
public interface RecipeRepository extends JpaRepository<Recipe, Long>
```

| Piece | Means |
|---|---|
| `interface` | You declare **what** you need. No bodies |
| `extends JpaRepository` | …and inherit the methods every repository has |
| `<Recipe,` | The entity this repository stores |
| `Long>` | The type of its `@Id` field |
---
layout: default
---

# What you get for free

| Method | Does |
|---|---|
| `findAll()` | Every recipe |
| `findById(id)` | One, as an `Optional` |
| `save(recipe)` | Insert if it has no id, update if it has |
| `existsById(id)` | `true` or `false`, without loading it |
| `deleteById(id)` | Delete — and cascade to its ingredients |
| `count()` | How many |

<v-click>

You have written every one of these by hand. Twice.

</v-click>
---
layout: default
---

# Predict

<div class="mt-8 text-xl">

`RecipeServiceImpl` asks for a `RecipeRepository`.

**No class in your project implements it.**

</div>

<v-click>

<div class="mt-8 text-3xl font-bold">

What does Spring inject?

</div>

</v-click>
---
layout: default
---

# Ask it

```java
System.out.println(recipeRepository.getClass().getName());
```

<v-click>

```
jdk.proxy2.$Proxy223
```

</v-click>

<v-clicks>

- A **proxy** — the same trick as last session's aspect
- The service's proxy was `RecipeServiceImpl$$SpringCGLIB$$0` — a subclass of *your class*
- This one implements *your interface*. **There is no class of yours behind it**

</v-clicks>
---
layout: default
---

# Two kinds of proxy

<div class="grid grid-cols-2 gap-8 mt-6">
<div class="p-6 rounded-lg border-2 border-gray-500">

### The service

`RecipeServiceImpl$$SpringCGLIB$$0`

A generated **subclass** of a class you wrote.

Your code runs inside it.

</div>
<div class="p-6 rounded-lg border-2 border-orange-500">

### The repository

`jdk.proxy2.$Proxy223`

A generated object that implements **your interface**.

There is no code of yours inside it at all.

</div>
</div>
---
layout: default
---

# What Spring Data does at startup

<v-clicks>

1. Scans your packages for interfaces that extend `Repository`
2. Finds `RecipeRepository`
3. Reads each method: inherited? a name it can turn into a query? an `@Query`?
4. Builds a proxy that answers all of them
5. Registers it as a bean — so the service can ask for it

</v-clicks>

<v-click>

<div class="mt-6 text-lg">

A method it cannot make sense of → **the application refuses to start.** Better than a surprise later.

</div>

</v-click>
---
layout: image
image: /session-09-repository-proxy.svg
backgroundSize: contain
---
---
layout: default
---

# Why you changed two methods an hour ago

```java
return recipeRepository.findById(id).orElseThrow(...);
recipeRepository.findByCuisineIgnoreCase(cuisine);
recipeRepository.deleteById(id);
```

<v-click>

<div class="mt-6 text-xl">

Every one is a `JpaRepository` method — same name, same signature.

</div>

</v-click>
---
layout: default
---

# The interface, before and after

```java
public interface RecipeRepository {
    List<Recipe> findAll();
    Optional<Recipe> findById(Long id);
    List<Recipe> findByCuisineIgnoreCase(String cuisine);
    Recipe save(Recipe recipe);
    void deleteById(Long id);
}
```

<v-click>

```java
public interface RecipeRepository extends JpaRepository<Recipe, Long> {
    List<Recipe> findByCuisineIgnoreCase(String cuisine);
}
```

</v-click>

<v-click>

Four of the five were already `JpaRepository`'s. The fifth is a name Spring Data can read.

</v-click>
---
layout: statement
---

# The service does not change. The controller does not change. The tests do not change.

That is what the layers from the REST session were for
---
layout: default
---

# The tests do not notice either

```java
@MockitoBean
private RecipeRepository recipeRepository;

when(recipeRepository.findById(1L)).thenReturn(Optional.of(AMOK));
```

<v-clicks>

- The controller tests **mock** the repository
- A mock does not care whether the interface extends `JpaRepository`
- Same method names → same tests, untouched

</v-clicks>
---
layout: default
---

# Checkpoint 3 — what you do

<v-clicks>

1. **Delete** `InMemoryRecipeRepository.java` — the whole file
2. Replace `RecipeRepository` — it now `extends JpaRepository<Recipe, Long>`
3. `mvn test` — twelve green. No test file was touched
4. Start the application, `curl` two recipes in
5. H2 console: `select * from ingredients`

</v-clicks>
---
layout: statement
---

# Checkpoint 3

Delete one class, change one interface · `recipe-api/README.md`

*Twelve green, untouched · `curl` two recipes · find their rows in the H2 console*
---
layout: default
---

# The list is empty — why?

```bash
curl http://localhost:8080/recipes
[]
```

<v-click>

<div class="mt-6 text-xl">

Fish Amok, Lok Lak and Carbonara were created in `@PostConstruct` — **inside the class you just
deleted.**

</div>

</v-click>

<v-click>

<div class="mt-6 text-xl">

Next session brings them back, properly: as a database change, not as Java code.

</div>

</v-click>
---
layout: default
---

# Your data, in a real table

```bash
curl -X POST http://localhost:8080/recipes -H "Content-Type: application/json" \
  -d '{"title":"Fish Amok","cuisine":"khmer","minutes":45,"servings":4,
       "ingredients":["fish","coconut milk","kroeung"]}'
```

<v-click>

```sql
select * from ingredients;
```

| ID | NAME | RECIPE_ID |
|---|---|---|
| 1 | fish | 1 |
| 2 | coconut milk | 1 |
| 3 | kroeung | 1 |

</v-click>

<v-click>

Twenty minutes ago these tables were empty. **The application writes to them now.**

</v-click>
---
layout: default
---

# Now read the log

`curl http://localhost:8080/recipes` — read the log:

```sql
select ... from recipes r1_0
select ... from ingredients i1_0 where i1_0.recipe_id=? order by i1_0.id
select ... from ingredients i1_0 where i1_0.recipe_id=? order by i1_0.id
```

<v-click>

<div class="mt-6 text-xl">

Two recipes, three `select`s.

</div>

</v-click>

<v-click>

<div class="mt-4 text-lg">

One for the list. Then one more **per recipe** — because the ingredients are lazy.

</div>

</v-click>
---
layout: statement
---

# N+1

One query for the list. N more — one per item.

*Nothing is broken. The answer is right. It is just slow in a way that grows with your data.*
---
layout: default
---

# It grows with your data

| Recipes | Queries |
|---|---|
| 3 | 4 |
| 100 | 101 |
| 10,000 | 10,001 |

<v-clicks>

- On your laptop, with three recipes: invisible
- In production, with real data: the page that takes eight seconds
- **The only place you can see it is the SQL log**

</v-clicks>
---
layout: default
---

# Pause

<div class="mt-8 text-2xl">

The restaurant has **400** recipes.

</div>

<v-click>

<div class="mt-8 text-3xl font-bold">

How many queries does `GET /recipes` send?

</div>

</v-click>

<v-click>

<div class="mt-6 text-xl">

401. Next session: one annotation, and they become one.

</div>

</v-click>
---
layout: section
---

# Asking the repository questions
---
layout: statement
---

# *"Show us the quick recipes. And the menu is growing — do not send all of it at once."*

Two requests. Neither needs a line of SQL from you
---
layout: default
---

# You have already written one

```java
List<Recipe> findByCuisineIgnoreCase(String cuisine);
```

<v-click>

`find` · `By` · `Cuisine` · `IgnoreCase`

</v-click>

<v-click>

```sql
select ... from recipes r1_0 where upper(r1_0.cuisine)=upper(?)
```

</v-click>

<v-click>

<div class="mt-4 text-xl">

A **derived query**. Spring Data reads the method **name** and writes the SQL.

</div>

</v-click>
---
layout: default
---

# How the name is read

`findByCuisineIgnoreCase`

| Word | Becomes |
|---|---|
| `find` | `select` |
| `By` | `where` |
| `Cuisine` | the `cuisine` field → the `cuisine` column |
| `IgnoreCase` | `upper(...) = upper(?)` |

<v-click>

<div class="mt-6 text-lg">

The parameter fills the `?`. One parameter for each condition, in the order they appear in the name.

</div>

</v-click>
---
layout: default
---

# Words that compare

| Method name | Asks for |
|---|---|
| `findByCuisine(c)` | cuisine equals |
| `findByMinutesLessThan(n)` | fewer than *n* minutes |
| `findByMinutesLessThanEqual(n)` | *n* minutes or fewer |
| `findByMinutesGreaterThan(n)` | more than *n* minutes |
| `findByMinutesBetween(a, b)` | from *a* to *b*, both included |
| `findByCuisineIn(list)` | cuisine is any one in the list |
| `findByInternalNotesIsNull()` | no notes at all |

<v-click>

`Between` takes **two** parameters. `IsNull` takes **none**.

</v-click>
---
layout: default
---

# Words for text

| Method name | SQL | Finds |
|---|---|---|
| `findByTitleContaining("am")` | `like '%am%'` | Fish **Am**ok |
| `findByTitleStartingWith("Lok")` | `like 'Lok%'` | **Lok** Lak |
| `findByTitleEndingWith("ara")` | `like '%ara'` | Carbon**ara** |
| `findByTitleContainingIgnoreCase("AM")` | `upper(...) like upper(...)` | Fish Amok |

<v-click>

<div class="mt-6 text-lg">

You pass the plain text. Spring Data adds the `%` signs.

</div>

</v-click>
---
layout: default
---

# Joining conditions, sorting, limiting

| Method name | Asks for |
|---|---|
| `findByCuisineAndServings(c, n)` | both must be true |
| `findByCuisineOrCuisine(a, b)` | either one |
| `findByCuisineOrderByMinutesAsc(c)` | sorted, quickest first |
| `findByCuisineOrderByMinutesDesc(c)` | sorted, slowest first |
| `findTop3ByOrderByMinutesAsc()` | only the three quickest |
| `findFirstByCuisineOrderByMinutesAsc(c)` | only the quickest one |

<v-click>

`OrderBy` always goes **last**. `Top3` and `First` go right after `find`.

</v-click>
---
layout: default
---

# It does not have to start with `find`

| Method name | Returns | Asks |
|---|---|---|
| `countByCuisine(c)` | `long` | how many? |
| `existsByTitle(t)` | `boolean` | is there one? |
| `deleteByCuisine(c)` | `void` | delete every match |

<v-click>

<div class="mt-6 text-lg">

`existsBy` and `countBy` never load a recipe. They are cheaper than `findBy(...).size()`.

</div>

</v-click>
---
layout: default
---

# The return type is part of the question

| You write | You are saying |
|---|---|
| `List<Recipe> findBy…` | there may be many — or none: an empty list |
| `Optional<Recipe> findBy…` | at most one — or none: an empty `Optional` |
| `Recipe findBy…` | exactly one — and `null` if there is none |
| `long countBy…` | a number |
| `boolean existsBy…` | yes or no |

<v-click>

<div class="mt-4 text-lg">

Prefer `Optional` to a bare `Recipe`. It makes you decide what happens when nothing is found.

</div>

</v-click>
---
layout: default
---

# Misspell a field

```java
List<Recipe> findByKitchen(String kitchen);
```

<v-click>

```
No property 'kitchen' found for type 'Recipe'
```

</v-click>

<v-clicks>

- The application **does not start**
- Spring Data checks every method name against the entity at startup
- A wrong name can never reach a user. You find out in ten seconds

</v-clicks>
---
layout: default
---

# Predict

```java
List<Recipe> findTop2ByCuisineIgnoreCaseOrderByMinutesAsc(String cuisine);
```

<div class="mt-4">

Fish Amok (khmer, 45) · Lok Lak (khmer, 25) · Nom Banh Chok (khmer, 60) · Carbonara (italian, 20)

</div>

<v-click>

<div class="mt-6 text-2xl font-bold">

What does `findTop2…("KHMER")` return?

</div>

</v-click>

<v-click>

<div class="mt-6 text-xl">

Lok Lak, then Fish Amok. Khmer only, any case, quickest first, two of them.

</div>

</v-click>
---
layout: default
---

# The quick recipes

```java
List<Recipe> findByMinutesLessThanEqualOrderByMinutesAsc(int minutes);
```

<v-click>

```sql
select ... from recipes r1_0 where r1_0.minutes<=? order by r1_0.minutes
```

</v-click>

<v-clicks>

- One line in the interface. No class, no SQL
- `GET /recipes/quick?maxMinutes=30` calls it, through the service
- When a name grows longer than this one, stop — next session's `@Query` is the better tool

</v-clicks>
---
layout: section
---

# A page at a time
---
layout: statement
---

# 400 recipes, one response

`GET /recipes` loads every row, builds every object, and sends all of it

*The client shows ten*
---
layout: default
---

# A page is two numbers

| | Means |
|---|---|
| **page** | which page — **starting at 0** |
| **size** | how many on each page |

<v-click>

| `page`, `size` | You get |
|---|---|
| 0, 10 | recipes 1–10 |
| 1, 10 | recipes 11–20 |
| 2, 10 | recipes 21–30 |

</v-click>

<v-click>

The first page is page **0**. Everybody gets this wrong once.

</v-click>
---
layout: default
---

# `Pageable` in, `Page` out

```java
Page<Recipe> findAll(Pageable pageable);
```

<v-clicks>

- You do **not** write this method. `JpaRepository` already has it
- `Pageable` — the request: which page, what size, in what order
- `Page<Recipe>` — the answer: the recipes on that page, **and how many there are in total**

</v-clicks>
---
layout: default
---

# Building the request

```java
Pageable pageable = PageRequest.of(0, 2, Sort.by("minutes"));

Page<Recipe> result = recipeRepository.findAll(pageable);
```

| Piece | Means |
|---|---|
| `0` | the first page |
| `2` | two recipes on a page |
| `Sort.by("minutes")` | quickest first |

<v-click>

`"minutes"` is the **field** name again — not the column.

</v-click>
---
layout: default
---

# Two queries, not one

```sql
select ... from recipes r1_0 order by r1_0.minutes fetch first ? rows only
select count(r1_0.id) from recipes r1_0
```

<v-clicks>

- The first fetches **only the rows on this page** — the database does the cutting
- The second counts every row, so the page can say "page 1 of 2"
- Page 1 and later add an `offset`: skip the rows before this page

</v-clicks>
---
layout: default
---

# What a `Page` knows

| Method | Gives you |
|---|---|
| `getContent()` | the recipes on this page — a `List<Recipe>` |
| `getNumber()` | which page this is |
| `getSize()` | the size that was asked for |
| `getTotalElements()` | how many recipes exist |
| `getTotalPages()` | how many pages that makes |
| `hasNext()` | is there another page? |
---
layout: default
---

# Sorting

```java
Sort.by("minutes")                              // ascending
Sort.by("minutes").descending()
Sort.by("cuisine").and(Sort.by("title"))        // cuisine first, then title
```

<v-clicks>

- **Always sort when you page.** Without an order, the database may hand out the same row on two pages
- Two recipes with the same `minutes`? Add `id` as the tie-breaker
- `Sort.by(property).and(Sort.by("id"))`

</v-clicks>
---
layout: default
---

# Any derived query can take one

```java
Page<Recipe> findByCuisineIgnoreCase(String cuisine, Pageable pageable);

List<Recipe> findByMinutesLessThanEqual(int minutes, Sort sort);
```

<v-click>

<div class="mt-6 text-lg">

Add a `Pageable` or a `Sort` as the **last** parameter. Spring Data adds the paging to the query it
was already writing.

</div>

</v-click>
---
layout: default
---

# In the service: two rules

```java {all|1|4|5|6-7}
private static final Set<String> SORTABLE = Set.of("title", "cuisine", "minutes", "servings");

public Page<Recipe> findPage(int page, int size, String sortBy) {
    int safeSize = Math.min(size, 50);
    String property = SORTABLE.contains(sortBy) ? sortBy : "id";
    Sort sort = Sort.by(property).and(Sort.by("id"));
    return recipeRepository.findAll(PageRequest.of(page, safeSize, sort));
}
```

<v-clicks at="1">

- The fields a client may sort by — a list **you** chose
- `?size=100000` is "send everything" again. Cap it
- Anything not on the list sorts by `id`. Nobody sorts by `internalNotes`
- The tie-breaker, then the query

</v-clicks>
---
layout: default
---

# What goes back to the client

```java
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
```

<v-clicks>

- A DTO, for the same reason `RecipeResponse` is one — **you** decide what the JSON looks like
- `Page` is Spring's class. Its JSON shape is Spring's to change
- `<T>` — it wraps a page of anything. Recipes today

</v-clicks>
---
layout: default
---

# In the controller

```java {all|3-5|6|7-8|9-10}
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
---
layout: default
---

# Why a new URL?

<div class="grid grid-cols-2 gap-8 mt-6">
<div class="p-6 rounded-lg border-2 border-gray-400">

### `GET /recipes`

```json
[ { "id": 1 }, { "id": 2 } ]
```

An array. Clients already read it this way.

</div>
<div class="p-6 rounded-lg border-2 border-green-600">

### `GET /recipes/page`

```json
{ "content": [ ], "totalPages": 2 }
```

An object, with the list inside.

</div>
</div>

<v-click>

<div class="mt-6 text-lg">

Change the first one and every client breaks — and so do your twelve tests. In a **new** API, page
the list from day one.

</div>

</v-click>
---
layout: default
---

# Spring can build the `Pageable` for you

```java
@GetMapping("/page")
public ... pageOfRecipes(Pageable pageable) {
```

<v-clicks>

- Spring reads `?page=0&size=2&sort=minutes,desc` straight into the parameter
- Less code — and the client can sort by **any** field, with **any** size
- Today you build it by hand, so that you see each piece and keep the two rules

</v-clicks>
---
layout: default
---

# Checkpoint 4 — what you do

<v-clicks>

1. One derived query in `RecipeRepository` — the quick recipes
2. `PageResponse` — a new record in `dto`
3. Two methods in the service, two endpoints in the controller
4. `mvn test` — twelve green, untouched
5. `curl` four recipes in, then ask for the quick ones, and for page 0 and page 1
6. Read the log: find the `fetch first` and the `count`

</v-clicks>
---
layout: statement
---

# Checkpoint 4

Quick recipes, and a page at a time · `recipe-api/README.md`

*Twelve green · `/recipes/quick` · `/recipes/page?size=2&sort=minutes` · two queries in the log*
---
layout: default
---

# Count the queries again

`GET /recipes/page?size=2` — in the log:

```sql
select ... from recipes r1_0 order by r1_0.id fetch first ? rows only
select count(r1_0.id) from recipes r1_0
select ... from ingredients i1_0 where i1_0.recipe_id=? order by i1_0.id
select ... from ingredients i1_0 where i1_0.recipe_id=? order by i1_0.id
```

<v-clicks>

- The page, the count — and the N+1 is still there, one per recipe **on the page**
- Paging made it smaller. It did not fix it
- Next session does

</v-clicks>
---
layout: section
---

# What you now know that you did not
---
layout: default
zoom: 0.8
---

# Before today, and now

| Before today | Now |
|---|---|
| Recipes lived in a map | Rows in real tables, written by SQL you can read |
| JPA and Hibernate sounded like rivals | A specification, and its implementation |
| A connection just existed | A pool lends them, limits them, and tells you when it runs out |
| A record held a recipe | An entity — no-arg constructor, setters, no `@Data` |
| A `List<String>` of ingredients | Two tables, one foreign key, a mirror with `mappedBy` |
| A repository was a class you wrote | An interface. Spring Data writes the object |
| Every question needed code | A method name is the query: `findByMinutesLessThanEqual…` |
| `GET /recipes` sent everything | `Pageable` in, `Page` out — the database cuts the list |
| Queries were invisible | You read them in the log — and saw your first N+1 |
---
layout: default
zoom: 0.75
---

# The words you now own

| Word | In one line |
|---|---|
| **ORM** | Translating between objects and rows |
| **JPA** | The specification — annotations and interfaces |
| **Hibernate** | The implementation — it writes the SQL |
| **Entity** | A class Hibernate stores: one object, one row |
| **Owning side** | The end of a relationship that has the foreign key |
| **Cascade** | Do to the children what you do to the parent |
| **Lazy** | Loaded when first touched |
| **Spring Data repository** | An interface; the implementation is generated |
| **Derived query** | A query written from the method's name |
| **`Pageable` / `Page`** | Which page you want, and the page you got — with the total |
| **N+1** | One query for a list, then one more per item |
---
layout: default
zoom: 0.8
---

# When it goes wrong

| You see | It means |
|---|---|
| `cannot find symbol: method id()` | Code still calling the record. It is `getId()` now |
| `String cannot be converted to Long` | An id still written `"1"`. It is `1L` |
| `No default constructor for entity` | `@NoArgsConstructor` is missing |
| `getRecipe()` is `null` in a test | Added with `getIngredients().add(...)`, not `addIngredient(...)` |
| `No property 'x' found for type 'Recipe'` | A derived query's name uses a field the entity does not have |
| `Page index must not be less than zero` | `?page=-1`. Pages start at 0 |
| H2 console is a 404 | `spring-boot-h2console` is missing |
| `GET /recipes` returns `[]` after checkpoint 3 | Correct — the seeded recipes were in the class you deleted |
---
layout: default
---

# What is still missing

<v-clicks>

- Stop the application, start it again → **the recipes you added are gone**
- H2 lives in memory. The tables are rebuilt from nothing at every start
- The three house recipes are gone too — they lived in the class you deleted
- `GET /recipes` sends one query per recipe

</v-clicks>
---
layout: statement
---

# Next session: PostgreSQL

A migration tool builds the tables and brings the house recipes back · the N+1 becomes one query

*Have PostgreSQL installed before you come*
---
layout: default
---

# Extra, for after class

<div class="mt-6 text-xl">

`recipe-api/EXTRA-tests.md` — five tests that talk to the database directly.

</div>

<v-clicks>

- `@DataJpaTest` — Hibernate and a real H2, no web at all
- Persist, flush, clear, find — proving a mapping field by field
- The first-level cache, and the N+1, pinned down in tests
- **In the lecture, two more:** the connection pool in depth — leaks, sizing across instances, the `DEBUG` log — and JPA auditing: `@CreatedDate`, `@LastModifiedDate`

</v-clicks>

<v-click>

<div class="mt-6 text-lg opacity-80">

Not needed for next session. The last sections of the lecture explain every idea in them.

</div>

</v-click>
