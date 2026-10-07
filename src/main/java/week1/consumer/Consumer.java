package week1.consumer;

import java.util.Objects;

import week1.model.Task;
import week1.queue.QueueManager;
import week1.tracking.ProcessEnum;
import week1.tracking.Statistics;
import week1.tracking.TaskTracker;

public final class Consumer {

    private final QueueManager queue;
    private final TaskTracker tracker;
    private final Statistics statistics;

    public Consumer (QueueManager queue, TaskTracker taskTracker, Statistics statistics) {
        this.queue = Objects.requireNonNull(queue);
        this.tracker = Objects.requireNonNull(taskTracker);
        this.statistics = Objects.requireNonNull(statistics);
    }

    public void processTask(Task task)  {
        if (task != null) {
            ProcessEnum p = tracker.markProcessed(task.getTaskId());
            if (p == ProcessEnum.SUCCESS){
                statistics.incTasksConsumed();
            } else {
                statistics.incDuplicatesDetected();
            }
        }
    }

    public void run() throws InterruptedException {
        while (true) {
            Task task = queue.getTask();
            if (task == null) {
                return;
            }
            if (task.getTaskId() == Task.SHUTDOWN) {
                return;
            }
            processTask(task);
        }
    }
}
