package utils;

public class Counter {

    private int maxElements;
    private int responses;
    private Object lock;

    public Counter(int maxElements) {
        this.maxElements = maxElements;
        this.responses = 0;
    }

    public synchronized void addResponse(){
//        synchronized (lock){
            responses++;
            notifyAll();
//        }
    }

    public synchronized Object getLock() {
        return lock;
    }

    public synchronized int getMaxElements() {
        return maxElements;
    }

    public synchronized int getResponses() {
        return responses;
    }

}
