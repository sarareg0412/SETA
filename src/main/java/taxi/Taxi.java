package taxi;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import utils.Position;
import utils.Utils;

import javax.xml.bind.annotation.XmlRootElement;
import java.awt.*;
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
        initTaxi();
        // The taxi requests to join the network
        TaxiNetwork taxiNetwork = insertTaxi(taxiInfo);
        System.out.print("Taxi added with position: " + taxiNetwork.getPosition() + "\n");
        position = taxiNetwork.getPosition();
        for (TaxiInfo taxiInfo : taxiNetwork.getTaxiInfoList())
            System.out.print("Taxis present : " + taxiInfo.getId() + "\n");
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
    public static TaxiNetwork insertTaxi(TaxiInfo taxiInfo){
        String path = Utils.taxiServiceAddress + taxiServicePath + "/add";
        ClientResponse clientResponse = Utils.postRequest(client, path, taxiInfo);
        System.out.print("ok " + clientResponse +"\n");
        //TODO: gestire Client Response diverse da 200
        if (clientResponse == null){
            //TODO
        }
        return clientResponse.getEntity(TaxiNetwork.class);
    }
}
