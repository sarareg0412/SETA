package utils;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

public class Utils {
    public static final String taxiServiceAddress = "http://localhost:1337/";
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

    public static int getDistrictFromPosition(Position p){
        int half = cellsNumber/2;
        if (p.getX() < half && p.getY() < half)
            return 1;
        if (p.getX() < half && p.getY() >= half)
            return 2;
        if (p.getX() >= half && p.getY() < half)
            return 3;
        if (p.getX() >= half && p.getY() >= half)
            return 4;

        return 0;
    }
}
