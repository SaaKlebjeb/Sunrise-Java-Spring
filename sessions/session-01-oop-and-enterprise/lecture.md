# Java OOP & the Enterprise Context

*A guided lesson. Read it start to finish; stop at the **Pause** prompts and actually answer them before reading on.*

**Before you start**

- You should be comfortable with Java classes, interfaces and collections. If any of that is shaky, review *Sunrise Java Fundamentals* sessions 1–5 first.
- Install **JDK 25** and an editor — **IntelliJ IDEA** (Community Edition is fine) is what the course assumes. Open a terminal and run `java -version` and `javac -version`; both should print 25. A JRE-only install will not do — `javac` is the compiler and you need it.
- **You edit in IntelliJ, but you run from the terminal.** This session is a folder of plain `.java` files with no build tool, and IntelliJ cannot reliably find a main class in that arrangement without Maven — so the run instructions below are all terminal commands. IntelliJ has a terminal built in (**View → Tool Windows → Terminal**, or **Alt+F12**) if you would rather not leave the window. From Maven onwards you get IntelliJ's run buttons back.
- This session writes plain Java. No Spring yet — that is deliberate. You cannot appreciate what Spring does for you until you have done it by hand once.

**By the end you'll be able to:**

- Say which of the four OOP pillars enterprise Java actually leans on, and why.
- Explain "program to an interface, not an implementation" in terms of *what can change*.
- Say which parts of Java are polymorphic and which only look like it.
- Distinguish **Java SE** from **Java EE / Jakarta EE**, describe what a Jakarta EE container does, and say where Spring sits — and where it deliberately does not.
- Wire a service's collaborators through constructor injection, and say why that beats `new`.
- Predict which classes must be recompiled when a requirement changes.

---

## The four pillars, reviewed honestly

You already know the four pillars. You can define them. That is not the same as knowing *what they are for* in a system with fifty classes and four developers.

Here is the honest version:

| Pillar | What the textbook says | What it is actually for in enterprise Java |
|---|---|---|
| **Abstraction** | Hide complexity, expose essentials | Naming a capability (`PaymentGateway`) separately from any way of providing it |
| **Encapsulation** | Keep fields private | Keeping a change *inside* one class instead of rippling across twenty |
| **Inheritance** | Reuse code from a parent | Rarely used for reuse. Mostly framework hooks and typing |
| **Polymorphism** | One interface, many forms | **The one that carries the weight.** How you swap an implementation without touching a caller |

Notice the shape of that right-hand column. Every entry is about **change** — what can change, what has to change with it, and what is safe. That is not an accident. In a program you write once and throw away, none of the four pillars buy you anything. They are all insurance against a future edit, and enterprise software is nothing but future edits.

One example of each, in the domain you will use in the exercise.

### Encapsulation — a change that stays put

Start with the version that looks simplest:

```java
public class Order {
    public String customerEmail;
    public double total;
}
```

This works. You can build one, read the total, print a receipt. For an afternoon's script it is fine. Two things go wrong with it in a real system, and they go wrong slowly.

**Problem one: it cannot defend itself.**

```java
Order order = new Order();
order.total = -9999;                  // nothing stops this
order.customerEmail = "not-an-email"; // nor this
```

A public field has no opinion. Any code anywhere can put anything in it, including code written next year by someone who has never read this class. The class cannot enforce a single rule about its own contents.

Now the closed version:

```java
public class Order {
    private final String customerEmail;
    private final double total;

    public Order(String customerEmail, double total) {
        if (customerEmail == null || !customerEmail.contains("@")) {
            throw new IllegalArgumentException("not an email: " + customerEmail);
        }
        if (total < 0) {
            throw new IllegalArgumentException("total cannot be negative");
        }
        this.customerEmail = customerEmail;
        this.total = total;
    }

    public String customerEmail() { return customerEmail; }
    public double total()         { return total; }
}
```

The constructor is now the **only door into the object**, and you can put a guard on a door. An `Order` that exists is an `Order` that passed the checks. That property has a name — an **invariant**: something true of every instance, for its whole life, that no caller can break.

`final` is what makes the invariant hold *for the whole life*. Without it, a setter could put the object back into a state the constructor would have rejected. With it, the object is checked once and then cannot change. An object that cannot change also cannot be corrupted by two threads at once, which is why you will see `private final` on nearly every field in every Spring class you write for the rest of this course.

**Problem two: blast radius.**

This is the one people underrate. A requirement arrives:

> "Stop storing money as a `double`. We are losing pennies on large orders. Store whole cents."

With the public-field version, `total` is not a private detail — it is part of the public shape of the class. Every line anywhere in the codebase that says `order.total` is coupled to the fact that a total is *a double, in dollars*. Change the field and every one of those lines stops compiling. Four callers in a teaching example; four hundred in a real system, across modules owned by other teams.

With the closed version, the change is entirely internal:

```java
public class Order {
    private final long totalCents;                       // changed

    public Order(String customerEmail, double dollars) {
        this.totalCents = Math.round(dollars * 100);     // changed
        ...
    }

    public double total() { return totalCents / 100.0; } // changed
}
```

Three lines, one file. Nothing outside can tell the difference. **That is the whole product `private` sells you**: not secrecy, but a boundary that changes cannot cross.

> **Run it.** In a terminal, change into the `examples/` folder and run:
>
> ```
> java BlastRadius.java
> ```
>
> That is the whole command — one file, no compile step, no build tool. Java has been able
> to run a single source file directly since version 11.
>
> It prints two orders that behave identically, then breaks one of them. Then do the
> experiment in the comment at the bottom of the file — change the public `double total`
> to a `long totalCents` and **count the compile errors**. Run `javac BlastRadius.java` and
> the compiler lists every one of them, then tells you the total. Each error is a line
> somewhere else that has to be opened because of a decision made inside one class. That
> count is the blast radius, and you just measured it.

**`private final` should be your default for fields.** Loosen it when you have a reason, not the other way round.

> **Pause.** A public getter and a public field both let a caller read the total. So what does the getter actually buy you? Name two changes you can make behind a getter that you could not make behind a field.

### Abstraction — naming the capability, not the mechanism

```java
public interface OrderRepository {
    void save(Order order);
}
```
Read what this does *not* say. No MySQL. No SQL, connections, tables, transactions. It names a capability — *somewhere to put an order* — and stops. Those other things are details of **one way** of keeping the promise.

The value shows when there is more than one way:

```java
public class MySqlOrderRepository implements OrderRepository {
    public void save(Order order) {
        // open a connection, begin a transaction, INSERT, commit
    }
}

public class InMemoryOrderRepository implements OrderRepository {
    private final List<Order> stored = new ArrayList<>();
    public void save(Order order) {
        stored.add(order);
    }
}
```

Two mechanisms, wildly different in cost and complexity, keeping the **same promise**. Any code written against `OrderRepository` works with either — including code written before the second one existed.

**A good abstraction hides the mechanism. A leaky one advertises it.** Here is the same interface done badly:

```java
public interface OrderRepository {
    Connection getConnection();          // leak: only a database has one of these
    void executeInsert(String sql);      // leak: names SQL out loud
    void setAutoCommit(boolean flag);    // leak: a JDBC concept, not a storage concept
}
```

This compiles. It even looks tidy. But `InMemoryOrderRepository` cannot honestly implement it — there is no connection to return — so you would end up returning `null` and hoping. The interface has quietly become "a MySQL repository, spelled as an interface", and every caller now depends on the mechanism after all. You paid for the abstraction and got none of the benefit.

The test to apply: **could a completely different mechanism implement this interface without lying?** If a file-based, in-memory, or fake-for-testing version would have to return `null` or throw `UnsupportedOperationException`, the interface is leaking.

> **Pause.** Which of these belong on an `EmailSender` interface, and which are leaks?
> `send(String to, String body)` · `setSmtpPort(int port)` · `boolean isConnected()` · `sendWithRetry(String to, String body, int attempts)`
> For each leak, say what a console-only implementation would have to do about it.

### Inheritance — the one you will use least

Inheritance is the pillar most over-taught and least used. In enterprise Java it earns its place in exactly one common situation: **plugging into a framework that will call you.**

```java
public class OrderServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) { ... }
}
```

You are not reusing `HttpServlet`'s code so much as **filling in a slot it defines**. The framework holds the main loop; your subclass is the part it calls. Keep that image — it comes back at the end of the session under the name *Inversion of Control*.

Now the trap. Inheritance used for **reuse** rather than for *is-a* goes wrong in a way that is hard to undo:

```java
// tempting, and wrong
public class DiscountedOrder extends Order {
    @Override
    public double total() {
        return super.total() * 0.9;
    }
}
```

Nothing here fails immediately, which is what makes it dangerous. But you have just broken a promise the rest of the system relies on: everywhere in the codebase, an `Order`'s total is *the number it was constructed with*. Now, sometimes, it is not. Every accounting check, every audit report, every equality comparison written against `Order` is silently wrong for this one subclass, and none of them mention `DiscountedOrder` anywhere — so no search will find them.

The fix is almost always **composition**: hold the thing instead of inheriting from it.

```java
public class DiscountPolicy {
    private final double rate;

    public DiscountPolicy(double rate) { this.rate = rate; }

    public double apply(Order order) {
        return order.total() * (1 - rate);
    }
}
```

`Order` keeps its meaning. The discount is a separate idea with a separate name, and it can be swapped, tested and configured on its own. You have gone from one class doing two jobs to two classes doing one each.

Two questions worth asking before every `extends`:

1. **Is-a, or has-a?** "A discounted order *is an* order" sounds fine in English, which is exactly the trap. Ask it about the *contract* instead: is every promise `Order` makes still true here? If not, it is not an is-a.
2. **Am I inheriting to get code, or to fit a slot?** Reaching for a parent's methods is a reuse motive, and composition does reuse better.

> **Pause.** Open any project you have written. Count the `extends` and count the `implements`. Which is more common? Now look at each `extends` and put it in one of two piles: *framework hook* or *code reuse*. How big is the second pile, and could each one have been a field instead?

### Polymorphism — the one that carries the weight

```java
OrderRepository repository = new MySqlOrderRepository();
repository.save(order);

repository = new InMemoryOrderRepository();
repository.save(order);
```

`repository.save(order)` is **the same line twice**, and it does two different things.

```
OrderRepository repository = new MySqlOrderRepository();
^^^^^^^^^^^^^^^                ^^^^^^^^^^^^^^^^^^^^^^
the DECLARED type              the ACTUAL object
```

- The **declared type** decides what you are *allowed to call*. The compiler checks that, before anything runs.
- The **actual object** decides what *actually runs*. The JVM decides that, at the moment of the call.

Two decisions, two deciders, two moments. And the consequence is the sentence the rest of the course rests on:

> **The code doing the calling does not know which implementation it got. So whoever *creates* the object decides the behaviour of code they never touched.**

You have been relying on this since your first week of Java without anyone naming it:

```java
List<Order> orders = new ArrayList<>();
```

`List` is the interface — what you can do. `ArrayList` is one implementation — how it is actually stored. Swap in `new LinkedList<>()` and not one other line changes.

#### Three things that are *not* polymorphic

This is where people get burned, and it is worth ten minutes now to save you an afternoon later. Only **non-private, non-static instance methods** are chosen by the actual object. Three things that look similar are chosen by the **declared type**, at compile time:

**1. Fields are hidden, never overridden.**

```java
class Parent { String label = "PARENT"; }
class Child extends Parent { String label = "CHILD"; }

Parent asParent = new Child();
Child  asChild  = new Child();

asParent.label   // "PARENT"  ← the declared type decided
asChild.label    // "CHILD"
```

The same kind of object gives two different answers depending on which variable you are holding it in. Nothing about the object changed. This is one more reason fields are `private`: a private field cannot be accessed through a declared type from outside, so the confusion cannot arise.

**2. `static` methods are hidden, never overridden.**

```java
class Parent { static String describe() { return "PARENT"; } }
class Child extends Parent { static String describe() { return "CHILD"; } }
```

A `static` method belongs to the class, not to any object, so there is no object to dispatch on. `Parent.describe()` and `Child.describe()` are simply two different methods that share a name. This is why you cannot put a `static` method on an interface and expect implementations to replace it — and, more usefully, it is why **static utility methods are hard to substitute in tests**. A `static` call is welded in at compile time. There is nothing to inject.

**3. `private` methods are not inherited, so they are not overridden.**

```java
class Parent {
    private String secret() { return "PARENT"; }
    String callSecret()     { return secret(); }   // always Parent's
}
class Child extends Parent {
    private String secret() { return "CHILD"; }    // a different method entirely
}

new Child().callSecret()   // "PARENT"
```

> **Run it.** From the `examples/` folder: `java DispatchDemo.java`
>
> **Predict all four answers on paper first.** The first half shows dispatch working; the
> second shows the three cases above. Getting a prediction wrong on paper is how you remember it.

Then note the practical rule this gives you, because it explains a design choice you will see everywhere for the rest of the course:

> **The only part of Java you can swap out from the outside is a non-private, non-static instance method reached through an interface.** That is why enterprise code puts its behaviour there and almost nowhere else.

**Enterprise Java is built almost entirely on abstraction + polymorphism.** Inheritance is a distant fourth.

---

## Program to an interface

> **Program to an interface, not an implementation.**

Everyone can recite it. Here is the *why*, with code.

### Version A

```java
public class OrderService {
    private final MySqlOrderRepository repository = new MySqlOrderRepository();
    private final SmtpOrderEmailSender emailSender = new SmtpOrderEmailSender();

    public void placeOrder(Order order) {
        repository.save(order);
        emailSender.send(order.customerEmail(), "Order confirmed");
    }
}
```

Nothing here is stupid. It compiles, it works, and it is what most working code looks like. There is no bug. The problem only shows up when something needs to **change**.

Ask five questions:

1. **Test `placeOrder` without a MySQL server running?** No. `new MySqlOrderRepository()` runs the instant an `OrderService` is created.
2. **Test it without sending a real email?** No — your test emails a real customer, every run.
3. **Move to PostgreSQL — what changes?** `OrderService`. Read that again: the rules about *placing an order* must be edited because someone picked a different database.
4. **Switch to SendGrid — what changes?** `OrderService` again.
5. **How many reasons does this class have to change?** Three. Two too many.

The diagnosis in one sentence: **it knows what it wants *and* exactly how it will be done. The second half is the problem.**

### Version B

```java
public class OrderService {
    private final OrderRepository repository;    // declared type: the interface
    private final EmailSender emailSender;

    public OrderService(OrderRepository repository, EmailSender emailSender) {
        this.repository = repository;            // handed in from outside
        this.emailSender = emailSender;
    }

    public void placeOrder(Order order) {        // unchanged
        repository.save(order);
        emailSender.send(order.customerEmail(), "Order confirmed");
    }
}
```

Two changes only: **the fields are typed by interfaces**, and **the objects arrive through the constructor**. `placeOrder` is untouched — the logic was never the problem.

| | Version A | Version B |
|---|---|---|
| Test without MySQL? | ❌ no | ✅ pass any `OrderRepository` |
| Test without real email? | ❌ no | ✅ pass a fake `EmailSender` |
| Move to PostgreSQL? | edit `OrderService` | **new class**, `OrderService` untouched |
| Switch to SendGrid? | edit `OrderService` | **new class**, `OrderService` untouched |
| Reasons to change? | **3** | **1** — the ordering rules |

"Untouched" is stronger than it sounds. `OrderService` is not merely spared an *edit* — its compiled `.class` file comes out **byte-for-byte identical**, because nothing in its source changed. (A real build tool would not even bother recompiling it; the blunt `javac *.java` you will use today rebuilds the folder every time, but the bytes it produces for this class are the same ones.) A file that never changes cannot acquire a new bug. You will confirm this yourself in the exercise.

### Someone still calls `new`

The `new` did not vanish. The **decision moved outward**, to the place that assembles the application:

```java
public class Main {
    public static void main(String[] args) {
        OrderRepository repository  = new MySqlOrderRepository();
        EmailSender     emailSender = new SmtpOrderEmailSender();

        OrderService service = new OrderService(repository, emailSender);
        service.placeOrder(new Order("alice@example.com", 250.00));
    }
}
```

Four names for what just happened:

| Name | Means |
|---|---|
| **Dependency** | an object another object needs to do its job |
| **Dependency injection (DI)** | supplying those from outside instead of constructing them |
| **Constructor injection** | doing it through the constructor — the form to default to |
| **Assembler** *(composition root)* | the one class that knows every concrete type. Today: `Main` |

Why the constructor specifically? There are three ways to hand an object its collaborators, and they are not equal:

| Form | Looks like | Why not |
|---|---|---|
| **Constructor** | `new OrderService(repo, mail)` | ✅ the default. Dependencies **required**, fields stay `final`, object is valid the moment it exists |
| Setter | `service.setRepository(repo)` | An object can exist half-wired. Fields cannot be `final`. Forget one call and you get a `NullPointerException` at 2 a.m., far from the cause |
| Field (reflection) | `@Autowired private OrderRepository repo;` | The class cannot be constructed at all without the framework — including in a plain unit test. You will see this in old Spring code; do not write new code this way |

The constructor form makes a whole category of bug impossible rather than merely unlikely: **if it compiles, every dependency is present.** The encapsulation habit from earlier, paying off.

**You have just written by hand the thing Spring automates.** Once you meet the Spring container, it replaces this `main()` method, and *nothing else about the design changes*. `OrderService` will look exactly as it does above.

> **Pause.** A teammate says "this is over-engineering for two dependencies." Give the strongest version of their argument, then answer it. Does your answer depend on how likely the database is to change, or on whether you intend to write tests at all?

---

## Where "enterprise" comes in

So far everything has been plain object design. It applies to any Java program. "Enterprise" adds one thing: **your code no longer owns the main loop.** Something else — a server — accepts the request, decides when to call you, and cleans up after you. The rest of this half is about what that something is.

### Java SE, Java EE, and what "EE" was added to

The names only make sense as a pair, so start with the split.

**Java SE — Standard Edition.** The JVM plus the standard library: `java.lang`, `java.util`,
collections, threads, files, sockets. Everything you have written so far is Java SE. You run it
standalone — your `main()` starts, does its work, exits — and any library beyond the standard
one, you brought in yourself.

**Java EE — Enterprise Edition.** A *second* set of APIs sitting on top of Java SE, covering the
server-side problems the standard library deliberately does not: receiving HTTP requests, mapping
objects to database tables, transactions across resources, validating incoming data. The useful way
to picture them is as **extensions of the Java standard library** — the same kind of thing as
`java.util`, just not shipped inside the JVM.

And here is the historical catch: you did not download them either. You got them by running your
application **inside an application server** — WildFly, WebLogic, WebSphere, GlassFish — and *the
server* provided the implementations of every one of those APIs. Your application was a `.war` file
you handed over. The server owned the APIs; your code only used them.

| | Java SE | Java EE / Jakarta EE |
|---|---|---|
| What it is | JVM + standard library | extra server-side APIs on top of SE |
| Package prefix | `java.*` | `javax.*`, now `jakarta.*` |
| How you run it | standalone — your own `main()` | deployed into an application server |
| Who provides the implementations | the JDK | the application server |

**Then the name changed.** Oracle stopped developing the Java EE standard and handed the
specifications to the **Eclipse Foundation**. Eclipse could not keep the name "Java" — Oracle owns
that trademark — so the whole thing was rebranded **Jakarta EE**. Same lineage, same specs, new
steward, new name. The package prefix had to change for the same trademark reason, which is the
`javax.*` → `jakarta.*` trap two pages from here.

*(Where does Spring fit in that picture? Nowhere yet — deliberately. Hold the question; there is a
section on exactly that at the end of this half.)*

### Jakarta EE is specifications, not code

The naming changed three times, which is most of why it is confusing:

**J2EE** (1999) → **Java EE** (2006) → **Jakarta EE** (2018)

Same lineage. Jakarta EE is a **set of specifications** — interfaces and rules — for server-side Java. It is **not an implementation**. Vendors implement the specs.

| Specification | What it is for | Common implementation |
|---|---|---|
| **Servlet** | Receiving HTTP requests | Tomcat, Jetty, Undertow |
| **Bean Validation** | Declaring rules like `@NotNull`, `@Email` | Hibernate Validator |
| **JSON binding** | Turning objects into JSON and back | Jackson *(de facto)* |
| **JPA** | Mapping objects to database tables | **Hibernate**, EclipseLink |
| **JTA** | Transactions across resources | Narayana, Atomikos |
| **CDI** | Dependency injection, the Jakarta way | Weld *(Spring does this its own way)* |

Look at that table, then back at the four pillars. **Spec vs. implementation *is* interface vs. implementation** — the same idea, scaled from one class to the whole industry. You write against `jakarta.persistence.EntityManager`; Hibernate provides it; you could in principle swap in EclipseLink. Same trick, bigger blast radius.

**Why does the industry bother?** For the same reason `OrderService` types its field as `OrderRepository`. A specification is an interface with more than one vendor behind it, and it exists so that no single vendor owns your codebase. In the 2000s, application servers were expensive, and a company that had written directly against WebLogic's own classes could not leave WebLogic without a rewrite. Writing against the spec was how you kept the option open. It is the "reasons to change" habit applied at the scale of a purchasing decision.

Two practical notes you will hit within a week of real work:

- **The `javax.*` → `jakarta.*` rename.** When Oracle handed Java EE to Eclipse, the *name* Java EE could not come along — and neither could the `javax.*` package prefix. Jakarta EE 9 renamed every package: `javax.servlet.http.HttpServlet` became `jakarta.servlet.http.HttpServlet`. Nothing else changed; the classes are the same. But it means **an import copied from a pre-2020 tutorial will not compile against a modern Spring Boot project**, and the error message will not explain why. Spring Boot 3 and later are `jakarta.*`. If a Stack Overflow answer says `javax.persistence`, it is written for the old world — change the prefix and it usually works.
- **"Spec-compliant" is a claim you can check.** Implementations publish which version of which spec they satisfy. This matters when you upgrade.

> **Pause.** Given that framing — is Hibernate a "framework" or an "implementation"? What about Spring? What about Tomcat? For each, name the spec (if any) it implements.

### The request lifecycle

Every web request in Java lands in the same place. Hold this shape in your head for the rest of the course:

```
Browser
  │  POST /orders   { "email": "...", "total": 250.00 }
  ▼
Servlet container   (Tomcat)
  │  owns the socket · speaks HTTP · manages a thread pool
  │  builds an HttpServletRequest object
  ▼
Servlet
  │  your code finally runs
  ▼
Your application logic
  │  reads the request, does the work, produces a result
  ▼
Servlet writes to HttpServletResponse  →  Tomcat serializes it back to HTTP  →  Browser
```

A **servlet container** is the piece that owns the network socket, understands the HTTP protocol, manages a pool of threads, and calls your code once per request. Without one you would be parsing HTTP text by hand.

Be precise about the division of labour, because it explains a lot of later behaviour:

| The container does | You do |
|---|---|
| Listens on the port, accepts TCP connections | — |
| Parses raw HTTP bytes into an `HttpServletRequest` | — |
| Keeps a pool of worker threads | — |
| Picks a thread and calls your servlet | Everything inside that call |
| Turns your `HttpServletResponse` back into HTTP bytes | — |
| Handles keep-alive, timeouts, TLS | — |

### Your servlet — how it is registered, how it is called

The container has to be told which URLs go to which servlet. Historically that was an XML file,
`web.xml`; since Servlet 3.0 it is usually an annotation:

```java
@WebServlet("/orders")
public class OrderServlet extends HttpServlet { ... }
```

A URL pattern, mapped to a class — that is the whole registration mechanism. Note the `extends`:
inheritance-as-a-framework-hook from the pillars, in the wild.

You will not write servlets in this course, so **two things are worth carrying forward** and the
rest you can let go.

**One: the container calls *one* servlet instance from many threads at once.** It does not create a
new servlet per request — it keeps a single instance and calls it from a pool, one thread per
in-flight request. So anything you store in a field is shared by every request in the system
simultaneously, and request-scoped data belongs in **local variables and parameters** instead.

Remember the shape of that, not the details. It is the reason **Spring beans are singletons by
default** and the reason you will be told, repeatedly, to keep them stateless — the same constraint
wearing a different name. We come back to it properly when you meet bean scopes.

**Two: inside the method you are handed an `HttpServletRequest` and an `HttpServletResponse`, and
nothing else.** Reading the body, parsing the JSON, converting the types, validating them, choosing
a status code, writing the response JSON — all of it is yours to write, for every endpoint,
slightly differently each time. That is how a codebase ends up with error responses in three
different shapes.

Spring MVC takes over every one of those jobs: Jackson parses, Bean Validation checks, Spring
decides the status code and the error shape. **None of that work disappeared — it moved behind
`DispatcherServlet`.** Knowing this layer exists is the difference between *using* Spring and being
*confused by* Spring: when a 400 comes back with a message you did not write, you now know who
wrote it.

### Where Spring sits

A common misconception: *"Spring is an alternative to Java EE."* Not quite. A short history makes the relationship clear:

| When | What happened |
|---|---|
| 1999–2002 | J2EE arrives. The unit of work is the **EJB** — powerful, and painful: XML descriptors, mandatory interfaces, near-impossible to unit test |
| 2002–2003 | Rod Johnson publishes a book arguing most of that is unnecessary, with a small container as the example. The code becomes **Spring** |
| 2004–2013 | Spring grows. Still XML-heavy at first, then annotations. You still deploy a `.war` into a server |
| 2014 | **Spring Boot**. Sensible defaults, no XML, and the servlet container is *embedded in your application* |
| today | `java -jar app.jar` starts your app *and* its server. There is no server to install |

**Be precise about what Spring is not.** Spring is **not an implementation of the Java/Jakarta EE
specification.** It is not certified against it, it has never tried to be, and no version of it
ships "the Jakarta EE platform". It was created *in opposition* to Java EE and to the
application-server concept behind it.

What Spring does do is **selectively reuse** a few Jakarta specs where they were genuinely good,
mostly under the hood and mostly hidden behind its own abstractions: the Servlet API underneath
Spring MVC, Bean Validation behind `@NotNull`, JPA and Hibernate behind Spring Data. So yes, you
will see `jakarta.*` imports in a Spring project — but that is **reuse, not compliance**, and it is
where the relationship ends. Notice what Spring pointedly did *not* adopt: CDI, the Jakarta
dependency-injection spec. It built its own container instead. That is the whole story in one row of
the table above.

So the tension was never Spring versus every last spec. What Spring rejected was the
**application-server deployment model**:

```
Traditional Java EE                     Spring Boot
─────────────────────                   ───────────
Application Server (WildFly)            java -jar app.jar
  └─ your app.war                         └─ embedded Tomcat
      └─ (server installed, patched,          └─ your app
         configured, shared by apps)
```

The practical differences are worth naming:

| | Application server | Spring Boot |
|---|---|---|
| What you build | a `.war` | an executable `.jar` |
| Who owns the server | ops, separately installed | your build, pinned in `pom.xml` |
| "Works on my machine" | server config differs per environment | the server *is* the artifact |
| Starting it | deploy to a running server | `java -jar` |
| Multiple apps | share one server | one process each |

### So what do you do with all of this?

**Park it.** You are here to learn Spring, and for that you can **forget Jakarta EE for now.** You
will not learn the Jakarta specs in this course, you will not write an EJB, and you will never
deploy anything to an application server. If you finish this session thinking "I now have to go
study Jakarta EE too" — no. You do not.

The reason we spent this half of the session on it is not so you can use it. It is so the vocabulary
stops being a mystery:

- when an import says `jakarta.servlet`, you know what world it came from;
- when someone says "the container", you know what job that thing does;
- when a Stack Overflow answer says `javax.persistence`, you know it is written for the old world
  and what to change;
- when Spring Boot's docs say "embedded Tomcat", you know what it is embedding and what it replaced.

Later — once you are deep enough into Spring — you will find it **exposing Jakarta concepts
underneath its own abstractions**: an `EntityManager` under a Spring Data repository, an
`HttpServletRequest` you can ask for as a controller parameter, a `jakarta.validation` annotation on
a request object. That is the moment this half of the session pays off. Until then, everything you
need is Spring.

> **Pause.** Why does "the server lives inside your jar" matter once you start containerising with Docker? Think about what has to go inside the image in each model. Guess before you get there.

---

## The habit: count reasons to change

One habit to build now, because every session in this course reinforces it.

When you look at a class, ask: **who can force this file to change?**

Back to Version A of `OrderService`:

| Change request | Who files the ticket | Does `OrderService.java` change? |
|---|---|---|
| "Orders over $1000 need manager approval" | Product owner | ✅ yes — correct, that is its job |
| "We are moving to PostgreSQL" | Infrastructure | ❌ **yes — wrong** |
| "Switch email provider to SendGrid" | Ops | ❌ **yes — wrong** |
| "Add a retry when the mail server is down" | Ops | ❌ **yes — wrong** |

Three of four are wrong. In Version B, only the first one touches the file. **That is the entire measurable benefit**, and it is why the extra interfaces pay for themselves.

Note what makes it measurable: you are not counting lines, or classes, or cleverness. You are counting **people who can send you a ticket that lands in this file**. That number is real, and you can count it in five minutes for any class you own.

You will meet this table again in the design-patterns course as the **Single Responsibility Principle**.

> **Pause.** Draw this table for one class in your own code. Be honest about how many distinct people could file a ticket that opens that file.

---

## Exercise — Invert `OrderService`

Open the `src/` folder.

**Almost everything is already written for you.** The two interfaces, all three implementations, and `Order` are complete and working — read them, but do not change them. Your job is **four small edits** across two files.

**How you check your work:**

In a terminal, change into the `src/` folder and run these two commands:

```
javac *.java
java Verify
```

The first compiles every file in the folder; the second runs the checker. Repeat both after
each edit — press the up-arrow twice in the terminal and you are re-running in two keystrokes.

*(If your shell does not expand `*.java`, name the files instead: `javac Order.java
OrderRepository.java EmailSender.java MySqlOrderRepository.java SmtpOrderEmailSender.java
InMemoryOrderRepository.java OrderService.java Main.java Verify.java`.)*

You can run the application itself the same way, with `java Main`.

**8 checks**, PASS or FAIL each. When all 8 pass you are done — you do not need to ask anyone whether it is right. Do not edit `Verify.java`.

One thing to expect: it will not *compile* until EDIT 2 is done, because `Verify` needs your constructor to exist. Before that, `javac *.java` prints exactly one error — `constructor OrderService in class OrderService cannot be applied to given types` — and no `.class` files are produced. That is normal, and one clean error is what a correct starting point looks like.

### `OrderService.java` — two edits (~10 min)

**EDIT 1 — change the field types to the interfaces and delete the `new`.**

```java
// before
private final MySqlOrderRepository repository = new MySqlOrderRepository();

// after
private final OrderRepository repository;
```

Same for the email sender: `private final EmailSender emailSender;`

When you are done, **the word `new` must not appear anywhere in the file.** That is the mechanical test for whether the inversion is complete.

**EDIT 2 — add the constructor.**

```java
public OrderService(OrderRepository repository, EmailSender emailSender) {
    this.repository = repository;
    this.emailSender = emailSender;
}
```

Notice `placeOrder()` does not change at all.

### `Main.java` — two edits (~10 min)

**EDIT 3 — uncomment the four lines that wire the real application.** The variables are declared with the *interface* types, not the concrete classes; that is the habit, and it stops you accidentally using a MySQL-only method later.

Expected output:

```
[MySQL] Saving order for alice@example.com totalling $250.00
[SMTP] To: alice@example.com | Order confirmed
```

**EDIT 4 — uncomment the three lines that build a second `OrderService`**, this time with `InMemoryOrderRepository` — a class that already exists and already works — reusing the same email sender.

Expected additional output:

```
[Memory] Stored order 1 for bob@example.com
[SMTP] To: bob@example.com | Order confirmed
```

### If you get stuck

The four errors people actually hit, and what each one means:

| The compiler says | What it means | Fix |
|---|---|---|
| `constructor OrderService in class OrderService cannot be applied to given types` | EDIT 2 is not done, or the parameter order is wrong | Add the constructor exactly as shown; the order is `(repository, emailSender)` |
| `variable repository might not have been initialized` | You deleted the `new` (EDIT 1) but have not assigned the field in a constructor yet | Finish EDIT 2 |
| `incompatible types: MySqlOrderRepository cannot be converted to OrderRepository` | Almost always the reverse — check you did not swap the two constructor arguments | Repository first, email sender second |
| `cannot find symbol: method connect()` | You typed the field as the interface, then called a method only the MySQL class has | That method should not be called from `OrderService` at all — that is the point of the exercise |

If `Verify` runs but some checks fail, read the failure text — each one names the edit it belongs to. Fix them in order; EDIT 3 and EDIT 4's checks cannot pass until 1 and 2 do.

### Then prove the point (~5 min)

EDIT 4 changed how the application stores orders. Ask yourself:

- Did you open `OrderService.java` to do it? **No.**
- Can you prove you never touched it? **Yes.** Right-click `OrderService.java` in IntelliJ's
  Project view and choose **Local History → Show History**. IntelliJ keeps its own record of
  every change you make to a file, independently of Git — and it records edits, not builds,
  so it is unaffected by the fact that `javac *.java` recompiles the whole folder every time.

You will see exactly two entries — EDIT 1 and EDIT 2, both made before you ever ran the
program successfully. **EDIT 4 produced no entry at all**, because you did not open the file.
Compare that with `Main.java`, whose history shows every edit of the afternoon.

That is the whole lesson, and it is the only part of today you should be able to demonstrate to a sceptic in thirty seconds.

### Before you close it

Read `Verify.java` — specifically `RecordingRepository` and `RecordingSender` near the top. Those are **test doubles**: fake collaborators that record what happened so a check can assert on it.

They could not exist at all against the *original* `OrderService`, because it built its own collaborators and gave you no way to substitute anything. That is not a coincidence — it is the same property seen from the other side: **code that is easy to test is code whose dependencies come from outside.** The testing session gives this a name and a proper library.

### Finished early? Three extensions

These are **optional and not checked by `Verify`** — the 8 checks stay at 8. You check these by reading the output, which is the honest way to check a program that prints things. Do them in `Main.java` and new files; still do not touch `OrderService.java`.

**EXTENSION A — a third implementation (~5 min).**
Write `ConsoleEmailSender implements EmailSender` that prints `[CONSOLE] To: <email> | <message>` instead of pretending to use SMTP. Wire a third `OrderService` with it in `Main`. Count how many existing lines you had to change to introduce a whole new way of sending mail. (It should be zero, plus the lines that build the new service.)

**EXTENSION B — a decorator (~10 min).**
Write:

```java
public class LoggingOrderRepository implements OrderRepository {
    private final OrderRepository delegate;

    public LoggingOrderRepository(OrderRepository delegate) {
        this.delegate = delegate;
    }

    public void save(Order order) {
        System.out.println("[LOG] about to save order for " + order.customerEmail());
        delegate.save(order);
        System.out.println("[LOG] saved");
    }
}
```

Then wire it in `Main` **around** the MySQL repository:

```java
OrderRepository repository = new LoggingOrderRepository(new MySqlOrderRepository());
```

Read that line twice. It is an `OrderRepository` that *contains* an `OrderRepository`. `OrderService` cannot tell the difference and does not recompile. You have just added behaviour to every save in the system without editing a single existing class — which is the same trick Spring AOP performs automatically later in the course, and you now know it is not magic.

**EXTENSION C — break it deliberately (~5 min).**
In `Main`, pass the arguments to the `OrderService` constructor in the wrong order and try to compile. What stops you, and at what moment? Now imagine the same two dependencies had been supplied by setters instead of a constructor. What would have stopped you then, and when would you have found out?

---

## Recap

- Enterprise Java runs on **abstraction + polymorphism**. Inheritance is minor — mostly framework hooks.
- **`private` is about blast radius**, not secrecy: it is the boundary a change cannot cross. `final` extends that guarantee across the object's whole life.
- A good abstraction **hides the mechanism**. If a different mechanism could not implement it without lying, it is leaking.
- Prefer **composition over inheritance** for reuse. `extends` is for fitting a framework's slot.
- **Declared type** decides what you may call; the **object in it** decides what runs. That gap is polymorphism — and it applies only to non-private, non-static instance methods. Fields, statics and privates are chosen at compile time.
- **Program to an interface** = name the capability separately from the way of providing it, so the way can change without the caller changing.
- **Dependency injection** = an object receives its collaborators instead of constructing them. **Constructor injection** is the default form; it keeps fields `final`, makes dependencies required, and makes a half-wired object impossible.
- The class that knows all the concrete types is the **assembler**. Today that is `main()`. Later it is the Spring container, and nothing else changes.
- **Java SE** is the JVM and its standard library, run standalone. **Java EE** was a set of extra server-side APIs on top of it, supplied by an **application server**; Oracle handed the specs to the Eclipse Foundation and they were renamed **Jakarta EE**.
- **Jakarta EE** is specifications; Hibernate/Tomcat/Jackson are implementations. Specs exist so no single vendor owns your codebase. `javax.*` became `jakarta.*` in Jakarta EE 9.
- A **servlet container** owns HTTP and calls your code once per request, from a **shared instance on many threads** — so request state belongs in local variables, never in fields.
- **Spring is not an implementation of Jakarta EE.** It was created in opposition to Java EE and the application-server model; it reuses selected specs (Servlet, Bean Validation, JPA) under its own abstractions, and built its own DI container rather than adopting CDI. Spring Boot embeds the server in your jar.
- **To learn Spring, you can forget Jakarta EE for now.** It resurfaces later, underneath Spring's own abstractions — that is when this half pays off.
- Habit to keep: **count who can force this class to change.** More than one is a smell.

## Glossary

| Term | Meaning |
|---|---|
| Declared type vs. actual object | What you may call vs. what actually runs |
| Polymorphism | The gap between those two, used deliberately |
| Blast radius | How far a change inside one class is felt outside it |
| Invariant | Something true of every instance for its whole life, that no caller can break |
| Leaky abstraction | An interface whose methods reveal one particular mechanism |
| Composition | Holding an object as a field instead of inheriting from it |
| Decorator | An implementation that wraps another implementation of the same interface and adds behaviour |
| Dependency | An object another object needs in order to do its job |
| Dependency injection (DI) | Supplying an object's dependencies from outside instead of it constructing them |
| Constructor injection | DI performed through the constructor; makes dependencies required and the object immutable |
| Inversion of Control (IoC) | The general idea that the framework calls you, rather than you calling the framework |
| Assembler / composition root | The single place that knows the concrete classes and wires the object graph |
| Test double | A stand-in collaborator used in a test — only possible if dependencies come from outside |
| Java SE (Standard Edition) | The JVM and its standard library — `java.*`. What you run standalone |
| Jakarta EE (formerly J2EE / Java EE) | A set of server-side Java specifications layered on top of Java SE: Servlet, JPA, Bean Validation, and others |
| Application server | WildFly, WebLogic, GlassFish — software you deploy a `.war` into, which supplies the Jakarta EE implementations for you |
| Servlet container | Software (Tomcat, Jetty) that speaks HTTP and invokes your servlet per request |
| Thread per request | The container's model: one servlet instance, many concurrent threads calling it |
| Stateless | Holding no per-request data in fields, so a shared instance is safe under concurrency |
| Specification vs. implementation | The interface the industry agrees on vs. a vendor's concrete provision of it |

## Take it home

Pick a class from any project you have written that calls `new` on something non-trivial — a database client, an HTTP client, a file writer, a clock.

1. Write down every reason that class could be forced to change, and who files the ticket.
2. Extract an interface for the thing it constructs.
3. Move the `new` up to the caller.
4. Write down what you can now test that you could not before.

Bring your answer to the testing session — it is the test we will write.

## Looking ahead

Next we replace the hand-written `Order` class with a **record** — the same class in one line, with the constructor guard still in it — and model the result of `placeOrder` as a **sealed interface**, so the compiler can force you to handle every outcome.
