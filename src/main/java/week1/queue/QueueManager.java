package week1.queue;

import week1.model.Task;

public final class QueueManager {

    private final week1.queue.BlockingQueue<Task> queue = new week1.queue.BlockingQueue<>();

    public void submitTask(Task task) throws InterruptedException {
        queue.put(task);
    }

    public Task getTask() throws InterruptedException {
        return queue.take();
    }
}
