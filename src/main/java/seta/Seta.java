package seta;

import unimi.dps.ride.RideOuterClass.Ride;
import unimi.dps.ride.RideOuterClass.Ride.*;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import utils.Position;
import utils.Utils;

import java.util.Random;

/* Main process that simulates the taxi service requests generated by the
citizens of the smart city. */
public class Seta {
    public static MqttClient client;
    public static MqttConnectOptions connOpts;
    public static int qos;

    public static void main (String argv[]){
        try {
            initializeComponents();
            //The client is now connected to the broker
            client.connect(connOpts);
            while (true){
                //Two new rides are published every 5 seconds
                publishNewRide();
                publishNewRide();
                Thread.sleep(10000);
            }
        }catch (MqttException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void initializeComponents() throws MqttException {

        client = new MqttClient(Utils.MQTTBrokerAddress, MqttClient.generateClientId());
        connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);         //session will be persistent
        qos = 2;      //quality of service set to 2
    }

    /*Pass the */
    public static Ride createNewRide(String id){
        Position start = Utils.getRandomPosition();
        Position finish;
        do {
            finish = Utils.getRandomPosition();
        }while (finish.equals(start));

        //Sets the message with the ride infos to be sent to the taxi network
        Ride ride = Ride.newBuilder().setId(id)
                .setStart(PositionMsg.newBuilder().setX(start.getX())
                        .setY(start.getY()).build())
                .setFinish(PositionMsg.newBuilder().setX(finish.getX())
                        .setY(finish.getY()).build())
                .build();

        return ride;
    }

    public static void publishNewRide() throws MqttException {
        //Seta starts creating the rides with a positive random id
        Ride ride = createNewRide(String.valueOf(Math.abs(new Random().nextInt())));
        MqttMessage msg = new MqttMessage(ride.toByteArray());
        msg.setQos(qos);
        System.out.print("Ride published:" + ride);
        client.publish(Utils.getDistrictTopicFromPosition(new Position(ride.getStart().getX(), ride.getStart().getY())), msg);
    }

}
