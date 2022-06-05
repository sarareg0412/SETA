package taxi.modules;

import statistics.Stats;
import statistics.StatsQueue;
import taxi.Taxi;

import java.sql.Timestamp;

public class SendStatsThread extends Thread{
    private Taxi taxi;
    private StatsQueue statsQueue;

    public SendStatsThread(Taxi taxi) {
        this.taxi = taxi;
        statsQueue = new StatsQueue();
    }

    @Override
    public void run() {
        while (true){
            try {
                Thread.sleep(15000);        //Statistics are sent every 15 seconds
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public Stats computeStats(){
        Stats stats = new Stats();
        stats.setBattery(taxi.getBatteryLevel());
        stats.setTimestamp(new Timestamp(System.currentTimeMillis()));

        return stats;
    }
}
