package utils;

import com.google.protobuf.Empty;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import taxi.Taxi;
import unimi.dps.ride.Ride;

import javax.ws.rs.HttpMethod;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/* Class holding the project's utils */
public class Utils {
    public static final String              SERVICES_ADDRESS = "http://localhost:1337/";
    public static final String              TAXI_SERVICE_PATH = "taxis";
    public static final String              STATS_SERVICE_PATH = "stats";

    public static final String              MQTT_BROKER_ADDRESS = "tcp://localhost:1883";

    public static final String              SETA_AVAILABLE = "seta/smartcity/seta/available/";
    public static final String              TAKEN_RIDE = "seta/smartcity/rides/taken";
    public static final String              TAXI_AVAILABLE = "seta/smartcity/taxi/available/";
    public static final String              TAXI_UNAVAILABLE = "seta/smartcity/taxi/unavailable/";
    public static String[]                  DISTRICT_TOPICS = new String[]{"seta/smartcity/rides/district1",
                                                                         "seta/smartcity/rides/district2",
                                                                         "seta/smartcity/rides/district3",
                                                                         "seta/smartcity/rides/district4"};
    public static final int                 CELLS_NUMBER = 10;
    public static final int                 MAX_BATTERY_LEVEL = 100;
    public static final int                 MIN_BATTERY_LEVEL   = 30;

    public static final int                 SLIDING_WINDOWS_BUFFER_LENGTH = 8;
    public static final double              OVERLAP = 0.5;

    public static Position[]                RECHARGE_STATIONS = {    new Position(0,0),
                                                                    new Position(0,9),
                                                                    new Position(9,9),
                                                                    new Position(9,0)
                                                            };

    public static final DecimalFormat       DECIMAL_FORMAT = new DecimalFormat("0.00");

    public static Position getRandomStartingPosition(){
        return RECHARGE_STATIONS[(int) (Math.random() * (RECHARGE_STATIONS.length))];
    }

    public static Position getRandomPosition(){
        return new Position((int) (Math.random()*10 ), (int) (Math.random()*10 ));
    }

    public static Position getPositionFromPositionMsg(Ride.RideMsg.PositionMsg positionMsg){
        return new Position(positionMsg.getX(),positionMsg.getY());
    }

    public static String getDistrictTopicFromPosition(Position p){
        return DISTRICT_TOPICS[getDistrictFromPosition(p)-1];
    }

    public static int getDistrictFromPosition(Position p){
        int half = CELLS_NUMBER /2;
        int district = 1;   //default value set to 1
        if (p.getX() < half && p.getY() < half)
            district = 1;
        else if (p.getX() < half && p.getY() >= half)
            district = 2;
        else if (p.getX() >= half && p.getY() >= half)
            district = 3;
        else if (p.getX() >= half && p.getY() < half)
            district = 4;
        return district;
    }

    public static double getDistanceBetweenPositions(Position p1, Position p2){
        return Math.sqrt((Math.pow( p1.getX() - p2.getX(),2)) + (Math.pow( p1.getY() - p2.getY(),2)));
    }

    /* Given a client, url and object, send a DELETE request with that object as parameter*/
    public static ClientResponse sendRequest(Client client, String url, String request){
        WebResource webResource = client.resource(url);
        try {

            switch (request){
                case HttpMethod.DELETE:
                    return webResource.type("application/json").delete(ClientResponse.class);
                case HttpMethod.GET:
                    return webResource.type("application/json").get(ClientResponse.class);
                default:
                    break;
            }
        } catch (ClientHandlerException e) {
            System.out.println("Service unavailable. Please try again.");
            return null;
        }

        return null;
    }

    public static String printTaxiList(List<Taxi> taxis){
        StringBuilder s = new StringBuilder();
        for (Taxi t : taxis){
            s.append(t.getTaxiInfo().getId() + " ");
        }

        return s.toString();
    }

    public static boolean isTimestampValid(String s){
        SimpleDateFormat format = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
        try{
            Date date = format.parse(s);
            return true;
        }catch(ParseException e) {
            return false;
        }
    }

    public static boolean moreThanEqual(String t1, String t2){
        return t1.compareToIgnoreCase(t2) <= 0;
    }

    public static Position getNearestStationPosition(Position p){
        return RECHARGE_STATIONS[getDistrictFromPosition(p) - 1];
    }

    public static double getDistanceFromRechargeStation(Position p){
        return getDistanceBetweenPositions(p, getNearestStationPosition(p));
    }

    /* Notifies SETA that taxi is not available anymore in that district */
    public static void publishUnavailable(Position p, MqttClient client, int qos) throws MqttException {
        MqttMessage msg = new MqttMessage(Empty.newBuilder().build().toByteArray());
        msg.setQos(qos);
        client.publish(Utils.TAXI_UNAVAILABLE + Utils.getDistrictFromPosition(p), msg);
    }

    /* Notifies SETA that taxi is available in a new district */
    public static void publishAvailable(MqttClient client, int qos, Position position) throws MqttException {
        MqttMessage msg = new MqttMessage(Empty.newBuilder().build().toByteArray());
        msg.setQos(qos);
        client.publish(Utils.TAXI_AVAILABLE + Utils.getDistrictFromPosition(position), msg);
    }

    public static void unsubscribeFromTopic(MqttClient client, String topic) throws MqttException {
        client.unsubscribe(topic);
        //System.out.println("> Taxi unsubscribed from topic : " + topic);
    }

    public static void subscribeToTopic(MqttClient client, int qos, String topic) throws MqttException {
        client.subscribe(topic, qos);
        //System.out.println("> Taxi subscribed to topic : " + topic);
    }

}
