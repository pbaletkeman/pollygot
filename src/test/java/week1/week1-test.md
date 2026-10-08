# > 70 %

1. doWork() is private and returns only the report string. The test needs the Statistics and TaskTracker objects, not a printed string. Either make doWork() package-private and return a small record (e.g. RunResult(statistics, tracker)), or have the test do its own wiring as above. Second option needs zero production changes.
2. Never use Thread.sleep to "wait for things to finish" — always join(timeout) + isAlive() assertion. Sleeps either flake or slow the suite down.
3. The join-before-pills ordering is the assertion most likely to catch a real bug: if someone later moves the pill submission before pt*.join(), a consumer can exit early and Test A immediately reports consumed != 10000.

---

| Class                       | ~Exec. lines | Now      | Tests to add                                                                                                                                                                                                                                             | Est. after       |
| ---                         | ---          | ---      | ---                                                                                                                                                                                                                                                      | ----------       |
| queue.BlockingQueue         | 45           | ✅ ~95% | add: close() idempotent (2nd call no-op), concurrent 1-producer/1-consumer smoke test                                                                                                                                                                     | ~98%            |
| application.MainApplication | ~75          | ❌ 0%   | end-to-end pipeline test (Test A from before) — this is the decisive one                                                                                                                                                                                  | ~95%            |
| model.Task                  | ~30          | ❌ 0%   | TaskTest: both constructors, all getters/setters, fluent builders return this, equals/hashCode contract (equal objects → equal hash; unequal → not), toString, SHUTDOWN == -1                                                                             | ~100% (trivial) |
| tracking.Statistics         | ~20          | ❌ 0%   | StatisticsTest: start at 0; each inc* increments by exactly 1; toString; getReport() string contains Produced/Consumed/Duplicates values                                                                                                                  | ~100%           |
| consumer.Consumer           | ~15          | ❌ 0%   | ConsumerTest (see below)                                                                                                                                                                                                                                  | ~95%            |
| tracking.TaskTracker        | ~10          | ❌ 0%   | TaskTrackerTest: first markProcessed → SUCCESS, second → DUPLICATE, set size stays 1, getProcessedIds() returns a copy (mutating it doesn't corrupt tracker), toString                                                                                    | ~100%           |
| tracking.TaskIdGenerator    | ~8           | ❌ 0%   | TaskIdGeneratorTest: starts at 0; incNextId returns-then-increments (0,1,2…); getNextId doesn't advance; concurrent test: 8 threads × 1,000 incNextId → 8,000 distinct IDs in a ConcurrentHashMap (proves no lost increments — strong interview artifact) | ~100%           |
| producer.Producer           | ~8           | ❌ 0%   | ProducerTest: addTasks(5) → produced == 5, queue holds 5, IDs are 0–4; addTasks(0) → nothing; null ctor args → NullPointerException                                                                                                                       | ~100%           |
| queue.QueueManager          | ~6           | ❌ 0%   | QueueManagerTest: both constructors (default capacity 1000); submitTask/getTask FIFO round-trip; getTask on empty+closed → null                                                                                                                           | ~100%           |
| tracking.ProcessEnum        | ~0           | ❌ 0%   | covered incidentally by TaskTrackerTest/ConsumerTest                                                                                                                                                                                                      | 100%            |

```plaintext

    Suggested test classes (7 new files)
    src/test/java/week1/
    ├── BlockingQueueTest.java        (exists — extend)
    ├── MainApplicationTest.java      ← pipeline: no loss, no duplicates, 10,000
    ├── ConsumerTest.java             ← run() with 3 tasks + pill → consumed == 3, exits on pill;
    │                                    exits on null (closed queue);
    │                                    processTask(null) is a no-op;
    │                                    SUCCESS vs DUPLICATE counter paths
    ├── StatisticsTest.java
    ├── TaskTrackerTest.java
    ├── TaskIdGeneratorTest.java      ← incl. concurrent uniqueness
    ├── ProducerTest.java
    ├── TaskTest.java
    └── QueueManagerTest.java
```

Recommended order

1. StatisticsTest, TaskTrackerTest, TaskTest, QueueManagerTest — pure trivial, fastest wins
2. TaskIdGeneratorTest (incl. concurrency), ProducerTest, ConsumerTest
