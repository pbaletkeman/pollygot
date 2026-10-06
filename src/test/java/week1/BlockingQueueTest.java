package week1;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import week1.queue.BlockingQueue;

public class BlockingQueueTest {

    private static final long GET_TIMEOUT_SECONDS = 5;

    private ExecutorService queueExecutor;
    private BlockingQueue<String> blockingQueue;

    @BeforeEach
    void setUp() {
        queueExecutor = Executors.newSingleThreadExecutor();
    }

    @AfterEach
    void tearDown() {
        // Close first: unblocks the worker parked in await() if an assertion
        // failed before the queue was drained. Then stop the executor thread.
        queueExecutor.shutdownNow();
    }

    @Test
    void put_blocks_when_full_and_releases_on_take() throws InterruptedException {
        blockingQueue = new BlockingQueue<>(1);

        // Fill the queue on the main thread so the async put below has no space.
        blockingQueue.put("a");

        CountDownLatch aboutToPut = new CountDownLatch(1);

        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            aboutToPut.countDown();           // fires immediately before the call
            try {
                blockingQueue.put("b");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }, queueExecutor);

        // Latch proves the worker reached the call site; the sleep is a backstop
        // covering the gap between countDown() and actually entering await().
        aboutToPut.await();
        Thread.sleep(100);

        // The assertion that matters: without it the test passes even if put
        // never blocks (busy-waits) or the queue grows past its capacity.
        assertFalse(
            future.isDone(),
            "put(\"b\") must park while the queue is full, not complete"
        );

        // take() frees the one slot and signals notFull, releasing the parked put.
        assertEquals("a", blockingQueue.take());

        // Bounded get: an unbounded one turns a regression into a hung CI job.
        // assertDoesNotThrow so a failure isn't misdiagnosed through the
        // ExecutionException wrapping trap.
        assertDoesNotThrow(
            () -> future.get(GET_TIMEOUT_SECONDS, TimeUnit.SECONDS),
            "parked put should complete normally once space is available"
        );

        // "b" was admitted, in FIFO order.
        assertEquals("b", blockingQueue.take());

        blockingQueue.close();
    }

    @Test
    void close_releases_blocked_put_with_exception() throws InterruptedException {
        blockingQueue = new BlockingQueue<>(1);
        // Fill the queue on the main thread so the async put below has no space.
        blockingQueue.put("a");

        CountDownLatch aboutToPut = new CountDownLatch(1);

        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            aboutToPut.countDown();           // fires immediately before the call
            try {
                blockingQueue.put("b");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }, queueExecutor);

        // Latch proves the worker reached the call site; the sleep is a backstop
        // covering the gap between countDown() and actually entering await().
        aboutToPut.await();
        Thread.sleep(100);

        // Belt and braces: "b" must NOT have been admitted while the queue was
        // full. Without this, a put() that silently ignores capacity would let
        // the future complete and the test below would pass for the wrong reason.
        assertFalse(
            future.isDone(),
            "put(\"b\") must park while the queue is full, not complete"
        );

        // Deliberately NOT calling take() here: a take() would signal notFull and
        // wake the parked put on its own, so a broken close() that forgets to
        // signalAll would still pass. close() has to be the only thing that
        // releases this worker.
        blockingQueue.close();

        // A Future never throws IllegalStateException directly -- it wraps it in
        // ExecutionException (CompletableFuture additionally wraps in
        // CompletionException, which get() unwraps). assertThrows on
        // IllegalStateException.class would fail and send you debugging the
        // wrong thing.
        ExecutionException wrapped = assertThrows(
            ExecutionException.class,
            () -> future.get(GET_TIMEOUT_SECONDS, TimeUnit.SECONDS),
            "close() must release the parked put with an exception"
        );
        assertInstanceOf(
            IllegalStateException.class,
            wrapped.getCause(),
            "the parked put must fail with IllegalStateException(\"queue is closed\")"
        );

        // close() does not discard buffered items: they drain first, and only an
        // empty closed queue reports EOF as null.
        assertEquals("a", blockingQueue.take());
        assertNull(blockingQueue.take(), "empty + closed queue must return null");

        blockingQueue.close();
    }

    @Test
    void close_releases_blocked_take_with_null() throws InterruptedException {
        blockingQueue = new BlockingQueue<>(2);

        CountDownLatch aboutToTake = new CountDownLatch(1);

        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            aboutToTake.countDown();           // fires immediately before the call
            try {
                return blockingQueue.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }, queueExecutor);

        // Latch proves the worker reached the call site; the sleep is a backstop
        // covering the gap between countDown() and actually entering await().
        aboutToTake.await();
        Thread.sleep(100);

        assertFalse(future.isDone(),
            "take() must park while the queue is empty, not return early");

        blockingQueue.close();

        assertDoesNotThrow(() -> assertNull(future.get(GET_TIMEOUT_SECONDS, TimeUnit.SECONDS),
            "close() must release the parked take with null (EOF)"));

        blockingQueue.close();
    }

    @Test
    void closed_queue_drains_remaining_items_before_returning_null() throws InterruptedException {

        blockingQueue = new BlockingQueue<>(3);

        CountDownLatch aboutToPut = new CountDownLatch(1);

        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            aboutToPut.countDown();           // fires immediately before the call
            try {
                blockingQueue.put("x");
                blockingQueue.put("y");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }, queueExecutor);

        // Latch proves the worker reached the call site; the sleep is a backstop
        // covering the gap between countDown() and actually entering await().
        aboutToPut.await();
        Thread.sleep(100);

        blockingQueue.close();
        assertEquals("x", blockingQueue.take());
        assertEquals("y", blockingQueue.take());

        assertNull(blockingQueue.take());
        assertNull(blockingQueue.take());
    }


    @Test
    void put_null_IllegalArgumentException() throws InterruptedException {

        blockingQueue = new BlockingQueue<>(1);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> blockingQueue.put(null), "put(null) must be rejected");
        assertEquals("null cannot be added to queue", ex.getMessage());

        assertEquals(1, blockingQueue.getSize());
        blockingQueue.put("A");
        assertEquals("A", blockingQueue.take());
        blockingQueue.close();
    }

    @Test
    void size_zero_IllegalArgumentException() {

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> new BlockingQueue<>(0), "buffer size must be > 0");
        assertEquals("buffer size must be > 0", ex.getMessage());
    }

    @Test
    void size_negative_IllegalArgumentException() {

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> new BlockingQueue<>(-1), "buffer size must be > 0");
        assertEquals("buffer size must be > 0", ex.getMessage());
    }

    @Test
    void set_size_negative_IllegalArgumentException() {
        blockingQueue = new BlockingQueue<>();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> blockingQueue.setSize(-1), "buffer set size must be > 0");
        assertEquals("buffer set size must be > 0", ex.getMessage());
    }

    @Test
    void set_size_zero_IllegalArgumentException() {
        blockingQueue = new BlockingQueue<>();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> blockingQueue.setSize(0), "buffer set size must be > 0");
        assertEquals("buffer set size must be > 0", ex.getMessage());
    }

    @Test
    void set_size() {
        blockingQueue = new BlockingQueue<>();
        blockingQueue.setSize(10);
        assertEquals(10, blockingQueue.getSize());
    }

    @Test
    void set_size_constructor() {
        blockingQueue = new BlockingQueue<>(1000);
        assertEquals(1000, blockingQueue.getSize());
    }

    @Test
    void default_constructor() {
        blockingQueue = new BlockingQueue<>();
        assertEquals(1000, blockingQueue.getSize());
    }
}
