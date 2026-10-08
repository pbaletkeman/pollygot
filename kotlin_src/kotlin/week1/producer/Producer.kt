class Producer {

    val taskIdGenerator: TaskIdGenerator
    val statistics: Statistics
    val queueManager: QueueManager

    constructor(queueManager: QueueManager, taskIdGenerator: TaskIdGenerator, statistics: Statistics) {
        queueManager = queueManager
        taskIdGenerator = taskIdGenerator
        statistics = statistics
    }

    fun addTasks(num: int) {
        for (i in 1..num) {
            val taskId = taskIdGenerator.incNextId();
            val t = Task(taskId, "task-" + taskid, LocalDateTime.now())
            queueManager.submitTask(t)
            statistics.incTasksProduced()
        }
    }
}
