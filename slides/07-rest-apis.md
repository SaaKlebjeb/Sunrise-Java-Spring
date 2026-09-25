---
layout: section
---

# REST APIs

Design, validation, and the shape of an error

---
layout: default
---

# Learning Objectives

<v-clicks>

- Say what REST **constrains**, and why "JSON over HTTP" is not the same claim
- Design resource URLs that name things rather than describe actions
- Say which methods are **safe**, which are **idempotent**, and what a client does with that
- Choose a status code on purpose, and return it with `ResponseEntity`
- Explain why the object you store is not the object you send
- Validate a request body with `@Valid` and Bean Validation
- Give every error in your API **one shape**, with `ProblemDetail`

</v-clicks>

---
layout: default
---

# Who you are building for today

<div class="mt-4 border-2 border-blue-500/50 rounded-lg px-6 py-3 bg-blue-500/10">

**Sunrise Kitchen** runs a small recipe site. Their web team and their phone app need **one API**
for the same recipes.

A recipe has a **title**, a **cuisine**, **minutes**, **servings** and **ingredients**. Staff keep
a private **internal note** — and **that note must never reach a client.**

List, filter by cuisine, read one, add, replace, delete. When something fails: **the same error
shape every time**, naming the field and the reason.

</div>

<v-click>

<div class="mt-6 text-center text-xl">

The requirements are handed to you. **The work today is technical.**

</div>

</v-click>

---
layout: default
---

# What we are building

| The client says | Method and URL | Answers with |
|---|---|---|
| "Show all recipes" | `GET /recipes` | `200` + a list |
| "Filter by cuisine" | `GET /recipes?cuisine=khmer` | `200` + a shorter list |
| "Show one recipe" | `GET /recipes/{id}` | `200`, or **`404`** |
| "Add a recipe" | `POST /recipes` | **`201`** + `Location`, or **`400`** naming the fields |
| "Replace a recipe" | `PUT /recipes/{id}` | `200`, or `404` — test 14 |
| "Delete a recipe" | `DELETE /recipes/{id}` | **`204`**, no body |

<v-click>

<div class="mt-6">

Five packages — `domain` · `dto` · `repository` · `service` · `controller`. No database yet: recipes
live in a `Map`. Every request goes **controller → service → repository**.

</div>

</v-click>

---
layout: default
zoom: 0.85
---

# Where you will be at the end

<div class="grid grid-cols-2 gap-6 mt-4">

<div>

```java
@RestController
@RequestMapping("/recipes")
public class RecipeController {

  @GetMapping
  public List<RecipeResponse> all(
      @RequestParam(required = false) String cuisine)

  @GetMapping("/{id}")
  public RecipeResponse one(@PathVariable String id)
                                  // throws -> 404
  @PostMapping
  public ResponseEntity<RecipeResponse> create(
      @Valid @RequestBody CreateRecipeRequest request)
                                  // 201 + Location
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(
      @PathVariable String id)    // 204
}
```

</div>

<div>

```json
{
  "type": "https://docs.sunrise.dev/errors/validation-failed",
  "title": "Validation failed",
  "status": 400,
  "detail": "The request body has 2 invalid field(s)",
  "instance": "/recipes",
  "errors": {
    "title": "title must not be blank",
    "minutes": "minutes must be at least 1"
  }
}
```

</div>

</div>

<v-click>

<div class="mt-4 text-center text-xl">

**The goal:** this controller, this error body, **fourteen tests green**.

</div>

</v-click>

---
layout: section
---

# The question we refused to answer

---
layout: default
---

# Last session's eighth test

You wrote this one yourself.

```json
POST /loans

{"isbn": "978-0132350884"}
```

<v-click>

No `member`. You had to decide what **should** happen, then look at what did.

</v-click>

---
layout: default
---

# What the room usually asserts

<div class="grid grid-cols-2 gap-8 mt-10">

<div>

## You said

<div class="text-3xl font-bold text-green-500 mt-4">400</div>

Bad Request

</div>

<div>

<v-click>

## You got

<div class="text-3xl font-bold text-red-500 mt-4">200</div>

OK — and a loan for `null`

</v-click>

</div>

</div>

---
layout: statement
---

# Nothing was broken.

Nobody had been told the field was required.

---
layout: default
---

# Four gaps, not one

<v-clicks>

1. **Nothing said the field was required** → Bean Validation
2. **The response said `200` when nothing sensible happened** → status codes on purpose
3. **Your internal record was also your wire format** → request and response DTOs
4. **When it does fail, the failure has no agreed shape** → `ProblemDetail`

</v-clicks>

<v-click>

<div class="mt-10 text-xl text-center">

Four gaps. One session. That is the whole plan.

</div>

</v-click>

---
layout: default
---

# Write this down before we start

<div class="mt-8 border-2 border-green-500/50 rounded-lg px-6 py-4 bg-green-500/10">

What status code **should** a `POST` with a missing required field return?

And what should be in the body?

</div>

<v-click>

<div class="mt-8">

One line. You will check that guess **twice** today.

</div>

</v-click>

---
layout: section
---

# REST is a set of constraints

Accept them, and you get properties you did not have to build

---
layout: default
---

# Ask ten developers

<v-click>

<div class="text-2xl mt-10 text-center opacity-70">

"REST? It's JSON over HTTP."

</div>

</v-click>

<v-click>

<div class="mt-10 text-center">

That is a description of the **traffic**.

Not a definition of the **architecture**.

</div>

</v-click>

---
layout: default
---

# Client–server, and a uniform interface

<div class="grid grid-cols-2 gap-8 mt-6">

<div>

## Client–server

The client holds the interface.

The server holds the data and the rules.

<v-click>

**You can rewrite the Android app without touching the server.**

</v-click>

</div>

<div>

## Uniform interface

The same small set of methods, applied to everything. No new vocabulary per resource.

<v-click>

`GET` means the same against `/recipes` as against any API in the world.

</v-click>

</div>

</div>

---
layout: default
---

# Statelessness

<v-clicks>

- Every request carries everything needed to understand it
- The server keeps nothing about "this client" between requests
- **This is what lets ten copies sit behind a load balancer sharing no memory**

</v-clicks>

<v-click>

<div class="mt-8 opacity-70">

It is also why the security session later has something to say about where a logged-in
identity actually lives.

</div>

</v-click>

---
layout: default
---

# Cacheability, and layers

<v-clicks>

- **Cacheability** — a response can say whether it may be stored and reused
- It only works because of the method contracts, in a moment
- **Layered system** — a client cannot tell your app from a proxy in front of it
- That is what makes gateways, CDNs and load balancers possible at all

</v-clicks>

---
layout: default
---

# What REST is not

<v-clicks>

- It does not require JSON
- It does not require Spring
- It does not forbid you from being pragmatic

</v-clicks>

<v-click>

<div class="mt-8 text-xl">

What it gives you is **expectations your clients already have.**

</div>

</v-click>

<v-click>

<div class="mt-6 text-xl">

Break one, and somebody downstream writes a special case that never goes away.

</div>

</v-click>

---
layout: default
---

# Not JSON — one method, two bodies

```java
@GetMapping(value = "/recipes/{id}", produces = {
        MediaType.APPLICATION_JSON_VALUE,
        MediaType.APPLICATION_XML_VALUE
})
public RecipeResponse getRecipe(@PathVariable Long id) {
    return recipeService.findById(id);
}
```

<v-click>

```
curl -H "Accept: application/json" .../recipes/1   → {"id":1,"title":"Amok",...}
curl -H "Accept: application/xml"  .../recipes/1   → <RecipeResponse>...</RecipeResponse>
```

</v-click>

<v-click>

<div class="mt-6 text-xl">

The `Accept` header picks the body. Same URL, same rules — no `/getRecipeAsXml`.

</div>

</v-click>

---
layout: default
---

# Not Spring — same endpoint, no annotations

```javascript
app.get('/recipes/:id', (req, res) => {
    const recipe = recipeService.findById(req.params.id);
    res.status(200).json(recipe);
});

app.post('/recipes', (req, res) => {
    const saved = recipeService.save(req.body);
    res.status(201).location(`/recipes/${saved.id}`).json(saved);
});
```

<v-click>

<div class="mt-6 text-xl">

Node, Express. Same URL, same verb, same `201` + `Location` on create.

**Spring's annotations are a shorter way to say this — not a different rule.**

</div>

</v-click>

---
layout: default
---

# Pragmatic beats pure

```
GET /recipes/search?q=khmer&maxMinutes=30
```

<v-click>

<div class="mt-4 text-xl">

A purist says this belongs in the query string on `/recipes` instead. Most real APIs use
`/search` anyway — it reads clearly, and it is what developers expect.

</div>

</v-click>

<v-click>

<div class="mt-6 text-xl">

**Nobody follows every rule without exception. What matters is the exception was on purpose.**

</div>

</v-click>

---
layout: default
---

# Pause

<div class="mt-8 border-2 border-amber-500/50 rounded-lg px-6 py-4 bg-amber-500/10">

Statelessness says the server keeps nothing between requests.

Your `lending-desk` kept books in a `@Repository` holding a `List`.

**Is that a violation?**

</div>

<v-click>

<div class="mt-8">

No. Statelessness is about **client session state** — who is logged in, what page they are on,
step 3 of their wizard.

Application state every client sees the same way is exactly what a server is for.

</div>

</v-click>

---
layout: section
---

# Resources, and the URLs that name them

---
layout: statement
---

# A resource is a thing.

Its URL is that thing's name.

---
layout: default
---

# Names are nouns

```
GET    /recipes              the collection
POST   /recipes              add one to the collection
GET    /recipes/42           one item
PUT    /recipes/42           replace that item
DELETE /recipes/42           remove that item
```

<v-click>

Five operations. One noun. Nothing to memorise.

</v-click>

---
layout: default
---

# The way it gets written first

```
GET  /getAllRecipes
POST /createRecipe
POST /updateRecipe?id=42
POST /deleteRecipe?id=42
```

<v-click>

Both work. Both return JSON.

</v-click>

<v-click>

<div class="mt-6 text-xl">

Only one of them is an interface anybody can **predict**.

</div>

</v-click>

---
layout: default
---

# What the second one costs you

<v-clicks>

- Every operation is a `POST` → **nothing is cacheable**
- Every operation is a `POST` → **nothing is safely retryable**
- The verb is in the path → a new operation means a new URL to document
- `/getAllRecipes` tells you nothing about `/getRecipeIngredients`

</v-clicks>

<v-click>

<div class="mt-8">

There is no system to learn. Only a list to memorise.

</div>

</v-click>

---
layout: default
---

# The shape of a path

<v-clicks>

- **Collections are plural** — `/recipes`, not `/recipe`. You are naming a set.
- **Identity goes in the path** — `/recipes/42`. The 42 is part of the name.
- **Filtering goes in the query string** — `/recipes?cuisine=khmer`. Still the collection.
- **Nesting shows containment** — and one level of it.

</v-clicks>

---
layout: default
---

# Path or query?

<div class="grid grid-cols-2 gap-8 mt-8">

<div>

## Path

`/recipes/42`

Returns **one** thing

The value is **identity**

</div>

<div>

## Query

`/recipes?cuisine=khmer`

Returns **a list**

The value is **a filter**

</div>

</div>

<v-click>

<div class="mt-10 text-center text-xl">

If it returns one item, it was identity. Put it in the path.

</div>

</v-click>

---
layout: default
---

# Nesting, and where it goes wrong

<div class="mt-8">

```
/recipes/42/ingredients
```

Fine. One level. Containment.

</div>

<v-click>

<div class="mt-8">

```
/users/7/recipes/42/ingredients/3/substitutions
```

A database schema that escaped onto the internet.

</div>

</v-click>

---
layout: default
---

# Conventions your clients already have

<v-clicks>

- No file extensions — `/recipes`, not `/recipes.json`
- No trailing slashes
- Lowercase
- Hyphens, not underscores — `/recipe-books`, not `/recipe_books`

</v-clicks>

<v-click>

<div class="mt-8 opacity-70">

Not laws. Conventions. But conventions nobody has to be told.

</div>

</v-click>

---
layout: default
---

# Not everything is a noun

<div class="text-xl mt-4">

**What is the URL for "publish this recipe"?** Three defensible answers.

</div>

<v-clicks>

- `PUT /recipes/42/published` with `true` — you made the state a resource
- `POST /recipes/42/publish` — you admitted it is an action. Widely done. Not a sin.
- `PATCH /recipes/42` with `{"status": "PUBLISHED"}` — you changed a field

</v-clicks>

<v-click>

<div class="mt-10 border-2 border-green-500/50 rounded-lg px-6 py-4 bg-green-500/10">

Pick one. **Be consistent.**

A consistent imperfect convention beats one that is perfect in three places and improvised in six.

</div>

</v-click>

---
layout: default
---

# Pause

<div class="mt-8 border-2 border-amber-500/50 rounded-lg px-6 py-4 bg-amber-500/10">

Design the URLs for:

1. All recipes by one author
2. One recipe's ingredients
3. Searching recipes that take under 30 minutes

</div>

<v-click>

<div class="mt-6">

Then check each one: **is this a noun, and is identity in the right place?**

</div>

</v-click>

---
layout: section
---

# The promises a method makes

---
layout: default
---

# Two words carry most of it

<v-clicks>

**Safe** — does not change anything on the server.
A client, a browser, a crawler or a proxy may call it whenever it likes.

**Idempotent** — once or five times leaves the server in **the same state**.
The *responses* may differ. The *state* may not.

</v-clicks>

---
layout: default
---

# The table

| Method | Safe | Idempotent | Means |
|---|---|---|---|
| `GET` | ✅ | ✅ | Read. Never changes anything. |
| `HEAD` | ✅ | ✅ | A `GET`'s headers, no body. |
| `POST` | ❌ | ❌ | Create. Twice creates two. |
| `PUT` | ❌ | ✅ | Replace at a known URL. |
| `PATCH` | ❌ | ❌ | Partial update. |
| `DELETE` | ❌ | ✅ | Remove. Twice leaves it gone. |

---
layout: default
---

# Why anybody cares

<v-clicks>

- You click **"Save"** on a slow website
- Nothing happens for a few seconds
- **You click it again, just in case**

</v-clicks>

<div class="grid grid-cols-2 gap-8 mt-8">

<div>

<v-click>

## If it was `PUT`

The second click writes the same recipe over itself.

**Nothing bad happens.**

</v-click>

</div>

<div>

<v-click>

## If it was `POST`

The second click creates a **second** recipe.

**A duplicate someone has to find and delete.**

</v-click>

</div>

</div>

---
layout: statement
---

# Idempotency is what makes a retry safe.

Every network client retries.

---
layout: default
---

# The one that catches everybody

```
DELETE /recipes/42     →  204 No Content
DELETE /recipes/42     →  404 Not Found
```

<v-click>

Different responses. **Is `DELETE` still idempotent?**

</v-click>

<v-click>

<div class="mt-8 border-2 border-green-500/50 rounded-lg px-6 py-4 bg-green-500/10">

**Yes.** Idempotency is a claim about **server state**, not about the response.

After one call it is gone. After five calls it is gone.

</div>

</v-click>

---
layout: default
---

# Pause

<div class="mt-8 border-2 border-amber-500/50 rounded-lg px-6 py-4 bg-amber-500/10">

Your API needs *"add one to the number of times this recipe was cooked"*.

**Which method?**

Is your answer safe? Idempotent?

What happens when a flaky client retries it three times?

</div>

---
layout: default
---

# Three layers, one direction

<div class="flex items-stretch justify-center gap-2 mt-10 text-center text-sm">

<div class="flex flex-col justify-center px-3 opacity-70">HTTP<br>request</div>
<div class="flex items-center text-2xl opacity-60">→</div>

<div class="w-44 border-2 border-green-500/60 rounded-lg px-3 py-4 bg-green-500/10">
<div class="font-bold text-lg">controller</div>
<div class="mt-2">Which URL? Which status?</div>
<div class="mt-3 text-xs opacity-70">knows HTTP</div>
</div>

<div class="flex items-center text-2xl opacity-60">→</div>

<div class="w-44 border-2 border-blue-500/60 rounded-lg px-3 py-4 bg-blue-500/10">
<div class="font-bold text-lg">service</div>
<div class="mt-2">What may the app do?</div>
<div class="mt-3 text-xs opacity-70">knows neither</div>
</div>

<div class="flex items-center text-2xl opacity-60">→</div>

<div class="w-44 border-2 border-amber-500/60 rounded-lg px-3 py-4 bg-amber-500/10">
<div class="font-bold text-lg">repository</div>
<div class="mt-2">Where are recipes kept?</div>
<div class="mt-3 text-xs opacity-70">knows storage</div>
</div>

<div class="flex items-center text-2xl opacity-60">→</div>
<div class="flex flex-col justify-center px-3 opacity-70">the Map<br><span class="text-xs">(a database, later)</span></div>

</div>

<v-clicks>

- **No layer skips one.** The controller never calls the repository — even when it would be shorter.
- Swap HTTP for a CLI: the controller is rewritten, the service is not. Swap the `Map` for
  PostgreSQL: the repository is rewritten, the service is not.

</v-clicks>

---
layout: default
zoom: 0.9
---

# An interface and an `Impl`

```java
public interface RecipeService {                         // WHAT the app can do
    List<Recipe> findAll(String cuisine);
    Optional<Recipe> findById(String id);
}

@Service
public class RecipeServiceImpl implements RecipeService {    // HOW it does it
    private final RecipeRepository recipeRepository;

    public RecipeServiceImpl(RecipeRepository recipeRepository) {
        this.recipeRepository = recipeRepository;
    }

    public List<Recipe> findAll(String cuisine) {
        return cuisine == null
                ? recipeRepository.findAll()
                : recipeRepository.findByCuisine(cuisine);
    }
    public Optional<Recipe> findById(String id) { return recipeRepository.findById(id); }
}
```

<v-clicks>

- The controller asks for **`RecipeService`** and never names the `Impl` — program to an interface
- **Thin today, on purpose.** Checkpoint 5: its first decision. Next session: real business rules.

</v-clicks>

---
layout: default
zoom: 0.85
---

# Where every file goes

<div class="grid grid-cols-2 gap-6 mt-2">

<div>

```
com.sunrise.recipeapi
├── RecipeApiApplication      generated
├── domain/
│   └── Recipe                        CP1
├── dto/
│   ├── CreateRecipeRequest           CP3
│   └── RecipeResponse                CP3
├── repository/
│   ├── RecipeRepository              CP1
│   └── InMemoryRecipeRepository      CP1
├── service/
│   ├── RecipeService                 CP1
│   ├── RecipeServiceImpl             CP1
│   └── RecipeNotFoundException       CP5
└── controller/
    ├── RecipeController              CP1
    └── ApiExceptionHandler           CP5
```

</div>

<div>

<v-clicks>

- **Every file has one home.** The README header above each code block is its full path.
- Each class asks for the layer below **by its interface**, in the constructor
- **Only `controller` knows HTTP** — no `ResponseEntity` below it
- **Only `dto` goes on the wire** — `Recipe` is converted in the controller
- All four test classes live in the **test** `controller` package

</v-clicks>

</div>

</div>

---
layout: section
---

# Checkpoint 1

Generate the project. One resource, two ways to ask for it.

---
layout: default
---

# Checkpoint 1 — the brief

<div class="mt-4 border-2 border-blue-500/50 rounded-lg px-6 py-3 bg-blue-500/10">

"The web team needs a list of recipes on the home page, and a single recipe on its own page. A
cuisine filter in the sidebar — Khmer, Thai, Vietnamese — should narrow the list."

</div>

<v-clicks>

- **Doing:** generate the project, give it one resource — **the collection** and **one item** —
  through controller → service → repository. The filter is a query parameter, not a new URL.
- **Goal:** `/recipes` → 3 recipes · `?cuisine=khmer` → 2 · `/recipes/1` → 1. **Tests 1, 2, 3.**

</v-clicks>

---
layout: default
zoom: 0.85
---

# Checkpoint 1 — the code

```java
public record Recipe(String id, String title, String cuisine, int minutes,
                     int servings, List<String> ingredients, String internalNotes) { }
```

```java
@RestController
@RequestMapping("/recipes")
public class RecipeController {

    private final RecipeService recipeService;          // the interface, not the Impl

    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @GetMapping                                  // GET /recipes  and  ?cuisine=khmer
    public List<Recipe> all(@RequestParam(required = false) String cuisine) {
        return recipeService.findAll(cuisine);
    }

    @GetMapping("/{id}")                         // GET /recipes/1
    public Recipe one(@PathVariable String id) {
        return recipeService.findById(id).orElse(null);
    }
}
```

<v-click>

<div class="text-red-500">

Two things wrong **on purpose**: returning `Recipe` leaks the note (checkpoint 3), `orElse(null)`
says `200` for nothing (checkpoint 2).

</div>

</v-click>

---
layout: default
---

# Checkpoint 1 — your turn

<div class="mt-6">

**start.spring.io** · Maven · Java 25 · Boot 4.1.1

Group `com.sunrise` · Artifact `recipe-api`

Tick **Spring Web** and **Validation**.

</div>

<v-click>

<div class="mt-8 border-2 border-red-500/50 rounded-lg px-6 py-4 bg-red-500/10">

**Type over the package name.** Initializr will give you `com.sunrise.recipe_api`.

You want `com.sunrise.recipeapi`. One underscore breaks all fourteen tests.

</div>

</v-click>

<v-click>

<div class="mt-6">

Then: `Recipe`, `RecipeRepository`, `RecipeController`. **Tests 1, 2, 3.**

</div>

</v-click>

---
layout: section
---

# Status codes that mean something

---
layout: default
---

# Everything returns 200 right now

<v-clicks>

- That is what a method returning an object does by default
- It is the second gap

</v-clicks>

<v-click>

<div class="mt-10 text-xl">

A status code is the first thing every client reads.

For many clients it is the **only** thing they read before deciding whether to open the body.

</div>

</v-click>

---
layout: default
---

# 2xx — it worked

<v-clicks>

- **`200 OK`** — here is your answer. Correct for most `GET`s.
- **`201 Created`** — a new resource exists. **Send a `Location` header naming it.**
- **`204 No Content`** — it worked, and there is deliberately nothing to say.

</v-clicks>

---
layout: default
---

# 4xx — the client is wrong

<v-clicks>

- **`400 Bad Request`** — malformed, or fails validation. Today's big one.
- **`401 Unauthorized`** — badly named. It means *unauthenticated*.
- **`403 Forbidden`** — I know who you are and you may not do this.
- **`404 Not Found`** — no resource by that name.
- **`409 Conflict`** — well-formed, but fights the current state.
- **`422 Unprocessable Content`** — understood, semantically wrong.

</v-clicks>

<v-click>

<div class="mt-8">

And **`500 Internal Server Error`** — your code threw and you did not handle it.
**Every `500` in a log is a bug with your name on it.**

</div>

</v-click>

---
layout: section
---

# The mistake

---
layout: default
---

# You have seen this API

```json
HTTP/1.1 200 OK

{"success": false, "error": "Recipe not found"}
```

The `200` is a lie told to every layer that is not your client's business logic.

<v-clicks>

- **Monitoring** counts it as a success
- **A cache** may store it
- **A retry policy** will not retry it
- **A generic client library** hands it back as data
- **Every consumer** must read your body to learn what HTTP has a field for

</v-clicks>

---
layout: statement
---

# You already have a status line.

Use it.

---
layout: default
---

# Saying it in Spring

Returning an object gives you `200`. To choose, return `ResponseEntity<T>`:

```java
@PostMapping
ResponseEntity<RecipeResponse> create(@RequestBody CreateRecipeRequest request) {
    Recipe saved = recipeService.create(request.toRecipe());
    URI location = URI.create("/recipes/" + saved.id());
    return ResponseEntity.created(location).body(RecipeResponse.from(saved));
}
```

<v-click>

`created(location)` sets **both** the `201` and the `Location` header.

</v-click>

<v-click>

<div class="mt-6 text-xl">

That header is not decoration. It is how a client learns the URL of the thing it just made
**without guessing how you build IDs.**

</div>

</v-click>

---
layout: default
---

# An empty success

```java
@DeleteMapping("/{id}")
ResponseEntity<Void> delete(@PathVariable String id) {
    recipeRepository.remove(id);
    return ResponseEntity.noContent().build();
}
```

<v-click>

`204`. No body. `Void`, not `null`.

</v-click>

---
layout: default
---

# There is a shorter way

<div class="grid grid-cols-2 gap-6 mt-8">

<div>

## `@ResponseStatus`

```java
@ResponseStatus(HttpStatus.CREATED)
@PostMapping
RecipeResponse create(...) {
```

Shorter.

Cannot set `Location`.

Cannot vary by outcome.

</div>

<div>

## `ResponseEntity`

```java
return ResponseEntity
    .created(uri)
    .body(...);
```

Longer.

Sets any header.

Decides at runtime.

</div>

</div>

<v-click>

<div class="mt-8 text-center">

Fixed status → the annotation. The method has to decide → `ResponseEntity`.

</div>

</v-click>

---
layout: default
---

# Pause

<div class="mt-6 border-2 border-amber-500/50 rounded-lg px-6 py-4 bg-amber-500/10">

Name the status:

1. A `GET` for an ID that does not exist
2. A `POST` that succeeds
3. A `POST` whose body is missing a required field
4. A `DELETE` of something already deleted
5. A `GET` of a collection that is empty

</div>

<v-click>

<div class="mt-6">

Number 5 is `200` with `[]`. **An empty collection is not a missing collection** — you met this
in last session's ninth test.

</div>

</v-click>

---
layout: section
---

# Checkpoint 2

Status codes you chose.

---
layout: default
---

# Checkpoint 2 — the brief

<div class="mt-4 border-2 border-blue-500/50 rounded-lg px-6 py-3 bg-blue-500/10">

"The phone app needs to add a recipe and be told where it ended up. It needs to delete one. And a
stale link to a deleted recipe must look different from one that loaded fine — it currently cannot."

</div>

<v-click>

| Request | Today | After |
|---|---|---|
| `POST /recipes` | does not exist | **`201`** + `Location: /recipes/4` |
| `DELETE /recipes/{id}` | does not exist | **`204`**, empty body |
| `GET /recipes/999` | `200`, empty body | **`404`** |

</v-click>

<v-click>

**Goal:** find the `201` and the `Location` with `curl -i`, on your own screen. **Tests 4, 5, 6.**

</v-click>

---
layout: default
zoom: 0.85
---

# Checkpoint 2 — the code

```java
@PostMapping
public ResponseEntity<Recipe> create(@RequestBody Recipe incoming) {
    Recipe saved = recipeService.create(incoming);
    return ResponseEntity
            .created(URI.create("/recipes/" + saved.id()))      // 201 + Location, one call
            .body(saved);
}

@DeleteMapping("/{id}")
public ResponseEntity<Void> delete(@PathVariable String id) {
    recipeService.delete(id);
    return ResponseEntity.noContent().build();                   // 204
}

@GetMapping("/{id}")
public ResponseEntity<Recipe> one(@PathVariable String id) {
    return recipeService.findById(id)
            .map(ResponseEntity::ok)                             // 200
            .orElseGet(() -> ResponseEntity.notFound().build()); // 404
}
```

<v-click>

Three lines of `Optional` ceremony to say "or 404". Correct — and deleted at checkpoint 5.

</v-click>

---
layout: default
---

# Checkpoint 2 — your turn

<v-clicks>

- `POST /recipes` → **`201`** with a **`Location`**
- `DELETE /recipes/{id}` → **`204`**, no body
- `GET /recipes/{id}` for a missing id → **`404`**

</v-clicks>

<v-click>

<div class="mt-8 border-2 border-green-500/50 rounded-lg px-6 py-4 bg-green-500/10">

Use `curl -i`. Find the `201` and the `Location` **with your own eyes**, not just in a green test.

</div>

</v-click>

<v-click>

<div class="mt-4">

**Tests 4, 5, 6.**

</div>

</v-click>

---
layout: statement
---

# Break

10 minutes

---
layout: section
---

# The entity is not the API

---
layout: default
---

# Look at your controller

```java
@GetMapping("/{id}")
Recipe one(@PathVariable String id) { ... }
```

<v-click>

`Recipe` is your internal record.

</v-click>

<v-click>

It is also, right now, your **public wire format**.

</v-click>

<v-click>

<div class="mt-8 text-xl">

Two jobs. One class. The moment they disagree, you have a problem you cannot fix without
breaking somebody.

</div>

</v-click>

---
layout: default
---

# One: you leak

<v-clicks>

- Add `internalNotes` for the kitchen staff
- Or a `costPrice`
- Or, on some other entity later, a `passwordHash`

</v-clicks>

<v-click>

<div class="mt-8 text-xl text-red-400">

It is in the JSON the same afternoon.

Nobody decided to publish it. It published itself.

</div>

</v-click>

---
layout: default
---

# Two and three

<div class="grid grid-cols-2 gap-8 mt-6">

<div>

## You cannot refactor

Rename `minutes` to `prepMinutes`, because the name was poor.

<v-click>

A five-second refactor.

**A breaking change for every client in production.**

</v-click>

</div>

<div>

## Your input is not your output

A client must not send an `id` — you assign it. But `Recipe` has one, so `POST` accepts it.

<v-click>

What do you do with `{"id": 999}`? The question only exists because **one class is doing both
jobs.**

</v-click>

</div>

</div>

---
layout: default
---

# The fix, and it is not clever

```java
// what a client may send
public record CreateRecipeRequest(
        String title, String cuisine, int minutes,
        int servings, List<String> ingredients) { }

// what a client gets back
public record RecipeResponse(
        String id, String title, String cuisine, int minutes,
        int servings, List<String> ingredients) {

    public static RecipeResponse from(Recipe recipe) { ... }
}
```

---
layout: default
---

# Yes, this is duplication

<v-click>

<div class="mt-8 text-xl">

It is the useful kind.

</div>

</v-click>

<v-clicks>

- The duplication **is** the seam between what you store and what you promise
- It lets those two change at different speeds
- `Recipe` is free to grow fields, change names, become a database entity
- None of it reaches a client

</v-clicks>

---
layout: statement
---

# They may look identical.

They may not be the same class.

---
layout: section
---

# Jackson and records

---
layout: default
---

# Out is simple. In is interesting.

<div class="grid grid-cols-2 gap-8 mt-6">

<div>

## Going out

Jackson reads the record's accessors.

One JSON key per component, in declaration order.

</div>

<div>

<v-click>

## Coming in

A record has **no no-arg constructor** and **no setters** — Jackson cannot make an empty object
and fill it.

So it uses the **canonical constructor**, matching JSON keys to parameter names.

</v-click>

</div>

</div>

<v-click>

<div class="mt-8 opacity-70">

No annotation needed: your class files carry parameter names. Jackson arrived with the web
starter — you did not add it.

</div>

</v-click>

---
layout: default
---

# Which brings us back

<div class="grid grid-cols-2 gap-8 mt-4">

<div>

```json
{"title": "Amok"}
```

...into a record that also has `cuisine`, `minutes`, `servings`, `ingredients`.

**Jackson does not fail.** It fills what it found and defaults the rest.

</div>

<div>

| Component type | Missing key becomes |
|---|---|
| `String`, `List`, any object | `null` |
| `int`, `long` | `0` |
| `boolean` | `false` |
| `Integer`, `Long`, `Boolean` | `null` |

</div>

</div>

<v-click>

<div class="mt-4">

`new CreateRecipeRequest("Amok", null, 0, 0, null)` — and `minutes = 0` is indistinguishable from
a client that genuinely sent zero.

</div>

</v-click>

---
layout: statement
---

# That is the 200 OK, explained.

Jackson's job is to translate, not to judge.

---
layout: default
---

# What about extra fields?

```json
{"title": "Amok", "colour": "green"}
```

<v-clicks>

- By default Spring Boot configures Jackson to **ignore** unknown properties
- Your client sends nonsense, your API accepts it
- Their typo silently does nothing, and they never learn

</v-clicks>

<div class="grid grid-cols-2 gap-8 mt-8">

<div>

## Strict

An unknown field is a `400`

Catches client typos immediately

</div>

<div>

## Tolerant

Unknown fields ignored

Add fields without breaking old clients

</div>

</div>

<v-click>

<div class="mt-10">

Most **public** APIs choose tolerance. Most **internal** APIs should probably choose strictness.

</div>

</v-click>

<v-click>

<div class="mt-6 text-xl">

The point: it is a decision, and right now you are making it by accident.

</div>

</v-click>

---
layout: default
---

# Pause

<div class="mt-8 border-2 border-amber-500/50 rounded-lg px-6 py-4 bg-amber-500/10">

`RecipeResponse` has an `id`. `CreateRecipeRequest` does not.

A client `POST`s `{"title": "Amok", "id": "999"}`.

**What happens, with Jackson's defaults? Is that what you want?**

</div>

---
layout: section
---

# Checkpoint 3

Split the DTOs.

---
layout: default
---

# Checkpoint 3 — the brief

<div class="mt-4 border-2 border-blue-500/50 rounded-lg px-6 py-3 bg-blue-500/10">

"The staff note is showing up in the app — a supplier found it. That stops today. And the app can
`POST` a recipe with an `id` of its choosing, so two phones can fight over one id. Ids are yours to
hand out."

</div>

<v-click>

One cause: `Recipe` is what you **store** *and* what you **publish**. Split it.

| Record | Job | `id` | `internalNotes` |
|---|---|---|---|
| `dto.CreateRecipeRequest` | what a client **sends** | no | no |
| `dto.RecipeResponse` | what a client **gets back** | yes | no |
| `domain.Recipe` | what **you** keep | yes | yes |

</v-click>

<v-click>

**Goal:** the note is gone from every response; `Recipe` in **no** controller signature. **Tests 7, 8.**

</v-click>

---
layout: default
zoom: 0.8
---

# Checkpoint 3 — the code

```java
public record CreateRecipeRequest(String title, String cuisine, int minutes,
                                  int servings, List<String> ingredients) {
    public Recipe toRecipe() {
        return new Recipe(null, title, cuisine, minutes, servings, ingredients, null);
    }
}

public record RecipeResponse(String id, String title, String cuisine, int minutes,
                             int servings, List<String> ingredients) {
    public static RecipeResponse from(Recipe r) {
        return new RecipeResponse(r.id(), r.title(), r.cuisine(),
                r.minutes(), r.servings(), r.ingredients());
    }
}
```

```java
@PostMapping
public ResponseEntity<RecipeResponse> create(@RequestBody CreateRecipeRequest request) {
    Recipe saved = recipeService.create(request.toRecipe());    // DTO in  -> domain
    return ResponseEntity
            .created(URI.create("/recipes/" + saved.id()))
            .body(RecipeResponse.from(saved));                  // domain  -> DTO out
}
```

<v-click>

The conversion happens **at the edge**, in the controller. Inside, it is still `Recipe`.

</v-click>

---
layout: default
---

# Checkpoint 3 — your turn

<v-clicks>

- `CreateRecipeRequest` in — no `id`, no `internalNotes`
- `RecipeResponse` out — no `internalNotes`
- **`Recipe` appears in no controller signature at all**

</v-clicks>

<v-click>

<div class="mt-8 border-2 border-red-500/50 rounded-lg px-6 py-4 bg-red-500/10">

**Prove the leak first.** Seed `internalNotes` with something that would embarrass you, and
`curl` it *before* you fix it.

You shipped that in checkpoint 1 without deciding to.

</div>

</v-click>

<v-click>

<div class="mt-4">

**Tests 7, 8.**

</div>

</v-click>

---
layout: section
---

# Validation you do not write

---
layout: default
---

# The obvious version

```java
if (request.title() == null || request.title().isBlank()) {
    return ResponseEntity.badRequest().body("title must not be blank");
}
if (request.cuisine() == null || request.cuisine().isBlank()) { ... }
if (request.minutes() < 1) { ... }
```

<v-clicks>

Every field. Every endpoint. Forever.

- Eighteen lines of checking around three lines of work
- The rules buried in a controller
- Nobody looking at the **type** would ever find them

</v-clicks>

---
layout: statement
---

# Put the rule on the field.

---
layout: default
---

# One starter

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

<v-click>

Bean Validation is a **Jakarta standard**. Spring Boot wires it in.

You ticked it at Initializr this morning.

</v-click>

---
layout: default
---

# The rules live where the field is

```java
public record CreateRecipeRequest(

        @NotBlank(message = "title must not be blank")
        String title,

        @Min(value = 1, message = "minutes must be at least 1")
        int minutes,

        @NotEmpty(message = "a recipe needs at least one ingredient")
        List<@NotBlank String> ingredients) { }
```

---
layout: default
---

# The ones you will use constantly

| Annotation | Holds when |
|---|---|
| `@NotNull` | not `null` |
| `@NotBlank` | a `String`, not `null`, not only whitespace |
| `@NotEmpty` | `String` / collection / map, not `null`, not empty |
| `@Size(min=, max=)` | length or size in range |
| `@Min` / `@Max` | a number in range |
| `@Positive` | sign check, more readable than `@Min(1)` |
| `@Email` | it looks like an email address |
| `@Pattern(regexp=)` | matches a regular expression |

<div class="mt-4 opacity-70">

All from `jakarta.validation.constraints`.

</div>

---
layout: default
---

# Three different questions

<div class="mt-8">

| On a `String` | `null` | `""` | `"   "` |
|---|---|---|---|
| `@NotNull` | ❌ | ✅ accepted | ✅ accepted |
| `@NotEmpty` | ❌ | ❌ | ✅ accepted |
| `@NotBlank` | ❌ | ❌ | ❌ |

</div>

<v-click>

<div class="mt-8 text-xl">

Only `@NotBlank` means what you usually mean.

</div>

</v-click>

---
layout: section
---

# The annotation that actually does it

---
layout: default
---

# Constraints do nothing on their own

```java
@PostMapping
ResponseEntity<RecipeResponse> create(
        @Valid @RequestBody CreateRecipeRequest request) {
```

<v-click>

The trigger is `@Valid`, **on the controller parameter.**

</v-click>

<v-clicks>

Leave it off and every constraint is silently inert.

- No warning
- No startup failure
- Nothing in the log
- The annotations sit there **looking like they are working**

</v-clicks>

---
layout: statement
---

# When validation "doesn't work"

this is the reason nine times in ten.

---
layout: default
---

# What happens when it fails

<v-clicks>

- Spring throws `MethodArgumentNotValidException`
- **Before your method body runs**
- Your controller never sees a bad object

</v-clicks>

<v-click>

<div class="mt-8 text-xl">

Which is the real prize: the method can assume its input is sane, and go back to being three
lines.

</div>

</v-click>

---
layout: default
---

# What validation is not for

<v-clicks>

- Bean Validation checks the **shape** of a request — present, non-blank, in range, well-formed
- It cannot check anything that needs to know the rest of your system

</v-clicks>

<div class="grid grid-cols-2 gap-8 mt-6">

<div>

## Validation

`title` is not blank

`minutes` is at least 1

`email` looks like an email

→ `400`

</div>

<div>

## Business rule

No two recipes share a title

This user used their quota

That ingredient is discontinued

→ usually `409`

</div>

</div>

<v-click>

<div class="mt-10 text-center text-xl">

**If answering the question requires a lookup, it is not validation.**

</div>

</v-click>

---
layout: default
---

# Pause — do this one, do not just think it

<div class="mt-6 border-2 border-amber-500/50 rounded-lg px-6 py-4 bg-amber-500/10">

1. Add `@NotBlank` to `title`. Do **not** add `@Valid`.
2. `POST` a blank title. Watch it succeed.
3. Add `@Valid`. Send it again.

</div>

<v-click>

<div class="mt-8 text-xl">

The difference between those two runs is the thing to remember from this segment.

</div>

</v-click>

---
layout: section
---

# Checkpoint 4

Validate the request.

---
layout: default
---

# Checkpoint 4 — the brief

<div class="mt-4 border-2 border-blue-500/50 rounded-lg px-6 py-3 bg-blue-500/10">

"Somebody submitted the new-recipe form with everything blank and it saved. A recipe with no title,
no cuisine, zero minutes and no ingredients is on the home page. The API should have refused it."

</div>

<v-click>

| Field | Rule | Annotation |
|---|---|---|
| `title`, `cuisine` | not empty, not just spaces | `@NotBlank` |
| `minutes`, `servings` | at least 1 | `@Min(1)` |
| `ingredients` | at least one | `@NotEmpty` |

</v-click>

<v-click>

**Goal:** a blank form gets **`400`**, not `201`, and nothing is saved. **Tests 9, 10.**

</v-click>

---
layout: default
zoom: 0.85
---

# Checkpoint 4 — the code

The rules sit on the record:

```java
public record CreateRecipeRequest(
        @NotBlank(message = "title must not be blank")           String title,
        @NotBlank(message = "cuisine must not be blank")         String cuisine,
        @Min(value = 1, message = "minutes must be at least 1")  int minutes,
        @Min(value = 1, message = "servings must be at least 1") int servings,
        @NotEmpty(message = "a recipe needs at least one ingredient")
        List<@NotBlank String> ingredients) {
    // toRecipe() unchanged
}
```

<v-click>

One annotation on the controller is what **runs** them:

```java
@PostMapping
public ResponseEntity<RecipeResponse> create(@Valid @RequestBody CreateRecipeRequest request) {
```

</v-click>

---
layout: default
---

# Checkpoint 4 — your turn

<v-clicks>

- Constraints on `CreateRecipeRequest`, each with a `message` a stranger could read
- **Run the tests with `@Valid` missing first.** Watch 9 and 10 fail.
- Add `@Valid`. Watch them pass.
- Then `curl -i` a blank title and **read the body you get**

</v-clicks>

<v-click>

<div class="mt-8">

Hold on to how unhelpful that body is. **Tests 9, 10.**

</div>

</v-click>

---
layout: section
---

# One error shape everywhere

---
layout: default
---

# Count your error shapes

<v-clicks>

- The `400` from validation — a wall of Spring-shaped JSON
- Your `404` from checkpoint 2 — a different shape entirely
- Your `409`, when you write one — a third

</v-clicks>

<v-click>

<div class="mt-8 text-xl">

**Every error in your API is currently a one-off.**

</div>

</v-click>

<v-clicks>

- A client writing error handling writes **a branch per endpoint**
- Multiply by every API in a company
- Which is why there is a standard for this

</v-clicks>

---
layout: default
---

# RFC 9457 — Problem Details

```json
{
  "type": "https://docs.sunrise.dev/errors/validation-failed",
  "title": "Validation failed",
  "status": 400,
  "detail": "The request body has 2 invalid fields",
  "instance": "/recipes"
}
```

<v-click>

One JSON shape for all HTTP errors. Everywhere.

</v-click>

---
layout: default
zoom: 0.9
---

# The five members

<v-clicks>

- **`type`** — a URI naming the *kind* of problem. **Branch on this** — it is stable, your prose is not.
- **`title`** — a short human summary of that type
- **`status`** — the HTTP status, repeated in the body
- **`detail`** — what went wrong with *this specific* request
- **`instance`** — which request it was

</v-clicks>

<v-click>

<div class="mt-4 border-2 border-blue-500/50 rounded-lg px-6 py-3 bg-blue-500/10">

**`type` names the error. `instance` names the request.**

`type` is not an endpoint — it is a name, like an exception class. In a real API it links to a page
documenting the error; `docs.sunrise.dev` is an example and leads nowhere. Unset, it is
`about:blank`.

</div>

</v-click>

<v-click>

<div class="mt-4">

You may add your own members alongside. That is where per-field errors go.

</div>

</v-click>

---
layout: statement
---

# Spring already has it.

`org.springframework.http.ProblemDetail` — no dependency, no library to pick.

---
layout: default
---

# Turn it on for Spring's own errors

```properties
spring.mvc.problemdetails.enabled=true
```

<v-clicks>

- Covers the errors **Spring MVC raises itself** — unsupported method, unreadable body
- Does **not** cover exceptions you wrote

</v-clicks>

<v-click>

<div class="mt-6">

For those you write a handler. You have earned one.

</div>

</v-click>

---
layout: default
---

# `@RestControllerAdvice`

<v-clicks>

- A bean that catches exceptions from **all** your controllers
- Turns them into responses
- One place, one shape, every endpoint

</v-clicks>

---
layout: default
zoom: 0.8
---

# Your own exception

```java
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {

    @ExceptionHandler(RecipeNotFoundException.class)
    ProblemDetail handleNotFound(RecipeNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setType(URI.create("https://docs.sunrise.dev/errors/recipe-not-found"));
        problem.setTitle("Recipe not found");
        return problem;
    }
}
```

<v-click>

<div class="mt-2 border-2 border-red-500/50 rounded-lg px-6 py-2 bg-red-500/10">

**`@Order` is required.** The property switched on Boot's own advice, which handles validation
errors too. Without `@Order`, Boot's answers first — `"Bad Request"`, no `errors` map.

</div>

</v-click>

---
layout: default
---

# And the payoff

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
    Map<String, String> errors = new HashMap<>();
    for (var fieldError : exception.getFieldErrors()) {
        errors.put(fieldError.getField(), fieldError.getDefaultMessage());
    }

    ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "The request body has " + errors.size() + " invalid field(s)");
    problem.setTitle("Validation failed");
    problem.setProperty("errors", errors);
    return problem;
}
```

<v-clicks>

- `getFieldErrors()` — one entry per failed constraint, each knowing its field name and the
  `message` you wrote on the annotation
- `setProperty` adds a member beside the standard five

</v-clicks>

---
layout: default
---

# What the client gets

```json
{
  "type": "https://docs.sunrise.dev/errors/validation-failed",
  "title": "Validation failed",
  "status": 400,
  "detail": "The request body has 2 invalid field(s)",
  "instance": "/recipes",
  "errors": {
    "title": "title must not be blank",
    "minutes": "minutes must be at least 1"
  }
}
```

<v-click>

Every field, named, with a reason. **From annotations you wrote once, on a record.**

</v-click>

---
layout: default
---

# And the controller gets simpler

<div class="grid grid-cols-2 gap-6 mt-6">

<div>

## Before

```java
return recipeService.findById(id)
    .map(RecipeResponse::from)
    .map(ResponseEntity::ok)
    .orElseGet(() ->
        ResponseEntity.notFound().build());
```

</div>

<div>

## After

```java
// the service throws when it is missing
return RecipeResponse.from(
    recipeService.findById(id));
```

</div>

</div>

<v-click>

<div class="mt-8 text-center">

The client sees the same `404`. **You changed where the decision lives.**

</div>

</v-click>

---
layout: default
---

# There is a second way

<v-clicks>

- Spring ships `ResponseEntityExceptionHandler`
- Extend it and inherit handling for **every** Spring MVC exception
- The right choice on a large API

</v-clicks>

<v-click>

<div class="mt-6 border-2 border-amber-500/50 rounded-lg px-6 py-4 bg-amber-500/10">

But it maps `MethodArgumentNotValidException` in a `final` method — you override a **protected
hook**, not an `@ExceptionHandler`.

Know it exists. Learn the plain advice first.

</div>

</v-click>

---
layout: default
---

# Pause

<div class="mt-8 border-2 border-amber-500/50 rounded-lg px-6 py-4 bg-amber-500/10">

Your advice catches `RecipeNotFoundException`.

What happens today if your service throws a `NullPointerException` instead?

**What does the client see? What should they see?**

</div>

---
layout: section
---

# Checkpoint 5

The advice, then three decisions.

---
layout: default
---

# Checkpoint 5 — the brief

<div class="mt-4 border-2 border-blue-500/50 rounded-lg px-6 py-3 bg-blue-500/10">

"Every failure from your API looks different. We are writing a `switch` per endpoint. Give us
**one** error body, always the same keys — and for a form problem, the field names and messages so
we can put them under the right boxes."

</div>

<v-clicks>

- **Doing:** move every "this went wrong" decision into **one** class — the advice — answering in
  RFC 9457 `ProblemDetail` shape. Then three tests of your own.
- **Goal:** a blank title and a missing id come back with **the same keys**; the `400` adds an
  `errors` map. **Test 11, 1–10 still green**, then **12, 13, 14** — three decisions.

</v-clicks>

---
layout: default
zoom: 0.75
---

# Checkpoint 5 — the code: the advice

```java
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)       // before Boot's own advice
public class ApiExceptionHandler {

    @ExceptionHandler(RecipeNotFoundException.class)
    ProblemDetail handleNotFound(RecipeNotFoundException exception) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setType(URI.create("https://docs.sunrise.dev/errors/recipe-not-found"));
        problem.setTitle("Recipe not found");
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new HashMap<>();
        for (var fieldError : exception.getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "The request body has " + errors.size() + " invalid field(s)");
        problem.setType(URI.create("https://docs.sunrise.dev/errors/validation-failed"));
        problem.setTitle("Validation failed");
        problem.setProperty("errors", errors);
        return problem;
    }
}
```

---
layout: default
zoom: 0.85
---

# Checkpoint 5 — the code: a shorter controller

```properties
spring.mvc.problemdetails.enabled=true
```

```java
// RecipeServiceImpl -- the service decides: missing is an ERROR
public Recipe findById(String id) {
    return recipeRepository.findById(id)
            .orElseThrow(() -> new RecipeNotFoundException(id));
}
```

```java
// RecipeController -- one line
@GetMapping("/{id}")
public RecipeResponse one(@PathVariable String id) {
    return RecipeResponse.from(recipeService.findById(id));
}
```

<v-click>

No `ResponseEntity`, no `Optional` dance — and **test 6 stays green**.

Same response. Different home for the decision.

</v-click>

---
layout: default
---

# Checkpoint 5 — your turn

<v-clicks>

- `spring.mvc.problemdetails.enabled=true`, and `curl` a bad request again
- Write `ApiExceptionHandler` — two handlers
- Then **simplify** `GET /recipes/{id}` to throw instead of returning `ResponseEntity`
- Test 6 stays green. That is the point.

</v-clicks>

<v-click>

<div class="mt-6">

**Test 11.**

</div>

</v-click>

---
layout: default
---

# Checkpoint 5 — then three decisions

The last three tests are given too. Each one is a **decision**, written down as an assertion.

<v-clicks>

- **12** — last session's question, answered: missing `cuisine` → **`400`**, naming the field
- **13** — a second `DELETE` → **`404`**. Still idempotent: the promise is about **server state**
- **14** — `PUT` to an id nobody has → **`404`**. The other defensible answer is `201`

</v-clicks>

<v-click>

<div class="mt-8 border-2 border-green-500/50 rounded-lg px-6 py-4 bg-green-500/10">

Read the comment above each test: it says **what the other choice would cost**.

Then `mvn test`. **All fourteen green.**

</div>

</v-click>

---
layout: section
---

# What you now owe every API you write

---
layout: default
---

# Four gaps, closed

| Gap | Closed by |
|---|---|
| Nothing said the field was required | Bean Validation, and `@Valid` |
| The status code meant nothing | `ResponseEntity`, chosen on purpose |
| The stored object was the wire format | A request DTO and a response DTO |
| Every failure had a different shape | `ProblemDetail` and one advice |

<v-clicks>

And the habits underneath: resources are **nouns**, identity goes in the **path** and filters in
the **query string**, and the method you choose is a **promise** to every client that will ever
retry.

</v-clicks>

---
layout: default
---

# Three things to notice before next time

<v-clicks>

1. Look at your controller's constructor. How much of that class is not the interesting part?
2. Count the lines in your controller that are **not about HTTP**. Where should they live?
3. Where would you put *"log how long every endpoint takes"* — without editing every endpoint?

</v-clicks>

---
layout: statement
---

# Next session answers all three.

Then asks what has been doing the rest of this for you the whole time.
