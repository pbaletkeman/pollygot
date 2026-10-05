package src.main.java.week1.tracking;

import java.util.List;
import java.util.Objects;

public final class TaskTracker {
    private List<Integer> processedIds;

    TaskTracker() {
    }

    TaskTracker(List<Integer> processedIds) {
        this.processedIds = processedIds;
    }

    public List<Integer> getProcessedIds() {
        return this.processedIds;
    }

    public void setProcessedIds(List<Integer> processedIds) {
        this.processedIds = processedIds;
    }

    public TaskTracker processedIds(List<Integer> processedIds) {
        setProcessedIds(processedIds);
        return this;
    }

    public ProcessEnum markProcessed(Integer id) {
        boolean found = processedIds.contains(id);
        if (found) {
            return ProcessEnum.DUPLICATE;
        } else {
            processedIds.add(id);
            return  ProcessEnum.SUCCESS;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof TaskTracker)) {
            return false;
        }
        TaskTracker taskTracker = (TaskTracker) o;
        return Objects.equals(processedIds, taskTracker.processedIds);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(processedIds);
    }

    @Override
    public String toString() {
        return "{ processedIds='" + getProcessedIds() + "'}";
    }



}
