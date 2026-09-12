---
layout: section
---

# Spring Framework & Dependency Injection

The container replaces your `main()` — and Boot writes its paperwork

---
layout: default
---

# Learning Objectives

<v-clicks>

- Describe the problem Spring solves, in terms of a `main()` you have already written
- Explain **Inversion of Control** by saying who calls whom — without the word "magic"
- Say what Spring is, what Spring Boot is, and why confusing them costs weeks
- Generate a project from the **Spring Initializr** and justify every box you ticked
- Explain the **root package rule** and predict the failure that breaks it
- Register beans with stereotypes and with `@Bean` methods
- Choose **constructor injection**, and defend it with three specific arguments
- Say what **singleton** really means, and name the bug that follows
- Use `prototype` — and explain why marking a class prototype is **not enough**
- Resolve two beans of one interface: `@Primary`, `@Qualifier`, `List<T>`

</v-clicks>

---
layout: default
---

# A new domain starts here

<div class="grid grid-cols-2 gap-6 mt-8">

<div>

## Behind us

Order processing · employees · a bookshop

Records, sealed types, streams, JUnit, Mockito

</div>

<div>

<v-click>

## From here

**Library lending**

Nothing carries over except the habits

</v-click>

</div>

</div>

<v-click>

<div class="mt-8 border-2 border-green-500/50 rounded-lg px-6 py-4 bg-green-500/10">

Fall behind last session and you still start today clean.

</div>

</v-click>

---
layout: default
---

# You start from an empty package

<v-clicks>

- No skeleton project
- No `TODO` markers to fill in
- You get a **requirement** and an **empty package** — the real shape of the job

</v-clicks>

<v-click>

<div class="mt-6 border-2 border-blue-500/50 rounded-lg px-6 py-4 bg-blue-500/10">

Every checkpoint ends with **the code, folded away.**

Open it when you are stuck on *Java*. Today's lesson is the wiring, and being stuck on a
`for` loop costs you the part that matters.

</div>

</v-click>

---
layout: section
---

# The `main()` method

You have been writing it by hand since the first session

---
layout: default
---

# Wiring, done by hand

```java
public static void main(String[] args) {
    BookRepository books = new InMemoryBookRepository();
    NotificationSender email = new EmailNotificationSender();
    AuditLog audit = new AuditLog("sunrise-library");

    LendingService lending = new LendingService(books, email, audit);

    lending.lend("978-0132350884", "sokha");
}
```

<v-click>

<div class="mt-6 border-2 border-green-500/50 rounded-lg px-6 py-4 bg-green-500/10">

**Nothing is wrong with this.** It is good code.

It is also the smallest possible version of the problem.

</div>

</v-click>

---
layout: default
---

# Now give it thirty objects

<v-clicks>

- `InMemoryBookRepository` becomes `JdbcBookRepository`
- which needs a `DataSource`
- which needs a connection pool
- which needs a config object
- and `LendingService` gains a fourth collaborator

</v-clicks>

<v-click>

<div class="mt-6 border-2 border-yellow-500/50 rounded-lg px-6 py-4 bg-yellow-500/10">

Six lines are now ninety — in a file that has nothing to do with lending books.

</div>

</v-click>

---
layout: default
---

# Problem one — the order matters

```java
LendingService lending = new LendingService(books, email, audit);

BookRepository books = new InMemoryBookRepository();   // cannot find symbol
NotificationSender email = new EmailNotificationSender();
AuditLog audit = new AuditLog("sunrise-library");
```

<v-click>

You cannot build `LendingService` before its repository.

</v-click>

<v-click>

So you are **hand-sorting a dependency graph**.

</v-click>

<v-click>

<div class="mt-6 text-sm opacity-80">

The compiler will tell you when you get it wrong — but only after you have done the sorting.

</div>

</v-click>

---
layout: default
---

# Problem two — there is more than one

<v-clicks>

- the application has one
- the integration test has one
- the other integration test has one
- the nightly batch job has one

</v-clicks>

<v-click>

<div class="mt-6 border-2 border-red-500/50 rounded-lg px-6 py-4 bg-red-500/10">

`JdbcBookRepository` gains a third constructor argument.

**Four files change. The one you forget is the one that breaks at 3am.**

</div>

</v-click>

---
layout: statement
---

# Which of those three is just tedium?

Take a minute. Answer before the next slide.

---
layout: default
---

# The tedium is the least of it

<div class="grid grid-cols-3 gap-4 mt-8">

<div class="border-2 border-gray-500/40 rounded-lg px-4 py-4">

**Ninety lines**

Annoying.

Survivable.

</div>

<v-click>

<div class="border-2 border-yellow-500/50 rounded-lg px-4 py-4 bg-yellow-500/10">

**The ordering**

A structure you maintain in your head.

</div>

</v-click>

<v-click>

<div class="border-2 border-red-500/50 rounded-lg px-4 py-4 bg-red-500/10">

**Four copies**

A structure with no single source of truth.

</div>

</v-click>

</div>

---
layout: statement
---

# The real problem

Your object graph is a real, important structure —

and it exists only as **instructions in a method**,

written out four times, with **no way to check that the four agree**.

---
layout: section
---

# Inversion of Control

A sentence about who calls whom

---
layout: default
---

# Before and after

<div class="grid grid-cols-2 gap-6 mt-8">

<div>

## Before

**Your code calls `new`**

Your code is in charge.

The objects are what it makes.

</div>

<div>

<v-click>

## After

**Something else calls your constructor**

It reads what you asked for, finds it, builds it first, hands it in.

Your code is the thing that gets called.

</v-click>

</div>

</div>

<v-click>

<div class="mt-8 border-2 border-green-500/50 rounded-lg px-6 py-4 bg-green-500/10">

That reversal is what the word **inversion** means. Nothing more mystical than that.

</div>

</v-click>

---
layout: default
---

# Three words to pin down

| Word | What it is |
|---|---|
| **Bean** | An object the container made and manages. That is the entire definition |
| **Bean definition** | The *recipe* — which class, how to build it, how many. Not the object |
| **`ApplicationContext`** | The container — holds definitions, builds beans, hands them out |

<v-click>

<div class="mt-8 border-2 border-blue-500/50 rounded-lg px-6 py-4 bg-blue-500/10">

The definition is written **once, at startup**.

The bean may be built once, or on every request, or never — that is what **scope** decides.

</div>

</v-click>

---
layout: statement
---

# The `ApplicationContext` is a

# `Map<String, Object>`

with very good rules about how the values get made

Not a metaphor. Not a lie you unlearn later.

---
layout: default
---

# IoC and DI are not the same size

<div class="grid grid-cols-2 gap-6 mt-8">

<div>

## Inversion of Control

The **principle**.

Something else is in charge of building.

</div>

<div>

<v-click>

## Dependency Injection

The **mechanism**.

The container reads a constructor, finds matching beans, passes them in.

</v-click>

</div>

</div>

<v-click>

<div class="mt-8 text-sm opacity-80">

Job adverts use the two words interchangeably. They are not interchangeable.

</div>

</v-click>

---
layout: statement
---

# You already do DI by hand

So what is Spring actually adding?

One sentence. It is not "the injection".

---
layout: default
---

# What Spring adds

<v-clicks>

- Not the injection — your constructors already take their collaborators
- **The graph** — it exists as something the program knows about
- **One place** — described once, not written out four times
- **The ordering** — worked out for you, not maintained in your head

</v-clicks>

<v-click>

<div class="mt-8 border-2 border-green-500/50 rounded-lg px-6 py-4 bg-green-500/10">

You were already doing the good part. Spring stops you doing it four times.

</div>

</v-click>

---
layout: section
---

# What Spring is

And what it is not

---
layout: statement
---

# Spring is a container

A library called `spring-context`

Everything else is built on top of it

---
layout: default
---

# What Spring is not

<v-clicks>

- **Not an application server.** Tomcat is one. Spring runs inside one, or embeds one
- **Not a web framework.** Spring MVC is a web framework built on the container
- **Not an ORM.** Spring Data JPA is a layer over Hibernate, built on the container
- **Not Spring Boot.** Boot is a set of defaults for configuring the container

</v-clicks>

<v-click>

<div class="mt-8 border-2 border-blue-500/50 rounded-lg px-6 py-4 bg-blue-500/10">

Everything on that list is **built on** the thing this session is about.

Hold that, and **nothing today will feel like magic** — including the parts that are genuinely clever.

</div>

</v-click>

---
layout: default
---

# The container runs without Boot

```java
var context = new AnnotationConfigApplicationContext(AppConfig.class);
LendingService lending = context.getBean(LendingService.class);
```

<v-click>

Two lines. Build a container from a config class, ask it for a bean.

</v-click>

<v-click>

**That is the whole container API.** No Boot, no web server, no properties file.

</v-click>

<v-click>

<div class="mt-6 border-2 border-yellow-500/50 rounded-lg px-6 py-4 bg-yellow-500/10">

We will not write applications this way.

That it is *possible* is the proof that **Boot is a convenience layer, not the framework**.

</div>

</v-click>

---
layout: section
---

# What Spring Boot adds

Four ideas. Three of them you use before the break

---
layout: default
---

# The container is not the hard part

Starting an application is mostly paperwork — and Boot answers all of it with defaults.

| Paperwork | Without Boot | With Boot |
|---|---|---|
| Which versions work together | you choose, and get it wrong | a **parent POM** chooses |
| Which jars for a web app | you research | one **starter** |
| A config class and scan root | you write it | `@SpringBootApplication` |
| A server to run in | install and configure Tomcat | **embedded**, configured |
| Sensible defaults | you write them | **auto-configuration** |

---
layout: default
---

# Starters

<v-clicks>

- A dependency that exists **only to pull in other dependencies**
- `spring-boot-starter-web` contains almost no code of its own
- It declares: Spring MVC, Jackson, a validator, an embedded Tomcat
- At versions known to work together

</v-clicks>

<v-click>

<div class="mt-8 border-2 border-green-500/50 rounded-lg px-6 py-4 bg-green-500/10">

One line in, a working set out.

</div>

</v-click>

---
layout: default
---

# The parent POM

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.0.8</version>
</parent>
```

<v-click>

Remember the `<properties>` block you wrote by hand — one version each for JUnit, AssertJ, Mockito?

</v-click>

<v-click>

<div class="mt-6 border-2 border-green-500/50 rounded-lg px-6 py-4 bg-green-500/10">

This is that block, maintained by the Spring team, for **several hundred libraries**.

</div>

</v-click>

---
layout: default
---

# So your POM names almost no versions

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

<v-click>

No `<version>`. It comes from the parent.

</v-click>

<v-click>

<div class="mt-6 border-2 border-blue-500/50 rounded-lg px-6 py-4 bg-blue-500/10">

That is not the generator being clever.

It is **ordinary Maven dependency management** — and you already know how it works.

</div>

</v-click>

---
layout: default
---

# `@SpringBootApplication` is three annotations

| | What it is | Where you meet it |
|---|---|---|
| `@Configuration` | this class can hold bean definitions | **today**, after the break |
| `@ComponentScan` | scan from this class's package, downwards | **today**, after the break |
| `@EnableAutoConfiguration` | configure from what is on the classpath | the Spring Boot session |

<v-click>

<div class="mt-8 border-2 border-green-500/50 rounded-lg px-6 py-4 bg-green-500/10">

**Two of the three are what this session teaches.**

The third is the genuinely new thing Boot brings, and it gets a session of its own.

</div>

</v-click>

---
layout: default
---

# Auto-configuration, in one slide

<v-clicks>

- Tomcat on the classpath → start a web server on port 8080
- A database driver on the classpath → build a `DataSource`
- Jackson on the classpath → configure JSON conversion

</v-clicks>

<v-click>

<div class="mt-8 border-2 border-yellow-500/50 rounded-lg px-6 py-4 bg-yellow-500/10">

This is the part that can feel like magic — and it is **deliberately not today's lesson**.

It is ordinary `@Configuration` classes carrying an "only if". Taken apart properly later.

</div>

</v-click>

---
layout: statement
---

# Spring is the container

# Boot is a very good default configuration of it

plus a way to start it

It is the **same container** — and you will run the command that proves it after the break

---
layout: section
---

# Generating the project

start.spring.io

---
layout: default
---

# The Spring Initializr

A form that hands you a project.

<v-clicks>

- Pick a build tool, a language, a Boot version, a Java version
- Name the thing
- Tick the dependencies you need
- **Generate** → a zip

</v-clicks>

<v-click>

<div class="mt-8 border-2 border-blue-500/50 rounded-lg px-6 py-4 bg-blue-500/10">

Everything it writes is something you can already read. That is why it comes now and not earlier.

</div>

</v-click>

---
layout: default
---

# The form, field by field

| Field | Pick | Why |
|---|---|---|
| **Project** | Maven | You wrote a POM by hand. Stay on the tool you can read |
| **Language** | Java | — |
| **Spring Boot** | **4.0.8** | Pinned for this course. No `SNAPSHOT`, `M` or `RC` |
| **Group** | `com.sunrise` | Reversed domain, exactly as in the Maven session |
| **Artifact** | `lending-desk` | Becomes the folder and the jar |
| **Packaging** | Jar | The app embeds its own server |
| **Java** | **25** | Match your JDK, or nothing compiles |

---
layout: default
---

# One field matters more than it looks

<div class="mt-8 border-2 border-blue-500/50 rounded-lg px-6 py-4 bg-blue-500/10">

## Package name

`com.sunrise.lendingdesk`

</div>

<v-click>

<div class="mt-8">

Remember that string.

We come back to it in five minutes, and it explains most "why is my bean not found" questions you will ever ask.

</div>

</v-click>

---
layout: default
---

# Dependencies for today

<div class="mt-10 text-center">

<div class="inline-block border-2 border-green-500/50 rounded-lg px-12 py-8 bg-green-500/10">

## None

</div>

</div>

<v-click>

<div class="mt-8">

The lending desk is a console application. No web layer, no database.

The base project already contains **the container** and **the test support**.

</div>

</v-click>

---
layout: statement
---

# Tick nothing you cannot justify out loud

---
layout: default
---

# Why that rule is not pedantry

<v-clicks>

- An unticked box costs nothing
- **A ticked box is a pile of auto-configuration you did not ask for**
- Tick Spring Web today and a web server starts, on a port, for no reason

</v-clicks>

<v-click>

<div class="mt-8 border-2 border-green-500/50 rounded-lg px-6 py-4 bg-green-500/10">

For the rest of your career: if you cannot say in one sentence what a dependency is for, do not add it.

</div>

</v-click>

---
layout: default
---

# Generate, unzip, open

<v-clicks>

1. **Generate** — you get a zip
2. Unzip it somewhere sensible
3. Open the **`pom.xml`**

</v-clicks>

<v-click>

<div class="mt-8 border-2 border-red-500/50 rounded-lg px-6 py-4 bg-red-500/10">

**Open the `pom.xml`, not the folder.**

Open the folder and you get text files with no run buttons and no red underlines.

This has cost more class time than any other single mistake in this course.

</div>

</v-click>

---
layout: default
---

# What the generator wrote — the POM

```xml
<parent>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.0.8</version>
</parent>

<properties>
    <java.version>25</java.version>
</properties>

<dependencies>
    <dependency>
        <artifactId>spring-boot-starter</artifactId>
    </dependency>
    <dependency>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---
layout: default
---

# Read it against the one you wrote by hand

<v-clicks>

- **One `<properties>` entry** — and it is the Java version
- **Two dependencies** — and neither names a version
- **`spring-boot-starter-test` is `<scope>test</scope>`** — same scope as JUnit, same reason

</v-clicks>

<v-click>

<div class="mt-8 border-2 border-green-500/50 rounded-lg px-6 py-4 bg-green-500/10">

Nothing here is new. It is the POM you already know, with the version numbers moved upstairs.

</div>

</v-click>

---
layout: default
---

# Demo — where is the container?

```bash
mvn dependency:tree
```

<v-click>

Look for **`spring-context`**. It is in there, several levels down, under `spring-boot-starter`.

</v-click>

<v-click>

<div class="mt-6 border-2 border-green-500/50 rounded-lg px-6 py-4 bg-green-500/10">

**Boot did not replace the container. It depends on it.**

</div>

</v-click>

<v-click>

<div class="mt-6 text-sm opacity-80">

That one command is also the answer to "where did this class on my classpath come from?" for the rest of your career.

</div>

</v-click>

---
layout: default
---

# The application class

```java
package com.sunrise.lendingdesk;

@SpringBootApplication
public class LendingDeskApplication {
    public static void main(String[] args) {
        SpringApplication.run(LendingDeskApplication.class, args);
    }
}
```

<v-click>

<div class="mt-6">

Still a `main` method. Still the entry point.

It just **starts a container** instead of building objects.

</div>

</v-click>

---
layout: statement
---

# The root package rule

The application class sits at the top

Everything else lives underneath it

---
layout: default
---

# Why — and it is the string from the form

`@SpringBootApplication` includes `@ComponentScan` **with no argument**.

<v-click>

Which means: *scan the package this class is in, and everything under it.*

</v-click>

<v-click>

<div class="mt-6">

| Class | Found? |
|---|---|
| `com.sunrise.lendingdesk.service.LendingService` | ✅ |
| `com.sunrise.lendingdesk.repository.InMemoryBookRepository` | ✅ |
| `com.sunrise.other.Whatever` | ❌ |

</div>

</v-click>

---
layout: default
---

# And nothing tells you

<v-clicks>

- The context still starts
- Most of the application works
- One bean is silently absent — until something asks for it

</v-clicks>

<v-click>

```
NoSuchBeanDefinitionException: No qualifying bean of type
'com.sunrise.lendingdesk.repository.BookRepository' available
```

</v-click>

<v-click>

<div class="mt-6 border-2 border-red-500/50 rounded-lg px-6 py-4 bg-red-500/10">

**Not one word about packages.** Nobody guesses this the first time. Everybody recognises it the second.

</div>

</v-click>

---
layout: statement
---

# You move the application class

# one package down

Which beans survive?

---
layout: default
---

# The rest of what you got

<v-clicks>

- **`application.properties`** — empty, and that is fine. Configuration goes here; the Boot session fills it
- **`LendingDeskApplicationTests`** — one test, `@SpringBootTest`, empty body
- **`mvnw` / `mvnw.cmd`** — the Maven wrapper. Builds on a machine with no Maven installed

</v-clicks>

<v-click>

<div class="mt-8 border-2 border-blue-500/50 rounded-lg px-6 py-4 bg-blue-500/10">

That empty test is **not pointless**. `@SpringBootTest` starts the whole container —

so a context that will not start **fails it**. It is a smoke test for your wiring.

</div>

</v-click>

---
layout: default
---

# Demo — run it before writing anything

```bash
mvn test
mvn spring-boot:run
```

<v-clicks>

- A banner
- A line saying how long startup took
- And then it exits

</v-clicks>

<v-click>

<div class="mt-6 border-2 border-green-500/50 rounded-lg px-6 py-4 bg-green-500/10">

**That is not a failure.** With no web starter there is nothing to keep it alive.

The container started, found nothing to do, and shut down cleanly.

</div>

</v-click>

---
layout: center
---

# Break

10 minutes

---
layout: section
---

# Registering beans

The container can only build what it knows about

---
layout: statement
---

# Can you edit the class?

There are exactly two ways, and one question chooses between them.

**Yes** → a stereotype **No** → a `@Bean` method

---
layout: default
---

# `@Component`

```java
@Component
public class EmailNotificationSender implements NotificationSender { }
```

<v-click>

<div class="mt-6">

One annotation. The class is now something the container knows how to build.

</div>

</v-click>

---
layout: default
---

# The stereotypes

| Annotation | Says | Also does |
|---|---|---|
| `@Component` | a bean | — |
| `@Service` | business logic | nothing, technically |
| `@Repository` | talks to a data store | translates persistence exceptions |
| `@Controller` | handles web requests | is found by MVC's handler mapping |

---
layout: default
---

# Demo — open `@Service` in the IDE

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component                      // ← there it is
public @interface Service { }
```

<v-click>

So is `@Repository`. So is `@Controller`.

</v-click>

<v-click>

<div class="mt-6 border-2 border-blue-500/50 rounded-lg px-6 py-4 bg-blue-500/10">

A **meta-annotation** — Spring's word for an annotation that carries another one.

To the container, all four are the same thing.

</div>

</v-click>

---
layout: default
---

# So why do the other three exist?

<v-clicks>

**1. A reader learns the layer from the annotation.**

`@Repository` says "this is the edge of the application" before you read a line of it. Worth a lot in a codebase with four hundred classes.

**2. Two of them genuinely change behaviour.**

The right-hand column of that table. Exception translation matters in the persistence course; handler mapping matters in the MVC session.

**And when nothing fits, use `@Component`.**

A `NotificationSender` is not a service, a repository or a controller. Forcing a class into the wrong word is worse than the generic annotation.

</v-clicks>

---
layout: default
---

# Every bean has a name

```java
@Component
public class SmsNotificationSender { }        // bean name: "smsNotificationSender"

@Component("sms")
public class SmsNotificationSender { }        // bean name: "sms"
```

<v-click>

The default is the simple class name, first letter lower-cased.

</v-click>

<v-click>

<div class="mt-6 border-2 border-yellow-500/50 rounded-lg px-6 py-4 bg-yellow-500/10">

Notice this now. You will need it when two beans implement one interface and you have to say which one you meant.

</div>

</v-click>

---
layout: default
---

# Scanning already happens

<v-clicks>

- There is no `@ComponentScan` in your code
- There does not need to be — `@SpringBootApplication` carries it
- Rooted at its own package

</v-clicks>

<v-click>

<div class="mt-8 border-2 border-green-500/50 rounded-lg px-6 py-4 bg-green-500/10">

So the only thing you have to get right is **where the class lives**.

</div>

</v-click>

---
layout: default
---

# Demo — watch a bean appear and vanish

```java
@Bean
CommandLineRunner names(ApplicationContext ctx) {
    return args -> Arrays.stream(ctx.getBeanDefinitionNames()).sorted()
        .forEach(System.out::println);
}
```

<v-clicks>

1. Annotate a throwaway class inside the tree → it is in the list
2. Move it to `com.sunrise.elsewhere` → it is gone, with **no error**
3. Ask for it → now read the failure

</v-clicks>

<v-click>

<div class="mt-4 border-2 border-blue-500/50 rounded-lg px-6 py-4 bg-blue-500/10">

**`CommandLineRunner`** — a bean that runs once, after the container is fully built. It is how a console application does its work, and you need it in the exercise.

</div>

</v-click>

---
layout: default
---

# `@Bean` — for classes you did not write

Scanning finds classes you were able to annotate. That leaves out everything else.

<v-clicks>

- a `DataSource`
- an `ObjectMapper`
- an HTTP client
- **anything from a jar**

</v-clicks>

---
layout: default
---

# A `@Bean` method

```java
@Configuration
public class AuditConfig {

    @Bean
    public AuditLog auditLog() {
        return new AuditLog("sunrise-library");
    }
}
```

<v-click>

<div class="mt-6 border-2 border-green-500/50 rounded-lg px-6 py-4 bg-green-500/10">

You wrote `new`, and that is **correct**.

</div>

</v-click>

---
layout: statement
---

# The rule was never "never call `new`"

It is: call it in **one place, deliberately**,

instead of everywhere by accident

---
layout: default
---

# Three details worth memorising

<v-clicks>

- **The method name is the bean name.** Rename the method, rename the bean, break anything that asked for it by name
- **The return type is what others match on.** Declare the interface, not the implementation, when you want the freedom to change it
- **A `@Bean` method can take parameters**, and the container fills them from other beans

</v-clicks>

---
layout: default
---

# Where do `@Bean` methods live?

<v-clicks>

- The application class **is** a `@Configuration` class — they work there
- A separate, named config class is easier to find
- `AuditConfig`, `ClientConfig`, `SecurityConfig` — one concern each

</v-clicks>

<v-click>

<div class="mt-8 text-sm opacity-80">

Boot removed the config class you *had* to write. It did not remove the ones worth writing.

</div>

</v-click>

---
layout: default
---

# One thing about `@Configuration`

```java
@Configuration
public class DataConfig {

    @Bean
    public DataSource dataSource() { return new HikariDataSource(); }

    @Bean
    public BookRepository books() {
        return new JdbcBookRepository(dataSource());   // a second one?
    }
}
```

<v-click>

<div class="mt-4">

**No.** Spring subclasses your config class at runtime and intercepts that call.

The second one returns the bean that already exists.

</div>

</v-click>

---
layout: default
---

# Remember that

<v-clicks>

- It is a real behaviour you would otherwise misread
- And that interception is the same mechanism behind `@Transactional`, `@Cacheable`, `@PreAuthorize`

</v-clicks>

<v-click>

<div class="mt-8 border-2 border-blue-500/50 rounded-lg px-6 py-4 bg-blue-500/10">

The container is quietly wrapping your objects in subclasses of themselves.

**The next session is entirely about how that works — and where it breaks.**

</div>

</v-click>

---
layout: section
---

# How the container fills a dependency

Constructor, setter, field — and they are not equivalent

---
layout: default
---

# Constructor injection

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

---
layout: default
---

# Notice what is not there

<div class="mt-8 text-center text-3xl">

No <code>@Autowired</code>

</div>

<v-clicks>

- A class with **exactly one constructor** has already told Spring everything
- Since Spring 4.3, that constructor is used automatically
- Write a second constructor and you must annotate the one you mean

</v-clicks>

<v-click>

<div class="mt-6 text-sm opacity-80">

That is the only situation where `@Autowired` on a constructor earns its place.

</div>

</v-click>

---
layout: default
---

# Three arguments, in order of weight

<v-clicks>

**1. If it compiles, every dependency is present.**

There is no state in which a `LendingService` exists without a repository. Not "should not" — *cannot*.

**2. The fields are `final`.**

Nothing reassigns them after construction. Safe to share between threads — which matters enormously in about ten minutes.

**3. It is testable with `new`.**

`new LendingService(fakeRepo, fakeSender, log)`. No container, no reflection. Last session's tests work unchanged.

</v-clicks>

---
layout: default
---

# Setter injection

```java
@Autowired
public void setNotifier(NotificationSender notifier) {
    this.notifier = notifier;
}
```

<v-clicks>

- Legal, and occasionally right — for a **genuinely optional** dependency
- Cost: a window where the object exists and is not ready
- Cost: the field cannot be `final`

</v-clicks>

<v-click>

<div class="mt-6 border-2 border-yellow-500/50 rounded-lg px-6 py-4 bg-yellow-500/10">

If you cannot name the case where the dependency is absent, you do not have an optional dependency.

</div>

</v-click>

---
layout: default
---

# Field injection

```java
@Service
public class LendingService {
    @Autowired private BookRepository books;      // don't
}
```

<v-click>

<div class="mt-8">

It is shorter.

**That is the entire case for it.**

</div>

</v-click>

---
layout: default
---

# And it loses on three counts

<v-clicks>

- **It hides dependencies.** The constructor is the class's public statement of what it needs. This deletes that statement — and nothing stops the list growing to fifteen, because nothing makes it uncomfortable
- **The field cannot be `final`.** The compiler stops helping
- **It is untestable without a container.** No constructor to pass anything to. Start Spring, or poke the field with reflection

</v-clicks>

<v-click>

<div class="mt-6 border-2 border-red-500/50 rounded-lg px-6 py-4 bg-red-500/10">

**A class that is hard to test is usually badly wired** — and field injection makes a badly wired class *feel* fine.

</div>

</v-click>

---
layout: statement
---

# "Fifteen dependencies, so a constructor would be unreadable"

Two things are wrong with that argument.

Name both.

---
layout: default
---

# The `final` field is the tell

You can review this from across the room.

<div class="grid grid-cols-2 gap-6 mt-8">

<div class="border-2 border-green-500/50 rounded-lg px-6 py-4 bg-green-500/10">

Fields are `final`

**→ constructor injection**

</div>

<v-click>

<div class="border-2 border-yellow-500/50 rounded-lg px-6 py-4 bg-yellow-500/10">

Fields are not `final`

**→ something else is going on**

and it is worth asking why

</div>

</v-click>

</div>

---
layout: default
---

# Circular dependencies

`A` needs `B`. `B` needs `A`.

<v-click>

```
BeanCurrentlyInCreationException:
Error creating bean with name 'a': Requested bean is currently in creation
```

</v-click>

<v-click>

<div class="mt-6 border-2 border-green-500/50 rounded-lg px-6 py-4 bg-green-500/10">

**That is a feature.**

A cycle is two classes that cannot be understood, tested or reused apart.

</div>

</v-click>

---
layout: default
---

# Fail at startup, not in three years

<v-clicks>

- Field injection can paper over a cycle — and then it survives for three years
- Failing at startup is the cheapest possible moment to find out
- The fix is a **design** fix: pull the shared part into a third class, or make one direction an event

</v-clicks>

---
layout: section
---

# Scopes and lifecycle

How many should exist, and when does anything get to run

---
layout: default
---

# Singleton — one instance per container, per bean name

Created **eagerly**, at startup. And it does not mean what you think.

<v-clicks>

- **Not** the Singleton pattern. No private constructor, no `getInstance()`. The class is ordinary; the container simply makes one
- **One per container**, not one per JVM. Two contexts, two instances
- **Per bean name**, not per class. Register the same class twice under two names and you get two instances

</v-clicks>

---
layout: default
---

# The bug this causes

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

<v-click>

<div class="mt-4 border-2 border-red-500/50 rounded-lg px-6 py-4 bg-red-500/10">

One `LendingService`. Every request. Many threads at once.

**Shared, unsynchronised, mutable state.**

</div>

</v-click>

<v-click>

<div class="mt-4 text-center text-xl">

It **will** be wrong. Not always, not reproducibly — the worst kind of wrong.

</div>

</v-click>

---
layout: default
---

# The rule

<div class="mt-6 border-2 border-green-500/50 rounded-lg px-6 py-4 bg-green-500/10">

A singleton bean must be **stateless**, or its state must be **immutable or thread-safe**.

</div>

<div class="grid grid-cols-2 gap-6 mt-8">

<div>

<v-click>

## Fine

Collaborators in `final` fields — set once, never change

</v-click>

</div>

<div>

<v-click>

## Not fine

A counter · a "current user" · a `StringBuilder` · a cached last result

</v-click>

</div>

</div>

---
layout: default
---

# This is the commonest Spring bug that is not an exception

<v-clicks>

- Nothing throws
- Nothing logs
- The tests pass, because tests are single-threaded

</v-clicks>

<v-click>

<div class="mt-8 border-2 border-green-500/50 rounded-lg px-6 py-4 bg-green-500/10">

Constructor injection with `final` fields is most of the protection —

it makes the safe thing the **easy** thing.

</div>

</v-click>

---
layout: statement
---

# Which of these are safe on a singleton?

`final BookRepository books` · `final List<String> auditTrail = new ArrayList<>()`

`String currentMember` · `final AtomicInteger lentToday`

---
layout: default
---

# Eager, and why that is good

Singletons are built **at startup**, not on first use.

<v-clicks>

- A missing bean → fails at startup
- A typo in a qualifier → fails at startup
- A broken `@Bean` method → fails at startup

</v-clicks>

<v-click>

<div class="mt-6 border-2 border-green-500/50 rounded-lg px-6 py-4 bg-green-500/10">

In front of whoever started it — instead of at 3am, on the one path nobody exercised.

</div>

</v-click>

---
layout: default
---

# `@Lazy` turns that off

<v-clicks>

- Occasionally right — an expensive object most runs never touch
- You are trading a **startup** failure for a **runtime** one
- So do it on purpose

</v-clicks>

<v-click>

<div class="mt-8 border-2 border-blue-500/50 rounded-lg px-6 py-4 bg-blue-500/10">

This is also why the empty `@SpringBootTest` in your project is not a pointless test.

It starts the container, so it fails the moment your wiring is wrong.

</div>

</v-click>

---
layout: default
---

# `prototype`

```java
@Component
@Scope("prototype")
public class LoanTicketPrinter { }
```

<v-click>

A new instance **every time somebody asks**.

</v-click>

<v-click>

<div class="mt-6">

Use it when the bean holds per-use state — a builder, a printer, a per-request accumulator.

</div>

</v-click>

---
layout: default
---

# Two things that surprise everyone

<v-clicks>

**The container stops caring once it hands the object over.**

It creates the prototype, injects it, runs `@PostConstruct` — and forgets it.
**`@PreDestroy` never runs on a prototype.** If it holds a resource, closing it is yours.

**Marking the class is not enough.**

Which is the next slide, and it is the best fifteen minutes of the session.

</v-clicks>

---
layout: statement
---

# The prototype-in-a-singleton trap

---
layout: default
---

# Predict what this does

```java
@Service
public class TicketDesk {
    private final LoanTicketPrinter printer;

    public TicketDesk(LoanTicketPrinter printer) {
        this.printer = printer;
    }

    public String issue(String isbn, String member) {
        return printer.print(isbn, member);
    }
}
```

<div class="mt-4">

`LoanTicketPrinter` is `@Scope("prototype")`. Two calls to `issue(...)`.

**How many printers?**

</div>

---
layout: default
---

# One. Forever.

<v-clicks>

- It compiles
- The context starts
- **No warning of any kind**
- Every ticket comes out of the same printer

</v-clicks>

<v-click>

<div class="mt-6 border-2 border-yellow-500/50 rounded-lg px-6 py-4 bg-yellow-500/10">

Nothing is broken. The prototype scope was honoured perfectly — the container was asked for a printer exactly once, made a new one as instructed, and was never asked again.

**The injection point is wrong.**

</div>

</v-click>

---
layout: statement
---

# A singleton's dependencies are injected once

when the singleton is built

and that was the only time anybody asked for a printer

---
layout: default
---

# Inject the ability to get one

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

---
layout: default
---

# `ObjectProvider<T>`

<v-clicks>

- An ordinary field of an ordinary type
- Taken through the constructor like everything else
- `getObject()` goes back to the container **each time**

</v-clicks>

<v-click>

<div class="mt-8 border-2 border-green-500/50 rounded-lg px-6 py-4 bg-green-500/10">

Nothing new to learn. Same constructor, same `final` field, one different type.

</div>

</v-click>

---
layout: default
---

# There are two other fixes

<v-clicks>

- `@Lookup` on an abstract method
- A scoped proxy: `@Scope(value = "prototype", proxyMode = TARGET_CLASS)`

</v-clicks>

<v-click>

Both work.

</v-click>

<v-click>

<div class="mt-6 border-2 border-green-500/50 rounded-lg px-6 py-4 bg-green-500/10">

`ObjectProvider` is the one to reach for: **it says what it is on the page**,

and the next person to read the class does not have to know a trick.

</div>

</v-click>

<v-click>

<div class="mt-4 text-center text-xl">

**You will write this wrong on purpose in the exercise.** Two minutes, two identical serials on screen, and it stays learned.

</div>

</v-click>

---
layout: default
---

# The web scopes

| Scope | One per |
|---|---|
| `request` | HTTP request |
| `session` | user session |
| `application` | servlet context |

<v-click>

<div class="mt-8">

They only exist in a web application, and they are the right answer surprisingly rarely.

A request-scoped bean injected into a singleton needs a scoped proxy — and by the time you have explained that to a colleague, passing the value as a method argument would have been simpler.

</div>

</v-click>

---
layout: default
---

# Lifecycle, for a singleton

<v-clicks>

1. **instantiate** — the constructor runs, dependencies arrive
2. **`@PostConstruct`** — your hook, after the object is fully built
3. **in use** — for the life of the application
4. **`@PreDestroy`** — on orderly shutdown
5. **destroyed**

</v-clicks>

---
layout: default
---

# The callbacks

```java
@Repository
public class InMemoryBookRepository implements BookRepository {

    @PostConstruct
    void seed() { ... }

    @PreDestroy
    void flush() { ... }
}
```

<v-click>

<div class="mt-6 border-2 border-yellow-500/50 rounded-lg px-6 py-4 bg-yellow-500/10">

These are **`jakarta.annotation`** annotations, not Spring ones.

A Java standard that Spring honours. Your IDE will happily offer you a wrong import.

</div>

</v-click>

---
layout: default
---

# What actually goes in `@PostConstruct`?

<v-clicks>

- With constructor injection, the dependencies are **already there** when the constructor runs
- So the classic reason for this hook does not apply to you
- What is left is real but narrow

</v-clicks>

<v-click>

<div class="mt-6">

Work too heavy or too failure-prone for a constructor:

**loading seed data · warming a cache · validating that a config combination makes sense**

</div>

</v-click>

<v-click>

<div class="mt-6 border-2 border-green-500/50 rounded-lg px-6 py-4 bg-green-500/10">

If it is just `this.x = x`, it belongs in the constructor.

</div>

</v-click>

---
layout: statement
---

# Can you rely on `@PreDestroy`?

It does not run on a prototype.

It does not run on `SIGKILL`.

---
layout: section
---

# Two beans, one interface

The bill for every interface you wrote in the first session

---
layout: default
---

# Two senders, one injection point

```java
public LendingService(BookRepository books, NotificationSender notifier, AuditLog audit)
```

<v-click>

```
NoUniqueBeanDefinitionException: expected single matching bean but found 2:
emailNotificationSender, smsNotificationSender
```

</v-click>

<v-click>

<div class="mt-6 border-2 border-green-500/50 rounded-lg px-6 py-4 bg-green-500/10">

**Read that message. It names both candidates.**

Nine times out of ten it tells you immediately which one you forgot.

</div>

</v-click>

<v-click>

<div class="mt-4 text-center text-xl">

Spring's exceptions are unusually good — and almost nobody reads past the first line.

</div>

</v-click>

---
layout: default
---

# Three answers to three different questions

<div class="grid grid-cols-3 gap-4 mt-10">

<div class="border-2 border-blue-500/50 rounded-lg px-4 py-6 bg-blue-500/10">

**`@Primary`**

"this is the default"

</div>

<v-click>

<div class="border-2 border-blue-500/50 rounded-lg px-4 py-6 bg-blue-500/10">

**`@Qualifier`**

"this injection point wants that one"

</div>

</v-click>

<v-click>

<div class="border-2 border-blue-500/50 rounded-lg px-4 py-6 bg-blue-500/10">

**`List<T>`**

"give me all of them"

</div>

</v-click>

</div>

---
layout: default
---

# `@Primary`

```java
@Component
@Primary
public class EmailNotificationSender implements NotificationSender { }
```

<v-clicks>

- One bean per type may be primary
- Every injection point that does not say otherwise gets it
- Use it when there genuinely **is** a normal choice and the others are exceptions

</v-clicks>

<v-click>

<div class="mt-6 border-2 border-red-500/50 rounded-lg px-6 py-4 bg-red-500/10">

Do not use it to silence an error you have not thought about.

**A `@Primary` chosen at random is a wiring bug that starts cleanly.**

</div>

</v-click>

---
layout: default
---

# `@Qualifier`

```java
public LendingService(@Qualifier("smsNotificationSender") NotificationSender notifier)
```

<v-click>

The string is a **bean name** — which is why it was worth knowing where bean names come from.

</v-click>

<v-click>

<div class="mt-6">

Use it when the choice belongs to the **caller** rather than to the type. It is the honest option when there is no sensible default.

</div>

</v-click>

<v-click>

<div class="mt-6 text-sm opacity-80">

Writing the same qualifier at ten injection points is the signal that `@Primary` was the right tool.

</div>

</v-click>

---
layout: default
---

# Resolution by parameter name

```java
public LendingService(NotificationSender smsNotificationSender) { ... }
```

<v-click>

This also works. Spring falls back to matching the parameter name against the bean name.

</v-click>

<v-click>

<div class="mt-6 border-2 border-red-500/50 rounded-lg px-6 py-4 bg-red-500/10">

**Fragile.** Renaming a parameter — a rename any IDE will do without comment — silently rewires the application.

</div>

</v-click>

<v-click>

<div class="mt-6">

Know it so you can recognise it in somebody else's code. **Do not write it.**

</div>

</v-click>

---
layout: default
---

# `List<T>` — all of them

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

---
layout: default
---

# Look at what that field asks for

<v-clicks>

- Not a `NotificationSender`
- A **`List`** of them

</v-clicks>

<v-click>

<div class="mt-8 border-2 border-blue-500/50 rounded-lg px-6 py-4 bg-blue-500/10">

The container reads that as *"every bean of this type"*.

It does **not** go hunting for a bean that happens to be a `List`.

</div>

</v-click>

---
layout: statement
---

# This is the Strategy pattern

and you did not write any of the plumbing

---
layout: default
---

# No plumbing at all

<v-clicks>

- No registry · no `Map` in a static initialiser · no `if` chain
- Write a third sender, annotate it, and `BroadcastNotifier` picks it up **without being edited**

</v-clicks>

<v-click>

<div class="mt-8 border-2 border-green-500/50 rounded-lg px-6 py-4 bg-green-500/10">

The open/closed principle, showing up as a language feature.

**Every interface you wrote in the first session was for this.**

</div>

</v-click>

---
layout: default
---

# Three details

<v-clicks>

- **`Map<String, NotificationSender>`** gives you bean name → bean. What you want when the choice is *data* ("the user picked `sms`") rather than code
- **Order is not guaranteed** unless you ask. `@Order(1)`, `@Order(2)`, or implement `Ordered`
- **An empty list is not an error.** If zero senders would be a bug, say so in the constructor — the container will not say it for you

</v-clicks>

<v-click>

<div class="mt-6 text-sm opacity-80">

When order matters and nobody said so, you have a bug that works on your machine.

</div>

</v-click>

---
layout: default
---

# Optional dependencies

```java
public LendingService(ObjectProvider<AuditLog> audit) {
    this.audit = audit.getIfAvailable(NullAuditLog::new);
}
```

<v-click>

`ObjectProvider` again, for a different reason: **"one if there is one"**.

</v-click>

<v-click>

<div class="mt-6 border-2 border-yellow-500/50 rounded-lg px-6 py-4 bg-yellow-500/10">

`@Autowired(required = false)` does something similar and leaves you a `null` field —

a step backwards from the `Optional` habits of the last session.

</div>

</v-click>

---
layout: section
---

# Exercise

Build the lending desk

---
layout: default
---

# What you have and what you write

<div class="grid grid-cols-2 gap-6 mt-6">

<div>

## You have

An application class

An empty properties file

One empty test

</div>

<div>

<v-click>

## You write

**Everything else**

No skeleton. No `TODO`s. No code to copy.

</v-click>

</div>

</div>

<v-click>

<div class="mt-8 border-2 border-blue-500/50 rounded-lg px-6 py-4 bg-blue-500/10">

**Seven checkpoints.** For each one the README gives the class, the package, the responsibility, the signatures other steps need — and the **exact console output** you should see.

</div>

</v-click>

---
layout: default
---

# The comparison is the grade

<v-clicks>

- Run the application after each checkpoint
- Compare what you see against what the README says you should see
- There is no test suite handing you a number

</v-clicks>

<v-click>

<div class="mt-8 border-2 border-yellow-500/50 rounded-lg px-6 py-4 bg-yellow-500/10">

This is harder than the last four sessions, deliberately.

**In a real project nobody leaves the blanks for you.**

</div>

</v-click>

---
layout: default
---

# Do checkpoint 7 wrong on purpose

<v-clicks>

- The README tells you how
- Write the obvious field first
- Watch two tickets come out with the **same serial number**
- Then swap in `ObjectProvider` and watch it change

</v-clicks>

<v-click>

<div class="mt-8 border-2 border-green-500/50 rounded-lg px-6 py-4 bg-green-500/10">

Reading about it is not the same as watching it. It is the best two minutes in the exercise.

</div>

</v-click>

---
layout: default
---

# Before you ask for help

<v-clicks>

- The README has an **If you get stuck** table
- Organised by *what you see on screen*
- Every row was produced by actually breaking a working solution

</v-clicks>

<v-click>

<div class="mt-8 border-2 border-blue-500/50 rounded-lg px-6 py-4 bg-blue-500/10">

Half the value of today is learning to read Spring's failures. Read the table first.

</div>

</v-click>

---
layout: section
---

# Homework

The catalogue service

---
layout: default
---

# A second project, generated from scratch

<v-clicks>

- A different slice of the library, so a bad afternoon does not follow you home
- **Six checkpoints**, and much less hand-holding
- The README names the classes and the expected output, and stops

</v-clicks>

<v-click>

<div class="mt-8 border-2 border-green-500/50 rounded-lg px-6 py-4 bg-green-500/10">

**Generating it yourself for the second time is the point of the first step.**

Ninety seconds, and it is the skill that outlives everything else today.

</div>

</v-click>

---
layout: default
---

# Two things that appear only there

<v-clicks>

- **`@Order`** — controlling the order of an injected `List`
- **Two candidates, neither `@Primary`** — so `@Qualifier` is the only way through

</v-clicks>

<v-click>

<div class="mt-8">

And three ungraded investigations, which are the part worth doing:

read a real `NoUniqueBeanDefinitionException` · move the application class and watch the scan break · find `spring-context` in `mvn dependency:tree`

</div>

</v-click>

---
layout: section
---

# Recap

---
layout: default
---

# The problem and the idea

<v-clicks>

- **The problem** is not typing `new`. It is that your object graph exists only as instructions in a method, written out several times, with no way to check the copies agree
- **IoC** is a sentence about who calls whom. **DI** is the mechanism that delivers it
- **The `ApplicationContext` is a `Map<String, Object>`** with very good rules about how the values get made

</v-clicks>

---
layout: default
---

# Spring and Boot

<v-clicks>

- **Spring is the container. Boot is a very good default configuration of it**, plus a way to start it
- **Boot is four ideas:** starters, a parent POM that owns the versions, `@SpringBootApplication`, auto-configuration
- **`@SpringBootApplication`** = `@Configuration` + `@ComponentScan` + `@EnableAutoConfiguration` — and two of the three are what today taught
- **The root package rule:** application class at the top, everything else underneath
- **Tick nothing on the Initializr you cannot justify out loud**

</v-clicks>

---
layout: default
---

# Beans

<v-clicks>

- **Two ways to register one**, and the question is "can I edit this class?" — a stereotype if yes, a `@Bean` method if no
- **The stereotypes are all `@Component`.** Two also do something; all four tell a reader which layer they are in
- **Constructor injection** — if it compiles every dependency is present, the fields can be `final`, and the class is testable with `new`

</v-clicks>

---
layout: default
---

# Scope and ambiguity

<v-clicks>

- **Singleton means one per container per bean name**, built eagerly. Its state must be immutable or thread-safe
- **A prototype injected into a singleton is injected once.** `ObjectProvider` is the fix
- **`@PostConstruct` is for work too heavy for a constructor**, not for wiring. `@PreDestroy` never runs on a prototype
- **Ambiguity has three answers:** `@Primary`, `@Qualifier`, `List<T>` — and the last one is the Strategy pattern delivered by the container

</v-clicks>

---
layout: section
---

# Looking ahead

---
layout: default
---

# Something intercepted an ordinary method call

```java
@Bean
public BookRepository books() {
    return new JdbcBookRepository(dataSource());   // did not build a second one
}
```

<v-clicks>

- Spring subclassed your config class at runtime
- The same mechanism commits a database transaction around a method that says nothing about transactions

</v-clicks>

<v-click>

<div class="mt-6 border-2 border-blue-500/50 rounded-lg px-6 py-4 bg-blue-500/10">

The next session is entirely about how that interception works — and about the one situation where it silently does nothing at all.

**Bring your `lending-desk` project.**

</div>

</v-click>
