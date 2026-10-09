package week1;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import week1.model.Task;

public class TaskTest {

    @Test
    void testEquals_SameObject_ShouldReturnTrue() {
        Task task = new Task(0, "payload", LocalDateTime.now());

        // Reflexive: An object must equal itself
        assertEquals(task, task);
    }

    @Test
    void testEquals_differentObject_ShouldNotEqual() {
        Task task = new Task(0, "payload", LocalDateTime.now());
        Task task1 = new Task(1, "payload1", LocalDateTime.now());

        assertNotEquals(task, task1);
    }

    @Test
    void testEquals_differentPayload_ShouldNotEqual() {
        LocalDateTime t = LocalDateTime.now();
        Task task = new Task(0, "payload", t);
        Task task1 = new Task(0, "payload1", t);

        // Reflexive: An object must equal itself
        assertNotEquals(task, task1);
    }

    @Test
    void testHash() {
        Task task = new Task(0, "payload", LocalDateTime.now());

        // Rule 1: Multiple calls on the same object must return the same value
        int initialHash = task.hashCode();
        assertEquals(initialHash, task.hashCode(), "Hash code must remain consistent across calls");
        assertEquals(initialHash, task.hashCode());
    }

    @Test
    void testEquals_differentTime_ShouldNotEqual() {
        Task task = new Task(0, "payload", LocalDateTime.now());
        Task task1 = new Task(0, "payload", LocalDateTime.MIN);

        // Reflexive: An object must equal itself
        assertNotEquals(task, task1);
    }

    @Test
    void test_differentObject_Typea() {
        Task task = new Task(0, "payload", LocalDateTime.now());

        // Reflexive: An object must equal itself
        assertNotEquals(LocalDateTime.class, task);
    }

    @Test
    void  test_toString() {
        LocalDateTime t = LocalDateTime.now();

        Task task = new Task(0, "payload", t);

        assertEquals("{ taskId='0', payload='payload', createTimestamp='" + t + "'}", task.toString());

    }

    @Test
    void test_setters() {

        Task task = new Task(0, "payload", LocalDateTime.now());
        task.setTaskId(1);
        task.setPayload("new payload");
        task.setCreateTimestamp(LocalDateTime.MIN);

        assertEquals(1, task.getTaskId());
        assertEquals("new payload", task.getPayload());
        assertEquals(LocalDateTime.MIN, task.getCreateTimestamp());

    }

    @Test
    void test_fluent() {

        LocalDateTime n = LocalDateTime.now();

        Task task = new Task(0, "payload", n);
        Task t = task.taskId(1);

        assertEquals("{ taskId='1', payload='payload', createTimestamp='" + n + "'}", t.toString());

        t = t.payload("new payload");
        assertEquals("{ taskId='1', payload='new payload', createTimestamp='" + n + "'}", t.toString());

        t = t.createTimestamp(LocalDateTime.MIN);
        assertEquals("{ taskId='1', payload='new payload', createTimestamp='" + LocalDateTime.MIN + "'}", t.toString());

    }
}
