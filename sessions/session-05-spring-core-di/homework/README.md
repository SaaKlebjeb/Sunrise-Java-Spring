# Homework — The Catalogue Service

**Six checkpoints. A second project, generated from scratch.**

The exercise walked you through it. This does not. You get the classes, the contracts and the
expected output, and the rest is yours.

A different slice of the library, deliberately: if the exercise went badly you start this one
clean.

---

## Checkpoint 1 — generate it yourself

`start.spring.io`, again, from nothing:

| Field | Value |
|---|---|
| Project | Maven |
| Language | Java |
| Spring Boot | **4.0.8** |
| Group | `com.sunrise` |
| Artifact | `catalogue-service` |
| Package name | `com.sunrise.catalogueservice` |
| Packaging | Jar |
| Java | **25** |
| Dependencies | **none** |

Unzip it, **open the `pom.xml`**, and run `mvn test`. One test, and it passes.

**Doing this a second time is the point of the checkpoint.** It takes ninety seconds and it is
the skill that outlives everything else in this session. Do not copy the `lending-desk` folder
and rename things.

Then add a `CommandLineRunner` bean that prints one line, and run `mvn spring-boot:run` to prove
your runner is wired.

```
catalogue service up
```

---

## Checkpoint 2 — the catalogue

| Class | Package | Responsible for |
|---|---|---|
| `CatalogueEntry` | `...catalogueservice.domain` | A record: ISBN, title, author, times borrowed |
| `CatalogueRepository` | `...catalogueservice.repository` | `List<CatalogueEntry> findAll()` and `List<CatalogueEntry> findByTitleContaining(String fragment)` |
| `InMemoryCatalogueRepository` | `...catalogueservice.repository` | The implementation, seeded in a `@PostConstruct` |

Seed data, exactly:

| ISBN | Title | Author | Borrowed |
|---|---|---|---|
| `978-0132350884` | Clean Code | Robert Martin | 41 |
| `978-0134685991` | Effective Java | Joshua Bloch | 27 |
| `978-0201616224` | The Pragmatic Programmer | Andrew Hunt | 63 |
| `978-0596009205` | Head First Design Patterns | Eric Freeman | 12 |

`findByTitleContaining` is case-insensitive.

```
seeded 4 entries
=== all ===
Clean Code
Effective Java
The Pragmatic Programmer
Head First Design Patterns
```

---

## Checkpoint 3 — two rankers, and no default

| Class | Package | Responsible for |
|---|---|---|
| `SearchRanker` | `...catalogueservice.rank` | `List<CatalogueEntry> rank(List<CatalogueEntry> entries)` |
| `AlphabeticalRanker` | `...catalogueservice.rank` | Sorts by title, A→Z |
| `PopularityRanker` | `...catalogueservice.rank` | Sorts by times borrowed, most first |

Both are beans. **Neither of them is the default** — there is no `@Primary` in this project, and
adding one is not the answer.

So any class that wants *one specific ranker* has to say which, by name. The lecture named the
tool for that; it takes a bean name as its string, and the bean names here are the defaults.

Prove it from the runner: get the alphabetical one by name and print its result.

```
=== alphabetical ===
Clean Code
Effective Java
Head First Design Patterns
The Pragmatic Programmer
```

If the application will not start, read the exception. It names both candidates and it is the
single most useful message in Spring.

---

## Checkpoint 4 — all of them, in an order you control

| Class | Package | Responsible for |
|---|---|---|
| `RankerCatalogue` | `...catalogueservice.rank` | Reporting which rankers exist, in a defined order |

Contract:

```
List<String> names()      // the simple class name of each ranker, in order
```

It takes a **`List<SearchRanker>`** through its constructor. You do not build that list.

**The order must be popularity first, alphabetical second**, and you must make that true without
touching `RankerCatalogue` — the ordering belongs on the rankers themselves. There is an
annotation for it, mentioned once in the lecture, and lower numbers come first.

```
=== rankers ===
PopularityRanker
AlphabeticalRanker
```

**Before you add the annotation, run it once without.** Whatever order you get is not guaranteed
— it happens to be stable on your machine today. That is the bug the annotation prevents, and
seeing the unordered version first is worth the extra run.

---

## Checkpoint 5 — a class from a jar

| Class | Package | Responsible for |
|---|---|---|
| `SlugGenerator` | `...catalogueservice.thirdparty` | Turning a title into a URL slug |
| `CatalogueConfig` | `...catalogueservice.config` | Registering it |

Contract:

```
SlugGenerator(String separator)     // a constructor argument
String slugify(String title)        // lower-case, non-letters become the separator, no repeats
```

Same rule as the exercise: **`SlugGenerator` may not carry a single annotation.** Register it
with a `@Bean` method, built with the separator `-`.

Prove it from the runner:

```
=== slugs ===
clean-code
effective-java
the-pragmatic-programmer
head-first-design-patterns
```

---

## Checkpoint 6 — put it together

| Class | Package | Responsible for |
|---|---|---|
| `SearchService` | `...catalogueservice.search` | Searching the catalogue and returning ranked, slugged results |

Contract:

```
List<String> search(String fragment)    // "<slug> (borrowed <n> times)", most popular first
```

It takes the repository, the popularity ranker **by name**, and the slug generator — all through
one constructor, into `final` fields. No `@Autowired`. No `new`.

```
=== search: "a" ===
the-pragmatic-programmer (borrowed 63 times)
clean-code (borrowed 41 times)
effective-java (borrowed 27 times)
head-first-design-patterns (borrowed 12 times)
```

All four titles contain an `a`. Search for `design` and you should get one line.

---

## What "done" looks like

All six blocks in order, plus `mvn test` green, plus no `new` in `src/main/java` outside the
`@Bean` method and the records.

---

## Three investigations — ungraded, and the part worth doing

**1. Read a real `NoUniqueBeanDefinitionException`.**
Add `@Primary` to *both* rankers and start the application. Then remove both and ask for a bare
`SearchRanker` somewhere. Two different failures, two different messages. Write down the first
line of each and what it told you.

**2. Break the root package rule and diagnose it from the failure alone.**
Move `CatalogueServiceApplication` from `com.sunrise.catalogueservice` down into
`com.sunrise.catalogueservice.app` — the class, nothing else. Start it. Now work out, **from the
message only**, what went missing and why. Then explain in one sentence why moving the
*application class* broke beans you never touched.

**3. Find the container.**
Run `mvn dependency:tree`. Find `spring-context` in the output and note how many levels down it
is. Then count the jars a project with *no dependencies ticked* actually pulls in. That number
is the honest answer to "what does a starter do".

---

## A note on where this goes

Keep both projects. The next session builds directly on `lending-desk` — you will add an aspect
to the service you wired, and the first thing it explains is what was intercepting that `@Bean`
method call.
