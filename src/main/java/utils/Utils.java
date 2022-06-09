package utils;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import taxi.Taxi;
import unimi.dps.ride.RideOuterClass;

import javax.ws.rs.HttpMethod;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class Utils {
    public static final String servicesAddress = "http://localhost:1337/";
    public static final String taxiServicePath = "taxis";
    public static final String statsServicePath = "stats";

    public static final String MQTTBrokerAddress = "tcp://localhost:1883";
    public static String[] topics = new String[]{"seta/smartcity/rides/district1",
                                                 "seta/smartcity/rides/district2",
                                                 "seta/smartcity/rides/district3",
                                                 "seta/smartcity/rides/district4"};
    public static final int cellsNumber = 10;
    public static final int MAX_BATTERY = 100;
    public static final int SLIDING_WINDOWS_BUFFER_LENGTH = 8;
    public static final double OVERLAP = 0.5;
    public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.00");

    public static Position[] rechargeStations = {  new Position(0,0),
                                                new Position(0,9),
                                                new Position(9,0),
                                                new Position(9,9)};

    /* Given a client and url, send a GET request to that url */
    public static ClientResponse getRequest(Client client, String url){
        WebResource webResource = client.resource(url);
        try {
            return webResource.type("application/json").get(ClientResponse.class);
        } catch (ClientHandlerException e) {
            System.out.println("Server non disponibile");
            return null;
        }
    }

    public static Position getRandomStartingPosition(){
        return rechargeStations[(int) (Math.random() * (rechargeStations.length))];
    }

    public static Position getRandomPosition(){
        return new Position((int) (Math.random()*10 ), (int) (Math.random()*10 ));
    }

    public static Position getPositionFromPositionMsg(RideOuterClass.Ride.PositionMsg positionMsg){
        return new Position(positionMsg.getX(),positionMsg.getY());
    }

    public static String getDistrictTopicFromPosition(Position p){
        return topics[getDistrictFromPosition(p)-1];
    }

    public static int getDistrictFromPosition(Position p){
        int half = cellsNumber/2;
        int district = 1;   //default value set to 1
        if (p.getX() < half && p.getY() < half)
            district = 1;
        if (p.getX() < half && p.getY() >= half)
            district = 2;
        if (p.getX() >= half && p.getY() >= half)
            district = 3;
        if (p.getX() >= half && p.getY() < half)
            district = 4;
        return district;
    }

    public static double getDistanceBetweenPositions(Position p1, Position p2){
        return Math.sqrt((Math.pow( p1.getX() + p2.getX(),2)) + (Math.pow( p1.getY() + p2.getY(),2)));
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
            System.out.println("Service unavailable");
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

}
