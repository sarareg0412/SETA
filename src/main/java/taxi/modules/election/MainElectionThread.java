package taxi.modules.election;

import com.google.protobuf.TextFormat;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import ride.Ride;
import taxi.Taxi;
import taxi.TaxiUtils;
import taxi.modules.SendOKThread;
import unimi.dps.ride.Ride.*;
import unimi.dps.taxi.TaxiRPCServiceOuterClass.*;
import utils.Counter;
import utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainElectionThread extends Thread{
    TaxiUtils taxiUtils;
    RideMsg rideMsg;
    Counter electionCounter;

    public MainElectionThread(RideMsg rideMsg) {
        this.taxiUtils = TaxiUtils.getInstance();
        this.rideMsg = rideMsg;
    }

    @Override
    public void run() {
        System.out.println("> [ELEC] NEW RIDE REQUEST " + TextFormat.shortDebugString(rideMsg));
        taxiUtils.setInElection(true);
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
        System.out.println("> START Counter: max " + electionCounter.getMaxElements() + " resp:" + electionCounter.getResponses());
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
        System.out.println("> END Counter: max " + electionCounter.getMaxElements() + " resp:" + electionCounter.getResponses());
//        try {
//            waitAllOk();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

        System.out.println("> [ELEC] Election finished");

        if(electionCounter.getResponses() >= electionCounter.getMaxElements() )
            taxiUtils.setMaster(true);


        if (taxiUtils.isMaster()) {
            // The current taxi was elected by the others to take the ride
            taxiUtils.setAvailable(false);
            // Current taxi has to free the others
            System.out.println("> [ELEC] Taxi " + taxiUtils.getTaxiInfo().getId() + " is taking the ride " + rideMsg.getId());
//            others.removeIf(taxi -> taxi.getTaxiInfo().getId().equals(taxiUtils.getTaxiInfo().getId()));
//            if (others.size() > 0) {
//                System.out.println("> [ELEC] Notifies the others that the ride has been taken.");
//                FinishElectionMsg msg = FinishElectionMsg.newBuilder()
//                        .setOk("OK")
//                        .setId(TaxiUtils.getInstance().getTaxiInfo().getId())
//                        .setRideId(rideMsg.getId())
//                        .build();
//                for (Taxi other : others) {
//                        SendOKThread thread = new SendOKThread(other.getTaxiInfo(), Utils.ELECTION, msg);
//                        thread.start();
//                }
//                //Join ?
//            }
            try {
                publishTakenRide(rideMsg);
            } catch (MqttException e) {
                System.out.println("> An error occurred while taking the ride. ");
            }
        }
    }
//
//    public void waitAllOk() throws InterruptedException {
//        synchronized (electionCounter.getLock()) {
//            while (electionCounter.getResponses() < electionCounter.getMaxElements()) {
//                electionCounter.getLock().wait();
//            }
//        }
//    }

    /* Sends back an mqtt message to SETA that the ride was taken */
    public void publishTakenRide(RideMsg ride) throws MqttException {
        MqttMessage msg = new MqttMessage(ride.toByteArray());
        msg.setQos(taxiUtils.getQos());
        taxiUtils.getMQTTClient().publish(Utils.TAKEN_RIDE, msg);
    }

}
