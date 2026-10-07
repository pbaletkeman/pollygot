package week1.tracking;

import java.util.concurrent.atomic.AtomicInteger;

public final class TaskIdGenerator {
    private final AtomicInteger nextId;

    public int getNextId() {
        return this.nextId.get();
    }

    public int incNextId() {
        return nextId.getAndIncrement();
    }

    public TaskIdGenerator() {
        this.nextId = new AtomicInteger(0);
    }

    @Override
    public String toString() {
        return "{ nextId='" + getNextId() + "'}";
    }

}
