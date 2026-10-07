package week1.application;

import week1.consumer.Consumer;
import week1.producer.Producer;
import week1.queue.QueueManager;
import week1.tracking.Statistics;
import week1.tracking.TaskIdGenerator;
import week1.tracking.TaskTracker;
import week1.model.Task;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

public final class MainApplication {


    private void doWork() throws InterruptedException {
        TaskIdGenerator taskIdGenerator = new TaskIdGenerator();
        Statistics statistics = new Statistics();
        TaskTracker taskTracker = new TaskTracker();
        QueueManager queueManager = new QueueManager(10000);

        Producer producer =  new Producer(queueManager, taskIdGenerator, statistics);
        Consumer consumer = new Consumer(queueManager, taskTracker, statistics);

        Thread pt1 = new Thread(() -> {
            try {
                producer.addTasks(3333);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "producer-1");

        Thread pt2 = new Thread(() -> {
            try {
                producer.addTasks(3333);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "producer-2");

        Thread pt3 = new Thread(() -> {
            try {
                producer.addTasks(3334);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "producer-3");

        Thread c1Thread = new Thread(() -> {
            try {
                consumer.run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "consumer-1");

        Thread c2Thread = new Thread(() -> {
            try {
                consumer.run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "consumer-2");

        Thread c3Thread = new Thread(() -> {
            try {
                consumer.run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "consumer-3");

        Thread c4Thread = new Thread(() -> {
            try {
                consumer.run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "consumer-4");

        Thread c5Thread = new Thread(() -> {
            try {
                consumer.run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "consumer-5");

        pt1.start();
        pt2.start();
        pt3.start();
        c1Thread.start();
        c2Thread.start();
        c3Thread.start();
        c4Thread.start();
        c5Thread.start();
        pt1.join();
        pt2.join();
        pt3.join();

        for (int i = 1; i<=5; i++) {
            Task t = new Task(Task.SHUTDOWN, "shutdown", LocalDateTime.now());
            queueManager.submitTask(t);
        }

        c1Thread.join(TimeUnit.SECONDS.toMillis(30));
        c2Thread.join(TimeUnit.SECONDS.toMillis(30));
        c3Thread.join(TimeUnit.SECONDS.toMillis(30));
        c4Thread.join(TimeUnit.SECONDS.toMillis(30));
        c5Thread.join(TimeUnit.SECONDS.toMillis(30));
        if (c1Thread.isAlive()){
            System.err.println("consumer-1 did not exit properly, missing posion pill?");
        }
        if (c2Thread.isAlive()){
            System.err.println("consumer-2 did not exit properly, missing posion pill?");
        }
        if (c3Thread.isAlive()){
            System.err.println("consumer-3 did not exit properly, missing posion pill?");
        }
        if (c4Thread.isAlive()){
            System.err.println("consumer-4 did not exit properly, missing posion pill?");
        }
        if (c5Thread.isAlive()){
            System.err.println("consumer-5 did not exit properly, missing posion pill?");
        }
    }

    static void main(String[] args) {

    }
}


//  5. Step 1 — create one of each, shared: TaskIdGenerator, Statistics, TaskTracker, QueueManager
//  6. Steps 2–4 — two pools:
//   - Producer pool (3 threads): 3 × Producer(statistics, taskIdGenerator, queueManager)
//   - Consumer pool (5 threads): 5 × Consumer(queueManager, tracker, statistics)
//   - Submit producer.addTasks(n) ×3, consumer.run() ×5
//   - Totals must hit exactly 10000: e.g. 3334 + 3333 + 3333
//  7. Step 5 — shut down the producer pool, awaitTermination(...) until all 3 finish
//  8. Step 6 — submit 5 pills (one per consumer):
//   for (int i = 0; i < 5; i++)
//       queueManager.submitTask(new Task(Task.SHUTDOWN, "shutdown", LocalDateTime.now()));
//   Pills go straight to the queue, bypassing Statistics → produced stays 10000 ✓
//  9. Step 7 — shut down consumer pool, await until all 5 exit
// 10. Step 8 — print the report and assert: produced = 10000, consumed = 10000, duplicates = 0, tracker.getProcessedIds().size() = 10000
