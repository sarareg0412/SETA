package seta;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import ride.Ride;
import taxi.TaxiUtils;
import utils.Utils;
import unimi.dps.ride.Ride.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SetaUtils {
    private Map<Integer, List<Ride>> pendingRides;               // Map of pending rides by district

    private Map<Integer, Integer> nTaxiMap;                      // Maps that keeps the number of taxis available for each district
    private int qos;
    private MqttClient client;
    private MqttConnectOptions connOpts;

    private static SetaUtils instance;

    public SetaUtils() {
        this.pendingRides = new HashMap<>();
        //Map initialized with all values to 0 for each district
        this.nTaxiMap = new HashMap<Integer,Integer>(){{
            put(1,0);
            put(2,0);
            put(3,0);
            put(4,0);
        }};
    }

    //Singleton instance that returns the list of taxis in the system
    public synchronized static SetaUtils getInstance(){
        if(instance==null)
            instance = new SetaUtils();
        return instance;
    }

    public synchronized void updateNTaxiMap(int k, int v){
        nTaxiMap.put(k,v);
    }

    public synchronized Integer getNTaxiForDistrict(int k){
        return nTaxiMap.get(k);
    }

    /* Remove pending ride from list */
    public synchronized void removePendingRideFromMap(RideMsg rideMsg){
        Ride ride = new Ride(rideMsg);
        int distr = Utils.getDistrictFromPosition(ride.getStart());
        if (containsRide(distr, ride.getId())) {
            List<Ride> rideList = getPendingRidesFromDistrict(distr);
            pendingRides.put(distr, removeRideFromList(rideList,rideMsg.getId()));
            System.out.println("> RIDE ASSIGNED. Pending ride " + ride.getId() + " removed from list ");
        }
    }


    /* The ride is added to the pending list */
    public synchronized void addPendingRideToMap(Ride ride){
        int distr = Utils.getDistrictFromPosition(ride.getStart());

        if (!containsRide(distr, ride.getId())) {
            List<Ride> rideList = getPendingRidesFromDistrict(distr);

            if (rideList == null)
                rideList = new ArrayList<>();

            rideList.add(ride);
            pendingRides.put(distr, rideList);
        }
    }

    public synchronized List<Ride> removeRideFromList(List<Ride> list, String id){
        int index = 0;
        for (Ride r : list){
            if (r.getId().equals(id))
                index = list.indexOf(r);
        }

        list.remove(index);
        return list;
    }

    /* Returns true if a ride from a certain district was still pending */
    public boolean containsRide(int district, String id){
        List<Ride> districtPendingRides = getPendingRidesFromDistrict(district);
        if (districtPendingRides != null) {
            for (Ride r : districtPendingRides) {
                if (r.getId().equals(id))
                    return true;
            }
        }
        return false;
    }

    public void resetIsSentStatus(){
        for (int i=1; i<=4; i++){
            List<Ride> list = getPendingRidesFromDistrict(i);
            if (list != null) {
                list.forEach(ride -> ride.setSent(false));
            }
        }
    }

    public synchronized List<Ride> getPendingRidesFromDistrict(int district) {
        return pendingRides.get(district);
    }

    public int getQos() {
        return qos;
    }

    public void setQos(int qos) {
        this.qos = qos;
    }

    public MqttClient getClient() {
        return client;
    }

    public void setClient(MqttClient client) {
        this.client = client;
    }

    public MqttConnectOptions getConnOpts() {
        return connOpts;
    }

    public void setConnOpts(MqttConnectOptions connOpts) {
        this.connOpts = connOpts;
    }
}
