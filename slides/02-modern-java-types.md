---
layout: section
---

# Modern Java Types

`var` · records · sealed types · text blocks

---
layout: default
---

# Learning Objectives

<v-clicks>

- Use **`var`** where it helps — and name the one place it silently costs you type safety
- Replace a data-carrier class with a **record**, and say what the compiler wrote instead
- Validate *and* normalise on the way in with a **compact constructor**
- Say where a record's immutability **stops**, and close that hole in one line
- Close a set of outcomes with a **sealed interface**, and say what the compiler can now prove
- Write SQL and JSON with **text blocks** — and predict where the spaces go

</v-clicks>

---
layout: section
---

# `var`

The type is still there. You just stopped typing it twice.

---
layout: default
---

# It is not what you think it is

```java
var orders = new ArrayList<Order>();          // an ArrayList<Order>
var total  = 0.0;                             // a double
var name   = "Alice";                         // a String
```

<v-click>

<div class="mt-5 border-2 border-red-500/50 rounded-lg px-6 py-4 bg-red-500/10">

This is **not** JavaScript's `var`, and it is **not** dynamic typing.

<div class="mt-2 text-sm">
The compiler works the type out, writes it into the class file, and fixes it forever.
<code>name = 42;</code> does not compile.
</div>

</div>

</v-click>

<v-click>

<div class="mt-4 text-center text-green-300">

Nothing about your program got looser at runtime.
Only how much you had to type.

</div>

</v-click>

---
layout: two-cols
---

# Where it helps

The type appears **twice** on one line:

```java
Map<String, List<OrderLine>> lines =
    new HashMap<String, List<OrderLine>>();
```

```java
var lines =
    new HashMap<String, List<OrderLine>>();
```

Nothing is lost — it's three words to the right.

::right::

<div class="mt-14"></div>

Any right-hand side that **names its own type**:

```java
var reader = new BufferedReader(...);
var scale  = new BigDecimal("19.99");
var i      = 0;
```

<v-click>

<div class="mt-6 border-l-4 border-green-500 pl-4 text-sm">

All obvious. Nobody has to open another file.

</div>

</v-click>

---
layout: default
---

# Where it hurts

```java
var result = service.process(order);
```

<v-click>

<div class="mt-4">

What is `result`? You have to go and read `process()`.

</div>

</v-click>

<v-click>

<div class="mt-4 text-red-300">

Fourteen keystrokes saved **once**, charged to every future reader.

</div>

</v-click>

<v-click>

<div class="mt-6">

The awkward middle case:

```java
var orders = repository.findAllByStatus(ACTIVE);
```

<div class="text-sm mt-2">

`List<Order>`? `Set`? A `Stream` you must close? A `Page<Order>` with pagination on it?

All four are plausible — and which one it is **changes what you can do on the next line.**

</div>

</div>

</v-click>

---
layout: statement
---

# The rule

Use `var` when the right-hand side
**already states the type**.

Refuse it when the type comes from a method call
whose return type you can't guess from its name.

---
layout: default
---

# Four hard limits

```java
public class Bad {
    var count = 0;                      // no - not on a field
    public var compute(var x) { ... }   // no - not on a parameter or return type

    void method() {
        var nothing;                    // no - nothing to infer from
        var empty = null;               // no - null has no type of its own
    }
}
```

<v-click>

<div class="mt-5 border-2 border-blue-500/60 rounded-lg px-6 py-4 bg-blue-500/10 text-center">

Four different error messages. **One reason.**

<div class="mt-2">`var` is the compiler copying a type it can already see.
Where there's no type to see, there's nothing to copy.</div>

</div>

</v-click>

---
layout: default
---

# The gotcha: `var` + the diamond

```java {all|1|2|4-6}
List<String> a = new ArrayList<>();   // an ArrayList<String>
var          b = new ArrayList<>();   // an ArrayList<Object>  <-- !

b.add("hello");
b.add(42);       // compiles. of course it does.
b.add(true);     // so does this
```

<v-click>

<div class="mt-4">

The diamond `<>` takes its type from the **left-hand side**. `var` removed the left-hand side.

</div>

</v-click>

<v-click>

<div class="mt-4 text-red-300">

No warning. You find out when something downstream casts.

</div>

</v-click>

<v-click>

<div class="mt-4 border-2 border-green-500/60 rounded-lg px-6 py-3 bg-green-500/10 text-center">

Spell the type argument out where `var` can see it:
`var b = new ArrayList<String>();`

</div>

</v-click>

---
layout: default
---

# Demo — `VarLab.java`

<div class="text-sm mt-2">

`java VarLab.java`

</div>

<div class="grid grid-cols-2 gap-4 mt-6 text-sm">

<div class="border-2 border-blue-500/60 rounded-lg px-5 py-3 bg-blue-500/10">
<div class="text-blue-300 font-bold">The arc</div>
<div class="mt-2">Three variables with the type written out, then the same three with <code>var</code>.</div>
</div>

<div class="border-2 border-green-500/60 rounded-lg px-5 py-3 bg-green-500/10">
<div class="text-green-300 font-bold">Watch for</div>
<div class="mt-2">The printed types are <strong>identical</strong>. Then one gotcha: <code>var</code> with a bare <code>&lt;&gt;</code>.</div>
</div>

</div>

---
layout: default
---

# Pause — would you write these with `var`?

<div class="mt-6 text-sm">

```java
1.  var reader  = new BufferedReader(new FileReader("data.csv"));
2.  var config  = loadConfiguration();
3.  var i       = 0;
4.  var entries = repository.findAllByStatus(ACTIVE);
5.  var total   = order.total();
```

</div>

<div class="mt-8 border-l-4 border-yellow-500 pl-6">

A reason for each — not a feeling.

**Two of them are genuinely arguable.** Find out which two, and say what
the argument turns on.

</div>

---
layout: default
---

# Last session's `Order`

```java
public class Order {
    private final String customerEmail;
    private final double total;

    public Order(String customerEmail, double total) {
        this.customerEmail = customerEmail;
        this.total = total;
    }

    public String customerEmail() { return customerEmail; }
    public double total()         { return total; }
}
```

<v-click>

Twelve lines to say **"an order is an email and a total."**

</v-click>

---
layout: default
---

# And it isn't finished

<div class="text-sm mt-4">

| Missing | What breaks |
|---|---|
| `equals` | Two orders with identical data are **not equal** |
| `hashCode` | Put it in a `HashSet`, never find it again |
| `toString` | It appears in your logs as `Order@342c38f8` |

</div>

<v-click>

<div class="mt-6">

Write those three properly → about **forty lines**, of which **thirty-eight**
say nothing a human needs to read.

</div>

</v-click>

---
layout: statement
---

# The problem isn't the length

Long is survivable.

The problem is that **boilerplate goes out of date.**

---
layout: default
---

# How it actually goes wrong

<div class="mt-4">

Someone adds a third field to `Order`. They forget to add it to `equals`.

</div>

<v-click>

<div class="mt-5 border-2 border-red-500/50 rounded-lg px-6 py-4 bg-red-500/10">

Two different orders now compare as **equal**.

A `HashSet` silently drops one. A cache returns the wrong object.
A test passes that should have failed.

<div class="mt-2 text-sm">Nothing in the language notices.</div>

</div>

</v-click>

---
layout: default
---

# And there are dozens of them

<div class="grid grid-cols-2 gap-4 mt-6 text-sm">

<div class="border-2 border-blue-500/60 rounded-lg px-5 py-3 bg-blue-500/10">
<div class="text-blue-300 font-bold">Every one of these is a data class</div>
<div class="mt-2">every REST request body · every response · every row read from a database · every message on a queue · every value object between them</div>
</div>

<div class="border-2 border-purple-500/60 rounded-lg px-5 py-3 bg-purple-500/10">
<div class="text-purple-300 font-bold">Forty lines each</div>
<div class="mt-2">A medium-sized Spring service has <strong>dozens</strong> of them — each one generated by a tool and maintained by a human.</div>
<div class="mt-2 opacity-80">Say the same thing in less code — and get <em>more</em> compiler help while doing it.</div>
</div>

</div>

---
layout: section
---

# Records

If a tool can generate it, the language should own it.

---
layout: default
---

# What a data class actually needs

<div class="grid grid-cols-2 gap-6 text-sm mt-4">

<div>

1. `private final` fields — **meaningful**
2. a constructor assigning them — *mechanical*
3. an accessor per field — *mechanical*
4. `equals` over all fields — *mechanical*
5. `hashCode` matching it — *mechanical*
6. `toString` — *mechanical*

</div>

<div>

<v-click>

Your IDE generates **2–6**.

<div class="mt-4 p-3 rounded bg-amber-500/15">

And a generated method isn't free — it's a **copy of a fact**.

Copies drift.

</div>

</v-click>

</div>

</div>

---
layout: statement
---

# That's the tell

If a tool can write it,
the **language** should own it.

---
layout: default
---

# The whole class

<div class="mt-10 text-center">

```java
public record Order(String customerEmail, double total) { }
```

</div>

<v-click>

<div class="mt-10 text-center text-xl text-green-300">

One line.

</div>

</v-click>

<v-click>

<div class="mt-4 text-center">

And **more complete** than the twelve-line version —
because it also has `equals`, `hashCode` and `toString`.

</div>

</v-click>

---
layout: default
---

# What the compiler generated

<div class="text-sm">

| You get | Behaviour |
|---|---|
| `private final` fields | Always `private final` |
| `Order(String, double)` | The **canonical constructor** |
| `customerEmail()`, `total()` | Accessors — **no `get` prefix** |
| `equals(Object)` | `true` when *every* component is equal |
| `hashCode()` | Consistent with `equals` |
| `toString()` | `Order[customerEmail=alice@example.com, total=250.0]` |

</div>

<v-click>

<div class="mt-5 text-center text-green-300">

None of it can go out of date, because none of it is a copy.

</div>

</v-click>

---
layout: two-cols
---

# Two details that catch people

**No `get`.**

`order.total()`, not `order.getTotal()`.

A record component isn't a JavaBean
property — the accessor is named
after the thing itself.

<div class="text-sm mt-3 opacity-80">

Some old reflection-based libraries look
for `getXxx` and won't find it. Jackson
understands records natively, so you
won't hit this here.

</div>

::right::

<div class="mt-14"></div>

**It's `final`.**

Not "you shouldn't extend it" — you
**cannot**.

Nor can a record extend a class: it
already extends `java.lang.Record`,
and Java has single inheritance.

---
layout: default
---

# Value semantics — the actual reason

```java
var a = new Order("alice@example.com", 250.00);
var b = new Order("alice@example.com", 250.00);
```

<div class="mt-4 text-center text-lg font-mono">

<v-click>

<div>a == b &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;→&nbsp; <span class="text-blue-300">false</span> &nbsp;&nbsp;<span class="text-sm opacity-70">two separate objects. correct.</span></div>

</v-click>

<v-click>

<div class="mt-2">a.equals(b) &nbsp;→&nbsp; <span class="text-green-300">true</span> &nbsp;&nbsp;<span class="text-sm opacity-70">same data, so the SAME ORDER</span></div>

</v-click>

</div>

<v-click>

<div class="mt-8">

```java
var seen = new HashSet<Order>();
seen.add(a);
seen.contains(b);      // true with a record.  false with the hand-written class.
```

</div>

</v-click>

---
layout: statement
---

# This is not a detail

Every `HashSet`. Every `HashMap` key.
Every `list.contains(...)`. Every `assertEquals`.

All of them go through `equals` and `hashCode`.

---
layout: default
---

# And the bugs don't look like `equals` bugs

<div class="mt-6 space-y-3 text-sm">

<div class="border-2 border-red-500/50 rounded-lg px-5 py-3 bg-red-500/10">
A set that accepts <strong>duplicates</strong>
</div>

<div class="border-2 border-red-500/50 rounded-lg px-5 py-3 bg-red-500/10">
A cache that <strong>never hits</strong>
</div>

<div class="border-2 border-red-500/50 rounded-lg px-5 py-3 bg-red-500/10">
A test that fails while printing <strong>two identical-looking objects</strong>
</div>

</div>

<v-click>

<div class="mt-6 text-center text-green-300">

Last session's class gets all of this wrong — not through a mistake,
but because **nobody wrote the two methods.**

A record cannot get it wrong.

</div>

</v-click>

---
layout: default
---

# The trade: immutable and final

Records **cannot**:

- change a component after construction — no setters, and no way to add one
- extend a class, or be extended

<v-click>

<div class="mt-6">

For data crossing a boundary — a request body, a response, a query result,
a message — **that is exactly what you want.**

</div>

</v-click>

<v-click>

<div class="mt-4 text-green-300">

An object that cannot change also cannot be corrupted by two threads at once.
Which is why you see records everywhere in code that runs inside a server.

</div>

</v-click>

---
layout: default
---

# Pause

<div class="border-l-4 border-yellow-500 pl-6 mt-10 text-xl">

`Order` is now immutable.

A colleague asks: *"what if I need to apply a discount to an order?"*

<div class="mt-6">

What do you tell them?

</div>

<div class="mt-4 text-base opacity-70">

Hint: you do not modify it.

</div>

</div>

---
layout: default
---

# You make a new one

```java
public record Order(String customerEmail, double total) {

    public Order withDiscount(double percent) {
        return new Order(customerEmail, total * (1 - percent / 100));
    }
}
```

<v-click>

<div class="mt-5">

Records **can** have methods. They just can't have mutable state.

</div>

</v-click>

<v-click>

<div class="mt-5 border-2 border-purple-500/60 rounded-lg px-6 py-4 bg-purple-500/10">

A method like this is a **wither** — by convention `withXxx`.

<div class="mt-2">It's how you "change" an immutable object: you don't.
You produce a new one and leave the original alone.</div>

</div>

</v-click>

---
layout: default
---

# Compact constructors

```java {all|4|5-7|8-10}
public record Order(String customerEmail, double total) {

    // no parameter list, no field assignment
    public Order {
        if (customerEmail == null || customerEmail.isBlank()) {
            throw new IllegalArgumentException("customerEmail is required");
        }
        if (total < 0) {
            throw new IllegalArgumentException("total cannot be negative");
        }
    }
}
```

---
layout: default
---

# Read the shape — it's unlike anything else in Java

<div class="mt-6 space-y-4">

<div class="border-2 border-blue-500/60 rounded-lg px-6 py-4 bg-blue-500/10">
<div class="text-blue-300 font-bold">No parameter list</div>
<div class="mt-2 text-sm"><code>public Order {</code> — not <code>public Order(String customerEmail, double total) {</code>.
The parameters are implied. They're the components.</div>
</div>

<div class="border-2 border-blue-500/60 rounded-lg px-6 py-4 bg-blue-500/10">
<div class="text-blue-300 font-bold">No <code>this.customerEmail = customerEmail;</code></div>
<div class="mt-2 text-sm">The compiler adds every assignment <strong>after</strong> your code runs.</div>
</div>

</div>

<v-click>

<div class="mt-5 text-center text-green-300">

That second point is what makes the next slide possible.

</div>

</v-click>

---
layout: default
---

# Normalising: reassign the parameter

```java {all|5}
public Order {
    if (customerEmail == null || customerEmail.isBlank()) {
        throw new IllegalArgumentException("customerEmail is required");
    }
    customerEmail = customerEmail.trim().toLowerCase();
}
```

<v-click>

<div class="mt-5">

Reassigning the **parameter** changes what gets stored, because the assignment
hasn't happened yet.

</div>

</v-click>

<v-click>

<div class="mt-5 border-2 border-amber-500/60 rounded-lg px-6 py-3 bg-amber-500/15">

This is the **one place in Java** where reassigning a parameter is a feature
rather than a code smell.

</div>

</v-click>

---
layout: statement
---

# An invalid `Order` can now never exist

Not "is checked in the service layer."

Not "is usually validated."

**Cannot exist.**

---
layout: default
---

# What that's worth

<div class="mt-6">

There is no sequence of legal Java that produces an `Order` with a negative total.

</div>

<v-click>

<div class="mt-5">

So every method downstream is freed from re-checking.

</div>

</v-click>

<v-click>

<div class="mt-5 border-2 border-green-500/60 rounded-lg px-6 py-4 bg-green-500/10">

Every `if (order.total() < 0)` guard you would have written scattered through
the codebase is now **dead code you never wrote.**

</div>

</v-click>

<v-click>

<div class="mt-5 text-center text-sm opacity-80">

One check, at the boundary, once.

</div>

</v-click>

---
layout: default
---

# Pause — where can a bad object live?

<div class="border-l-4 border-yellow-500 pl-6 mt-8">

Two designs:

<div class="mt-4">

**(a)** an `Order` **record** with a compact constructor rejecting negative totals

**(b)** a mutable `Order` **class** with a `validate()` method you call afterwards

</div>

<div class="mt-6 text-lg">

In which design can an invalid `Order` exist?

**For how long?**

And who has to remember to call `validate()`?

</div>

</div>

---
layout: section
---

# The trap

Records are only **shallowly** immutable.

---
layout: default
---

# This compiles, runs, and ruins your afternoon

```java {all|1|3-6|8-9}
public record Basket(String owner, List<String> items) { }

var items = new ArrayList<String>();
items.add("widget");

var basket = new Basket("alice", items);

items.add("SNEAKED IN");             // the caller still holds the list
basket.items().add("AND ANOTHER");   // so does anyone we hand it to
```

<v-click>

<div class="mt-4 text-center font-mono text-red-300">

Basket[owner=alice, items=[widget, SNEAKED IN, AND ANOTHER]]

</div>

</v-click>

---
layout: default
---

# What actually happened

<div class="grid grid-cols-2 gap-4 mt-8 text-sm">

<div class="border-2 border-green-500/60 rounded-lg px-5 py-4 bg-green-500/10">
<div class="text-green-300 font-bold">The field IS final</div>
<div class="mt-2">The reference can't be re-pointed at a different list.</div>
</div>

<div class="border-2 border-red-500/50 rounded-lg px-5 py-4 bg-red-500/10">
<div class="text-red-300 font-bold">The list is not</div>
<div class="mt-2"><code>final</code> says <strong>nothing</strong> about the object it points at.</div>
</div>

</div>

<v-click>

<div class="mt-8 text-center text-xl text-green-300">

A record is only as immutable as its **components**.

</div>

</v-click>

<v-click>

<div class="mt-5 text-sm text-center">

`String`, `int`, `BigDecimal`, `LocalDate`, another record → fine, they're immutable themselves.

`List`, `Map`, an array, any mutable class → **a hole.**

</div>

</v-click>

---
layout: default
---

# The fix is one line

```java {all|3}
public record Basket(String owner, List<String> items) {
    public Basket {
        items = List.copyOf(items);
    }
}
```

<v-click>

<div class="mt-5 text-sm">

`List.copyOf` does **two** jobs at once:

</div>

<div class="mt-3 grid grid-cols-2 gap-4 text-sm">

<div class="border-2 border-blue-500/60 rounded-lg px-5 py-3 bg-blue-500/10">
It <strong>copies</strong> — the caller's later edits can't reach in
</div>

<div class="border-2 border-blue-500/60 rounded-lg px-5 py-3 bg-blue-500/10">
The copy is <strong>unmodifiable</strong> — nobody who gets it back from <code>items()</code> can edit it
</div>

</div>

</v-click>

<v-click>

<div class="mt-4 text-sm opacity-80 text-center">

It also rejects a null list — so decide deliberately whether you want
`items == null ? List.of() : List.copyOf(items)`.

</div>

</v-click>

---
layout: default
---

# When a record is the wrong answer

<div class="text-sm mt-4">

| Situation | Why not |
|---|---|
| The object must change after construction | No setters, ever. Not negotiable. |
| It has **behaviour**, not data — a service, a repository, a gateway | Not a data carrier. `FakePaymentGateway` today is a class, correctly. |
| It needs to extend a class | A record already extends `Record`. |
| It's a **JPA entity** | Next slide. |

</div>

<v-click>

<div class="mt-6 text-center">

Records aren't a replacement for classes.
They're a replacement for **one kind** of class.

</div>

</v-click>

---
layout: statement
---

# Records for DTOs.
# Classes for JPA entities.

An entity must be mutable — Hibernate sets fields after loading it —
and needs a no-arg constructor, which Hibernate calls before it has any data.

A record has neither, and can't be given either.

---
layout: default
---

# Demo — `RecordAnatomy.java`

<div class="text-sm mt-2">

`java RecordAnatomy.java`

</div>

<div class="grid grid-cols-2 gap-4 mt-5 text-sm">

<div class="border-2 border-blue-500/60 rounded-lg px-5 py-3 bg-blue-500/10">
<div class="text-blue-300 font-bold">The arc</div>
<div class="mt-2">Three lines from the hand-written class: <code>OldOrder@76ed1b7c</code>, <code>equals</code> false, a <code>HashSet</code> that loses it.</div>
</div>

<div class="border-2 border-green-500/60 rounded-lg px-5 py-3 bg-green-500/10">
<div class="text-green-300 font-bold">Watch for</div>
<div class="mt-2">The <strong>same three lines</strong> from the record, all correct. Then the compact constructor, and the trap.</div>
</div>

</div>

<v-click>

<div class="mt-5 border-l-4 border-yellow-500 pl-4 text-sm">

**Nobody made a mistake in that first class.** Every line of the bad output came from code that compiles and reads fine.

</div>

</v-click>

---
layout: default
---

# Pause — audit your own code

<div class="border-l-4 border-yellow-500 pl-6 mt-8">

Find a data-carrier class you've written — only fields, a constructor, getters.

<div class="mt-6">

1. How many lines is it? How many would survive as a record?

2. And the more interesting one:
   **does it have a setter that is actually called anywhere?**

</div>

<div class="mt-6 text-green-300">

If yes — it isn't a data carrier.
Write down what it really is.

</div>

</div>

---
layout: section
---

# Sealed types

Telling the compiler that a list is **complete**.

---
layout: default
---

# The problem

<div class="mt-6 text-lg">

Charging an order ends **three** ways:

</div>

<div class="grid grid-cols-3 gap-4 mt-6 text-center">

<div class="border-2 border-green-500/60 rounded-lg px-4 py-5 bg-green-500/10">
<div class="font-bold text-green-300">Approved</div>
<div class="mt-2 text-sm opacity-80">has a transaction id</div>
</div>

<div class="border-2 border-red-500/50 rounded-lg px-4 py-5 bg-red-500/10">
<div class="font-bold text-red-300">Declined</div>
<div class="mt-2 text-sm opacity-80">has a reason</div>
</div>

<div class="border-2 border-yellow-500/60 rounded-lg px-4 py-5 bg-yellow-500/10">
<div class="font-bold text-yellow-300">Out of stock</div>
<div class="mt-2 text-sm opacity-80">has a SKU and a quantity</div>
</div>

</div>

<v-click>

<div class="mt-8 text-center text-xl">

How do you **return** that?

</div>

</v-click>

---
layout: default
---

# Three things most codebases do

```java
// (a) a boolean - where does the decline reason go?
boolean placeOrder(Order order);

// (b) exceptions as control flow - expensive, and the caller can simply not catch
void placeOrder(Order order) throws PaymentDeclinedException, OutOfStockException;

// (c) a status blob - nothing stops you building an invalid combination
record Result(boolean ok, String errorCode, String message, Integer available) {}
```

<v-click>

<div class="mt-5 text-center">

(c) is the interesting failure — because it **looks fine.**

</div>

</v-click>

---
layout: default
---

# It compiles. It's a record. It holds everything you need.

```java
record Result(boolean ok, String errorCode, String message, Integer available) {}
```

<v-click>

<div class="mt-6 text-sm">

Here's what it **also** allows:

```java
new Result(true,  "DECLINED", "Amount exceeds limit", null);   // succeeded AND declined
new Result(false, null,       null,                   null);   // failed, won't say why
new Result(true,  null,       null,                   0);      // succeeded, reports stock
```

</div>

</v-click>

<v-click>

<div class="mt-5 border-2 border-red-500/50 rounded-lg px-6 py-4 bg-red-500/10">

Three states that mean nothing — all built with ordinary, legal Java.

</div>

</v-click>

---
layout: default
---

# So somewhere there's a comment

<div class="border-l-4 border-yellow-500 pl-6 mt-8 text-xl italic">

"If `ok` is true, `errorCode` will be null."

</div>

<v-click>

<div class="mt-8 text-center text-lg">

That's **documentation**, not a rule.

</div>

</v-click>

<v-click>

<div class="mt-4 text-center text-red-300">

Nothing enforces it. Someone will break it on a Thursday.

</div>

</v-click>

---
layout: statement
---

# What you actually want

There are **exactly three** outcomes,

each carries **its own** data,

and the **compiler** knows the list is complete.

---
layout: default
---

# Sealed interfaces

```java {all|1-2|4-6}
public sealed interface PaymentResult
        permits Approved, Declined, OutOfStock { }

public record Approved(String transactionId)        implements PaymentResult { }
public record Declined(String reason)               implements PaymentResult { }
public record OutOfStock(String sku, int available) implements PaymentResult { }
```

<v-click>

<div class="mt-6 border-2 border-purple-500/60 rounded-lg px-6 py-4 bg-purple-500/10 text-center text-lg">

Read it as an English sentence:

<div class="mt-2 text-purple-200">

*a `PaymentResult` **is** an `Approved`, a `Declined`, or an `OutOfStock`
— and nothing else, ever.*

</div>

</div>

</v-click>

---
layout: default
---

# 1 · Each outcome carries exactly its own data

```java
public record Approved(String transactionId)        implements PaymentResult { }
public record Declined(String reason)               implements PaymentResult { }
public record OutOfStock(String sku, int available) implements PaymentResult { }
```

<v-click>

<div class="mt-5">

No field that is meaningless half the time.
No combination that needs a comment to explain when it applies.

</div>

</v-click>

<v-click>

<div class="mt-5 border-2 border-green-500/60 rounded-lg px-6 py-4 bg-green-500/10 text-center">

Look at the three declarations again.

**Not one of those nonsense states can be written.**

</div>

</v-click>

---
layout: default
---

# 2 · Nobody can add a fourth

```java
record Refunded(String transactionId) implements PaymentResult { }
```

<v-click>

<div class="mt-4 font-mono text-sm text-red-300 border-2 border-red-500/50 rounded-lg px-5 py-3 bg-red-500/10">

error: class is not allowed to extend sealed class: PaymentResult<br>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;(as it is not listed in its 'permits' clause)

</div>

</v-click>

<v-click>

<div class="mt-5 text-sm">

`Refunded` is a perfectly good record written by a perfectly reasonable developer.
The compiler refuses it because **the author of the interface did not list it.**

</div>

</v-click>

<v-click>

<div class="mt-4 text-green-300">

Compare: someone adds a fifth value to a `String errorCode` field.
Nothing breaks. It compiles, it deploys, and a customer tells you.

</div>

</v-click>

---
layout: default
---

# 3 · The compiler knows the list is complete

```java
// NEXT SESSION - shown so you know what today is buying
String describe(PaymentResult result) {
    return switch (result) {
        case Approved a   -> "Approved: " + a.transactionId();
        case Declined d   -> "Declined: " + d.reason();
        case OutOfStock o -> "Only " + o.available() + " left of " + o.sku();
        // no `default` - the compiler knows there are exactly three
    };
}
```

<v-click>

<div class="mt-4 border-2 border-green-500/60 rounded-lg px-6 py-4 bg-green-500/10">

Add a fourth outcome to `permits` → **every incomplete `switch` stops compiling**,
and the compiler names each one.

<div class="mt-2 text-sm">Compare with adding a value to a status `String`: nothing breaks, and you find out in production.</div>

</div>

</v-click>

---
layout: default
---

# Three rules for a permitted subtype

<div class="mt-5 space-y-3">

<div class="border-2 border-blue-500/60 rounded-lg px-6 py-3 bg-blue-500/10">
<div class="text-blue-300 font-bold">1 · It must actually implement or extend the sealed type</div>
<div class="mt-1 text-sm">Listing it in <code>permits</code> is not enough.</div>
</div>

<div class="border-2 border-blue-500/60 rounded-lg px-6 py-3 bg-blue-500/10">
<div class="text-blue-300 font-bold">2 · It must declare itself <code>final</code>, <code>sealed</code>, or <code>non-sealed</code></div>
<div class="mt-1 text-sm">Exactly one of the three. You cannot leave it open by accident.</div>
</div>

<div class="border-2 border-blue-500/60 rounded-lg px-6 py-3 bg-blue-500/10">
<div class="text-blue-300 font-bold">3 · Same module — or, without modules, the same package</div>
<div class="mt-1 text-sm">So the sealed type and its subtypes are written and reviewed together. That's the point.</div>
</div>

</div>

<v-click>

<div class="mt-5 text-center text-green-300">

A **record is implicitly final** — so rule 2 costs nothing.
That's why "sealed interface over records" is the idiom.

</div>

</v-click>

---
layout: default
---

# `non-sealed` — the escape hatch

```java
public sealed interface PaymentResult permits Approved, Declined, PartialResult { }

public non-sealed class PartialResult implements PaymentResult { }   // anyone may extend
```

<v-click>

<div class="mt-4">

Closed at the top, open underneath. It exists for real cases — usually a framework
offering a small closed set **plus one extension point**.

</div>

</v-click>

<v-click>

<div class="mt-5 border-l-4 border-yellow-500 pl-4">

Ask before you use it: **what did the seal buy me if one branch is still open?**

<div class="mt-2 text-sm">Exhaustiveness still works — any subclass of <code>PartialResult</code> <em>is</em> a <code>PartialResult</code>.
But "each outcome carries exactly its own data" is gone for that branch.</div>

<div class="mt-2 text-sm text-green-300">Sometimes worth it. Usually a smell.</div>

</div>

</v-click>

---
layout: default
---

# Sealed *classes* work the same way

```java
public sealed class Shape permits Circle, Square { }

public final class Circle extends Shape { }
public final class Square extends Shape { }
```

<v-click>

<div class="mt-6">

Same rules, same benefits.

</div>

</v-click>

<v-click>

<div class="mt-4 border-2 border-purple-500/60 rounded-lg px-6 py-4 bg-purple-500/10">

In practice you'll reach for a sealed **interface** far more often —
because the permitted types are usually records, and a record cannot extend a class.

</div>

</v-click>

---
layout: default
---

# Sealed types vs. enums

<div class="text-sm mt-4">

| | `enum` | sealed interface + records |
|---|---|---|
| The set is closed | ✅ | ✅ |
| Each case carries **its own** data | ❌ every constant has the same fields | ✅ different components per case |
| One instance per case | ✅ a singleton per constant | ❌ as many as you construct |
| Exhaustive `switch` | ✅ | ✅ when sealed |

</div>

<v-click>

<div class="mt-5">

**The dividing line is data.**

`Status { PENDING, SHIPPED, DELIVERED }` → an enum. The cases carry nothing but identity.

`Approved(transactionId)` / `Declined(reason)` → a sealed interface. Each case carries something different.

</div>

</v-click>

<v-click>

<div class="mt-4 text-green-300 text-center">

Writing an enum with three fields, two of them null for most constants?
You wanted a sealed interface.

</div>

</v-click>

---
layout: two-cols
---

# Seal it

- A fixed set of outcomes, states, events
- You want an exhaustive `switch`
- The set is a **domain fact** — "a payment is approved, declined, or out of stock"
- You own every implementation and intend to keep it that way

::right::

# Leave it open

- A plugin or strategy interface
- `OrderRepository` from last session — new implementations are the **whole point**
- Anything a third party should implement
- Anything where you want to *not* know the implementations

---
layout: statement
---

# The question underneath

A sealed type says:
*"this list is a fact about the domain,
and I will change it deliberately."*

An open interface says:
*"surprise me."*

Both correct. Different jobs.

---
layout: default
---

# Pause

<div class="border-l-4 border-yellow-500 pl-6 mt-10 text-xl">

Should `OrderRepository` from last session be **sealed**?

<div class="mt-6">

Answer with a reason, not a feeling.

</div>

<div class="mt-6 text-base">

Then ask the same question about `PaymentGateway` in today's exercise —
and notice the answer is the same but the **reason is slightly different.**

</div>

</div>

---
layout: default
---

# Be honest: what sealing buys you *today*

```java {all|1|2|10}
if (result instanceof Approved a) {
    return "Approved: " + a.transactionId();
}
if (result instanceof Declined d) {
    return "Declined: " + d.reason();
}
if (result instanceof OutOfStock o) {
    return "Only " + o.available() + " left of " + o.sku();
}
throw new IllegalStateException("unknown outcome: " + result);   // unreachable
```

<v-click>

<div class="mt-4 text-green-300">

**No cast.** `a` is already an `Approved` — the pattern variable replaces
`instanceof` followed by a cast on the next line. Worth the change on its own.

</div>

</v-click>

---
layout: default
---

# But that last line is dead code

```java
throw new IllegalStateException("unknown outcome: " + result);   // unreachable
```

<v-click>

<div class="mt-5">

You know it can never run: the type is sealed, and all three outcomes are handled.

The compiler doesn't know it yet — and won't let a method end without returning.

</div>

</v-click>

<v-click>

<div class="mt-5 border-2 border-red-500/50 rounded-lg px-6 py-4 bg-red-500/10">

**Worse:** add a fourth outcome to `permits` and this method **still compiles.**

<div class="mt-2">Nothing warns you. It just throws at runtime for the new case.</div>

</div>

</v-click>

---
layout: statement
---

# Hold on to that annoyance

It is exactly what an exhaustive `switch` fixes next session.

And sealing **today** is what makes that possible.

---
layout: default
---

# Demo — `SealedProof.java`

<div class="text-sm mt-2">

`java SealedProof.java`

</div>

<div class="grid grid-cols-2 gap-4 mt-5 text-sm">

<div class="border-2 border-blue-500/60 rounded-lg px-5 py-3 bg-blue-500/10">
<div class="text-blue-300 font-bold">The arc</div>
<div class="mt-2">Two status-blob states that compile and mean nothing → the three sealed records that cannot express either.</div>
</div>

<div class="border-2 border-green-500/60 rounded-lg px-5 py-3 bg-green-500/10">
<div class="text-green-300 font-bold">Watch for</div>
<div class="mt-2">The <strong>compile error</strong>, typed live: a fourth outcome added without permission. This is the moment of the session.</div>
</div>

</div>

---
layout: default
---

# Pause — build the nonsense

<div class="border-l-4 border-yellow-500 pl-6 mt-8">

Back to option (c):

```java
record Result(boolean ok, String errorCode, String message, Integer available) {}
```

<div class="mt-6 text-lg">

Write down **two** field combinations that are nonsensical
but that the type system happily allows.

</div>

<div class="mt-5 text-green-300">

Now check: can **either** of them be constructed out of
`Approved`, `Declined` and `OutOfStock`?

</div>

</div>

---
layout: section
---

# Text blocks

The quotes are just quotes.

---
layout: default
---

# Multi-line strings, the Java 8 way

```java
String sql = "SELECT p.id, p.name, p.price " +
             "FROM product p " +
             "JOIN category c ON c.id = p.category_id " +
             "WHERE c.name = ? AND p.active = true " +
             "ORDER BY p.price DESC";
```

<v-clicks>

- Every line needs a **trailing space inside the quotes** — miss one and you get `FROM product pJOIN category c`, which fails at runtime with a message about syntax near `pJOIN`
- Every line needs a `+`
- Nobody reviewing it can see the SQL for the punctuation

</v-clicks>

---
layout: default
---

# The same thing as a text block

```java
String sql = """
        SELECT p.id, p.name, p.price
        FROM product p
        JOIN category c ON c.id = p.category_id
        WHERE c.name = ? AND p.active = true
        ORDER BY p.price DESC
        """;
```

<v-click>

<div class="mt-6 text-center text-lg text-green-300">

No concatenation. No trailing spaces to get wrong.

And the SQL **looks like SQL**.

</div>

</v-click>

---
layout: default
---

# JSON is where it really pays

<div class="grid grid-cols-2 gap-4 text-sm mt-4">

<div>

**Before** — every quote escaped

```java
String json =
  "{\"name\":\"Widget\"," +
  "\"price\":9.99," +
  "\"tags\":[\"new\",\"sale\"]}";
```

</div>

<div>

**After**

```java
String json = """
        {
          "name": "Widget",
          "price": 9.99,
          "tags": ["new", "sale"]
        }
        """;
```

</div>

</div>

<v-click>

<div class="mt-6 text-center">

You'll write these constantly: JPQL in the persistence course,
expected JSON in controller tests once you reach Spring MVC.

</div>

</v-click>

---
layout: statement
---

# A text block is not a new type

It produces an ordinary `String`.

Anything that takes a `String` takes one.
`equals` works against a concatenated string.
The compiler does all the work — there is no runtime cost.

---
layout: default
---

# Rule 1 — incidental indentation is stripped

<div class="mt-4">

The compiler finds the **least-indented non-blank line** and removes that much
whitespace from every line.

</div>

<v-click>

<div class="mt-5 border-2 border-amber-500/60 rounded-lg px-6 py-4 bg-amber-500/15 text-center text-lg">

**The closing `"""` counts as one of those lines.**

</div>

</v-click>

<v-click>

<div class="mt-5 text-center">

Which means: **where you put the closing delimiter decides the answer.**

</div>

</v-click>

---
layout: default
---

# Three blocks, one character apart

```java {all|1-3|5-7|9-10}
String a = """
        hello
        """;        // "hello\n"           closing """ aligned with content

String b = """
        hello
""";                // "        hello\n"   closing """ at column 0

String c = """
        hello""";   // "hello"             no line break, so no trailing \n
```

<v-click>

<div class="mt-5">

`a` and `c` differ by **exactly one character**.

</div>

</v-click>

<v-click>

<div class="mt-3 text-red-300">

`b` kept eight leading spaces you typed to make the *source* readable
and never wanted in the string.

</div>

</v-click>

---
layout: statement
---

# The habit

Keep the closing `"""`
in the **same column as your content**

— unless you deliberately want leading spaces.

---
layout: default
---

# Rule 2 — the trailing newline

<div class="mt-4">

Closing `"""` on its own line → the last content line ends with `\n`.

</div>

<v-click>

<div class="mt-4 text-sm">

Usually what you want for a receipt or a log line.
Usually **not** what you want for a query or JSON you're about to compare in a test.

</div>

</v-click>

<v-click>

<div class="mt-5">

Two ways to suppress it:

```java
// (i) closing delimiter on the content line
String c = """
        hello""";

// (ii) a backslash at the end of the last line - it swallows the line break
String d = """
        hello \
        world""";      // "hello world" - no newline anywhere
```

</div>

</v-click>

---
layout: default
---

# The escape you'll eventually need

<div class="mt-4">

Text blocks **strip trailing whitespace from every line.** Always. Silently.

</div>

<v-click>

<div class="mt-4">

Usually a mercy. Occasionally the exact thing you wanted:

```java
String e = """
        hello\s\s\s
        world""";      // "hello   \nworld" - the three spaces survive
```

</div>

</v-click>

<v-click>

<div class="mt-5 border-l-4 border-yellow-500 pl-4 text-sm">

You need `\s` rarely. But when a test compares two strings character by character
and they look **identical** in the failure message — this is often what's missing.

</div>

</v-click>

---
layout: default
---

# There is no string interpolation in Java

<div class="mt-5">

`${name}` inside a text block gives you the literal characters `$`, `{`, `n`, `a`, `m`, `e`, `}`.

</div>

<v-click>

<div class="mt-5">

Use `.formatted(...)` — `String.format` with the format string on the left:

```java
String receipt = """
        RECEIPT
        -------
        Customer: %s
        Total:    $%.2f
        Status:   %s
        """.formatted(customer, total, status);
```

</div>

</v-click>

<v-click>

<div class="mt-4 text-center text-green-300 text-sm">

Reads far better with a text block than `String.format("""...""", ...)` does.

</div>

</v-click>

---
layout: default
---

# When *not* to use one

<div class="mt-6">

A text block earns its keep from about **three lines** up. Below that it's ceremony:

</div>

```java
String name = """
        Widget""";      // four lines of machinery for one word

String name = "Widget"; // do this
```

<v-click>

<div class="mt-6 text-center">

The feature is for **multi-line** strings. That's the whole test.

</div>

</v-click>

---
layout: default
---

# Demo — `TextBlockLab.java`

<div class="text-sm mt-2">

`java TextBlockLab.java`

</div>

<div class="grid grid-cols-2 gap-4 mt-5 text-sm">

<div class="border-2 border-blue-500/60 rounded-lg px-5 py-3 bg-blue-500/10">
<div class="text-blue-300 font-bold">The arc</div>
<div class="mt-2">SQL glued with <code>+</code>, printing <code>FROM product pJOIN category c</code> → the same two strings as text blocks.</div>
</div>

<div class="border-2 border-green-500/60 rounded-lg px-5 py-3 bg-green-500/10">
<div class="text-green-300 font-bold">Watch for</div>
<div class="mt-2">The last two lines: <code>[hello\n]</code> against <code>[hello]</code>. One character, and it is the one that fails your check.</div>
</div>

</div>

<v-click>

<div class="mt-5 text-center">

That one character is the single most common way people fail today's text-block check.

</div>

</v-click>

---
layout: default
---

# Where all four show up in Spring

<div class="text-sm mt-3">

| Feature | Where |
|---|---|
| **Records** | Request and response DTOs — `record CreateProductRequest(String name, BigDecimal price)`, with validation annotations on the components. Jackson handles them natively |
| **Records** | Query projections: `select new com.example.ProductSummary(p.name, p.price) from Product p` |
| **Records** | *Not* JPA entities |
| **Sealed types** | The result of an operation that can fail more than one way — so a controller maps each case to a status code without reading a string |
| **`var`** | Test setup and local wiring, where the right-hand side is a `new` |
| **Text blocks** | JPQL and native queries · expected JSON in `MockMvc` tests · multi-line config |

</div>

---
layout: statement
---

# The same argument as last session

**A good type stops a change from spreading.**

A record stops `equals` drifting out of date.
A compact constructor stops an invalid object existing.
A sealed interface stops a fourth outcome appearing unnoticed.

Each time, the compiler does work
that would otherwise be a code review's job.

---
layout: section
---

# Exercise

## Modernise the order domain

24 checks · ~45 min

---
layout: default
---

# Exercise — seven steps

<div class="text-sm mt-3">

| | File(s) | What |
|---|---|---|
| **1** | `Order.java` | → a record. Compact constructor: validate + normalise. Add `withDiscount` |
| **2** | `PaymentResult` + 3 outcomes + `FakePaymentGateway` | Seal it. Three records. Rules **in order** |
| **3** | `OrderReporter.describe` | `instanceof` with pattern variables — no casts |
| **4** | `OrderReporter.receipt` | Text block that **ends with** a newline |
| **5** | `OrderReporter.summaryJson` | Text block that **does not** |
| **6** | `Main.java` | Given. Run it, read the output |
| **7** | — | `javac *.java` then `java Verify` → **24 checks** |

</div>

<v-click>

<div class="mt-4 border-l-4 border-yellow-500 pl-4 text-sm">

**The unedited folder does not compile — nine errors, two causes.**
No `withDiscount` yet, and the three outcomes are still empty classes.
Do Steps 1 and 2, *then* compile. Students who aren't told this assume they broke something.

</div>

</v-click>

---
layout: default
---

# Steps 4 and 5 differ by one character

<div class="grid grid-cols-2 gap-4 mt-4 text-sm">

<div>

**Step 4 — the receipt** *(ends with `\n`)*

```
RECEIPT
-------
Customer: alice@example.com
Total:    $250.00
Status:   Approved: TX-1001
```

</div>

<div>

**Step 5 — the JSON** *(does not)*

```
{
  "customer": "alice@example.com",
  "total": 250.00,
  "status": "Approved: TX-1001"
}
```

</div>

</div>

<v-clicks>

- Not one quote in the JSON is escaped. Compare with what you'd have written in Java 8
- Use `%.2f` so the total prints `250.00`, not `250.0`
- Call `describe(result)` — don't hard-code the status. `Verify` checks the template against **two** outcomes

</v-clicks>

---
layout: default
---

# What success looks like

<div class="text-sm mt-2">

```
--- the same first order, as JSON ---
{
  "customer": "alice@example.com",
  "total": 250.00,
  "status": "Approved: TX-1001"
}

Discounted order: Order[customerEmail=alice@example.com, total=225.0]
The original:     Order[customerEmail=alice@example.com, total=250.0]
Same value? true
```

</div>

<v-click>

<div class="mt-4 text-sm">

The first order is built as `new Order("Alice@Example.com ", 250.00)` — mixed case,
trailing space — and comes out `alice@example.com`. **Your compact constructor did that**,
once, at the boundary.

</div>

</v-click>

<v-click>

<div class="mt-3 border-2 border-green-500/60 rounded-lg px-6 py-3 bg-green-500/10 text-center">

`Same value? true` — two separately-built records, equal.
Last session's class printed `false`. **Confirm it.**

</div>

</v-click>

---
layout: default
---

# If you get stuck

<div class="text-xs mt-3">

| The compiler or `Verify` says | What it means |
|---|---|
| `cannot find symbol: method withDiscount(int)` | Step 1 not done. Nothing compiles until it is |
| `constructor Approved ... cannot be applied to given types` | `Approved` is still an empty class, not a record |
| `class is not allowed to extend sealed class` | A type is missing from `permits`, or a name is misspelled |
| `... might not have been initialized` in the compact constructor | You wrote `this.customerEmail = ...`. Assign the **parameter** instead |
| `missing return statement` in `describe` | Add the final `throw new IllegalStateException(...)` |
| A text-block check fails, strings look **identical** | Whitespace. `Verify` says so explicitly when that's the only difference |
| `...does NOT end in a newline` fails, everything looks right | One trailing `\n`. Move the closing `"""` up, or end that line with `\` |
| `the rules are applied in order` fails | Limit first, then `nostock`, then approve |

</div>

---
layout: default
---

# Finished early? Three extensions

<div class="text-sm mt-2 opacity-80">Optional. Not checked by <code>Verify</code> — the count stays at 24.</div>

<div class="mt-4 space-y-3 text-sm">

<div class="border-2 border-blue-500/60 rounded-lg px-5 py-3 bg-blue-500/10">
<div class="text-blue-300 font-bold">A · Gatecrash the seal <span class="opacity-60 font-normal">~3 min</span></div>
<div class="mt-1">Add <code>record Refunded(...) implements PaymentResult</code> <strong>without</strong> touching <code>permits</code>. Read the error out loud.</div>
</div>

<div class="border-2 border-purple-500/60 rounded-lg px-5 py-3 bg-purple-500/10">
<div class="text-purple-300 font-bold">B · Now add it properly <span class="opacity-60 font-normal">~7 min</span></div>
<div class="mt-1">Put it in <code>permits</code> and route orders over $10,000 to it. <code>describe()</code> <strong>still compiles</strong> — and throws at runtime. Nothing warned you.</div>
</div>

<div class="border-2 border-yellow-500/60 rounded-lg px-5 py-3 bg-yellow-500/10">
<div class="text-yellow-300 font-bold">C · Add a mutable component <span class="opacity-60 font-normal">~5 min</span></div>
<div class="mt-1">Give <code>Order</code> a <code>List&lt;String&gt; tags</code> and edit it from <code>Main</code>. The shallow-immutability trap, on your own code.</div>
</div>

</div>

<v-click>

<div class="mt-3 text-center text-green-300 text-sm">

**B is the one to do if you only do one.** It creates the itch next session scratches.

</div>

</v-click>

---
layout: default
---

# Homework — the support desk

<div class="mt-4">

`homework/` — a second self-contained exercise. Different domain, same four features.

</div>

<div class="text-sm mt-4">

| Part | What |
|---|---|
| 1 | `Ticket` → a record with **four** components, one of which is a `List` |
| 2 | `TicketOutcome` sealed over `Resolved` / `Escalated` / `Rejected`; four routing rules, in order |
| 3 | `describe()` with pattern variables |
| 4 | A text block that ends with a newline |
| 5 | A text block that doesn't — **SQL** this time, not JSON |

</div>

<v-click>

<div class="mt-4 border-2 border-red-500/50 rounded-lg px-6 py-3 bg-red-500/10">

**The part today's exercise never tested:** `Ticket` holds a `List<String> tags`,
and two checks try to reach inside your record from outside. `final` will not save you.

</div>

</v-click>

<v-click>

<div class="mt-3 text-center text-sm">

`javac *.java` → `java Verify` → **26 checks.** Budget 45–60 min. Bring it next session.

</div>

</v-click>

---
layout: default
---

# Recap

<div class="text-xs">

- **`var`** infers a local's type — use it when the right-hand side already states the type. Locals with an initializer only
- **`var` + a bare diamond gives you `Object`.** Spell the type argument out on the right
- **Records**: one line for `private final` fields, canonical constructor, accessors with **no `get`**, `equals`, `hashCode`, `toString`
- **Value semantics** are the reason: two records with the same components are equal, so `HashSet`, map keys and `assertEquals` all behave
- **Compact constructors** validate *and* normalise before assignment → an invalid instance **cannot exist**
- **A record is only as immutable as its components.** A `List` component is a hole; `List.copyOf` closes it
- **Records for DTOs, classes for JPA entities**
- **Sealed** types close a hierarchy — each outcome carries only its own data, and no nonsense combination can be built
- A permitted subtype is `final`, `sealed` or `non-sealed`, in the same package or module. Records are implicitly final — hence the idiom
- **Enum when the cases carry nothing; sealed interface when each carries something different**
- Sealing buys a **closed set** today and **exhaustiveness** next session
- **`instanceof` with a pattern variable** replaces `instanceof` + a cast
- **Text blocks**: where the closing `"""` sits decides the indentation; whether it's on its own line decides the trailing newline. No interpolation — use `.formatted(...)`

</div>

<v-click>

**Next:** every `if` in `describe()` is deleted and replaced with one exhaustive `switch`.

</v-click>
