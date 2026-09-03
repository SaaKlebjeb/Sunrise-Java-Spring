# Build Tools and Functional Java

*Maven and Gradle, lambda expressions, and the Stream API.*

*A guided lesson. Read it start to finish; work the code as you go; stop at the **Pause** prompts and actually answer them before reading on.*

**Before you start**

- You need **JDK 25**, and **Maven 3.9 or newer**. Check both: `java -version`, `javac -version` and `mvn -v`. A JRE-only install fails on the first compile.
- You need **network access the first time you build**. Maven downloads what your project depends on, once, into a folder called `~/.m2/repository`. On a slow connection the very first `mvn test` can take several minutes. Every build after that is offline and instant.
- IntelliJ IDEA, as always, is the editor.

**By the end you'll be able to:**

- Say what a build tool actually does, and why a folder of `.java` files stops scaling the moment you need somebody else's code.
- Read a `pom.xml`: coordinates, dependencies, scope, properties, plugins — and write the two lines that make a project compile and test.
- Name the Maven lifecycle phases in order, and say what `mvn test` does that `mvn compile` does not.
- Read `mvn dependency:tree` and explain where a library you never asked for came from.
- Say in one sentence what Gradle does differently, and pick between them for a reason rather than a habit.
- Replace an anonymous inner class with a **lambda**, and say where the lambda got its type.
- Use `Predicate`, `Function`, `Consumer` and `Supplier` — and write your own functional interface.
- Turn a loop into a **stream pipeline**, and say why nothing runs until the terminal operation.
- Group a list into a `Map` with `Collectors.groupingBy`, and name the case where you should have written the loop after all.

Two halves today, and the second one needs the first. We start with the build tool, because the exercise is a real Maven project — the first one in this course.

> One promise from last session lands later than you were told: the `instanceof` chain with the unreachable `throw` gets deleted when we do **switch expressions and pattern matching**, which now comes after the testing session. Hold on to the annoyance a little longer. It is still the best argument for that feature you will ever be given.

---

## Why you have not needed a build file until now

For two sessions your project has been a folder of `.java` files. You compiled them together, you ran one of them, and it worked. That is a real and honest way to write Java, and it stops working the day you need one of three things.

**One: somebody else's code.** Every real project uses libraries — JSON parsing, HTTP, database drivers, testing. A library arrives as a `.jar` file, and the compiler only knows about a jar if you put it on the *classpath*:

```
javac -cp lib/gson-2.11.jar:lib/junit-5.11.jar:lib/opentest4j-1.3.jar OrderService.java
```

That line has three problems, and they get worse in order. It is long. It is different on Windows (`;` instead of `:`). And you have to have found, downloaded and stored those three jars yourself — including `opentest4j`, which you have never heard of and did not ask for.

**Two: a repeatable build.** Compile, run the tests, package a jar, stamp a version on it. On your machine, on a colleague's machine, and on a build server at three in the morning with nobody watching. "It works on my machine" is a bug report, and its root cause is almost always a difference in one of those jars.

**Three: everybody's project looking the same.** When you join a team, `src/main/java` is where the code is. Not `source/`, not `java/`, not `code/v2_final`. You did not have to ask.

A build tool solves all three. You declare *what* you depend on and *what* you want done; the tool works out *how*.

> **Pause.** You have `Main.java` and it needs one library, which itself needs two more. Sketch what you would have to do by hand — where do the jars come from, where do you keep them, and what happens when a colleague clones your project? Count the steps. That number is the honest answer to "why do I need Maven for a small project".

---

## Maven

Maven's central idea is **convention over configuration**. Rather than describing your project, you arrange it the way Maven expects, and then you only write down the parts that are unusual.

The whole description lives in one file, `pom.xml` — the **P**roject **O**bject **M**odel.

### Coordinates

Every Maven project has three coordinates, and together they name it uniquely in the world:

```xml
<groupId>com.sunrise</groupId>
<artifactId>lambda-drills</artifactId>
<version>1.0.0</version>
```

| | What it is | Convention |
|---|---|---|
| `groupId` | who owns it | a domain name backwards — `org.apache.commons`, `com.sunrise` |
| `artifactId` | which project | lower case, hyphens — `commons-lang3`, `spring-boot-starter-web` |
| `version` | which release | `1.0.0`. A version ending in `-SNAPSHOT` means "still moving" |

These are not decoration. They are exactly what somebody else writes down when they want to depend on *you*. Every jar in the world is addressed this way, and Maven Central — the public repository — is a directory of them.

### The standard layout

```
lambda-drills/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/          ← your code
    │   └── resources/     ← config files, packaged into the jar
    └── test/
        ├── java/          ← your tests
        └── resources/     ← test-only config
```

The split that matters is **main** against **test**. Test code can see main code; main code cannot see test code; and nothing under `test/` is packaged into the jar you ship. You get all of that for free, by putting files in the right folder.

This is also the moment IntelliJ starts working properly for you. For the last two sessions there was no project model — no `pom.xml`, nothing telling the IDE which folder was a source root — so the green run arrows in the gutter were unreliable or absent. Open a `pom.xml` as a project and IntelliJ reads the source roots, the dependencies and the test folder straight out of it. The arrows work from now on.

### Dependencies, and the one attribute people skip

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

Three coordinates and a **scope**. The scope is the part people leave out, and it is the part that matters:

| Scope | On the compile classpath | On the test classpath | Packaged in the jar |
|---|---|---|---|
| `compile` *(default)* | yes | yes | yes |
| `test` | no | yes | **no** |
| `provided` | yes | yes | no — the server supplies it |
| `runtime` | no | yes | yes — a JDBC driver, say |

`test` scope is why your users never receive your testing library, and why a stray `import org.junit...` in `src/main/java` fails to compile. That failure is the scope doing its job.

`provided` is worth recognising now because you will meet it in the Spring sessions: the Servlet API is `provided`, because the application server already has it and shipping a second copy inside your war is how you get a `ClassCastException` that takes a day to understand.

### Properties

```xml
<properties>
    <maven.compiler.release>25</maven.compiler.release>
    <junit.version>6.0.1</junit.version>
</properties>
```

Two different things here, and the difference is worth knowing.

`junit.version` is a variable you invented. Write `${junit.version}` anywhere in the POM and Maven substitutes it. With five JUnit artifacts in a real project, this is the difference between one edit and five.

`maven.compiler.release` is **not** yours — it is a setting the compiler plugin reads. Leave it out and Maven builds for a very old Java, and your records do not compile. The error says `records are not supported in -source 8`, which is a true statement about a Java version you did not choose and did not know you had asked for.

### The lifecycle

Maven does not have commands so much as **phases**, and they run in order. Asking for a phase runs every phase before it:

| Phase | What it does |
|---|---|
| `validate` | is the project sane |
| `compile` | `src/main/java` → `target/classes` |
| `test` | compile `src/test/java`, then run the tests |
| `package` | put `target/classes` into a `.jar` in `target/` |
| `verify` | run integration tests and checks |
| `install` | copy that jar into your local `~/.m2/repository`, so your other projects can depend on it |
| `deploy` | push it to a shared repository for your team |

So `mvn package` compiles **and** tests **and** packages. There is no way to package without testing, unless you go out of your way to skip them — which is a decision, made in public, on the command line, rather than a thing that quietly never happened.

`mvn clean` is separate: it deletes `target/`. `mvn clean test` is the one to reach for when a build behaves in a way that makes no sense.

> **Pause.** Your colleague says "the tests pass on my machine but the build server says the jar is missing a class". They have been running `mvn compile` and copying `target/classes` by hand. Which phase have they never run, and what would it have told them?

### Plugins do the actual work

Maven itself compiles nothing. Each phase is *bound* to a plugin goal, and the plugin does the work:

| Phase | Plugin |
|---|---|
| `compile` | `maven-compiler-plugin` |
| `test` | `maven-surefire-plugin` |
| `package` | `maven-jar-plugin` |

You mostly do not see them, because a default binding exists for each. You start naming them when you want a specific version — which you should, because "whatever version my Maven happens to ship" is exactly the kind of difference that makes a build reproducible on one machine and not another:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>3.5.4</version>
        </plugin>
    </plugins>
</build>
```

Surefire is the plugin that runs your tests. When you get "0 tests run" from a project full of tests, Surefire is usually the thing to look at first.

### Demo — the lifecycle, on screen

Your instructor runs this against a finished copy of today's exercise:

```
mvn clean test
mvn dependency:tree
mvn package
```

Three things to watch for. The **first** run downloads a great deal and the second is instant — that is `~/.m2/repository` filling up. `mvn test` prints a line per test class and a `Tests run:` summary. And `mvn package` produces a file in `target/` whose name is `artifactId-version.jar`, which is your coordinates, written out as a filename.

---

## Transitive dependencies

You asked for one library. Look at what arrived:

```
com.sunrise:lambda-drills:jar:1.0.0
\- org.junit.jupiter:junit-jupiter:jar:6.0.1:test
   +- org.junit.jupiter:junit-jupiter-api:jar:6.0.1:test
   |  +- org.opentest4j:opentest4j:jar:1.3.0:test
   |  +- org.junit.platform:junit-platform-commons:jar:6.0.1:test
   |  \- org.apiguardian:apiguardian-api:jar:1.1.2:test
   \- org.junit.jupiter:junit-jupiter-params:jar:6.0.1:test
```

You declared one dependency and got six. That is a **transitive dependency**: your dependency's dependencies, fetched for you. It is the single largest convenience Maven gives you, and it is also where the hard problems live.

`mvn dependency:tree` is how you look at it. Learn it now — the day a class you have never imported appears in a stack trace, this command is the answer.

### The diamond

Two of your dependencies need the same third library, at different versions:

```
your-app
├── library-A  →  commons-lang3 3.12
└── library-B  →  commons-lang3 3.17
```

Only one version can be on the classpath. Maven picks it with **nearest wins**, and the rule has exactly two steps:

1. **Depth.** The version fewest steps from your POM wins. A version you declare yourself is zero steps away, so it beats everything.
2. **Declaration order, only as a tiebreak.** If two versions are equally deep, the one Maven reaches first wins — and "first" means the order the dependencies appear **in your own POM**, top to bottom.

In the diamond above both versions are one step away, so step 1 is a tie and step 2 decides. Library-A is declared first, so **3.12** wins, and library-B now runs against a version it never saw. Move library-B's `<dependency>` block above library-A's and the winner becomes 3.17 instead — same libraries, same tree, no version number edited, different jar on the classpath. That is the whole rule.

Now note what the rule is *not*. It is not "newest wins": 3.12 beat 3.17 here. And it is **not** "the version each library was tested against" — Maven has no idea what version library-A was built or tested against, because that fact is written down nowhere it can read. It is not in A's POM, which lists a *minimum* it needs rather than the version it was proven against, and it is not in A's jar. So "tested against" cannot break the tie, cannot override depth, and cannot rescue you.

The loser of that tie is handed a version it has never seen. Usually nothing happens. Occasionally you get `NoSuchMethodError` at runtime — a method that existed when the library was compiled and does not exist in the version that actually shipped.

The fix is to decide explicitly. Declare the version you want directly in your own POM, and it is now the nearest one:

```xml
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-lang3</artifactId>
    <version>3.17.0</version>
</dependency>
```

You will not do this often. But `NoSuchMethodError` on a method you can see in the source is one of the more disorienting bugs in Java, and now you know the shape of it.

> **Pause.** Why is `NoSuchMethodError` a **runtime** error and not a compile error? What did the compiler see that the JVM did not? (This is worth getting right — it is the same reason `provided` scope can bite you.)

---

## Gradle, briefly

Gradle solves the same problem and makes two different choices.

```kotlin
// build.gradle.kts
plugins {
    java
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:6.0.1")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}
```

That is the *same build* you have been running all session. Put the two files next to each other and every line has a counterpart:

| What it says | `pom.xml` | `build.gradle.kts` |
|---|---|---|
| Who and what this project is | `<groupId>`, `<artifactId>`, `<version>` | `group`, `version`, and the folder name |
| Where jars come from | Maven Central, implicit | `repositories { mavenCentral() }`, explicit |
| A dependency | a nine-line `<dependency>` block | one string: `"group:artifact:version"` |
| "Tests only" | `<scope>test</scope>` | the *configuration name*: `testImplementation` |
| "Needed to compile, provided at runtime" | `<scope>provided</scope>` | `compileOnly` |
| Which Java | `<maven.compiler.release>` | `java { toolchain { … } }` |
| Run the tests | `mvn test` | `./gradlew test` |
| Where the code lives | `src/main/java`, `src/test/java` | `src/main/java`, `src/test/java` — identical |

That last row is the important one. Gradle adopted Maven's standard layout wholesale, so moving between the two is a matter of reading a different build file, not reorganising a project.

The one genuine concept swap is scope. Maven says *what a dependency is for* with a `<scope>` element; Gradle says it by *which bucket you put the dependency in* — `implementation`, `testImplementation`, `compileOnly`. Same idea, different grammar.

Fourteen lines against roughly forty of XML, and the difference is not only length.

**The build file is a program.** Kotlin or Groovy, not XML. You can write a loop, a condition, a function. That is genuinely more powerful, and it is genuinely how build files become unreadable — a Maven POM is boring and you can always tell what it does, which on a large team is a feature.

**It is faster on big projects.** Gradle tracks which inputs changed and skips work whose inputs did not, and it caches build outputs — sometimes across machines. On a ten-module build this is the difference between eight minutes and forty seconds. On a one-module build it is the difference between two seconds and two seconds.

| | Maven | Gradle |
|---|---|---|
| Build file | `pom.xml`, declarative | `build.gradle(.kts)`, a program |
| Learning curve | shallow — there is one way | steeper — there are several |
| Speed on large builds | fine | markedly better |
| Predictability | high | depends on what was written |
| Used with Spring | yes — the common default | **yes, also fully supported** — just less common |
| Also strong in | enterprise Java generally | Android, and larger Java shops |

**Gradle is a first-class Spring option — do not read this table as "Maven for Spring, Gradle for Android".** [start.spring.io](https://start.spring.io) offers Maven and Gradle from the same dropdown, and every Spring Boot starter you will meet in this course resolves identically either way. Spring Boot even ships its own Gradle plugin alongside its Maven plugin. The Spring team builds Spring itself with Gradle. What is true is that Maven is what you will *more often be handed* on a Java team, which is a statement about how many teams chose it, not about what Spring supports.

**What this course uses, and why.** Everything here is Maven, because the rest of the curriculum is Maven and because `pom.xml` is the file you are most likely to be handed on your first day. Recognise a `build.gradle.kts`, be able to find the dependency block in it, and do not let anyone tell you one of them is correct.

> **Pause.** Your team's build takes 40 seconds and the project has one module. Somebody proposes migrating to Gradle for speed. What is the honest cost side of that decision — and what would have to be true for it to be worth doing?

---

## Lambdas

Now the language half. It exists for a reason you have already met without noticing.

### The problem

Some interfaces have exactly one method. `Comparator` has `compare`. `Runnable` has `run`. `Predicate` has `test`. Before Java 8, handing one of those to a method meant writing this:

```java
Comparator<Employee> bySalary = new Comparator<Employee>() {
    @Override
    public int compare(Employee a, Employee b) {
        return Double.compare(b.salary(), a.salary());
    }
};
```

Six lines. Exactly one of them says anything: `Double.compare(b.salary(), a.salary())`. The other five are the compiler asking you to fill in a form it could have filled in itself — it already knows the interface, so it already knows the method name, the parameter types and the return type.

### The lambda

```java
Comparator<Employee> bySalary = (a, b) -> Double.compare(b.salary(), a.salary());
```

Same type. Same object at runtime. Same behaviour. The five ceremonial lines are gone because the compiler recovers every one of them from the type on the left.

The syntax, in four steps of shrinking:

```java
(Employee a, Employee b) -> { return Double.compare(b.salary(), a.salary()); }   // everything spelled out
(a, b) -> { return Double.compare(b.salary(), a.salary()); }                     // types inferred
(a, b) -> Double.compare(b.salary(), a.salary())                                 // one expression: no braces, no return
name -> name.toUpperCase()                                                       // one parameter: no parentheses
() -> System.out.println("hello")                                                // no parameters: empty parentheses
```

Braces mean "a block", and a block needs an explicit `return`. No braces means "this expression is the answer". Mixing the two — `(a, b) -> { Double.compare(a, b); }` — compiles to a method that returns nothing, and the error you get talks about incompatible types.

### Where a lambda gets its type

A lambda has no type of its own. It takes the type of the thing it is assigned to, and that thing must be an interface with **exactly one abstract method**. Such an interface is called a **functional interface**.

```java
@FunctionalInterface
public interface Discount {
    double applyTo(double amount);
}

Discount tenPercent = amount -> amount * 0.9;
```

`@FunctionalInterface` is optional and you should still write it. It does one thing: it makes the compiler refuse the interface if it ever has two abstract methods. Without it, a colleague adds a second method, and the error appears at every place a lambda was written — not at the change that caused it.

Two things that do **not** break the rule: `default` methods and `static` methods have bodies, so they are not abstract. `Comparator` has a dozen of them (`reversed()`, `thenComparing(...)`) and is still a functional interface.

### The four you will meet everywhere

`java.util.function` has forty-odd interfaces. You need four, plus `Comparator`:

| Interface | Method | Takes | Gives back | Used for |
|---|---|---|---|---|
| `Predicate<T>` | `test(T)` | one value | `boolean` | filtering |
| `Function<T,R>` | `apply(T)` | one value | another | transforming |
| `Consumer<T>` | `accept(T)` | one value | nothing | doing something with it |
| `Supplier<T>` | `get()` | nothing | a value | producing one on demand |
| `Comparator<T>` | `compare(T,T)` | two values | `int` | ordering |

```java
Predicate<Employee> senior   = e -> e.salary() > 80000;
Function<Employee, String> name = e -> e.name();
Consumer<String> print       = s -> System.out.println(s);
Supplier<List<String>> empty = () -> new ArrayList<>();
```

`Predicate` and `Function` also **combine**, which is the part people miss:

```java
Predicate<Employee> inEngineering = e -> e.department().equals("Engineering");

senior.and(inEngineering)     // both
senior.or(inEngineering)      // either
senior.negate()               // not
```

Three named ideas and a combinator beat one fused condition that nobody can name.

### Method references

When your lambda does nothing but call an existing method, say so:

```java
e -> e.name()               →  Employee::name
s -> s.toUpperCase()        →  String::toUpperCase
s -> Integer.parseInt(s)    →  Integer::parseInt
() -> new ArrayList<>()     →  ArrayList::new
System.out::println         // an instance method of a particular object
```

Four kinds — static, an instance method of one particular object, an instance method of whichever object arrives, and a constructor. You do not need to be able to name the four. You need to read `Employee::name` as "the name of each one" without slowing down.

### Capture, and "effectively final"

A lambda can read local variables from around it:

```java
String prefix = "Hello, ";
Supplier<String> greet = () -> prefix + "world";
```

But only if the variable is never reassigned. Add `prefix = "Hi, ";` anywhere in the method and the compiler stops you:

```
error: local variables referenced from a lambda expression
       must be final or effectively final
```

**Effectively final** means: you could have written `final` in front of it and nothing would have broken.

The reason is worth understanding rather than memorising. The lambda may run long after the method that created it has returned — on another thread, or next Tuesday — so it captures the *value*, not the variable. If the variable could still change, you would have two answers to "what is `prefix`", and which one you got would depend on timing. Java refuses the ambiguity instead of resolving it badly.

Fields are a different story: a lambda can read and write `this.something` freely, and that is exactly why lambdas that touch shared state need the same care as any other concurrent code.

> **Pause.** Which of these can be a lambda, and why?
> 1. `interface Handler { void handle(Event e); }`
> 2. `interface Repo { void save(Order o); Order find(long id); }`
> 3. `interface Named { String name(); default String shout() { return name().toUpperCase(); } }`
> 4. `abstract class Task { abstract void run(); }`

---

## Streams

Lambdas made it cheap to pass behaviour around. Streams are what happens when the library takes that seriously.

### The loop that does four things

```java
List<String> result = new ArrayList<>();
for (Employee e : staff) {
    if (e.salary() > 60000) {
        result.add(e.name());
    }
}
result.sort(Comparator.naturalOrder());
```

Nothing is wrong with this. It works, everyone can read it, and if it is the clearest thing in the file then leave it alone.

But read what it actually says: *make an empty list; walk the collection; test each one; take a field; append it; sort the result.* Six instructions describing the mechanics. The intent — "the names of everyone over 60000, sorted" — is not written down anywhere. You reconstruct it every time you read the loop.

```java
List<String> result = staff.stream()
        .filter(e -> e.salary() > 60000)
        .map(Employee::name)
        .sorted()
        .toList();
```

The mechanics are gone. What is left is the sentence you would have said out loud.

### The shape of a pipeline

Every pipeline has exactly three parts:

```
staff.stream()                      ← 1. a SOURCE
     .filter(...)                   ← 2. INTERMEDIATE operations, any number
     .map(...)                      ←    each returns another stream
     .sorted()
     .toList();                     ← 3. one TERMINAL operation, which ends it
```

Sources: `collection.stream()`, `Arrays.stream(array)`, `Stream.of(a, b, c)`, `IntStream.range(0, 10)`.

A stream is **not** a collection. It holds no elements, it can be consumed only once, and — this is the important one — it **never modifies its source**. `staff` is exactly as it was afterwards. That is why the loop above had to copy the list before sorting it and the pipeline did not.

### Nothing happens until the terminal operation

```java
staff.stream()
     .filter(e -> e.salary() > 60000)
     .map(Employee::name);          // nothing has run. nothing at all.
```

Intermediate operations are **lazy**. They record what you asked for and return. Only a terminal operation pulls elements through.

And when it does, they go through **one at a time, all the way down** — not stage by stage. `filter` sees Alice, and if she passes, `map` sees Alice immediately, before `filter` has looked at Bob. Which is what makes this work:

```java
staff.stream()
     .filter(e -> e.salary() > 60000)
     .findFirst();          // stops the moment it finds one
```

On a thousand employees, that examines as few as one. The equivalent loop with an early `return` does the same thing, and now you know they are the same thing.

The commonest beginner bug in this material is a pipeline with no terminal operation — code that looks complete, compiles if you assign it to a `Stream`, and does nothing whatsoever.

### The operations worth knowing today

**Intermediate — stream in, stream out:**

| | |
|---|---|
| `filter(Predicate)` | keep the ones that pass |
| `map(Function)` | replace each with something else |
| `sorted()` / `sorted(Comparator)` | order them |
| `distinct()` | drop repeats — uses `equals`, which your records already have |
| `limit(n)` / `skip(n)` | the first n / everything after the first n |
| `flatMap(Function)` | each element becomes a stream; flatten them into one |

**Terminal — the stream ends here:**

| | |
|---|---|
| `toList()` | the answer, as a `List` |
| `collect(Collector)` | the answer, in any shape you like |
| `count()` | how many |
| `anyMatch` / `allMatch` / `noneMatch` | a `boolean`, and it stops as soon as it knows |
| `findFirst()` / `findAny()` | an `Optional`, because there may be none |
| `min` / `max` | an `Optional`, for the same reason |
| `forEach(Consumer)` | do something with each. Returns nothing |
| `reduce(...)` | fold them into one value |

**Numbers are their own thing.** `mapToInt`, `mapToDouble` and friends give you an `IntStream` or `DoubleStream`, which is where `sum()` and `average()` live — a `Stream<Employee>` has neither:

```java
double payroll = staff.stream().mapToDouble(Employee::salary).sum();
double average = staff.stream().mapToDouble(Employee::salary).average().orElse(0);
```

`average()` returns an `OptionalDouble`, and it is right to. The average of no employees is not zero; it is a question with no answer. `.orElse(0)` is you deciding what to do about that, in the open.

### Collectors

`collect` is the general terminal operation, and `Collectors` is a class full of ready-made recipes:

```java
.collect(Collectors.toList())          // .toList() is the short way
.collect(Collectors.toSet())
.collect(Collectors.joining(", "))     // one String, separators handled
.collect(Collectors.counting())
.collect(Collectors.summarizingDouble(Employee::salary))   // count, sum, min, max, average in one pass
```

### `groupingBy` — the one to memorise

```java
Map<String, List<Employee>> byDept = staff.stream()
        .collect(Collectors.groupingBy(Employee::department));
```

One line for what is otherwise a `HashMap`, a loop and a `computeIfAbsent`. And it takes a second argument — a **downstream** collector — that says what to do with each group instead of just listing it:

```java
Map<String, Long> headcount = staff.stream()
        .collect(Collectors.groupingBy(Employee::department, Collectors.counting()));

Map<String, Double> averagePay = staff.stream()
        .collect(Collectors.groupingBy(Employee::department,
                 Collectors.averagingDouble(Employee::salary)));

Map<String, List<String>> namesByDept = staff.stream()
        .collect(Collectors.groupingBy(Employee::department,
                 Collectors.mapping(Employee::name, Collectors.toList())));
```

The downstream collector is the idea that makes collectors worth learning, and it is the one that takes a week to feel natural. It is most of your homework.

### When not to use a stream

This is not a style rule. These are cases where the loop is the better code:

- **You need the index.** Streams have no index. `IntStream.range(0, list.size())` to get one is a loop wearing a costume.
- **You need to modify the source.** Streams do not. `removeIf` exists for that.
- **You need to `break` out with real logic.** `findFirst` and `anyMatch` cover the easy cases; anything more complicated is clearer as a loop.
- **The lambda has to catch a checked exception.** None of the standard functional interfaces declare `throws`, so the `try/catch` goes inside the lambda, and it is ugly there.
- **It is genuinely harder to read.** A five-line pipeline that a colleague has to trace with a finger is worse than a seven-line loop. Readability is the whole justification for streams; a pipeline that is not readable has nothing left to offer.

Performance is rarely the argument in either direction. On small collections a loop is faster and it does not matter; on very large ones `parallelStream()` exists and you should not reach for it until you have measured.

> **Pause.** Rewrite each of these as a pipeline, or say why you would not:
> 1. Sum the salaries of everyone in HR.
> 2. Find the position in the list of the first employee earning over 100000.
> 3. Print every name.
> 4. Remove everyone who has left the company from the list you were given.

---

## Exercise — Lambda and Stream Drills

**22 checks, three sets, in `lambda-drills/`.** This is a real Maven project and the checks are real JUnit tests. You will write tests like them next session; today you only run them.

Every drill shows you working imperative code as a comment, and asks for the same result as a lambda or a stream. Each one fits on a line.

### Step 0 — finish the POM (~10 min)

Open `pom.xml`. Two `TODO` blocks, and nothing builds until both are done: the compiler release property, and the JUnit dependency with `<scope>test</scope>`. The exact text to add is in the comments.

Then open the `pom.xml` **as a project** in IntelliJ — **File → Open**, select the `pom.xml` file itself, and choose *Open as Project*. Opening the folder instead is the commonest mistake and gives you an IDE that knows nothing.

Run the tests once now, before writing anything. **All 22 fail.** That is correct — every drill returns `null` or `0`. You want to see red before you go looking for green.

### Step 1 — Set A, lambdas (~15 min)

`LambdaDrills.java`, eight drills: `Predicate`, `Function`, `Consumer`, `Supplier`, `Comparator`, your own functional interface, a captured parameter, and two predicates joined with `.and(...)`.

A8 asks for two predicates combined. A single fused lambda passes the test and misses the point — write it the way it asks.

### Step 2 — Set B, streams that reshape a list (~12 min)

`StreamDrills.java`, seven drills: `filter`, `map`, `sorted`, `distinct`, `limit`, and chaining them. B6 is three operations in one pipeline; the order you put them in is the whole drill.

### Step 3 — Set C, streams that produce an answer (~15 min)

`ReportDrills.java`, seven drills ending in a number, a boolean, a `String` or a `Map`. C7 is `groupingBy` with a downstream collector — the shape you will use most often in real service code.

If you do not reach Set C in class, finish it at home. The homework does not depend on it.

### How you check your work

In IntelliJ: right-click `src/test/java` → **Run 'All Tests'**. Or the **Maven** tool window → **Lifecycle → test**. Or, in a terminal beside the `pom.xml`:

```
mvn test
```

Done looks like:

```
Tests run: 22, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### If you get stuck

| What you see | What it means |
|---|---|
| `package org.junit.jupiter.api does not exist` | POM TODO 2 — the JUnit dependency |
| `records are not supported in -source 8` | POM TODO 1 — the compiler release property |
| `Result should not be null` | that drill still returns `null` |
| `local variables referenced from a lambda expression must be final or effectively final` | you reassigned a variable the lambda reads |
| `incompatible types: Stream<String> cannot be converted to List<String>` | no terminal operation. Add `.toList()` |
| `Discount is not a functional interface` | it has two abstract methods now. Put it back |
| `cannot find symbol: method getName()` | `Employee` is a record. It is `name()` |
| No green arrows, and `Employee` is unknown | you opened the folder, not the `pom.xml` |

### Finished early?

Three extensions in `lambda-drills/README.md`, none checked by the tests. The third one — write `namesByDepartment` as a loop as well, and compare the two side by side — is the one worth doing. Deciding which you would rather maintain is a judgement you should form deliberately rather than inherit.

---

## Homework — Bookshop Drills

**12 checks, in `homework/`, a separate Maven project in a different domain.** Budget 45–60 minutes. It does not continue from the class exercise; if class went badly, start here clean.

In class, streams produced a list or a number. The homework produces `Map`s — which is what a reporting method in a real service returns. Set D is `groupingBy` with downstream collectors and `partitioningBy`; set E is `flatMap`, `joining`, `summarizingDouble` and `toMap`.

Two are deliberately harder than anything you did in class, and both are things you will actually hit at work:

- **D6** needs `maxBy` downstream, which gives you an `Optional` per group, and `collectingAndThen` to unwrap it. Expect to look it up.
- **E4** wants `toMap` where two records share a key. Write it with two arguments first, on purpose, and read the `IllegalStateException: Duplicate key`. Then fix it. That is a five-second diagnosis for the rest of your career, bought for one minute now.

Its POM is complete — you did that part in class.

---

## Recap

- A build tool answers three questions you cannot answer by hand: **where do the jars come from**, **is this build the same everywhere**, and **where does the code go**.
- A Maven project is **coordinates** + **dependencies** + **a standard layout**. `groupId:artifactId:version` names it; other projects use exactly those three lines to depend on it.
- **Scope** is not optional detail. `test` keeps your testing library out of the jar you ship; `provided` says the server already has it.
- Phases run **in order**, and asking for one runs all the earlier ones. `mvn package` tests. `mvn clean test` is your reset button.
- **Transitive dependencies** are the best and the worst of Maven. `mvn dependency:tree` is how you look; **nearest wins** is how conflicts resolve; `NoSuchMethodError` is what it looks like when that goes wrong.
- **Gradle** is the same job with a programmable build file and better caching. Recognise it, be able to find the dependency block, do not have an opinion about which is correct.
- A **lambda** is an anonymous implementation of an interface with exactly one abstract method. It takes its type from the left-hand side; that is where the parameter types come from.
- Captured local variables must be **effectively final**, because the lambda captures a value and may outlive the method.
- A **stream** is a source, some lazy intermediate operations, and exactly one terminal operation. Nothing runs before the terminal operation, and elements flow through one at a time.
- **`groupingBy` with a downstream collector** is the single most useful thing in `Collectors`.
- Loops are still correct. Index, mutation, checked exceptions, and plain readability are all good reasons to write one.

---

## Glossary

| Term | Meaning |
|---|---|
| **POM** | `pom.xml`, the Project Object Model — the whole description of a Maven project |
| **Coordinates** | `groupId:artifactId:version`, the unique name of an artifact |
| **Artifact** | the thing a build produces or consumes — usually a `.jar` |
| **Maven Central** | the public repository nearly every Java dependency comes from |
| **Local repository** | `~/.m2/repository`, your downloaded copy of everything |
| **Scope** | when a dependency is on the classpath, and whether it is shipped |
| **Phase** | one step of the lifecycle. Asking for a phase runs the earlier ones |
| **Goal** | one piece of work a plugin can do. Phases are bound to goals |
| **Transitive dependency** | a dependency of a dependency, fetched for you |
| **Nearest wins** | Maven's rule for version conflicts: fewest steps from your POM; ties broken by declaration order in your POM. Never by "newest" or "tested against" |
| **Functional interface** | an interface with exactly one abstract method |
| **Lambda** | `(params) -> body`, an anonymous implementation of one |
| **Method reference** | `Type::method`, a lambda that only calls one existing method |
| **Effectively final** | never reassigned after initialisation, so `final` would have been legal |
| **Stream** | a pipeline over a source. Holds nothing, changes nothing, runs once |
| **Intermediate operation** | returns another stream. Lazy — records the request and returns |
| **Terminal operation** | ends the pipeline and makes it run. Exactly one per pipeline |
| **Collector** | a recipe for gathering a stream into a result — a list, a `String`, a `Map` |
| **Downstream collector** | the second argument to `groupingBy`: what to do with each group |

---

## Take it home

- Open a Java project you did not write — anything on GitHub — and read its `pom.xml` or `build.gradle`. Count the dependencies you recognise. Then run `mvn dependency:tree` and count how many arrived that nobody declared.
- Find a loop in your own old code that builds a list. Rewrite it as a pipeline. Then decide, honestly, which one you would rather read in a year. Both answers are respectable; having a reason is the point.
- Look up `Collectors.teeing`. You will not need it soon. It is worth seeing once, because it shows how far the collector idea goes.

---

## Looking ahead

You ran tests today without writing any. Next session you write them: **JUnit 5** properly, **AssertJ** for assertions that read like sentences, and **Mockito** for standing in for the things you do not want to call for real. Every one of those arrives as three lines in the POM you just learned to read — which is why the build tool came first.

The switch expressions and pattern matching promised at the end of last session come after that.
