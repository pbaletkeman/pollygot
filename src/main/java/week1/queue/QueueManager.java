package week1.queue;

import week1.model.Task;

public final class QueueManager {

    private final BlockingQueue<Task> queue;

    public QueueManager(int capacity) {
        this.queue = new BlockingQueue<>(capacity);
    }

    public QueueManager() {
        this.queue = new BlockingQueue<>();
    }

    public void submitTask(Task task) throws InterruptedException {
        this.queue.put(task);
    }

    public Task getTask() throws InterruptedException {
        return this.queue.take();
    }
}
