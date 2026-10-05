package week1.tracking;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class Statistics {

    private AtomicInteger tasksProduced;
    private AtomicInteger tasksConsumed;
    private AtomicInteger duplicatesDetected;

    public Statistics() {
        this.tasksProduced = new AtomicInteger(0);
        this.tasksConsumed = new AtomicInteger(0);
        this.duplicatesDetected = new AtomicInteger(0);
    }

    public AtomicInteger getTasksProduced() {
        return this.tasksProduced;
    }

    public AtomicInteger getTasksConsumed() {
        return this.tasksConsumed;
    }

    public AtomicInteger getDuplicatesDetected() {
        return this.duplicatesDetected;
    }

    public Statistics incDuplicatesDetected() {
        this.duplicatesDetected.getAndIncrement();
        return this;
    }

    public Statistics incTasksProduced() {
        this.tasksProduced.getAndIncrement();
        return this;
    }

    public Statistics incTasksConsumed() {
        this.tasksConsumed.getAndIncrement();
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof Statistics)) {
            return false;
        }
        Statistics statistics = (Statistics) o;
        return tasksProduced == statistics.tasksProduced &&
            tasksConsumed == statistics.tasksConsumed &&
            duplicatesDetected == statistics.duplicatesDetected;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tasksProduced, tasksConsumed, duplicatesDetected);
    }

    @Override
    public String toString() {
        return "{" +
            " tasksProduced='" + getTasksProduced() + "'" +
            ", tasksConsumed='" + getTasksConsumed() + "'" +
            ", duplicatesDetected='" + getDuplicatesDetected() + "'" +
            "}";
    }

}
