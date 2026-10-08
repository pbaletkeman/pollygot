package week1;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import week1.tracking.ProcessEnum;
import week1.tracking.TaskTracker;

public class TaskTrackerTest {

    @Test
    void test_tasktracker_flags_second_processing_of_same_id() {

        TaskTracker tracker = new TaskTracker();
        assertEquals(ProcessEnum.SUCCESS, tracker.markProcessed(42));
        assertEquals(ProcessEnum.DUPLICATE, tracker.markProcessed(42));
        assertEquals(ProcessEnum.DUPLICATE, tracker.markProcessed(42));
        assertEquals(1, tracker.getProcessedIds().size());
    }

    @Test
    void test_toString() {
        TaskTracker tracker = new TaskTracker();
        tracker.markProcessed(1);
        tracker.markProcessed(2);
        tracker.markProcessed(3);
        tracker.markProcessed(4);
        tracker.markProcessed(5);

        assertEquals("{ processedIds=[1, 2, 3, 4, 5]}", tracker.toString());
    }

}
