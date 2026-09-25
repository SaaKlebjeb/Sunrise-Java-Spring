# REST APIs — Design, Validation & Errors

*The shape of an API somebody else can use*

*A guided lesson. Read it start to finish; work the code as you go; stop at the **Pause**
prompts and actually answer them before reading on.*

**Before you start**

- Complete the Spring MVC session. You do not bring that project with you — today starts a new
  one, and it is the project you will keep for the rest of the course.
- **Network access at the start of the session** — you generate a new project and two starters
  come down from Maven Central.
- **Open the `pom.xml`, not the folder.** Every session says this because it is the commonest
  ten-minute loss in the course.
- **Java 25 · Spring Boot 4.1.1 · Maven.** Do not mix versions.
- **Fourteen checks, across five checkpoints.** Every test — and every class — is given to you in
  the exercise README, from generating the project to the last green test. Your job is to read it,
  run it, and be able to say why each line is there. The last three tests are each a design
  decision, and the README says what was decided and what the alternative costs.
- **You are not asked to invent the requirements.** Every checkpoint in the README opens with the
  piece of the client's brief that it answers, and then gives you the code. Your attention belongs
  on what each line does, not on guessing what a recipe site needs.

**By the end you'll be able to:**

- Say what REST constrains, and why "JSON over HTTP" is not the same claim
- Design resource URLs that name things rather than describe actions
- Say which HTTP methods are safe, which are idempotent, and what a client is allowed to do with
  that knowledge
- Choose a status code on purpose, and return it with `ResponseEntity`
- Explain why the object you store is not the object you send, and split them
- Validate a request body with `@Valid` and Bean Validation constraints
- Give every error in your API the same shape, using `ProblemDetail` and `@RestControllerAdvice`

---

## Who you are building for today

One paragraph, read once, and then you can forget it — this lecture and the README both repeat
whichever piece you need right before each checkpoint.

> **Sunrise Kitchen** runs a small recipe site and wants an HTTP API so their web team and their
> phone app can share one set of recipes. A recipe has a title, a cuisine, minutes, servings and
> ingredients. Staff also keep a **private internal note** on some recipes, and that note must never
> reach a client. They need to list recipes, filter by cuisine, read one, add one, replace one and
> delete one — and when something goes wrong they want the same error shape every time, naming the
> field and the reason.

That brief is deliberately small, and it is handed to you rather than discovered. **The work today
is technical.** Every checkpoint tells you what the client wanted and then shows you the code; what
you are being asked to do is understand why each piece is shaped the way it is, run it, and break it
on purpose where the README tells you to.

### What we are building

Translated out of the client's words, the brief is six operations on one kind of thing:

| The client says | Method and URL | Answers with |
|---|---|---|
| "Show all recipes" | `GET /recipes` | `200` and a list |
| "Filter the list by cuisine" | `GET /recipes?cuisine=khmer` | `200` and a shorter list |
| "Show one recipe" | `GET /recipes/{id}` | `200`, or `404` if there is no such recipe |
| "Add a recipe" | `POST /recipes` | `201` and a `Location` header, or `400` naming the bad fields |
| "Replace a recipe" | `PUT /recipes/{id}` | `200`, or `404` — a decision, in the last test |
| "Delete a recipe" | `DELETE /recipes/{id}` | `204`, nothing in the body |

Five packages hold it — `domain`, `dto`, `repository`, `service`, `controller` — and no database
yet. Recipes live in a `Map` until the persistence sessions. A request always travels
**controller → service → repository**, and no layer skips one.

### Where you will be at the end

Five checkpoints, one gap closed by each of the last four. By the end the controller looks like
this — **read it now, not to understand it, but so you recognise each line when it arrives**:

```java
@RestController
@RequestMapping("/recipes")
public class RecipeController {

    @GetMapping
    public List<RecipeResponse> all(@RequestParam(required = false) String cuisine) { ... }

    @GetMapping("/{id}")
    public RecipeResponse one(@PathVariable String id) { ... }         // throws -> 404

    @PostMapping
    public ResponseEntity<RecipeResponse> create(
            @Valid @RequestBody CreateRecipeRequest request) { ... }    // 201 + Location

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) { ... } // 204
}
```

`Recipe` — the thing you store — appears nowhere in it. And every failure, whatever caused it,
comes back in one shape the client can handle once:

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

**The goal, stated as something you can check:** that controller, that error body, and **all
fourteen tests green**.

---

## The question we refused to answer

Last session ended with a test you wrote yourself and probably got wrong.

```java
// POST /loans with a body that is missing the member
{"isbn": "978-0132350884"}
```

You had to decide what *should* happen, assert it, and then look at what actually happened.

Most rooms assert `400 Bad Request`. That is the right instinct and the wrong answer. What you
actually got was **`200 OK`**, and a loan recorded for a member called `null`.

Nothing was broken. Jackson read the JSON, found no `member` key, and did the only thing it can
do with a missing `String` on a record: it passed `null` to the constructor. Your controller
took the object, your service stored it, and the whole stack reported success — because nobody
in that stack had been told the field was required.

That is not a bug in Spring. It is a gap in your API, and it is one of four:

1. **Nothing said the field was required.** Today: Bean Validation.
2. **The response said `200` when nothing sensible happened.** Today: status codes on purpose.
3. **Your internal record was also your wire format.** Today: request and response DTOs.
4. **When it does finally fail, the failure has no agreed shape.** Today: `ProblemDetail`.

Four gaps, one session. Everything below closes one of them.

> **Pause.** Before reading on, write down the status code you think a `POST` with a missing
> required field *should* return, and one sentence saying what the response body should contain.
> You will check that guess twice today.

---

## REST is a set of constraints

Ask ten developers what REST means and nine will say "JSON over HTTP". That is not a definition;
it is a description of the traffic.

REST is a set of **constraints** on how a distributed system is put together. Accept the
constraints and you get properties you did not have to build. The ones that matter to you today:

**Client–server.** The client holds the user interface, the server holds the data and the rules.
Neither knows how the other is implemented. You can rewrite your Android app without touching
the server.

**Statelessness.** Every request carries everything needed to understand it. The server keeps
nothing about "this client" between requests. This is the constraint that lets you run ten
copies of your application behind a load balancer without any of them sharing memory — and it is
why the security session later has something to say about where a logged-in identity actually
lives.

**Uniform interface.** The same small set of methods, applied to everything. You do not learn a
new vocabulary per resource. `GET` means the same thing against `/recipes` as it does against
any other API in the world.

**Cacheability.** A response can say whether it may be stored and reused. This only works because
of the method contracts you will meet in a moment.

**Layered system.** A client cannot tell whether it is talking to your application or to a proxy
in front of it. That is what makes gateways, CDNs and load balancers possible at all.

### What this is not

**REST does not require JSON.** JSON won because it is convenient, not because REST demands it.
The constraint is *uniform interface*, and part of that is letting the client and server agree
on a representation via a header, not a URL. A single `@GetMapping` can honestly serve two
formats:

```java
@GetMapping(value = "/recipes/{id}", produces = {
        MediaType.APPLICATION_JSON_VALUE,
        MediaType.APPLICATION_XML_VALUE
})
public RecipeResponse getRecipe(@PathVariable Long id) {
    return recipeService.findById(id);
}
```

```
curl -H "Accept: application/json" localhost:8080/recipes/1
{"id":1,"title":"Amok","minutes":45}

curl -H "Accept: application/xml" localhost:8080/recipes/1
<RecipeResponse><id>1</id><title>Amok</title><minutes>45</minutes></RecipeResponse>
```

Same method, same URL, same rules — the `Accept` header picked the body. That is the "In and out
— resolvers, converters" segment later today, arriving early. Compare that to `/getRecipeAsXml`,
which is a second URL for a format choice that never needed one.

**REST does not require Spring.** Nothing above names a framework. The same resource, same verb,
same status code, written in Node with Express:

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

Compare that to the `@GetMapping` / `@PostMapping` pair you will write today. Same URL, same verb,
same `201` + `Location` header on create — Spring's annotations are a shorter way to say what the
Express callbacks say by hand. Neither is more RESTful than the other; Spring is a convenience
over the constraints, not the source of them. If you moved this course to Node or Go tomorrow,
every rule in this section would still hold; only the syntax would change.

**REST does not forbid you from being pragmatic.** You already saw one exception: "publish this
recipe" is not a noun, and none of the three answers was pure. Here is a second one, on the same
URL you have been using all session:

```
GET /recipes/search?q=khmer&maxMinutes=30
```

Strictly, a URL should name **one thing** — `/recipes` names the collection, `/recipes/42` names
one item. `/recipes/search` names neither; it is a question, not a resource. A purist would say
this belongs in the query string on `/recipes` instead: `GET /recipes?q=khmer&maxMinutes=30`.

Most real APIs use `/search` anyway, because it reads clearly and it is what every developer
expects to type. That is the whole point: **being 100% pure and being usable are sometimes
different goals**, and when they pull apart, being consistent and honest about the choice beats
being technically correct. Nobody's API follows every rule in this section without exception —
what makes an API good is that its exceptions are on purpose, not accidents nobody noticed.

> **Pause.** Statelessness says the server keeps nothing between requests. Your `lending-desk`
> kept its books in a `@Repository` holding a `List`. Is that a violation? Write one sentence
> either way before reading on.
>
> *(It is not. Statelessness is about **client session state** — who is logged in, what page
> they are on, what is in step 3 of their wizard. Application state that every client sees the
> same way is exactly what a server is for.)*

---

## Resources, and the URLs that name them

A resource is a **thing**, and its URL is that thing's name. This is the single rule from which
most of REST's URL guidance falls out.

Names are nouns. Not verbs.

```
GET    /recipes              the collection
POST   /recipes              add one to the collection
GET    /recipes/42           one item
PUT    /recipes/42           replace that item
DELETE /recipes/42           remove that item
```

Now the same API written the way it is usually written first:

```
GET  /getAllRecipes
POST /createRecipe
POST /updateRecipe?id=42
POST /deleteRecipe?id=42
```

Both work. Both return JSON. Only one of them is an interface anybody can predict.

Look at what the second version costs you. Every operation is a `POST`, so no proxy or browser
can cache anything, and no client can safely retry anything. The verb lives in the path, so
adding an operation means adding a URL and documenting it. And `/getAllRecipes` tells a new
developer nothing about `/getRecipeIngredients` — there is no system to learn, only a list to
memorise.

### The shape of a path

**Collections are plural.** `/recipes`, not `/recipe`. You are naming a set.

**Identity goes in the path.** `/recipes/42` — the 42 is part of the name of the thing.

**Filtering goes in the query string.** `/recipes?cuisine=khmer` — this is still the collection,
just a view of it. If it returns a list, it is a filter; if it returns one item, the value was
identity and belongs in the path.

**Nesting shows containment, and only one level of it.**
`/recipes/42/ingredients` is fine. `/users/7/recipes/42/ingredients/3/substitutions` is a
database schema that escaped onto the internet.

**No file extensions, no trailing slashes, lowercase, hyphens not underscores.** `/recipe-books`,
not `/recipeBooks` or `/recipe_books`. These are conventions, not laws, but they are conventions
your clients already have.

### The awkward cases, honestly

Not everything is a noun. What is the URL for "publish this recipe"?

You have three defensible answers, and a room will argue about them for ten minutes:

- `PUT /recipes/42/published` with a body of `true` — you made the state a resource.
- `POST /recipes/42/publish` — you admitted it is an action. Widely done. Not a sin.
- `PATCH /recipes/42` with `{"status": "PUBLISHED"}` — you changed a field.

Pick one and be consistent. An API with a consistent imperfect convention is easier to use than
one that is perfect in three places and improvised in six.

> **Pause.** Design the URLs for: all recipes by one author; one recipe's ingredients; searching
> recipes that take under 30 minutes. Write them before reading on, then check each one against
> "is this a noun, and is identity in the right place?"

---

## The promises a method makes

The uniform interface is only worth something because each method comes with a contract. Two
words carry most of it.

**Safe** — the method does not change anything on the server. A client, a browser, a crawler or
a proxy may call it whenever it likes, including twice, including for no reason.

**Idempotent** — calling it once and calling it five times leave the server in the same state.
The *responses* may differ. The *state* may not.

| Method | Safe | Idempotent | What it means |
|---|---|---|---|
| `GET` | ✅ | ✅ | Read. Never changes anything. |
| `HEAD` | ✅ | ✅ | The headers of a `GET`, without the body. |
| `POST` | ❌ | ❌ | Create, or anything that does not fit. Calling twice creates two. |
| `PUT` | ❌ | ✅ | Replace at a known URL. Calling twice leaves one, unchanged. |
| `PATCH` | ❌ | ❌ | Partial update. Not idempotent in general. |
| `DELETE` | ❌ | ✅ | Remove. Calling twice leaves it gone. |

### Why anybody cares

You have done this yourself: click "Save" on a slow website, see nothing happen for a few seconds,
and click it again just in case.

If that button sends `PUT /recipes/42`, clicking twice is harmless — the second click just writes
the same recipe over itself. If it sends `POST /recipes`, clicking twice **creates two recipes**,
and now there is a duplicate sitting in the database that somebody has to notice and delete.

**Idempotency is what makes an impatient click, or an app that retries a failed request
automatically, safe instead of a bug.** You are choosing, with your method, whether doing the
same thing twice by accident costs you anything.

### The one that catches everybody

`DELETE /recipes/42` twice: the first returns `204 No Content`, the second returns `404 Not Found`.
Different responses. Is `DELETE` still idempotent?

**Yes.** Idempotency is a claim about *server state*, not about the response. After one call the
recipe is gone; after five calls the recipe is gone. That is the whole promise.

> **Pause.** Your API needs "add one to the number of times this recipe was cooked". Which method?
> Is your answer safe? Idempotent? What happens when a flaky client retries it three times?

---

## Three layers, one direction

Before the first line of code, decide where code *goes*. Everything you write today lives in one
of three layers, and each layer answers exactly one question:

| Layer | Answers | Knows about HTTP? | Knows where data is kept? |
|---|---|---|---|
| **controller** | Which URL? Which method? Which status code? | **yes** | no |
| **service** | What is the application allowed to do? | no | no |
| **repository** | Where are recipes kept, and how do I get them back? | no | **yes** |

A request goes down — controller → service → repository — and the answer comes back up. **No
layer skips one**: the controller never calls the repository directly, even when it would be one
line shorter.

The test for "which layer does this line belong to?" is to imagine replacing a neighbour. Swap
HTTP for a command-line tool tomorrow: everything in the controller is rewritten, nothing in the
service is. Swap the `Map` for PostgreSQL: the repository is rewritten, the service is not. **A
line that would survive both swaps belongs in the service.**

### The service is an interface and a class

```java
public interface RecipeService {                 // WHAT the application can do
    List<Recipe> findAll(String cuisine);
    Optional<Recipe> findById(String id);
}

@Service
public class RecipeServiceImpl implements RecipeService {   // HOW it does it
    private final RecipeRepository recipeRepository;
    // ...
}
```

The controller's constructor asks for a **`RecipeService`** — the interface — and Spring hands it
the one `@Service` that implements it. The controller never names `RecipeServiceImpl`. It is the
same *program to an interface* you used for `RecipeRepository`, one layer up. Many teams write
every service this way; some skip the interface until a second implementation exists. You will
meet both. This course uses the interface.

### Where every file goes

By the end of today the project holds eleven classes, and each has exactly one home:

```
com.sunrise.recipeapi
├── RecipeApiApplication.java          generated
├── domain/
│   └── Recipe.java                    checkpoint 1
├── dto/
│   ├── CreateRecipeRequest.java       checkpoint 3
│   └── RecipeResponse.java            checkpoint 3
├── repository/
│   ├── RecipeRepository.java          checkpoint 1   (interface)
│   └── InMemoryRecipeRepository.java  checkpoint 1   (@Repository)
├── service/
│   ├── RecipeService.java             checkpoint 1   (interface)
│   ├── RecipeServiceImpl.java         checkpoint 1   (@Service)
│   └── RecipeNotFoundException.java   checkpoint 5
└── controller/
    ├── RecipeController.java          checkpoint 1   (@RestController)
    └── ApiExceptionHandler.java       checkpoint 5   (@RestControllerAdvice)
```

The four test classes all go in the **test** tree's `controller` package. The exercise README gives
every file in full, with its path as the header above the code block.

Three rules hold for every one of them:

1. **Each class asks for the layer below by its interface**, through the constructor. Nothing calls
   `new` on a repository or a service.
2. **Only the controller knows HTTP** — no `ResponseEntity`, status code or `@PathVariable` below it.
3. **Only the `dto` records go on the wire.** `Recipe` is converted in the controller, both ways.

### Be honest: today it is thin

At checkpoint 1 the service only passes calls through to the repository, and it is fair to ask
why it is there. Two answers. It takes its **first decision at checkpoint 5** — asking for a recipe
that is not there becomes an *error*, decided once, in the service — and the controller gets
shorter because of it. And **next session it gets real business rules** (no two recipes with the
same title, a limit on ingredients) that belong in neither neighbour. The layer is here from the
first endpoint so the controller never learns the habit of talking to storage.

> **Pause.** "A recipe title may not be longer than 100 characters" and "a recipe title may not
> repeat an existing one" — which layer does each belong in? *(Hint: one of them needs to look
> something up.)*

---

## Checkpoint 1

*The client wants a list of recipes, a cuisine filter on that list, and a page for one recipe.*

**What the client asked for**

> "The web team needs to show a list of recipes on the home page, and a single recipe on its own
> page. There is also a cuisine filter in the sidebar — Khmer, Thai, Vietnamese — and picking one
> should narrow the list."

**What we are doing.** Generating the project and giving it its first resource, through all three
layers. Two things to read: **the collection** and **one item**. The filter narrows the
collection; it is a query parameter, not a new URL.

**The goal.** `curl http://localhost:8080/recipes` returns three seeded recipes,
`?cuisine=khmer` returns two, `/recipes/1` returns one — **tests 1, 2 and 3 green.**

**The code.** One record for the recipe, a repository and a service each behind an interface, and a
controller mapped at `/recipes` that talks **only to the service**:

```java
public record Recipe(String id, String title, String cuisine, int minutes,
                     int servings, List<String> ingredients, String internalNotes) { }
```

```java
@RestController
@RequestMapping("/recipes")
public class RecipeController {

    private final RecipeService recipeService;              // the interface, not the Impl

    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @GetMapping                                   // GET /recipes  and  GET /recipes?cuisine=khmer
    public List<Recipe> all(@RequestParam(required = false) String cuisine) {
        return recipeService.findAll(cuisine);
    }

    @GetMapping("/{id}")                          // GET /recipes/1
    public Recipe one(@PathVariable String id) {
        return recipeService.findById(id).orElse(null);
    }
}
```

Two things in there are wrong on purpose. Returning `Recipe` itself leaks `internalNotes` —
checkpoint 3. `orElse(null)` answers `200` for a recipe that does not exist — checkpoint 2.

**Your turn.** Generate the project, then build `Recipe`, `RecipeRepository`,
`InMemoryRecipeRepository`, `RecipeService`, `RecipeServiceImpl` and `RecipeController` from the
exercise README, which has every class in full, imports and seed data included.

---

## Status codes that mean something

You now have endpoints that return `200` for everything, because that is what a method returning
an object does by default. That is the second gap.

A status code is the first thing every client reads, and for a great many clients it is the
*only* thing they read before deciding whether to look at the body at all.

### The ones you owe your clients

**2xx — it worked.**

- `200 OK` — here is your answer. The default, and correct for most `GET`s.
- `201 Created` — a new resource exists. **Send a `Location` header naming it.**
- `204 No Content` — it worked and there is deliberately nothing to say. The right answer to a
  `DELETE`.

**4xx — the client is wrong.**

- `400 Bad Request` — the request is malformed or fails validation. Today's big one.
- `401 Unauthorized` — you have not told me who you are. *(Badly named: it means unauthenticated.)*
- `403 Forbidden` — I know who you are and you may not do this.
- `404 Not Found` — no resource by that name.
- `409 Conflict` — the request is well-formed but fights the current state. A duplicate, or an
  edit to something that moved underneath you.
- `422 Unprocessable Content` — well-formed, understood, semantically wrong. Some teams use this
  for validation instead of `400`. Both are defensible; pick one.

**5xx — you are wrong.**

- `500 Internal Server Error` — your code threw and you did not handle it. Every `500` in a log
  is a bug with your name on it.

### The mistake

```json
HTTP/1.1 200 OK

{"success": false, "error": "Recipe not found"}
```

This is the most common mistake in REST APIs, and it is worth being precise about why it is bad.

The `200` is a lie told to every layer of the system that is not your client's business logic.
Monitoring counts it as a success. A cache may store it. A retry policy will not retry it. A
generic client library hands it back as data. And every consumer of your API must now read your
body to learn something HTTP has a field for — which means learning *your* error convention,
which no other API shares.

You already have a status line. Use it.

### Saying it in Spring

Returning an object gives you `200`. To choose, return `ResponseEntity<T>`:

```java
@PostMapping
ResponseEntity<RecipeResponse> create(@RequestBody CreateRecipeRequest request) {
    Recipe saved = recipeRepository.add(request.toRecipe());
    URI location = URI.create("/recipes/" + saved.id());
    return ResponseEntity.created(location).body(RecipeResponse.from(saved));
}
```

`ResponseEntity.created(location)` sets both the `201` and the `Location` header. That header is
not decoration: it is how a client learns the URL of the thing it just made without guessing how
you build IDs.

For an empty success:

```java
@DeleteMapping("/{id}")
ResponseEntity<Void> delete(@PathVariable String id) {
    recipeRepository.remove(id);
    return ResponseEntity.noContent().build();
}
```

There is also `@ResponseStatus(HttpStatus.CREATED)` on the method, which is shorter and cannot
set a `Location` header or vary by outcome. Use it when the status is fixed; use `ResponseEntity`
when the method has to decide.

> **Pause.** For each, name the status: a `GET` for an ID that does not exist; a `POST` that
> succeeds; a `POST` whose body is missing a required field; a `DELETE` of something already
> deleted; a `GET` of a collection that is empty.
>
> *(That last one is `200` with `[]`. An empty collection is not a missing collection — you met
> this in last session's ninth test.)*

---

## Checkpoint 2

*The client wants the app to be told where a new recipe ended up, and to be able to tell a deleted
recipe apart from one that loaded fine.*

**What the client asked for**

> "The phone app needs to add a recipe and get told where it ended up, so it can jump straight to
> the new page without re-fetching the list. It needs to delete one. And when somebody follows a
> stale link to a recipe that has been deleted, the app must be able to tell that apart from a
> recipe that loaded fine — it currently cannot."

**What we are doing.** Three sentences, three status codes, each one chosen on purpose rather than
left at Spring's default `200`:

| Request | Today | After this checkpoint |
|---|---|---|
| `POST /recipes` | does not exist | **`201`** and a `Location: /recipes/4` header |
| `DELETE /recipes/{id}` | does not exist | **`204`**, empty body |
| `GET /recipes/999` | `200` with an empty body | **`404`** |

**The goal.** `curl -i` a `POST` and find the `201` and the `Location` header **on your own
screen** — **tests 4, 5 and 6 green.**

**The code.** `ResponseEntity` is how a controller method says the status and the headers as well
as the body:

```java
@PostMapping
public ResponseEntity<Recipe> create(@RequestBody Recipe incoming) {
    Recipe saved = recipeService.create(incoming);
    return ResponseEntity
            .created(URI.create("/recipes/" + saved.id()))   // 201 + Location, in one call
            .body(saved);
}

@DeleteMapping("/{id}")
public ResponseEntity<Void> delete(@PathVariable String id) {
    recipeService.delete(id);
    return ResponseEntity.noContent().build();                // 204
}

@GetMapping("/{id}")
public ResponseEntity<Recipe> one(@PathVariable String id) {
    return recipeService.findById(id)
            .map(ResponseEntity::ok)                          // 200 + the recipe
            .orElseGet(() -> ResponseEntity.notFound().build()); // 404
}
```

Look at what `one()` now costs — three lines of `Optional` ceremony to say "or 404". It is correct,
and at checkpoint 5 you delete it.

**Your turn.** Replace `one()`, add `create()` and `delete()`, and run the `curl -i` from the
README.

---

## Break — 10 minutes

---

## The entity is not the API

Here is what your controller probably looks like right now:

```java
@GetMapping("/{id}")
Recipe one(@PathVariable String id) { ... }
```

`Recipe` is your internal record. It is also, right now, your public wire format. Those are two
different jobs held by one class, and the moment they disagree you have a problem you cannot fix
without breaking somebody.

### Three ways it goes wrong

**You leak.** Add an `internalNotes` field for the kitchen staff, or a `costPrice`, or later a
`passwordHash` on some other entity. It is in the JSON the same afternoon. Nobody decided to
publish it; it published itself.

**You cannot refactor.** Rename `minutes` to `prepMinutes` because the name was poor. That is a
five-second refactor and a breaking change for every client in production.

**Your input is not your output.** A client creating a recipe must not send an `id` — you assign
it. But `Recipe` has an `id`, so `POST` accepts one, and now you must decide what to do when
somebody sends `{"id": 999}`. Ignore it silently? Reject it? The question only exists because
the class is doing both jobs.

### The fix, and it is not clever

Two records, each with one job.

```java
// what a client may send
public record CreateRecipeRequest(
        String title,
        String cuisine,
        int minutes,
        int servings,
        List<String> ingredients) { }

// what a client gets back
public record RecipeResponse(
        String id,
        String title,
        String cuisine,
        int minutes,
        int servings,
        List<String> ingredients) {

    public static RecipeResponse from(Recipe recipe) {
        return new RecipeResponse(recipe.id(), recipe.title(), recipe.cuisine(),
                recipe.minutes(), recipe.servings(), recipe.ingredients());
    }
}
```

**Where they live.** Both records go in `com.sunrise.recipeapi.dto`, a package of its own beside
`domain`, `repository` and `controller`. Not inside `controller` — these two records are the
contract with your client, and next session a service layer will be handing them around as well.
And note what the HTTP package is called: **`controller`**, because the classes in it are
controllers and the advice that serves them. A package called `web` tells you nothing that
`controller` does not.

Yes, this is duplication. It is the useful kind: the duplication is the seam between what you
store and what you promise, and it is what lets those two change at different speeds. `Recipe`
becomes free to grow fields, change names, and later become a database entity with annotations
all over it, and none of that reaches a client.

**The rule:** *the object you store and the object you send are allowed to look identical. They
are not allowed to be the same class.*

### Jackson and records

Jackson is the library that turns your objects into JSON and back. It arrived with the web
starter; you did not add it.

Going **out** is simple: Jackson reads the record's accessors and writes one JSON key per
component, in declaration order.

Coming **in** is more interesting. A record has no no-arg constructor and no setters, so Jackson
cannot make an empty object and fill it. It uses the **canonical constructor** and matches JSON
keys to parameter names. This works without any annotation because your class files carry
parameter names — that is one of the things records give you for free.

Which brings us to the question from the start of the session.

### What Jackson does with a missing field

```json
{"title": "Amok"}
```

...deserialized into a record that also has `cuisine`, `minutes`, `servings` and `ingredients`.

Jackson does not fail. It fills what it found and supplies **the type's default** for everything
else:

| Component type | Missing key becomes |
|---|---|
| `String`, `List`, any object | `null` |
| `int`, `long` | `0` |
| `boolean` | `false` |
| `Integer`, `Long`, `Boolean` | `null` |

So `new CreateRecipeRequest("Amok", null, 0, 0, null)` is what your controller receives, and
`minutes = 0` is indistinguishable from a client that genuinely sent zero.

**That is the `200 OK` from last session, explained.** Nothing failed, because nothing had been
told what "required" means. Jackson's job is to translate, not to judge.

### What about extra fields?

```json
{"title": "Amok", "colour": "green"}
```

By default Spring Boot configures Jackson to **ignore** unknown properties. Your client sends
nonsense, your API accepts it, and the client never learns it was ignored — their typo just
silently does nothing.

You can turn that off, and decide that an unknown field is a `400`. It is a real trade-off:
strictness catches client typos immediately, tolerance lets you add fields without breaking old
clients. Most public APIs choose tolerance. Most internal APIs should probably choose strictness.
The point is that it is a decision, and right now you are making it by accident.

> **Pause.** Your `RecipeResponse` has an `id`; your `CreateRecipeRequest` does not. A client
> `POST`s `{"title": "Amok", "id": "999"}`. What happens, with Jackson's defaults? Is that the
> behaviour you want?

---

## Checkpoint 3

*A supplier found the staff-only note in the app, and two phones can currently pick the same id.*

**What the client asked for**

> "Two problems. First, the staff note is showing up in the app — one of our suppliers found it.
> That has to stop today. Second, the app can currently `POST` a recipe with an `id` of its
> choosing, which means two phones can fight over the same id. Ids are yours to hand out."

**What we are doing.** Both problems have one cause: `Recipe` is what you store **and** what you
publish. We split it into three records, each with one job:

| Record | Package | Job | Has `id`? | Has `internalNotes`? |
|---|---|---|---|---|
| `CreateRecipeRequest` | `dto` | what a client may **send** | no | no |
| `RecipeResponse` | `dto` | what a client **gets back** | yes | no |
| `Recipe` | `domain` | what **you** keep | yes | yes |

**The goal.** `curl /recipes/1` no longer contains `"supplier: central market, pays late"`, a client-supplied `id` is
ignored, and **`Recipe` appears in no controller method signature** — **tests 7 and 8 green.**

**The code.** Each DTO knows how to convert itself, so the controller stays short:

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
    Recipe saved = recipeService.create(request.toRecipe());     // DTO in  -> domain
    return ResponseEntity
            .created(URI.create("/recipes/" + saved.id()))
            .body(RecipeResponse.from(saved));                   // domain  -> DTO out
}
```

The conversion happens at the edge, in the controller. Inside, it is still `Recipe`.

**Your turn.** **`curl /recipes/1` first and read the leak on your own screen.** Then write the two
records, change every controller method to use them, and `curl` again.

---

## Validation you do not write

Now close gap one. A client sends a recipe with no title. You want a `400`, and you want to say
which field.

The obvious version:

```java
if (request.title() == null || request.title().isBlank()) {
    return ResponseEntity.badRequest().body("title must not be blank");
}
if (request.cuisine() == null || request.cuisine().isBlank()) { ... }
if (request.minutes() < 1) { ... }
```

Every field, every endpoint, forever. And notice what it does to the method: eighteen lines of
checking around three lines of work, with the rules buried in a controller where nobody looking
at the type would find them.

### Put the rule on the field

Bean Validation is a Jakarta standard. Spring Boot wires it in when one starter is on the POM:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

You already have it — that was the second box you ticked at Initializr this morning. So the rules
can live on the record components, where the field is:

```java
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
        List<@NotBlank String> ingredients) { }
```

The constraints you will use constantly, all from `jakarta.validation.constraints`:

| Annotation | Holds when |
|---|---|
| `@NotNull` | the value is not `null` |
| `@NotBlank` | a `String` is not `null`, and not only whitespace |
| `@NotEmpty` | a `String`, collection or map is not `null` and not empty |
| `@Size(min=, max=)` | length or size is in range |
| `@Min` / `@Max` | a number is in range |
| `@Positive` / `@PositiveOrZero` | sign checks, more readable than `@Min(1)` |
| `@Email` | it looks like an email address |
| `@Pattern(regexp=)` | it matches a regular expression |

`@NotNull`, `@NotEmpty` and `@NotBlank` are three different questions, and picking the wrong one
is the commonest validation bug there is. `@NotNull` on a `String` accepts `""`. `@NotEmpty`
accepts `"   "`. Only `@NotBlank` means what you usually mean.

### The annotation that actually does it

Constraints on the record do nothing on their own. The trigger is `@Valid` **on the controller
parameter**:

```java
@PostMapping
ResponseEntity<RecipeResponse> create(@Valid @RequestBody CreateRecipeRequest request) {
```

Leave `@Valid` off and every constraint is silently inert. There is no warning, no startup
failure, nothing in the log — the annotations sit there looking like they are working. **When
validation "doesn't work", this is the reason nine times in ten.** Check for `@Valid` first,
always.

When a constraint fails, Spring throws `MethodArgumentNotValidException` before your method body
runs. Your controller never sees a bad object — which is the real prize. The method can assume
its input is sane, and go back to being three lines.

### What validation is not for

Bean Validation checks the **shape** of a request: present, non-blank, in range, well-formed. It
cannot check anything that requires knowing the rest of your system.

"No two recipes may share a title" needs the store. "This user has used their daily quota" needs
the user. Those are **business rules**. They live in the service layer, they throw meaningful
exceptions, and they usually deserve `409 Conflict` rather than `400`.

A useful line: **if answering the question requires a lookup, it is not validation.**

> **Pause.** Try it before the checkpoint. Add `@NotBlank` to `title`, do *not* add `@Valid`, and
> `POST` a blank title. Then add `@Valid` and send it again. The difference between those two
> runs is the thing to remember from this segment.

---

## Checkpoint 4

*Somebody submitted the form blank and it saved. A recipe with no title is now on the home page.*

**What the client asked for**

> "Somebody submitted the new-recipe form with everything blank and it saved. There is now a recipe
> with no title, no cuisine, zero minutes and no ingredients sitting on the home page. The API
> should have refused it."

**What we are doing.** Writing the client's rules down as annotations on `CreateRecipeRequest`, and
switching them on in the controller:

| Field | Rule | Annotation |
|---|---|---|
| `title`, `cuisine` | not empty, not just spaces | `@NotBlank` |
| `minutes`, `servings` | at least 1 | `@Min(1)` |
| `ingredients` | at least one | `@NotEmpty` |

**The goal.** A blank form gets **`400`**, not `201`, and nothing is saved — **tests 9 and 10
green.** You will also see the one bug everybody hits: annotations that look right and do nothing.

**The code.** The rules sit on the record components:

```java
public record CreateRecipeRequest(
        @NotBlank(message = "title must not be blank")        String title,
        @NotBlank(message = "cuisine must not be blank")      String cuisine,
        @Min(value = 1, message = "minutes must be at least 1")  int minutes,
        @Min(value = 1, message = "servings must be at least 1") int servings,
        @NotEmpty(message = "a recipe needs at least one ingredient")
        List<@NotBlank String> ingredients) {
    // toRecipe() unchanged
}
```

And one annotation on the controller parameter is what actually runs them:

```java
@PostMapping
public ResponseEntity<RecipeResponse> create(@Valid @RequestBody CreateRecipeRequest request) {
```

**Your turn.** Two runs, deliberately. **First the constraints without `@Valid`** — tests 9 and 10
fail. Then add `@Valid` and watch them pass. Then `curl -i` a blank title and look closely at the
`400` you get.

---

## One error shape everywhere

You have a `400`. Look at what came with it — a big Spring-shaped JSON object with a timestamp,
a path, a generic message, and no clear statement of *which field was wrong and why*.

And your `404` from checkpoint 2? Different shape entirely. Your `409`, when you write one, will
be a third. That is gap four: **every error in your API is currently a one-off.**

A client writing error handling against your API has to write a branch per endpoint. Multiply by
every API in a company and you get the reason there is a standard for this.

### RFC 9457 — Problem Details

One JSON shape for all HTTP errors, everywhere:

```json
{
  "type": "https://docs.sunrise.dev/errors/validation-failed",
  "title": "Validation failed",
  "status": 400,
  "detail": "The request body has 2 invalid fields",
  "instance": "/recipes"
}
```

- **`type`** — a URI identifying the *kind* of problem. This is the field a client should branch
  on: it is stable, whereas your prose is not. It is **not** an endpoint of your API and not the
  request that failed — it is a *name* for the error, like an exception class, written as a URI so
  it cannot collide with anybody else's. In a real API it links to a page documenting that error;
  `docs.sunrise.dev` is an example domain and leads nowhere. Leave it unset and Spring sends
  `about:blank`, meaning "nothing beyond the status code".
- **`title`** — a short human summary of that type.
- **`status`** — the HTTP status, repeated in the body.
- **`detail`** — what went wrong with *this specific* request.
- **`instance`** — which request it was. **`type` names the error; `instance` names the request.**

You may add your own members alongside these, which is where per-field errors go.

Spring has this as a first-class type: **`org.springframework.http.ProblemDetail`**. No
dependency, no library to pick.

### Turning it on for Spring's own errors

One property, and Spring's built-in exceptions start answering in this shape:

```properties
spring.mvc.problemdetails.enabled=true
```

That covers the errors Spring MVC raises itself — unsupported method, unreadable body,
validation failure. It does **not** cover exceptions you wrote. For those you write a handler,
and now is the moment you have earned one.

### `@RestControllerAdvice`

An advice class is a bean that catches exceptions from **all** your controllers and turns them
into responses. One place, one shape, every endpoint.

```java
package com.sunrise.recipeapi.controller;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
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

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "The request body has " + errors.size() + " invalid field(s)");
        problem.setType(URI.create("https://docs.sunrise.dev/errors/validation-failed"));
        problem.setTitle("Validation failed");
        problem.setProperty("errors", errors);
        return problem;
    }
}
```

Read the second handler again, because it is the payoff of the whole session. `getFieldErrors()`
gives you one entry per failed constraint, each knowing its field name and the `message` you
wrote on the annotation. `setProperty("errors", ...)` adds a member beside the standard five.
The client gets:

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

Every field, named, with a reason. From annotations you wrote once, on a record.

### Three things worth knowing

**`@Order` is not decoration.** The property from the last section switched on Boot's *own*
advice, and it handles `MethodArgumentNotValidException` too. Two advices can handle the same
exception and the one with the higher precedence wins. Without `@Order(Ordered.HIGHEST_PRECEDENCE)`
Boot's answers first, and the client gets `"title": "Bad Request"` with no `errors` map — your
handler is correct and never runs. Take the line out once, watch test 11 fail, put it back.

**The controller gets simpler, not more complex.** Your `GET /recipes/{id}` stops returning
`ResponseEntity` and going through an `Optional` dance. The service throws
`RecipeNotFoundException`, the controller just returns the object, and the advice turns the
exception into a `404` with a proper body, for every endpoint, forever. Three layers, three jobs:
the service **decides**, the advice **translates**, the controller **delegates**.

**There is a second way.** Spring ships `ResponseEntityExceptionHandler`, a base class your
advice can extend to inherit handling for every Spring MVC exception. It is the right choice on
a large API — but it maps `MethodArgumentNotValidException` in a `final` method, so you override
a protected hook rather than writing `@ExceptionHandler` for it. Know it exists; the plain advice
above is the one to learn first.

> **Pause.** Your advice catches `RecipeNotFoundException`. What happens today if your service
> throws a `NullPointerException` instead? What does the client see, and what should they see?

---

## Checkpoint 5

*The client is writing a `switch` per endpoint because every failure from your API looks different.
They want one error body, always the same keys, naming the field and the reason.*

**What the client asked for**

> "Every failure from your API looks different. A bad form is one shape, a missing recipe is
> another, and the next thing you add will be a third. We are writing a `switch` per endpoint. Give
> us **one** error body, always the same keys, and when it is a form problem tell us the field names
> and the messages so we can put them under the right boxes."

**What we are doing.** Moving every "this went wrong" decision into **one** class, the advice, which
answers in RFC 9457 `ProblemDetail` shape. Then three tests of your own.

**The goal.** A blank title and a missing id come back with **the same keys** — `type`, `title`,
`status`, `detail`, `instance` — and the `400` adds an `errors` map of field to message. **Test 11
green, 1–10 still green**, then **12, 13 and 14** — three decisions, written down as tests.

**The code.** One property turns the shape on for Spring's own errors:

```properties
spring.mvc.problemdetails.enabled=true
```

One advice class catches from every controller:

```java
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)       // go before Boot's own advice -- see above
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

The **service** takes its first decision — a missing recipe is an error, not an empty `Optional`:

```java
@Override
public Recipe findById(String id) {                     // RecipeServiceImpl
    return recipeRepository.findById(id)
            .orElseThrow(() -> new RecipeNotFoundException(id));
}
```

`RecipeNotFoundException` lives in the `service` package — "no such recipe" is a business fact.
Turning it into a `404` is HTTP's business, so that stays in the advice. And the controller gets
**shorter** — `one()` loses `ResponseEntity` and the `Optional` dance:

```java
@GetMapping("/{id}")
public RecipeResponse one(@PathVariable String id) {
    return RecipeResponse.from(recipeService.findById(id));
}
```

**Your turn.** Write `RecipeNotFoundException`, the advice and the new `one()`, `curl` both
failures and compare the bodies. Then copy in `YourTurnTest` from the README. **12, 13 and 14**
are given too, and each is a **decision** before it is a test: 12 is last session's question
answered, 13 decides that a second `DELETE` is `404` (and is still idempotent), 14 decides that a
`PUT` to an id nobody has is `404`, not `201`. The comments in each say what the alternative costs.
**All fourteen green.**

---

## What you now owe every API you write

Four gaps, closed:

| Gap | Closed by |
|---|---|
| Nothing said the field was required | Bean Validation constraints, and `@Valid` |
| The status code did not mean anything | `ResponseEntity`, and choosing on purpose |
| The stored object was also the wire format | A request DTO and a response DTO |
| Every failure had a different shape | `ProblemDetail` and one `@RestControllerAdvice` |

And the habits underneath them: resources are nouns, identity goes in the path, filters go in the
query string, and the method you choose is a promise to every client that will ever retry.

### What you will notice next session

Look at your controller's constructor, and at how much of that class is not the interesting part.
Then count the lines in that controller that are not about HTTP at all — the `Optional` dance, the
emptiness check, the decision about what a second `DELETE` means — and ask where those belong. And
ask yourself where you would put "log how long every endpoint takes", without editing every
endpoint.

Three questions. Next session answers all three, and then asks what has been doing the rest of
this for you the whole time.
