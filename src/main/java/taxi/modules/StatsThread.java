package taxi.modules;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import statistics.Stats;
import statistics.StatsQueue;
import taxi.Taxi;
import taxi.TaxiUtils;
import utils.Utils;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

public class StatsThread extends Thread{
    private StatsQueue statsQueue;
    private Client client = Client.create();

    public StatsThread() {
        statsQueue = new StatsQueue();
    }

    @Override
    public void run() {
        while (true){
            try {
                Thread.sleep(15000);        //Statistics are sent every 15 seconds
                //The stats list gets emptied
                Stats finalStat = computeStats(statsQueue.getAllAndEmptyQueue());
                String path = Utils.servicesAddress + Utils.statsServicePath + "/add";
                ClientResponse clientResponse = sendPOSTRequest(client, path, finalStat);
                int statusInfo = clientResponse.getStatus();

                if (ClientResponse.Status.OK.getStatusCode() == statusInfo) {
                    //Stat correctly added
                    System.out.println("Stats correctly added for taxi " + TaxiUtils.getInstance().getTaxiInfo().getId());
                }else {
                    System.out.println(clientResponse);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /* Computing the statistics to send to the server*/
    private Stats computeStats(List<Stats> statsList){
        Stats stats = new Stats();
        double km = 0.0;
        ArrayList<Double> pollution = new ArrayList<>();
        for (Stats s : statsList){
            pollution.add(s.getKmDriven());
            km += s.getKmDriven();
        }

        stats.setKmDriven(km);
        stats.setCompletedRides(statsList.size());
        stats.setTaxiId(TaxiUtils.getInstance().getTaxiInfo().getId());
        stats.setBattery(TaxiUtils.getInstance().getBatteryLevel());
        stats.setTimestamp(new Timestamp(System.currentTimeMillis()).toString());
        stats.setAirPollutionLev(pollution);

        return stats;
    }

    public StatsQueue getStatsQueue() {
        return statsQueue;
    }

    /* Given a client, url and object, send a POST request with that object as parameter*/
    public ClientResponse sendPOSTRequest(Client client, String url, Stats stats){
        WebResource webResource = client.resource(url);
        String input = new Gson().toJson(stats);
        try {
            return webResource.type("application/json").post(ClientResponse.class, input);
        } catch (ClientHandlerException e) {
            System.out.println("Service unavailable");
            return null;
        }
    }
}
