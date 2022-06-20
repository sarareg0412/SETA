package taxi;

import REST.TaxiResponse;
import com.google.gson.Gson;
import com.google.protobuf.Empty;
import com.google.protobuf.TextFormat;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.WebResource;
import exceptions.taxi.TaxiAlreadyPresentException;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.eclipse.paho.client.mqttv3.*;
import ride.Ride;
import simulator.MeasurementsBuffer;
import simulator.PM10Simulator;
import statistics.Stats;
import statistics.modules.*;
import taxi.modules.*;
import taxi.modules.election.ElectionThread;
import taxi.modules.election.MainElectionThread;
import taxi.modules.recharge.CheckBatteryThread;
import taxi.modules.recharge.MainRechargeThread;
import unimi.dps.taxi.TaxiRPCServiceGrpc;
import unimi.dps.taxi.TaxiRPCServiceGrpc.TaxiRPCServiceStub;
import unimi.dps.taxi.TaxiRPCServiceOuterClass.*;
import utils.Position;
import utils.Utils;

import unimi.dps.ride.Ride.*;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

@XmlRootElement
public class Taxi {
    private TaxiInfo taxiInfo;                          // Taxi's info: id; port number and address
    private TaxiUtils taxiUtils;                        // Taxi's infos and list of other taxis shared between the taxi's thread

    //Ride district subscription
    private Client RESTClient;

    private PM10Simulator pm10Simulator;
    //Threads
    private GRPCServerThread grpcServerThread;
    private StatsThread statsThread;
    private StdInThread stdInThread;
    private MainRechargeThread mainRechargeThread;
    private CheckBatteryThread checkBatteryThread;

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
                System.out.println("> Taxi added with position: " + taxiResponse.getPosition() + "\n");
                initTaxiUtils(taxiResponse.getPosition());
                initThreads();

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
                        System.out.println("> Taxi present : " + otherTaxiInfo.getId() + "\n");
                        // The taxi notifies the others that it is now part of the network
                        notifyOtherTaxi(newTaxiMsg, otherTaxiInfo);
                    }
                }
                taxiUtils.addNewTaxiToList(this);           //The taxi's list now contains itself
                initComplete = true;
            } catch (TaxiAlreadyPresentException e) {
                System.out.println(e.getMessage() + "\n");
            } catch (Exception e) {
                System.out.println(e.getMessage() + "\n");
            }
        }while (!initComplete);

        try {
            initMqttComponents();
            subscribeToTopic(Utils.getDistrictTopicFromPosition(taxiUtils.getPosition()));
            //Notifies seta
            publishAvailable();
            // Start to acquire pollution levels from sensor
            pm10Simulator.start();
            // Start to send statistics as soon as the taxi is subscribed to the district's topic
            statsThread.start();
        } catch (MqttException e) {
            e.printStackTrace();
        }
        mainRechargeThread.start();     // Start thread to recharge
        checkBatteryThread.start();
        stdInThread.start();             // Start thread to exit in a controlled way

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
                while (taxiUtils.isInElection()){
                    taxiUtils.getInElectionLock().wait();
                }
                if (!taxiUtils.wantsToCharge() && taxiUtils.isAvailable()) {
                    MainElectionThread electionThread = new MainElectionThread(rideMsg);
                    electionThread.start();
                    electionThread.join();
                    if (taxiUtils.isMaster()) {
                        takeRide(new Ride(rideMsg));
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
        taxiUtils.setInElection(false);                     // Taxi is not in election
    }

    public void initThreads(){
        grpcServerThread = new GRPCServerThread();
        grpcServerThread.start();                           // RPC thread started
        stdInThread = new StdInThread();
        statsThread = new StatsThread();
        pm10Simulator = new PM10Simulator(new MeasurementsBuffer());
        mainRechargeThread = new MainRechargeThread();
        checkBatteryThread = new CheckBatteryThread();
    }

    public void initTaxi(){

        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
        boolean check = true;

        while(check) {
            System.out.println("> Insert Taxi ID: \n");

            try {

                taxiInfo.setId(inFromUser.readLine());
                if (!taxiInfo.getId().equals(""))
                    check = false;
                else
                    throw new IOException();

            }catch (IOException e){
                System.out.println("> Please insert a valid ID. \n");
            }catch (Exception e){
                System.out.println("> An error occurred. Please insert a value\n");
            }
        }

        check = true;

        while(check) {
            System.out.println("> Insert port number: \n");

            try {
                String s = inFromUser.readLine();
                if (!s.equals("")){
                    taxiInfo.setPort(Integer.parseInt(s));
                    check = false;
                }else
                    throw new IOException();
            }catch (Exception e){
                System.out.println("> Not a number. Please insert Integer Value\n");
            }
        }

        check = true;

        while(check) {
            System.out.println("> Insert address: \n");

            try {
                taxiInfo.setAddress(inFromUser.readLine());
                if (!taxiInfo.getAddress().equals(""))
                    check = false;
                else
                    throw new IOException();

            }catch (IOException e){
                System.out.println("> Please insert a valid address. \n");
            }catch (Exception e){
                System.out.println("> An error occurred. Please insert a value\n");
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
            System.out.println("> Service unavailable");
            return null;
        }
    }

    /* The taxi acts as a client and notifies the other taxis that it has been
    * added to the network. */
    public void notifyOtherTaxi(TaxiInfoMsg request , TaxiInfo other) throws InterruptedException {
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(other.getAddress()+":" + other.getPort()).usePlaintext().build();

        TaxiRPCServiceStub stub = TaxiRPCServiceGrpc.newStub(channel);

        stub.addTaxi(request, new StreamObserver<Empty>() {
            @Override
            public void onNext(Empty value) {
                // The current taxi correctly notified the others
                System.out.println("> Other taxis correctly reached. Taxi " + taxiInfo.getId() + " list : " + Utils.printTaxiList(taxiUtils.getTaxisList()));
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

    /* Notifies SETA that it is available */
    public void publishAvailable() throws MqttException {
        MqttMessage msg = new MqttMessage(Empty.newBuilder().build().toByteArray());
        msg.setQos(taxiUtils.getQos());
        taxiUtils.getMQTTClient().publish(Utils.TAXI_AVAILABLE + Utils.getDistrictFromPosition(taxiUtils.getPosition()), msg);
        System.out.println("> TAXI AVAILABLE Correctly notified SETA");
    }

    /* Notifies SETA that taxi is not available anymore in that district */
    public static void publishUnavailable(Position p, MqttClient client, int qos) throws MqttException {
        MqttMessage msg = new MqttMessage(Empty.newBuilder().build().toByteArray());
        msg.setQos(qos);
        client.publish(Utils.TAXI_UNAVAILABLE + Utils.getDistrictFromPosition(p), msg);
        System.out.println("> TAXI UNAVAILABLE Correctly notified SETA");
    }

    public void takeRide(Ride ride) throws MqttException {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Position oldPosition = taxiUtils.getPosition();
        // Taxi completed the ride; changes position
        taxiUtils.setPosition(ride.getFinish());

        //Taxi subscribes to new distric topic if necessary
        if (Utils.getDistrictFromPosition(oldPosition) != Utils.getDistrictFromPosition(ride.getFinish())) {
            unsubscribe(Utils.getDistrictTopicFromPosition(oldPosition));
            publishUnavailable(oldPosition,taxiUtils.getMQTTClient(), taxiUtils.getQos());
            subscribeToTopic(Utils.getDistrictTopicFromPosition(taxiUtils.getPosition()));
            publishAvailable();
        }
        // Taxi battery level decreases
        taxiUtils.setBatteryLevel(taxiUtils.getBatteryLevel() - (int) Math.floor(ride.getKmToTravel(oldPosition)));
        // Adds the stats of the current ride to the queue of stats to be sent to the Admin Server
        taxiUtils.setAvailable(true);
        taxiUtils.setInElection(false);
        taxiUtils.setMaster(false);
        System.out.println("> [ELEC] RIDE COMPLETED " + ride.getId());
        addStatsToQueue(ride.getKmToTravel(oldPosition));
    }


    public void addStatsToQueue(double km){
        Stats stats = new Stats();
        stats.setTaxiId(taxiInfo.getId());
        stats.setKmDriven(km);
        statsThread.getStatsQueue().put(stats);
    }

    private void unsubscribe(String topic) throws MqttException {
        taxiUtils.getMQTTClient().unsubscribe(topic);
        System.out.println("> Taxi unsubscribed from topic : " + topic);
    }

    private void subscribeToTopic(String topic) throws MqttException {
        taxiUtils.getMQTTClient().subscribe(topic, taxiUtils.getQos());
        System.out.println("> Taxi subscribed to topic : " + topic);
    }

    public TaxiInfo getTaxiInfo() {
        return taxiInfo;
    }

    public void setTaxiInfo(TaxiInfo taxiInfo) {
        this.taxiInfo = taxiInfo;
    }

}
