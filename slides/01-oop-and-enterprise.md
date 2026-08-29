---
layout: section
---

# Java OOP & the Enterprise Context

The four pillars, honestly · then where Spring actually lives

---
layout: default
---

# Learning Objectives

<v-clicks>

- Name which OOP pillar enterprise Java actually **leans on**
- Explain *program to an interface* in terms of **what can change**
- Say which parts of Java are **polymorphic** — and which only look like it
- Describe what a **servlet container** does, and why that makes your code shared
- Wire collaborators by hand with **constructor injection**
- Count **who can force a class to change**

</v-clicks>

---
layout: default
---

# The Four Pillars — the honest version

| Pillar | Textbook | What it's actually for |
|---|---|---|
| **Abstraction** | Hide complexity | Naming a capability apart from any way of providing it |
| **Encapsulation** | Private fields | Keeping a change *inside* one class |
| **Inheritance** | Reuse from a parent | Rarely reuse. Mostly framework hooks |
| **Polymorphism** | One interface, many forms | **Carries the weight** — swap an impl without touching the caller |

<v-click>

<div class="mt-4">

Every entry in that right-hand column is about **change**.

</div>

</v-click>

<v-click>

<div class="mt-2 text-green-300">

In a program you write once and throw away, none of the four buy you anything.
Enterprise software is nothing *but* future edits.

</div>

</v-click>

---
layout: section
---

# Encapsulation

Not secrecy. **Blast radius.**

---
layout: default
---

# Start with the version that looks simplest

```java
public class Order {
    public String customerEmail;
    public double total;
}
```

<v-click>

It works. For an afternoon's script it is genuinely fine.

</v-click>

<v-click>

<div class="mt-6">

Two things go wrong in a real system — and they go wrong **slowly**.

</div>

</v-click>

---
layout: default
---

# Problem 1 — it cannot defend itself

```java
Order order = new Order();
order.total = -9999;                   // nothing stops this
order.customerEmail = "not-an-email";  // nor this
```

<v-click>

<div class="mt-6">

A public field **has no opinion**. Any code, anywhere, written by anyone,
at any point in the next five years.

The class cannot enforce a single rule about its own contents.

</div>

</v-click>

---
layout: default
---

# The constructor is a door you can guard

```java {all|6-11|12-13|16-17}
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

---
layout: default
---

# That gives you an invariant

<div class="border-2 border-green-500/60 rounded-lg px-6 py-4 bg-green-500/10 mt-4">

**Invariant** — something true of *every* instance, for its *whole life*,
that no caller can break.

</div>

<v-click>

<div class="mt-6">

An `Order` that **exists** is an `Order` that **passed the checks**.

</div>

</v-click>

<v-click>

<div class="mt-6 text-sm">

And `final` is what makes it hold for the whole life — without it, a setter could
put the object back into a state the constructor would have rejected.

</div>

</v-click>

<v-click>

<div class="mt-3 text-green-300">

An object that cannot change also cannot be corrupted by two threads at once.
Remember that — it comes back after the break.

</div>

</v-click>

---
layout: default
---

# Problem 2 — blast radius

A requirement arrives:

<div class="border-l-4 border-yellow-500 pl-4 my-5">

*"Stop storing money as a `double`. We are losing pennies on large orders.
Store whole cents."*

</div>

<v-click>

With `public double total`, every line in the codebase that says `order.total`
is coupled to **"a double, in dollars"**.

</v-click>

<v-click>

<div class="mt-3">

Change the field → every one of those lines stops compiling.

Four callers in a teaching example. **Four hundred** in a real system,
across modules owned by other teams.

</div>

</v-click>

---
layout: default
---

# Behind a method, the same change is three lines

```java {2|5|8}
public class Order {
    private final long totalCents;                       // changed

    public Order(String customerEmail, double dollars) {
        this.totalCents = Math.round(dollars * 100);     // changed
    }

    public double total() { return totalCents / 100.0; } // changed
}
```

<v-click>

<div class="mt-6 text-center text-green-300 text-xl">

One file. Nothing outside can tell the difference.

</div>

</v-click>

<v-click>

<div class="mt-4 text-center text-sm">

That is what `private` sells you: **not secrecy — a boundary a change cannot cross.**

</div>

</v-click>

---
layout: default
---

# Run it — measure your own blast radius

<div class="text-sm mt-2">

From a terminal in `examples/` — `java BlastRadius.java`

</div>

<div class="grid grid-cols-2 gap-4 mt-6 text-sm">

<div class="border-2 border-blue-500/60 rounded-lg px-5 py-3 bg-blue-500/10">
<div class="text-blue-300 font-bold">What it prints</div>
<div class="mt-2">A public-field order and a private-field order behaving <strong>identically</strong> — then one of them being fed nonsense.</div>
</div>

<div class="border-2 border-green-500/60 rounded-lg px-5 py-3 bg-green-500/10">
<div class="text-green-300 font-bold">What you do</div>
<div class="mt-2">The experiment at the bottom of the file: change the public field to <code>long totalCents</code> and <strong>count the compile errors</strong>.</div>
</div>

</div>

<v-click>

<div class="mt-6 text-center">

That count **is** the blast radius. `private` is the tool that keeps it at zero.

</div>

</v-click>

---
layout: statement
---

# `private final` is your default

Loosen it when you have a reason.

Not the other way round.

---
layout: default
---

# Pause

<div class="border-l-4 border-yellow-500 pl-6 mt-8 text-xl">

A public getter and a public field both let a caller read the total.

**So what does the getter actually buy you?**

Name two changes you can make behind a getter
that you could not make behind a field.

</div>

---
layout: section
---

# Abstraction

Name the **capability**. Not the mechanism.

---
layout: default
---

# Read what it doesn't say

```java
public interface OrderRepository {
    void save(Order order);
}
```

<v-click>

<div class="grid grid-cols-2 gap-4 mt-6 text-sm">

<div class="border-2 border-red-500/50 rounded-lg px-5 py-3 bg-red-500/10">
<div class="text-red-300 font-bold">Not mentioned</div>
<div class="mt-2">MySQL · SQL · connections · tables · transactions · drivers</div>
</div>

<div class="border-2 border-green-500/60 rounded-lg px-5 py-3 bg-green-500/10">
<div class="text-green-300 font-bold">Mentioned</div>
<div class="mt-2">Somewhere to put an order.</div>
<div class="mt-2 opacity-70">That's it. That's the capability.</div>
</div>

</div>

</v-click>

---
layout: default
---

# One promise, two mechanisms

```java {all|1-5|7-12}
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

<v-click>

<div class="mt-4 text-green-300">

Wildly different in cost and complexity. **Same promise.**

Code written against `OrderRepository` works with either — including code
written *before the second one existed*.

</div>

</v-click>

---
layout: default
---

# The same interface, done badly

```java
public interface OrderRepository {
    Connection getConnection();          // only a database has one
    void executeInsert(String sql);      // names SQL out loud
    void setAutoCommit(boolean flag);    // a JDBC concept, not a storage concept
}
```

<v-click>

<div class="mt-5">

This compiles. It even looks tidy.

</div>

</v-click>

<v-click>

<div class="mt-3">

But `InMemoryOrderRepository` **cannot implement it honestly** — there is no
connection to return. So it returns `null` and hopes.

</div>

</v-click>

<v-click>

<div class="mt-3 text-red-300">

You have written "a MySQL repository, spelled as an interface."
You paid for the abstraction and got none of the benefit.

</div>

</v-click>

---
layout: statement
---

# The leak test

Could a **completely different mechanism**
implement this without lying?

If a fake would have to return `null` or throw
`UnsupportedOperationException` — it's leaking.

---
layout: default
---

# Pause — which are leaks?

<div class="text-lg mt-6">

On an `EmailSender` interface:

</div>

<div class="grid grid-cols-2 gap-3 mt-5 text-sm font-mono">

<div class="border-2 border-gray-500/50 rounded px-4 py-3 bg-gray-500/10">send(String to, String body)</div>
<div class="border-2 border-gray-500/50 rounded px-4 py-3 bg-gray-500/10">setSmtpPort(int port)</div>
<div class="border-2 border-gray-500/50 rounded px-4 py-3 bg-gray-500/10">boolean isConnected()</div>
<div class="border-2 border-gray-500/50 rounded px-4 py-3 bg-gray-500/10">sendWithRetry(to, body, attempts)</div>

</div>

<div class="mt-6 border-l-4 border-yellow-500 pl-4">

For each leak: what would a **console-only** implementation have to do about it?

</div>

---
layout: section
---

# Inheritance

The pillar most over-taught, and least used.

---
layout: default
---

# The one legitimate use: a framework hook

```java {all|1}
public class OrderServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) { ... }
}
```

<v-click>

<div class="mt-6">

You are not reusing `HttpServlet`'s code so much as **filling in a slot it defines.**

</div>

</v-click>

<v-click>

<div class="mt-4 border-2 border-purple-500/60 rounded-lg px-6 py-4 bg-purple-500/10">

The framework holds the main loop. Your subclass is the part it calls.

<div class="mt-2 text-sm opacity-80">Keep that image — it comes back at the end of the session under the name <strong>Inversion of Control</strong>.</div>

</div>

</v-click>

---
layout: default
---

# The trap — inheriting for reuse

```java
// tempting, and wrong
public class DiscountedOrder extends Order {
    @Override
    public double total() {
        return super.total() * 0.9;
    }
}
```

<v-click>

<div class="mt-5">

Nothing fails immediately. That is what makes it dangerous.

</div>

</v-click>

<v-click>

<div class="mt-4 text-red-300">

Everywhere in the codebase, an `Order`'s total is *the number it was constructed with*.

Now, sometimes, it isn't.

</div>

</v-click>

<v-click>

<div class="mt-4 text-sm">

Every accounting check, audit report and equality comparison written against `Order`
is silently wrong for this one subclass — and **none of them mention `DiscountedOrder`**,
so no search will find them.

</div>

</v-click>

---
layout: default
---

# The fix is almost always composition

```java
public class DiscountPolicy {
    private final double rate;

    public DiscountPolicy(double rate) { this.rate = rate; }

    public double apply(Order order) {
        return order.total() * (1 - rate);
    }
}
```

<v-click>

<div class="grid grid-cols-2 gap-4 mt-5 text-sm">

<div class="border-2 border-green-500/60 rounded-lg px-5 py-3 bg-green-500/10">
<div class="text-green-300 font-bold">`Order` keeps its meaning</div>
<div class="mt-1">No promise was broken.</div>
</div>

<div class="border-2 border-green-500/60 rounded-lg px-5 py-3 bg-green-500/10">
<div class="text-green-300 font-bold">The discount has a name</div>
<div class="mt-1">Swappable, testable, configurable on its own.</div>
</div>

</div>

</v-click>

<v-click>

<div class="mt-4 text-center">

One class doing two jobs → **two classes doing one each.**

</div>

</v-click>

---
layout: default
---

# Two questions before every `extends`

<div class="mt-6 space-y-5">

<div class="border-2 border-blue-500/60 rounded-lg px-6 py-4 bg-blue-500/10">
<div class="text-blue-300 font-bold">1 · Is-a, or has-a?</div>
<div class="mt-2 text-sm">"A discounted order <em>is an</em> order" sounds fine in English — that's the trap.
Ask it about the <strong>contract</strong>: is every promise the parent makes still true here?</div>
</div>

<div class="border-2 border-blue-500/60 rounded-lg px-6 py-4 bg-blue-500/10">
<div class="text-blue-300 font-bold">2 · Am I inheriting to get code, or to fit a slot?</div>
<div class="mt-2 text-sm">Reaching for a parent's methods is a <strong>reuse</strong> motive.
Composition does reuse better.</div>
</div>

</div>

---
layout: default
---

# Pause — audit your own `extends`

<div class="border-l-4 border-yellow-500 pl-6 mt-8">

Open any project you've written.

1. Count the `extends`. Count the `implements`. Which is more common?

2. Put each `extends` in one of two piles:
   **framework hook** or **code reuse**.

3. How big is the second pile? Could each one have been a **field** instead?

</div>

---
layout: section
---

# Polymorphism

The one that carries the weight.

---
layout: default
---

# The same line, twice, doing two things

```java
OrderRepository repository = new MySqlOrderRepository();
repository.save(order);

repository = new InMemoryOrderRepository();
repository.save(order);
```

<v-click>

<div class="mt-4 text-center">

```
OrderRepository repository = new MySqlOrderRepository();
^^^^^^^^^^^^^^^                ^^^^^^^^^^^^^^^^^^^^^^
the DECLARED type              the ACTUAL object
```

</div>

</v-click>

<div class="grid grid-cols-2 gap-4 mt-4 text-sm">

<v-click>

<div class="border-2 border-blue-500/60 rounded-lg px-5 py-3 bg-blue-500/10">
<div class="text-blue-300 font-bold">Declared type</div>
<div class="mt-1">what you're <strong>allowed to call</strong></div>
<div class="mt-1 text-xs opacity-70">the compiler · before anything runs</div>
</div>

</v-click>

<v-click>

<div class="border-2 border-purple-500/60 rounded-lg px-5 py-3 bg-purple-500/10">
<div class="text-purple-300 font-bold">Actual object</div>
<div class="mt-1">what <strong>actually runs</strong></div>
<div class="mt-1 text-xs opacity-70">the JVM · at the moment of the call</div>
</div>

</v-click>

</div>

---
layout: statement
---

# The sentence the course rests on

The code doing the calling **does not know**
which implementation it got.

So whoever **creates** the object decides the behaviour
of code they never touched.

---
layout: statement
---

# You've relied on this since week one

```java
List<Order> orders = new ArrayList<>();
```

`List` = what you can do. `ArrayList` = how it's stored.

Swap in `LinkedList` — **nothing else changes.**

---
layout: section
---

# Three things that are *not* polymorphic

Ten minutes now saves you an afternoon later.

---
layout: default
---

# 1 · Fields are hidden, never overridden

```java
class Parent { String label = "PARENT"; }
class Child extends Parent { String label = "CHILD"; }

Parent asParent = new Child();
Child  asChild  = new Child();
```

<v-click>

<div class="text-center mt-4 text-lg font-mono">

<div>asParent.label &nbsp;→&nbsp; <span class="text-red-300">"PARENT"</span></div>
<div>asChild.label &nbsp;&nbsp;→&nbsp; <span class="text-red-300">"CHILD"</span></div>

</div>

</v-click>

<v-click>

<div class="mt-5 text-center">

The **same kind of object**, two different answers — depending on which
variable you happen to be holding it in.

</div>

</v-click>

<v-click>

<div class="mt-4 text-sm text-green-300 text-center">

One more reason fields are `private`: then the confusion cannot arise at all.

</div>

</v-click>

---
layout: default
---

# 2 · `static` methods are hidden too

```java
class Parent { static String describe() { return "PARENT"; } }
class Child extends Parent { static String describe() { return "CHILD"; } }
```

<v-click>

<div class="mt-5">

A `static` method belongs to the **class**, not to any object.
There is no object to dispatch on — just two methods sharing a name.

</div>

</v-click>

<v-click>

<div class="mt-5 border-2 border-red-500/50 rounded-lg px-6 py-4 bg-red-500/10">

**The consequence that will bite you:** `static` utility methods are
*hard to substitute in tests*.

A `static` call is welded in at compile time. There is nothing to inject.

</div>

</v-click>

---
layout: default
---

# 3 · `private` methods aren't inherited

```java {all|3|7|10}
class Parent {
    private String secret() { return "PARENT"; }
    String callSecret()     { return secret(); }   // always Parent's
}

class Child extends Parent {
    private String secret() { return "CHILD"; }    // a different method entirely
}

new Child().callSecret()   // "PARENT"
```

<v-click>

<div class="mt-5">

`Child` did not override anything. It declared an unrelated method
that happens to have the same name.

</div>

</v-click>

---
layout: default
---

# Run it — predict first

<div class="text-sm mt-2">

From `examples/` — `java DispatchDemo.java`

</div>

<div class="text-sm mt-5">

| | Chosen by | When |
|---|---|---|
| **instance method** | the **actual object** | at the moment of the call |
| field | the declared type | at compile time |
| `static` method | the class name | at compile time |
| `private` method | the declaring class | at compile time |

</div>

<v-click>

<div class="mt-5 border-l-4 border-yellow-500 pl-4">

**Write your four predictions down before you run it.**
Getting one wrong on paper is how you remember it.

</div>

</v-click>

---
layout: statement
---

# The rule that explains everything after this

The only part of Java you can **swap out from the outside**
is a non-private, non-static **instance method**
reached through an **interface**.

That's why enterprise code puts its behaviour there,
and almost nowhere else.

---
layout: statement
---

> Program to an **interface**,
> not an **implementation**.

Everyone can recite it. Let's find out *why*.

---
layout: default
---

# Version A — programming to an implementation

```java {all|2-3|6-7}
public class OrderService {
    private final MySqlOrderRepository repository = new MySqlOrderRepository();
    private final SmtpOrderEmailSender emailSender = new SmtpOrderEmailSender();

    public void placeOrder(Order order) {
        repository.save(order);
        emailSender.send(order.customerEmail(), "Order confirmed");
    }
}
```

<v-click>

**Nothing here is stupid.** It compiles, it works, it's what most working code looks like.

There is no bug. The problem only appears when something needs to **change**.

</v-click>

---
layout: default
---

# Five questions

<div class="text-sm">

| Question | Version A |
|---|---|
| Test without MySQL running? | ❌ No — the `new` runs at construction |
| Test without emailing a real customer? | ❌ No — every run, including the 12 debug runs |
| Move to PostgreSQL — what changes? | `OrderService` |
| Switch to SendGrid — what changes? | `OrderService` |
| How many reasons to change? | **Three** |

</div>

<v-click>

<div class="mt-4">

Ordering logic has nothing to do with which database we use — yet
**the database can force this file open.**

</div>

</v-click>

<v-click>

<div class="mt-3 text-green-300">

It knows **what it wants** *and* **exactly how it will be done**. The second half is the problem.

</div>

</v-click>

---
layout: default
---

# Version B — programming to an interface

```java {all|2-3|5-8|10-13}
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

<v-click>

Two changes: **fields typed by interfaces**, **objects arrive through the constructor**.

`placeOrder` is untouched — the logic was never the problem.

</v-click>

---
layout: default
---

# The same five questions

<div class="text-sm">

| | Version A | Version B |
|---|---|---|
| Test without MySQL? | ❌ | ✅ pass any `OrderRepository` |
| Test without real email? | ❌ | ✅ pass a fake `EmailSender` |
| Move to PostgreSQL? | edit `OrderService` | **new class**, `OrderService` untouched |
| Switch to SendGrid? | edit `OrderService` | **new class**, `OrderService` untouched |
| Reasons to change | **3** | **1** — the ordering rules |

</div>

<v-click>

<div class="mt-5 text-green-300">

"Untouched" is stronger than it sounds: `OrderService.class` stays **byte-for-byte
identical** — nothing in its source changed. A real build tool wouldn't even recompile it.
And a file that never changes can't acquire a new bug.

</div>

</v-click>

---
layout: default
---

# So who calls `new`?

Somebody still has to. The decision moves **outward**:

```java {all|3-4|6-7}
public class Main {
    public static void main(String[] args) {
        OrderRepository repository  = new MySqlOrderRepository();
        EmailSender     emailSender = new SmtpOrderEmailSender();

        OrderService service = new OrderService(repository, emailSender);
        service.placeOrder(new Order("alice@example.com", 250.00));
    }
}
```

<div class="text-sm mt-4">

| Name | Means |
|---|---|
| **Dependency** | an object another object needs to do its job |
| **Dependency injection** | supplying those from outside instead of building them |
| **Constructor injection** | through the constructor — dependencies **required**, fields stay `final` |
| **Assembler** *(composition root)* | the one class that knows every concrete type. Today: `Main` |

</div>

---
layout: default
---

# Why the *constructor* specifically?

<div class="text-sm mt-3">

| Form | Looks like | Why not |
|---|---|---|
| **Constructor** | `new OrderService(repo, mail)` | ✅ **the default.** Required, `final`, valid the moment it exists |
| Setter | `service.setRepository(repo)` | Can exist half-wired. No `final`. Forget one call → `NullPointerException` at 2 a.m. |
| Field *(reflection)* | `@Autowired private OrderRepository repo;` | Cannot be constructed without the framework — **including in a plain unit test** |

</div>

<v-click>

<div class="mt-6 border-2 border-green-500/60 rounded-lg px-6 py-4 bg-green-500/10">

The constructor form makes a whole category of bug **impossible** rather than merely unlikely:

<div class="mt-2 text-green-300 text-lg">If it compiles, every dependency is present.</div>

</div>

</v-click>

---
layout: statement
---

# You just wrote what Spring automates

Later, the **Spring container** replaces this `main()` —
and *nothing else about the design changes*.

`OrderService` will look exactly as it does now. That's the point.

---
layout: default
---

# Pause — argue the other side

<div class="border-l-4 border-yellow-500 pl-6 mt-8 text-lg">

A teammate says:

*"This is over-engineering for two dependencies."*

<div class="mt-6">

1. Give the **strongest** version of their argument.
2. Then answer it.
3. Does your answer depend on how likely the database is to change —
   or on whether you intend to **write tests at all**?

</div>

</div>

---
layout: section
---

# Where "enterprise" comes in

Your code no longer owns the main loop.

---
layout: default
---

# That's the whole difference

<div class="mt-6 text-lg">

Everything so far was plain object design. It applies to any Java program.

</div>

<v-click>

<div class="mt-6 border-2 border-purple-500/60 rounded-lg px-6 py-5 bg-purple-500/10">

"Enterprise" adds **one** thing:

<div class="mt-3 text-purple-200 text-xl">

Something else — a server — accepts the request,
decides when to call you, and cleans up after you.

</div>

</div>

</v-click>

<v-click>

<div class="mt-6 text-center">

The rest of this half is about what that something is.

</div>

</v-click>

---
layout: default
---

# Two editions, one language

<div class="grid grid-cols-2 gap-6 mt-6 text-sm">

<div class="border-2 border-blue-500/60 rounded-lg px-5 py-4 bg-blue-500/10">

### Java SE — *Standard Edition*

The JVM + the standard library.

`java.lang` · `java.util` · collections
threads · files · sockets

**Everything you've written so far.**

Runs standalone: your `main()` starts,
does its work, exits.

</div>

<v-click>

<div class="border-2 border-purple-500/60 rounded-lg px-5 py-4 bg-purple-500/10">

### Java EE — *Enterprise Edition*

A **second** set of APIs, on top of SE.

HTTP requests · objects ↔ tables
transactions · validation

The server-side problems the standard
library deliberately doesn't solve.

</div>

</v-click>

</div>

<v-click>

<div class="mt-6 text-center text-green-300 text-lg">

Think of the EE APIs as **extensions of the Java standard library** —
same kind of thing as `java.util`, just not shipped with the JVM.

</div>

</v-click>

---
layout: default
---

# So where did those extra APIs come from?

<div class="mt-4 text-lg">

You didn't download them either.

</div>

<v-click>

<div class="mt-4">

You got them by running your application **inside an application server** —
**WildFly**, WebLogic, WebSphere, GlassFish.

```
Application Server  (WildFly)
  ├─ implementations of JPA, CDI, Servlet, JTA, ...
  └─ your app.war        ← you shipped this
```

</div>

</v-click>

<v-click>

<div class="mt-5 border-2 border-yellow-500/60 rounded-lg px-6 py-4 bg-yellow-500/10">

**The server owned the APIs. Your code only used them.**

You shipped a `.war`. Someone else installed, patched and configured the thing it ran in.

</div>

</v-click>

---
layout: default
---

# SE vs. EE — the summary

<div class="text-sm mt-6">

| | **Java SE** | **Java EE / Jakarta EE** |
|---|---|---|
| What it is | JVM + standard library | extra server-side APIs on top of SE |
| Package prefix | `java.*` | `javax.*`, now `jakarta.*` |
| How you run it | standalone — your own `main()` | deployed into an application server |
| Who provides the implementations | the JDK | the application server |

</div>

<v-click>

<div class="mt-6">

**Then the name changed.** Oracle stopped developing the Java EE standard and handed the
specs to the **Eclipse Foundation**.

</div>

</v-click>

<v-click>

<div class="mt-3 text-green-300">

Eclipse couldn't keep the name *"Java"* — Oracle owns the trademark.
So it was rebranded **Jakarta EE**.

Same lineage, same specs, new steward, new name — and, for the same trademark reason,
a new package prefix.

</div>

</v-click>

---
layout: default
---

# Jakarta EE = specifications, not code

**J2EE** (1999) → **Java EE** (2006) → **Jakarta EE** (2018)

Same lineage. Specs define interfaces and rules. **Vendors implement them.**

<div class="text-sm mt-4">

| Specification | What it's for | Implementation |
|---|---|---|
| **Servlet** | Receiving HTTP requests | Tomcat, Jetty |
| **Bean Validation** | `@NotNull`, `@Email` | Hibernate Validator |
| **JSON binding** | Objects ↔ JSON | Jackson *(de facto)* |
| **JPA** | Objects ↔ tables | **Hibernate** |
| **JTA** | Transactions | Narayana |
| **CDI** | DI, the Jakarta way | Weld *(Spring does its own)* |

</div>

---
layout: statement
---

# Look at that table, then back at the pillars

Spec vs. implementation **is**
interface vs. implementation.

The same idea, scaled from one class
to the whole industry.

---
layout: default
---

# Why does the industry bother?

<div class="mt-4">

Same reason `OrderService` types its field as `OrderRepository`.

</div>

<v-click>

<div class="mt-5 border-2 border-blue-500/60 rounded-lg px-6 py-4 bg-blue-500/10">

A **specification is an interface with more than one vendor behind it**,
and it exists so that no single vendor owns your codebase.

</div>

</v-click>

<v-click>

<div class="mt-5 text-sm">

In the 2000s application servers were expensive. A company that had written directly
against WebLogic's own classes **could not leave WebLogic without a rewrite**.

Writing against the spec was how you kept the option open.

</div>

</v-click>

<v-click>

<div class="mt-4 text-green-300 text-center">

"Count the reasons to change" — applied to a purchasing decision.

</div>

</v-click>

---
layout: default
---

# The trap you'll hit this week: `javax` → `jakarta`

When Oracle handed Java EE to Eclipse, the **name** couldn't come along.
Neither could the package prefix.

```java
javax.servlet.http.HttpServlet      // pre-2020
jakarta.servlet.http.HttpServlet    // Jakarta EE 9 onward
```

<v-click>

<div class="mt-5">

Nothing else changed. **The classes are the same.**

</div>

</v-click>

<v-click>

<div class="mt-5 border-2 border-red-500/50 rounded-lg px-6 py-4 bg-red-500/10">

An import copied from a pre-2020 tutorial **will not compile** against a modern
Spring Boot project — and the error will not explain why.

<div class="mt-2 text-sm">Spring Boot 3+ is `jakarta.*`. If Stack Overflow says `javax.persistence`,
change the prefix and it usually works.</div>

</div>

</v-click>

---
layout: default
---

# Pause — framework or implementation?

<div class="grid grid-cols-3 gap-4 mt-10 text-center">

<div class="border-2 border-gray-500/50 rounded-lg px-5 py-6 bg-gray-500/10">
<div class="text-xl font-bold">Hibernate</div>
</div>

<div class="border-2 border-gray-500/50 rounded-lg px-5 py-6 bg-gray-500/10">
<div class="text-xl font-bold">Spring</div>
</div>

<div class="border-2 border-gray-500/50 rounded-lg px-5 py-6 bg-gray-500/10">
<div class="text-xl font-bold">Tomcat</div>
</div>

</div>

<div class="mt-8 border-l-4 border-yellow-500 pl-4">

For each one: is it a **framework** or an **implementation**?
And which specification (if any) does it implement?

</div>

---
layout: default
---

# The request lifecycle

<div class="flex flex-col items-center gap-1 text-sm mt-2">

<div class="border-2 border-gray-500/60 rounded-lg px-6 py-2 bg-gray-500/10 font-mono">Browser</div>
<div class="text-xl text-gray-500">↓ <span class="text-xs font-mono">POST /orders</span></div>

<div class="border-2 border-green-500/60 rounded-lg px-6 py-2 bg-green-500/10">
<strong class="text-green-300">Servlet container</strong> <span class="opacity-70">(Tomcat)</span>
<div class="text-xs opacity-80">owns the socket · speaks HTTP · manages the thread pool</div>
</div>
<div class="text-xl text-gray-500">↓ <span class="text-xs font-mono">HttpServletRequest</span></div>

<div class="border-2 border-blue-500/60 rounded-lg px-6 py-2 bg-blue-500/10">
<strong class="text-blue-300">Servlet</strong>
<div class="text-xs opacity-80">your code finally runs</div>
</div>
<div class="text-xl text-gray-500">↓</div>

<div class="border-2 border-purple-500/60 rounded-lg px-6 py-2 bg-purple-500/10">
<strong class="text-purple-300">Your application logic</strong>
</div>

</div>

<v-click>

<div class="text-center mt-3 text-sm">

With Spring MVC you'll find **one** servlet sitting there: `DispatcherServlet`.

</div>

</v-click>

---
layout: two-cols
---

# Division of labour

**The container does**

<div class="text-sm mt-3">

- Listens on the port, accepts TCP
- Parses raw HTTP bytes into `HttpServletRequest`
- Keeps a pool of worker threads
- Picks a thread and **calls your servlet**
- Turns your response back into HTTP bytes
- Keep-alive, timeouts, TLS

</div>

::right::

<div class="mt-14"></div>

**You do**

<div class="text-sm mt-3">

- Everything inside that one call.

</div>

<v-click>

<div class="mt-8 border-l-4 border-green-500 pl-4 text-sm">

That asymmetry is the deal. You gave up the main loop and got
HTTP, threading and connection management for free.

</div>

</v-click>

---
layout: default
---

# Your servlet — how it's registered, how it's called

Historically an XML file, `web.xml`. Since Servlet 3.0, usually an annotation:

```java
@WebServlet("/orders")
public class OrderServlet extends HttpServlet { ... }
```

A URL pattern, mapped to a class. That is the whole registration mechanism.

<v-click>

<div class="mt-5 border-2 border-yellow-500/60 rounded-lg px-6 py-4 bg-yellow-500/10">

**One thing to carry forward:** the container does **not** make a servlet per request.
It makes **one instance** and calls it from **many threads at once**.

<div class="mt-2 text-sm">

So request data lives in **local variables** — never in fields.

</div>

</div>

</v-click>

<v-click>

<div class="mt-4 text-sm text-green-300">

Remember the shape, not the detail. It's the reason **Spring beans are singletons
and must stay stateless** — we come back to it with bean scopes.

</div>

</v-click>

---
layout: default
---

# What Spring took over

<div class="mt-3">

Inside the method you get an `HttpServletRequest` and an `HttpServletResponse` — and nothing else.

</div>

<div class="text-sm mt-4">

| In a raw servlet, you write... | Later, in Spring |
|---|---|
| Read the body, parse the JSON | **Jackson** |
| Convert and validate the fields | **Bean Validation** |
| Choose the status code | **Spring MVC** |
| Write the error JSON — differently each time | **Spring MVC**, one shape |

</div>

<v-click>

<div class="mt-5 border-2 border-purple-500/60 rounded-lg px-6 py-4 bg-purple-500/10 text-center">

**None of that work disappeared — it moved behind `DispatcherServlet`.**

</div>

</v-click>

<v-click>

<div class="mt-4 text-center text-green-300 text-sm">

That's the difference between **using** Spring and being **confused by** Spring:
when a 400 comes back with a message you didn't write, you now know who wrote it.

</div>

</v-click>

---
layout: default
---

# Where Spring sits — the short history

<div class="text-sm mt-3">

| When | What happened |
|---|---|
| **1999–2002** | J2EE arrives. The unit of work is the **EJB** — powerful, and painful: XML descriptors, mandatory interfaces, near-impossible to unit test |
| **2002–2003** | Rod Johnson publishes a book arguing most of that is unnecessary, with a small container as the example. The code becomes **Spring** |
| **2004–2013** | Spring grows. XML-heavy at first, then annotations. You still deploy a `.war` into a server |
| **2014** | **Spring Boot.** Sensible defaults, no XML, and the servlet container is *embedded in your application* |
| **today** | `java -jar app.jar` starts your app *and* its server. There is no server to install |

</div>

<v-click>

<div class="mt-4 text-green-300">

So the tension was never Spring **versus the specs**.

</div>

</v-click>

---
layout: two-cols
---

# Two deployment models

**Traditional Java EE**

```
Application Server (WildFly)
  └─ your app.war
```

Deploy *into* a heavyweight server
that ops installed, patched and
configured — and that other apps share.

::right::

<div class="mt-14"></div>

**Spring Boot**

```
java -jar app.jar
  └─ embedded Tomcat
      └─ your app
```

The server lives *inside* your jar.
It is pinned in your `pom.xml`
like any other dependency.

---
layout: default
---

# What that actually changes

<div class="text-sm mt-4">

| | Application server | Spring Boot |
|---|---|---|
| What you build | a `.war` | an executable `.jar` |
| Who owns the server | ops, installed separately | your build, pinned in `pom.xml` |
| "Works on my machine" | server config differs per environment | **the server *is* the artifact** |
| Starting it | deploy to a running server | `java -jar` |
| Multiple apps | share one server | one process each |

</div>

<v-click>

<div class="mt-6 text-center text-green-300">

What Spring rejected was never *every spec*.

It is anti **application server**.

</div>

</v-click>

---
layout: default
zoom: 0.95
---

# Be precise: what Spring is **not**

<div class="mt-4 border-2 border-red-500/60 rounded-lg px-6 py-4 bg-red-500/10 text-lg">

Spring is **not an implementation of the Java/Jakarta EE specification.**

</div>

<v-click>

<div class="mt-4 text-sm">

Not certified against it. Never tried to be. Ships no "Jakarta EE platform."
It was created *in opposition* to Java EE and the application-server concept behind it.

</div>

</v-click>

<v-click>

<div class="mt-5">

What it *does* do is **selectively reuse** a few specs, mostly under the hood,
mostly hidden behind its own abstractions:

<div class="text-sm mt-2">

| Jakarta spec | Where Spring quietly uses it |
|---|---|
| Servlet | underneath Spring MVC |
| Bean Validation | behind `@NotNull` on a request object |
| JPA / Hibernate | behind a Spring Data repository |
| **CDI** | ❌ **not adopted** — Spring built its own container |

</div>

</div>

</v-click>

<v-click>

<div class="mt-4 text-center text-green-300">

`jakarta.*` imports in a Spring project are **reuse, not compliance.**

</div>

</v-click>

---
layout: statement
---

# So what do you do with all of this?

## Park it.

<div class="text-xl mt-6 text-green-300">

You're here to learn Spring — and for that,
you can **forget Jakarta EE for now.**

</div>

<div class="text-base mt-6 opacity-80">

No Jakarta specs to memorise. No EJBs. No application server.
If you leave thinking *"now I have to go study Jakarta EE too"* — you don't.

</div>

---
layout: default
zoom: 0.95
---

# Then why did we spend an hour on it?

<div class="mt-4 text-lg">

Not so you can *use* it. So the vocabulary stops being a mystery.

</div>

<div class="text-sm mt-5">

| When you see... | You now know... |
|---|---|
| `import jakarta.servlet.…` | what world that came from |
| "the container" | what job that thing actually does |
| `javax.persistence` on Stack Overflow | it's written for the old world — and what to change |
| "embedded Tomcat" in the Boot docs | what it's embedding, and what it replaced |

</div>

<v-click>

<div class="mt-6 border-2 border-purple-500/60 rounded-lg px-6 py-4 bg-purple-500/10">

Later — once you're deep enough into Spring — you'll catch it **exposing Jakarta concepts
underneath its own abstractions**: an `EntityManager` under a Spring Data repository,
an `HttpServletRequest` you can ask for as a controller parameter.

<div class="mt-2 text-purple-200">

That's the moment this half of the session pays off. Until then: **Spring.**

</div>

</div>

</v-click>

---
layout: statement
---

# Pause — guess ahead

Why does *"the server lives inside your jar"*
matter once you start containerising with Docker?

<div class="text-base mt-6 opacity-80">

Think about what has to go inside the image in each model.

</div>

---
layout: default
---

# The habit: count reasons to change

| Change request | From | Does `OrderService` change? |
|---|---|---|
| "Orders > $1000 need approval" | Product | ✅ Yes — correct |
| "We're moving to PostgreSQL" | Infrastructure | ❌ **Wrong** |
| "Switch to SendGrid" | Ops | ❌ **Wrong** |
| "Retry when mail is down" | Ops | ❌ **Wrong** |

<v-click>

Three of four are wrong in Version A. In Version B, only the first touches the file.

</v-click>

<v-click>

<div class="mt-3 text-green-300">

You're not counting lines or cleverness. You're counting **people who can send you
a ticket that lands in this file.**

</div>

</v-click>

---
layout: section
---

# Exercise

## Invert `OrderService`

Almost everything is already written. **Four small edits.**

---
layout: default
---

# Exercise — four edits, two files

**Given and working — read, don't change:** `Order`, both interfaces,
`MySqlOrderRepository`, `SmtpOrderEmailSender`, `InMemoryOrderRepository`.

<div class="text-sm mt-4">

| | File | Edit |
|---|---|---|
| **1** | `OrderService.java` | field types → the interfaces; delete the `new` |
| **2** | `OrderService.java` | add the constructor (3 lines) |
| **3** | `Main.java` | uncomment: wire MySQL + SMTP |
| **4** | `Main.java` | uncomment: a 2nd service using `InMemoryOrderRepository` |

</div>

<div class="text-sm mt-4">

In `src/` — `javac *.java` then `java Verify` → **8 checks**.
Both commands after every edit. Up-arrow twice to repeat them.

</div>

<v-click>

It won't **compile** until EDIT 2 — `Verify` needs your constructor. One error, saying so. Normal.

</v-click>

---
layout: default
---

# What success looks like

After EDIT 3:

```
[MySQL] Saving order for alice@example.com totalling $250.00
[SMTP] To: alice@example.com | Order confirmed
```

After EDIT 4:

```
[Memory] Stored order 1 for bob@example.com
[SMTP] To: bob@example.com | Order confirmed
```

<v-click>

<div class="mt-5 border-2 border-green-500/60 rounded-lg px-6 py-3 bg-green-500/10 text-center">

```
ALL 8 CHECKS PASSED
```

</div>

</v-click>

<v-click>

<div class="mt-4 text-center text-sm">

There is no *"ask the instructor if it's right"* step. **The test tells you.**

</div>

</v-click>

---
layout: default
---

# If you get stuck

<div class="text-xs mt-3">

| The compiler says | What it means | Fix |
|---|---|---|
| `constructor OrderService ... cannot be applied to given types` | EDIT 2 not done, or parameters in the wrong order | Add the constructor as shown: `(repository, emailSender)` |
| `variable repository might not have been initialized` | You deleted the `new` but haven't assigned the field yet | Finish EDIT 2 |
| `incompatible types: MySqlOrderRepository cannot be converted to OrderRepository` | Usually the two constructor arguments swapped | Repository first, email sender second |
| `cannot find symbol: method connect()` | You typed the field as the interface, then called a MySQL-only method | That call shouldn't be in `OrderService` at all — that's the point |

</div>

<v-click>

<div class="mt-5 border-l-4 border-green-500 pl-4 text-sm">

If `Verify` runs but checks fail, read the failure text — each names its edit.
Fix them in order; the EDIT 3 and 4 checks can't pass until 1 and 2 do.

</div>

</v-click>

---
layout: default
---

# EDIT 4 is the whole lesson — prove it

<div class="text-sm mt-3">

Right-click `OrderService.java` → **Local History → Show History**

</div>

<div class="grid grid-cols-2 gap-4 mt-5 text-sm">

<div class="border-2 border-green-500/60 rounded-lg px-5 py-3 bg-green-500/10">
<div class="text-green-300 font-bold">OrderService.java</div>
<div class="mt-2"><strong>Two</strong> entries — EDIT 1 and EDIT 2. Both from before the program ever ran.</div>
<div class="mt-2 opacity-80">EDIT 4 produced <strong>none</strong>.</div>
</div>

<div class="border-2 border-blue-500/60 rounded-lg px-5 py-3 bg-blue-500/10">
<div class="text-blue-300 font-bold">Main.java</div>
<div class="mt-2">Every edit of the afternoon.</div>
</div>

</div>

<v-click>

<div class="mt-5">

You changed how the application **stores orders** — and never opened the class
that places them.

</div>

</v-click>

<v-click>

<div class="mt-6 border-l-4 border-green-500 pl-4">

Then read `RecordingRepository` in `Verify.java`. Those fakes **could not exist**
against the original `OrderService`.

*Code that is easy to test is code whose dependencies come from outside.*
The testing session gives that a name and a library.

</div>

</v-click>

---
layout: default
---

# Finished early? Three extensions

<div class="text-sm mt-2 opacity-80">Optional. Not checked by <code>Verify</code> — the 8 checks stay at 8. Still don't touch <code>OrderService.java</code>.</div>

<div class="mt-4 space-y-3 text-sm">

<div class="border-2 border-blue-500/60 rounded-lg px-5 py-3 bg-blue-500/10">
<div class="text-blue-300 font-bold">A · A third implementation <span class="opacity-60 font-normal">~5 min</span></div>
<div class="mt-1">Write <code>ConsoleEmailSender</code>. Wire a third service with it. Count how many <em>existing</em> lines you had to change. (Zero.)</div>
</div>

<div class="border-2 border-purple-500/60 rounded-lg px-5 py-3 bg-purple-500/10">
<div class="text-purple-300 font-bold">B · A decorator <span class="opacity-60 font-normal">~10 min</span></div>
<div class="mt-1">An <code>OrderRepository</code> that <em>contains</em> an <code>OrderRepository</code> and logs around it.</div>
</div>

<div class="border-2 border-yellow-500/60 rounded-lg px-5 py-3 bg-yellow-500/10">
<div class="text-yellow-300 font-bold">C · Break it deliberately <span class="opacity-60 font-normal">~5 min</span></div>
<div class="mt-1">Swap the constructor arguments. What stops you, and <em>when</em>? What would have stopped you with setters?</div>
</div>

</div>

---
layout: default
---

# Extension B — read this line twice

```java
OrderRepository repository =
    new LoggingOrderRepository(new MySqlOrderRepository());
```

<v-click>

<div class="mt-5">

An `OrderRepository` that **contains** an `OrderRepository`.

`OrderService` cannot tell the difference, and does not recompile.

</div>

</v-click>

<v-click>

<div class="mt-6 border-2 border-green-500/60 rounded-lg px-6 py-4 bg-green-500/10">

You just added behaviour to **every save in the system** without editing
a single existing class.

<div class="mt-2 text-sm">That is the same trick Spring AOP performs automatically later in the course —
and you now know it isn't magic.</div>

</div>

</v-click>

---
layout: default
---

# Recap

<div class="text-xs">

- Enterprise Java runs on **abstraction + polymorphism**; inheritance is mostly framework hooks
- **`private` is blast radius**, not secrecy. `final` extends the guarantee across the object's life
- A good abstraction **hides the mechanism**; if a fake would have to lie, it's leaking
- Prefer **composition over inheritance** for reuse. `extends` is for fitting a framework's slot
- **Declared type** decides what you may call; the **object** decides what runs — for instance methods *only*. Fields, statics and privates are compile-time
- **Program to an interface** = name the capability apart from the mechanism
- **Constructor injection**: collaborators handed in, fields `final`, no `new`. If it compiles, everything is present
- The **assembler** knows the concrete types — today `main()`, later the Spring container
- **Jakarta EE** is specs; Hibernate/Tomcat/Jackson are implementations. `javax.*` → `jakarta.*` in Jakarta EE 9
- A **servlet container** owns HTTP and calls **one shared instance from many threads** — so request state lives in locals, never fields
- Spring isn't anti-Jakarta; it's anti **application server**
- Habit: **count who can force this class to change.** More than one is a smell

</div>

<v-click>

**Next:** `Order` becomes a **record**, and the outcome is modelled as a **sealed interface**.

</v-click>
