package week1.producer;

import java.time.LocalDateTime;
import java.util.Objects;

import week1.model.Task;
import week1.queue.QueueManager;
import week1.tracking.Statistics;
import week1.tracking.TaskIdGenerator;

public final class Producer {

    private final TaskIdGenerator taskIdGenerator;
    private final Statistics statistics;
    private final QueueManager queueManager;

    public Producer(QueueManager queueManager, TaskIdGenerator taskIdGenerator, Statistics statistics) {
        this.statistics = Objects.requireNonNull(statistics);
        this.taskIdGenerator = Objects.requireNonNull(taskIdGenerator);
        this.queueManager = Objects.requireNonNull(queueManager);
    }

    public void addTasks(int num) throws InterruptedException {
        for (int i = 0; i < num; i++) {

            int taskId = taskIdGenerator.incNextId();
            Task t = new Task(taskId, "task-" + taskId, LocalDateTime.now());
            queueManager.submitTask(t);
            statistics.incTasksProduced();

        }
    }

}
