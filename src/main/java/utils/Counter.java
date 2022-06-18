package utils;

public class Counter {
    private int maxElements;

    private int responses;
    private Object lock;

    public Counter(int maxElements) {
        this.maxElements = maxElements;
        this.responses = 0;
    }

    public void addResponse(){
        synchronized (lock){
            responses ++;
        }
    }
}
