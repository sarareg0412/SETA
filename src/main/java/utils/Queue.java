package utils;

import java.util.ArrayList;

/* General class that implements a queue with syncronized access to the elements */
public class Queue <T>{
    public ArrayList<T> buffer = new ArrayList<T>();

    /* The taxi puts the stats in a controlled way */
    public synchronized void put(T t) {
        buffer.add(t);
    }

    public synchronized ArrayList<T> getAllAndEmptyQueue() {
        ArrayList<T> queueCopy = new ArrayList<T>(buffer);
        buffer.clear();
        return queueCopy;
    }
}
