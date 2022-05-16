package seta;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import ride.Ride;
import utils.Position;
import utils.Utils;

/* Main process that simulates the taxi service requests generated by the
citizens of the smart city.*/
public class Seta {
    public static MqttClient client;
    public static MqttConnectOptions connOpts;
    public static String[] topics;
    public static int qos;

    public static void main (String argv[]){
        try {
            initializeComponents();
        }catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public static void initializeComponents() throws MqttException {
        topics = new String[]{"seta/smartcity/rides/district1",
                              "seta/smartcity/rides/district2",
                              "seta/smartcity/rides/district3",
                              "seta/smartcity/rides/district4"};
        client = new MqttClient("tcp://localhost:1883", MqttClient.generateClientId());
        connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);         //session will be persistent
        qos = 2;      //quality of service set to 2
    }

    public static Ride createNewRide(String id){
        Position start = Utils.getRandomPosition();
        Position finish;
        do {
            finish = Utils.getRandomPosition();
        }while (finish.equals(start));

        return new Ride(id, start, finish);
    }
}
