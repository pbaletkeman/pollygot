package ca.letkeman.week1.queue

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

import ca.letkeman.week1.task.Task

const val DEFAULT_SIZE = 1000
class BlockingQueue <T> {
    private val buffer = mutableListOf<T>()

    private val lock = ReentrantLock()
    private val notEmpty = lock.newCondition()
    private val notFull = lock.newCondition()
    private var closed = false

    var capacity: Int = DEFAULT_SIZE
        get() = lock.withLock { field }              // reads are always locked
        set(value) {
            require(value > 0) { "buffer size must be > 0" }
            lock.withLock {
                require(value >= buffer.size) { "capacity $value < ${buffer.size} elements already queued" }
                field = value
                notFull.signalAll()                  // a bigger queue may unblock producers
            }
        }

    constructor(size: Int) {
        this.capacity = size
    }

    constructor(){
        this(DEFAULT_SIZE)
    }

    fun put(item: T) {

        lock.lock()
        try {
            while (buffer.size >= this.capacity && !closed){
                notFull.await()
            }
            check(!closed) { "queue is closed" }
            buffer.add(item)
            notEmpty.signal()
        } finally {
            lock.unlock()
        }
    }

    fun take(): T?  {
        lock.lock()
        try {
            while (buffer.isEmpty() && !closed) {
                notEmpty.await()
            }
            if (!buffer.isEmpty()){
                val item = buffer.removeAt(0)
                notFull.signal()
                return item
            } else {
                return null
            }
        } finally {
            lock.unlock()
        }
    }

    fun close() {
        lock.lock()
        try {
            if (closed) {
                return
            }
            closed = true
            notFull.signalAll()
            notEmpty.signalAll()
        } finally {
            lock.unlock()
        }
    }
}

class QueueManager {

    private val queue: BlockingQueue<Task>

    constructor (capacity: Int) {
        this.queue = BlockingQueue<Task>(capacity)
    }

    constructor () {
        this.queue = BlockingQueue<Task>()
    }

    fun submitTask(task: Task) = this.queue.put(task)

    fun getTask(): Task? = this.queue.take()

}
