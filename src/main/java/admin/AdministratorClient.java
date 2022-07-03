package admin;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import exceptions.taxi.TaxiNotFoundException;
import services.stats.Stats;
import services.stats.StatsResponse;
import taxi.TaxiInfo;
import services.taxi.TaxiResponse;
import utils.Utils;

import javax.ws.rs.HttpMethod;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class AdministratorClient {
    private static Client client = Client.create();
    private static BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));;
    private static boolean quit = false;

    public static void main(String[] argv){

        while (!quit) {
            System.out.println("\n> Select the service you want to ask for:");
            System.out.println("> [1] Print the list of the taxis currently in the network.");
            System.out.println("> [2] Print the average of n statistics of a Taxi.");
            System.out.println("> [3] Print the average statistics of all taxis occurred between two timestamps.");
            System.out.println("> Press ENTER to stop.");

            try {
                String s = inFromUser.readLine();
                if (!s.equals("")){
                    int n = Integer.parseInt(s);
                    if (n<1 || n>4)
                        throw new IOException();
                    else
                        startStatsService(n);
                }else {
                    System.out.println("> Quitting Administrator Client.");
                    quit = true;
                }
            } catch (IOException e) {
                System.out.println("> Please insert a valid number.");
            }
        }
    }

    public static void startStatsService(int n){
        switch (n){
            case 1:
                try {
                    printTaxiList();
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
                break;
            case 2:
                try {
                    printTaxiStats();
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
                break;
            case 3:
                try {
                    printTimestampStats();
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
                break;
        }
    }

    public static void printTaxiList() throws Exception {
        String path = Utils.SERVICES_ADDRESS + Utils.TAXI_SERVICE_PATH + "/getTaxiList";
        ClientResponse clientResponse = Utils.sendRequest(client, path, HttpMethod.GET);

        int statusInfo = clientResponse.getStatus();

        TaxiResponse taxiResponse;
        if (ClientResponse.Status.OK.getStatusCode() == statusInfo) {
            taxiResponse = clientResponse.getEntity(TaxiResponse.class);
            if (taxiResponse.getTaxiInfoList() != null){
                System.out.println("> Taxis in the network:");
                for (TaxiInfo info : taxiResponse.getTaxiInfoList()){
                    System.out.println(info + "");
                }
            }else
                System.out.println("> There are no taxis in the network yet.");

        }else {
            throw new Exception("Status code: "+ statusInfo);
        }
    }

    public static void printTaxiStats() throws Exception {
        boolean check = true;
        String id="";

        while (check) {
            System.out.println("> Insert the ID of the taxi:");
            try {
                id = inFromUser.readLine();
                if (!id.equals(""))
                    check = false;
                else
                    throw new IOException();

            }catch (IOException e){
                    System.out.println("> Please insert a valid ID. ");
            } catch (Exception e) {
                System.out.println("> An error occurred. Please insert a value");
            }
        }

        check =  true;
        int n=0;
        while (check) {
            System.out.println("> How many statistics would you like to check? ");
            try {
                String s = inFromUser.readLine();
                if (!s.equals("")){
                    n = Integer.parseInt(s);
                    check = false;
                }else
                    throw new IOException();
            } catch (IOException e) {
                System.out.println("> Please insert a valid number.");
            } catch (Exception e) {
               System.out.println("> An Error occurred. Please insert a number. ");
            }
        }

        String path = Utils.SERVICES_ADDRESS + Utils.STATS_SERVICE_PATH + "/getLastNTaxiStats/" + id +"/"+ n;
        ClientResponse clientResponse = Utils.sendRequest(client, path, HttpMethod.GET);
        int statusInfo = clientResponse.getStatus();

        StatsResponse response;
        if (ClientResponse.Status.OK.getStatusCode() == statusInfo) {
            response = clientResponse.getEntity(StatsResponse.class);
            if (response.getStatsList() != null){
                computeAverage(response.getStatsList());
            }else
                System.out.println("> There are no statistics for the taxi "+ id +" yet.");
        } else if (ClientResponse.Status.NOT_FOUND.getStatusCode() == statusInfo) {
            //The taxi specified does not exist
            throw new TaxiNotFoundException("No stats found for taxi " + id +".");
        }else {
            throw new Exception("> Status code: "+ statusInfo);
        }
    }

    public static void printTimestampStats() throws Exception {
        boolean check = true;
        String t1="", t2 ="";

        while (check) {
            System.out.println("> Insert the first timestamp [format: yyyy-mm-dd HH:mm]");
            try {
                t1 = inFromUser.readLine();
                if (Utils.isTimestampValid(t1)) {
                    check = false;
                }else
                    throw new IOException();

            }catch (IOException e){
                System.out.println("> Timestamp not valid. Please insert a valid timestamp.");
            } catch (Exception e) {
                System.out.println("> An error occurred. Please insert a timestamp");
            }
        }

        check = true;
        while (check) {
            System.out.println("> Insert the second timestamp [format: yyyy-mm-dd HH:mm]");
            try {
                t2 = inFromUser.readLine();
                if (Utils.isTimestampValid(t2) && Utils.moreThanEqual(t1, t2)) {
                    check = false;
                }else
                    throw new IOException();

            }catch (IOException e){
                System.out.println("> Timestamp not valid. Please insert a valid timestamp.");
            } catch (Exception e) {
                System.out.println("> An error occurred. Please insert a timestamp");
            }
        }

        String path = Utils.SERVICES_ADDRESS + Utils.STATS_SERVICE_PATH + "/getStatsBwTimestamps/" + t1.replace(' ', '_') +"/"+ t2.replace(' ', '_');
        ClientResponse clientResponse = Utils.sendRequest(client, path, HttpMethod.GET);
        int statusInfo = clientResponse.getStatus();

        StatsResponse response;
        if (ClientResponse.Status.OK.getStatusCode() == statusInfo) {
            response = clientResponse.getEntity(StatsResponse.class);
            if (response.getStatsList() != null){
                computeAverage(response.getStatsList());
            }else
                System.out.println("> There are no statistics between "+ t1 +" and "+ t2 +".");
        }else {
            throw new Exception("> An error occurred. Status code: "+ statusInfo);
        }

    }

    public static void computeAverage(List<Stats> statsList){

        double avgKm = statsList.stream().mapToDouble(Stats::getKmDriven).average().orElse(0.0);;
        double avgBattery = statsList.stream().mapToDouble(Stats::getBattery).average().orElse(0.0);
        double avgRides = statsList.stream().mapToDouble(Stats::getCompletedRides).average().orElse(0.0);
        /* First every stat list's of pollution levels average is computed, then the average of these is also computed */
        double avgPollution = statsList.stream()
                                        .mapToDouble(
                                            value -> value.getAirPollutionLev()
                                                            .stream()
                                                            .mapToDouble(Double::doubleValue)
                                                            .average().orElse(0.0)
                                        ).average().orElse(0.0);


        System.out.println("> Statistics averages:");
        System.out.println("> " + Utils.DECIMAL_FORMAT.format(avgKm) + " average km driven; " +
                                  Utils.DECIMAL_FORMAT.format(avgBattery) + "% average battery; " +
                                  Utils.DECIMAL_FORMAT.format(avgPollution) + " average pollution level; " +
                                  Utils.DECIMAL_FORMAT.format(avgRides) + " rides completed.");
    }
}
