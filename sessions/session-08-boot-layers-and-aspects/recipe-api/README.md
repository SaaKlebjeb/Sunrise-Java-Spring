# Exercise — The Recipe API, generated for you

Same project as last session. You do not generate anything today: you open what you already have
and let two tools write code you used to type by hand — then write one piece of code that changes
what a method call *means*.

**Java 25 · Spring Boot 4.1.1 · Maven.**

---

## Who you are building for today

**Sunrise Kitchen** again. One new thing from the client this session: *"we want a record of
every call into our own rules, not the network."* It does not change what a client sees on a
happy path. It is about code you already have.

## Start here

**Copy `../starting-point/recipe-api/` and work in that.** It is last session's reference
solution — fourteen green tests, five endpoints, DTOs, validation, the service layer and the error
advice all in place. Everything you add today goes into that copy; nothing else is provided for
you to copy in from disk. Every piece of code this exercise gives you — the aspects, the config
classes, the tests — is printed in full below, at the checkpoint where you need it. Type or paste
it into your own copy of the project.

If you already have your own finished project from last session and would rather keep working in
it, that is fine — run `mvn test` first and confirm fourteen green before anything else.

**Do this without embarrassment and without falling behind.** From last session every exercise
works this way for exactly this reason.

---

## What today's sixteen checks are, and when they go green

| File | Checks | Green by |
|---|---|---|
| your 14 tests from last session | 1–14 | already green |
| `RecipeMapperTest` | 15, 16 | checkpoint 2 |

### Pace

Three checkpoints in three hours — there is room to slow down on checkpoint 2, which is the one
with a deliberate red-then-green step.

---

## Checkpoint 1 — Lombok, and the constructor you stop writing

**Nothing about your behaviour changes here. Tests 1–14 must still be green at the end.**

Add both dependencies to `pom.xml`:

```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```

and, because Lombok needs the annotation processor named explicitly from Java 23 on:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <configuration>
                <annotationProcessorPaths>
                    <path>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                        <version>${lombok.version}</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
        <!-- spring-boot-maven-plugin stays where it is -->
    </plugins>
</build>
```

Leave that plugin block out and Lombok compiles, runs, generates nothing, and every constructor it
was supposed to write comes back as `cannot find symbol`. It is the commonest Lombok problem there
is, and the error points nowhere near the cause.

Then replace the hand-written constructors:

```java
@RestController
@RequestMapping("/recipes")
@RequiredArgsConstructor
public class RecipeController {
    private final RecipeService recipeService;
    // no constructor
}
```

Do the same in `RecipeServiceImpl`. Leave `InMemoryRecipeRepository` alone — you do not have to use
a tool everywhere just because it is on the classpath.

`mvn test` — **fourteen green.** You changed the typing, not the behaviour.

---

## Checkpoint 2 — MapStruct, and the mapping you stop writing

**What the client asked for:** nothing new — this is the same contract as last session. What
changes is who writes the conversion.

Look at `CreateRecipeRequest.toRecipe()` and `RecipeResponse.from()`. Every field, typed out, once
per direction. Add a field to `Recipe` next month and nothing tells you the response forgot it.

### Add the dependency and the second processor

```xml
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>${mapstruct.version}</version>
</dependency>
```

Add `${mapstruct.version}` to `<properties>` (`1.6.3`), and add **two more** paths to the
`annotationProcessorPaths` block from checkpoint 1 — **in this order**, Lombok first:

```xml
<path>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok-mapstruct-binding</artifactId>
    <version>0.2.0</version>
</path>
<path>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct-processor</artifactId>
    <version>${mapstruct.version}</version>
</path>
```

MapStruct has to see the getters and constructors Lombok generates, so Lombok's processor has to
run first. Get the order wrong and MapStruct reports fields that do not exist.

### Write the mapper

A new package, `mapper/`:

```java
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface RecipeMapper {

    RecipeResponse toResponse(Recipe recipe);

    List<RecipeResponse> toResponses(List<Recipe> recipes);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "internalNotes", ignore = true)
    Recipe toRecipe(CreateRecipeRequest request);
}
```

`id` and `internalNotes` have no partner on `CreateRecipeRequest` — say in writing that they are
left empty. `unmappedTargetPolicy = ReportingPolicy.ERROR` is what turns *"somebody added a field
and forgot the mapping"* into a build failure instead of a silent gap.

**Delete `toRecipe()` from `CreateRecipeRequest` and `from()` from `RecipeResponse`.** Inject
`RecipeMapper` into `RecipeController` and replace every call site.

### Run the tests. Watch some of them fail.

`@WebMvcTest` does not load plain `@Component` beans, and `componentModel = "spring"` is what makes
the generated `RecipeMapperImpl` one. Add it to each controller test's `@Import`:

```java
@Import({RecipeServiceImpl.class, RecipeMapperImpl.class})
```

`RecipeMapperImpl` does not exist in your editor until you build once — it is generated under
`target/generated-sources/annotations`. Open it and read it; it is the code you would have typed.

### Add this test

No Spring in it at all — `new RecipeMapperImpl()`. A generated class is still just a class. Create
`RecipeMapperTest` in `src/test/java/com/sunrise/recipeapi/mapper/`:

```java
package com.sunrise.recipeapi.mapper;

import com.sunrise.recipeapi.domain.Recipe;
import com.sunrise.recipeapi.dto.CreateRecipeRequest;
import com.sunrise.recipeapi.dto.RecipeResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * No Spring anywhere in this file. The mapper is a generated class, and a generated
 * class is still just a class: new it, call it, assert on what came back.
 */
@DisplayName("The generated mapper, without a container")
class RecipeMapperTest {

    private final RecipeMapper mapper = new RecipeMapperImpl();

    @Test
    @DisplayName("15. a recipe becomes a response with every field copied and internalNotes left behind")
    void responseCarriesTheFieldsAndNotTheNotes() {
        Recipe stored = new Recipe("1", "Fish Amok", "khmer", 45, 4,
                List.of("fish", "coconut milk"), "supplier pays late");

        RecipeResponse response = mapper.toResponse(stored);

        assertThat(response).isEqualTo(new RecipeResponse(
                "1", "Fish Amok", "khmer", 45, 4, List.of("fish", "coconut milk")));
    }

    @Test
    @DisplayName("16. a request becomes a recipe with no id and no internalNotes -- the two the client may not choose")
    void requestLeavesTheServerOwnedFieldsEmpty() {
        CreateRecipeRequest request = new CreateRecipeRequest(
                "Lok Lak", "khmer", 25, 2, List.of("beef", "lettuce"));

        Recipe recipe = mapper.toRecipe(request);

        assertThat(recipe.id()).isNull();
        assertThat(recipe.internalNotes()).isNull();
        assertThat(recipe.title()).isEqualTo("Lok Lak");
        assertThat(recipe.ingredients()).containsExactly("beef", "lettuce");
    }
}
```

`mvn test` — **sixteen green.**

### Now break it on purpose

Remove the `@Mapping(target = "internalNotes", ignore = true)` line and run `mvn compile`. Read the
error. Put the line back.

---

## Checkpoint 3 — one aspect, recording your own rules

**What the client asked for:** *"we want a record of every call into our own rules, not the
network."*

### Add the starter first

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aspectj</artifactId>
</dependency>
```

> ### ⚠️ `spring-boot-starter-aspectj`, not `spring-boot-starter-aop`
>
> Boot 4 renamed it. Every tutorial written before 2026 says `-aop`, and that name is deprecated
> and going away.

**No `@EnableAspectJAutoProxy` anywhere, and that is not a mistake.** Outside Boot you would add
it to a `@Configuration` class yourself. Boot's auto-configuration finds AspectJ on the classpath
the moment you add this starter and turns proxying on for you — the same reflex you already know
from starters wiring things up without a line of your own config.

### The aspect

A new package, `aspect/`:

```java
@Aspect
@Component
@Slf4j
public class ServiceCallAspect {

    @Around("execution(public * com.sunrise.recipeapi.service.RecipeServiceImpl.*(..))")
    public Object logCall(ProceedingJoinPoint joinPoint) throws Throwable {
        log.info("service:{}", joinPoint.getSignature().getName());
        return joinPoint.proceed();
    }
}
```

That is the whole thing. `@Slf4j` is Lombok again, from checkpoint 1 — the `log` field you did not
have to type. Two things worth noticing, not just typing past:

- **`@Component` as well as `@Aspect`.** `@Aspect` is an AspectJ annotation and Spring is not
  AspectJ. Leave `@Component` off and this compiles, deploys, and never runs.
- **`proceed()` exactly once**, and return what it returns. Skip it and the real method never runs
  at all.

**There is no `try`/`catch` here.** If the real method throws, the exception just propagates — you
do not have to do anything for that to happen correctly. You would only add a `catch` if you
wanted to log the failure too, and then the rule is: catch, log, **rethrow**. An aspect that
swallows a failure is worse than no aspect.

### Your turn

`mvn test` — **still sixteen green.** You added behaviour that nothing asserts on yet. Then, while
your application is running, type:

```java
System.out.println(recipeService.getClass().getName());
```

Read what comes back. It is not `RecipeServiceImpl`. Call a couple of endpoints and watch the
console — one log line per service method call, in the order they ran.

---

## If you get stuck

**`cannot find symbol: method getTitle()` after adding Lombok** — the `annotationProcessorPaths`
block. Checkpoint 1.

**MapStruct says a field is unmapped** — `unmappedTargetPolicy = ReportingPolicy.ERROR` means every
target field needs a source or an explicit `ignore = true`. The error names the field.

**`@WebMvcTest` cannot find `RecipeMapperImpl`** — it is generated at compile time. Run `mvn compile`
once, then check `@Import`.

**Your aspect never fires** — in this order: is `@Component` on it; is the pointcut's package and
class name spelled right; is the call coming from another bean rather than from the object itself.

**Tests fail to compile after checkpoint 2** — they now import `RecipeMapperImpl`. If you have not
built once yet, that class does not exist.

**`APPLICATION FAILED TO START` mentioning a constructor** — a class needs a bean that does not
exist. Read the "Description" paragraph in the failure block; Boot writes it in English and names
the class.
