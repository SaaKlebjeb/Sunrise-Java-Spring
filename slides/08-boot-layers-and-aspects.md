---
layout: section
---

# Lombok, MapStruct & Aspects

Write it once. Let the machine write the rest.
---
layout: default
---

# Learning Objectives

<v-clicks>

- Say what Lombok generates, and read it before you trust it
- Put two annotation processors in one build, in the right order
- Replace hand-written mapping with a MapStruct mapper, and open the class it generated
- Decide when a mapper should stay hand-written
- Explain why the bean you injected is **not** the class you wrote
- Write an aspect, put it on the right layer, and predict when it will not fire
- Choose between a filter, an interceptor and an aspect on purpose

</v-clicks>
---
layout: section
---

# Three questions you were left with

---
layout: default
---

# One — this constructor

```java
@RestController
@RequestMapping("/recipes")
public class RecipeController {

    private final RecipeService recipeService;

    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }
}
```

<v-click>

Six lines to say *"this class needs a service"*.

</v-click>
---
layout: default
---

# Two — this mapping

```java
public Recipe toRecipe() {
    return new Recipe(null, title, cuisine, minutes, servings, ingredients, null);
}

public static RecipeResponse from(Recipe recipe) {
    return new RecipeResponse(recipe.id(), recipe.title(), recipe.cuisine(),
            recipe.minutes(), recipe.servings(), recipe.ingredients());
}
```

<v-click>

Every field, typed out, once per direction.

</v-click>

<v-click>

<div class="mt-6 text-xl">

Add a field to `Recipe` next month — and **nothing tells you** the response forgot it.

</div>

</v-click>
---
layout: default
---

# Three — "keep a record of every call"

<v-click>

You know how to do this.

</v-click>

<v-clicks>

- Open each method
- Add one line, log it
- Five endpoints, five edits

</v-clicks>

<v-click>

And again next time somebody adds an endpoint and forgets.

</v-click>

---
layout: statement
---

# Two of these are fixed at compile time

The third is fixed while the application runs

*Lombok and MapStruct write code for you. An aspect changes what a call means.*
---
layout: section
---

# The constructor you stopped writing

---
layout: default
---

# Six lines

```java
@RestController
@RequestMapping("/recipes")
public class RecipeController {

    private final RecipeService recipeService;

    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }
}
```

---
layout: default
---

# Two

```java
@RestController
@RequestMapping("/recipes")
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;
}
```

<v-click>

<div class="mt-8 text-xl">

One constructor, taking every `final` field that has not been assigned.

</div>

</v-click>

<v-click>

<div class="mt-4 text-xl">

Which is **exactly** the constructor injection you have written by hand since the container
session.

</div>

</v-click>

---
layout: statement
---

# You may skip typing something

once you can read what it generates

---
layout: default
---

# The ones worth knowing

| Annotation | Generates |
|---|---|
| `@RequiredArgsConstructor` | A constructor for every `final` field — **the one you will use** |
| `@Getter` / `@Setter` | Accessors, per field or per class |
| `@Slf4j` | `private static final Logger log = ...` |
| `@Builder` | A fluent builder |

<v-click>

<div class="mt-6 text-lg opacity-80">

And nothing else today.

</div>

</v-click>

---
layout: default
---

# `@Data` on an entity is a trap

<div class="mt-8 text-xl">

It generates `equals`, `hashCode` and `toString` across **every** field.

</div>

<v-clicks>

- On a JPA entity — which you meet next session — `hashCode` changes when the id is assigned
- `toString` triggers lazy loading
- `equals` walks relationships

</v-clicks>

<v-click>

<div class="mt-6 text-xl font-bold">

Use the small annotations deliberately. Do not reach for the one that does everything.

</div>

</v-click>

---
layout: default
---

# The error you will hit in two years

```
cannot find symbol
  symbol:   method getTitle()
```

<v-click>

<div class="mt-8 text-xl">

Lombok is an **annotation processor**, and **Java 23 stopped running those** just because they are
on the classpath.

</div>

</v-click>

<v-click>

<div class="mt-6 text-xl">

From Java 23 on it must be named in `maven-compiler-plugin`'s `annotationProcessorPaths`, or it
silently does nothing.

</div>

</v-click>

<v-click>

<div class="mt-4 text-lg opacity-80">

Your POM has it. Somebody else's will not.

</div>

</v-click>

---
layout: default
---

# Where Lombok is not the answer

<div class="mt-10 text-xl">

When the constructor is **doing** something — validating, normalising, choosing between
collaborators.

</div>

<v-click>

<div class="mt-10 text-2xl font-bold">

Lombok removes typing, not thinking.

</div>

</v-click>

---
layout: default
---

# Checkpoint 1

Lombok, in the build and in two classes.

<v-clicks>

- Add Lombok, **and** name it in `annotationProcessorPaths`
- `@RequiredArgsConstructor` on `RecipeController` and `RecipeServiceImpl`
- `mvn test` — **still green.** You changed the typing, not the behaviour

</v-clicks>

<v-click>

<div class="mt-6 text-lg opacity-80">

Then open the compiled class and find the constructor you never wrote.

</div>

</v-click>
---
layout: section
---

# The mapping you keep retyping
---
layout: default
---

# A mapper is an interface

```java
@Mapper(componentModel = "spring")
public interface RecipeMapper {

    RecipeResponse toResponse(Recipe recipe);

    List<RecipeResponse> toResponses(List<Recipe> recipes);

    Recipe toRecipe(CreateRecipeRequest request);
}
```

<v-click>

<div class="mt-6 text-xl">

No body. You write **what** you want converted. Not how.

</div>

</v-click>
---
layout: default
---

# At compile time, it writes the class

```java
@Component
public class RecipeMapperImpl implements RecipeMapper {

    @Override
    public RecipeResponse toResponse(Recipe recipe) {
        if (recipe == null) {
            return null;
        }
        // ... one local variable per field ...
        return new RecipeResponse(id, title, cuisine, minutes, servings, ingredients);
    }
}
```

<v-click>

<div class="mt-6 text-xl">

Roughly that. **Open yours** — it is under `target/generated-sources/annotations`.

</div>

</v-click>

<v-click>

<div class="mt-4 text-xl">

It is the code you would have typed. No reflection, no magic at run time.

</div>

</v-click>
---
layout: default
---

# It matches by name

<v-clicks>

- `title` on the request, `title` on the record — **mapped**
- Different names? `@Mapping(source = "minutes", target = "cookTime")`
- Records work: MapStruct calls the constructor

</v-clicks>

<v-click>

<div class="mt-8 text-xl">

Same names is the whole reason your DTOs were shaped like the domain in the first place.

</div>

</v-click>
---
layout: default
---

# The fields with no partner

`Recipe` has an `id` and `internalNotes`. `CreateRecipeRequest` has neither.

```java
@Mapping(target = "id", ignore = true)
@Mapping(target = "internalNotes", ignore = true)
Recipe toRecipe(CreateRecipeRequest request);
```

<v-click>

<div class="mt-6 text-xl">

You **say** that they are left empty — on purpose, in writing.

</div>

</v-click>
---
layout: default
---

# Make forgetting a compile error

```java
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
```

<v-click>

<div class="mt-8 text-xl">

Add a field to `Recipe` next month and the **build fails**, naming the field.

</div>

</v-click>

<v-click>

<div class="mt-6 text-xl font-bold">

The problem from the start of the session, closed by the compiler instead of by memory.

</div>

</v-click>
---
layout: default
---

# Two annotation processors, one build

```xml
<annotationProcessorPaths>
    <path> lombok                  </path>   <!-- first  -->
    <path> lombok-mapstruct-binding </path>   <!-- second -->
    <path> mapstruct-processor     </path>   <!-- last   -->
</annotationProcessorPaths>
```

<v-click>

<div class="mt-6 text-xl">

MapStruct has to see the getters and constructors Lombok generates, so Lombok runs **first**.

</div>

</v-click>

<v-click>

<div class="mt-4 text-lg opacity-80">

Get the order wrong and MapStruct reports fields that do not exist.

</div>

</v-click>
---
layout: default
---

# The mapper is a bean

<v-clicks>

- `componentModel = "spring"` puts `@Component` on the generated class
- The controller asks for `RecipeMapper` in its constructor, like any other collaborator
- `@WebMvcTest` does **not** scan plain components — so it will not find it

</v-clicks>

<v-click>

<div class="mt-6 text-xl">

The fix is one line: `@Import(RecipeMapperImpl.class)` on the test.

</div>

</v-click>
---
layout: default
---

# You can test it without Spring

```java
RecipeMapper mapper = new RecipeMapperImpl();

RecipeResponse response = mapper.toResponse(recipe);

assertThat(response.title()).isEqualTo("Fish Amok");
```

<v-click>

<div class="mt-6 text-xl">

A generated class is still just a class. `new` it.

</div>

</v-click>
---
layout: default
---

# Where a mapper is not the answer

When the conversion **decides** something — a calculation, a default, a choice between two sources.

<v-click>

<div class="mt-8 text-xl">

A mapper full of `expression = "java(...)"` is a hand-written mapper written in the wrong place.

</div>

</v-click>

<v-click>

<div class="mt-8 text-2xl font-bold">

Same rule as Lombok: it removes typing, not thinking.

</div>

</v-click>
---
layout: default
---

# Checkpoint 2

A mapper, and the two hand-written ones deleted.

<v-clicks>

- Add MapStruct **and** its processor, after Lombok
- Write `RecipeMapper`; delete `toRecipe()` and `from()`
- Run the tests. **Watch them fail** — the controller test cannot find the mapper
- Fix it with `@Import`. Then break the mapper on purpose: remove one `ignore` and read the error

</v-clicks>
---
layout: section
---

# Break — 10 minutes

---
layout: section
---

# What is aspect-oriented programming?

---
layout: default
---

# "Keep a record of every service call"

<v-clicks>

- Open `findAll`. Add one line, log it.
- Open `findById`. Do it again.
- Five methods, five edits — and a sixth one nobody remembers to touch

</v-clicks>

<v-click>

<div class="mt-8 text-xl font-bold">

The logic is identical five times. Only the method name changes.

</div>

</v-click>

---
layout: statement
---

# A cross-cutting concern

Behaviour that belongs to many places in your code at once, not to any one class

*Logging. Security checks. Caching. Retrying. Transactions.*

---
layout: default
---

# The idea

<div class="mt-10 text-xl">

Write the behaviour **once**, in one place. Tell the framework which methods it applies to.

</div>

<v-click>

<div class="mt-8 text-2xl font-bold">

Not one line of `RecipeServiceImpl` changes.

</div>

</v-click>

---
layout: default
---

# The vocabulary

| Word | What it is | For "record every service call" |
|---|---|---|
| **Join point** | A moment advice could run — always a method call | `RecipeServiceImpl.create(...)` running |
| **Pointcut** | An expression selecting join points | "every public method on `RecipeServiceImpl`" |
| **Advice** | The code that runs at a join point | Record the method name |
| **Aspect** | The class holding pointcut and advice | `ServiceCallAspect` |

---
layout: default
---

# Five kinds of advice, and one that matters

<div class="mt-6 text-xl">

`@Before` · `@After` · `@AfterReturning` · `@AfterThrowing` · **`@Around`**

</div>

<v-click>

<div class="mt-8 text-xl">

`@Around` is the only one that can see both sides, change the arguments, change the return value,
or **decide not to call the method at all**.

</div>

</v-click>

<v-click>

<div class="mt-6 text-lg opacity-80">

If `@Before` will do, use `@Before`. Know `@Around` because everything else is a special case of
it.

</div>

</v-click>

---
layout: section
---

# The aspect that does it

Start with the simplest version there is

---
layout: default
---

# One dependency, before any of this compiles

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aspectj</artifactId>
</dependency>
```

<v-click>

<div class="mt-6 text-lg opacity-80">

**`spring-boot-starter-aspectj`, not `spring-boot-starter-aop`.** Boot 4 renamed it — every
tutorial written before 2026 says `-aop`, and that name is deprecated.

</div>

</v-click>

<v-click>

<div class="mt-6 text-lg opacity-80">

**No `@EnableAspectJAutoProxy` anywhere, and that is not a mistake.** Outside Boot you would add
it yourself. Boot's auto-configuration finds AspectJ on the classpath the moment this starter is
added, and turns proxying on for you — the same reflex as every other starter.

</div>

</v-click>

---
layout: default
---

# Log a message every time a recipe is created

```java
@Aspect
@Component
@Slf4j
public class ServiceCallAspect {

    @Before("execution(* ...RecipeServiceImpl.create(..))")
    public void logCall(JoinPoint joinPoint) {
        log.info("a recipe was created");
    }
}
```

<v-click>

<div class="mt-4 text-lg opacity-80">

`@Slf4j` is Lombok again. A log line, in a method, on a class — nothing new except which method it
runs on.

</div>

</v-click>

<v-click>

<div class="mt-4 text-lg opacity-80">

No `proceed()`, no return value. `@Before` only watches — Spring runs `create()` either way.

</div>

</v-click>

---
layout: default
---

# Now widen it

<div class="mt-6 text-xl">

*"A record of every call into our own rules"* meant all of them, not just `create`. One problem.

</div>

<v-click>

<div class="mt-6 text-lg opacity-80">

**The advice type.** `@Before` can only watch. `@Around` can also decide whether the call happens
and what it returns — one form that covers everything the other four can do.

</div>

</v-click>

<v-click>

<div class="mt-8">

```java
@Around("execution(public * ...RecipeServiceImpl.*(..))")
public Object logCall(ProceedingJoinPoint joinPoint) throws Throwable {
    log.info("service:{}", joinPoint.getSignature().getName());
    return joinPoint.proceed();
}
```

</div>

</v-click>

<v-click>

<div class="mt-6 text-lg opacity-80">

Two changes: `create` becomes `*`, and `@Before`/`JoinPoint` become `@Around`/`ProceedingJoinPoint`.
The log line barely moves — `{}` is SLF4J's placeholder, filled from the method's own name instead
of a fixed string.

</div>

</v-click>

---
layout: default
---

# That is the whole thing

```java
@Aspect
@Component
@Slf4j
public class ServiceCallAspect {

    @Around("execution(public * ...RecipeServiceImpl.*(..))")
    public Object logCall(ProceedingJoinPoint joinPoint) throws Throwable {
        log.info("service:{}", joinPoint.getSignature().getName());
        return joinPoint.proceed();
    }
}
```

<v-click>

<div class="mt-4 text-lg opacity-80">

Still just `@Slf4j` and a log line — the pointcut is the only thing that changed.

</div>

</v-click>

---
layout: default
---

# Two things to get right

<v-clicks>

- `@Component` **as well as** `@Aspect` — or it compiles, deploys, and never runs (true of both versions)
- `proceed()` exactly once, and return what it returns — the price of `@Around`: it will not call the real method for you

</v-clicks>

<v-click>

<div class="mt-8 text-xl">

No `try`/`catch` here — a failure just propagates. Add one only if you want to log the failure
too, and then: catch, log, **rethrow**. Never swallow.

</div>

</v-click>

---
layout: default
---

# Reading the pointcut

```
execution(public * com.sunrise.recipeapi.service.RecipeServiceImpl.*(..))
|         |      | |                                              |  |
|         |      | |                                              |  any parameters
|         |      | |                                              any method name
|         |      | the type
|         |      any return type
|         public methods only
at the moment the method runs
```

---
layout: default
---

# The two that catch people

<v-clicks>

`*` is **one name segment**

`..` is **any number of anything**

</v-clicks>

<v-click>

<div class="mt-8 text-xl">

So `(..)` is any parameter list, and `service..*` is that package **and everything under it**.

</div>

</v-click>

<v-click>

<div class="mt-8 text-xl">

**Name your pointcuts once you have more than one.** A `@Pointcut` method with an empty body turns
a regex into a sentence.

</div>

</v-click>

---
layout: statement
---

# `@Aspect` is not magic on its own

Nothing about Java lets one class attach behaviour to another's methods

---
layout: section
---

# How Spring actually makes this happen

---
layout: default
---

# Type this into your running application

```java
System.out.println(recipeService.getClass().getName());
```

<v-click>

```
com.sunrise.recipeapi.service.RecipeServiceImpl$$SpringCGLIB$$0
```

</v-click>

<v-click>

<div class="mt-8 text-xl">

That class **did not exist when you compiled**. It was generated at startup. It extends yours.

</div>

</v-click>

<v-click>

<div class="mt-6 text-xl">

**Every method is overridden.** Your advice runs first, then the override calls `super` to reach
your real code.

</div>

</v-click>

---
layout: image
image: /session-08-aspect-proxy.svg
backgroundSize: contain
---

---
layout: default
---

# Two ways to make the wrapper

| | Wraps | Requires |
|---|---|---|
| **JDK dynamic proxy** | An interface | The bean must have one; only interface methods are advised |
| **CGLIB subclass** | The class itself | The class must not be `final`, and neither must the method |

<v-click>

<div class="mt-8 text-xl">

Boot uses CGLIB by default — **even when the class has an interface**, which is why the name above
is `RecipeServiceImpl$$SpringCGLIB`, not `RecipeService$$SpringCGLIB`.

</div>

</v-click>

<v-click>

<div class="mt-4 text-xl">

A `final` class or method cannot be wrapped at all. Spring will not tell you it skipped it.

</div>

</v-click>

---
layout: default
---

# Which layer, and this is the whole argument

<v-clicks>

**On the service** — *which of my rules ran?* A question about your application.

**On the controller** — every HTTP entry point, most of which just delegate through. Occasionally
useful, rarely what you meant.

**On the repository** — every read and write. Genuinely useful, and Spring Data gives it to you
free in two sessions.

</v-clicks>

---
layout: statement
---

# A layer you can point an aspect at cleanly

is a layer with a clean job

---
layout: default
---

# Checkpoint 3

The aspect above, added to your own project — call a few endpoints and watch the console.

<v-click>

<div class="mt-6 text-lg opacity-80">

Then print `getClass().getName()` and look at it with your own eyes.

</div>

</v-click>

---
layout: section
---

# Filter, interceptor, aspect

You are only building an aspect today — but it only makes sense once you know what it is not.

---
layout: default
---

# Filter, interceptor, aspect

| | What it is | Sees | Knows |
|---|---|---|---|
| **Filter** | A Servlet idea, older than Spring | Every request, including the ones about to 404 | Raw request and response. No idea which controller |
| **Interceptor** | A Spring MVC idea | Requests that reached the dispatcher | The handler — and still HTTP |
| **Aspect** | A Spring container idea | Calls to any bean method | Method, arguments, return value. **Nothing about HTTP** |

---
layout: statement
---

# Question one — does it need to know about HTTP at all?

<v-click>

<div class="mt-8 text-xl">

**No.** Transactions, caching, retries, audit, method-level authorisation — none of them care
whether the call came from an HTTP request, a scheduled job, or another method calling in
directly.

</div>

</v-click>

<v-click>

<div class="mt-6 text-2xl font-bold">

Use an aspect.

</div>

</v-click>

<v-click>

<div class="mt-10 text-xl opacity-80">

**Yes?** Next question.

</div>

</v-click>

---
layout: default
---

# Question two — must it run for every request?

<div class="mt-6 text-lg opacity-70">

Including the ones that never reach a controller — a 404, a request Spring rejects before
routing.

</div>

<v-click>

<div class="mt-8 text-xl">

**Yes.** Correlation ids, access logs, CORS, compression, authentication — all of it has to run
whether or not routing ever succeeds.

</div>

</v-click>

<v-click>

<div class="mt-6 text-2xl font-bold">

Use a filter.

</div>

</v-click>

<v-click>

<div class="mt-10 text-xl">

**No — only once a specific handler is chosen?** Per-endpoint timing, a feature flag on one
route.

</div>

</v-click>

<v-click>

<div class="mt-4 text-2xl font-bold">

Use an interceptor.

</div>

</v-click>

<v-click>

<div class="mt-10 text-lg opacity-80">

The whole of Spring Security is filters. Which is why the security session opens there.

</div>

</v-click>

---
layout: section
---

# What you now know that you did not

---
layout: default
---

# Before today, and now

| Before today | Now |
|---|---|
| The constructor was six lines of typing | One annotation — and you can read what it wrote |
| Mapping was every field, typed once per direction | A mapper interface, and a compile error when a field is forgotten |
| You inject the class you wrote | You inject a generated subclass that wraps it |
| Cross-cutting meant editing every method | One aspect, one pointcut, on the layer that earns it |
| Filter, interceptor and aspect looked interchangeable | Each answers "does this care about HTTP" differently |
---
layout: default
---

# What is under the API

<div class="mt-8 text-xl">

Every recipe you stored today is in a `LinkedHashMap` that empties when you stop the application.

</div>

<v-click>

<div class="mt-8 text-xl">

Next session that repository **interface** stays exactly where it is, and something else
implements it.

</div>

</v-click>

---
layout: statement
---

# Not one line of `RecipeServiceImpl` has to change

That is what the layer was for
