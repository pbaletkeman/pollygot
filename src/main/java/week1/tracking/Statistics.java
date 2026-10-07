package week1.tracking;

import java.util.concurrent.atomic.AtomicInteger;

public final class Statistics {

    private final AtomicInteger tasksProduced;
    private final AtomicInteger tasksConsumed;
    private final AtomicInteger duplicatesDetected;


    public Statistics() {
        this.tasksProduced = new AtomicInteger(0);
        this.tasksConsumed = new AtomicInteger(0);
        this.duplicatesDetected = new AtomicInteger(0);
    }

    public int getTasksProduced() {
        return this.tasksProduced.get();
    }

    public int getTasksConsumed() {
        return this.tasksConsumed.get();
    }

    public int getDuplicatesDetected() {
        return this.duplicatesDetected.get();
    }

    public void incDuplicatesDetected() {
        this.duplicatesDetected.getAndIncrement();
    }

    public void incTasksProduced() {
        this.tasksProduced.getAndIncrement();
    }

    public void incTasksConsumed() {
        this.tasksConsumed.getAndIncrement();
    }

    @Override
    public String toString() {
        return "{ tasksProduced=" + getTasksProduced()
            + ", tasksConsumed=" + getTasksConsumed()
            + ", duplicatesDetected=" + getDuplicatesDetected()
            + "}";
    }

}
