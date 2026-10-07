package week1.tracking;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class TaskTracker {
    private final Set<Integer> processedIds = ConcurrentHashMap.newKeySet();

    public TaskTracker() {
    }

    public Set<Integer> getProcessedIds() {
        return Set.copyOf(this.processedIds);
    }

    public ProcessEnum markProcessed(Integer id) {
        return processedIds.add(id) ? ProcessEnum.SUCCESS : ProcessEnum.DUPLICATE;
    }

    @Override
    public String toString() {
        return "{ processedIds=" + processedIds + "}";
    }
}
