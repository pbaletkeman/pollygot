package week1.tracking;

import java.util.concurrent.atomic.AtomicInteger;

public final class TaskIdGenerator {
    private final AtomicInteger nextId;

    public AtomicInteger getNextId() {
        return this.nextId;
    }

    public int incNextId() {
        return nextId.getAndIncrement();
    }

    public TaskIdGenerator() {
        this.nextId = new AtomicInteger(0);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof TaskIdGenerator taskIdGenerator)) {
            return false;
        }
        return this.nextId.get() == taskIdGenerator.nextId.get();
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(nextId.get());
    }

    @Override
    public String toString() {
        return "{ nextId='" + getNextId() + "'}";
    }

}
