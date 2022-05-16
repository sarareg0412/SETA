package utils;

import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import javafx.geometry.Pos;

import java.awt.*;

public class Utils {
    public static final String taxiServiceAddress = "http://localhost:1337/";
    public static Position[] rechargeStations = {  new Position(0,0),
                                                new Position(0,9),
                                                new Position(9,0),
                                                new Position(9,9)};

    /* Given a client, url and object, send a POST request with that object as parameter*/
    public static <T> ClientResponse postRequest(Client client, String url, T t){
        WebResource webResource = client.resource(url);
        String input = new Gson().toJson(t);
        try {
            return webResource.type("application/json").post(ClientResponse.class, input);
        } catch (ClientHandlerException e) {
            System.out.println("Server non disponibile");
            return null;
        }
    }

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

    public static Position getRandomStartPosition(){
        return rechargeStations[(int) (Math.random()*5)];
    }

}
