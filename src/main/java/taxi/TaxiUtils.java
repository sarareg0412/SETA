package taxi;

import exceptions.taxi.TaxiNotFoundException;
import org.eclipse.paho.client.mqttv3.MqttClient;
import simulator.MeasurementsBuffer;
import utils.Counter;
import utils.Position;
import utils.Queue;
import utils.Utils;

import java.util.ArrayList;
import java.util.List;

/* Class containing all shared infos between the taxi's threads*/
public class TaxiUtils {
    private TaxiInfo                    taxiInfo;                       // Taxi's info: id; port number and address
    private List<Taxi>                  taxisList;                          // List of other taxis
    private int                         batteryLevel;                       // Taxi's battery level
    private Position                    position;                           // Taxi's position

    private MqttClient                  MQTTClient;
    private int                         qos;

    private String                      currentRide;

    private boolean                     isAvailable;                        // Taxi is available to take the ride
    private Object                      availableLock;                      // Available's lock

    private boolean                     isCharging;                         // Taxi is currently recharging
    private boolean                     wantsToCharge;                      // Taxi wants to charge
    private final Object                wantsToChargeLock;                  // Wants to charge lock
    private Counter                     rechargeCounter;                    // Counter for # of taxi participating
    private long                        rechargeTimestamp;
    private Queue<TaxiInfo>             rechargeRequests;                   // Queue of recharging requests

    private Object                      inElectionLock;                      // Available's lock

    private boolean                     isMaster;                           // Taxi is selected to take the ride

    private boolean                     quit;                               //Taxi wants to leave the network

    private static TaxiUtils instance;

    public TaxiUtils() {
        this.taxisList = new ArrayList<>();
        this.position = new Position();
        this.rechargeRequests = new Queue<>();
        this.wantsToChargeLock = new Object();
        this.availableLock = new Object();
        this.inElectionLock = new Object();
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
        synchronized (availableLock) {
            return isAvailable;
        }
    }

    public synchronized void setAvailable(boolean available) {
        synchronized (availableLock) {
            isAvailable = available;
            if (isAvailable){
                // Notifies all that the taxi is now available
                availableLock.notifyAll();
            }
        }
    }

    public Object getAvailableLock() {
        return availableLock;
    }

    public synchronized boolean isCharging() {
        return isCharging;
    }

    public synchronized void setCharging(boolean charging) {
        isCharging = charging;
    }

    public boolean isInTheSameDistrict(Position position){
        return Utils.getDistrictFromPosition(getPosition()) == Utils.getDistrictFromPosition(position);
    }

    public boolean isMaster() {
        return isMaster;
    }

    public void setMaster(boolean master) {
        isMaster = master;
    }

    public boolean wantsToCharge() {
        synchronized (wantsToChargeLock){
            return wantsToCharge;
        }
    }

    public void setWantsToCharge(boolean wantsToCharge) {
        synchronized (wantsToChargeLock) {
            this.wantsToCharge = wantsToCharge;
            if (wantsToCharge){
                // Notifies the MainRechargeThread that the taxi wants to charge
                wantsToChargeLock.notify();
            }
        }
    }

    public Object getWantsToChargeLock() {
        return wantsToChargeLock;
    }

    public synchronized Counter getRechargeCounter() {
        return rechargeCounter;
    }

    public synchronized void setRechargeCounter(Counter rechargeCounter) {
        this.rechargeCounter = rechargeCounter;
    }

    public long getRechargeTimestamp() {
        return rechargeTimestamp;
    }

    public void setRechargeTimestamp(long rechargeTimestamp) {
        this.rechargeTimestamp = rechargeTimestamp;
    }

    public boolean quit() {
        return quit;
    }

    public void setQuit(boolean quit) {
        this.quit = quit;
    }

    public synchronized Queue<TaxiInfo> getRechargeRequests() {
        return rechargeRequests;
    }

    public synchronized void setRechargeRequests(Queue<TaxiInfo> rechargeRequests) {
        this.rechargeRequests = rechargeRequests;
    }

    public MqttClient getMQTTClient() {
        return MQTTClient;
    }

    public void setMQTTClient(MqttClient MQTTClient) {
        this.MQTTClient = MQTTClient;
    }

    public int getQos() {
        return qos;
    }

    public void setQos(int qos) {
        this.qos = qos;
    }

    public String getCurrentRide() {
        return currentRide;
    }

    public void setCurrentRide(String currentRide) {
        this.currentRide = currentRide;
    }

    @Override
    public String toString() {
        return "> Taxi " + getTaxiInfo().getId() + " status: "
                + "D" + Utils.getDistrictFromPosition(getPosition()) +"; "
                + getPosition() +"; "
                + getBatteryLevel() +"%;";
    }
}
