# Exercise — Give the Lending Desk a Front Door

You are not generating anything today. Open the **`lending-desk`** project you built in the
container session — **open the `pom.xml`, not the folder** — and keep working in it.

Right now it is a console application. It has a container full of beans, a `CommandLineRunner`
that prints, and no way for anything outside the JVM to reach it. By the end of today it answers
on a port.

**Java 25 · Spring Boot 4.0.8 · Maven.**

---

## First, copy the tests in

Copy the whole `src/test` tree from **this folder** into your own project, so that you end up
with:

```
lending-desk/
  src/
    main/java/com/sunrise/lendingdesk/...     <- yours, from the container session
    test/java/com/sunrise/lendingdesk/web/
        LendingControllerWebTest.java          <- 7 tests, given
        YourTurnTest.java                      <- 2 tests, YOU write these
```

The tests need one dependency your project does not have yet. Add it to the POM now — `test`
scope, **no `<version>`**:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc-test</artifactId>
    <scope>test</scope>
</dependency>
```

`@WebMvcTest` lives there, not in the `spring-boot-starter-test` you already have. On Boot 4 the
web layer's test support is its own starter.

**Nine tests.** Seven are written for you and must end up green. Two are
`fail("TODO: write this test")` skeletons and checkpoint 4 is where you write them.

Run them with `mvn test`. **Right now every one of them fails, and most will not even compile**,
because none of the classes they refer to exist yet. That is expected. The compiler errors are
your specification: they name the classes and methods you are about to build.

### Pace

The checkpoints are released one at a time, each straight after the part of the session that
teaches it. **Aim to finish checkpoint 3 in the room, and to have started 4.** The rest of 4 is
for the rest of the day. That is the plan, not a fallback.

### When you get stuck on Java

Each checkpoint ends with a collapsed **Stuck?** block holding the complete code. Open it when
you are stuck on **Java**, not when you are stuck on **Spring** — the Spring part is the lesson,
and reading the answer to it costs you the session.

---

## Checkpoint 1 — give it a web face

One line of XML, and your application stops being a console program.

Add **one starter** to the POM — `spring-boot-starter-webmvc`, a `groupId` and an `artifactId`,
**no `<version>`**, because the parent owns that. Exactly the way you were taught to read the
generated POM in the container session.

Then, before you write any code:

```
mvn dependency:tree
```

**Find two things in that output and write them down:** an embedded server, and a JSON library.
You asked for neither by name. One starter, and both arrived.

Now run it:

```
mvn spring-boot:run
```

Your `CommandLineRunner` still prints what it always printed — and then **the application does
not exit.** It is listening on 8080. Nobody configured a port, a server, or a thread pool.

### Then one endpoint

| Class | Package | Responsible for |
|---|---|---|
| `LendingController` | `...lendingdesk.web` | Answering HTTP requests about books |

| Verb and path | Returns |
|---|---|
| `GET /books` | every book on the shelf, from `BookRepository#findAll` |

It is a `@RestController`. It takes `BookRepository` as a constructor parameter and a `final`
field — the same way every collaborator has been injected since the container session. Nothing
new about the wiring. The only new thing is the annotation on the method.

### Expected output

```
$ curl localhost:8080/books
[{"isbn":"978-0132350884","title":"Clean Code","copies":3},
 {"isbn":"978-0134685991","title":"Effective Java","copies":2},
 {"isbn":"978-0201616224","title":"The Pragmatic Programmer","copies":1}]
```

**Note what you did not write.** No JSON. No serialisation. No status code. You returned a
`List<Book>` and something turned it into that.

### Check yourself

```
mvn test -Dtest=LendingControllerWebTest#listsEveryBook
```

<details>
<summary><strong>Stuck? Here is the code for checkpoint 1</strong></summary>

`pom.xml` — inside `<dependencies>`

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc</artifactId>
</dependency>
```

`web/LendingController.java`

```java
package com.sunrise.lendingdesk.web;

import com.sunrise.lendingdesk.domain.Book;
import com.sunrise.lendingdesk.repository.BookRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class LendingController {

    private final BookRepository books;

    public LendingController(BookRepository books) {
        this.books = books;
    }

    @GetMapping("/books")
    public List<Book> all() {
        return books.findAll();
    }
}
```

</details>

---

## Checkpoint 2 — three ways in

A URL carries data in two different places, and they do not mean the same thing.

| Verb and path | Returns |
|---|---|
| `GET /books` | every book — **must still work with no parameter at all** |
| `GET /books?available=true` | only the books with copies left |
| `GET /books/{isbn}` | one book, or **404** |

Three things to get right:

- The **available** flag is a `@RequestParam` with a **default of `false`**. A `@RequestParam`
  with no default is mandatory, and `GET /books` would start returning 400.
- The **isbn** is a `@PathVariable`. The name in the braces and your parameter name must match.
- **404 is not an `if`.** `findByIsbn` already returns `Optional<Book>`, and
  `ResponseEntity.of(...)` turns that straight into a 200-with-body or an empty 404. One line,
  no branch.

### Expected output

```
$ curl localhost:8080/books/978-0132350884
{"isbn":"978-0132350884","title":"Clean Code","copies":3}

$ curl -i localhost:8080/books/nonsense
HTTP/1.1 404
```

### Check yourself

```
mvn test -Dtest=LendingControllerWebTest
```

Tests 1 to 4 green. Tests 5, 6 and 7 stay red — you have not built `POST /loans` yet.

<details>
<summary><strong>Stuck? Here is the code for checkpoint 2</strong></summary>

`web/LendingController.java` — the changes only

```java
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

    @GetMapping("/books")
    public List<Book> all(@RequestParam(defaultValue = "false") boolean available) {
        List<Book> everything = books.findAll();
        if (!available) {
            return everything;
        }
        return everything.stream()
                         .filter(book -> book.copies() > 0)
                         .toList();
    }

    @GetMapping("/books/{isbn}")
    public ResponseEntity<Book> one(@PathVariable String isbn) {
        return ResponseEntity.of(books.findByIsbn(isbn));
    }
```

</details>

---

## Checkpoint 3 — send something in

So far everything has travelled outward. Now something arrives in the body.

| Class | Package | Responsible for |
|---|---|---|
| `LoanRequest` | `...lendingdesk.web` | What the client sends to borrow a book. A record: isbn, member |

| Verb and path | Takes | Returns |
|---|---|---|
| `POST /loans` | a `LoanRequest` in the body | the `LendingResult` from `LendingService#lend` |

The controller now needs `LendingService` as well as `BookRepository`. Add it as a constructor
parameter and a `final` field. **Not a second constructor, and not a field annotation.**

*Yes, that is a lot of constructor for two fields. There is a tool that generates it, and you meet
it in the Spring Boot session — after this one, on purpose.*

The parameter carrying the body needs `@RequestBody`. Leave it off and Spring assumes the
parameter is a request parameter instead, and hands you an object full of `null`s rather than an
error — which is worse, because it looks like it worked.

### Expected output

```
$ curl -X POST localhost:8080/loans \
       -H 'Content-Type: application/json' \
       -d '{"isbn":"978-0201616224","member":"sokha"}'
{"ok":true,"message":"lent to sokha"}

$ curl -X POST localhost:8080/loans \
       -H 'Content-Type: application/json' \
       -d '{"isbn":"978-0201616224","member":"sokha"}'
{"ok":false,"message":"no copies left"}
```

The shelf seeded that title with one copy. The second call is your `LendingService` from the
container session, unchanged, answering over HTTP.

### Then break it on purpose

**Send the same request with no `Content-Type` header.** Read the status you get, and find it in
the lifecycle table in the lecture. Your controller method never ran — nothing could read the
body, so nothing got as far as calling you.

### Check yourself

```
mvn test -Dtest=LendingControllerWebTest
```

All seven green.

<details>
<summary><strong>Stuck? Here is the code for checkpoint 3</strong></summary>

`web/LoanRequest.java`

```java
package com.sunrise.lendingdesk.web;

public record LoanRequest(String isbn, String member) { }
```

`web/LendingController.java` — the changes only

```java
import com.sunrise.lendingdesk.domain.LendingResult;
import com.sunrise.lendingdesk.service.LendingService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

    private final BookRepository books;
    private final LendingService lending;

    public LendingController(BookRepository books, LendingService lending) {
        this.books = books;
        this.lending = lending;
    }

    @PostMapping("/loans")
    public LendingResult lend(@RequestBody LoanRequest request) {
        return lending.lend(request.isbn(), request.member());
    }
```

</details>

---

## Checkpoint 4 — write the last two tests

Open `YourTurnTest`. Two methods, each one `fail("TODO: write this test")`. Delete the line and
write the test.

The class is already set up: a `MockMvc`, and `@MockitoBean` mocks for both collaborators. You are
not wiring anything, only writing two `mockMvc.perform(...)` calls and deciding what to assert.

**Test 8 — `POST /loans` with the member missing from the body.** Send
`{"isbn": "978-0132350884"}` and nothing else. Decide what you think should happen, assert it,
then look at what Spring actually does. If your assertion was wrong, do not change it until you
can explain the real behaviour — that gap is the whole of the next two sessions.

**Test 9 — `GET /books` when the shelf is empty.** Stub `findAll()` to return an empty list.
Assert the status, and assert the body is an empty JSON array. Empty is not missing, and an API
that 404s an empty collection makes every client that uses it worse forever.

### Check yourself

```
mvn test
```

**Nine green.** That is the session.

---

## If you get stuck

| What you see | Build error or code error | What it means |
|---|---|---|
| `package org.springframework.web.bind.annotation does not exist` | build | `spring-boot-starter-webmvc` is not on the POM, or you added a `<version>` that does not resolve |
| The app prints and then **exits** | code | No web starter. Boot has nothing to keep alive, so it runs the `CommandLineRunner` and stops |
| `Port 8080 was already in use` | code | An earlier `mvn spring-boot:run` is still going. Stop it, or set `server.port` |
| `package org.springframework.boot.webmvc.test.autoconfigure does not exist` | build | `spring-boot-starter-webmvc-test` is missing. Add it at `test` scope — see *First, copy the tests in* |
| `package org.springframework.boot.test.autoconfigure.web.servlet does not exist` | build | That import is from a Boot 3 tutorial. On Boot 4 it is `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest` |
| `cannot find symbol: class MockitoBean` | build | You wrote `@MockBean`. It was removed in Boot 4. It is `@MockitoBean`, from `org.springframework.test.context.bean.override.mockito` |
| `404` on a path you are sure you wrote | code | The mapping table has no row for that verb and path. Check the verb, then the spelling. Your breakpoint will never hit — the method was never a candidate |
| `400` naming a parameter | code | An argument resolver could not produce a value. A `@RequestParam` with no default is mandatory |
| `415 Unsupported Media Type` | code | You POSTed without `Content-Type: application/json`, so no converter would read the body. In checkpoint 3 this is the correct answer |
| A `LoanRequest` full of `null`s, no error | code | You left `@RequestBody` off. Spring took the parameter for a request parameter instead |
| `IllegalStateException: Ambiguous mapping` | code | Two methods claim the same verb and path. `/books/{isbn}` and `/books/{id}` are the same path |
| `mvn test` runs zero tests | build | You copied `src/test` to the wrong depth. It is `src/test/java/com/sunrise/lendingdesk/web/` |

---

## What you did not do today

You wrote endpoints, but you did not design an API. Which verb belongs on which operation, which
status code means what, whether `LoanRequest` should be the same shape as what you store, what an
error body should look like, and what happens when the input is nonsense — all of that is the
next module, and none of it is guesswork.

Test 8 is the one that points at it. **Hold on to your answer** — the next session opens on it.
