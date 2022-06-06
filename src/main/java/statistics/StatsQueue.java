package statistics;

import java.util.ArrayList;

/* Queue that contains the local statistics computed by the taxi. */
public class StatsQueue {
    public ArrayList<Stats> statsBuffer = new ArrayList<Stats>();

    /* The taxi puts the stats in a controlled way */
    public synchronized void put(Stats stats) {
        statsBuffer.add(stats);
    }

    public synchronized ArrayList<Stats> getAllAndEmptyQueue() {
        ArrayList<Stats> statsCopy = new ArrayList<Stats>(statsBuffer);
        statsBuffer.clear();
        return statsCopy;
    }
}
