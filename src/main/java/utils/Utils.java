package utils;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import unimi.dps.ride.RideOuterClass;

public class Utils {
    public static final String taxiServiceAddress = "http://localhost:1337/";
    public static final String MQTTBrokerAddress = "tcp://localhost:1883";
    public static String[] topics = new String[]{"seta/smartcity/rides/district1",
                                                 "seta/smartcity/rides/district2",
                                                 "seta/smartcity/rides/district3",
                                                 "seta/smartcity/rides/district4"};
    public static final int cellsNumber = 10;

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

    public static String getDistrictTopicFromPosition(int x, int y){
        return topics[getDistrictFromPosition(x, y)-1];
    }

    public static int getDistrictFromPosition(int x, int y){
        int half = cellsNumber/2;
        int district = 1;   //default value set to 1
        if (x < half && y < half)
            district = 1;
        if (x < half && y >= half)
            district = 2;
        if (x >= half && y >= half)
            district = 3;
        if (x >= half && y < half)
            district = 4;
        return district;
    }

    /* Given a client, url and object, send a DELETE request with that object as parameter*/
    public static ClientResponse sendDELETERequest(Client client, String url){
        WebResource webResource = client.resource(url);
        try {
            return webResource.type("application/json").delete(ClientResponse.class);
        } catch (ClientHandlerException e) {
            System.out.println("Service unavailable");
            return null;
        }
    }
}
