package week1.queue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BlockingQueue {
    private static final int DEFAULT_SIZE = 1000;
    private final List<String> buffer = new ArrayList<>();
    private final Lock lock = new ReentrantLock();
    private final Condition notFull  = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();
    private final int capacity;
    private boolean closed = false;

    public BlockingQueue(int size) throws IllegalArgumentException {
        if (size > 0) {
            this.capacity = size;
        } else {
            throw new IllegalArgumentException("buffer size must be > 0");
        }
    }

    public BlockingQueue() {
        this.capacity = DEFAULT_SIZE;
    }

    public void put(String item) throws InterruptedException{
        if (item == null){
            throw new IllegalArgumentException("null cannot be added to queue");
        }
        lock.lock();
        try {
            while (buffer.size() == this.capacity && !closed) {
                notFull.await();
            }
            if (closed){
                throw new IllegalStateException("queue is closed");
            }
            buffer.add(item);
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    public String take() throws InterruptedException{
        lock.lock();
        try {
            while (buffer.isEmpty() && !closed){
                notEmpty.await();
            }
            if (!buffer.isEmpty()){
                String item = buffer.remove(0);
                notFull.signal();
                return item;

            } else {
                return null;
            }
        } finally {
            lock.unlock();
        }
    }

    public void close() {
        lock.lock();
        try {
            if (closed){
                return;
            }
            closed = true;
            notFull.signalAll();
            notEmpty.signalAll();
        } finally {
            lock.unlock();
        }
    }
}
