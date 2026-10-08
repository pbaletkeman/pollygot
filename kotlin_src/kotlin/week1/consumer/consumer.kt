import ca.letkeman.week1.model.task

class consumer {

    val queue: QueueManager
    val tracker: TaskTracker
    val statistics: Statistics

    constructor(queue: QueueManager, tracker: TaskTracker, statistics: Statistics) {
        this.queue = queue
        this.statistics = statistics
        this.tracker = tracker
    }

    fun processTask(task: Task){
        val p = tracker.markProcessed(task.taskId)
        if (p == ProcessEnum.SUCCESS) {
            statistics.incTaskConsumed()
        } else {
            statistics.incDuplicatesDetected();
        }
    }

    fun run() {
        while (true) {
            val task: Task? = queue.getTask()
            if (task == null) {
                return
            }
            if (task.taskId == Task.SHUTDOWN) {
                return
            }
            processTask(task)
        }
    }

}
