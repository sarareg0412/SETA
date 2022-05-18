package taxi;

import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.WebResource;
import exceptions.taxi.TaxiAlreadyPresentException;
import utils.Position;
import utils.Utils;

import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.BufferedReader;
import java.io.InputStreamReader;

@XmlRootElement
public class Taxi {
    public static TaxiInfo taxiInfo;               // Taxi's info: id; port number and address
    public static int batteryLevel;                // Taxi's battery level
    public static Position position = new Position();     // Taxi's current position in Cartesian coordinates
    private static String taxiServicePath;
    private static Client client;


    public static void main(String argv[]){
        initComponents();
        while (true){
            initTaxi();

            try {
                // The taxi requests to join the network
                TaxiNetwork taxiNetwork = insertTaxi(taxiInfo);
                System.out.print("Taxi added with position: " + taxiNetwork.getPosition() + "\n");
                position = taxiNetwork.getPosition();
                for (TaxiInfo taxiInfo : taxiNetwork.getTaxiInfoList())
                    System.out.print("Taxis present : " + taxiInfo.getId() + "\n");
            } catch (TaxiAlreadyPresentException e) {
                System.out.print(e.getMessage() + "\n");
            } catch (Exception e) {
                System.out.print(e.getMessage() + "\n");
            }
        }
    }

    public static void initComponents(){
        taxiServicePath = "taxis";
        taxiInfo = new TaxiInfo();
        client = Client.create();
    }

    public static void initTaxi(){

        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
        boolean check = true;

        while(check) {
            System.out.print("Insert Taxi ID: \n");

            try {
                taxiInfo.setId(inFromUser.readLine());
                check = false;
            }catch (Exception e){
                System.out.println("An error occurred. Please insert a value\n");
            }
        }

        check = true;

        while(check) {
            System.out.print("Insert port number: \n");

            try {
                taxiInfo.setPort(Integer.parseInt(inFromUser.readLine()));
                check = false;
            }catch (Exception e){
                System.out.println("Not a number. Please insert Integer Value\n");
            }
        }

        check = true;

        while(check) {
            System.out.print("Insert address: \n");

            try {
                taxiInfo.setAddress(inFromUser.readLine());
                check = false;
            }catch (Exception e){
                System.out.println("An error occurred. Please insert a value\n");
            }
        }
    }

    /* A new taxi requested to enter the smart city */
    public static TaxiNetwork insertTaxi(TaxiInfo taxiInfo) throws Exception {
        String path = Utils.taxiServiceAddress + taxiServicePath + "/add";
        ClientResponse clientResponse = sendPOSTRequest(client, path, taxiInfo);
        System.out.print("ok " + clientResponse +"\n");
        if (clientResponse == null){
            //TODO
        }

        TaxiNetwork taxiNetworkResponse = null;

        int statusInfo = clientResponse.getStatus();

        if (Status.OK.getStatusCode() == statusInfo) {
            //Taxi correctly added
            taxiNetworkResponse = clientResponse.getEntity(TaxiNetwork.class);
        } else if (Status.CONFLICT.getStatusCode() == statusInfo) {
            //Taxi already added
            throw new TaxiAlreadyPresentException();
        }else {
            throw new Exception("Status code: "+ statusInfo);
        }

        return taxiNetworkResponse;
    }

    /* Given a client, url and object, send a POST request with that object as parameter*/
    public static ClientResponse sendPOSTRequest(Client client, String url, TaxiInfo t){
        WebResource webResource = client.resource(url);
        String input = new Gson().toJson(t);
        try {
            return webResource.type("application/json").post(ClientResponse.class, input);
        } catch (ClientHandlerException e) {
            System.out.println("Service unavailable");
            return null;
        }
    }
}
