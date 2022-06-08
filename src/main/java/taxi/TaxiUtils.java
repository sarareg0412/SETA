package taxi;

import exceptions.taxi.TaxiNotFoundException;
import utils.Position;
import utils.Queue;

import java.util.ArrayList;
import java.util.List;

/* Class containing all shared infos between the taxi's threads*/
public class TaxiUtils {

    private TaxiInfo taxiInfo;                          // Taxi's info: id; port number and address
    private List<Taxi> taxisList;                       // List of other taxis
    private int batteryLevel;                           // Taxi's battery level
    private Position position;                          // Taxi's position
    private Queue<Double> measurementAvgQueue;     // Queue of the measurement's averages

    private boolean isAvailable;                        // Taxi is available to take the ride
    private boolean isCharging;                         // Taxi is currently recharging
    private boolean isElected;                          // Taxi is elected and can take the rida

    private static TaxiUtils instance;

    public TaxiUtils() {
        this.taxisList = new ArrayList<>();
        this.position = new Position();
        measurementAvgQueue = new Queue<>();
    }

    //Singleton instance that returns the list of taxis in the system
    public synchronized static TaxiUtils getInstance(){
        if(instance==null)
            instance = new TaxiUtils();
        return instance;
    }

    public TaxiInfo getTaxiInfo() {
        return taxiInfo;
    }

    public void setTaxiInfo(TaxiInfo taxiInfo) {
        this.taxiInfo = taxiInfo;
    }

    public List<Taxi> getTaxisList() {
        return new ArrayList<>(taxisList);
    }

    public synchronized int getBatteryLevel() {
        return batteryLevel;
    }

    public synchronized void setBatteryLevel(int batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    public synchronized Position getPosition() {
        return position;
    }

    public synchronized void setPosition(Position position) {
        this.position = position;
    }

    public synchronized void addNewTaxiToList(Taxi taxi){
        taxisList.add(taxi);
    }

    public synchronized void removeTaxiFromList(String id) throws TaxiNotFoundException {
        int index = -1;

        for (Taxi t : getTaxisList()){
            if (t.getTaxiInfo().getId().equals(id))
                index = getTaxisList().indexOf(t);
        }

        if (index == -1)
            throw new TaxiNotFoundException();
        else
            taxisList.remove(index);
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean available) {
        isAvailable = available;
    }

    public boolean isCharging() {
        return isCharging;
    }

    public void setCharging(boolean charging) {
        isCharging = charging;
    }

    public synchronized boolean isElected() {
        return isElected;
    }

    public synchronized void setElected(boolean elected) {
        isElected = elected;
    }

    public void addAvgToQueue(double avg){
        measurementAvgQueue.put(avg);
    }

    public ArrayList<Double> getMeasurementAvgQueue() {
        return measurementAvgQueue.getAllAndEmptyQueue();
    }
}
