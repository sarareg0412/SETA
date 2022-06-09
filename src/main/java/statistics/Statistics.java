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

        if (statsList == null)
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

    /* Returns a list of n statistics of a given taxi. */
    public List<Stats> getLastNTaxiStats(String id, int n) throws TaxiNotFoundException {
        List<Stats> listCopy = getStatsHashMap().get(id);     // This is synchronized
        if (listCopy == null)
            throw new TaxiNotFoundException();

        int size = listCopy.size();

        if (n >= size){
            n = size;   // returns all stats
        }
        // List ordered from last to first
        listCopy.sort((o1, o2) -> o2.getTimestamp().compareToIgnoreCase(o1.getTimestamp()));
        return new ArrayList<>(listCopy.subList( 0, n));
    }

    public List<Stats> getStatsBwTimestamps(String t1, String t2){
        t1 = t1.replace("_", " ");
        t2 = t2.replace("_", " ");
        HashMap<String, List<Stats>> mapCopy = getStatsHashMap();     // This is synchronized
        List<Stats> list = new ArrayList<>();
        // Scan all sets
        for (String id : mapCopy.keySet()){
            // Scan all lists for the taxi
            for (Stats s : mapCopy.get(id)){
                if (s.getTimestamp().compareToIgnoreCase(t1) >= 0 &&
                        s.getTimestamp().compareToIgnoreCase(t2) <= 0)
                    list.add(s);
            }
        }

        return list;
    }

    public synchronized HashMap<String, List<Stats>> getStatsHashMap() {
        return statsHashMap;
    }

}
