fun doWork() {
    val taskIdGenerator = TaskIdGenerator()
    val statistics = Statistics()
    val taskTracker = taskTracker()
    val queueManager = QueueManager(10000)

    val producer = Producer(queueManager, taskIdGenerator, statistics)
    val consumer = Consumer(queueManager, taskTracker, statistics)
}
