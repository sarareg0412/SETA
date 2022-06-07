package taxi;

import com.google.gson.Gson;
import com.google.protobuf.Empty;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.WebResource;
import exceptions.taxi.TaxiAlreadyPresentException;
import exceptions.taxi.TaxiNotFoundException;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.eclipse.paho.client.mqttv3.*;
import statistics.Stats;
import taxi.modules.GRPCServerThread;
import taxi.modules.StatsThread;
import taxi.modules.ExitThread;
import unimi.dps.taxi.TaxiRPCServiceGrpc;
import unimi.dps.taxi.TaxiRPCServiceGrpc.TaxiRPCServiceStub;
import unimi.dps.taxi.TaxiRPCServiceOuterClass.TaxiInfoMsg;
import utils.Position;
import utils.Utils;

import unimi.dps.ride.RideOuterClass.Ride;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@XmlRootElement
public class Taxi {
    private TaxiInfo taxiInfo;                          // Taxi's info: id; port number and address
    private int batteryLevel;                           // Taxi's battery level
    private Position position = new Position();         // Taxi's current position in Cartesian coordinates

    private List<Taxi> taxisList = new ArrayList<>();   // List of other taxis

    private Client client;

    //Ride district subscription
    private MqttClient mqttClient;
    private String districtTopic;
    private int qos;

    //RPC components
    private GRPCServerThread grpcServerThread;
    private StatsThread statsThread;
    private ExitThread exitThread;

    public static void main(String argv[]){
        Taxi taxi = new Taxi();
        taxi.startTaxi();
    }

    public Taxi() {
    }

    public Taxi(TaxiInfo taxiInfo) {
        this.taxiInfo = taxiInfo;
    }

    public Taxi(TaxiInfo taxiInfo, Position position){
        this.taxiInfo = taxiInfo;
        this.position = position;
    }

    public void startTaxi(){
        initGeneralComponents();
        boolean initComplete = false;
        do{
            initTaxi();

            try {
                // The taxi requests to join the network
                TaxiResponse taxiResponse = insertTaxi(taxiInfo);
                System.out.print("Taxi added with position: " + taxiResponse.getPosition() + "\n");
                position = taxiResponse.getPosition();

                initThreads();
                grpcServerThread.start();

                if(taxiResponse.getTaxiInfoList() != null){
                    //There are other taxis in the network, the current taxi notifies them
                    //that it has been succesfully added to the network
                    //The message to send is created only once
                    TaxiInfoMsg newTaxiMsg = TaxiInfoMsg.newBuilder()
                            .setId(taxiInfo.getId())
                            .setAddress(taxiInfo.getAddress())
                            .setPort(taxiInfo.getPort())
                            .setPosition(Ride.PositionMsg.newBuilder()
                                                        .setX(position.getY())
                                                        .setY(position.getY())
                                                        .build())
                            .build();
                    for (TaxiInfo otherTaxiInfo : taxiResponse.getTaxiInfoList()) {
                        Taxi other = new Taxi(otherTaxiInfo);
                        getTaxisList().add(other);          // The taxi's list is updated
                        System.out.print("Taxis present : " + otherTaxiInfo.getId() + "\n");
                        // The taxi notifies the others that it is now part of the network
                        notifyOtherTaxi(newTaxiMsg, other);
                    }
                }
                getTaxisList().add(this);           //The taxi's list now contains itself
                initComplete = true;
            } catch (TaxiAlreadyPresentException e) {
                System.out.print(e.getMessage() + "\n");
            } catch (Exception e) {
                System.out.print(e.getMessage() + "\n");
            }
        }while (!initComplete);

        try {
            initMqttComponents();
            mqttClient.subscribe(districtTopic, qos);
            System.out.print("Taxi subscribed to district : " + districtTopic + "\n");
            // Start to send statistics as soon as the taxi is subscribed to the district's topic
            statsThread.start();
        } catch (MqttException e) {
            e.printStackTrace();
        }
        exitThread.start();

        while (true) {
            try {
                //addStatsToQueue();
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void initGeneralComponents(){
        taxiInfo = new TaxiInfo();
        batteryLevel = 100;
        client = Client.create();
    }

    public void initMqttComponents() throws MqttException {
        mqttClient = new MqttClient(Utils.MQTTBrokerAddress, taxiInfo.getId());
        districtTopic = Utils.getDistrictTopicFromPosition(position);
        qos = 2;
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);
        mqttClient.connect(connOpts);

        mqttClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                String time = new Timestamp(System.currentTimeMillis()).toString();
                Ride ride =  Ride.parseFrom(message.getPayload());
                System.out.println("****************************NEW RIDE***********************************");
                System.out.println("Message Arrived at Time: " + time + "  Topic: " + topic + "  Message: "
                        + ride.toString());
                System.out.println("***********************************************************************");
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
    }

    public void initThreads(){
        grpcServerThread = new GRPCServerThread(this);
        exitThread = new ExitThread(this);
        statsThread = new StatsThread(this);
    }

    public void initTaxi(){

        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
        boolean check = true;

        while(check) {
            System.out.print("Insert Taxi ID: \n");

            try {

                taxiInfo.setId(inFromUser.readLine());
                if (!taxiInfo.getId().equals(""))
                    check = false;
                else
                    System.out.print("Please insert a valid ID. \n");

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
                if (!taxiInfo.getAddress().equals(""))
                    check = false;
                else
                    System.out.print("Please insert a valid address. \n");
            }catch (Exception e){
                System.out.println("An error occurred. Please insert a value\n");
            }
        }
    }

    /* A new taxi requested to enter the smart city */
    public TaxiResponse insertTaxi(TaxiInfo taxiInfo) throws Exception {
        String path = Utils.servicesAddress + Utils.taxiServicePath + "/add";
        ClientResponse clientResponse = sendPOSTRequest(client, path, taxiInfo);
        if (clientResponse == null){
            //TODO
        }

        TaxiResponse taxiResponse = null;

        int statusInfo = clientResponse.getStatus();

        if (Status.OK.getStatusCode() == statusInfo) {
            //Taxi correctly added
            taxiResponse = clientResponse.getEntity(TaxiResponse.class);
        } else if (Status.CONFLICT.getStatusCode() == statusInfo) {
            //Taxi already added
            throw new TaxiAlreadyPresentException();
        }else {
            throw new Exception("Status code: "+ statusInfo);
        }

        return taxiResponse;
    }

    /* Given a client, url and object, send a POST request with that object as parameter*/
    public ClientResponse sendPOSTRequest(Client client, String url, TaxiInfo t){
        WebResource webResource = client.resource(url);
        String input = new Gson().toJson(t);
        try {
            return webResource.type("application/json").post(ClientResponse.class, input);
        } catch (ClientHandlerException e) {
            System.out.println("Service unavailable");
            return null;
        }
    }

    /* The taxi acts as a client and notifies the other taxis that it has been
    * added to the network. */
    public void notifyOtherTaxi(TaxiInfoMsg request , Taxi other) throws InterruptedException {
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(other.getTaxiInfo().getAddress()+":" + other.getTaxiInfo().getPort()).usePlaintext().build();

        TaxiRPCServiceStub stub = TaxiRPCServiceGrpc.newStub(channel);

        stub.addTaxi(request, new StreamObserver<Empty>() {
            @Override
            public void onNext(Empty value) {
                // The current taxi correctly notified the others
                System.out.println("Other taxis correctly reached. Taxi " + taxiInfo.getId() + " list : " + printTaxiList());
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onCompleted() {
                channel.shutdown();
            }
        });
        channel.awaitTermination(1, TimeUnit.SECONDS);
    }

    public synchronized void addNewTaxiToList(Taxi taxi){
        taxisList.add(taxi);
    }

    public synchronized void removeTaxiFromList(String id) throws TaxiNotFoundException {
        int index = -1;

        for (Taxi t : getTaxisList()){
            if (t.getTaxiInfo().getId().equals(id))
                index = getTaxisList().indexOf(t);
        }

        if (index == -1)
            throw new TaxiNotFoundException();
        else
            taxisList.remove(index);
    }

    public void startElection(){

    }

    public void addStatsToQueue(){
        Stats stats = new Stats();
        stats.setTaxiId(taxiInfo.getId());
        stats.setKmDriven(Math.random()*20 + 5.0);
        statsThread.getStatsQueue().put(stats);
    }

    public TaxiInfo getTaxiInfo() {
        return taxiInfo;
    }

    public void setTaxiInfo(TaxiInfo taxiInfo) {
        this.taxiInfo = taxiInfo;
    }

    public synchronized List<Taxi> getTaxisList() {
        return taxisList;
    }

    public void setTaxisList(List<Taxi> taxisList) {
        this.taxisList = taxisList;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public int getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(int batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    public String printTaxiList(){
        ArrayList<Taxi> taxis = new ArrayList<>(getTaxisList());
        StringBuilder s = new StringBuilder();
        for (Taxi t : taxis){
            s.append(t.getTaxiInfo().getId() + " ");
        }

        return s.toString();
    }
}
