# Week 1 Tests

Read it — 81 lines, two condition variables, one closed flag. Before the tests, here's the behavior contract you're actually testing, because every good test names the invariant it protects:

| Line  | Behavior                                | Invariant                |
| ---   | ---                                     | ---                      |
| 18–24 | size <= 0 throws                        | constructor validation   |
| 31–33 | put(null) throws                        | non-null elements        |
| 36–38 | put parks while full                    | bounded blocking         |
| 39–41 | put after close throws                  | no writes after shutdown |
| 52–53 | take parks while empty                  | unbounded blocking       |
| 55–58 | take returns head then signals notFull  | FIFO + space release     |
| 61    | take returns null when empty and closed | null = end-of-stream     |
| 71–76 | close is idempotent + wakes everyone    | shutdown broadcast       |

Note line 61 carefully: a closed queue with items still in the buffer drains first — take() returns real items until it's empty, and only then returns null. That ordering is the thing most people get wrong in their tests.

Pseudocode for the three tests that are actually hard
The straightforward ones (null input, bad capacity, FIFO order) you can write directly. These three need structure:

```plaintext
Test A — put blocks when full, unblocks on take
TEST put_blocks_when_full_and_releases_on_take
    q <- BlockingQueue(1)
    q.put("a")                        // now full

    future <- ASYNC q.put("b")        // dedicated thread / executor

    ASSERT future is NOT done after a short quiet period
        // proves it parked on notFull (line 37), not just slow

    ASSERT q.take() equals "a"
    ASSERT future.get(1, SECONDS) completes normally   // "b" admitted
    ASSERT q.take() equals "b"
```

The isDone() == false assertion is the whole test — without it you're only testing that put eventually returns, which passes even if put never blocks (busy-waits) or if the queue grows past capacity.

```plaintext
Test B — close unblocks a parked put, which must throw
TEST close_releases_blocked_put_with_exception
    q <- BlockingQueue(1)
    q.put("a")

    future <- ASYNC q.put("b")
    AWAIT until future is blocked      // see note below

    q.close()

    e <- future.get(1, SECONDS)        // expect exceptional completion
    ASSERT e unwraps to IllegalStateException
```

Note on AWAIT until blocked: a bare sleep here is the classic flaky-test source — on a loaded CI box the thread may not have reached await() yet, so close() runs first and the test passes for the wrong reason (or fails spuriously). Prefer polling a latch the worker sets right before calling put, plus a short sleep as backstop, and write the assertion so it can't silently pass on timing alone.

```plaintext
Test C — close unblocks a parked take, which must return null
TEST close_releases_blocked_take_with_null
    q <- BlockingQueue(2)
    future <- ASYNC q.take()           // empty queue, will park on notEmpty

    AWAIT until worker is about to call take
    q.close()

    ASSERT future.get(1, SECONDS) is null      // line 61 path
```

```plaintext
Test D — the drain-before-null ordering (this one has no threads)
TEST closed_queue_drains_remaining_items_before_returning_null
    q <- BlockingQueue(3)
    q.put("x"); q.put("y")
    q.close()

    ASSERT q.take() equals "x"
    ASSERT q.take() equals "y"
    ASSERT q.take() is null
    ASSERT q.take() is null      // idempotent after EOF
```

This fails if someone "simplifies" take to check closed first — which is exactly the refactor a future maintainer will attempt.
Full case list

| #   | Test                                               | Asserts                                                                                                          |
| --- | ---                                                | ---                                                                                                              |
| 1   | constructor rejects 0 and negative                 | IllegalArgumentException                                                                                         |
| 2   | default constructor accepts 1000 items then blocks | capacity = DEFAULT_SIZE                                                                                          |
| 3   | put(null)                                          | IllegalArgumentException, and queue still usable afterward                                                       |
| 4   | FIFO across many elements                          | order preserved                                                                                                  |
| 5   | A: put blocks when full                            | isDone() == false, then releases                                                                                 |
| 6   | B: close → parked put throws                       | IllegalStateException                                                                                            |
| 7   | C: close → parked take returns null                | null                                                                                                             |
| 8   | D: drain then null                                 | order above                                                                                                      |
| 9   | close twice                                        | second call no-op, no exception (line 71)                                                                        |
| 10  | put after close on a non-full queue                | still throws (line 39 runs before line 42 regardless of capacity)                                                |
| 11  | interrupt a parked take                            | InterruptedException propagates, lock released — follow with a put/take from another thread to prove no deadlock |
| 12  | N producers + M consumers concurrently             | total items out == total in, no duplicates, no lost elements                                                     |

Test 12 is the only one that can catch a lost-wakeup bug. Run it in a loop (say 100 iterations) — concurrency bugs are probabilistic, and a single pass proves almost nothing.
Two traps specific to this file

Traps in your assertions. Test 6 wraps the exception: a Future delivers it as ExecutionException with the IllegalStateException as the cause. Asserting assertThrows(IllegalStateException.class, future::get) will fail and send you debugging the wrong thing.
The signal vs signalAll question (lines 43, 57). This code uses single signal calls, which is correct only because exactly one waiter can make progress per state change. Worth asking yourself: if a future change added a putAll that frees five slots, would signal() on line 57 still be right? That's the kind of question a good test file documents — put the reasoning in a comment above test 12.
A check before you write these

---

Both, then — take() first since the harness depends on understanding it.
take() line by line (lines 49–66)
**Line 50 — lock.lock()**
Acquires the monitor outside any try. This is deliberate and correct: if lock() itself were inside a try/finally, an interrupt or failure there would run finally and call unlock() on a lock you never held → IllegalStateException masking the real error. Good habit to copy.
**Line 52 — while (buffer.isEmpty() && !closed)**
Two things packed together:

- The while (not if) is because await() can return without the condition being true. signalAll() on line 76 wakes every waiter, but if there's only one item in the buffer, the first consumer to wake takes it and the other four find the buffer empty again. Without the loop they'd fall through to line 55 and read a phantom state. Spurious wakeups are legal in the JVM spec, too — the loop is the only defense.
- && !closed is the shutdown escape hatch. It's what lets line 61 exist: when close() flips the flag and broadcasts, the loop condition goes false on an empty buffer and control falls out of the loop instead of parking forever.

**Lines 55–58 — the happy path**
if (!buffer.isEmpty()) — this is a second check, separate from the loop guard, because both loop exits are now possible: "not empty anymore" (someone signaled) or "closed". The buffer may still have items in it. buffer.remove(0) takes index 0 → FIFO. Then notFull.signal() — this is the handoff back to a parked producer on line 37. Without it, a full queue would stay full forever after one take, because nobody wakes the producer.
return inside try still runs finally, so the lock is released before the value ever reaches the caller. That's the entire reason this method can be written this concisely.

**Lines 60–62 — the EOF path**
Buffer empty and closed → return null. Note this null is a sentinel, not an error. The contract callers must learn: keep calling take() until it returns null, exactly like Iterator.hasNext(). A caller that treats null as a value to store will NPE downstream.

**Line 63 — finally { lock.unlock(); }**
Runs on normal return, on return null, and on InterruptedException from await() at line 53. That third case is the one people miss: an interrupt leaves await() with the lock re-acquired (it's released and re-acquired internally across the wait), so without this finally an interrupted consumer would leave the lock held and deadlock the whole queue.
One thing I'd flag: buffer.remove(0) on an ArrayList shifts every remaining element — O(n) per take. At DEFAULT_SIZE = 1000 it's invisible, but the idiomatic backing store for a FIFO is ArrayDeque, which is O(1) at the head. Behavior is identical; don't change it mid-testing, just know why.
The concurrency harness
The core problem: you cannot synchronize with sleep. Time is not a happens-before relationship. Every flaky concurrency test traces back to "I slept 100ms and assumed the other thread got there."
The fix is to replace time with observable state. Three primitives do all the work:

**1. A CountDownLatch (or CyclicBarrier) as a "ready" gate**
Each worker counts down immediately before the call you want to block, and the main thread await()s it. This proves the thread reached the call site — not that it's inside await(), but it eliminates the biggest timing gap.
**2. A bounded Future.get(timeout) — never an unbounded one**
An unbounded get() turns a regression (thread never wakes) into a hung CI job instead of a 1-second failure. Always pass a timeout, and pick it generously: 5 seconds is "instant" to a human and an eternity to a parked thread.
**3. A poll loop with a deadline for "is it parked yet"**
For test A you need "the producer is blocked." You can't observe that directly, so poll a flag the worker sets, with a deadline. Combine with the isDone() == false assertion I showed earlier — belt and braces.
The structure, in pseudocode:

```plaintext
TEST concurrent_drain_loses_nothing
    FOR iteration IN 1..100
        q        <- BlockingQueue(3)          // small: forces blocking both ways
        produced <- concurrent set
        start    <- latch(1)                  // release everyone at once
        done     <- latch(P + 1)

        FOR p IN 1..PRODUCERS
            ASYNC:
                start.await()
                FOR each item assigned to p
                    q.put(item)
                done.countDown()

        FOR c IN 1..CONSUMERS
            ASYNC:
                start.await()
                WHILE (v <- q.take()) is not null
                    collect v
                done.countDown()

        start.countDown()                     // go

        // producers finish first, then close, so consumers see EOF
        AWAIT producers_done
        q.close()
        AWAIT done with timeout(10 SECONDS)   // fails loud, not hangs

        ASSERT collected multiset == produced multiset
              // same size, same elements, no duplicates
```

Three design choices worth understanding:

- capacity = 3, not 1000. With a large buffer nothing ever blocks, so the test never exercises lines 37/53 — it passes even if the blocking logic is completely broken. Small capacity forces the park/signal cycle thousands of times per run.
- close() only after producers finish. If you close early, put legitimately throws IllegalStateException (line 40) and your multiset assertion fails for a correct behavior. Decide whether you're testing "clean shutdown" or "shutdown under load" — they're different tests, and the second one asserts that partial results are consistent rather than complete.
- The multiset assertion, not a count. Counting only catches lost items. Comparing element identity catches a subtler bug: two consumers grabbing the same element (a missing while at line 52) would preserve the count while duplicating one item and dropping another.

Run it 100× inside the test, not once with @RepeatedTest — you want the iteration cheap and the failure reproducible from a single test result.
Quick check before you write it: in the harness above, why does the main thread call q.close() itself rather than having the last producer do it? Think about what happens if a producer throws.
