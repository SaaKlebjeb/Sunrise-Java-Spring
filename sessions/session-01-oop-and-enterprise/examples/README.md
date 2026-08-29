# Runnable examples

Two small programs that demonstrate the pillars instead of describing them. They are
**not** the exercise — the exercise lives in `../src/`. Nothing here is graded and nothing
here is checked by `Verify`. Run them, predict the output before you look, and do the
experiment at the bottom of each file.

Neither needs a build tool. From a terminal in this folder:

```
java BlastRadius.java
java DispatchDemo.java
```

One file, one command, no compile step — Java runs a single source file directly. Edit in
IntelliJ if you like, but run here: with no build tool in the folder, IntelliJ cannot reliably
work out which class to launch. Its built-in terminal (**Alt+F12**) keeps you in one window.

| File | Pillar | The question it answers |
|---|---|---|
| `BlastRadius.java` | Encapsulation | What does `private` actually buy me, if the program works either way? |
| `DispatchDemo.java` | Polymorphism | Which things are chosen by the object, and which by the declared type? |

**`BlastRadius.java`** puts a public-field order and a private-field order side by side. They
behave identically until (a) someone assigns nonsense to the public field, and (b) the way
money is stored has to change. The comment at the bottom asks you to inflict that change
yourself and count the compile errors. That count is the blast radius.

**`DispatchDemo.java`** shows the one thing that *is* polymorphic — a non-private, non-static
instance method — next to the three things that only look like it: fields, `static` methods
and `private` methods. All three are resolved from the **declared type** at compile time, and
all three catch people out. Predict all four answers on paper before running it.
