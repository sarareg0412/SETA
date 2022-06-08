package taxi.modules;

import statistics.Stats;
import taxi.Taxi;
import taxi.TaxiInfo;
import taxi.TaxiNetwork;

import java.util.ArrayList;

public class ElectionThread extends Thread{
    public TaxiNetwork electionNetwork;

    public ElectionThread(){
        electionNetwork = new TaxiNetwork();
    }

    @Override
    public void run() {
        super.run();
    }
}
