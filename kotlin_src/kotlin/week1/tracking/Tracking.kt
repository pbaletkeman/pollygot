package ca.letkeman.week1.tracking

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock

enum class ProcessEnum {DUPLICATE, SUCCESS}

class TaskIdGenerator {
    val nextId: AtomicInteger = AtomicInteger(0)

    fun incNextId() = nextId.getAndIncrement()
}

data class Statistics(val tasksProduced: AtomicInteger, val tasksConsumed: AtomicInteger, val duplicatesDetected: AtomicInteger)

class TaskTracker(val processedIds: MutableList<Int>) {
    private val lock = ReentrantLock()

    fun markProcessed(id: Int): ProcessEnum {
        lock.lock()
        try {
            val found = processedIds.contains(id)
            if (found){
                return ProcessEnum.DUPLICATE
            } else {
                processedIds.add(id)
                return ProcessEnum.SUCCESS
            }
        } finally {
            lock.unlock()
        }
    }

}
