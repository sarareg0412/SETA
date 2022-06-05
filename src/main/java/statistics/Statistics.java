package statistics;

import exceptions.taxi.TaxiNotFoundException;

import javax.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Statistics {

    private HashMap<String, List<Stats>> statsHashMap;        //Hashmap of the statistics of each taxi

    private static Statistics instance;

    public Statistics() {
        this.statsHashMap = new HashMap<>();
    }

    public static synchronized Statistics getInstance() {
        if(instance==null)
            instance = new Statistics();
        return instance;
    }

    /* Adds statistcs of a given taxi to the current list. */
    public synchronized void addStatistics(Stats stats){
        String id = stats.getTaxiId();
        List<Stats> statsList = statsHashMap.get(id);

        if (stats == null)
            statsList = new ArrayList<Stats>();

        statsList.add(stats);
        statsHashMap.put(id,statsList);
    }

    /* Returns a list of associated statistics of a given taxi. The list
    * may be updated while the stats are being retrieved. */
    public List<Stats> getStatsBytaxiId(String id) throws TaxiNotFoundException {
        HashMap<String, List<Stats>> mapCopy = getStatsHashMap();     // This is synchronized
        if (mapCopy.get(id) == null)
            throw new TaxiNotFoundException();

        return mapCopy.get(id);
    }

    public synchronized HashMap<String, List<Stats>> getStatsHashMap() {
        return statsHashMap;
    }

}
