package utils;

/* Class used as a counter for the election and the recharge processes */
public class Counter {

    private int                         maxResponses;               // Max responses possibles
    private int                         responses;                  // Current number of responses gotten
    private final Object                lock;                       // Responses' lock

    public Counter(int maxElements) {
        this.maxResponses = maxElements;
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

    public synchronized int getMaxResponses() {
        return maxResponses;
    }

    public synchronized int getResponses() {
        synchronized (lock) {
            return responses;
        }
    }

}
