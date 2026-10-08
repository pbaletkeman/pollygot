package week1;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import week1.tracking.Statistics;

public class StatisticsTest {

    @Test
    void test_toString() {
        Statistics statistics = new Statistics();

        assertEquals("{ tasksProduced=0, tasksConsumed=0, duplicatesDetected=0}", statistics.toString());
    }

    @Test
    void test_getReport() {
        Statistics statistics = new Statistics();

        String report = "\n=======================\n Statistics\n=======================\n"
        + "- Produced = 0\n- Consumed = 0\n- Duplicates = 0\n- Processed IDs = 0\n-----------------------";

        assertEquals(report, statistics.getReport());
    }

    @Test
    void test_increment(){
        Statistics statistics = new Statistics();
        statistics.incDuplicatesDetected();
        statistics.incTasksProduced();
        statistics.incTasksConsumed();

        assertEquals(1, statistics.getTasksProduced());
        assertEquals(1, statistics.getTasksConsumed());
        assertEquals(1, statistics.getDuplicatesDetected());
    }
}
