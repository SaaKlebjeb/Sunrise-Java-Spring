# Spring MVC

*What happens between the port and your method*

*A guided lesson. Read it start to finish; work the code as you go; stop at the **Pause**
prompts and actually answer them before reading on.*

**Before you start**

- Complete the Spring container session, and **bring the `lending-desk` project you built there.**
  Today adds to it rather than starting something new.
- **Network access at the start of the session** — one new starter comes down from Maven Central.
- **Open the `pom.xml`, not the folder.** Every session says this because it is the commonest
  ten-minute loss in the course.
- **Java 25 · Spring Boot 4.0.8 · Maven.** Do not mix versions.

**By the end you'll be able to:**

- Add one starter and say what it brought in, and what it started
- Trace an HTTP request through the `DispatcherServlet` and name every hand-off
- Distinguish handler mapping, handler adapter, argument resolver and message converter
- Explain what `@RestController` does that `@Controller` does not
- Say what thread-per-request means: one request, one thread, start to finish
- Test a web layer with `MockMvc`, and say what the `@WebMvcTest` slice does and does not load

---

## You have an application nobody can reach

Look at what you built last session. A container full of beans — a shelf, a lending service, two
notification senders, an audit log, a ticket desk. Wiring you understand, a `mvn test` that
passes, and a `CommandLineRunner` that prints to a console.

And absolutely no way for anything outside that JVM to use any of it.

That is five sessions of Java with no front door. Today you add one, and then you spend the rest
of the session finding out what happened when you did.

---

## Give it a web face

### One line of XML

Add one dependency to the POM:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc</artifactId>
</dependency>
```

No `<version>`. You know why by now — the parent POM owns versions, and that was the whole point
of reading the generated POM last session.

### What came with it

```
mvn dependency:tree
```

Two things in that output that you did not ask for by name:

- **An embedded Tomcat.** A servlet container, inside your application, as a library.
- **Jackson.** A JSON library.

One starter. One line. Both arrived, along with everything they need.

### And now the application does not stop

```
mvn spring-boot:run
```

Your `CommandLineRunner` runs and prints exactly what it printed yesterday — and then the process
**stays alive**. It is listening on port 8080.

You did not configure a port. You did not configure a server, a connector, a thread pool, or a
shutdown hook. You did not write a `main` loop. The jar on the classpath was enough.

This is the same move you have seen twice already and it will keep happening: **a class is present
on the classpath, therefore a default is configured.** That is auto-configuration, and it gets a
whole session of its own later in the course.

### One endpoint

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

Nothing about the wiring is new. A constructor parameter and a `final` field, exactly as every
collaborator has been injected since last session. The class is a `@Component` like any other —
`@RestController` is a stereotype, and the container registers it by scanning, under the same root
package rule you already know.

The only genuinely new thing is `@GetMapping`.

Run it, and ask for the books:

```
$ curl localhost:8080/books
[{"isbn":"978-0132350884","title":"Clean Code","copies":3},
 {"isbn":"978-0134685991","title":"Effective Java","copies":2},
 {"isbn":"978-0201616224","title":"The Pragmatic Programmer","copies":1}]
```

**Look at what you did not write.** You did not write JSON. You did not serialise anything. You
did not set a status code or a content type. You returned a `List<Book>` from an ordinary method
and something in between turned it into that.

> **Pause.** Before reading on, write down — in your own words, no looking — what you think
> happened between `curl` sending `GET /books` and your `all()` method being called. Three or four
> steps. **Keep the paper.** You will check it against the real answer later in this session, and
> the gap between the two is the actual content of the next two hours.

---

## One servlet in front of everything

### The old way, briefly

In the first session you met a raw `HttpServlet`: a class with `doGet` and `doPost`, mapped to a
URL pattern, running inside a servlet container. One servlet per URL, or per family of URLs. A
large application meant a large pile of them and a configuration file mapping them all.

You are not going back there. But the *shape* matters, because Spring MVC did not replace the
servlet — it put exactly **one** servlet in front of everything and made that servlet a dispatcher.

### The same endpoint, three ways

Here is `GET /books` — the endpoint you wrote in checkpoint 1 — as a raw servlet:

```java
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

Every line is yours: the content type, the status, the loop, the JSON — and a book title with a
quote in it breaks that JSON. The repository is built with `new`, because there is no container.

The same endpoint in Spring MVC before Boot, around 2010:

```java
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

The JSON and the routing have left your code. What is left is ceremony: `@ResponseBody` on every
method, `@Autowired` on the constructor, the long form of the mapping.

Now compare it with the controller you wrote a few minutes ago. `@RestController` stands in for
`@Controller` plus `@ResponseBody` on every method, `@GetMapping` is the short form of the
mapping, and a class with a single constructor is injected without `@Autowired`. **Every job the
servlet did by hand still gets done. Somebody else does it** — and that somebody is the subject of
the rest of this section.

### The Front Controller pattern

That one servlet is called `DispatcherServlet`. Every request to your application — `/books`,
`/loans`, `/anything` — arrives at the same servlet. It does not handle any of them itself. Its
entire job is to work out who *should* handle this request, arrange for that to happen, and turn
whatever comes back into an HTTP response.

This is a named design pattern: **Front Controller**. One entry point, central routing,
per-request work delegated outward.

The reason it is worth a name rather than a shrug is what it buys you. Because every request
passes through one object, anything that must happen for *every* request has exactly one place to
live. Exception handling. Content negotiation. Locale resolution. You will use all three without
ever writing a line of dispatch code, and it is the single servlet that makes that possible.

### You never registered it

There is no `web.xml` in your project and no line in any configuration class that mentions it.
Search your own source for the word and you will not find it.

`spring-boot-starter-webmvc` put Tomcat on the classpath. Boot's auto-configuration saw Tomcat there,
saw `DispatcherServlet` there, and registered one against `/` — the whole URL space.

Before Boot, you registered it yourself. This is what it took — two XML files:

```xml
<!-- WEB-INF/web.xml -->
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

<!-- WEB-INF/dispatcher-servlet.xml (namespace declarations omitted) -->
<beans>
  <context:component-scan base-package="com.sunrise.lendingdesk"/>
  <mvc:annotation-driven/>
</beans>
```

From Spring 3.1 the same registration could be written in Java, but it was still yours to write:

```java
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

Then you packaged the application as a WAR and deployed it into a Tomcat you had installed and
configured yourself. Your version of all of that is the one `<dependency>` you added at the top of
the session. **Every line of it still runs in your application** — Boot writes it for you.

Sit with that for a second. The central object of the entire web framework is something you did
not name, did not configure, and cannot find by searching your own code. It is real, it is
running, and it is the thing your request hits first.

---

## How a URL finds your method

`DispatcherServlet` has a request and needs a method. The thing that answers that question is a
**handler mapping**.

When your application starts, Spring scans every `@Controller` and `@RestController` bean, reads
every `@RequestMapping` on it — including the shorthand forms — and builds a table. Roughly:

| Method | Path | Handler |
|---|---|---|
| `GET` | `/books` | `LendingController#all` |
| `GET` | `/books/{isbn}` | `LendingController#one` |
| `POST` | `/loans` | `LendingController#lend` |

The table is built **once, at startup**. It is not rebuilt per request and it is not worked out by
reflection each time. When a request arrives, the mapping is a lookup.

Two consequences follow, and both are things you will meet.

**A URL that is not in that table returns 404 without any of your code running.** If you are
debugging a 404 and you have put a breakpoint in your controller method, the breakpoint will never
hit — and that is not a mystery, it is the mapping telling you your method was never a candidate.

**Two methods claiming the same verb and path is a startup failure, not a runtime one.** The
application refuses to start and the error names both methods. It can know that before a single
request arrives, because the table is built before the port opens.

### The annotations

`@RequestMapping` is the general form and takes a method:

```java
@RequestMapping(path = "/books", method = RequestMethod.GET)
```

Nobody writes that. The shorthand annotations exist for each verb and you should use them:

| Annotation | Verb | Use it for |
|---|---|---|
| `@GetMapping` | GET | Reading something. Must not change state |
| `@PostMapping` | POST | Creating something, or an action with effects |
| `@PutMapping` | PUT | Replacing something wholesale |
| `@PatchMapping` | PATCH | Changing part of something |
| `@DeleteMapping` | DELETE | Removing something |

Which verb belongs on which operation is a design question, and it is the next session's question,
not today's. Today you need them to exist so that the mapping table has more than one row in it.

`@RequestMapping` also works at class level, and then every method path is relative to it:

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

Use it when a controller's paths share a prefix. It keeps the prefix in one place, and when it
changes you change one line.

### Two ways to get a value out of a URL

A URL carries data in two different places and they mean different things.

**Path variables** are part of the resource's identity:

```
GET /books/978-0132350884
```

```java
@GetMapping("/books/{isbn}")
public Book one(@PathVariable String isbn) { ... }
```

The name in the braces and the parameter name must match, or you name it explicitly with
`@PathVariable("isbn")`. That mismatch is a common early bug and the error message is not
especially kind about it.

**Request parameters** modify the request without identifying the thing:

```
GET /books?available=true
```

```java
@GetMapping("/books")
public List<Book> all(@RequestParam(defaultValue = "false") boolean available) { ... }
```

A `@RequestParam` with no `defaultValue` and no `required = false` is **mandatory** — leave it off
the URL and you get a 400 before your method runs. That is usually what you want, and it is
occasionally a surprise.

The rule of thumb, and it will carry you a long way: **if removing it changes which thing you are
talking about, it is a path variable. If removing it changes how you want that thing shown to you,
it is a request parameter.**

> **Pause.** `GET /books/978-0132350884/loans?since=2026-01-01`. Which parts are path variables,
> which is a request parameter, and why is `since` not part of the path?

---

## From your return value to JSON

You now have a method and a way to call it. Two problems remain, and they are mirror images.

Your method signature says `one(String isbn)`. HTTP has no `String isbn` — it has a URL. Your
method returns a `Book`. HTTP has no `Book` — it has bytes and a content type.

Something has to bridge both directions.

### Going in: argument resolvers

Before your method is invoked, Spring walks its parameters one at a time and asks a chain of
**argument resolvers** which of them knows how to produce a value for this parameter.

`@PathVariable String isbn`? The path-variable resolver takes it, pulls the matching segment out
of the URL, converts it to `String`, done. `@RequestParam boolean available`? The request-param
resolver takes it and converts `"true"` to `true`. `HttpServletRequest request`? There is a
resolver for that too, which hands you the raw request object.

This is why controller methods can have almost any signature you like. There is no fixed interface
to implement — there is a list of resolvers, and each one claims the parameters it recognises. It
is open-ended by design, and you can add your own.

### Going out: message converters

Your method returns a `Book`. An `HttpMessageConverter` turns it into bytes.

Spring looks at what the client said it accepts, looks at the type you returned, and picks a
converter. For an object and a client that accepts JSON, that is the Jackson converter, which
serialises the object and sets `Content-Type: application/json`.

You never configured Jackson. `spring-boot-starter-webmvc` brought it in, and auto-configuration
registered the converter — the same pattern as Tomcat, the same pattern as the `DispatcherServlet`
itself. By now you should be expecting this.

The same converters run in reverse for `@RequestBody`:

```java
@PostMapping("/loans")
public LendingResult lend(@RequestBody LoanRequest request) { ... }
```

JSON arrives in the body, the Jackson converter reads it into a `LoanRequest`, your method gets an
object. `@RequestBody` is the marker that says *this parameter comes from the body, run it through
a converter* — without it, Spring assumes the parameter is a request parameter or a model
attribute, and you get an object full of nulls rather than an error, which is worse.

**And if no converter can read the body**, because the client did not say what it was sending, the
request fails with **415 Unsupported Media Type** — again, before your method runs. You will do
that one on purpose in the exercise.

> **Yes, that is a lot of constructor for two fields**, and you are about to add a second
> collaborator to the controller. You will write a great many of these. There is a tool that
> generates them, and you meet it in the Spring Boot session — deliberately after this one,
> because a shortcut you adopt before you understand the long way is not a shortcut.

### And this is what `@RestController` is

`@Controller` is a stereotype — a `@Component` that the handler mapping will scan for request
mappings. On its own, a `String` returned from a `@Controller` method is interpreted as **a view
name**: Spring hands it to a view resolver, which finds a template, renders it, and returns HTML.

`@ResponseBody` says: do not do that. Take the return value, run it through a message converter,
put the result in the body.

`@RestController` is exactly `@Controller` + `@ResponseBody`, applied to every method in the
class. That is the whole of it. There is no separate REST machinery.

**The failure this explains:** put `@Controller` where you meant `@RestController`, return a
`String`, and you get a 404 or a template error rather than your string. Spring did not ignore
your return value — it took it as the name of a view that does not exist. Once you know that, the
error message stops being nonsense.

You will not build a view layer in this course. The curriculum is an API curriculum. But you need
to know the view path exists, because it is the default, and defaults are what you hit when you
forget an annotation.

---

## The lifecycle, end to end

Put it together. This is the diagram the whole session is for, and it is worth being able to draw
from memory.

1. The request arrives at the embedded server, which hands it to the servlet mapped at `/` — the
   `DispatcherServlet`.
2. `DispatcherServlet` asks each **handler mapping** in turn: do you have a handler for this
   method and path?
3. `DispatcherServlet` finds a **handler adapter** that knows how to invoke that kind of handler.
4. The adapter runs the **argument resolvers** over your method's parameters.
5. **Your method is called.** Everything before this line is framework; everything inside is
   yours.
6. The return value goes to a **return value handler**, which for `@ResponseBody` picks an
   **`HttpMessageConverter`** and writes the body.
7. The response goes back out through the server.

All of it inside one servlet, on one thread, in one method call. There is no queue, no event loop,
no magic.

> **Pause.** Get out the paper from earlier. Compare. Which steps did you not know were there?
> Those are the steps whose failures you would not have been able to diagnose.

### Why this is worth memorising

Because errors name these components, and the names are useless until they mean something.

| You see | It means | Look at |
|---|---|---|
| 404, breakpoint never hit | No mapping matched | Your paths and verbs — step 2 |
| 400 with a parameter name | An argument resolver could not produce a value | Step 4 |
| 406 Not Acceptable | No converter for what the client will accept | Step 6 |
| 415 Unsupported Media Type | No converter could read the body you sent | Step 4, `@RequestBody` |
| 500, your code in the stack trace | Your method threw | Step 5. This one is yours |

Four of those five are framework steps, and in every one of them your controller never ran.
Knowing which side of *your method* a failure is on is most of web debugging.

> **There is more in the lifecycle than this.** Requests can also be intercepted — before the
> dispatcher, and around the handler — and there are two more layers you have not met. They are
> genuinely useful and they are also the easiest thing in Spring to reach for at the wrong depth,
> so they are taught later in the course, alongside the third layer that makes the comparison
> worth having.

---

## One request, one thread

The servlet container keeps a pool of threads. A request arrives, a thread is taken from the pool,
that thread carries the request through every one of the steps above, writes the response, and
goes back to the pool.

**One request, one thread, start to finish.** This is called **thread-per-request**, and it is the
execution model of every servlet application, including yours.

---

## Testing the web layer

You have been testing since the testing session — plain JUnit, AssertJ, Mockito on ordinary
objects. None of that goes away. But none of it can answer the question *"does `GET /books/{isbn}`
return 404 for an unknown ISBN?"*, because that question is about routing, argument resolution and
status codes, and those live in the framework, not in your class.

Calling `controller.one("bad-isbn")` directly in a test proves nothing about any of it. The method
runs, but no mapping was consulted, no resolver ran, no converter wrote a body, and no status code
was ever produced. You would be testing a plain Java method that happens to have annotations on
it.

You need something that drives the real `DispatcherServlet`. That is **`MockMvc`**.

### `MockMvc`

`MockMvc` runs the whole request lifecycle — mapping, adapters, resolvers, your method, converters
— **without starting a server or opening a socket.** No port, no network, milliseconds per test.

```java
mockMvc.perform(get("/books/978-0132350884"))
       .andExpect(status().isOk())
       .andExpect(jsonPath("$.title").value("Clean Code"));
```

`jsonPath` reads into the response body: `$` is the root, `$.title` a field, `$.length()` the size
of an array, `$[0].isbn` the first element's field. It is a small language and you will pick it up
from the three or four forms you actually use.

### `@WebMvcTest` — the slice

```java
@WebMvcTest(LendingController.class)
class LendingControllerWebTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean LendingService lending;
    @MockitoBean BookRepository books;
}
```

This starts a context containing **only the web layer**: your controllers, the MVC infrastructure,
`@ControllerAdvice`, and the converters. Your services and repositories are **not** in it. Anything
your controller needs, you supply as a mock with `@MockitoBean`.

That is what "slice" means, and it is the point rather than a limitation. The test is fast — a
second or so — and it is honest about what it covers: the controller and the plumbing around it,
with the rest of the application stubbed out. When you are asking *"is the route right, is the
status right, is the JSON shaped right"*, this is the correct tool, and it is the tool you will
reach for most.

> **On `@MockitoBean`:** if you search for this you will find `@MockBean` everywhere. `@MockBean`
> was deprecated and **removed**; on Boot 4 it does not exist. The replacement is `@MockitoBean`,
> from `org.springframework.test.context.bean.override.mockito`. Every tutorial written before 2024
> is wrong about this and it will cost you ten minutes if nobody warns you. You have now been
> warned.

> **Same trap, second annotation:** `@WebMvcTest` moved in Boot 4. It lives in
> `spring-boot-starter-webmvc-test` — **not** in `spring-boot-starter-test` — and its package is
> `org.springframework.boot.webmvc.test.autoconfigure`. An import copied from an older tutorial,
> `org.springframework.boot.test.autoconfigure.web.servlet`, no longer exists.

### One thing to know about the slice before you meet it the hard way

Spring **caches a test context and reuses it** across every test class with the same
configuration. Two `@WebMvcTest` classes naming the same controller and the same mocks share one
context — and therefore one controller instance. If one of them leaves state behind, the other
inherits it.

> **There is a fuller kind of test than the slice** — one that loads your entire application
> context and then puts a `MockMvc` on top of it. It is slower, and it is the right answer in a
> small number of cases where the thing being tested *is* the interaction between layers rather
> than the controller. You will meet it, and the reason it exists, later in the course, at the
> point where the slice visibly fails to see something. Until then: **default to the slice.**

---

## Exercise — the lending desk gets a front door

`lending-desk/README.md` has the detail. In outline, four checkpoints:

1. **Give it a web face** — one starter, `mvn dependency:tree`, a server nobody configured, and
   one endpoint.
2. **Three ways in** — a `@RequestParam` filter and a `@PathVariable`, with a real 404.
3. **Send something in** — `POST /loans` with `@RequestBody`, then break it on purpose with a
   missing `Content-Type`.
4. **Write the last two tests yourself.**

### How you check your work

**Nine tests.** Copy the `src/test` tree from this folder into your own project and run `mvn test`.

Seven of them are written for you and must go green, all in the `@WebMvcTest` slice. The remaining
**two are `fail("TODO: write this test")` skeletons in `YourTurnTest`**, and checkpoint 4 is where
you write them. `mvn test` is red until you do, and the red list is your
to-do list.

The checkpoints are released one at a time, each straight after the part of the session that
teaches it. Aim to **finish checkpoint 3 in the room and to have started 4**; the rest of 4 is for the
rest of the day. That is the plan rather than a fallback, and a student who thinks they are behind
stops thinking.

There is no "ask the instructor whether it worked" step in this session.

---

## Recap

- **One starter, and the application stopped being a console program.** Tomcat and Jackson came
  with it; a server you did not configure is listening on 8080.
- **One servlet in front of everything.** `DispatcherServlet` is the Front Controller. You did not
  register it; auto-configuration did.
- **How a URL finds your method.** The handler mapping table is built at startup. A 404 means your
  method was never a candidate, which is why the breakpoint never hits — and two methods on one
  path fail before the port even opens.
- **In and out.** Argument resolvers build your parameters; message converters turn your return
  value into bytes. `@RestController` is `@Controller` + `@ResponseBody`, and nothing more.
- **The lifecycle.** Seven steps, one thread, one method call. Knowing which side of *your method*
  a failure sits on is most of web debugging.
- **One request, one thread.** A thread from the container's pool carries each request through
  all seven steps, start to finish. That is thread-per-request.
- **Testing.** `MockMvc` drives the real lifecycle without a socket. `@WebMvcTest` loads the web
  layer and nothing else — and that is the point.

## Homework

There is no homework exercise this session. Use the time to **draft your personal project**.

## Looking ahead

Next session takes the endpoints you built today and asks the design questions this one
deliberately refused: which verb belongs on which operation, which status code actually means what
happened, what shape the body should be, and what should happen when the input is nonsense.

It opens on your own test 8 — the `POST` with a field missing, where most of you will have
asserted something different from what Spring really did. Bring your answer.
