package admin;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import exceptions.taxi.TaxiAlreadyPresentException;
import taxi.TaxiInfo;
import taxi.TaxiNetwork;
import utils.Utils;

import javax.ws.rs.HttpMethod;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class AdministratorClient {
    private static Client client = Client.create();

    public static void main(String[] argv){
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            boolean check = true;
            int n = 0;
            while (check) {
                System.out.println("Select the service you want to ask for: \n");
                System.out.println("[1] Print the list of the taxis currently in the network \n");
                System.out.println("[2] Print the average statistics of a Taxi currently in the network\n");
                System.out.println("[3] Print the average statistics of all taxis occurred between two timestamps\n");
                try {
                    n = Integer.parseInt(inFromUser.readLine());
                    check = false;
                } catch (IOException e) {
                    System.out.print("Please insert a valid number.\n");
                    e.printStackTrace();
                }
            }

            startService(n);
        }
    }

    public static void startService(int n){
        switch (n){
            case 1:
                try {
                    printTaxiList();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    public static void printTaxiList() throws Exception {
        String path = Utils.taxiServiceAddress + Utils.taxiServicePath + "/getTaxiList";
        ClientResponse clientResponse = Utils.sendRequest(client, path, HttpMethod.GET);
        if (clientResponse == null){
            //TODO
        }

        int statusInfo = clientResponse.getStatus();

        TaxiNetwork taxiNetwork;
        if (ClientResponse.Status.OK.getStatusCode() == statusInfo) {
            taxiNetwork = clientResponse.getEntity(TaxiNetwork.class);
            if (taxiNetwork.getTaxiInfoList() != null){
                System.out.print("Taxis in the network:\n");
                for (TaxiInfo info : taxiNetwork.getTaxiInfoList()){
                    System.out.print(info + "\n");
                }
            }else
                System.out.print("There are no taxis in the network yet.\n");

        }else {
            throw new Exception("Status code: "+ statusInfo);
        }
    }
}
