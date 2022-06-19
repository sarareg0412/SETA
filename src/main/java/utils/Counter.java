package utils;

public class Counter {

    private int maxElements;
    private int responses;
    private final Object lock;

    public Counter(int maxElements) {
        this.maxElements = maxElements;
        this.responses = 0;
        this.lock = new Object();
    }

    public void addResponse(){
        synchronized (lock){
            responses++;
            lock.notifyAll();
        }
    }

    public synchronized Object getLock() {
        return lock;
    }

    public synchronized int getMaxElements() {
        return maxElements;
    }

    public synchronized int getResponses() {
        synchronized (lock) {
            return responses;
        }
    }
}
