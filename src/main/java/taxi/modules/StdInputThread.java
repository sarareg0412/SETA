package taxi.modules;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import exceptions.taxi.TaxiNotFoundException;
import taxi.TaxiInfo;
import utils.Utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class StdInputThread extends Thread {
    TaxiInfo taxiInfo;

    public StdInputThread(TaxiInfo taxiInfo) {
        this.taxiInfo = taxiInfo;
    }

    @Override
    public void run() {
        stopTaxi();
    }

    private void stopTaxi(){
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
        boolean check = false;

        while(!check) {
            System.out.print("Write \"quit\" to terminate Taxi.\n");

            try {
                check = inFromUser.readLine().equalsIgnoreCase("quit");
                if (check )
                    deleteTaxi();
            }catch (Exception e){
                System.out.println("An error occurred.\n");
            }
        }
        System.exit(0);
    }

    private void deleteTaxi() throws Exception {
        String path = Utils.taxiServiceAddress + "taxis" + "/delete/" + taxiInfo.getId();
        ClientResponse clientResponse = Utils.sendDELETERequest(Client.create(), path);
        System.out.print("ok " + clientResponse +"\n");
        if (clientResponse == null){
            //TODO
        }
        int statusInfo = clientResponse.getStatus();

        if (ClientResponse.Status.OK.getStatusCode() == statusInfo) {
            //Taxi correctly deleted
            System.out.print("Taxi correctly deleted.\n");
        } else if (ClientResponse.Status.CONFLICT.getStatusCode() == statusInfo) {
            //Taxi already added
            throw new TaxiNotFoundException();
        }else {
            throw new Exception("Status code: "+ statusInfo);
        }
    }
}
