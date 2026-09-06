# Homework — three questions your suite cannot answer

**Budget 45 minutes.** There is no separate project for this one. You work in the same
folder you used in class: `../order-tests/`.

## The task

Your thirteen tests are green. Three behaviours still have no test at all, and each one is a
different kind of blind spot. Write one test for each, in the file where it belongs, next to
the tests you already wrote.

Nobody tells you what to assert this time. You get the question; you work out what the code
should do and pin it down.

### 1. An invalid discount

`Order.withDiscount` refuses a percentage below 0 or above 100. Every test you wrote in class
handed it a sensible number, so nothing checks the refusal.

Delete the guard clause and run the suite. Still green — that is the gap. Put it back, then
write the test that would have gone red.

### 2. A null list, not an empty one

`OrderReporter.summarise` guards two different things in one `if`: `orders == null` and
`orders.isEmpty()`. You tested the empty list. Nothing calls it with `null`.

Two conditions, one test. Decide what `summarise(null)` should do before you write anything —
returning the zero report and throwing are both defensible, and the test is where you say
which one this class promises.

### 3. An order that is legitimately free

Change `total < 0` to `total <= 0` in `Order` and run the suite. **Everything still passes**,
because no test you wrote ever built a free order — a replacement, a fully-discounted line, a
promotional item.

Put it back and write the test that pins down what is **allowed**.

## Why the third one is the point

Cases 1 and 2 are ordinary gaps: a branch nobody exercised. Case 3 is different. The rule got
**stricter**, not broken, and a suite made entirely of "it refuses bad input" tests cannot
tell the difference between a deliberate rule change and somebody quietly cutting off a real
customer.

Most suites are written that way. Yours does not have to be.

## How to go about it

For each of the three:

1. **Guess first.** "What could somebody change in this method that none of my tests would
   notice?" One minute of guessing teaches more than reading the answer.
2. Make the change in `src/main/java`, run the suite, watch it stay green.
3. **Undo the change.**
4. Write the test that would have caught it. Run it, watch it pass.
5. Break the code once more, and confirm the new test goes red. A test you have never seen
   fail is a test you have no reason to trust.

Done looks like **sixteen green tests**, and three breaks you can no longer make without
something going red.

## What to hand in

Nothing to submit. Bring one sentence to the next session: **which of the three you had never
thought to test, and why.**
