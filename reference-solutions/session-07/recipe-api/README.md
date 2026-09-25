# Reference solution — the Recipe API

The finished state of Session 7's exercise, and **the starting point for Session 8**.

✅ **Copied to `sessions/session-08-boot-layers-and-aspects/starting-point/recipe-api/` on 15 Sept
2026**, so that a student who fell behind can rejoin. The Recipe API runs from Session 7 to Session
13 as one project; without a per-session starting point, one bad week locks a student out of six
sessions.

## The package layout — changed 18 Sept 2026

```
com.sunrise.recipeapi
├── RecipeApiApplication
├── domain/       Recipe
├── dto/          CreateRecipeRequest, RecipeResponse
├── repository/   RecipeRepository, InMemoryRecipeRepository
├── service/      RecipeService, RecipeServiceImpl, RecipeNotFoundException
└── controller/   RecipeController, ApiExceptionHandler
```

**There is no `web` package.** The HTTP layer is `controller`; the DTOs sit in `dto`, a peer package
rather than a child of `controller`. Tests live under
`src/test/java/com/sunrise/recipeapi/controller/`.

**The service layer arrives here, in Session 7** (since 25 Sept): a `RecipeService` interface and
`RecipeServiceImpl`. It is thin by design; its only decision is "a missing recipe is an error". The
web tests `@Import(RecipeServiceImpl.class)` and still mock the repository.

## What is here

- All fourteen of the session's checks, answered — **including `YourTurnTest`** (since 25 Sept 2026
  the exercise README gives students every test, so the three decisions are in this tree too).
- `src/test` here is **identical** to `sessions/session-07-rest-apis/recipe-api/src/test`.
- **Every code block in the exercise README is generated from these files**, so the two cannot
  drift. Change a file here, regenerate the README.

## The decisions this solution made

| Question | This solution | The other defensible answer |
|---|---|---|
| Second `DELETE` of the same id | `404` | `204` every time |
| `PUT` to an id nobody has | `404` | create it, return `201` |
| Unknown JSON fields | ignored (Boot's default) | reject with `400` |

## ✅ Verified — 25 Sept 2026, Java 25.0.2, Maven 3.9.16, Boot 4.1.1

- `mvn clean test`: **14 run, 0 failures.**
- The student path was replayed from the exercise README's own code blocks, checkpoint by
  checkpoint. Green after each: CP1 1–3 · CP2 1–6 · CP3 1–8 · CP4 without `@Valid` 1–8 · CP4 with
  `@Valid` 1–10 · CP5 all 14 — exactly what the README promises.
- Every `curl` output in the exercise README was observed against the running jar.

**Two things the first real run found:**

1. **`@Order(Ordered.HIGHEST_PRECEDENCE)` on `ApiExceptionHandler` is required.**
   `spring.mvc.problemdetails.enabled=true` registers Boot's own advice, which also handles
   `MethodArgumentNotValidException` and wins without `@Order` — test 11 then fails with
   `expected:<Validation failed> but was:<Bad Request>`.
2. **The `Location` header is the relative `/recipes/1`** — test 4's assertion is right as written.

**The `.java` files were still in `web` / `web.dto` until 25 Sept** — the 18 Sept hand rename never
reached them. Moved to `controller` / `dto` in this pass, main and test.
