package week1.application;

public final class MainApplication {
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
