# Exercise — The Recipe API

Today you start a new project, and it is the last one you will start. **The Recipe API is the
project for the rest of the course** — it gets taken apart and rebuilt in layers next session, put
into a database after that, secured, containerised and shipped through a pipeline. Everything you
build today, you keep.

**Java 25 · Spring Boot 4.1.1 · Maven.**

---

## How this exercise works

**Everything is given to you** — every class, every import, and every test. Nothing is left for you
to invent. From generating the project to the last green test, this README walks you through it
one step at a time.

What *is* your job:

1. **Read each file before you paste it.** Every checkpoint says what the client wants and what
   each piece of code does. The lesson today is *why each line is there*, not how to type it.
2. **Run it.** `mvn test`, then `curl`. Every checkpoint shows the exact output you should see.
   If yours is different, stop and find out why before moving on.
3. **Do the deliberate breaks.** Three times today the README asks you to run something *before*
   it is fixed — the data leak, the missing `@Valid`, Spring's own error body. Do not skip them.
   Seeing it broken is how you recognise it at work.

When a checkpoint gives you a file you already have, **replace the whole file**. That is simpler
and safer than editing it line by line.

---

## The client, and what they asked for

Read it once, now. Every checkpoint below repeats the part it needs.

> **Sunrise Kitchen** runs a small recipe site. They want an HTTP API so their web team and their
> phone app can both read and write the same recipes.
>
> A recipe has a **title**, a **cuisine**, how many **minutes** it takes, how many **servings** it
> makes, and a list of **ingredients**. Staff also keep a private **internal note** on some recipes —
> supplier prices, who complained about it, that sort of thing. **That note is for staff only and
> must never leave the building.**
>
> The web team needs: list all recipes, filter the list by cuisine, read one recipe, add a recipe,
> replace a recipe, and delete a recipe. When something goes wrong they want to be told *which*
> field was wrong and *why*, in the same shape every time, so they can write the error handling
> once.

| The client says | Method and URL | Answers with |
|---|---|---|
| "Show all recipes" | `GET /recipes` | `200` and a list |
| "Filter by cuisine" | `GET /recipes?cuisine=khmer` | `200` and a shorter list |
| "Show one recipe" | `GET /recipes/{id}` | `200`, or `404` |
| "Add a recipe" | `POST /recipes` | `201` and a `Location` header, or `400` naming the bad fields |
| "Replace a recipe" | `PUT /recipes/{id}` | `200`, or `404` |
| "Delete a recipe" | `DELETE /recipes/{id}` | `204`, or `404` |

That is the whole business. Five checkpoints turn it into an API.

---

## Step 1 — Generate the project

Go to **start.spring.io** and fill it in exactly like this:

| Field | Value |
|---|---|
| Project | **Maven** |
| Language | **Java** |
| Spring Boot | **4.1.1** |
| Group | `com.sunrise` |
| Artifact | `recipe-api` |
| Name | `recipe-api` |
| Package name | **`com.sunrise.recipeapi`** — see the warning below |
| Packaging | **Jar** |
| Java | **25** |

Click **Add dependencies** and add **two**:

- **Spring Web** — `spring-boot-starter-webmvc`, the starter you added by hand last session.
- **Validation** — `spring-boot-starter-validation`. New today.

> ### ⚠️ The package name will be wrong
>
> Initializr fills the package name in from the artifact, so typing `recipe-api` gets you
> **`com.sunrise.recipe_api`**, with an underscore. Everything in this exercise says
> **`com.sunrise.recipeapi`**.
>
> **Type over the auto-filled value before you click Generate.** If you miss it, every test fails
> to compile and the reason is one underscore.

Click **Generate**, unzip the download, and in IntelliJ choose **File → Open** and select the
**`pom.xml`** — not the folder — then *Open as Project*. Opening the folder is the commonest
ten-minute loss in the course.

## Step 2 — Check the POM

Open `pom.xml` and confirm three things:

1. `spring-boot-starter-parent` at **4.1.1**, and no `<version>` on any starter.
2. `spring-boot-starter-webmvc` and `spring-boot-starter-validation` are both there.
3. In `test` scope: `spring-boot-starter-webmvc-test`. That is where `@WebMvcTest` lives. Last
   session you added it by hand; ticking **Spring Web** brought it this time.

## Step 3 — Make five empty packages

Right-click `src/main/java/com/sunrise/recipeapi` → **New → Package**, five times:

```
com.sunrise.recipeapi
├── RecipeApiApplication.java    <- generated, do not touch
├── domain/        the thing itself, as your application understands it
├── dto/           what goes out on the wire, in and out
├── repository/    where recipes are kept
├── service/       what the application does with recipes -- the decisions
└── controller/    HTTP: URLs, methods, status codes, error shapes
```

A request always travels the same way, and each layer only talks to the one below it:

```
HTTP request -> controller -> service -> repository -> the Map (a database, later)
```

Then do the same under `src/test/java/com/sunrise/recipeapi` for **one** package: `controller`.
Every test today goes there.

**There is no `web` package.** `dto` sits **beside** `controller`, not inside it — those records are
the contract with your client, not a detail of one class.

### Where every file goes

This is the project at the end of today. Every file the checkpoints give you has a place in this
tree — the header above each code block is its full path, so paste it there and nowhere else.

```
recipe-api/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/sunrise/recipeapi/
    │   │   ├── RecipeApiApplication.java          generated
    │   │   ├── domain/
    │   │   │   └── Recipe.java                    checkpoint 1
    │   │   ├── dto/
    │   │   │   ├── CreateRecipeRequest.java       checkpoint 3
    │   │   │   └── RecipeResponse.java            checkpoint 3
    │   │   ├── repository/
    │   │   │   ├── RecipeRepository.java          checkpoint 1   (interface)
    │   │   │   └── InMemoryRecipeRepository.java  checkpoint 1   (@Repository)
    │   │   ├── service/
    │   │   │   ├── RecipeService.java             checkpoint 1   (interface)
    │   │   │   ├── RecipeServiceImpl.java         checkpoint 1   (@Service)
    │   │   │   └── RecipeNotFoundException.java   checkpoint 5
    │   │   └── controller/
    │   │       ├── RecipeController.java          checkpoint 1   (@RestController)
    │   │       └── ApiExceptionHandler.java       checkpoint 5   (@RestControllerAdvice)
    │   └── resources/
    │       └── application.properties             checkpoint 5
    └── test/java/com/sunrise/recipeapi/controller/
        ├── RecipeControllerWebTest.java           checkpoint 1
        ├── RecipeRepresentationTest.java          checkpoint 3
        ├── ValidationWebTest.java                 checkpoint 4
        └── YourTurnTest.java                      checkpoint 5
```

### What each layer looks like

Each layer has one shape. Learn the shape now and every file below reads faster — the checkpoints
fill in the bodies.

```java
// domain — the thing itself. Plain data, no Spring.
public record Recipe(String id, String title, String cuisine, ...) { }

// repository — WHERE recipes are kept. Knows storage, nothing about HTTP.
public interface RecipeRepository {
    List<Recipe> findAll();
    Optional<Recipe> findById(String id);
    Recipe save(Recipe recipe);
    // ... findByCuisine, deleteById
}

@Repository
public class InMemoryRecipeRepository implements RecipeRepository { /* a Map */ }

// service — WHAT the application does. Talks to the repository, never to HTTP.
public interface RecipeService {
    List<Recipe> findAll(String cuisine);
    Recipe findById(String id);        // throws when missing -- from checkpoint 5
    // ... create, replace, delete
}

@Service
public class RecipeServiceImpl implements RecipeService {
    private final RecipeRepository recipeRepository;      // asks for the interface

    public RecipeServiceImpl(RecipeRepository recipeRepository) {
        this.recipeRepository = recipeRepository;
    }
}

// controller — HTTP only. Talks to the service, never to the repository.
@RestController
@RequestMapping("/recipes")
public class RecipeController {
    private final RecipeService recipeService;            // asks for the interface

    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @GetMapping("/{id}")
    public RecipeResponse one(@PathVariable String id) {
        return RecipeResponse.from(recipeService.findById(id));   // domain -> dto
    }
}
```

Three rules hold for every file you paste today:

1. **Each class asks for the layer below by its interface**, through the constructor. Nothing calls
   `new` on a repository or a service.
2. **Only the controller knows HTTP** — no `ResponseEntity`, status code or `@PathVariable` below it.
3. **Only the `dto` records go on the wire.** `Recipe` is converted in the controller, both ways.

## Step 4 — Run it once, empty

In a terminal in the project folder (**Alt+F12** in IntelliJ):

```
mvn test
```

The first run downloads Spring and takes a few minutes. It should end in **`BUILD SUCCESS`** — the
one generated test only checks that the application starts. **Delete that generated test class
now** (`src/test/java/com/sunrise/recipeapi/RecipeApiApplicationTests.java`): from here on the
tests you copy in are the ones that matter.

### The tests, and when each goes green

Fourteen tests arrive over five checkpoints, in four classes. Each checkpoint tells you which file
to copy in.

| Checkpoint | Test file you add | Green by the end of it |
|---|---|---|
| 1 | `RecipeControllerWebTest` | 1, 2, 3 |
| 2 | — | 1–6 |
| 3 | `RecipeRepresentationTest` | 1–8 |
| 4 | `ValidationWebTest` | 1–10 |
| 5 | `YourTurnTest` | **all 14** |

If a test that was green goes red, stop and read it — you changed a promise, not just some code.

**Pace.** Aim to finish checkpoint 4 in the room. Whatever does not finish in the room, finish in
your own time — there is no homework beyond this.

---

## Checkpoint 1 — a resource, and two ways to ask for it

### What the client asked for

> "The web team needs to show a list of recipes on the home page, and a single recipe on its own
> page. There is also a cuisine filter in the sidebar — Khmer, Thai, Vietnamese — and picking one
> should narrow the list."

Two things to read: **the whole collection** and **one item**. The filter narrows the collection;
it is a query parameter, not a new URL.

### The code

Six files, one layer at a time: the record is what you store, the repository is where it is kept,
the service is what the application *does* with recipes, and the controller is the HTTP front door.

**Why three layers?** Each one answers one question, and none of them answers another's:

| Layer | Answers | Knows about HTTP? | Knows where data is kept? |
|---|---|---|---|
| `controller` | Which URL, which method, which status code? | **yes** | no |
| `service` | What is the application allowed to do? | no | no |
| `repository` | Where are recipes kept, and how do I get them back? | no | **yes** |

Today the service is thin — at checkpoint 1 it only passes calls through. That is honest, and it
will not last: at checkpoint 5 it takes its first decision (a missing recipe is an *error*), and
next session it gets real business rules. The layer is here from the start so the controller never
learns to talk to the repository directly.

**`src/main/java/com/sunrise/recipeapi/domain/Recipe.java`**

```java
package com.sunrise.recipeapi.domain;

import java.util.List;

/**
 * What we store. Not what we send -- see dto/RecipeResponse.
 *
 * internalNotes is the field that must never leave the building. It is here
 * deliberately: every real entity grows one of these.
 */
public record Recipe(
        String id,
        String title,
        String cuisine,
        int minutes,
        int servings,
        List<String> ingredients,
        String internalNotes) {

    public Recipe withId(String newId) {
        return new Recipe(newId, title, cuisine, minutes, servings, ingredients, internalNotes);
    }
}
```

`internalNotes` is the staff-only note from the brief. It is here on purpose, and it matters at
checkpoint 3.

**`src/main/java/com/sunrise/recipeapi/repository/RecipeRepository.java`**

```java
package com.sunrise.recipeapi.repository;

import com.sunrise.recipeapi.domain.Recipe;

import java.util.List;
import java.util.Optional;

public interface RecipeRepository {

    List<Recipe> findAll();

    Optional<Recipe> findById(String id);

    List<Recipe> findByCuisine(String cuisine);

    Recipe save(Recipe recipe);

    boolean deleteById(String id);
}
```

**`src/main/java/com/sunrise/recipeapi/repository/InMemoryRecipeRepository.java`**

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

@Repository
public class InMemoryRecipeRepository implements RecipeRepository {

    private final Map<String, Recipe> shelf = new LinkedHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    @PostConstruct
    void seed() {
        save(new Recipe(null, "Fish Amok", "khmer", 45, 4,
                List.of("fish", "coconut milk", "kroeung"), "supplier: central market, pays late"));
        save(new Recipe(null, "Lok Lak", "khmer", 25, 2,
                List.of("beef", "lettuce", "lime", "kampot pepper"), "supplier: central market"));
        save(new Recipe(null, "Carbonara", "italian", 20, 2,
                List.of("spaghetti", "guanciale", "egg", "pecorino"), "supplier: deli, margin 12%"));
    }

    @Override
    public List<Recipe> findAll() {
        return new ArrayList<>(shelf.values());
    }

    @Override
    public Optional<Recipe> findById(String id) {
        return Optional.ofNullable(shelf.get(id));
    }

    @Override
    public List<Recipe> findByCuisine(String cuisine) {
        return shelf.values().stream()
                .filter(recipe -> recipe.cuisine().equalsIgnoreCase(cuisine))
                .toList();
    }

    @Override
    public Recipe save(Recipe recipe) {
        Recipe stored = recipe.id() == null
                ? recipe.withId(String.valueOf(nextId.getAndIncrement()))
                : recipe;
        shelf.put(stored.id(), stored);
        return stored;
    }

    @Override
    public boolean deleteById(String id) {
        return shelf.remove(id) != null;
    }
}
```

A `Map` stands in for a database until the persistence sessions. `@PostConstruct` seeds three
recipes when the application starts. **Look at the seed notes** — `"pays late"`, `"margin 12%"`.
That is the kind of sentence that ends up in a screenshot on the internet.

Now the service — an **interface** and a class that **implements** it:

**`src/main/java/com/sunrise/recipeapi/service/RecipeService.java`**

```java
package com.sunrise.recipeapi.service;

import com.sunrise.recipeapi.domain.Recipe;

import java.util.List;
import java.util.Optional;

/**
 * What the application can do with recipes -- no HTTP, no storage details.
 * The controller depends on this interface, not on the class that implements it.
 */
public interface RecipeService {

    List<Recipe> findAll(String cuisine);

    Optional<Recipe> findById(String id);
}
```

**`src/main/java/com/sunrise/recipeapi/service/RecipeServiceImpl.java`**

```java
package com.sunrise.recipeapi.service;

import com.sunrise.recipeapi.domain.Recipe;
import com.sunrise.recipeapi.repository.RecipeRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RecipeServiceImpl implements RecipeService {

    private final RecipeRepository recipeRepository;

    public RecipeServiceImpl(RecipeRepository recipeRepository) {
        this.recipeRepository = recipeRepository;
    }

    @Override
    public List<Recipe> findAll(String cuisine) {
        return cuisine == null ? recipeRepository.findAll() : recipeRepository.findByCuisine(cuisine);
    }

    @Override
    public Optional<Recipe> findById(String id) {
        return recipeRepository.findById(id);
    }
}
```

**Why an interface and an `Impl`?** The controller asks for a `RecipeService` — the interface — and
Spring hands it the one `@Service` that implements it. The controller never names
`RecipeServiceImpl`. It is the same *program to an interface* you already used for
`RecipeRepository`: the controller depends on **what** the service can do, not **which class** does
it. Many teams write every service this way; some skip the interface until there is a second
implementation. Both exist in real code — this course uses the interface.

**`src/main/java/com/sunrise/recipeapi/controller/RecipeController.java`**

```java
package com.sunrise.recipeapi.controller;

import com.sunrise.recipeapi.domain.Recipe;
import com.sunrise.recipeapi.service.RecipeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/recipes")
public class RecipeController {

    private final RecipeService recipeService;

    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    // GET /recipes  and  GET /recipes?cuisine=khmer
    @GetMapping
    public List<Recipe> all(@RequestParam(required = false) String cuisine) {
        return recipeService.findAll(cuisine);
    }

    // GET /recipes/1
    @GetMapping("/{id}")
    public Recipe one(@PathVariable String id) {
        return recipeService.findById(id).orElse(null);   // wrong on purpose -- checkpoint 2
    }
}
```

Nothing on the controller is new except the URL shapes: `@RequestMapping("/recipes")` on the class,
`@RequestParam` for the filter, `@PathVariable` for the id. Its constructor takes a
**`RecipeService`**, never the repository. **Two things in it are wrong on purpose**: returning `Recipe` leaks `internalNotes` (checkpoint 3), and `orElse(null)` answers
`200` for a recipe that does not exist (checkpoint 2).

### The tests

Copy this into `src/test/java/com/sunrise/recipeapi/controller/`:

**`src/test/java/com/sunrise/recipeapi/controller/RecipeControllerWebTest.java`**

```java
package com.sunrise.recipeapi.controller;

import com.sunrise.recipeapi.domain.Recipe;
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
@Import(RecipeServiceImpl.class)
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
```

`@WebMvcTest` loads only the web layer — controllers and advice, **not** `@Service` classes. So
`@Import(RecipeServiceImpl.class)` brings in your real service, and `@MockitoBean` replaces the
repository with a fake. Each test decides exactly what the "database" returns, and the request
goes through controller **and** service, just like in production.

### Run it

```
mvn test
```

**Expected: tests 1, 2 and 3 pass; 4, 5 and 6 fail.** 4 and 5 need endpoints you have not written;
6 needs a `404`. That is checkpoint 2.

Now start the application and ask it yourself:

```
mvn spring-boot:run
```

In a second terminal:

```
curl http://localhost:8080/recipes
curl "http://localhost:8080/recipes?cuisine=khmer"
curl http://localhost:8080/recipes/1
```

Three recipes, then two (both `khmer`), then one. **Look closely at the third response**: it has an
`internalNotes` field with `"supplier: central market, pays late"` in it. Hold that thought.

Stop the application with **Ctrl+C** before the next checkpoint.

---

## Checkpoint 2 — status codes you chose

### What the client asked for

> "The phone app needs to add a recipe and get told where it ended up, so it can jump straight to
> the new page without re-fetching the list. It needs to delete one. And when somebody follows a
> stale link to a recipe that has been deleted, the app must be able to tell that apart from a
> recipe that loaded fine — it currently cannot."

Three sentences, three status codes:

| Request | Now | After this checkpoint |
|---|---|---|
| `POST /recipes` | does not exist | **`201`** and `Location: /recipes/4` |
| `DELETE /recipes/{id}` | does not exist | **`204`**, empty body |
| `GET /recipes/999` | `200`, empty body | **`404`** |

### The code

The service learns to create and delete. Replace both service files:

**`src/main/java/com/sunrise/recipeapi/service/RecipeService.java`**

```java
package com.sunrise.recipeapi.service;

import com.sunrise.recipeapi.domain.Recipe;

import java.util.List;
import java.util.Optional;

/**
 * What the application can do with recipes -- no HTTP, no storage details.
 * The controller depends on this interface, not on the class that implements it.
 */
public interface RecipeService {

    List<Recipe> findAll(String cuisine);

    Optional<Recipe> findById(String id);

    Recipe create(Recipe recipe);

    void delete(String id);
}
```

**`src/main/java/com/sunrise/recipeapi/service/RecipeServiceImpl.java`**

```java
package com.sunrise.recipeapi.service;

import com.sunrise.recipeapi.domain.Recipe;
import com.sunrise.recipeapi.repository.RecipeRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RecipeServiceImpl implements RecipeService {

    private final RecipeRepository recipeRepository;

    public RecipeServiceImpl(RecipeRepository recipeRepository) {
        this.recipeRepository = recipeRepository;
    }

    @Override
    public List<Recipe> findAll(String cuisine) {
        return cuisine == null ? recipeRepository.findAll() : recipeRepository.findByCuisine(cuisine);
    }

    @Override
    public Optional<Recipe> findById(String id) {
        return recipeRepository.findById(id);
    }

    @Override
    public Recipe create(Recipe recipe) {
        return recipeRepository.save(recipe);
    }

    @Override
    public void delete(String id) {
        recipeRepository.deleteById(id);
    }
}
```

Then replace **`controller/RecipeController.java`**:

**`src/main/java/com/sunrise/recipeapi/controller/RecipeController.java`**

```java
package com.sunrise.recipeapi.controller;

import com.sunrise.recipeapi.domain.Recipe;
import com.sunrise.recipeapi.service.RecipeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/recipes")
public class RecipeController {

    private final RecipeService recipeService;

    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @GetMapping
    public List<Recipe> all(@RequestParam(required = false) String cuisine) {
        return recipeService.findAll(cuisine);
    }

    // 200 and the recipe, or 404
    @GetMapping("/{id}")
    public ResponseEntity<Recipe> one(@PathVariable String id) {
        return recipeService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // 201, and a Location header naming the new recipe
    @PostMapping
    public ResponseEntity<Recipe> create(@RequestBody Recipe incoming) {
        Recipe saved = recipeService.create(incoming);
        return ResponseEntity
                .created(URI.create("/recipes/" + saved.id()))
                .body(saved);
    }

    // 204, no body
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        recipeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

`ResponseEntity` lets a method say the **status** and **headers** as well as the body.
`ResponseEntity.created(uri)` sets `201` **and** the `Location` header in one call. Look at what
`one()` now costs — three lines of `Optional` ceremony to say "or 404". It is correct, and at
checkpoint 5 you delete it.

### Run it

```
mvn test
```

**Expected: tests 1–6 all pass.**

Then check it by hand — this is the part students skip. Start the app, and in the second terminal:

```
curl -i -X POST http://localhost:8080/recipes \
  -H 'Content-Type: application/json' \
  -d '{"title":"Lok Lak","cuisine":"khmer","minutes":25,"servings":2,"ingredients":["beef"]}'
```

`-i` prints the status line and headers. **Find the `201` and the `Location` with your own eyes:**

```
HTTP/1.1 201
Location: /recipes/4
Content-Type: application/json
```

Then `curl -i http://localhost:8080/recipes/999` — a `404`. Stop the app.

---

## Checkpoint 3 — what you store is not what you send

### What the client asked for

> "Two problems. First, the staff note is showing up in the app — one of our suppliers found it.
> That has to stop today. Second, the app can currently `POST` a recipe with an `id` of its
> choosing, which means two phones can fight over the same id. Ids are yours to hand out."

Both problems have one cause: **`Recipe` is doing two jobs** — what you store *and* what you
publish. Split it into three records, one job each:

| Record | Package | Job | `id` | `internalNotes` |
|---|---|---|---|---|
| `CreateRecipeRequest` | `dto` | what a client may **send** | no | no |
| `RecipeResponse` | `dto` | what a client **gets back** | yes | no |
| `Recipe` | `domain` | what **you** keep | yes | yes |

### First, prove the leak

Before you change anything, start the app and run:

```
curl http://localhost:8080/recipes/1
```

`"internalNotes":"supplier: central market, pays late"` is in the response. You shipped that at
checkpoint 1 without deciding to. **Read it on your own screen before you make it go away.**

### The code

Two new files in `dto`:

**`src/main/java/com/sunrise/recipeapi/dto/CreateRecipeRequest.java`**

```java
package com.sunrise.recipeapi.dto;

import com.sunrise.recipeapi.domain.Recipe;

import java.util.List;

/**
 * What a client may send. No id -- we assign it. No internalNotes -- none of
 * their business.
 */
public record CreateRecipeRequest(
        String title,
        String cuisine,
        int minutes,
        int servings,
        List<String> ingredients) {

    public Recipe toRecipe() {
        return new Recipe(null, title, cuisine, minutes, servings, ingredients, null);
    }
}
```

**`src/main/java/com/sunrise/recipeapi/dto/RecipeResponse.java`**

```java
package com.sunrise.recipeapi.dto;

import com.sunrise.recipeapi.domain.Recipe;

import java.util.List;

/**
 * What a client gets back. internalNotes is absent, and that absence is the
 * only reason this record exists.
 */
public record RecipeResponse(
        String id,
        String title,
        String cuisine,
        int minutes,
        int servings,
        List<String> ingredients) {

    public static RecipeResponse from(Recipe recipe) {
        return new RecipeResponse(
                recipe.id(),
                recipe.title(),
                recipe.cuisine(),
                recipe.minutes(),
                recipe.servings(),
                recipe.ingredients());
    }
}
```

And replace **`controller/RecipeController.java`** — `Recipe` now appears in **no** method
signature:

**`src/main/java/com/sunrise/recipeapi/controller/RecipeController.java`**

```java
package com.sunrise.recipeapi.controller;

import com.sunrise.recipeapi.domain.Recipe;
import com.sunrise.recipeapi.dto.CreateRecipeRequest;
import com.sunrise.recipeapi.dto.RecipeResponse;
import com.sunrise.recipeapi.service.RecipeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/recipes")
public class RecipeController {

    private final RecipeService recipeService;

    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @GetMapping
    public List<RecipeResponse> all(@RequestParam(required = false) String cuisine) {
        return recipeService.findAll(cuisine).stream().map(RecipeResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecipeResponse> one(@PathVariable String id) {
        return recipeService.findById(id)
                .map(RecipeResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<RecipeResponse> create(@RequestBody CreateRecipeRequest request) {
        Recipe saved = recipeService.create(request.toRecipe());   // DTO in  -> domain
        return ResponseEntity
                .created(URI.create("/recipes/" + saved.id()))
                .body(RecipeResponse.from(saved));                 // domain  -> DTO out
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        recipeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

The conversion happens **at the edge**, in the controller: request DTO → `Recipe` on the way in,
`Recipe` → response DTO on the way out. Inside, it is still `Recipe`.

### The tests

Copy this into the test `controller` package:

**`src/test/java/com/sunrise/recipeapi/controller/RecipeRepresentationTest.java`**

```java
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
```

### Run it

```
mvn test
```

**Expected: tests 1–8 pass.** Then start the app and `curl http://localhost:8080/recipes/1` again:

```json
{"id":"1","title":"Fish Amok","cuisine":"khmer","minutes":45,"servings":4,"ingredients":["fish","coconut milk","kroeung"]}
```

The note is gone, and there is no longer a class that could bring it back.

---

## Checkpoint 4 — say what "required" means

### What the client asked for

> "Somebody submitted the new-recipe form with everything blank and it saved. There is now a recipe
> with no title, no cuisine, zero minutes and no ingredients sitting on the home page. The API
> should have refused it."

The rules, written as Bean Validation annotations:

| Field | Rule | Annotation |
|---|---|---|
| `title`, `cuisine` | not empty, not just spaces | `@NotBlank` |
| `minutes`, `servings` | at least 1 | `@Min(1)` |
| `ingredients` | at least one, none blank | `@NotEmpty`, and `@NotBlank` on the element |

### The tests first, this time

Copy this into the test `controller` package:

**`src/test/java/com/sunrise/recipeapi/controller/ValidationWebTest.java`**

```java
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
```

### The code — in two runs, deliberately

**Run one: constraints, but no `@Valid`.** Replace **`dto/CreateRecipeRequest.java`** with this,
and leave the controller alone:

**`src/main/java/com/sunrise/recipeapi/dto/CreateRecipeRequest.java`**

```java
package com.sunrise.recipeapi.dto;

import com.sunrise.recipeapi.domain.Recipe;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * What a client may send. No id -- we assign it. No internalNotes -- none of
 * their business.
 */
public record CreateRecipeRequest(

        @NotBlank(message = "title must not be blank")
        String title,

        @NotBlank(message = "cuisine must not be blank")
        String cuisine,

        @Min(value = 1, message = "minutes must be at least 1")
        int minutes,

        @Min(value = 1, message = "servings must be at least 1")
        int servings,

        @NotEmpty(message = "a recipe needs at least one ingredient")
        List<@NotBlank(message = "an ingredient must not be blank") String> ingredients) {

    public Recipe toRecipe() {
        return new Recipe(null, title, cuisine, minutes, servings, ingredients, null);
    }
}
```

Every `message` is one a stranger could act on. Now:

```
mvn test
```

**Expected: 9 and 10 fail.** The annotations are on the record, they look right, and *nothing is
checking them*. This is the ten-minute bug everybody hits at work. **When validation "doesn't
work", check for `@Valid` first, always.**

**Run two: add `@Valid`.** Replace **`controller/RecipeController.java`** — the only changes are
`@Valid` on the `create` parameter and its import:

**`src/main/java/com/sunrise/recipeapi/controller/RecipeController.java`**

```java
package com.sunrise.recipeapi.controller;

import com.sunrise.recipeapi.domain.Recipe;
import com.sunrise.recipeapi.dto.CreateRecipeRequest;
import com.sunrise.recipeapi.dto.RecipeResponse;
import com.sunrise.recipeapi.service.RecipeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/recipes")
public class RecipeController {

    private final RecipeService recipeService;

    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @GetMapping
    public List<RecipeResponse> all(@RequestParam(required = false) String cuisine) {
        return recipeService.findAll(cuisine).stream().map(RecipeResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecipeResponse> one(@PathVariable String id) {
        return recipeService.findById(id)
                .map(RecipeResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<RecipeResponse> create(@Valid @RequestBody CreateRecipeRequest request) {
        Recipe saved = recipeService.create(request.toRecipe());   // DTO in  -> domain
        return ResponseEntity
                .created(URI.create("/recipes/" + saved.id()))
                .body(RecipeResponse.from(saved));                 // domain  -> DTO out
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        recipeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

```
mvn test
```

**Expected: tests 1–10 pass; 11 fails.** 11 wants a particular error body — checkpoint 5.

Now look at the `400` a client actually gets. Start the app:

```
curl -i -X POST http://localhost:8080/recipes \
  -H 'Content-Type: application/json' \
  -d '{"title":"","cuisine":"khmer","minutes":0,"servings":2,"ingredients":["fish"]}'
```

A `400` — good — with a generic Spring body that does not tell the client **which fields** were
wrong. The client asked for exactly that. Hold on to the irritation.

---

## Checkpoint 5 — one error shape, and three decisions

### What the client asked for

> "Every failure from your API looks different. A bad form is one shape, a missing recipe is
> another, and the next thing you add will be a third. We are writing a `switch` per endpoint. Give
> us **one** error body, always the same keys, and when it is a form problem tell us the field names
> and the messages so we can put them under the right boxes."

That shape is RFC 9457 — Problem Details — and Spring has it built in as `ProblemDetail`.

### Step 1 — Spring's own errors

Replace **`src/main/resources/application.properties`** with:

```properties
spring.application.name=recipe-api

# Spring MVC own exceptions answer as RFC 9457 Problem Details.
# Exceptions you wrote need the @RestControllerAdvice.
spring.mvc.problemdetails.enabled=true
```

Restart the app and send the blank title again. The body is now Problem Details — the standard
keys — but still Spring's generic version:

```json
{"detail":"Invalid request content.","instance":"/recipes","status":400,"title":"Bad Request"}
```

Standard shape, no field names. The advice fixes that.

### Step 2 — the exception and the advice

The exception goes in **`service`** — "there is no such recipe" is a business fact. The advice goes
in **`controller`** — turning that fact into a `404` is HTTP's business.

**`src/main/java/com/sunrise/recipeapi/service/RecipeNotFoundException.java`**

```java
package com.sunrise.recipeapi.service;

/**
 * "There is no recipe with this id" -- a business fact, so it lives in the
 * service package. Turning it into a 404 is HTTP's business: that happens in
 * the controller package, in ApiExceptionHandler.
 */
public class RecipeNotFoundException extends RuntimeException {

    public RecipeNotFoundException(String id) {
        super("No recipe with id " + id);
    }
}
```

**`src/main/java/com/sunrise/recipeapi/controller/ApiExceptionHandler.java`**

```java
package com.sunrise.recipeapi.controller;

import com.sunrise.recipeapi.service.RecipeNotFoundException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One error shape for the whole API.
 *
 * Deliberately does NOT extend ResponseEntityExceptionHandler. That base class
 * maps MethodArgumentNotValidException in a final method, so extending it means
 * overriding a protected hook instead of writing @ExceptionHandler -- correct on
 * a large API, one concept too many for a first REST session.
 *
 * @Order is required, not decoration. spring.mvc.problemdetails.enabled=true
 * registers Boot's own advice, which ALSO handles MethodArgumentNotValidException.
 * Without @Order, Boot's runs first and the client gets its generic "Bad Request"
 * body with no errors map. Observed 25 Sept 2026, Boot 4.1.1.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {

    private static final String PROBLEM_BASE = "https://docs.sunrise.dev/errors/";

    @ExceptionHandler(RecipeNotFoundException.class)
    ProblemDetail handleNotFound(RecipeNotFoundException exception) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setType(URI.create(PROBLEM_BASE + "recipe-not-found"));
        problem.setTitle("Recipe not found");
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (var fieldError : exception.getFieldErrors()) {
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "The request body has " + errors.size() + " invalid field(s)");
        problem.setType(URI.create(PROBLEM_BASE + "validation-failed"));
        problem.setTitle("Validation failed");
        problem.setProperty("errors", errors);
        return problem;
    }
}
```

**Read the `@Order` comment.** The property from step 1 switched on Boot's own advice, and it
handles validation errors too. Without `@Order(Ordered.HIGHEST_PRECEDENCE)`, Boot's advice answers
first and your `errors` map never appears. Want to see it? Remove the `@Order` line and run test 11,
then put it back.

`type` names the *kind* of error. It is not an endpoint of your API; in a real API it links to a
page documenting the error. `docs.sunrise.dev` is an example domain and leads nowhere.

### Step 3 — the service takes its first decisions

Replace both service files with the finished versions:

**`src/main/java/com/sunrise/recipeapi/service/RecipeService.java`**

```java
package com.sunrise.recipeapi.service;

import com.sunrise.recipeapi.domain.Recipe;

import java.util.List;

/**
 * What the application can do with recipes -- no HTTP, no storage details.
 *
 * The controller depends on this interface, not on RecipeServiceImpl. That is
 * the same "program to an interface" you met with RecipeRepository.
 */
public interface RecipeService {

    List<Recipe> findAll(String cuisine);

    Recipe findById(String id);

    Recipe create(Recipe recipe);

    Recipe replace(String id, Recipe recipe);

    void delete(String id);
}
```

**`src/main/java/com/sunrise/recipeapi/service/RecipeServiceImpl.java`**

```java
package com.sunrise.recipeapi.service;

import com.sunrise.recipeapi.domain.Recipe;
import com.sunrise.recipeapi.repository.RecipeRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * The layer in the middle. Today it makes one kind of decision: asking for a
 * recipe that is not there is an ERROR, not an empty Optional. Next session it
 * gets real business rules -- duplicate titles, an ingredient limit.
 *
 * Nothing here mentions HTTP. Nothing here knows the recipes live in a Map.
 */
@Service
public class RecipeServiceImpl implements RecipeService {

    private final RecipeRepository recipeRepository;

    public RecipeServiceImpl(RecipeRepository recipeRepository) {
        this.recipeRepository = recipeRepository;
    }

    @Override
    public List<Recipe> findAll(String cuisine) {
        return cuisine == null ? recipeRepository.findAll() : recipeRepository.findByCuisine(cuisine);
    }

    @Override
    public Recipe findById(String id) {
        return recipeRepository.findById(id)
                .orElseThrow(() -> new RecipeNotFoundException(id));
    }

    @Override
    public Recipe create(Recipe recipe) {
        return recipeRepository.save(recipe);
    }

    /** Test 14's decision: you cannot replace what is not there, so findById throws first. */
    @Override
    public Recipe replace(String id, Recipe recipe) {
        findById(id);
        return recipeRepository.save(recipe.withId(id));
    }

    /** Test 13's decision: deleting something already gone is a 404, not a silent 204. */
    @Override
    public void delete(String id) {
        if (!recipeRepository.deleteById(id)) {
            throw new RecipeNotFoundException(id);
        }
    }
}
```

`findById` no longer returns an `Optional`: asking for a recipe that is not there is now an
**error**, decided once, in the service. `delete` and the new `replace` make the same decision.
None of it mentions HTTP.

Then replace **`controller/RecipeController.java`** with the finished version:

**`src/main/java/com/sunrise/recipeapi/controller/RecipeController.java`**

```java
package com.sunrise.recipeapi.controller;

import com.sunrise.recipeapi.domain.Recipe;
import com.sunrise.recipeapi.dto.CreateRecipeRequest;
import com.sunrise.recipeapi.dto.RecipeResponse;
import com.sunrise.recipeapi.service.RecipeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * HTTP only: URLs, methods, status codes, and converting between the wire
 * format (DTOs) and the domain (Recipe). Every decision about recipes is the
 * service's.
 */
@RestController
@RequestMapping("/recipes")
public class RecipeController {

    private final RecipeService recipeService;

    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @GetMapping
    public List<RecipeResponse> all(@RequestParam(required = false) String cuisine) {
        return recipeService.findAll(cuisine).stream().map(RecipeResponse::from).toList();
    }

    /** One line. A missing recipe throws in the service; the advice makes it a 404. */
    @GetMapping("/{id}")
    public RecipeResponse one(@PathVariable String id) {
        return RecipeResponse.from(recipeService.findById(id));
    }

    /** 201, and a Location header naming what was just made. */
    @PostMapping
    public ResponseEntity<RecipeResponse> create(@Valid @RequestBody CreateRecipeRequest request) {
        Recipe saved = recipeService.create(request.toRecipe());
        return ResponseEntity
                .created(URI.create("/recipes/" + saved.id()))
                .body(RecipeResponse.from(saved));
    }

    /**
     * Test 14. This solution chose 404 for an id nobody has -- the decision is in
     * RecipeServiceImpl.replace. The other defensible choice is to create it and
     * return 201, since the client named the URL. Either way PUT stays idempotent.
     */
    @PutMapping("/{id}")
    public RecipeResponse replace(@PathVariable String id,
                                  @Valid @RequestBody CreateRecipeRequest request) {
        return RecipeResponse.from(recipeService.replace(id, request.toRecipe()));
    }

    /**
     * Test 13. A second DELETE is a 404 (decided in RecipeServiceImpl.delete). DELETE
     * is still idempotent: the promise is about server state, and after one call or
     * five the recipe is gone.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        recipeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

Look at what the controller lost:

- **`one()` is one line.** No `ResponseEntity`, no `Optional` dance. The service throws; the advice
  turns that into a `404`. Test 6 stays green: the client sees the same response — the decision just
  lives somewhere better.
- **`delete()` says `404` for something already gone** (test 13) — decided in the service.
- **`replace()` is new** — `PUT /recipes/{id}`, `404` if there is nothing to replace (test 14) —
  decided in the service.

Every line left in the controller is about HTTP. That is the goal.

### The tests — three decisions, written down

Copy this into the test `controller` package:

**`src/test/java/com/sunrise/recipeapi/controller/YourTurnTest.java`**

```java
package com.sunrise.recipeapi.controller;

import com.sunrise.recipeapi.domain.Recipe;
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

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Checkpoint 5. Three tests, and each one is a DECISION about what the API
 * should do, written down as an assertion.
 *
 * The code is given to you. Read each comment before you run it: the comment
 * says what was decided and what the other choice would have cost.
 */
@WebMvcTest(RecipeController.class)
@Import(RecipeServiceImpl.class)
@DisplayName("Three decisions")
class YourTurnTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecipeRepository recipeRepository;

    /**
     * 12. Last session's question, answered. A body with no `cuisine` key at all
     * -- not blank, absent. Last session the same shape returned 200. What changed
     * the answer is @NotBlank on the record plus @Valid on the parameter.
     */
    @Test
    @DisplayName("12. POST /recipes with cuisine missing from the body")
    void missingCuisineIsRejected() throws Exception {
        String body = """
                {
                  "title": "Fish Amok",
                  "minutes": 45,
                  "servings": 4,
                  "ingredients": ["fish"]
                }
                """;

        mockMvc.perform(post("/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.cuisine").value("cuisine must not be blank"));
    }

    /**
     * 13. DELETE the same recipe twice. The first call deletes it; the second
     * finds nothing.
     *
     * Decided: the second call is 404, so a client with a typo'd id is not told
     * it succeeded. The other defensible answer is 204 every time.
     *
     * Either way DELETE is still idempotent: idempotency is a promise about
     * SERVER STATE, not about the response. After one call or two, the recipe
     * is gone.
     */
    @Test
    @DisplayName("13. DELETE the same recipe twice")
    void deletingTwice() throws Exception {
        when(recipeRepository.deleteById("1")).thenReturn(true, false);

        mockMvc.perform(delete("/recipes/1"))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/recipes/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recipe not found"));
    }

    /**
     * 14. PUT /recipes/{id} for an id nobody has.
     *
     * Decided: 404 -- you cannot replace what is not there. The other defensible
     * answer is 201, because PUT means "make the resource at this URL look like
     * this" and the client chose the URL.
     *
     * The verify() line is the part that proves the decision: nothing was saved.
     */
    @Test
    @DisplayName("14. PUT /recipes/{id} for an id nobody has")
    void puttingToAnIdThatDoesNotExist() throws Exception {
        when(recipeRepository.findById("99")).thenReturn(Optional.empty());

        String body = """
                {
                  "title": "Fish Amok",
                  "cuisine": "khmer",
                  "minutes": 45,
                  "servings": 4,
                  "ingredients": ["fish"]
                }
                """;

        mockMvc.perform(put("/recipes/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());

        verify(recipeRepository, never()).save(any(Recipe.class));
    }
}
```

Each of these is a **decision** before it is a test. Read the comments: they say what was decided
and what the other choice would have cost.

- **12** is last session's question answered. The same request returned `200` a week ago. What
  changed it is `@NotBlank` plus `@Valid`.
- **13** — a second `DELETE` returns `404`. **Is `DELETE` still idempotent?** Yes: idempotency is a
  promise about **server state**, not the response. After one call or two, the recipe is gone.
- **14** — `PUT` to an id nobody has returns `404`. The other defensible answer is `201`: the client
  chose the URL, so create it. Whichever you pick, `PUT` must stay idempotent.

### Run it

```
mvn test
```

**Expected: all 14 pass.**

```
Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Then start the app and see every error in one shape:

```
curl -i -X POST http://localhost:8080/recipes \
  -H 'Content-Type: application/json' \
  -d '{"title":"","cuisine":"khmer","minutes":0,"servings":2,"ingredients":["fish"]}'
```

```
HTTP/1.1 400
Content-Type: application/problem+json

{"detail":"The request body has 2 invalid field(s)","instance":"/recipes","status":400,
 "title":"Validation failed","type":"https://docs.sunrise.dev/errors/validation-failed",
 "errors":{"minutes":"minutes must be at least 1","title":"title must not be blank"}}
```

```
curl -i http://localhost:8080/recipes/999
```

```
HTTP/1.1 404
Content-Type: application/problem+json

{"detail":"No recipe with id 999","instance":"/recipes/999","status":404,
 "title":"Recipe not found","type":"https://docs.sunrise.dev/errors/recipe-not-found"}
```

Same keys, both times. And the `DELETE` decision, live — the first call deletes recipe 1, the
second finds nothing:

```
curl -i -X DELETE http://localhost:8080/recipes/1     # HTTP/1.1 204
curl -i -X DELETE http://localhost:8080/recipes/1     # HTTP/1.1 404, and the same error shape
```

**All fourteen green is done.** You now have the project you will carry to Session 13:

```
com.sunrise.recipeapi
├── RecipeApiApplication.java
├── domain/        Recipe
├── dto/           CreateRecipeRequest, RecipeResponse
├── repository/    RecipeRepository, InMemoryRecipeRepository
├── service/       RecipeService, RecipeServiceImpl, RecipeNotFoundException
└── controller/    RecipeController, ApiExceptionHandler
```

---

## If you get stuck

**Build errors — the project is wrong, not your code:**

| What you see | What it means |
|---|---|
| `package com.sunrise.recipeapi does not exist` | Initializr's underscore. Your package is `recipe_api`. Rename it. |
| `cannot find symbol: class WebMvcTest` | `spring-boot-starter-webmvc-test` is missing from the POM in `test` scope. |
| `package jakarta.validation.constraints does not exist` | The Validation starter was not ticked. Add it. |
| `records are not supported in -source 8` | The POM's Java version, not your code. It should say 25. |
| `Could not resolve dependencies` | No network, or a typo in an artifactId. |
| `package com.sunrise.recipeapi.controller does not exist` | A class is in the wrong package. Check the `package` line at the top of the file, not the folder in your IDE. |

**Code errors — the project is fine:**

| What you see | What it means |
|---|---|
| A `400` you did not expect, on a body that looks right | A field name in your JSON does not match a record component. Jackson matched nothing and the constraint fired. |
| A `200` or `201` where you wanted a `400` | `@Valid` is missing from the controller parameter. Check this first, always. |
| Test 11 fails with `expected:<Validation failed> but was:<Bad Request>` | `@Order(Ordered.HIGHEST_PRECEDENCE)` is missing from `ApiExceptionHandler`. Boot's own advice answered first. |
| `internalNotes` in the response | Something still returns `Recipe` instead of `RecipeResponse`. |
| `Location` header absent | You used `ResponseEntity.status(201).body(...)` instead of `.created(uri)`. |
| `404` on every endpoint | Your controller is outside `com.sunrise.recipeapi`, so scanning never found it. |
| `415 Unsupported Media Type` | The request has no `Content-Type: application/json`. |
| `Web server failed to start. Port 8080 was already in use.` | Something else is on 8080 — often your app from the last checkpoint, still running. Stop it with **Ctrl+C**, or run `mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081` and `curl` port 8081. |
| Tests fail with `No qualifying bean of type 'com.sunrise.recipeapi.service.RecipeService'` | The test class is missing `@Import(RecipeServiceImpl.class)` — the slice does not load `@Service` classes. Or `RecipeServiceImpl` is missing `@Service`. |
| Every test fails with `APPLICATION FAILED TO START` | A `@Bean` on your application class needs something the slice does not load. Move it to its own `@Configuration`. |

**Stuck for more than ten minutes: ask.** The code is all in front of you, so if something is not
working it is a wiring problem, and wiring problems are faster to solve out loud.
