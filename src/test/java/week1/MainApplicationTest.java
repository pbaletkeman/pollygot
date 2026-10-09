package week1;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.function.ThrowingSupplier;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;


import week1.application.MainApplication;
import week1.consumer.Consumer;
import week1.model.Task;
import week1.producer.Producer;
import week1.queue.QueueManager;
import week1.tracking.Statistics;
import week1.tracking.TaskIdGenerator;
import week1.tracking.TaskTracker;

public class MainApplicationTest {

    private record PipelineResult(Statistics statistics, TaskTracker taskTracker) {}

    private static PipelineResult runPipeline(int n) throws InterruptedException {
        TaskIdGenerator taskIdGenerator = new TaskIdGenerator();
        Statistics statistics = new Statistics();
        TaskTracker taskTracker = new TaskTracker();
        QueueManager queueManager = new QueueManager(10000);

        Producer producer =  new Producer(queueManager, taskIdGenerator, statistics);
        Consumer consumer = new Consumer(queueManager, taskTracker, statistics);

        int each = n / 3;

        List<Thread> producers = List.of(
            producerThread(producer, each, "producer-1"),
            producerThread(producer, each, "producer-2"),
            producerThread(producer, n - 2 * each, "producer-3")
        );

        List<Thread> consumers = IntStream.rangeClosed(1, 5)
            .mapToObj(i -> consumerThread(consumer, "consumer-" + i))
            .toList();

        producers.forEach(Thread::start);
        consumers.forEach(Thread::start);

        joinAll(producers, "producers");

        for (int i = 0; i < 5; i++) {
            queueManager.submitTask(new Task(Task.SHUTDOWN, "shutdown", LocalDateTime.now()));
        }

        joinAll(consumers, "consumer");
        return new PipelineResult(statistics, taskTracker);
    }

    private static Thread producerThread(Producer p, int count, String name) {
        return new Thread(() -> {
            try { p.addTasks(count); }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    private static Thread consumerThread(Consumer c, String name) {
        return new Thread(() -> {
            try { c.run(); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    private static void joinAll(List<Thread> threads, String role) throws InterruptedException {
        for (Thread t: threads) {
            t.join(60);
        }
        for (Thread t: threads) {
            assertFalse(t.isAlive(), role + " `" + t.getName() + "` did not terminate");
        }
    }

    private static Set<Integer> expectedIds(int n) {
        return IntStream.rangeClosed(0, n - 1)
            .boxed()
            .collect(Collectors.toSet());
    }

    private static void assertNoLossNoDuplicates(int n, PipelineResult r) {
        Statistics s = r.statistics();
        final Set<Integer> ids = r.taskTracker().getProcessedIds();

        assertEquals(n, s.getTasksProduced(), "produced != " + n + " -> a producer short-circuited");
        assertEquals(n, s.getTasksConsumed(), "consumed != " + n + " -> tasks were LOST in the queue");
        assertEquals(0, s.getDuplicatesDetected(), "duplicates != 0 -> a tasks was processed twice");
        assertEquals(s.getTasksConsumed() + s.getDuplicatesDetected(), ids.size(),
            "consumed + duplicated must account for every id the tracker saw");
    }

    @Test
    @Timeout(30)
    void pipeline_processes_all_tasks_with_no_loss_and_no_duplicates1() throws InterruptedException {
        assertNoLossNoDuplicates(10000, runPipeline(10000));
    }

    @Test
    @Timeout(30)
    void pipeline_is_consistent_across_repeated_runs1() throws InterruptedException {
        for (int i = 0; i < 5; i++) {
            assertNoLossNoDuplicates(1000, runPipeline(1000));
        }
    }

    /////
    private static  MainApplication newApp() {
        return new MainApplication();
    }

    private static Set<Thread> liveThreads() {
        return new HashSet<>(Thread.getAllStackTraces().keySet());
    }

    private static long liveThreadsMatching(String prefix) {
        return liveThreads().stream()
            .filter(t -> t.getName().startsWith(prefix))
            .count();
    }

    /**
     * Poll until at least {@code expected} live threads match the prefix,
     * or the deadline passes. Returns true only if the count was reached.
     */
    private static boolean awaitLiveThreads(String prefix, int expected, long timeoutMillis)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            if (liveThreadsMatching(prefix) >= expected) {
                return true;
            }
            Thread.sleep(1);
        }
        return false;
    }

    private static List<Thread> fromBaseline(Set<Thread> baseline, Thread.State... states) {
        Set<Thread.State> want = Set.of(states);
        return liveThreads().stream()
            .filter(t -> !baseline.contains(t))
            .filter(t -> want.contains(t.getState()))
            .filter(MainApplicationTest::fromThisRun)
            .toList();
    }

    private static boolean fromThisRun(Thread t) {
        if (t.getName().startsWith("producer-") || t.getName().startsWith("consumer-")) {
            return true;
        }
        return Arrays.stream(t.getStackTrace())
            .anyMatch(f -> f.getClassName().startsWith("week1."));
    }

    private static String names(List<Thread> threads) {
        return threads.stream()
            .map(t -> t.getName() + " [" + t.getState() + "]")
            .collect(Collectors.joining(", "));
    }

    private static String watchdog(int seconds, ThrowingSupplier<String> body) throws Exception {
        return assertTimeoutPreemptively(Duration.ofSeconds(seconds), body,
            "watchdog: doWork() still running after " + seconds + "s");
    }

    private static void runInstance(CountDownLatch startGate,
                                   AtomicReference<String> report,
                                   AtomicReference<Throwable> failure) {
        try {
            startGate.await();
            report.set(watchdog(35, () -> newApp().doWork()));
        } catch (Throwable t) {
            failure.set(t);
        }
    }

    private static void assertInstance(String label, AtomicReference<String> report,
                                       AtomicReference<Throwable> failure) {
        Throwable error = failure.get();
        assertNull(error, label + " threw instead of returning a report: " + error);
        String text = report.get();
        assertNotNull(text, label + " returned no report");
        assertTrue(text.contains("Statistics"), label + " report header missing:\n" + text);
        assertTrue(text.contains("Produced = 10000"),
            label + " did not report its own 10000 - cross-talk?:\n" + text);
    }

    @Test
    void doWork_happy_path_completes_and_returns_reports() throws Exception {
        String report = watchdog(35, () -> newApp().doWork());

        assertNotNull(report, "doWork() returned no report");
        assertTrue(report.contains("Statistics"), "report header missing:\n" + report);
        assertTrue(report.contains("Produced = 10000"), "expected 3333 + 3333 + 3334:\n" + report);
    }

    @Test
    void every_produced_task_was_consumed_or_counted_as_duplicate() throws Exception {
        String report = watchdog(35, () -> newApp().doWork());

        final boolean produced = report.contains("Produced = 10000");
        final boolean consumed = report.contains("Consumed = 10000");
        final boolean duplicates = report.contains("Duplicates = 0");
        final boolean processed = report.contains("Processed IDs = ");
        assertTrue(produced);
        assertTrue(consumed);
        assertTrue(duplicates);
        assertTrue(processed);
    }

    @Test
    void terminates_on_its_own_poison_pills_worked() throws Exception {
        LocalDateTime t1 = LocalDateTime.now();
        watchdog(35, () -> newApp().doWork());
        LocalDateTime t2 = LocalDateTime.now();
        long diff = Duration.between(t1, t2).toMillis();
        assertTrue(diff < 30000, "took too long to complete");
    }

    @Test
    void no_thread_leak_after_return() throws Exception {
        final Set<Thread> baseline = liveThreads();

        watchdog(35, () -> newApp().doWork());

        Thread.sleep(500);

        assertEquals(0, liveThreadsMatching("consumer-"),
            "consumer threads still alive -> finally block did not shut the pool down");

        assertEquals(0, liveThreadsMatching("producer-"),
            "producer threads still alive -> finally block did not shut the pool down");

        List<Thread> waiting = fromBaseline(baseline, Thread.State.WAITING, Thread.State.TIMED_WAITING);
        assertTrue(waiting.isEmpty(), "threads from this run left parked: " + names(waiting));
    }

    @Test
    void pools_are_shutdown_even_wehen_body_throws() throws Exception {
        QueueManager poisoned = new QueueManager(10000);
        poisoned.close();

        ExecutionException thrown = assertThrows(ExecutionException.class,
            () -> watchdog(35, () -> newApp().doWork(poisoned)),
                "doWorkd must propagate, not swallow");

        assertTrue(thrown.getCause() instanceof IllegalStateException,
            "cause shold be the closed-queue failre, got: " + thrown.getCause());

        Thread.sleep(500);

        assertEquals(0, liveThreadsMatching("consumer-"), "finally did not shut consumers down");
        assertEquals(0, liveThreadsMatching("producer-"), "finally did not shut producers down");
    }

    @Test
    void interrupt_is_honoured_not_swallowed() throws InterruptedException {
        AtomicReference<Exception> outcome = new AtomicReference<>();
        CountDownLatch finished = new CountDownLatch(1);

        Thread worker = new Thread(() -> {
            try {
                newApp().doWork(new QueueManager(1));
            } catch (Exception e) {
                outcome.set(e);
            } finally {
                finished.countDown();
            }
        }, "doWork-worker");

        worker.start();

        long deadline = System.currentTimeMillis() + 1000;
        while (worker.isAlive()
            && worker.getState() != Thread.State.TIMED_WAITING
            && worker.getState() != Thread.State.WAITING
            && System.currentTimeMillis() < deadline) {
            Thread.sleep(1);
        }
        worker.interrupt();

        assertTrue(finished.await(10, TimeUnit.SECONDS), "worker never returned after interrupt");
        worker.join(1000);
        assertFalse(worker.isAlive(), "worker thread still running");

        assertInstanceOf(InterruptedException.class, outcome.get(),
            "interrupt must surface as InterruptedException, got: " + outcome.get());

        Thread.sleep(500);
        assertEquals(0, liveThreadsMatching("consumer-"), "consumer pool survived the interrupt");
        assertEquals(0, liveThreadsMatching("producer-"), "producer pool survived the interrupt");

    }

    @Test
    void repeated_runs_in_one_JVM_are_stable() throws Exception {
        final Set<Thread> baseline = liveThreads();

        for (int i = 0; i < 5; i++) {
            String report = watchdog(35, () -> newApp().doWork());
            boolean produced = report.contains("Produced = 10000");
            assertTrue(produced);
        }

        assertEquals(0, liveThreadsMatching("consumer-"),
            "consumer threads still alive -> finally block did not shut the pool down");

        assertEquals(0, liveThreadsMatching("producer-"),
            "producer threads still alive -> finally block did not shut the pool down");

        List<Thread> waiting = fromBaseline(baseline, Thread.State.WAITING, Thread.State.TIMED_WAITING);
        assertTrue(waiting.isEmpty(), "threads from this run left parked: " + names(waiting));
    }

    @Test
    void concurrent_instances_do_not_cross_talk() throws Exception {
        CountDownLatch startGate = new CountDownLatch(1);

        AtomicReference<String> firstReport = new AtomicReference<>();
        AtomicReference<Throwable> firstFailure = new AtomicReference<>();
        Thread first = new Thread(() -> runInstance(startGate, firstReport, firstFailure), "instance-1");

        AtomicReference<String> secondReport = new AtomicReference<>();
        AtomicReference<Throwable> secondFailure = new AtomicReference<>();
        Thread second = new Thread(() -> runInstance(startGate, secondReport, secondFailure), "instance-2");

        first.start();
        second.start();
        startGate.countDown();

        final boolean overlapped = awaitLiveThreads("consumer-", 10, 5_000);

        first.join(40000);
        second.join(40000);
        assertFalse(first.isAlive(), "instance-1 did not finish in 40s");
        assertFalse(second.isAlive(), "instance-2 did not finish in 40s");

        assertTrue(overlapped, "instances never overlapped - 10 consumer threads never coexisted");

        assertInstance("instance-1", firstReport, firstFailure);
        assertInstance("instance-2", secondReport, secondFailure);

        Thread.sleep(500);
        assertEquals(0, liveThreadsMatching("consumer-"), "consumer threads left behind");
        assertEquals(0, liveThreadsMatching("producer-"), "producer threads left behind");

    }

    // // TEST 8: concurrent instances do not cross-talk
    // FORK two threads, each running its own doWork() simultaneously
    // BOTH UNDER WATCHDOG 35s
    // ASSERT both return "Produced = 10000"            // separate queues/statistics
    // ASSERT liveThreadsMatching("consumer-") == 0     // 10 consumer threads total, all gone


}


// TEST SUITE: doWork()

// // ---------- helpers ----------
// FUNCTION newApp()            -> fresh MainApplication
// FUNCTION elapsedWhile(fn)    -> run fn, return milliseconds taken
// FUNCTION liveThreadsMatching(prefix) -> count of live threads whose name STARTS WITH prefix
// WATCHDOG (secs)              -> fail test if not finished in N seconds


// // TEST 1: happy path completes and returns a report
// report <- run doWork() UNDER WATCHDOG 35s
// ASSERT report IS NOT NULL
// ASSERT report CONTAINS "Statistics"
// ASSERT report CONTAINS "Produced = 10000"        // 3333 + 3333 + 3334
// ASSERT no watchdog timeout fired                 // did not hang


// // TEST 2: every produced task was consumed or counted as duplicate
// report <- run doWork()
// produced   <- PARSE report FOR "Produced = "
// consumed   <- PARSE report FOR "Consumed = "
// duplicates <- PARSE report FOR "Duplicates = "
// processed  <- PARSE report FOR "Processed IDs = "
// ASSERT produced == 10000
// ASSERT consumed + duplicates == processed
// ASSERT processed == 10000                        // queue fully drained, nothing lost
// ASSERT duplicates >= 0


// // TEST 3: method terminates on its own (poison pills worked)
// t <- elapsedWhile { run doWork() }
// ASSERT t < 30000 ms                              // consumers exited BEFORE the 30s
//                                                  // awaitTermination deadline, proving
//                                                  // 5 pills reached 5 consumers
//                                                  // (would otherwise throw or burn 30s)


// // TEST 4: no thread leak after return
// run doWork()
// WAIT 500 ms
// ASSERT liveThreadsMatching("consumer-") == 0     // finally-block shut pools down
// ASSERT liveThreadsMatching("producer-") == 0
// ASSERT no threads left in state WAITING/TIMED_WAITING from this run


// // TEST 5: pools are shut down even when the body throws
// INJECT failure so doWork throws ExecutionException
// CATCH the exception
// ASSERT it IS ExecutionException                  // propagated, not swallowed
// ASSERT liveThreadsMatching("consumer-") == 0     // finally ran shutdownNow()
// ASSERT liveThreadsMatching("producer-") == 0


// // TEST 6: interrupt is honoured, not swallowed
// RUN doWork() on worker thread
// INTERRUPT worker thread immediately (< 1s)
// ASSERT worker ends with InterruptedException     // declared behaviour
// ASSERT liveThreadsMatching("consumer-") == 0     // pools still cleaned up


// // TEST 7: repeated runs in one JVM are stable (no accumulation)
// FOR i IN 1..5:
//     run doWork() UNDER WATCHDOG 35s
//     ASSERT each run returns a report with "Produced = 10000"
// ASSERT liveThreadsMatching("consumer-") == 0     // 5 runs leaked nothing
// ASSERT liveThreadsMatching("producer-") == 0
// ASSERT total live thread count returns to baseline


// // TEST 8: concurrent instances do not cross-talk
// FORK two threads, each running its own doWork() simultaneously
// BOTH UNDER WATCHDOG 35s
// ASSERT both return "Produced = 10000"            // separate queues/statistics
// ASSERT liveThreadsMatching("consumer-") == 0     // 10 consumer threads total, all gone
