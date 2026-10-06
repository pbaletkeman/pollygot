package week1.model;

import java.time.LocalDateTime;
import java.util.Objects;

public final class Task {
    private String taskId;
    private String payload;
    private LocalDateTime createTimestamp;


    public Task() {
    }

    public Task(String taskId, String payload, LocalDateTime createTimestamp) {
        this.taskId = taskId;
        this.payload = payload;
        this.createTimestamp = createTimestamp;
    }

    public String getTaskId() {
        return this.taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getPayload() {
        return this.payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public LocalDateTime getCreateTimestamp() {
        return this.createTimestamp;
    }

    public void setCreateTimestamp(LocalDateTime createTimestamp) {
        this.createTimestamp = createTimestamp;
    }

    public Task taskId(String taskId) {
        setTaskId(taskId);
        return this;
    }

    public Task payload(String payload) {
        setPayload(payload);
        return this;
    }

    public Task createTimestamp(LocalDateTime createTimestamp) {
        setCreateTimestamp(createTimestamp);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Task task)) {
            return false;
        }
        return Objects.equals(taskId, task.taskId)
            && Objects.equals(payload, task.payload)
            && Objects.equals(createTimestamp, task.createTimestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId, payload, createTimestamp);
    }

    @Override
    public String toString() {
        return "{" + " taskId='" + getTaskId() + "'"
            + ", payload='" + getPayload() + "'"
            + ", createTimestamp='" + getCreateTimestamp() + "'}";
    }
}
