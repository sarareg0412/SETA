package admin;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import exceptions.taxi.TaxiNotFoundException;
import statistics.Stats;
import statistics.StatsResponse;
import taxi.TaxiInfo;
import taxi.TaxiResponse;
import utils.Utils;

import javax.ws.rs.HttpMethod;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;

public class AdministratorClient {
    private static Client client = Client.create();
    private static BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));;
    private static final DecimalFormat df = new DecimalFormat("0.00");

    public static void main(String[] argv){

        while (true) {
            boolean check = true;
            int n = 0;
            while (check) {
                System.out.println("\n> Select the service you want to ask for:");
                System.out.println("> [1] Print the list of the taxis currently in the network");
                System.out.println("> [2] Print the average of n statistics of a Taxi currently in the network");
                System.out.println("> [3] Print the average statistics of all taxis occurred between two timestamps");
                try {
                    n = Integer.parseInt(inFromUser.readLine());
                    check = false;
                } catch (IOException e) {
                    System.out.print("> Please insert a valid number.\n");
                    e.printStackTrace();
                }
            }

            startStatsService(n);
        }
    }

    public static void startStatsService(int n){
        switch (n){
            case 1:
                try {
                    printTaxiList();
                } catch (Exception e) {
                    System.out.print(e.getMessage());
                }
                break;
            case 2:
                try {
                    printTaxiStats();
                } catch (Exception e) {
                    System.out.print(e.getMessage());
                }
                break;
        }
    }

    public static void printTaxiList() throws Exception {
        String path = Utils.servicesAddress + Utils.taxiServicePath + "/getTaxiList";
        ClientResponse clientResponse = Utils.sendRequest(client, path, HttpMethod.GET);

        int statusInfo = clientResponse.getStatus();

        TaxiResponse taxiResponse;
        if (ClientResponse.Status.OK.getStatusCode() == statusInfo) {
            taxiResponse = clientResponse.getEntity(TaxiResponse.class);
            if (taxiResponse.getTaxiInfoList() != null){
                System.out.print("> Taxis in the network:\n");
                for (TaxiInfo info : taxiResponse.getTaxiInfoList()){
                    System.out.print(info + "\n");
                }
            }else
                System.out.print("> There are no taxis in the network yet.\n");

        }else {
            throw new Exception("Status code: "+ statusInfo);
        }
    }

    public static void printTaxiStats() throws Exception {
        boolean check = true;
        String id="";

        while (check) {
            System.out.println("\n> Insert the ID of the taxi:");
            try {
                id = inFromUser.readLine();
                if (!id.equals(""))
                    check = false;
                else
                    System.out.print("> Please insert a valid ID. \n");
            } catch (IOException e) {
                System.out.println("> An error occurred. Please insert a value\n");
            }
        }

        check =  true;
        int n=0;
        while (check) {
            System.out.println("\n> How many statistics would you like to check? ");
            try {
                n = Integer.parseInt(inFromUser.readLine());
                check = false;
            } catch (IOException e) {
               System.out.print("> Please insert a valid number. \n");
            }
        }

        String path = Utils.servicesAddress + Utils.statsServicePath + "/getLastNTaxiStats/" + id +"/"+ n;
        ClientResponse clientResponse = Utils.sendRequest(client, path, HttpMethod.GET);
        int statusInfo = clientResponse.getStatus();

        StatsResponse response;
        if (ClientResponse.Status.OK.getStatusCode() == statusInfo) {
            response = clientResponse.getEntity(StatsResponse.class);
            if (response.getStatsList() != null){
                computeAverage(response);
            }else
                System.out.print("> There are no statistics for the taxi "+ id +" yet.\n");
        } else if (ClientResponse.Status.NOT_FOUND.getStatusCode() == statusInfo) {
            //The taxi specified does not exist
            throw new TaxiNotFoundException(". No stats found for taxi " + id +".");
        }else {
            throw new Exception("> Status code: "+ statusInfo);
        }
    }

    public static void computeAverage(StatsResponse response){
        double avgKm = 0.0;
        double avgBattery = 0;
        double avgPollution = 0.0;
        double avgRides = 0.0;
        int size = response.getStatsList().size();

        for (Stats stat : response.getStatsList()){
            avgKm += stat.getKmDriven();
            avgBattery += stat.getBattery();
            avgPollution += stat.getAirPollutionLev().stream().mapToDouble(i ->i).sum() / stat.getAirPollutionLev().size();
            avgRides += stat.getCompletedRides();
        }
        System.out.println("> Statistics averages:");
        System.out.println("> " + df.format(avgKm/size) + " km driven;" +
                                  df.format(avgBattery/size) + "% battery; " +
                                  df.format(avgPollution/size) + " pollution level;" +
                                  df.format(avgRides/size) + " rides completed.");
    }
}
