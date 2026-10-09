package week1.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import week1.consumer.Consumer;
import week1.model.Task;
import week1.producer.Producer;
import week1.queue.QueueManager;
import week1.tracking.Statistics;
import week1.tracking.TaskIdGenerator;
import week1.tracking.TaskTracker;

public final class MainApplication {

    public String doWork() throws InterruptedException, ExecutionException {
        return doWork(new QueueManager(10000));
    }

    public String doWork(QueueManager queue) throws InterruptedException, ExecutionException {
        Objects.requireNonNull(queue);

        TaskIdGenerator generator = new TaskIdGenerator();
        Statistics statistics = new Statistics();
        TaskTracker tracker = new TaskTracker();
        Producer producer =  new Producer(queue, generator, statistics);
        Consumer consumer = new Consumer(queue, tracker, statistics);

        ExecutorService producerPool = Executors.newFixedThreadPool(3, named("producer"));
        ExecutorService consumerPool = Executors.newFixedThreadPool(5, named("consumer"));

        try {
            final List<Future<?>> consumers = IntStream.range(0, 5)
                .<Future<?>>mapToObj(i -> consumerPool.submit(consumerJob(consumer)))
                .toList();

            List<Future<?>> producers = List.of(
                producerPool.submit(producerJob(producer, 3333)),
                producerPool.submit(producerJob(producer, 3333)),
                producerPool.submit(producerJob(producer, 3334)));

            producerPool.shutdown();
            if (!producerPool.awaitTermination(30, TimeUnit.SECONDS)) {
                producerPool.shutdownNow();
                throw new IllegalStateException("producer pool hung");
            }
            for (Future<?> f: producers) {
                f.get();
            }

            for (int i = 0; i < 5; i++) {
                queue.submitTask(new Task(Task.SHUTDOWN, "shutdown", LocalDateTime.now()));
            }

            consumerPool.shutdown();
            if (!consumerPool.awaitTermination(30, TimeUnit.SECONDS)) {
                consumerPool.shutdown();
                throw new IllegalStateException("consumer pool hung - missing poison pill");
            }
            for (Future<?> f: consumers) {
                f.get();
            }

            return statistics.getReport();
        } finally {
             producerPool.shutdownNow();
             consumerPool.shutdownNow();
        }
    }

    private static Runnable producerJob(Producer p, int n) {
        return () -> {
            try {
                p.addTasks(n);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };
    }

    private static Runnable consumerJob(Consumer c) {
        return () -> {
            try {
                c.run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };
    }

    private static ThreadFactory named(String prefix) {
        AtomicInteger n = new AtomicInteger();
        return r -> new Thread(r, prefix + "-" + n.incrementAndGet());
    }

    static void main(String[] args) {
        MainApplication mainApplication = new MainApplication();
        try {
            System.out.println(mainApplication.doWork());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }
}
