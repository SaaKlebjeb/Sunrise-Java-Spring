# Spring Framework & Dependency Injection

*The container replaces your `main()` method — and Boot writes the container's paperwork.*

*A guided lesson. Read it start to finish; work the code as you go; stop at the **Pause** prompts and actually answer them before reading on.*

**Before you start**

- You need Maven and a browser. Today you generate your own project rather than opening one that was handed to you.
- **Network access.** You will download a project from `start.spring.io` and let Maven fetch its dependencies once.
- **Java 25 · Spring Boot 4.1.1 · Maven.** Do not mix versions.
- **A new domain starts here: library lending.** Nothing from the order-processing exercises carries over except the habits.
- **You start from an empty package.** There is no skeleton to fill in. The exercise tells you what each class must do and what the program must print. Each checkpoint also ends with the code in a folded block — open it when you are stuck on Java, because the wiring is the lesson and the plumbing is not.

**By the end you'll be able to:**

- Describe the problem Spring solves in terms of a `main()` method you have already written by hand.
- Explain Inversion of Control by saying **who calls whom**, without using the word "magic".
- Say what Spring is, what Spring Boot is, and why confusing the two makes both harder to learn.
- Generate a project from the Spring Initializr and **justify every box you ticked**.
- Read a generated `pom.xml` and say where the version numbers went.
- Explain the **root package rule** and predict the failure that breaks it.
- Register beans with `@Component` and its stereotypes, and with `@Bean` methods — and say which one each situation needs.
- Choose **constructor injection** and defend it against field injection with three specific arguments.
- Say what **singleton** actually means in Spring, and name the bug that follows from getting it wrong.
- Use `prototype`, and explain why marking a class prototype is **not enough on its own**.
- Use `@PostConstruct` and `@PreDestroy`, and say why one of them never runs on a prototype.
- Resolve two beans of one interface with `@Primary`, `@Qualifier`, and `List<T>` — and recognise the last one as the Strategy pattern.
- Read `NoSuchBeanDefinitionException` and `NoUniqueBeanDefinitionException` and know what to do about each.

---

## The `main()` method you have been writing

Go back to the very first session. You refactored `OrderService` so its collaborators arrived through interfaces instead of being created inside it. That was dependency injection, done by hand, and it worked.

Here is the same idea in today's domain, written the way it has to be written when nobody is helping:

```java
public static void main(String[] args) {
    BookRepository books = new InMemoryBookRepository();
    NotificationSender email = new EmailNotificationSender();
    AuditLog audit = new AuditLog("sunrise-library");

    LendingService lending = new LendingService(books, email, audit);

    lending.lend("978-0132350884", "sokha");
}
```

Six lines, and nothing is wrong with them. This is good code. It is also the smallest version of the problem.

### What happens when it grows

A real service layer has thirty or forty of these objects. `InMemoryBookRepository` becomes `JdbcBookRepository` and needs a `DataSource`, which needs a connection pool, which needs a config object. `LendingService` gains a fourth collaborator. Now:

- Those six lines are ninety, and they are in a file that has nothing to do with lending books.
- The **order** matters. You cannot build `LendingService` before its repository. You are hand-sorting a dependency graph, and the compiler will tell you when you get it wrong, but only after you have done the sorting.
- There is **more than one** of these `main`s. The application has one. Two test setups have one each. The nightly batch job has one. When `JdbcBookRepository` gains a third constructor argument, all four are edited, and the one you forget is the one that breaks at 3am.

> **Pause.** Before reading on: of those three problems, which one is actually about typing, and which are about something worse? Answer honestly — one of them is genuinely just tedium, and pretending otherwise is how people end up believing a framework is magic rather than useful.

### Naming the real problem

The tedium is real but it is not the point. The point is this:

**Your object graph is a real, important structure, and right now it exists only as instructions in a method — written out longhand, four times, with no way to check that the four agree.**

Spring's answer is to make that graph a thing the program knows about. You describe what exists and what needs what; something else works out the order and does the building.

---

## Inversion of Control

**IoC is a sentence about who calls whom.**

In the `main()` above, *your* code calls `new`. Your code is in charge; the objects are what it makes.

With a container, you never call `new` on `LendingService`. Something else does — it reads your constructor, works out that you need a `BookRepository`, finds one, builds it first, and hands it in. Your code is now the thing that gets called.

That reversal is the whole idea, and it is what "inversion" means. Nothing more mystical than that.

### The three words

You will hear these constantly, so pin them down now.

| Word | What it is |
|---|---|
| **Bean** | An object the container made and manages. That is the entire definition |
| **Bean definition** | The *recipe*: which class, how to build it, how many should exist. Not the object |
| **`ApplicationContext`** | The container itself — it holds the definitions, builds the beans, and hands them out |

The distinction between a bean and a bean definition is worth ten seconds. The definition is written once, at startup. The bean may be built once, or on every request, or never — and *that* is what scope decides.

### The honest one-sentence version

> The `ApplicationContext` is a `Map<String, Object>` with very good rules about how the values get made.

That is not a joke and it is not an oversimplification you will have to unlearn. If you can hold that picture, nothing in the rest of this session will feel like magic — including the parts that are genuinely clever.

### Dependency injection is how the map gets filled

IoC is the principle: something else is in charge of building. **Dependency injection is the specific technique** — the container reads what a constructor asks for, finds beans that match, and passes them in.

The two words are used almost interchangeably in job adverts. They are not the same size. IoC is the idea; DI is the mechanism Spring uses to deliver it.

> **Pause.** You have already done DI by hand — the constructor of `LendingService` takes its collaborators instead of building them. So what exactly is Spring adding? Say it in one sentence before reading on. (Hint: it is not "the injection".)

---

## What Spring is — and what it is not

The word "Spring" gets used for four different things and the confusion costs people weeks.

**Spring is a container.** That is the core, and it is a library called `spring-context`. It reads bean definitions, builds objects, injects them into each other, and manages their lifetime. Everything else in the ecosystem is built on top of it.

Worth saying plainly:

- **Not an application server.** Tomcat is one; Spring runs inside one, or embeds one.
- **Not a web framework.** Spring MVC is a web framework built on the container.
- **Not an ORM.** Spring Data JPA is a layer over Hibernate, built on the container.
- **Not Spring Boot.** Boot is a set of defaults and a startup routine for configuring the container.

Everything on that list is *built on* the thing this session is about. That is why the container comes first, even though we will be sitting inside a Boot project all afternoon.

### The container can run entirely without Boot

You will see this in older codebases and it is worth recognising:

```java
var context = new AnnotationConfigApplicationContext(AppConfig.class);
LendingService lending = context.getBean(LendingService.class);
```

Two lines: build a container from a config class, ask it for a bean. **That is the whole container API.** No Boot, no web server, no `application.properties`. We are not going to write applications this way — but the fact that it is possible is the proof that Boot is a convenience layer and not the framework itself.

---

## What Spring Boot adds

You have just been told the container is the real thing. So why does nobody start a project with two lines and a config class?

Because those two lines are the smallest part of starting an application. The rest is paperwork:

| Paperwork | Without Boot | With Boot |
|---|---|---|
| Which versions of forty jars work together | you choose, and get it wrong | a **parent POM** chooses |
| Which jars do I need for a web app at all | you research | one **starter** dependency |
| A config class and a scan root | you write it | `@SpringBootApplication` |
| A web server to run in | install and configure Tomcat | **embedded**, already configured |
| Sensible defaults for all of it | you write them | **auto-configuration** |

Boot is four ideas. Learn the names now; three of them you will use before the break.

### Starters

A **starter** is a dependency that exists only to pull in other dependencies. `spring-boot-starter-webmvc` contains almost no code — it declares that a web application needs Spring MVC, Jackson, a validator and an embedded Tomcat, at versions known to work together.

You add one line and get a working set instead of a research project.

### The parent POM

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.1.1</version>
</parent>
```

Remember the `<properties>` block you wrote by hand for JUnit, AssertJ and Mockito — one version number each, and it was your job to know they were compatible? The parent is that block, maintained by the Spring team, for several hundred libraries.

**The consequence you will see in ten minutes: your `pom.xml` will name almost no versions.** When you add `spring-boot-starter-webmvc` you write a `groupId` and an `artifactId` and stop. The version comes from the parent. That is not the generator being clever; it is ordinary Maven dependency management, and you already know how it works.

### `@SpringBootApplication`

One annotation that is three annotations:

| | What it is | Where you meet it |
|---|---|---|
| `@Configuration` | this class can hold bean definitions | today, after the break |
| `@ComponentScan` | scan **from this class's package, downwards** | today, after the break |
| `@EnableAutoConfiguration` | configure things based on what is on the classpath | the Spring Boot session |

Two of the three are the container annotations this session teaches. The third is the genuinely new thing Boot brings, and it gets a session of its own.

### Auto-configuration

Boot looks at what is on your classpath and configures it for you. Tomcat on the classpath → start a web server on port 8080. A database driver on the classpath → build a `DataSource`. Jackson on the classpath → configure JSON conversion.

**This is the one part of Boot that can feel like magic, and it is deliberately not today's lesson.** It is conditional configuration — ordinary `@Configuration` classes that carry an "only if" — and it gets taken apart properly in the Spring Boot session. Today, note where it happens and move on.

### The honest summary

> **Spring is the container. Boot is a very good default configuration of it, plus a way to start it.**

Nothing you learn about the container today becomes untrue inside a Boot application, because it is the same container. `spring-context` is in your project's dependency tree — you will see it there before the end of the session.

---

## Generating the project

Open [start.spring.io](https://start.spring.io). This is the **Spring Initializr**: a form that hands you a project.

### The form, field by field

| Field | What to pick | Why |
|---|---|---|
| **Project** | Maven | You wrote a POM by hand two sessions ago. Stay on the tool you can read |
| **Language** | Java | — |
| **Spring Boot** | **4.1.1** | The pinned version for this course. Not a `SNAPSHOT`, not an `M` or `RC` build — those are pre-releases |
| **Group** | `com.sunrise` | Reversed domain, exactly as in the sessions on Maven |
| **Artifact** | `lending-desk` | The project's name. Becomes the folder and the jar |
| **Name** | `lending-desk` | Fills itself in |
| **Package name** | `com.sunrise.lendingdesk` | **Remember this string.** It matters more than it looks. The form auto-fills `com.sunrise.lending_desk` from the artifact — type over it |
| **Packaging** | Jar | The application embeds its own server, so it is a jar, not a war |
| **Java** | **25** | Match your JDK, or nothing will compile |
| **Dependencies** | *see below* | — |

### Tick nothing you cannot justify out loud

For today: **no dependencies at all.** None. The lending desk is a console application with no web layer and no database, so the base project — which already contains the container and the test support — is exactly what it needs.

This feels wrong the first time. Everyone's instinct is to tick "Spring Web" because a Spring project surely needs it. Resist it. **An unexplained starter is a pile of auto-configuration you did not ask for**, and you will meet the consequences in the Spring Boot session when something starts a web server you did not want.

The rule, for the rest of your career: *if you cannot say in one sentence what a dependency is for, do not add it.*

### Generate, unzip, and open the `pom.xml`

Click **Generate**. You get a zip. Unzip it somewhere sensible.

**Open the `pom.xml`, not the folder.** IntelliJ needs the POM to build its project model. Open the folder and you get a directory of text files with no run buttons and no red underlines. This has cost more class time than any other single mistake in this course.

---

## Reading what the generator wrote

Four things arrived. Look at each one; none of it is unfamiliar.

### The POM

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.1.1</version>
</parent>

<properties>
    <java.version>25</java.version>
</properties>

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

Read it against the POM you wrote by hand:

- **One `<properties>` entry**, and it is the Java version. Every library version came from the parent.
- **Two dependencies**, and neither names a version.
- **`spring-boot-starter-test` is `<scope>test</scope>`** — the same scope you used for JUnit, for the same reason.

**Demo.** Run `mvn dependency:tree` and look for `spring-context`. It is in there, several levels down, underneath `spring-boot-starter`. That is the proof of the sentence from ten minutes ago: Boot did not replace the container; it depends on it.

That one command is also the answer to *"where did this class on my classpath come from?"* for the rest of your career.

### The application class

```java
package com.sunrise.lendingdesk;

@SpringBootApplication
public class LendingDeskApplication {
    public static void main(String[] args) {
        SpringApplication.run(LendingDeskApplication.class, args);
    }
}
```

Still a `main` method. Still the entry point. It just starts a container instead of building objects.

### The root package rule

Look at where that class lives: `com.sunrise.lendingdesk` — the package you typed into the form, and **nothing below it**.

`@SpringBootApplication` includes `@ComponentScan` with no argument, which means *"scan the package this class is in, and everything under it"*.

So:

- `com.sunrise.lendingdesk.service.LendingService` — **found**.
- `com.sunrise.lendingdesk.repository.InMemoryBookRepository` — **found**.
- `com.sunrise.other.Whatever` — **not found**, and nothing will tell you why until something asks for it.

**The application class lives at the root of your package tree. Everything else lives underneath it.** That single fact explains most "why is my bean not found" questions in Boot projects, and it is the commonest wiring bug there is. The failure is a `NoSuchBeanDefinitionException` that says nothing at all about packages.

> **Pause.** You move `LendingDeskApplication` from `com.sunrise.lendingdesk` down into `com.sunrise.lendingdesk.app`. Which beans can the container still find, and which cannot? Work it out before you try it — you will try it in the homework.

### The rest

- `src/main/resources/application.properties` — empty, and that is fine. It is where configuration goes; the Spring Boot session fills it.
- `src/test/java/.../LendingDeskApplicationTests.java` — one test, annotated `@SpringBootTest`, with an empty body. It is not a pointless test: `@SpringBootTest` starts the whole container, so **a context that will not start fails this test**. It is a smoke test for your wiring, and today it is the first thing that will go red.
- `mvnw` / `mvnw.cmd` — the Maven wrapper. It downloads the right Maven version, so a machine with no Maven installed can still build the project. Use `./mvnw test` if you like; `mvn test` is the same thing when Maven is on your PATH.

**Demo.** Run it now, before writing a line: `mvn test`, then `mvn spring-boot:run`. The application starts, prints a banner, reports how long it took, and exits — because with no web starter there is nothing to keep it alive. That is not a failure. That is the container starting, finding nothing to do, and shutting down cleanly.

---

## Registering beans

The container can only build what it knows about. There are exactly two ways to tell it, and the choice between them is not a style preference — it follows from one question: **can you edit the class?**

### `@Component` and the stereotypes

If the class is yours, annotate it:

```java
@Component
public class EmailNotificationSender implements NotificationSender { }
```

Spring ships three more that mean "component, and also...":

| Annotation | Says | Also does |
|---|---|---|
| `@Component` | a bean | — |
| `@Service` | business logic | nothing, technically |
| `@Repository` | talks to a data store | translates persistence exceptions into Spring's hierarchy |
| `@Controller` | handles web requests | is found by Spring MVC's handler mapping |

**Open `@Service` in your IDE.** It is annotated `@Component`. So is `@Repository`, so is `@Controller`. To the container they are the same thing — a *meta-annotation*, which is Spring's word for an annotation that carries another one.

So why bother? Two reasons, and they are not equal:

1. **A reader learns the layer from the annotation.** `@Repository` on a class says "this is the edge of the application" before you have read a line of it. That is worth a lot in a codebase with four hundred classes.
2. **Two of them do genuinely change behaviour** — the two right-hand entries in that table. `@Repository`'s exception translation matters in the persistence course; `@Controller`'s handler mapping matters in the Spring MVC session.

Use the specific one when it fits. Use `@Component` when nothing fits — a `NotificationSender` is not a service, a repository or a controller, and forcing it into one of those words is worse than the generic annotation.

### Every bean has a name

```java
@Component
public class SmsNotificationSender { }        // bean name: "smsNotificationSender"

@Component("sms")
public class SmsNotificationSender { }        // bean name: "sms"
```

The default is the simple class name with a lower-case first letter. You will need this in a few minutes when two beans implement one interface and you have to name the one you meant, so notice it now.

### Scanning happens already — you did not have to ask

This is the part that changed when you generated the project. There is no `@ComponentScan` in your code and there does not need to be one: `@SpringBootApplication` carries it, rooted at its own package.

So the only thing you have to get right is **where the class lives**. Annotate a class inside the root package tree and it is found. Put it outside and it is invisible.

**Demo.** Add `@Component` to a throwaway class inside the tree, print the bean names, see it. Move the class to `com.sunrise.elsewhere`, run again, watch it vanish — with no error, because nothing has asked for it yet. Then ask for it, and read the failure.

```java
@Bean
CommandLineRunner names(ApplicationContext ctx) {
    return args -> Arrays.stream(ctx.getBeanDefinitionNames()).sorted()
        .forEach(System.out::println);
}
```

`CommandLineRunner` is a Boot convenience: a bean that runs once, after the container is fully built. It is how a console application does its work, and you will need it in the exercise.

Read that list down. Your beans are in there under the names the container gave them, and so are a few dozen of Spring's own — the container puts its own machinery in the same map.

### `@Bean` methods — for classes you did not write

Scanning finds classes you were able to annotate. That leaves out everything else: a `DataSource`, an `ObjectMapper`, an HTTP client, anything from a jar.

```java
@Configuration
public class AuditConfig {

    @Bean
    public AuditLog auditLog() {
        return new AuditLog("sunrise-library");
    }
}
```

You wrote `new`, and that is correct. **A `@Bean` method is exactly where object creation is supposed to live now.** The rule was never "never call `new`"; it is "call it in one place, deliberately, instead of everywhere by accident".

Three details worth memorising:

- **The method name is the bean name.** This method defines a bean called `auditLog`. Rename the method, rename the bean, and break anything that asked for it by name.
- **The return type is what other beans match on.** `LendingService` asks for an `AuditLog` and gets this one. Declare the return type as the interface, not the implementation, when you want to keep the freedom to change it.
- **A `@Bean` method can take parameters**, and the container fills them in from other beans — which is how a `@Bean`-registered object gets its own dependencies.

A `@Configuration` class of your own is still worth writing in a Boot project, for exactly this. The application class *could* hold `@Bean` methods — it is a `@Configuration` class too — but a separate, named config class is easier to find.

### One thing about `@Configuration` you should be told now

```java
@Configuration
public class DataConfig {

    @Bean
    public DataSource dataSource() { return new HikariDataSource(); }

    @Bean
    public BookRepository books() {
        return new JdbcBookRepository(dataSource());   // calling it directly?
    }
}
```

That call to `dataSource()` looks like it makes a second `HikariDataSource`. It does not. Inside a `@Configuration` class, Spring subclasses your config at runtime and intercepts those calls, so the second one returns the bean that already exists.

Two reasons to know this today. First, it is a real behaviour you would otherwise misread. Second, **that interception is the same mechanism behind `@Transactional`, `@Cacheable` and `@PreAuthorize`** — the container is quietly wrapping your objects in subclasses of themselves. The Spring AOP session is entirely about how that works and where it breaks.

---

## How the container fills a dependency

Constructor, setter, field — three ways in. They are not equivalent, and the argument for the winner is short.

### Constructor injection

```java
@Service
public class LendingService {

    private final BookRepository books;
    private final NotificationSender notifier;
    private final AuditLog audit;

    public LendingService(BookRepository books, NotificationSender notifier, AuditLog audit) {
        this.books = books;
        this.notifier = notifier;
        this.audit = audit;
    }
}
```

**No `@Autowired`.** A class with exactly one constructor has already told Spring everything: since Spring 4.3 that constructor is used automatically. Write a second constructor and you must annotate the one you mean — which is the only situation where `@Autowired` on a constructor earns its place.

Three arguments for this style, in order of weight:

1. **If it compiles, every dependency is present.** There is no state in which a `LendingService` exists without a repository. Not "should not" — *cannot*.
2. **The fields are `final`.** Nothing reassigns them after construction, so the object is safe to share between threads, which matters enormously in about ten minutes.
3. **It is testable with `new`.** `new LendingService(fakeRepo, fakeSender, log)` — no container, no reflection, no annotations. Last session's tests all work on it unchanged.

### Setter injection

```java
@Autowired
public void setNotifier(NotificationSender notifier) {
    this.notifier = notifier;
}
```

Legal, and occasionally right — for a **genuinely optional** dependency, one the object works without. The cost is that there is now a window in which the object exists and is not ready, and the field cannot be `final`.

If you cannot name the case where the dependency is absent, you do not have an optional dependency. Use a constructor.

### Field injection

```java
@Service
public class LendingService {
    @Autowired private BookRepository books;      // don't
}
```

It is shorter. That is the entire case for it, and it loses on three counts:

- **It hides dependencies.** The constructor is the class's public statement of what it needs. Field injection deletes that statement, so the only way to find out what a class requires is to read every field — and nothing stops the list growing to fifteen, because nothing makes it uncomfortable.
- **The field cannot be `final`.** The compiler stops helping.
- **It is untestable without a container.** There is no constructor to pass anything to, so a plain unit test must either start Spring or use reflection to poke the field. Both are worse than what you already know how to do.

That last point is the one that matters. **A class that is hard to test is usually badly wired**, and field injection is a way of making a badly wired class *feel* fine.

> **Pause.** A colleague argues field injection is fine because their class has fifteen dependencies and a fifteen-argument constructor would be unreadable. There are two things wrong with that argument. Name both. (One is about constructors. The other is about the number fifteen.)

### The `final` field is the tell

You can review this from across the room. Constructor injection makes `final` fields possible; the other two make them impossible. So:

- Fields are `final` → constructor injection.
- Fields are not `final` → something else is going on, and it is worth asking why.

### Circular dependencies

`A` needs `B`, `B` needs `A`. With constructor injection this cannot be built, and Spring fails at startup with `BeanCurrentlyInCreationException`.

**That is a feature.** A cycle is a design problem — two classes that cannot be understood, tested or reused apart. Field injection can paper over it, and then the cycle survives for three years. Failing at startup is the cheapest possible moment to find out.

The fix is a design fix: pull the shared part into a third class, or make one direction an event.

---

## Scopes and lifecycle

Two questions the container answers for every bean: **how many of these should exist**, and **when does anything get to run**.

### Singleton is the default, and it does not mean what you think

By default, a bean definition produces **one instance per container, per bean name**, created eagerly at startup.

Three corrections to the word, because it is a badly chosen one:

- It is **not** the Singleton pattern. There is no private constructor, no static `getInstance()`. The class is an ordinary class; the container simply makes one.
- It is **one per container**, not one per JVM. Two contexts, two instances.
- It is **per bean name**, not per class. Register the same class twice under two names and you get two instances — which is a legitimate thing to do.

### The bug this causes

```java
@Service
public class LendingService {
    private int lentToday = 0;              // one field, one instance, every user

    public LendingResult lend(String isbn, String member) {
        lentToday++;
        ...
    }
}
```

One `LendingService` exists. Every request in the application runs through it, on many threads at once. That counter is shared, unsynchronised mutable state, and it will be wrong — not always, not reproducibly, which is the worst kind of wrong.

**The rule: a singleton bean must be stateless, or its state must be immutable or thread-safe.** Collaborators held in `final` fields are fine — they are set once and never change. A counter, a "current user", a `StringBuilder` being appended to across calls, a cached last result: not fine.

This is the most common Spring bug that is not an exception, and constructor injection with `final` fields is most of the protection against it — it makes the safe thing the easy thing.

> **Pause.** Which of these are safe as fields on a singleton `@Service`? (1) `private final BookRepository books;` (2) `private final List<String> auditTrail = new ArrayList<>();` (3) `private String currentMember;` (4) `private final AtomicInteger lentToday = new AtomicInteger();` — and for the one that is arguable, say what makes it arguable.

### Eager, and why that is good

Singletons are built at startup, not on first use. So a missing bean, a typo in a qualifier, a broken `@Bean` method — all of it fails **when the application starts**, in front of whoever started it.

The alternative is failing at 3am on the one code path nobody exercised. `@Lazy` turns eager off for a bean, and it is occasionally right — an expensive object that most runs never touch — but you are trading a startup failure for a runtime one, so do it on purpose.

This is also why the empty `@SpringBootTest` in your generated project is not a pointless test: it starts the container, so it fails the moment your wiring is wrong.

### `prototype`

```java
@Component
@Scope("prototype")
public class LoanTicketPrinter { }
```

A new instance **every time somebody asks**. Use it when the bean holds per-use state — a builder, a printer, a per-request accumulator.

Two things that surprise everyone:

- **The container stops caring once it hands the object over.** It creates the prototype, injects it, runs `@PostConstruct` — and then forgets it. **`@PreDestroy` never runs on a prototype.** If it holds a resource, closing it is yours.
- **Marking the class is not enough.** See below; this is the trap.

### The prototype-in-a-singleton trap

```java
@Service
public class TicketDesk {
    private final LoanTicketPrinter printer;                 // injected ONCE

    public TicketDesk(LoanTicketPrinter printer) {
        this.printer = printer;
    }

    public String issue(String isbn, String member) {
        return printer.print(isbn, member);                  // the same printer. Forever.
    }
}
```

It compiles. The context starts. No warning of any kind. And every ticket comes out of the same printer, because **a singleton's dependencies are injected once, when the singleton is built** — and that was the only time anybody asked for a printer. The prototype scope was honoured perfectly and never came up again.

The fix is to inject **the ability to get one**, not one:

```java
@Service
public class TicketDesk {
    private final ObjectProvider<LoanTicketPrinter> printers;

    public TicketDesk(ObjectProvider<LoanTicketPrinter> printers) {
        this.printers = printers;
    }

    public String issue(String isbn, String member) {
        return printers.getObject().print(isbn, member);     // a new one, every call
    }
}
```

`ObjectProvider<T>` is an ordinary field of an ordinary type, taken through the constructor like everything else. `getObject()` goes back to the container each time.

There are two other fixes — `@Lookup` on an abstract method, and a scoped proxy via `@Scope(value = "prototype", proxyMode = TARGET_CLASS)`. Both work. `ObjectProvider` is the one to reach for: it says what it is on the page, and the next person to read the class does not have to know a trick.

**You will write this wrong on purpose in the exercise.** Two minutes, two identical serial numbers on screen, and it will stay learned.

### The web scopes

`request` — one bean per HTTP request. `session` — one per user session. `application` — one per servlet context.

They only exist in a web application, and they are the right answer surprisingly rarely: a request-scoped bean injected into a singleton needs a scoped proxy, and by the time you have explained that to a colleague, passing the value as a method argument would have been simpler. They come back in the Spring MVC session, where the request lifecycle makes them concrete.

### Lifecycle callbacks

The order, for a singleton:

1. instantiate — the constructor runs, dependencies arrive
2. `@PostConstruct` — your hook, after the object is fully built
3. the bean is in use, for the life of the application
4. `@PreDestroy` — on orderly shutdown
5. destroyed

```java
@Repository
public class InMemoryBookRepository implements BookRepository {

    @PostConstruct
    void seed() { ... }

    @PreDestroy
    void flush() { ... }
}
```

**These are `jakarta.annotation` annotations, not Spring ones.** They are a Java standard that Spring honours. In a Boot project they arrive with `spring-boot-starter`, so the import works with no extra dependency — but importing them from the wrong package is a five-minute confusion the first time, and your IDE will happily offer you a wrong one.

**What actually goes in `@PostConstruct`?** With constructor injection, the dependencies are already there when the constructor runs — so the classic reason for this hook (field injection has not happened yet) does not apply to you. What is left is real but narrow: work that is too heavy or too failure-prone for a constructor. Loading seed data. Warming a cache. Validating that a configuration combination makes sense, so a bad config fails at startup with a sentence instead of at runtime with a stack trace.

If it is just `this.x = x`, it belongs in the constructor.

> **Pause.** `@PreDestroy` does not run on a prototype, and it does not run if the JVM is killed with `SIGKILL`. Given both of those, what is the honest answer to "can I rely on `@PreDestroy` to release a resource?" — and what does that imply about where cleanup should actually live?

---

## Two beans, one interface

This is the section that pays off the first session. You made everything an interface; here is the bill, and here is why it was worth paying.

Two classes implement `NotificationSender`. `LendingService` asks for one:

```java
public LendingService(BookRepository books, NotificationSender notifier, AuditLog audit)
```

The container looks for a bean of type `NotificationSender`, finds two, and stops:

```
NoUniqueBeanDefinitionException: expected single matching bean but found 2:
emailNotificationSender, smsNotificationSender
```

**Read that message.** It names both candidates. Nine times out of ten it tells you immediately which one you forgot to remove or annotate. Spring's exceptions are unusually good and most people never look past the first line.

There are three answers, and they are answers to three different questions.

### `@Primary` — "this is the default"

```java
@Component
@Primary
public class EmailNotificationSender implements NotificationSender { }
```

One bean per type may be primary. Every injection point that does not say otherwise gets it.

Use it when there genuinely is a normal choice and the others are exceptions. Do not use it to silence an error you have not thought about — a `@Primary` chosen at random is a wiring bug that starts cleanly.

### `@Qualifier` — "this injection point wants that one"

```java
public LendingService(@Qualifier("smsNotificationSender") NotificationSender notifier) { ... }
```

The string is a **bean name**, which is why it was worth knowing where bean names come from.

Use it when the choice belongs to the caller rather than to the type. It is the honest option when there is no sensible default — and if you find yourself writing the same qualifier at ten injection points, that is the signal that `@Primary` was the right tool.

### Resolution by parameter name — know it, don't rely on it

```java
public LendingService(NotificationSender smsNotificationSender) { ... }
```

This also works. Spring falls back to matching the parameter name against the bean name.

It is fragile: renaming a parameter — a rename any IDE will do without comment — silently rewires the application. Know it so you can recognise it in somebody else's code. Do not write it.

### `List<T>` — "give me all of them"

```java
@Service
public class BroadcastNotifier {

    private final List<NotificationSender> senders;

    public BroadcastNotifier(List<NotificationSender> senders) {
        this.senders = List.copyOf(senders);
    }

    public void announce(String member, String message) {
        senders.forEach(s -> s.send(member, message));
    }
}
```

Look at what that field asks for. Not a `NotificationSender` — a `List` of them. The container reads that as *"every bean of this type"*; it does not go hunting for a bean that happens to be a `List`.

**This is the Strategy pattern, and you did not write any of the plumbing.** No registry, no `Map` in a static initialiser, no `if` chain. Write a third sender, annotate it, and `BroadcastNotifier` picks it up without being edited. That is the open/closed principle showing up as a language feature, and it is the single most useful thing in this section.

Three details:

- **`Map<String, NotificationSender>`** gives you bean name → bean, which is what you want when the choice is data ("the user picked `sms`") rather than code.
- **Order is not guaranteed** unless you ask for it. `@Order(1)`, `@Order(2)` on the beans, or implement `Ordered`. When order matters and nobody said so, you have a bug that works on your machine.
- **An empty list is not an error.** If zero senders would be a bug, say so in the constructor. The container will not say it for you.

### Optional dependencies

```java
public LendingService(ObjectProvider<AuditLog> audit) {
    this.audit = audit.getIfAvailable(NullAuditLog::new);
}
```

`ObjectProvider` again, for a different reason: "one if there is one". `@Autowired(required = false)` does something similar and leaves you a `null` field, which is a step backwards from the `Optional` habits of the last session.

---

## Exercise — build the lending desk

`lending-desk/` is the project you generated before the break. It contains an application class, an empty properties file and one empty test. **Everything else is yours to write.**

There is no skeleton and no `TODO` marker. The `README.md` in `sessions/session-05-spring-core-di/lending-desk/` gives you, for each step:

- the class to create and **the package it goes in**,
- what it is responsible for, in a sentence,
- the method signatures other steps depend on,
- **the exact console output** you should see when the step is done,
- and **the code, in a folded block at the end of the checkpoint.**

**The folded block is an escape hatch, not the route.** Open it when you have lost five minutes
to something that is not about the container — a record you cannot get to compile, a `printf`
that will not line up. The wiring annotations are the whole lesson, and copying them before you
have tried costs you the session. Reading the block *after* you have it working is worth doing.

**Seven checkpoints.** Run the application after each one and compare what you see against what the README says you should see. That comparison is the grade; there is no test suite handing you a number.

This is deliberate and it is harder than the last four sessions. Wiring is not something you can learn by filling in blanks, because in a real project nobody leaves the blanks for you — you are given a requirement and an empty package. Today is the first time you get the real shape of the job.

**Do checkpoint 7 wrong on purpose.** The README tells you how. It is the best two minutes in the exercise.

If you get properly stuck, the README's *If you get stuck* table lists the failures by what you see on screen. Read that before asking — half the value of today is learning to read Spring's failures.

---

## Homework — the catalogue service

A second project, generated from scratch the same way, in a different slice of the library. **Six checkpoints**, and much less hand-holding: the homework README names the classes and the expected output and stops.

Two things appear there and nowhere else: **`@Order`** to control the order of an injected `List`, and two candidates where **neither is `@Primary`**, so `@Qualifier` is the only way through.

Three ungraded investigations close it, and they are the part worth doing: read a real `NoUniqueBeanDefinitionException`; move the application class one package down and work out from the failure what went missing; run `mvn dependency:tree` and find `spring-context` in there.

**Generating the project yourself for the second time is the point of the first step.** It takes ninety seconds and it is the skill that outlives everything else in this session.

---

## Recap

- **The problem** is not typing `new`. It is that your object graph exists only as instructions in a method, written out several times, with no way to check that the copies agree.
- **IoC** is a sentence about who calls whom. Your code used to call the constructor; now the container does. **DI** is the mechanism that delivers it.
- **The `ApplicationContext` is a `Map<String, Object>` with very good rules** about how the values get made.
- **Spring is the container. Boot is a very good default configuration of it, plus a way to start it.** `spring-context` is in your dependency tree; go and look.
- **Boot is four ideas:** starters, a parent POM that owns the versions, `@SpringBootApplication`, and auto-configuration.
- **`@SpringBootApplication` is `@Configuration` + `@ComponentScan` + `@EnableAutoConfiguration`** — and two of the three are what this session taught.
- **The root package rule:** the application class sits at the top, everything else underneath, because that is where scanning starts.
- **Tick nothing on the Initializr you cannot justify out loud.**
- **Two ways to register a bean**, and the question that chooses between them is "can I edit this class?" — a stereotype if yes, a `@Bean` method if no.
- **The stereotypes are all `@Component`.** Two of them also do something; all four tell a reader which layer they are in.
- **Constructor injection**, because if it compiles every dependency is present, the fields can be `final`, and the class is testable with `new`.
- **Singleton means one per container per bean name**, built eagerly. Its state must be immutable or thread-safe, and `final` collaborator fields are most of that protection.
- **A prototype injected into a singleton is injected once.** `ObjectProvider` is the fix.
- **`@PostConstruct` is for work too heavy for a constructor**, not for wiring. `@PreDestroy` never runs on a prototype.
- **Ambiguity has three answers:** `@Primary` for a default, `@Qualifier` for a specific injection point, `List<T>` for all of them — and the last one is the Strategy pattern delivered by the container.

## Looking ahead

Look again at the `@Configuration` note: Spring subclasses your config class at runtime so that calling one `@Bean` method from another returns the existing bean rather than building a second one. Something is intercepting an ordinary Java method call and changing what it does.

That same mechanism is how `@Transactional` commits a database transaction around a method you wrote, without that method containing a word about transactions. The next session is entirely about how the interception works — and about the one situation where it silently does nothing at all, which is a bug people ship for years without understanding.

Bring your `lending-desk` project. The next session builds on it directly.
