---
layout: section
---

# Spring MVC

What happens between the port and your method

---
layout: default
---

# Learning Objectives

<v-clicks>

- Add one starter and say what it brought in, and what it **started**
- Trace an HTTP request through the `DispatcherServlet` and name every hand-off
- Distinguish handler mapping, handler adapter, argument resolver and message converter
- Explain what `@RestController` does that `@Controller` does not
- Say what thread-per-request means: one request, one thread, start to finish
- Test a web layer with `MockMvc`, and say what `@WebMvcTest` does and does not load

</v-clicks>

---
layout: default
---

# What you built last session

<div class="grid grid-cols-2 gap-6 mt-8">

<div>

## You have

A container full of beans

A shelf, a lending service, two senders

An audit log, a ticket desk

A passing `mvn test`

</div>

<div>

<v-click>

## You do not have

Any way for anything outside that JVM

to reach **a single one of them**

</v-click>

</div>

</div>

---
layout: statement
---

# Five sessions of Java

with no front door.

---
layout: default
---

# Today

<v-clicks>

- You add one
- Then you spend two hours finding out what happened when you did

</v-clicks>

<v-click>

<div class="mt-8 border-2 border-green-500/50 rounded-lg px-6 py-4 bg-green-500/10">

Open the `lending-desk` project you built last session. **Open the `pom.xml`, not the folder.**

</div>

</v-click>

---
layout: section
---

# Give it a web face

---
layout: default
---

# One line of XML

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc</artifactId>
</dependency>
```

<v-click>

No `<version>`.

</v-click>

<v-click>

You know why by now — the parent POM owns versions. That was the point of reading the generated POM last session.

</v-click>

---
layout: default
---

# What came with it

```
mvn dependency:tree
```

<v-clicks>

- **An embedded Tomcat** — a servlet container, inside your application, as a library
- **Jackson** — a JSON library

</v-clicks>

<v-click>

You asked for neither by name.

</v-click>

<v-click>

One starter. One line. Both arrived, with everything they need.

</v-click>

---
layout: default
---

# And now it does not stop

```
mvn spring-boot:run
```

<v-clicks>

- Your `CommandLineRunner` prints exactly what it printed yesterday
- And the process **stays alive**
- It is listening on port 8080

</v-clicks>

---
layout: default
---

# What you did not configure

<v-clicks>

- A port
- A server
- A connector
- A thread pool
- A shutdown hook
- A `main` loop

</v-clicks>

<v-click>

<div class="mt-6 border-2 border-blue-500/50 rounded-lg px-6 py-4 bg-blue-500/10">

The jar on the classpath was enough.

</div>

</v-click>

---
layout: statement
---

# A class is present on the classpath,

therefore a default is configured.

You have seen this twice already.

---
layout: default
---

# One endpoint

```java
package com.sunrise.lendingdesk.web;

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

---
layout: default
---

# Almost none of that is new

<v-clicks>

- A constructor parameter and a `final` field — every collaborator since last session
- `@RestController` is a stereotype, so it is a `@Component`
- The container registers it by **scanning**, under the root package rule you already know

</v-clicks>

<v-click>

<div class="mt-8 border-2 border-green-500/50 rounded-lg px-6 py-4 bg-green-500/10">

The only genuinely new thing on this slide is `@GetMapping`.

</div>

</v-click>

---
layout: default
---

# Ask it for the books

```
$ curl localhost:8080/books
[{"isbn":"978-0132350884","title":"Clean Code","copies":3},
 {"isbn":"978-0134685991","title":"Effective Java","copies":2},
 {"isbn":"978-0201616224","title":"The Pragmatic Programmer","copies":1}]
```

---
layout: default
---

# Look at what you did not write

<v-clicks>

- You did not write JSON
- You did not serialise anything
- You did not set a status code
- You did not set a content type

</v-clicks>

<v-click>

You returned a `List<Book>` from an ordinary method, and something in between turned it into that.

</v-click>

---
layout: statement
---

# Write it down now

Three or four steps, your own words, no looking.

What happened between `GET /books` and `all()`?

**Keep the paper.**

---
layout: section
---

# Do it

## Give it a web face

---
layout: default
---

# Checkpoint 1

<v-clicks>

1. One starter on the POM, no version
2. `mvn dependency:tree` — find the server and the JSON library
3. `mvn spring-boot:run` — watch it not exit
4. One `@RestController`, one `@GetMapping("/books")`

</v-clicks>

<v-click>

Then `curl localhost:8080/books` and read your own JSON.

</v-click>

---
layout: section
---

# One servlet in front of everything

---
layout: default
---

# The old way

<v-clicks>

- A class extending `HttpServlet`
- `doGet`, `doPost`
- Mapped to a URL pattern
- One servlet per URL, or per family of URLs
- A large application meant a large pile of them

</v-clicks>

<v-click>

You are not going back there. But the **shape** matters.

</v-click>

---
layout: default
---

# The same endpoint, three ways

````md magic-move {lines: false}
```java
// Raw servlet, no Spring
@WebServlet("/books")
public class BookServlet extends HttpServlet {

    private final BookRepository books = new InMemoryBookRepository();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        resp.setContentType("application/json");
        resp.setStatus(HttpServletResponse.SC_OK);
        StringBuilder json = new StringBuilder("[");
        for (Book b : books.findAll()) {
            if (json.length() > 1) json.append(',');
            json.append("{\"isbn\":\"").append(b.isbn())
                .append("\",\"title\":\"").append(b.title())
                .append("\",\"copies\":").append(b.copies()).append('}');
        }
        json.append(']');
        resp.getWriter().write(json.toString());
    }
}
```
```java
// Spring MVC before Boot, around 2010
@Controller
@RequestMapping("/books")
public class LendingController {

    private final BookRepository books;

    @Autowired
    public LendingController(BookRepository books) {
        this.books = books;
    }

    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public List<Book> all() {
        return books.findAll();
    }
}
```
```java
// Spring Boot 4: what you just wrote
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
````

<v-click>

Every job the servlet did by hand still gets done. **Somebody else does it.**

</v-click>

---
layout: statement
---

# Spring MVC did not replace the servlet.

It put **one** in front of everything

and made that one a dispatcher.

---
layout: default
---

# `DispatcherServlet`

<v-clicks>

- Every request to your application arrives at the **same servlet**
- `/books`, `/loans`, `/anything` — all of it
- It handles **none** of them itself

</v-clicks>

<v-click>

Its entire job:

1. Work out who *should* handle this
2. Arrange for that to happen
3. Turn what comes back into an HTTP response

</v-click>

---
layout: default
---

# This has a name

<div class="mt-8 text-center">

<div class="text-3xl font-bold">Front Controller</div>

<div class="mt-4 opacity-80">One entry point · central routing · per-request work delegated outward</div>

</div>

<v-click>

<div class="mt-10 border-2 border-blue-500/50 rounded-lg px-6 py-4 bg-blue-500/10">

Because every request passes through one object, anything that must happen for **every** request has exactly one place to live.

</div>

</v-click>

---
layout: default
---

# What that buys you

<v-clicks>

- Exception handling
- Content negotiation
- Locale resolution

</v-clicks>

<v-click>

You will use all three without writing a line of dispatch code.

</v-click>

<v-click>

The single servlet is what makes that possible.

</v-click>

---
layout: default
---

# You never registered it

<v-clicks>

- No `web.xml` in your project
- No line in any configuration class that mentions it
- Search your own source for the word — it is not there

</v-clicks>

<v-click>

Auto-configuration saw Tomcat, saw `DispatcherServlet`, and registered one against `/`.

</v-click>

---
layout: default
---

# What it used to take

````md magic-move {lines: false}
```xml
<!-- Spring MVC before Boot: WEB-INF/web.xml -->
<servlet>
  <servlet-name>dispatcher</servlet-name>
  <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>
  <init-param>
    <param-name>contextConfigLocation</param-name>
    <param-value>/WEB-INF/dispatcher-servlet.xml</param-value>
  </init-param>
  <load-on-startup>1</load-on-startup>
</servlet>
<servlet-mapping>
  <servlet-name>dispatcher</servlet-name>
  <url-pattern>/</url-pattern>
</servlet-mapping>

<!-- ...and WEB-INF/dispatcher-servlet.xml -->
<beans>
  <context:component-scan base-package="com.sunrise.lendingdesk"/>
  <mvc:annotation-driven/>
</beans>
```
```java
// Spring 3.1 onward: the same thing in Java, still yours to write
public class WebAppInitializer
        extends AbstractAnnotationConfigDispatcherServletInitializer {
    protected Class<?>[] getRootConfigClasses()    { return null; }
    protected Class<?>[] getServletConfigClasses() { return new Class<?>[] { WebConfig.class }; }
    protected String[]   getServletMappings()      { return new String[] { "/" }; }
}

@Configuration
@EnableWebMvc
@ComponentScan("com.sunrise.lendingdesk")
public class WebConfig { }
```
```xml
<!-- Spring Boot 4: pom.xml. That is all. -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-webmvc</artifactId>
</dependency>
```
````

<v-click>

Plus a WAR, a Tomcat you installed yourself, and a deploy step. **Every line of it still runs in your application.**

</v-click>

---
layout: statement
---

# The central object of the entire web framework

is something you did not name,

did not configure,

and cannot find in your own code.

---
layout: section
---

# How a URL finds your method

---
layout: default
---

# The handler mapping

<v-click>

`DispatcherServlet` has a request. It needs a method.

</v-click>

<v-clicks>

At **startup**, Spring:

- scans every `@Controller` and `@RestController` bean
- reads every `@RequestMapping` on them
- builds a table

</v-clicks>

---
layout: default
---

# The table

| Method | Path | Handler |
|---|---|---|
| `GET` | `/books` | `LendingController#all` |
| `GET` | `/books/{isbn}` | `LendingController#one` |
| `POST` | `/loans` | `LendingController#lend` |

<v-click>

Built **once**, at startup. Not per request. Not by reflection each time.

</v-click>

<v-click>

When a request arrives, this is a **lookup**.

</v-click>

---
layout: statement
---

# A URL not in that table

returns **404**

without any of your code running.

---
layout: default
---

# Which explains a thing that looks broken

<v-click>

You are debugging a 404.

</v-click>

<v-click>

You put a breakpoint in your controller method.

</v-click>

<v-click>

It never hits.

</v-click>

<v-click>

<div class="mt-8 border-2 border-green-500/50 rounded-lg px-6 py-4 bg-green-500/10">

That is not a mystery. The mapping is telling you your method was **never a candidate**.

</div>

</v-click>

---
layout: default
---

# And one more consequence

<v-click>

Two methods claiming the same verb and path is a **startup** failure, not a runtime one.

</v-click>

<v-clicks>

- The application refuses to start
- The error names both methods
- No request has arrived yet

</v-clicks>

<v-click>

It can know that early because **the table is built before the port opens**.

</v-click>

---
layout: default
---

# The general form

```java
@RequestMapping(path = "/books", method = RequestMethod.GET)
```

<v-click>

Nobody writes that.

</v-click>

---
layout: default
---

# The shorthand

| Annotation | Verb | Use it for |
|---|---|---|
| `@GetMapping` | GET | Reading. Must not change state |
| `@PostMapping` | POST | Creating, or an action with effects |
| `@PutMapping` | PUT | Replacing wholesale |
| `@PatchMapping` | PATCH | Changing part |
| `@DeleteMapping` | DELETE | Removing |

<v-click>

Which verb belongs on which operation is a **design** question — and it is next session's question, not today's.

</v-click>

---
layout: default
---

# Class-level mapping

```java
@RestController
@RequestMapping("/books")
public class BookController {

    @GetMapping                 // GET /books
    public List<Book> all() { ... }

    @GetMapping("/{isbn}")      // GET /books/{isbn}
    public Book one(@PathVariable String isbn) { ... }
}
```

<v-click>

The prefix lives in one place. When it changes, you change one line.

</v-click>

---
layout: default
---

# A URL carries data in two places

<div class="grid grid-cols-2 gap-6 mt-8">

<div>

## Path

```
GET /books/978-0132350884
```

<v-click>

```java
@PathVariable String isbn
```

</v-click>

</div>

<div>

<v-click>

## Query

```
GET /books?available=true
```

</v-click>

<v-click>

```java
@RequestParam boolean available
```

</v-click>

</div>

</div>

<v-click>

They do not mean the same thing.

</v-click>

---
layout: default
---

# `@PathVariable`

```java
@GetMapping("/books/{isbn}")
public Book one(@PathVariable String isbn) { ... }
```

<v-clicks>

- Part of the resource's **identity**
- The name in the braces and your parameter name must **match**
- Or name it explicitly: `@PathVariable("isbn")`

</v-clicks>

<v-click>

That mismatch is a common early bug, and the error is not kind about it.

</v-click>

---
layout: default
---

# `@RequestParam`

```java
@GetMapping("/books")
public List<Book> all(@RequestParam(defaultValue = "false") boolean available) { ... }
```

<v-clicks>

- Modifies the request without identifying the thing
- No `defaultValue` and no `required = false` means **mandatory**
- Leave it off the URL and you get a **400** before your method runs

</v-clicks>

---
layout: statement
---

# The rule of thumb

Changes **which thing** you are talking about → path variable

Changes **how you want it shown** → request parameter

---
layout: statement
---

# Question

`GET /books/978-0132350884/loans?since=2026-01-01`

Which parts are path variables?

Why is `since` not one of them?

---
layout: section
---

# Do it

## Three ways in

---
layout: default
---

# Checkpoint 2

| Verb and path | Returns |
|---|---|
| `GET /books` | everything — **must still work with no parameter** |
| `GET /books?available=true` | only the ones with copies left |
| `GET /books/{isbn}` | one book, or **404** |

<v-click>

**404 is not an `if`.** `findByIsbn` already returns `Optional<Book>`, and `ResponseEntity.of(...)` turns that into a 200-with-body or an empty 404.

</v-click>

<v-click>

Tests 1–4 go green. Tests 5, 6 and 7 stay red.

</v-click>

---
layout: section
---

# Break

## Ten minutes

---
layout: section
---

# From your return value to JSON

---
layout: default
---

# Two problems, mirror images

<div class="grid grid-cols-2 gap-6 mt-8">

<div>

<v-click>

Your method says

```java
one(String isbn)
```

HTTP has no `String isbn`.

It has a **URL**.

</v-click>

</div>

<div>

<v-click>

Your method returns

```java
Book
```

HTTP has no `Book`.

It has **bytes** and a content type.

</v-click>

</div>

</div>

<v-click>

Something bridges both directions.

</v-click>

---
layout: default
---

# Going in — argument resolvers

<v-click>

Before your method runs, Spring walks its parameters **one at a time**.

</v-click>

<v-click>

For each one it asks a chain of resolvers: *do you know how to produce a value for this?*

</v-click>

---
layout: default
---

# Each resolver claims what it recognises

| Parameter | Resolved by |
|---|---|
| `@PathVariable String isbn` | pulls the segment out of the URL |
| `@RequestParam boolean available` | converts `"true"` to `true` |
| `HttpServletRequest request` | hands you the raw request |

<v-click>

This is why controller methods can have almost any signature you like.

</v-click>

<v-click>

There is no interface to implement. There is a **list**, and it is open-ended by design.

</v-click>

---
layout: default
---

# Going out — message converters

<v-clicks>

- Your method returns a `Book`
- Spring looks at what the client said it accepts
- Spring looks at the type you returned
- It picks an `HttpMessageConverter`

</v-clicks>

<v-click>

For an object and a client that accepts JSON, that is **Jackson**.

</v-click>

---
layout: default
---

# You never configured Jackson

<v-clicks>

- The starter brought it in
- Auto-configuration registered the converter
- Same pattern as Tomcat
- Same pattern as `DispatcherServlet` itself

</v-clicks>

<v-click>

<div class="mt-6 border-2 border-blue-500/50 rounded-lg px-6 py-4 bg-blue-500/10">

By now you should be **expecting** this.

</div>

</v-click>

---
layout: default
---

# The same converters run in reverse

```java
@PostMapping("/loans")
public LendingResult lend(@RequestBody LoanRequest request) { ... }
```

<v-clicks>

- JSON arrives in the body
- The Jackson converter reads it into a `LoanRequest`
- Your method gets an object

</v-clicks>

---
layout: default
---

# Leave `@RequestBody` off

<v-click>

Spring assumes the parameter is a request parameter instead.

</v-click>

<v-click>

You get an object full of `null`s — **not** an error.

</v-click>

<v-click>

<div class="mt-8 border-2 border-red-500/50 rounded-lg px-6 py-4 bg-red-500/10">

Which is worse than an error, because it looks like it worked.

</div>

</v-click>

---
layout: default
---

# And if no converter can read the body

<v-click>

Because the client never said what it was sending:

</v-click>

<v-click>

<div class="mt-6 text-center text-2xl font-bold">

415 Unsupported Media Type

</div>

</v-click>

<v-click>

<div class="mt-6 text-center opacity-80">

Again, before your method runs. You will do this one on purpose in the exercise.

</div>

</v-click>

---
layout: section
---

# So what is `@RestController`?

---
layout: default
---

# Start with `@Controller`

<v-clicks>

- A stereotype — a `@Component` the handler mapping scans
- Return a `String` from one of its methods...
- ...and Spring reads it as a **view name**

</v-clicks>

<v-click>

It hands the name to a view resolver, which finds a template, renders it, returns HTML.

</v-click>

---
layout: default
---

# `@ResponseBody` says: don't do that

<v-clicks>

- Take the return value
- Run it through a message converter
- Put the result in the body

</v-clicks>

---
layout: statement
---

# `@RestController`

is exactly

`@Controller` + `@ResponseBody`

---
layout: default
---

# That is the whole of it

<v-click>

There is no separate REST machinery.

</v-click>

<v-click>

<div class="mt-8 border-2 border-amber-500/50 rounded-lg px-6 py-4 bg-amber-500/10">

**The failure this explains:** write `@Controller` where you meant `@RestController`, return a `String`, get a 404 or a template error.

</div>

</v-click>

<v-click>

Spring did not ignore your return value. It took it as the **name of a view that does not exist**.

</v-click>

---
layout: default
---

# You will not build a view layer

<v-clicks>

- This is an API curriculum
- But the view path **is the default**
- And defaults are what you hit when you forget an annotation

</v-clicks>

---
layout: section
---

# The lifecycle, end to end

---
layout: default
---

# Seven steps

<div class="text-sm">

<v-clicks>

1. Request arrives at the server → the servlet mapped at `/`
2. **Handler mapping**: who handles this method and path?
3. **Handler adapter**: knows how to invoke that kind of handler
4. **Argument resolvers** run over your parameters
5. **Your method is called**
6. **Return value handler** → **message converter** writes the body
7. Response goes back out through the server

</v-clicks>

</div>

---
layout: default
---

# Where the line is

<div class="mt-6 rounded-lg border-2 border-blue-500/50 bg-blue-500/10 px-6 py-3 text-center">
Steps 1 – 4 &nbsp;·&nbsp; <span class="opacity-70">framework</span>
</div>

<v-click>

<div class="mt-3 rounded-lg border-2 border-green-500/60 bg-green-500/15 px-6 py-4 text-center font-bold">
Step 5 &nbsp;·&nbsp; your method
</div>

</v-click>

<v-click>

<div class="mt-3 rounded-lg border-2 border-blue-500/50 bg-blue-500/10 px-6 py-3 text-center">
Steps 6 – 7 &nbsp;·&nbsp; <span class="opacity-70">framework</span>
</div>

</v-click>

<v-click>

<div class="mt-8 text-center opacity-80">

One servlet · one thread · one method call. No queue, no event loop, no magic.

</div>

</v-click>

---
layout: statement
---

# Get out the paper

Compare it with the seven steps.

Which ones did you not know were there?

---
layout: statement
---

# Those are the steps

whose failures you could not have diagnosed.

---
layout: default
---

# Why memorise it

<div class="text-sm">

| You see | It means | Look at |
|---|---|---|
| 404, breakpoint never hit | No mapping matched | Step 2 |
| 400 naming a parameter | A resolver could not produce a value | Step 4 |
| 406 Not Acceptable | No converter for what the client accepts | Step 6 |
| 415 Unsupported Media Type | No converter could read your body | Step 4 |
| 500, your code in the trace | Your method threw | Step 5 — **yours** |

</div>

<v-click>

Four of those five are framework steps, and in every one your controller **never ran**.

</v-click>

---
layout: statement
---

# Knowing which side of *your method* a failure is on

is most of web debugging.

---
layout: default
---

# There is more in there than this

<v-clicks>

- Requests can also be **intercepted** — before the dispatcher, and around the handler
- Two more layers you have not met
- Genuinely useful, and the easiest thing in Spring to reach for at the wrong depth

</v-clicks>

<v-click>

<div class="mt-6 border-2 border-blue-500/50 rounded-lg px-6 py-4 bg-blue-500/10">

They come later in the course, alongside the **third** layer that makes the comparison worth having.

</div>

</v-click>

---
layout: section
---

# Do it

## Send something in

---
layout: default
---

# Checkpoint 3

| Verb and path | Takes | Returns |
|---|---|---|
| `POST /loans` | a `LoanRequest` in the body | the `LendingResult` from your service |

<v-clicks>

- The controller now needs `LendingService` **and** `BookRepository`
- Constructor parameter, `final` field — as always
- The body parameter needs `@RequestBody`

</v-clicks>

<v-click>

Then send it again **without** the `Content-Type` header, on purpose, and find the status in the table.

</v-click>

---
layout: default
---

# Yes, that is a lot of constructor

<v-click>

Two fields. Two parameters. Four lines that say the same thing twice.

</v-click>

<v-clicks>

- You will write a great many of these
- There is a tool that generates them for you

</v-clicks>

<v-click>

<div class="mt-8 border-2 border-blue-500/50 rounded-lg px-6 py-4 bg-blue-500/10">

You meet it in the Spring Boot session — **once you can read what it generates.**

</div>

</v-click>

<v-click>

<div class="mt-4 text-center opacity-70">

A shortcut you adopt before you understand the long way is not a shortcut.

</div>

</v-click>

---
layout: section
---

# Who runs your method

---
layout: default
---

# The model

<v-clicks>

- The container keeps a **pool** of threads
- A request arrives, a thread is taken from the pool
- That thread carries the request through all seven steps
- It writes the response and goes back to the pool

</v-clicks>

---
layout: statement
---

# One request, one thread,

start to finish.

**Thread-per-request.**

---
layout: section
---

# Testing the web layer

---
layout: default
---

# What you already know still works

<v-clicks>

- Plain JUnit
- AssertJ
- Mockito on ordinary objects

</v-clicks>

<v-click>

None of it goes away. None of it can answer this:

</v-click>

<v-click>

<div class="mt-6 text-center text-xl">

*Does `GET /books/{isbn}` return 404 for an unknown ISBN?*

</div>

</v-click>

---
layout: default
---

# Because calling the method proves nothing

```java
controller.one("bad-isbn");
```

<v-clicks>

- No mapping was consulted
- No resolver ran
- No converter wrote a body
- No status code was ever produced

</v-clicks>

<v-click>

You tested a plain Java method that happens to have annotations on it.

</v-click>

---
layout: statement
---

# You need something

that drives the real `DispatcherServlet`.

---
layout: default
---

# `MockMvc`

```java
mockMvc.perform(get("/books/978-0132350884"))
       .andExpect(status().isOk())
       .andExpect(jsonPath("$.title").value("Clean Code"));
```

<v-clicks>

- Runs the **whole lifecycle** — mapping, adapters, resolvers, your method, converters
- **Without starting a server or opening a socket**
- No port. Milliseconds per test

</v-clicks>

---
layout: default
---

# `jsonPath`, in four forms

| Expression | Reads |
|---|---|
| `$` | the root |
| `$.title` | a field |
| `$.length()` | the size of an array |
| `$[0].isbn` | the first element's field |

<v-click>

It is a small language. You will learn it from the three or four forms you actually use.

</v-click>

---
layout: default
---

# `@WebMvcTest` — the slice

```java
@WebMvcTest(LendingController.class)
class LendingControllerWebTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean LendingService lending;
    @MockitoBean BookRepository books;
}
```

<v-click>

A context containing **only the web layer**.

</v-click>

<v-click>

<div class="mt-4 text-sm opacity-80">

From `spring-boot-starter-webmvc-test` — package `org.springframework.boot.webmvc.test.autoconfigure`. The Boot 3 import no longer exists.

</div>

</v-click>

---
layout: default
---

# What is in the slice, and what is not

<div class="grid grid-cols-2 gap-6 mt-6">

<div>

## In

Your controllers

MVC infrastructure

`@ControllerAdvice`

The converters

</div>

<div>

<v-click>

## Out

Your services

Your repositories

</v-click>

</div>

</div>

<v-click>

Anything your controller needs, you supply as a mock with `@MockitoBean`.

</v-click>

---
layout: statement
---

# That is what "slice" means.

It is the point, not a limitation.

---
layout: default
---

# Why it is the tool you will reach for most

<v-clicks>

- Fast — a second or so
- **Honest about what it covers:** the controller and the plumbing around it
- The rest of the application stubbed out, by you, deliberately

</v-clicks>

<v-click>

*Is the route right? Is the status right? Is the JSON shaped right?* — this is the correct tool.

</v-click>

---
layout: statement
---

# `@MockitoBean`.

Not `@MockBean`.

---
layout: default
---

# Because it was removed

<v-clicks>

- `@MockBean` was deprecated, then **removed**
- On Boot 4 it does not exist
- The replacement is `@MockitoBean`, from `org.springframework.test.context.bean.override.mockito`

</v-clicks>

<v-click>

<div class="mt-8 border-2 border-red-500/50 rounded-lg px-6 py-4 bg-red-500/10">

Every tutorial written before 2024 is wrong about this. It costs ten minutes if nobody warns you. You have been warned.

</div>

</v-click>

---
layout: default
---

# One thing to know before it bites you

<v-clicks>

- Spring **caches a test context** and reuses it
- Across every test class with the same configuration
- Two `@WebMvcTest` classes naming the same controller and mocks share **one context**
- And therefore **one controller instance**

</v-clicks>

<v-click>

If one leaves state behind, the other inherits it.

</v-click>

---
layout: default
---

# There is a fuller kind of test

<v-clicks>

- It loads your **entire** application context, then puts a `MockMvc` on top
- Slower
- Right in a small number of cases — where the thing being tested **is** the interaction between layers

</v-clicks>

<v-click>

You meet it later in the course, at the point where the slice visibly fails to see something.

</v-click>

<v-click>

<div class="mt-6 border-2 border-green-500/50 rounded-lg px-6 py-4 bg-green-500/10">

Until then: **default to the slice.**

</div>

</v-click>

---
layout: section
---

# Do it

## Write the last two yourself

---
layout: default
---

# Checkpoint 4

<v-click>

`YourTurnTest` has two `fail("TODO: write this test")` methods. The `MockMvc` and both mocks are already there.

</v-click>

<v-clicks>

- **8.** `POST /loans` with the member missing from the body. Predict, assert, then look at what Spring actually does
- **9.** `GET /books` with an empty shelf. Empty is not missing

</v-clicks>

<v-click>

Then `mvn test`. **Nine green.**

</v-click>

---
layout: section
---

# Recap

---
layout: default
---

# What you can now do

<div class="text-sm">

<v-clicks>

- **One starter, and it stopped being a console program.** Tomcat and Jackson came with it
- **One servlet in front of everything.** Front Controller. Auto-configuration registered it, not you
- **The mapping table is built at startup.** A 404 means your method was never a candidate; two methods on one path fail before the port opens
- **Resolvers in, converters out.** `@RestController` is `@Controller` + `@ResponseBody`, and nothing more
- **Seven steps, one thread.** Knowing which side of *your method* a failure is on is most of web debugging
- **Thread-per-request.** One request, one thread, start to finish
- **`MockMvc` drives the real lifecycle without a socket.** `@WebMvcTest` loads the web layer and nothing else

</v-clicks>

</div>

---
layout: default
---

# Homework

<v-click>

No exercise this time.

</v-click>

<v-click>

<div class="mt-8 border-2 border-green-500/50 rounded-lg px-6 py-4 bg-green-500/10">

**Draft your personal project.**

</div>

</v-click>

---
layout: default
---

# Looking ahead

<v-click>

Next session takes the endpoints you built today and asks the questions this one deliberately refused.

</v-click>

<v-clicks>

- Which verb belongs on which operation
- Which status code actually means what happened
- What shape the body should be
- What should happen when the input is nonsense

</v-clicks>

<v-click>

<div class="mt-8 border-2 border-blue-500/50 rounded-lg px-6 py-4 bg-blue-500/10">

It opens on **your own test 8** — the `POST` with a field missing, where most of you will have asserted something different from what Spring really did. **Bring your answer.**

</div>

</v-click>
