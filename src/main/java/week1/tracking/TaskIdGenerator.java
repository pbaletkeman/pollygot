package src.main.java.week1.tracking;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public final class TaskIdGenerator {
    private AtomicInteger nextId;

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
        if (!(o instanceof TaskIdGenerator)) {
            return false;
        }
        TaskIdGenerator taskIdGenerator = (TaskIdGenerator) o;
        return Objects.equals(nextId, taskIdGenerator.nextId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(nextId);
    }

    @Override
    public String toString() {
        return "{ nextId='" + getNextId() + "'}";
    }

}
