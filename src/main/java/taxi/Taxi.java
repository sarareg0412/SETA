package taxi;

import REST.TaxiResponse;
import com.google.gson.Gson;
import com.google.protobuf.Empty;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.WebResource;
import exceptions.taxi.TaxiAlreadyPresentException;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.eclipse.paho.client.mqttv3.*;
import simulator.MeasurementsBuffer;
import simulator.PM10Simulator;
import statistics.modules.*;
import taxi.modules.*;
import taxi.modules.election.MainElectionThread;
import taxi.modules.recharge.MainRechargeThread;
import unimi.dps.taxi.TaxiRPCServiceGrpc;
import unimi.dps.taxi.TaxiRPCServiceOuterClass.*;
import utils.Position;
import utils.Utils;

import unimi.dps.ride.Ride.*;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


public class Taxi {
    private TaxiInfo taxiInfo;                          // Taxi's info: id; port number and address
    private TaxiUtils taxiUtils;                        // Taxi's infos and list of other taxis shared between the taxi's thread

    //Threads
    private GRPCServerThread grpcServerThread;
    private StatsThread statsThread;
    private StdInThread stdInThread;
    private MainRechargeThread mainRechargeThread;
    private PM10Simulator pm10Simulator;

    private Client RESTClient;


    public static void main(String argv[]){
        Taxi taxi = new Taxi();
        taxi.startTaxi();
    }

    public Taxi() {
    }

    public Taxi(TaxiInfo taxiInfo) {
        this.taxiInfo = taxiInfo;
    }

    public void startTaxi(){
        initGeneralComponents();
        boolean initComplete = false;
        do{
            initTaxi();

            try {
                // The taxi requests to join the network
                TaxiResponse taxiResponse = insertTaxi(taxiInfo);
                //System.out.println("> [INS] Taxi added with position: " + taxiResponse.getPosition());
                initTaxiUtils(taxiResponse.getPosition());
                initThreads();
                System.out.println("> [INS] Taxi correctly added to the network.");
                System.out.println(taxiUtils);
                if(taxiResponse.getTaxiInfoList() != null){
                    //There are other taxis in the network, the current taxi notifies them
                    //that it has been succesfully added to the network
                    //The message to send is created only once
                    TaxiInfoMsg newTaxiMsg = TaxiInfoMsg.newBuilder()
                            .setId(taxiInfo.getId())
                            .setAddress(taxiInfo.getAddress())
                            .setPort(taxiInfo.getPort())
                            .setPosition(RideMsg.PositionMsg.newBuilder()
                                                        .setX(taxiUtils.getPosition().getY())
                                                        .setY(taxiUtils.getPosition().getY())
                                                        .build())
                            .build();
                    for (TaxiInfo otherTaxiInfo : taxiResponse.getTaxiInfoList()) {
                        Taxi other = new Taxi(otherTaxiInfo);
                        taxiUtils.addNewTaxiToList(other);          // The taxi's list is updated
                        // The taxi notifies the others that it is now part of the network
                        notifyOtherTaxi(newTaxiMsg, otherTaxiInfo);
                    }
                }
                taxiUtils.addNewTaxiToList(this);           //The taxi's list now contains itself
                System.out.println("> [INS] Taxi list : " + Utils.printTaxiList(taxiUtils.getTaxisList()));
                initComplete = true;
            } catch (TaxiAlreadyPresentException e) {
                System.out.println(e.getMessage());
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }while (!initComplete);

        try {
            initMqttComponents();
            Utils.subscribeToTopic(taxiUtils.getMQTTClient(), taxiUtils.getQos(), Utils.getDistrictTopicFromPosition(taxiUtils.getPosition()));
            //Notifies seta
            Utils.publishAvailable(taxiUtils.getMQTTClient(), taxiUtils.getQos(), taxiUtils.getPosition());
            // Start to acquire pollution levels from sensor
            pm10Simulator.start();
            // Start to send statistics as soon as the taxi is subscribed to the district's topic
            statsThread.start();
        } catch (MqttException e) {
            e.printStackTrace();
        }
        mainRechargeThread.start();     // Start thread to recharge
        stdInThread.start();             // Start thread to exit in a controlled way
    }

    public void initGeneralComponents(){
        taxiInfo = new TaxiInfo();
        RESTClient = Client.create();
    }

    public void initMqttComponents() throws MqttException {
        taxiUtils.setMQTTClient(new MqttClient(Utils.MQTTBrokerAddress, taxiInfo.getId()));
        taxiUtils.setQos(2);
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);
        taxiUtils.getMQTTClient().connect(connOpts);

        taxiUtils.getMQTTClient().setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                RideMsg rideMsg =  RideMsg.parseFrom(message.getPayload());
                //To overcome the fact that a message can arrive even if the taxi moved to another district for delays
                if (taxiUtils.isInTheSameDistrict(new Position(rideMsg.getStart()))) {
                    // An election is started if the taxi is available and doesn't want to charge
                    if (!taxiUtils.wantsToCharge() && taxiUtils.isAvailable()) {
                        MainElectionThread electionThread = new MainElectionThread(rideMsg, statsThread.getStatsQueue());
                        electionThread.start();
                        electionThread.join();
                    }
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
    }

    public void initTaxiUtils(Position position){
        taxiUtils = TaxiUtils.getInstance();
        taxiUtils.setTaxiInfo(taxiInfo);
        taxiUtils.setPosition(position);
        taxiUtils.setBatteryLevel(Utils.MAX_BATTERY);       // Taxi initialized with max battery
        taxiUtils.setAvailable(true);                       // Taxi is set available to take rides
        taxiUtils.setCharging(false);                       // Taxi is not recharging
    }

    public void initThreads(){
        grpcServerThread = new GRPCServerThread();
        grpcServerThread.start();                           // RPC thread started
        stdInThread = new StdInThread();
        mainRechargeThread = new MainRechargeThread();
        // The measurements Buffer is created and shared between the stats thread and the PM10 Simulator
        MeasurementsBuffer buffer = new MeasurementsBuffer();
        statsThread = new StatsThread(buffer);
        pm10Simulator = new PM10Simulator(buffer);
    }

    public void initTaxi(){

        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
        boolean check = true;

        while(check) {
            System.out.println("> Insert Taxi ID:");

            try {
                taxiInfo.setId(inFromUser.readLine());
                if (!taxiInfo.getId().equals(""))
                    check = false;
                else
                    throw new IOException();

            }catch (IOException e){
                System.out.println("> Please insert a valid ID.");
            }catch (Exception e){
                System.out.println("> An error occurred. Please insert a value");
            }
        }

        check = true;

        while(check) {
            System.out.println("> Insert port number:");

            try {
                String s = inFromUser.readLine();
                if (!s.equals("")){
                    taxiInfo.setPort(Integer.parseInt(s));
                    check = false;
                }else
                    throw new IOException();
            }catch (Exception e){
                System.out.println("> Not a number. Please insert Integer Value");
            }
        }

        check = true;

        while(check) {
            System.out.println("> Insert address: ");

            try {
                taxiInfo.setAddress(inFromUser.readLine());
                if (!taxiInfo.getAddress().equals(""))
                    check = false;
                else
                    throw new IOException();

            }catch (IOException e){
                System.out.println("> Please insert a valid address. ");
            }catch (Exception e){
                System.out.println("> An error occurred. Please insert a value");
            }
        }
    }

    /* A new taxi requested to enter the smart city */
    public TaxiResponse insertTaxi(TaxiInfo taxiInfo) throws Exception {
        String path = Utils.servicesAddress + Utils.taxiServicePath + "/add";
        ClientResponse clientResponse = sendPOSTRequest(RESTClient, path, taxiInfo);
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
            throw new Exception("> Status code: "+ statusInfo);
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
            System.out.println("> Service currently unavailable. Please try again.");
            return null;
        }
    }

    /* The taxi acts as a client and notifies the other taxis that it has been
    * added to the network. */
    public void notifyOtherTaxi(TaxiInfoMsg request , TaxiInfo other) throws InterruptedException {
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(other.getAddress()+":" + other.getPort()).usePlaintext().build();
        TaxiRPCServiceGrpc.TaxiRPCServiceBlockingStub stub = TaxiRPCServiceGrpc.newBlockingStub(channel);
        Empty result = stub.addTaxi(request);
        channel.shutdown();
    }

    public TaxiInfo getTaxiInfo() {
        return taxiInfo;
    }

    public void setTaxiInfo(TaxiInfo taxiInfo) {
        this.taxiInfo = taxiInfo;
    }

}
