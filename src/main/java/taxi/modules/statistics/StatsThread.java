package taxi.modules.statistics;

import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import simulator.Measurement;
import simulator.MeasurementsBuffer;
import services.stats.Stats;
import taxi.TaxiUtils;
import utils.Queue;
import utils.Utils;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/* Thread that sends the statistics of a taxi every 15 seconds to the Admin server*/
public class StatsThread extends Thread{
    private Queue<Stats> statsQueue;
    private Client client = Client.create();
    MeasurementsBuffer measurementsBuffer;

    public StatsThread(MeasurementsBuffer measurementsBuffer)
    {
        this.measurementsBuffer = measurementsBuffer;
        statsQueue = new Queue<Stats>();
    }

    @Override
    public void run() {
        while (!TaxiUtils.getInstance().quit()){
            try {
                Thread.sleep(15000);        //Statistics are sent every 15 seconds
                //The stats list gets emptied
                Stats finalStat = computeStats(statsQueue.getAllAndEmptyQueue());
                String path = Utils.SERVICES_ADDRESS + Utils.STATS_SERVICE_PATH + "/add";
                ClientResponse clientResponse = sendPOSTRequest(client, path, finalStat);
                int statusInfo = clientResponse.getStatus();

                if (ClientResponse.Status.OK.getStatusCode() != statusInfo) {
                    System.out.println("> An error occurred. Status code: "+ statusInfo);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /* Computing the statistics to send to the server*/
    private Stats computeStats(List<Stats> statsList){
        Stats stats = new Stats();
        List<Measurement> measurementsList = measurementsBuffer.readAllAndClean();
        stats.setKmDriven(statsList.stream().mapToDouble(Stats::getKmDriven).sum());
        stats.setCompletedRides(statsList.size());
        stats.setTaxiId(TaxiUtils.getInstance().getTaxiInfo().getId());
        stats.setBattery(TaxiUtils.getInstance().getBatteryLevel());
        stats.setAirPollutionLev(new ArrayList<>(measurementsList.stream().map(Measurement::getValue).collect(Collectors.toList())));
        stats.setTimestamp(new Timestamp(System.currentTimeMillis()).toString());

        return stats;
    }

    public Queue<Stats> getStatsQueue() {
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
