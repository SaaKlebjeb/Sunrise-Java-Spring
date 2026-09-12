# Exercise — Build the Lending Desk

**Seven checkpoints. No skeleton and no `TODO` markers — but every checkpoint ends with the
code, folded away, so that being stuck on Java never costs you the concept.**

This folder contains a README and nothing else. The project you are building is the one you
generated from `start.spring.io` before the break: **`lending-desk`**, group `com.sunrise`,
package `com.sunrise.lendingdesk`, Java 25, Spring Boot 4.0.8, **no dependencies ticked**.

If you did not generate it, or you ticked something and want to start clean, generating it
again takes ninety seconds. Do that rather than patching a project you are unsure about.

---

## How this exercise works

For each checkpoint you get four things:

1. **The classes to create, and the package each one goes in.**
2. **What each class is responsible for**, in a sentence.
3. **The method signatures** other steps depend on. These are a *contract*, not source code —
   the bodies are yours. They are given so that checkpoint 4 can rely on what checkpoint 2 built.
4. **The exact console output** you should see when the checkpoint is done.
5. **The code, folded away** — a *Stuck? Here is the code* block at the end of the checkpoint.

**Open the code block when you are stuck, not before.** The wiring annotations are today's
lesson and copying them teaches you nothing; the plumbing around them — records, interfaces,
`System.out.println` — is not the lesson and is not worth losing ten minutes to. If you are
five minutes into a compile error that is not about the container, open the block.

The code is also there for afterwards. Getting it working and then reading a version somebody
else wrote is a better use of the block than opening it first.

**The comparison is the grade.** Run the application, look at what it printed, and check it
against what this README says. There is no test suite handing you a number today.

### The one piece of scaffolding you may copy

Everything prints through a `CommandLineRunner` — a bean that runs once, after the container is
fully built. The lecture shows its shape. Copy that shape; the contents are yours.

Put your runner in the application class to start with. It is a `@Configuration` class, so
`@Bean` methods work there.

### Pace

**In the room, aim to finish checkpoint 4.** Checkpoints 5, 6 and 7 are for the rest of the day
— do them before you start the homework, because the homework assumes them.

**Open the `pom.xml`, not the folder.**

---

## The domain

A library lending desk. Three books on the shelf, members who borrow them, and a notification
when a loan goes through.

Everything you write lives **under `com.sunrise.lendingdesk`**. That is not a style rule — it is
the root package rule from the lecture, and a class outside that tree is invisible to the
container.

---

## Checkpoint 1 — the shelf

**Create:**

| Class | Package | Responsible for |
|---|---|---|
| `Book` | `...lendingdesk.domain` | A book on the shelf. A record: ISBN, title, number of copies |
| `BookRepository` | `...lendingdesk.repository` | The interface the rest of the application talks to |
| `InMemoryBookRepository` | `...lendingdesk.repository` | The implementation, holding a `Map` in memory |

**The contract:**

```
Optional<Book> findByIsbn(String isbn)
List<Book>     findAll()
boolean        checkOut(String isbn)     // false if unknown or no copies left
```

**The wiring:**

- `InMemoryBookRepository` carries the stereotype that says *"this is the edge of the
  application"*.
- It starts **empty**, and a `@PostConstruct` method called `seed()` fills it. Do not seed in
  the constructor — seeding is exactly the kind of work the lecture says belongs in that hook.
- `seed()` prints one line so you can see when it ran.

**Seed data**, exactly:

| ISBN | Title | Copies |
|---|---|---|
| `978-0132350884` | Clean Code | 3 |
| `978-0134685991` | Effective Java | 2 |
| `978-0201616224` | The Pragmatic Programmer | 1 |

**Then** write a runner that asks the container for a `BookRepository` and prints the catalogue.

### Expected output

```
seeded 3 books
=== catalogue ===
978-0132350884 | Clean Code | 3
978-0134685991 | Effective Java | 2
978-0201616224 | The Pragmatic Programmer | 1
```

**Notice the order of those lines.** `seeded 3 books` comes first, before anything the runner
prints, because `@PostConstruct` runs while the container is still being built and the runner
only runs once it is finished. If you see them the other way round, you seeded in the wrong place.

<details>
<summary><b>Stuck? Here is the code for checkpoint 1</b></summary>

`domain/Book.java`

```java
package com.sunrise.lendingdesk.domain;

public record Book(String isbn, String title, int copies) { }
```

`repository/BookRepository.java`

```java
package com.sunrise.lendingdesk.repository;

import com.sunrise.lendingdesk.domain.Book;

import java.util.List;
import java.util.Optional;

public interface BookRepository {

    Optional<Book> findByIsbn(String isbn);

    List<Book> findAll();

    boolean checkOut(String isbn);
}
```

`repository/InMemoryBookRepository.java`

```java
package com.sunrise.lendingdesk.repository;

import com.sunrise.lendingdesk.domain.Book;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class InMemoryBookRepository implements BookRepository {

    private final Map<String, Book> shelf = new LinkedHashMap<>();

    @PostConstruct
    void seed() {
        put(new Book("978-0132350884", "Clean Code", 3));
        put(new Book("978-0134685991", "Effective Java", 2));
        put(new Book("978-0201616224", "The Pragmatic Programmer", 1));
        System.out.println("seeded " + shelf.size() + " books");
    }

    private void put(Book book) {
        shelf.put(book.isbn(), book);
    }

    @Override
    public Optional<Book> findByIsbn(String isbn) {
        return Optional.ofNullable(shelf.get(isbn));
    }

    @Override
    public List<Book> findAll() {
        return List.copyOf(shelf.values());
    }

    @Override
    public boolean checkOut(String isbn) {
        Book book = shelf.get(isbn);
        if (book == null || book.copies() == 0) {
            return false;
        }
        shelf.put(isbn, new Book(book.isbn(), book.title(), book.copies() - 1));
        return true;
    }
}
```

`LendingDeskApplication.java` — the runner lives here for now

```java
package com.sunrise.lendingdesk;

import com.sunrise.lendingdesk.repository.BookRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class LendingDeskApplication {

    public static void main(String[] args) {
        SpringApplication.run(LendingDeskApplication.class, args);
    }

    @Bean
    CommandLineRunner demo(BookRepository books) {
        return args -> {
            System.out.println("=== catalogue ===");
            books.findAll().forEach(book ->
                    System.out.println(book.isbn() + " | " + book.title() + " | " + book.copies()));
        };
    }
}
```

</details>

---

## Checkpoint 2 — two ways to tell somebody

**Create:**

| Class | Package | Responsible for |
|---|---|---|
| `NotificationSender` | `...lendingdesk.notify` | The interface: one way to tell a member something |
| `EmailNotificationSender` | `...lendingdesk.notify` | Prints an email-shaped line |
| `SmsNotificationSender` | `...lendingdesk.notify` | Prints an SMS-shaped line |

**The contract:**

```
void send(String member, String message)
```

**The wiring:**

- Both implementations are beans. Neither is a service, a repository or a controller, so use the
  annotation that fits.
- **Email is the normal choice**; SMS is the exception. Say so with an annotation rather than by
  deleting one of them.
- Give the SMS sender the explicit bean name `sms`. You will ask for it by that name later.

**Then** extend the runner: ask the container for a single `NotificationSender` and send one
message through it.

### Expected output

Everything from checkpoint 1, then:

```
=== notify ===
[email] to sokha: your card is active
```

**If you see `[sms]`, your default is on the wrong class.**
**If the application will not start at all, read the exception — it names both candidates.**

<details>
<summary><b>Stuck? Here is the code for checkpoint 2</b></summary>

`notify/NotificationSender.java`

```java
package com.sunrise.lendingdesk.notify;

public interface NotificationSender {

    void send(String member, String message);
}
```

`notify/EmailNotificationSender.java`

```java
package com.sunrise.lendingdesk.notify;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class EmailNotificationSender implements NotificationSender {

    @Override
    public void send(String member, String message) {
        System.out.println("[email] to " + member + ": " + message);
    }
}
```

`notify/SmsNotificationSender.java`

```java
package com.sunrise.lendingdesk.notify;

import org.springframework.stereotype.Component;

@Component("sms")
public class SmsNotificationSender implements NotificationSender {

    @Override
    public void send(String member, String message) {
        System.out.println("[sms] to " + member + ": " + message);
    }
}
```

The runner gains a second parameter. The container fills it with the `@Primary` one:

```java
    @Bean
    CommandLineRunner demo(BookRepository books, NotificationSender notifier) {
        return args -> {
            System.out.println("=== catalogue ===");
            books.findAll().forEach(book ->
                    System.out.println(book.isbn() + " | " + book.title() + " | " + book.copies()));

            System.out.println("=== notify ===");
            notifier.send("sokha", "your card is active");
        };
    }
```

</details>

---

## Checkpoint 3 — the service

**Create:**

| Class | Package | Responsible for |
|---|---|---|
| `LendingResult` | `...lendingdesk.domain` | The outcome of one attempt to lend. A record: whether it succeeded, and a message |
| `LendingService` | `...lendingdesk.service` | Lend a book to a member, and tell them |

**The contract:**

```
LendingResult lend(String isbn, String member)
```

**What `lend` does, in this order** — the order matters, because it decides how the printed
lines interleave with the ones your runner prints afterwards:

1. Ask the repository to check the book out.
2. If that fails, return an unsuccessful `LendingResult` and **send nothing**.
3. If it succeeds, send the member a message, **then** return a successful `LendingResult`.

So the sender's line is printed *during* the call, and your runner's line is printed *after* it
returns. If your two lines come out the other way round, you returned before notifying.

**The wiring — this is the point of the checkpoint:**

- `LendingService` carries the stereotype for business logic.
- It takes its two collaborators — a `BookRepository` and a `NotificationSender` — **through its
  constructor**, into `final` fields.
- **No `@Autowired` anywhere.** One constructor is all the container needs.
- **No `new` inside the class.** If you have written `new` anywhere in `LendingService`, the
  wiring is wrong.

**Then** extend the runner: lend `978-0201616224` to `sokha` twice, and print both results.

### Expected output

Everything above, then:

```
=== lending ===
[email] to sokha: you borrowed The Pragmatic Programmer
ok=true  978-0201616224 lent to sokha
ok=false 978-0201616224 no copies left
```

The Pragmatic Programmer has one copy. The second attempt has to fail, and it has to fail
**without** sending a notification — look at the output and confirm there is only one `[email]`
line in this block.

<details>
<summary><b>Stuck? Here is the code for checkpoint 3</b></summary>

`domain/LendingResult.java`

```java
package com.sunrise.lendingdesk.domain;

public record LendingResult(boolean ok, String message) { }
```

`service/LendingService.java`

```java
package com.sunrise.lendingdesk.service;

import com.sunrise.lendingdesk.domain.Book;
import com.sunrise.lendingdesk.domain.LendingResult;
import com.sunrise.lendingdesk.notify.NotificationSender;
import com.sunrise.lendingdesk.repository.BookRepository;
import org.springframework.stereotype.Service;

@Service
public class LendingService {

    private final BookRepository books;
    private final NotificationSender notifier;

    public LendingService(BookRepository books, NotificationSender notifier) {
        this.books = books;
        this.notifier = notifier;
    }

    public LendingResult lend(String isbn, String member) {
        if (!books.checkOut(isbn)) {
            return new LendingResult(false, "no copies left");
        }
        String title = books.findByIsbn(isbn).map(Book::title).orElse(isbn);
        notifier.send(member, "you borrowed " + title);
        return new LendingResult(true, "lent to " + member);
    }
}
```

The runner, extended. `%-5s` is what lines the two `ok=` columns up:

```java
    @Bean
    CommandLineRunner demo(BookRepository books, NotificationSender notifier, LendingService lending) {
        return args -> {
            // ... catalogue and notify blocks as before ...

            System.out.println("=== lending ===");
            String isbn = "978-0201616224";
            for (int attempt = 0; attempt < 2; attempt++) {
                LendingResult result = lending.lend(isbn, "sokha");
                System.out.printf("ok=%-5s %s %s%n", result.ok(), isbn, result.message());
            }
        };
    }
```

</details>

---

## Checkpoint 4 — a class you are not allowed to annotate

**Create:**

| Class | Package | Responsible for |
|---|---|---|
| `AuditLog` | `...lendingdesk.thirdparty` | Appending a line to an audit trail |
| `AuditConfig` | `...lendingdesk.config` | Registering the audit log as a bean |

**The contract:**

```
AuditLog(String system)          // a constructor argument, not a default
void record(String line)
```

**The rule for this checkpoint:** pretend `AuditLog` came out of a jar. **You may not put a
single annotation on it.** No `@Component`, no `@Service`, nothing. Treat the file as read-only
the moment you have written it.

That is the whole point: it is the situation the lecture described, and there is exactly one way
through it.

**The wiring:**

- `AuditConfig` is a `@Configuration` class holding one `@Bean` method that builds an `AuditLog`
  for the system name `sunrise-library`.
- `LendingService` gains a **third** constructor parameter and records one line per attempt,
  successful or not. **Record the audit line before notifying** — checkout, then audit, then
  notify, then return.

### Expected output

Everything above, but the lending block now reads:

```
=== lending ===
[audit sunrise-library] lend 978-0201616224 -> sokha OK
[email] to sokha: you borrowed The Pragmatic Programmer
ok=true  978-0201616224 lent to sokha
[audit sunrise-library] lend 978-0201616224 -> sokha REFUSED
ok=false 978-0201616224 no copies left
```

Five lines, and their order is the order of operations inside `lend`. If your audit line comes
after the email line, you called them the other way round — harmless, but the block will not
match.

**Before you move on**, check one thing: you have not called `new` on a single **collaborator**.
The only `new` calls left are value objects (`Book`, `LendingResult`), a collection or two, and
the one inside the `@Bean` method — where it is the permission the annotation grants you. That
is the actual point of the session.

<details>
<summary><b>Stuck? Here is the code for checkpoint 4</b></summary>

`thirdparty/AuditLog.java` — treat this file as read-only once written

```java
package com.sunrise.lendingdesk.thirdparty;

public class AuditLog {

    private final String system;

    public AuditLog(String system) {
        this.system = system;
    }

    public void record(String line) {
        System.out.println("[audit " + system + "] " + line);
    }
}
```

`config/AuditConfig.java`

```java
package com.sunrise.lendingdesk.config;

import com.sunrise.lendingdesk.thirdparty.AuditLog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuditConfig {

    @Bean
    public AuditLog auditLog() {
        return new AuditLog("sunrise-library");
    }
}
```

`LendingService` — a third constructor parameter, and audit before notify:

```java
    private final BookRepository books;
    private final NotificationSender notifier;
    private final AuditLog audit;

    public LendingService(BookRepository books, NotificationSender notifier, AuditLog audit) {
        this.books = books;
        this.notifier = notifier;
        this.audit = audit;
    }

    public LendingResult lend(String isbn, String member) {
        boolean checkedOut = books.checkOut(isbn);
        audit.record("lend " + isbn + " -> " + member + (checkedOut ? " OK" : " REFUSED"));

        if (!checkedOut) {
            return new LendingResult(false, "no copies left");
        }
        String title = books.findByIsbn(isbn).map(Book::title).orElse(isbn);
        notifier.send(member, "you borrowed " + title);
        return new LendingResult(true, "lent to " + member);
    }
```

**Notice what did not change:** the runner. It never knew how many collaborators
`LendingService` had.

</details>

---

## Checkpoint 5 — tell everybody

**Create:**

| Class | Package | Responsible for |
|---|---|---|
| `BroadcastNotifier` | `...lendingdesk.notify` | Send one message through **every** sender there is |

**The contract:**

```
void announce(String member, String message)
```

**The wiring:**

- Constructor injection again, but the parameter is a **`List<NotificationSender>`**.
- Do not build that list. Do not annotate it. Ask for it and the container fills it.
- Copy it into a `final` field defensively.

**Then** extend the runner: announce a closing time to `sokha`.

### Expected output

Everything above, then:

```
=== broadcast ===
[email] to sokha: the library closes at 18:00
[sms] to sokha: the library closes at 18:00
```

**Both senders, without `BroadcastNotifier` knowing either of them exists.** That is the Strategy
pattern, delivered by the container.

If only one line appears, one of your senders is not a bean. If the order is the other way round,
that is fine — order is not guaranteed unless you ask for it, and the homework is where you ask.

<details>
<summary><b>Stuck? Here is the code for checkpoint 5</b></summary>

`notify/BroadcastNotifier.java`

```java
package com.sunrise.lendingdesk.notify;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BroadcastNotifier {

    private final List<NotificationSender> senders;

    public BroadcastNotifier(List<NotificationSender> senders) {
        this.senders = List.copyOf(senders);
    }

    public void announce(String member, String message) {
        senders.forEach(sender -> sender.send(member, message));
    }
}
```

That constructor parameter is the whole checkpoint. `BroadcastNotifier` names no sender, imports
no implementation, and gets both.

In the runner:

```java
            System.out.println("=== broadcast ===");
            broadcast.announce("sokha", "the library closes at 18:00");
```

</details>

---

## Checkpoint 6 — break it on purpose

No new classes. Four experiments, each about two minutes. **Read every failure before you fix
it**, and undo each change before starting the next.

| # | Break this | What you should see | Why |
|---|---|---|---|
| 1 | Remove the "this is the default" annotation from the email sender | `NoUniqueBeanDefinitionException`, naming **both** candidates | Two beans of one type and no tie-breaker |
| 2 | Move `InMemoryBookRepository` to a package **outside** `com.sunrise.lendingdesk` | `NoSuchBeanDefinitionException` for `BookRepository` — with **nothing** about packages | The root package rule. This is the bug you will meet in a real project |
| 3 | Delete the stereotype from `LendingService` | `NoSuchBeanDefinitionException` — but look at **which** bean it names | The runner asked for it, and nothing had registered it |
| 4 | Remove the `@Bean` method from `AuditConfig` | `NoSuchBeanDefinitionException` for `AuditLog` | Scanning cannot help — the class has no annotation and may not have one |

**Write the first line of each exception down.** You will see all four of these again, in real
projects, and recognising them on sight is worth more than anything else in this exercise.

For experiment 1, also read the **second** paragraph of the message. Spring usually tells you
what to do about it.

---

## Checkpoint 7 — do this one wrong first

**Create:**

| Class | Package | Responsible for |
|---|---|---|
| `LoanTicketPrinter` | `...lendingdesk.ticket` | Printing one loan ticket, with a serial number unique to this printer |
| `TicketDesk` | `...lendingdesk.service` | Issuing a ticket for a loan |

**The contract:**

```
String print(String isbn, String member)     // on LoanTicketPrinter
String issue(String isbn, String member)     // on TicketDesk
```

**How the serial works:** give `LoanTicketPrinter` a `final` field holding a serial generated
**in its constructor** — four random digits, or a static counter, anything that differs between
instances. `print` includes that serial in the string it returns. So two tickets from the *same*
printer carry the *same* serial, and that is what makes the trap visible.

**The wiring:**

- `LoanTicketPrinter` is a bean, and it is **not** a singleton. One printer per use.
- `TicketDesk` is a service and takes what it needs through its constructor.

### Step one — write it the obvious way

Give `TicketDesk` a `final LoanTicketPrinter printer` field, injected through the constructor.
Then issue two tickets from the runner.

```
=== tickets ===
ticket 4417 | 978-0132350884 | sokha
ticket 4417 | 978-0134685991 | dara
```

**The same serial twice.** It compiled. The context started. Nothing warned you.

`LoanTicketPrinter` is marked prototype and the container honoured that perfectly — it was
simply never asked for a second one. A singleton's dependencies are injected **once**, when the
singleton is built, and that was the last time the container was involved.

### Step two — fix it

Change the field type so that `TicketDesk` holds *the ability to get a printer* rather than a
printer. Nothing else in the class changes shape: same constructor, same `final` field.

```
=== tickets ===
ticket 4417 | 978-0132350884 | sokha
ticket 9052 | 978-0134685991 | dara
```

Your serials will differ. **The two lines differing from each other is the whole result.**

Watching that number change is worth more than reading about it, which is why this checkpoint is
last and why you were told to do it wrong first.

<details>
<summary><b>Stuck? Here is the code for checkpoint 7</b></summary>

`ticket/LoanTicketPrinter.java`

```java
package com.sunrise.lendingdesk.ticket;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
@Scope("prototype")
public class LoanTicketPrinter {

    private final int serial = ThreadLocalRandom.current().nextInt(1000, 10000);

    public String print(String isbn, String member) {
        return "ticket " + serial + " | " + isbn + " | " + member;
    }
}
```

`service/TicketDesk.java` — **step one, the version that is wrong**

```java
package com.sunrise.lendingdesk.service;

import com.sunrise.lendingdesk.ticket.LoanTicketPrinter;
import org.springframework.stereotype.Service;

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

**Run it before reading on.** Two tickets, one serial.

`service/TicketDesk.java` — **step two, the fix**

```java
package com.sunrise.lendingdesk.service;

import com.sunrise.lendingdesk.ticket.LoanTicketPrinter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class TicketDesk {

    private final ObjectProvider<LoanTicketPrinter> printers;

    public TicketDesk(ObjectProvider<LoanTicketPrinter> printers) {
        this.printers = printers;
    }

    public String issue(String isbn, String member) {
        return printers.getObject().print(isbn, member);
    }
}
```

Same shape, same `final` field, one word different in the type. The field no longer holds a
printer; it holds *the ability to ask for one*.

In the runner:

```java
            System.out.println("=== tickets ===");
            System.out.println(desk.issue("978-0132350884", "sokha"));
            System.out.println(desk.issue("978-0134685991", "dara"));
```

</details>

---

## What "done" looks like

The full run, top to bottom, in this order:

```
seeded 3 books
=== catalogue ===
978-0132350884 | Clean Code | 3
978-0134685991 | Effective Java | 2
978-0201616224 | The Pragmatic Programmer | 1
=== notify ===
[email] to sokha: your card is active
=== lending ===
[audit sunrise-library] lend 978-0201616224 -> sokha OK
[email] to sokha: you borrowed The Pragmatic Programmer
ok=true  978-0201616224 lent to sokha
[audit sunrise-library] lend 978-0201616224 -> sokha REFUSED
ok=false 978-0201616224 no copies left
=== broadcast ===
[email] to sokha: the library closes at 18:00
[sms] to sokha: the library closes at 18:00
=== tickets ===
ticket 4417 | 978-0132350884 | sokha
ticket 9052 | 978-0134685991 | dara
```

Plus `mvn test` green — the generated `@SpringBootTest` starts your container, so it passes only
if the wiring works.

And the check that matters most: **you never called `new` on a collaborator.** Records,
collections and the one line inside the `@Bean` method are fine — everything that another class
depends on arrived through a constructor.

---

## If you get stuck

Look up what you *see*, not what you think is wrong.

| What you see | What it means | Where to look |
|---|---|---|
| `mvn` says "no tests to run", no run buttons in the IDE | The project model was never built | You opened the folder, not the `pom.xml` |
| `NoSuchBeanDefinitionException` for a class you definitely annotated | The container never scanned it | Is the class **under** `com.sunrise.lendingdesk`? |
| `NoUniqueBeanDefinitionException: ... found 2` | Two candidates, no tie-breaker. **It names both** | The default annotation on the email sender |
| `NoSuchBeanDefinitionException: ... 'AuditLog'` | Scanning cannot find it — and must not | The `@Bean` method in `AuditConfig` |
| `BeanCurrentlyInCreationException` | Two classes need each other | A cycle. It is a design problem, not a wiring one |
| `package jakarta.annotation does not exist` | Wrong import for `@PostConstruct` | It is a Jakarta annotation, not a Spring one. No new dependency needed |
| The catalogue prints nothing, no error | The shelf is empty | `seed()` is not annotated, or you named it something the container is not looking at |
| `seeded 3 books` prints **after** the catalogue | Seeding happened too late | You seeded from the runner instead of `@PostConstruct` |
| `[sms]` where you expected `[email]` | The default is on the wrong class | Checkpoint 2 |
| Only one line under `=== broadcast ===` | One sender is not a bean | Checkpoint 2's annotations |
| Two tickets, one serial | The desk holds one printer forever | Checkpoint 7, step two |
| The app starts, prints the banner, and exits with nothing else | Your runner is not a bean, or it is outside the root package | The `@Bean CommandLineRunner` method |
| A wall of red before any of your code runs | The failure is inside a bean being built | Scroll to `Caused by:` and read the **innermost** line |

**One cause, many symptoms.** A container that will not start takes the whole application down,
so one missing annotation looks like everything being broken. Read one message, fix one cause,
run again.

---

## Two things to try after it is green

Neither is graded; both take two minutes.

**Print every bean name.** Add a temporary runner that walks
`ApplicationContext.getBeanDefinitionNames()`, sorted. Everything you registered is in there,
under the name the container gave it — and so are several dozen beans Spring registered for its
own use. Find `lendingService` and `auditLog` in the list, and notice that `auditLog` is named
after your **method**, not your class.

**Ask for the SMS sender by name.** You gave it the bean name `sms` in checkpoint 2 and never
used it. Add a second `NotificationSender` parameter to some class, qualified by that name, and
watch a message go out over SMS while the default stays email.
