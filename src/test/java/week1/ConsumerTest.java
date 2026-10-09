package week1;

public class ConsumerTest {

}

// TEST consumer_increments_duplicate_counter

//     consumer <- new Consumer(queue, tracker, statistics)
//     consumer.processTask(new Task(7, "task-7", now()))  // -> SUCCESS path
//     consumer.processTask(new Task(7, "task-7", now()))  // -> duplicate path

//     ASSERT statistics.getTasksConsumed()    == 1
//     ASSERT statistics.getDuplicatesDetected == 1


// ConsumerTest.run() pattern:

// ```plaintext

//     queue.submitTask(task(1)); queue.submitTask(task(2)); queue.submitTask(task(3))
//     queue.submitTask(new Task(SHUTDOWN, "shutdown", now()))
//     consumer.run()                          // returns on pill — no thread needed
//     ASSERT statistics.consumed == 3
//     ASSERT statistics.produced == 0         // pills bypass Statistics
//     ASSERT statistics.duplicates == 0
// ```
