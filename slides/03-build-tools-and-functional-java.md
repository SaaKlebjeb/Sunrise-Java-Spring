---
layout: section
---

# Build Tools and Functional Java

Maven · Gradle · lambdas · streams

---
layout: default
---

# Learning Objectives

<v-clicks>

- Say what a **build tool** is for, and why a folder of `.java` files stops scaling
- Read a `pom.xml` — coordinates, dependencies, **scope**, properties, plugins
- Name the **lifecycle** phases in order, and say what `mvn test` does that `mvn compile` does not
- Read `mvn dependency:tree`, and explain where a library nobody asked for came from
- Say in one sentence what **Gradle** does differently
- Replace an anonymous inner class with a **lambda**, and say where it got its type
- Use `Predicate`, `Function`, `Consumer`, `Supplier` — and write your own
- Turn a loop into a **stream pipeline**, and say why nothing runs until the end
- Group a list into a `Map` with **`groupingBy`** — and name the case where the loop wins

</v-clicks>

---
layout: statement
---

# Two halves today

The second one needs the first.

Your exercise is a **real Maven project**.

---
layout: section
---

# Why you have no build file yet

---
layout: default
---

# Two sessions, one folder

```
session-02/
├── Order.java
├── PaymentResult.java
├── OrderReporter.java
└── Verify.java
```

<v-click>

Compile them together. Run one. It works.

</v-click>

<v-click>

<div class="mt-6 border-l-4 border-green-500 pl-4">

This is a real and honest way to write Java.

It stops working the day you need one of **three** things.

</div>

</v-click>

---
layout: default
---

# One — somebody else's code

Every real project uses libraries. JSON. HTTP. A database driver. Tests.

<v-click>

A library arrives as a `.jar`. The compiler only knows about a jar if it is on the **classpath**:

```
javac -cp lib/gson-2.11.jar:lib/junit-5.11.jar:lib/opentest4j-1.3.jar OrderService.java
```

</v-click>

<v-click>

<div class="mt-6 grid grid-cols-3 gap-3 text-sm">
<div class="border border-yellow-500/50 rounded px-3 py-2 bg-yellow-500/10">It's long</div>
<div class="border border-orange-500/50 rounded px-3 py-2 bg-orange-500/10">It's different on Windows — <code>;</code> not <code>:</code></div>
<div class="border border-red-500/50 rounded px-3 py-2 bg-red-500/10">You had to <b>find and download all three</b></div>
</div>

</v-click>

---
layout: default
---

# And you never asked for that third one

```
lib/gson-2.11.jar
lib/junit-5.11.jar
lib/opentest4j-1.3.jar     ← what is this?
```

<v-click>

<div class="mt-6 text-center text-lg">

JUnit needs it.

You have never heard of it, and your build breaks without it.

</div>

</v-click>

---
layout: default
---

# Two — a build that is the same everywhere

<v-clicks>

- Compile
- Run the tests
- Package a jar
- Stamp a version on it

</v-clicks>

<v-click>

On your machine. On a colleague's machine. On a build server at 3am with nobody watching.

</v-click>

<v-click>

<div class="mt-6 border-2 border-red-500/50 rounded-lg px-6 py-4 bg-red-500/10">

**"It works on my machine"** is a bug report.

Its root cause is almost always a difference in one of those jars.

</div>

</v-click>

---
layout: default
---

# Three — every project looking the same

<div class="grid grid-cols-2 gap-6 mt-4">

<div>

## Where is the code?

```
src/main/java
```

<v-click>

<div class="mt-4 text-sm opacity-80">

You did not have to ask. On any Java project you will ever join.

</div>

</v-click>

</div>

<div>

<v-click>

## The alternative

```
source/
java/
code/
code_v2/
code_v2_FINAL/
```

<div class="mt-4 text-sm opacity-80">

Every project a small mystery.

</div>

</v-click>

</div>

</div>

---
layout: statement
---

# A build tool

You declare **what** you depend on and **what** you want done.

It works out **how**.

---
layout: section
---

# Maven

---
layout: default
---

# Convention over configuration

<v-clicks>

- You do **not** describe your project to Maven
- You arrange it the way Maven already expects
- Then you write down only the unusual parts

</v-clicks>

<v-click>

<div class="mt-8 text-center">

The whole description is one file.

</div>

</v-click>

<v-click>

<div class="mt-2 text-center text-2xl text-green-400">

`pom.xml` — the **P**roject **O**bject **M**odel

</div>

</v-click>

---
layout: default
---

# Coordinates

```xml
<groupId>com.sunrise</groupId>
<artifactId>lambda-drills</artifactId>
<version>1.0.0</version>
```

<v-click>

<div class="mt-6">

| | What it is | Convention |
|---|---|---|
| `groupId` | who owns it | a domain name, backwards — `org.apache.commons` |
| `artifactId` | which project | lower case, hyphens — `spring-boot-starter-web` |
| `version` | which release | `1.0.0`. `-SNAPSHOT` means "still moving" |

</div>

</v-click>

---
layout: default
---

# Coordinates are an address

Not decoration. This is **exactly** what somebody else writes to depend on you:

```xml
<dependency>
    <groupId>com.sunrise</groupId>
    <artifactId>lambda-drills</artifactId>
    <version>1.0.0</version>
</dependency>
```

<v-click>

<div class="mt-6 border-l-4 border-green-500 pl-4">

Every jar in the world is addressed this way.

**Maven Central** is the directory.

</div>

</v-click>

---
layout: default
---

# The standard layout

```
lambda-drills/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/          ← your code
    │   └── resources/     ← config, packaged into the jar
    └── test/
        ├── java/          ← your tests
        └── resources/     ← test-only config
```

<v-click>

<div class="mt-4 text-center text-green-300">

You get all of it by putting files in the right folder.

</div>

</v-click>

---
layout: default
---

# `main` against `test`

The split that matters:

<v-clicks>

- Test code **can** see main code
- Main code **cannot** see test code
- Nothing under `test/` goes into the jar you ship

</v-clicks>

<v-click>

<div class="mt-8 border-2 border-green-500/50 rounded-lg px-6 py-4 bg-green-500/10">

An `import org.junit...` in `src/main/java` **fails to compile**.

That failure is the layout doing its job.

</div>

</v-click>

---
layout: default
---

# And now IntelliJ works

<div class="grid grid-cols-2 gap-6">

<div>

## The last two sessions

No `pom.xml`. No project model.

Nothing told the IDE which folder was a source root.

The green gutter arrows were missing or wrong.

</div>

<div>

<v-click>

## From today

Open the **`pom.xml`** as a project.

IntelliJ reads the source roots, the dependencies and the test folder straight out of it.

<div class="mt-4 text-green-300">The arrows work.</div>

</v-click>

</div>

</div>

<v-click>

<div class="mt-6 text-sm border-l-4 border-yellow-500 pl-4">

**File → Open → select the `pom.xml` itself → Open as Project.**
Opening the *folder* is the commonest mistake of the day.

</div>

</v-click>

---
layout: default
---

# Dependencies

```xml
<dependencies>
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>6.0.1</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

<v-click>

<div class="mt-6 text-center">

Three coordinates — and one attribute most people leave out.

</div>

</v-click>

---
layout: default
---

# Scope

| Scope | Compile classpath | Test classpath | In the jar |
|---|---|---|---|
| `compile` *(default)* | ✅ | ✅ | ✅ |
| `test` | ❌ | ✅ | **❌** |
| `provided` | ✅ | ✅ | ❌ — the server has it |
| `runtime` | ❌ | ✅ | ✅ — a JDBC driver |

<v-click>

<div class="mt-6 border-l-4 border-green-500 pl-4">

`test` is why your users never receive your testing library.

</div>

</v-click>

---
layout: default
---

# `provided` — remember this one

<v-clicks>

- The Servlet API is `provided`
- The application server already has it
- Ship a second copy inside your war and you get a `ClassCastException`
- Two identical classes, two classloaders, one very long afternoon

</v-clicks>

<v-click>

<div class="mt-8 text-center opacity-80">

You will meet this in the Spring sessions. Recognise the word now.

</div>

</v-click>

---
layout: default
---

# Properties

```xml
<properties>
    <maven.compiler.release>25</maven.compiler.release>
    <junit.version>6.0.1</junit.version>
</properties>
```

<v-click>

<div class="mt-6">

Two very different things on those two lines.

</div>

</v-click>

---
layout: two-cols
---

# `junit.version`

A variable **you invented**.

```xml
<version>${junit.version}</version>
```

<div class="mt-6 text-sm opacity-80">

With five JUnit artifacts in a real project, this is one edit instead of five.

</div>

::right::

<div class="mt-14"></div>

# `maven.compiler.release`

**Not yours.** A setting the compiler plugin reads.

<v-click>

<div class="mt-4 border-2 border-red-500/50 rounded-lg px-4 py-3 bg-red-500/10 text-sm">

Leave it out → Maven builds for a very old Java → your **records do not compile**.

</div>

</v-click>

---
layout: default
---

# The error you will see today

```
error: records are not supported in -source 8
  (use -source 16 or higher to enable records)
```

<v-click>

<div class="mt-8 text-center text-xl">

A true statement about a Java version you did not choose

and did not know you had asked for.

</div>

</v-click>

<v-click>

<div class="mt-6 text-center text-green-300">

It is TODO 1 in your POM. Not a bug in your code.

</div>

</v-click>

---
layout: default
---

# The lifecycle

Maven does not really have commands. It has **phases**, and they run in order.

| Phase | What it does |
|---|---|
| `validate` | is the project sane |
| `compile` | `src/main/java` → `target/classes` |
| `test` | compile the tests, then **run** them |
| `package` | build the `.jar` in `target/` |
| `verify` | integration tests and checks |
| `install` | copy the jar into `~/.m2/repository` |
| `deploy` | push it to a shared repository |

---
layout: statement
---

# Asking for a phase

runs **every phase before it**.

---
layout: default
---

# So `mvn package` tests

<v-clicks>

- `mvn compile` → validate, compile
- `mvn test` → validate, compile, **test**
- `mvn package` → validate, compile, test, **package**

</v-clicks>

<v-click>

<div class="mt-8 border-2 border-green-500/50 rounded-lg px-6 py-4 bg-green-500/10">

There is no way to package without testing —

unless you go out of your way to skip them, **on the command line, in public**.

</div>

</v-click>

<v-click>

<div class="mt-4 text-sm opacity-80">

A decision somebody made, rather than a thing that quietly never happened.

</div>

</v-click>

---
layout: default
---

# `mvn clean` is separate

```
mvn clean          # delete target/
mvn clean test     # delete target/, then build and test from nothing
```

<v-click>

<div class="mt-8 border-l-4 border-yellow-500 pl-4">

Your reset button.

When a build behaves in a way that makes no sense, this is the first thing to try.

</div>

</v-click>

---
layout: default
---

# Plugins do the actual work

Maven itself compiles nothing. Each phase is **bound to a plugin goal**.

| Phase | Plugin |
|---|---|
| `compile` | `maven-compiler-plugin` |
| `test` | `maven-surefire-plugin` |
| `package` | `maven-jar-plugin` |

<v-click>

<div class="mt-6 opacity-80">

You mostly do not see them, because every phase has a default binding.

</div>

</v-click>

---
layout: default
---

# When you do name one

```xml {all|4|5}
<build>
    <plugins>
        <plugin>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>3.5.4</version>
        </plugin>
    </plugins>
</build>
```

<v-click>

<div class="mt-6">

Because **"whatever version my Maven happens to ship"** is exactly the kind of difference
that makes a build reproducible on one machine and not another.

</div>

</v-click>

---
layout: default
---

# Surefire

The plugin that runs your tests.

<v-click>

<div class="mt-8 border-2 border-yellow-500/50 rounded-lg px-6 py-4 bg-yellow-500/10">

`Tests run: 0` from a project full of tests?

**Look at Surefire first.**

</div>

</v-click>

---
layout: default
---

# Demo — the lifecycle

```
mvn clean test
mvn dependency:tree
mvn package
```

<div class="grid grid-cols-2 gap-6 mt-6">

<div>

## The arc

<v-clicks>

- First run downloads the world
- Second run is instant
- `Tests run:` summary
- A jar appears in `target/`

</v-clicks>

</div>

<div>

## Watch for

<v-click>

The jar is called

`lambda-drills-1.0.0.jar`

<div class="mt-3 text-sm opacity-80">
Your coordinates, written out as a filename.
</div>

</v-click>

</div>

</div>

---
layout: section
---

# Transitive dependencies

The best thing about Maven. And the worst.

---
layout: default
---

# You declared one dependency

```
com.sunrise:lambda-drills:jar:1.0.0
\- org.junit.jupiter:junit-jupiter:jar:6.0.1:test
   +- org.junit.jupiter:junit-jupiter-api:jar:6.0.1:test
   |  +- org.opentest4j:opentest4j:jar:1.3.0:test
   |  +- org.junit.platform:junit-platform-commons:jar:6.0.1:test
   |  \- org.apiguardian:apiguardian-api:jar:1.1.2:test
   \- org.junit.jupiter:junit-jupiter-params:jar:6.0.1:test
```

<v-click>

<div class="mt-4 text-center text-xl text-green-300">

You got six.

</div>

</v-click>

---
layout: default
---

# `mvn dependency:tree`

<div class="mt-4 text-lg">

Learn this command today.

</div>

<v-click>

<div class="mt-8 border-l-4 border-green-500 pl-4">

The day a class you have never imported shows up in a stack trace,

this is the answer.

</div>

</v-click>

---
layout: default
---

# The diamond

```
your-app
├── library-A  →  commons-lang3 3.12
└── library-B  →  commons-lang3 3.17
```

<v-click>

<div class="mt-6 text-center text-xl">

Only **one** version can be on the classpath.

</div>

</v-click>

---
layout: default
---

# Nearest wins

<div class="mt-4 opacity-80">Two steps. In this order.</div>

<v-clicks>

- **1. Depth.** Fewest steps from *your* POM wins
- **2. Declaration order** — *only* to break a depth tie

</v-clicks>

<v-click>

<div class="mt-8 text-center text-xl">

A version **you** declare is **zero** steps away.

It beats everything.

</div>

</v-click>

---
layout: default
---

# Step 2 is a tiebreak, nothing more

<div class="mt-4">

It applies **only** when two versions are equally deep.

</div>

<v-click>

<div class="mt-6">

"First" = the order the dependencies appear **in your own POM**, top to bottom.

</div>

</v-click>

<v-click>

<div class="mt-8 border-2 border-amber-500/50 rounded-lg px-6 py-4 bg-amber-500/10">

Not the order in the tree output. Not alphabetical. **Your POM, top to bottom.**

</div>

</v-click>

---
layout: default
---

# Run the rule on the diamond

```xml
<dependency>…library-A…</dependency>   <!-- declared first -->
<dependency>…library-B…</dependency>
```

<v-clicks>

- Both `commons-lang3` versions are **one step** away → step 1 ties
- Step 2 decides: **library-A is declared first**
- Winner: **3.12**

</v-clicks>

<v-click>

<div class="mt-6 text-center text-red-300">

Library-**B** now runs against 3.12 — a version it has never seen.

</div>

</v-click>

---
layout: default
---

# Now swap the two blocks

```xml
<dependency>…library-B…</dependency>   <!-- declared first now -->
<dependency>…library-A…</dependency>
```

<v-clicks>

- Winner: **3.17**
- Same libraries. Same tree. **No version number edited.**
- Different jar on the classpath

</v-clicks>

<v-click>

<div class="mt-8 text-center text-xl">

Two lines moved in a file changed what your program runs.

</div>

</v-click>

---
layout: default
---

# What the rule is **not**

<v-clicks>

- Not **"newest wins"** — 3.12 beat 3.17 a moment ago
- Not **"the version each library was tested against"**

</v-clicks>

<v-click>

<div class="mt-8 border-2 border-red-500/50 rounded-lg px-6 py-4 bg-red-500/10">

Maven **cannot** know what library-A was tested against.

That fact is written down nowhere Maven can read it.

</div>

</v-click>

---
layout: default
---

# Why it cannot know

<v-clicks>

- **Not in A's POM** — that lists a *minimum* A needs, not what A was proven against
- **Not in A's jar** — a jar records no such thing

</v-clicks>

<v-click>

<div class="mt-10 text-center text-xl">

So "tested against" cannot break the tie,

cannot override depth, and **cannot rescue you**.

</div>

</v-click>

---
layout: default
---

# What actually happens

<v-clicks>

- The loser of the tie is handed a version it has never seen
- Usually: nothing
- Occasionally: `NoSuchMethodError`

</v-clicks>

<v-click>

<div class="mt-8 text-center">

A method that existed when the library was **compiled**

and does not exist in the version that **shipped**.

</div>

</v-click>

---
layout: default
---

# The fix is to decide

Declare it yourself. Now it is the nearest one.

```xml
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-lang3</artifactId>
    <version>3.17.0</version>
</dependency>
```

<v-click>

<div class="mt-6 opacity-80">

You will not do this often. But `NoSuchMethodError` on a method you can **see in the
source** is one of the more disorienting bugs in Java — and now you know its shape.

</div>

</v-click>

---
layout: section
---

# Gradle, briefly

---
layout: default
---

# The same build, twice

<div class="text-sm opacity-70 mb-2">Watch the file change. The build does not.</div>

<div class="relative h-[340px]">

<div v-click.hide="1" class="absolute inset-0 transition-all duration-700 ease-in-out">

<div class="text-xs font-bold text-orange-300 mb-1">pom.xml</div>

```xml
<project>
  <groupId>com.sunrise</groupId>
  <artifactId>lambda-drills</artifactId>
  <version>1.0.0</version>
  <properties>
    <maven.compiler.release>25</maven.compiler.release>
  </properties>
  <dependencies>
    <dependency>
      <groupId>org.junit.jupiter</groupId>
      <artifactId>junit-jupiter</artifactId>
      <version>6.0.1</version>
      <scope>test</scope>
    </dependency>
  </dependencies>
</project>
```

</div>

<div v-click="1" class="absolute inset-0 transition-all duration-700 ease-in-out">

<div class="text-xs font-bold text-green-300 mb-1">build.gradle.kts</div>

```kotlin
plugins { java }

group = "com.sunrise"
version = "1.0.0"

repositories { mavenCentral() }

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:6.0.1")
}

java {
    toolchain { languageVersion = JavaLanguageVersion.of(25) }
}
```

</div>

</div>

---
layout: default
transition: fade
---

# Every line has a counterpart

| What it says | `pom.xml` | `build.gradle.kts` |
|---|---|---|
| Who this project is | `<groupId>` `<artifactId>` `<version>` | `group`, `version`, folder name |
| Where jars come from | Central, implicit | `repositories { mavenCentral() }` |
| A dependency | a nine-line block | one string, `"group:artifact:version"` |
| Which Java | `<maven.compiler.release>` | `java { toolchain { … } }` |
| Run the tests | `mvn test` | `./gradlew test` |
| Where the code lives | `src/main/java` | `src/main/java` — **identical** |

<v-click>

<div class="mt-4 text-center">

Gradle took Maven's layout wholesale. Different build file, same project shape.

</div>

</v-click>

---
layout: default
---

# The one real concept swap

<div class="grid grid-cols-2 gap-8 mt-6">

<div>

## Maven

An **element** says what a dependency is for.

```xml
<scope>test</scope>
```

</div>

<div>

<v-click>

## Gradle

The **bucket you put it in** says it.

```kotlin
testImplementation("…")
```

</v-click>

</div>

</div>

<v-click>

| Maven scope | Gradle configuration |
|---|---|
| `compile` | `implementation` |
| `test` | `testImplementation` |
| `provided` | `compileOnly` |

</v-click>

---
layout: default
---

# The build file is a program

<div class="grid grid-cols-2 gap-6 mt-4">

<div>

## The power

Kotlin or Groovy, not XML.

A loop. A condition. A function.

</div>

<div>

<v-click>

## The cost

That is also how build files become unreadable.

A POM is boring, and you can always tell what it does.

</v-click>

</div>

</div>

<v-click>

<div class="mt-8 text-center opacity-80">

On a large team, boring is a feature.

</div>

</v-click>

---
layout: default
---

# It is faster — on big projects

<v-clicks>

- Tracks which inputs changed, skips work whose inputs did not
- Caches build outputs, sometimes across machines
- Ten-module build: **eight minutes → forty seconds**
- One-module build: **two seconds → two seconds**

</v-clicks>

---
layout: default
---

# Side by side

| | Maven | Gradle |
|---|---|---|
| Build file | `pom.xml`, declarative | a program |
| Learning curve | shallow — one way to do it | steeper — several |
| Speed on large builds | fine | markedly better |
| Predictability | high | depends what was written |
| **Used with Spring** | **yes — the common default** | **yes, fully supported — just less common** |
| Also strong in | enterprise Java generally | Android, larger Java shops |

---
layout: default
---

# Gradle *is* a Spring option

<div class="mb-4 opacity-80">Do not read that table as "Maven for Spring, Gradle for Android".</div>

<v-clicks>

- `start.spring.io` offers **both**, from the same dropdown
- Every Spring Boot starter resolves identically either way
- Spring Boot ships a **Gradle plugin** alongside its Maven plugin
- The Spring team builds **Spring itself** with Gradle

</v-clicks>

<v-click>

<div class="mt-8 text-center text-xl">

Maven is what you are **more often handed**.

That is a fact about teams, not about Spring.

</div>

</v-click>

---
layout: statement
---

# This course is Maven

Recognise a `build.gradle.kts`.

Find the dependency block in it.

**Do not have an opinion about which is correct.**

---
layout: section
---

# Lambdas

---
layout: default
---

# Some interfaces have exactly one method

<v-clicks>

- `Comparator` → `compare`
- `Runnable` → `run`
- `Predicate` → `test`

</v-clicks>

<v-click>

<div class="mt-8">

Before Java 8, handing one of those to a method meant this:

</div>

</v-click>

---
layout: default
---

# Six lines

```java {all|4}
Comparator<Employee> bySalary = new Comparator<Employee>() {
    @Override
    public int compare(Employee a, Employee b) {
        return Double.compare(b.salary(), a.salary());
    }
};
```

<v-click>

<div class="mt-6 text-center text-xl">

**Exactly one** of them says anything.

</div>

</v-click>

---
layout: default
---

# The other five are a form

The compiler already knows:

<v-clicks>

- the interface — you wrote it on the left
- therefore the method name
- therefore the parameter types
- therefore the return type

</v-clicks>

<v-click>

<div class="mt-8 text-center text-green-300 text-lg">

It was asking you to fill in a form it could have filled in itself.

</div>

</v-click>

---
layout: default
---

# The lambda

```java
Comparator<Employee> bySalary = (a, b) -> Double.compare(b.salary(), a.salary());
```

<v-clicks>

- Same type
- Same object at runtime
- Same behaviour

</v-clicks>

<v-click>

<div class="mt-8 border-l-4 border-green-500 pl-4">

The five ceremonial lines are gone because the compiler recovers every one of them
from the type on the left.

</div>

</v-click>

---
layout: default
---

# Four steps of shrinking

```java {all|1|2|3|4|5}
(Employee a, Employee b) -> { return Double.compare(b.salary(), a.salary()); }
(a, b) -> { return Double.compare(b.salary(), a.salary()); }
(a, b) -> Double.compare(b.salary(), a.salary())
name -> name.toUpperCase()
() -> System.out.println("hello")
```

<v-clicks>

- everything spelled out
- types inferred from the target
- one expression — no braces, **no `return`**
- one parameter — no parentheses
- no parameters — empty parentheses

</v-clicks>

---
layout: default
---

# Braces mean a block

<div class="grid grid-cols-2 gap-6 mt-4">

<div>

```java
(a, b) -> a + b
```

<div class="mt-3 text-sm text-green-300">
an expression. It IS the answer.
</div>

</div>

<div>

```java
(a, b) -> { return a + b; }
```

<div class="mt-3 text-sm text-green-300">
a block. Needs an explicit return.
</div>

</div>

</div>

<v-click>

<div class="mt-8 border-2 border-red-500/50 rounded-lg px-6 py-4 bg-red-500/10">

```java
(a, b) -> { Double.compare(a, b); }    // returns NOTHING
```

Compiles to a method that returns void. The error talks about incompatible types.

</div>

</v-click>

---
layout: default
---

# Where does a lambda get its type?

<v-click>

<div class="mt-6 text-center text-xl">

It has none of its own.

</div>

</v-click>

<v-click>

<div class="mt-6 text-center">

It takes the type of whatever it is assigned to —

and that must be an interface with **exactly one abstract method**.

</div>

</v-click>

<v-click>

<div class="mt-8 text-center text-2xl text-green-400">

a **functional interface**

</div>

</v-click>

---
layout: default
---

# Write your own

```java {all|1|2-4|6}
@FunctionalInterface
public interface Discount {
    double applyTo(double amount);
}

Discount tenPercent = amount -> amount * 0.9;
```

<v-click>

<div class="mt-4 text-sm opacity-80">

`tenPercent.applyTo(200.0)` → `180.0`

</div>

</v-click>

---
layout: default
---

# `@FunctionalInterface` is optional

Write it anyway. It does exactly one thing:

<v-click>

<div class="mt-6 border-2 border-green-500/50 rounded-lg px-6 py-4 bg-green-500/10">

The compiler refuses the **interface** if it ever gains a second abstract method.

</div>

</v-click>

<v-click>

<div class="mt-6">

Without it, a colleague adds a method — and the errors appear at every place
somebody wrote a lambda, not at the change that caused them.

</div>

</v-click>

---
layout: default
---

# Two things that do not break the rule

```java
public interface Comparator<T> {
    int compare(T a, T b);                    // the one abstract method

    default Comparator<T> reversed() { ... }  // has a body
    static <T> Comparator<T> naturalOrder() { ... }
}
```

<v-click>

<div class="mt-6">

`default` and `static` methods **have bodies**, so they are not abstract.

`Comparator` has a dozen of them and is still a functional interface.

</div>

</v-click>

---
layout: default
---

# The four you actually need

| Interface | Method | Takes | Gives back | For |
|---|---|---|---|---|
| `Predicate<T>` | `test(T)` | one | `boolean` | filtering |
| `Function<T,R>` | `apply(T)` | one | another | transforming |
| `Consumer<T>` | `accept(T)` | one | nothing | doing something |
| `Supplier<T>` | `get()` | nothing | a value | producing on demand |
| `Comparator<T>` | `compare(T,T)` | two | `int` | ordering |

<v-click>

<div class="mt-4 text-sm opacity-80">

`java.util.function` has forty-odd. You need these.

</div>

</v-click>

---
layout: default
---

# The four, in code

```java
Predicate<Employee>        senior = e -> e.salary() > 80000;
Function<Employee, String> name   = e -> e.name();
Consumer<String>           print  = s -> System.out.println(s);
Supplier<List<String>>     empty  = () -> new ArrayList<>();
```

<v-click>

<div class="mt-6 text-center opacity-80">

Four interfaces. Four one-liners. Nothing else to learn.

</div>

</v-click>

---
layout: default
---

# Predicates combine

```java
Predicate<Employee> senior        = e -> e.salary() > 80000;
Predicate<Employee> inEngineering = e -> e.department().equals("Engineering");

senior.and(inEngineering)
senior.or(inEngineering)
senior.negate()
```

<v-click>

<div class="mt-6 border-l-4 border-green-500 pl-4">

Three **named** ideas and a combinator

beat one fused condition that nobody can name.

</div>

</v-click>

---
layout: default
---

# Method references

When the lambda does nothing but call an existing method, say so:

```java
e -> e.name()                →  Employee::name
s -> s.toUpperCase()         →  String::toUpperCase
s -> Integer.parseInt(s)     →  Integer::parseInt
() -> new ArrayList<>()      →  ArrayList::new
s -> System.out.println(s)   →  System.out::println
```

<v-click>

<div class="mt-6 opacity-80">

Four kinds — static, one particular object, whichever object arrives, and a constructor.

</div>

</v-click>

---
layout: statement
---

# You do not need to name the four kinds

You need to read `Employee::name` as

**"the name of each one"**

without slowing down.

---
layout: default
---

# Capture

A lambda can read local variables from around it:

```java
String prefix = "Hello, ";
Supplier<String> greet = () -> prefix + "world";
```

<v-click>

<div class="mt-6">

But only if the variable is **never reassigned**.

</div>

</v-click>

<v-click>

```
error: local variables referenced from a lambda expression
       must be final or effectively final
```

</v-click>

---
layout: default
---

# "Effectively final"

<div class="mt-6 text-center text-xl">

You could have written `final` in front of it

and nothing would have broken.

</div>

---
layout: default
---

# Why the language insists

<v-clicks>

- The lambda may run long after the method that created it returned
- On another thread. Or next Tuesday.
- So it captures the **value**, not the variable

</v-clicks>

<v-click>

<div class="mt-8 border-2 border-green-500/50 rounded-lg px-6 py-4 bg-green-500/10">

If the variable could still change, "what is `prefix`" would have two answers,

and which one you got would depend on **timing**.

Java refuses the ambiguity instead of resolving it badly.

</div>

</v-click>

---
layout: default
---

# Fields are a different story

```java
class Counter {
    private int count;

    Runnable increment() {
        return () -> count++;    // perfectly legal
    }
}
```

<v-click>

<div class="mt-6 border-l-4 border-yellow-500 pl-4">

A lambda reads and writes `this.something` freely.

Which is exactly why lambdas that touch shared state need the same care
as any other concurrent code.

</div>

</v-click>

---
layout: default
---

# Pause — which of these can be a lambda?

<v-clicks>

1. `interface Handler { void handle(Event e); }`
2. `interface Repo { void save(Order o); Order find(long id); }`
3. `interface Named { String name(); default String shout() { ... } }`
4. `abstract class Task { abstract void run(); }`

</v-clicks>

<v-click>

<div class="mt-8 text-center opacity-80">

Give a reason for each. One of them is a trap that has nothing to do with counting methods.

</div>

</v-click>

---
layout: section
---

# Streams

Lambdas made it cheap to pass behaviour around.

This is what happens when the library takes that seriously.

---
layout: default
---

# The loop that does four things

```java
List<String> result = new ArrayList<>();
for (Employee e : staff) {
    if (e.salary() > 60000) {
        result.add(e.name());
    }
}
result.sort(Comparator.naturalOrder());
```

<v-click>

<div class="mt-4 text-center text-green-300">

Nothing is wrong with this.

</div>

</v-click>

---
layout: default
---

# But read what it says

<v-clicks>

- make an empty list
- walk the collection
- test each one
- take a field
- append it
- sort the result

</v-clicks>

<v-click>

<div class="mt-8 border-2 border-yellow-500/50 rounded-lg px-6 py-4 bg-yellow-500/10">

Six instructions describing **mechanics**.

The intent — *"the names of everyone over 60000, sorted"* — is not written down anywhere.

You reconstruct it every time you read the loop.

</div>

</v-click>

---
layout: default
---

# The pipeline

```java {all|2|3|4|5}
List<String> result = staff.stream()
        .filter(e -> e.salary() > 60000)
        .map(Employee::name)
        .sorted()
        .toList();
```

<v-click>

<div class="mt-6 text-center text-xl text-green-300">

The mechanics are gone.

What is left is the sentence you would have said out loud.

</div>

</v-click>

---
layout: default
---

# Every pipeline has three parts

```java {all|1|2-4|5}
staff.stream()                      // SOURCE
     .filter(...)                   // INTERMEDIATE, any number
     .map(...)                      //   each returns another stream
     .sorted()
     .toList();                     // TERMINAL, exactly one, ends it
```

<v-click>

<div class="mt-6 text-sm">

Sources: `collection.stream()` · `Arrays.stream(array)` · `Stream.of(a, b, c)` · `IntStream.range(0, 10)`

</div>

</v-click>

---
layout: default
---

# A stream is not a collection

<v-clicks>

- It holds **no elements**
- It can be consumed **once**
- It **never modifies its source**

</v-clicks>

<v-click>

<div class="mt-8 border-l-4 border-green-500 pl-4">

Which is why the loop had to copy the list before sorting it,

and the pipeline did not.

</div>

</v-click>

---
layout: default
---

# Nothing happens

```java
staff.stream()
     .filter(e -> e.salary() > 60000)
     .map(Employee::name);
```

<v-click>

<div class="mt-8 text-center text-2xl text-red-400">

Nothing has run. Nothing at all.

</div>

</v-click>

<v-click>

<div class="mt-6 text-center">

Intermediate operations are **lazy**. They record what you asked for, and return.

</div>

</v-click>

---
layout: default
---

# One at a time, all the way down

Not stage by stage.

<v-clicks>

- `filter` sees Alice
- if she passes, `map` sees Alice **immediately**
- before `filter` has looked at Bob

</v-clicks>

<v-click>

<div class="mt-8">

Which is what makes this work:

```java
staff.stream()
     .filter(e -> e.salary() > 60000)
     .findFirst();
```

</div>

</v-click>

<v-click>

<div class="mt-4 text-center text-green-300">

On a thousand employees, that may examine **one**.

</div>

</v-click>

---
layout: statement
---

# The equivalent loop

with an early `return`

does exactly the same thing.

Now you know they are the same thing.

---
layout: default
---

# The commonest bug in this material

```java
staff.stream()
     .filter(e -> e.salary() > 60000)
     .map(Employee::name);
```

<v-click>

<div class="mt-8 border-2 border-red-500/50 rounded-lg px-6 py-4 bg-red-500/10">

A pipeline with **no terminal operation**.

Looks complete. Compiles, if you assign it to a `Stream`.

Does nothing whatsoever.

</div>

</v-click>

---
layout: default
---

# Intermediate operations

| | |
|---|---|
| `filter(Predicate)` | keep the ones that pass |
| `map(Function)` | replace each with something else |
| `sorted()` / `sorted(Comparator)` | order them |
| `distinct()` | drop repeats — uses `equals`, which your records have |
| `limit(n)` / `skip(n)` | the first n / everything after the first n |
| `flatMap(Function)` | each element becomes a stream; flatten them into one |

---
layout: default
---

# Terminal operations

| | |
|---|---|
| `toList()` | the answer, as a `List` |
| `collect(Collector)` | the answer, in any shape you like |
| `count()` | how many |
| `anyMatch` / `allMatch` / `noneMatch` | a `boolean`, and it stops as soon as it knows |
| `findFirst()` / `findAny()` | an `Optional` — there may be none |
| `min` / `max` | an `Optional`, for the same reason |
| `forEach(Consumer)` | do something with each. Returns nothing |
| `reduce(...)` | fold them into one value |

---
layout: default
---

# Numbers are their own thing

```java {all|1|2}
double payroll = staff.stream().mapToDouble(Employee::salary).sum();
double average = staff.stream().mapToDouble(Employee::salary).average().orElse(0);
```

<v-click>

<div class="mt-6">

`mapToInt` / `mapToDouble` give you an `IntStream` / `DoubleStream` —
which is where `sum()` and `average()` live.

A `Stream<Employee>` has neither.

</div>

</v-click>

---
layout: default
---

# `average()` returns an `Optional`

<div class="mt-6 text-center text-xl">

The average of no employees is not zero.

It is a question with no answer.

</div>

<v-click>

<div class="mt-8 text-center">

```java
.average().orElse(0)
```

**You** deciding what to do about that, in the open.

</div>

</v-click>

---
layout: default
---

# Collectors

`collect` is the general terminal operation. `Collectors` is a class of recipes.

```java
.collect(Collectors.toList())          // .toList() is the short way
.collect(Collectors.toSet())
.collect(Collectors.joining(", "))     // one String, separators handled
.collect(Collectors.counting())
.collect(Collectors.summarizingDouble(Employee::salary))
```

<v-click>

<div class="mt-4 text-sm opacity-80">

`summarizingDouble` gives you count, sum, min, max **and** average — in one pass.

</div>

</v-click>

---
layout: default
---

# `groupingBy` — memorise this one

```java
Map<String, List<Employee>> byDept = staff.stream()
        .collect(Collectors.groupingBy(Employee::department));
```

<v-click>

<div class="mt-6">

One line for what is otherwise a `HashMap`, a loop and a `computeIfAbsent`.

</div>

</v-click>

---
layout: default
---

# And it takes a second argument

```java {all|1-3|5-7|9-12}
Map<String, Long> headcount = staff.stream()
        .collect(Collectors.groupingBy(Employee::department,
                 Collectors.counting()));

Map<String, Double> averagePay = staff.stream()
        .collect(Collectors.groupingBy(Employee::department,
                 Collectors.averagingDouble(Employee::salary)));

Map<String, List<String>> namesByDept = staff.stream()
        .collect(Collectors.groupingBy(Employee::department,
                 Collectors.mapping(Employee::name,
                                    Collectors.toList())));
```

---
layout: statement
---

# The downstream collector

is the idea that makes collectors worth learning.

It is also the one that takes a week to feel natural.

**It is most of your homework.**

---
layout: default
---

# When NOT to use a stream

Not a style rule. These are cases where the loop is the better code.

<v-clicks>

- **You need the index.** Streams have no index
- **You need to modify the source.** Streams do not. `removeIf` exists
- **You need to `break` with real logic.** `findFirst` covers the easy cases
- **The lambda must catch a checked exception.** No standard interface declares `throws`
- **It is genuinely harder to read**

</v-clicks>

<v-click>

<div class="mt-6 border-l-4 border-green-500 pl-4 text-sm">

Readability is the entire justification for streams.
A pipeline that is not readable has nothing left to offer.

</div>

</v-click>

---
layout: default
---

# And performance is rarely the argument

<v-clicks>

- Small collections: the loop is faster, and it does not matter
- Very large ones: `parallelStream()` exists
- Do not reach for it until you have **measured**

</v-clicks>

---
layout: default
---

# Pause — pipeline, or loop?

<v-clicks>

1. Sum the salaries of everyone in HR
2. Find the **position in the list** of the first employee earning over 100000
3. Print every name
4. Remove everyone who has left from the list you were given

</v-clicks>

<v-click>

<div class="mt-8 text-center opacity-80">

Two of these are pipelines. Two are loops. Say why.

</div>

</v-click>

---
layout: section
---

# Exercise

Lambda and Stream Drills

---
layout: default
---

# 22 checks, three sets

A **real Maven project**. The checks are **real JUnit tests**.

<v-click>

<div class="mt-6">

| Set | File | Drills | Focus |
|---|---|---|---|
| A | `LambdaDrills.java` | 8 | the four interfaces, your own, capture, `.and(...)` |
| B | `StreamDrills.java` | 7 | `filter`, `map`, `sorted`, `distinct`, `limit` |
| C | `ReportDrills.java` | 7 | `count`, `average`, `max`, `joining`, `groupingBy` |

</div>

</v-click>

<v-click>

<div class="mt-4 text-center text-green-300">

Every drill shows you working imperative code. Produce the same result in one line.

</div>

</v-click>

---
layout: default
---

# Step 0 — finish the POM

Two `TODO` blocks. Nothing builds until both are done.

<v-click>

<div class="mt-6">

| TODO | What | What you see if you skip it |
|---|---|---|
| 1 | `maven.compiler.release` | `records are not supported in -source 8` |
| 2 | the JUnit dependency, `<scope>test</scope>` | `package org.junit.jupiter.api does not exist` |

</div>

</v-click>

<v-click>

<div class="mt-6 border-l-4 border-yellow-500 pl-4">

Both errors are in the **build**, not in your code.

Read them once on purpose.

</div>

</v-click>

---
layout: default
---

# Run the tests before you write anything

<div class="mt-8 text-center text-3xl text-red-400">

All 22 fail.

</div>

<v-click>

<div class="mt-8 text-center">

That is correct. Every drill returns `null` or `0`.

You want to see red before you go looking for green.

</div>

</v-click>

---
layout: default
---

# How you check your work

<v-clicks>

- Right-click `src/test/java` → **Run 'All Tests'**
- or the **Maven** tool window → **Lifecycle → test**
- or `mvn test` in a terminal beside the `pom.xml`

</v-clicks>

<v-click>

<div class="mt-8">

```
Tests run: 22, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

</div>

</v-click>

---
layout: default
---

# Three things that will happen

<v-clicks>

- **`cannot find symbol: getName()`** — `Employee` is a record. It is `name()`
- **No green arrows, `Employee` unknown** — you opened the folder, not the `pom.xml`
- **`Stream<String> cannot be converted to List<String>`** — no terminal operation

</v-clicks>

<v-click>

<div class="mt-8 text-center opacity-80">

The full table is in `lambda-drills/README.md`. Nine rows. Read it when you are stuck,
not before.

</div>

</v-click>

---
layout: default
---

# Homework — Bookshop Drills

**12 checks. A separate Maven project, a different domain.** 45–60 minutes.

<v-clicks>

- It does **not** continue from class. If today went badly, start clean
- In class, streams produced a list or a number
- Here they produce **`Map`s** — what a real reporting method returns
- Set D: `groupingBy` with downstream collectors, `partitioningBy`
- Set E: `flatMap`, `joining`, `summarizingDouble`, `toMap`

</v-clicks>

---
layout: default
---

# Two of them are deliberately hard

<div class="grid grid-cols-2 gap-6 mt-6">

<div>

## D6

`maxBy` downstream gives you an `Optional` per group.

`collectingAndThen` unwraps it.

<div class="mt-3 text-sm opacity-80">
Expect to look it up. That is a normal day with collectors.
</div>

</div>

<div>

## E4

`toMap` where two books share an author.

<div class="mt-3 text-sm">
Write it with two arguments <b>on purpose</b>, read the
<code>IllegalStateException: Duplicate key</code>, then fix it.
</div>

<div class="mt-3 text-sm text-green-300">
A five-second diagnosis for the rest of your career, bought for one minute now.
</div>

</div>

</div>

---
layout: section
---

# Recap

---
layout: default
---

# The build

<v-clicks>

- A build tool answers three questions you cannot answer by hand: **where the jars come from**, **is this build the same everywhere**, **where the code goes**
- **Coordinates** name a project. Other projects use exactly those three lines to depend on it
- **Scope** is not optional detail. `test` keeps your test library out of the jar
- Phases run **in order**. `mvn package` tests. `mvn clean test` is the reset button
- **Transitive dependencies**: `dependency:tree` to look, **nearest wins** to resolve, `NoSuchMethodError` when it goes wrong

</v-clicks>

---
layout: default
---

# The language

<v-clicks>

- A **lambda** implements an interface with exactly one abstract method, and takes its type from the left-hand side
- Captured locals must be **effectively final** — the lambda captures a value and may outlive the method
- A **stream** is a source, lazy intermediate operations, and exactly one terminal operation
- Elements flow through **one at a time, all the way down**
- **`groupingBy` with a downstream collector** is the most useful thing in `Collectors`
- Loops are still correct

</v-clicks>

---
layout: statement
---

# Next session

You ran tests today without writing any.

**JUnit 5 · AssertJ · Mockito**

Three lines in the POM you just learned to read.
