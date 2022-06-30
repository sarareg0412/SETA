package taxi.modules.election;

import com.google.protobuf.TextFormat;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import ride.Ride;
import statistics.Stats;
import taxi.Taxi;
import taxi.TaxiUtils;
import unimi.dps.ride.Ride.*;
import unimi.dps.taxi.TaxiRPCServiceOuterClass.*;
import utils.Counter;
import utils.Position;
import utils.Queue;
import utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class MainElectionThread extends Thread{
    TaxiUtils taxiUtils;
    RideMsg rideMsg;
    Counter electionCounter;
    Queue<Stats> statsQueue;

    public MainElectionThread(RideMsg rideMsg, Queue<Stats> statsQueue) {
        this.taxiUtils = TaxiUtils.getInstance();
        this.rideMsg = rideMsg;
        this.statsQueue = statsQueue;
    }

    @Override
    public void run() {
        System.out.println("> [ELEC] NEW RIDE REQUEST " + TextFormat.shortDebugString(rideMsg));
        try {
            startElection(rideMsg);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void startElection(RideMsg rideMsg) throws MqttException {
        //The message to send is created only once
        ElectionMsg electionMessage = ElectionMsg.newBuilder()
                                                .setId(taxiUtils.getTaxiInfo().getId())
                                                .setDistance(Utils.getDistanceBetweenPositions(taxiUtils.getPosition(),
                                                        Utils.getPositionFromPositionMsg(rideMsg.getStart())))
                                                .setBattery(taxiUtils.getBatteryLevel())
                                                .setRide(rideMsg)
                                                .build();

        List<Taxi> others = new ArrayList<>(taxiUtils.getTaxisList());
        //Election counter set to current # of taxis in the network
        electionCounter = new Counter(others.size());

        ArrayList<Thread> threads = new ArrayList<>();

        for (Taxi otherTaxi : others) {
            // A new thread is created for the taxi to broadcasts the others and
            // itself to see to pick the master to take the ride
            ElectionThread electionThread = new ElectionThread(otherTaxi.getTaxiInfo(), electionMessage, electionCounter);
            threads.add(electionThread);
        }
        for (Thread t : threads){
            t.start();
        }
        /* Waits for all responses from threads */
        for (Thread t : threads){
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("counter ride "+ rideMsg.getId() + " " );
        if(electionCounter.getResponses() >= electionCounter.getMaxElements()){
            // The current taxi was elected by the others to take the ride
            System.out.println("> [ELEC] Taxi is taking the ride ...");
            taxiUtils.setCurrentRide(rideMsg.getId());
            taxiUtils.setAvailable(false);
            try {
                publishTakenRide(rideMsg);
                takeRide(new Ride(rideMsg));
            } catch (MqttException e) {
                System.out.println("> An error occurred while taking the ride. ");
            }
        }else {
            System.out.println("> [ELEC] Ride already taken.");

        }
    }

    /* Sends back an mqtt message to SETA that the ride was taken */
    public void publishTakenRide(RideMsg ride) throws MqttException {
        MqttMessage msg = new MqttMessage(ride.toByteArray());
        msg.setQos(taxiUtils.getQos());
        taxiUtils.getMQTTClient().publish(Utils.TAKEN_RIDE, msg);
    }

    public void takeRide(Ride ride) throws MqttException {

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Position oldPosition = taxiUtils.getPosition();
        // Taxi completed the ride; changes position
        taxiUtils.setPosition(ride.getFinish());

        //Taxi subscribes to new distric topic if necessary
        if (Utils.getDistrictFromPosition(oldPosition) != Utils.getDistrictFromPosition(ride.getFinish())) {
            Utils.unsubscribe(taxiUtils.getMQTTClient(), Utils.getDistrictTopicFromPosition(oldPosition));
            Utils.publishUnavailable(oldPosition,taxiUtils.getMQTTClient(), taxiUtils.getQos());
            Utils.subscribeToTopic(taxiUtils.getMQTTClient(), taxiUtils.getQos(), Utils.getDistrictTopicFromPosition(taxiUtils.getPosition()));
            Utils.publishAvailable(taxiUtils.getMQTTClient(), taxiUtils.getQos(), taxiUtils.getPosition());
        }
        // Taxi battery level decreases
        taxiUtils.setBatteryLevel(taxiUtils.getBatteryLevel() - (int) Math.floor(ride.getKmToTravel(oldPosition)));
        taxiUtils.setMaster(false);
        taxiUtils.setCurrentRide("");
        System.out.println("> [ELEC] RIDE COMPLETED " + ride.getId());
        // Adds the stats of the current ride to the queue of stats to be sent to the Admin Server
        addStatsToQueue(ride.getKmToTravel(oldPosition));
        if (taxiUtils.getBatteryLevel() < Utils.MIN_BATTERY_LEVEL){
            System.out.println("> [REC] Taxi needs to charge!");
            taxiUtils.setWantsToCharge(true);
        }else {
            taxiUtils.setAvailable(true);
        }
    }
    private void addStatsToQueue(double km){
        Stats stats = new Stats();
        stats.setTaxiId(taxiUtils.getTaxiInfo().getId());
        stats.setKmDriven(km);
        statsQueue.put(stats);
    }
}
