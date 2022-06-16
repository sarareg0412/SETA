package seta.modules;

import com.google.protobuf.TextFormat;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import seta.SetaUtils;
import unimi.dps.ride.Ride;
import utils.Position;
import utils.Utils;

import java.util.List;
import java.util.Random;

public class PublishRideThread extends Thread{
    SetaUtils setaUtils = SetaUtils.getInstance();

    @Override
    public void run() {
        try {
            publishRide();
        } catch (MqttException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void publishRide() throws MqttException, InterruptedException {
        Ride.RideMsg rideMsg;
        /* If there is a taxi available, publishes again a pending ride, otherwise
         * create a new ride from scratch */
        ride.Ride r = null;
        for(int i=1; i<=4; i++){
            if (setaUtils.getPendingRidesFromDistrict(i) != null){
                // There is at least a taxi available and a pending ride in that district
                if (setaUtils.getNTaxiForDistrict(i) > 0 && setaUtils.getPendingRidesFromDistrict(i).size()>0){
                    //Gets the first pending ride
                    r = getFirstPendingRideFromDistrict(i);
                    break;
                }
            }
        }
        /* No taxi available or pending rides were found, create new ride*/
        if (r == null){
            // Create the rides with a positive random id
            r = createNewRide(String.valueOf(Math.abs(new Random().nextInt())));
            setaUtils.addPendingRideToMap(r);
            System.out.println("> PUBLISHING NEW RIDE:" + r.getId());
        }else{
            System.out.println("> PUBLISHING PENDING RIDE:" + r.getId());
        }

        rideMsg = r.createRideMsg();

        MqttMessage msg = new MqttMessage(rideMsg.toByteArray());
        msg.setQos(setaUtils.getQos());
        setaUtils.getClient().publish(Utils.getDistrictTopicFromPosition(new Position(rideMsg.getStart())), msg);
        System.out.println("> RideMsg published:" + TextFormat.shortDebugString(rideMsg));

        //TODO client disconnect
    }

    /* Create a new ride */
    private ride.Ride createNewRide(String id){
        Position start = Utils.getRandomPosition();
        Position finish;
        do {
            finish = Utils.getRandomPosition();
        }while (finish.equals(start));

        // Sets the message with the ride infos to be sent to the taxi network
        return new ride.Ride(id, start, finish);
    }


    public synchronized ride.Ride getFirstPendingRideFromDistrict(int distr) throws InterruptedException {
        List<ride.Ride> list = setaUtils.getPendingRidesFromDistrict(distr);
        while (list.size() == 0){
            wait();
        }

        ride.Ride r = list.get(0);
        notifyAll();
        return r;
    }
}
