package taxi;

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
import io.grpc.stub.StreamObserver;
import org.eclipse.paho.client.mqttv3.*;
import ride.Ride;
import simulator.MeasurementsBuffer;
import simulator.PM10Simulator;
import statistics.Stats;
import taxi.modules.ElectionThread;
import taxi.modules.GRPCServerThread;
import taxi.modules.StatsThread;
import taxi.modules.ExitThread;
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
    private MqttClient MQTTClient;
    private Client RESTClient;
    private String districtTopic;
    private int qos;

    private PM10Simulator pm10Simulator;
    //Threads
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
                        System.out.print("> Taxi present : " + otherTaxiInfo.getId() + "\n");
                        // The taxi notifies the others that it is now part of the network
                        notifyOtherTaxi(newTaxiMsg, otherTaxiInfo);
                    }
                }
                taxiUtils.addNewTaxiToList(this);           //The taxi's list now contains itself
                initComplete = true;
            } catch (TaxiAlreadyPresentException e) {
                System.out.print(e.getMessage() + "\n");
            } catch (Exception e) {
                System.out.print(e.getMessage() + "\n");
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
        exitThread.start();             // Start thread to exit in a controlled way

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
        MQTTClient = new MqttClient(Utils.MQTTBrokerAddress, taxiInfo.getId());
        qos = 2;
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);
        MQTTClient.connect(connOpts);

        MQTTClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                String time = new Timestamp(System.currentTimeMillis()).toString();
                RideMsg rideMsg =  RideMsg.parseFrom(message.getPayload());
                System.out.println("****************************NEW RIDE***********************************");
                System.out.println("Message Arrived at Time: " + time + "  Topic: " + topic + "  Message: "
                        + rideMsg.toString());
                System.out.println("***********************************************************************");
                startElection(rideMsg);
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
        exitThread = new ExitThread();
        statsThread = new StatsThread();
        pm10Simulator = new PM10Simulator(new MeasurementsBuffer());
    }

    public void initTaxi(){

        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
        boolean check = true;

        while(check) {
            System.out.print("> Insert Taxi ID: \n");

            try {

                taxiInfo.setId(inFromUser.readLine());
                if (!taxiInfo.getId().equals(""))
                    check = false;
                else
                    throw new IOException();

            }catch (IOException e){
                System.out.print("> Please insert a valid ID. \n");
            }catch (Exception e){
                System.out.println("> An error occurred. Please insert a value\n");
            }
        }

        check = true;

        while(check) {
            System.out.print("> Insert port number: \n");

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
            System.out.print("> Insert address: \n");

            try {
                taxiInfo.setAddress(inFromUser.readLine());
                if (!taxiInfo.getAddress().equals(""))
                    check = false;
                else
                    throw new IOException();

            }catch (IOException e){
                System.out.print("> Please insert a valid address. \n");
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

    public void startElection(RideMsg rideMsg) throws MqttException {
        //The message to send is created only once
        ElectionMsg electionMessage = ElectionMsg.newBuilder()
                .setId(taxiInfo.getId())
                .setDistance(Utils.getDistanceBetweenPositions(taxiUtils.getPosition(),
                                                                Utils.getPositionFromPositionMsg(rideMsg.getStart())))
                .setBattery(taxiUtils.getBatteryLevel())
                .setRide(rideMsg)
                .build();

        taxiUtils.setElectionCounter(0);
        taxiUtils.setMaster(true);

        //TODO check synchronization for getTaxiList()
        ArrayList<ElectionThread> electionThreadList = new ArrayList<>();
        for (Taxi otherTaxi : TaxiUtils.getInstance().getTaxisList()) {
            // A new thread is created for the taxi to broadcasts the others and
            // itself to see to pick the master to take the ride
            ElectionThread electionThread = new ElectionThread(otherTaxi.getTaxiInfo(), electionMessage);
            electionThreadList.add(electionThread);
        }
        // Starts all threads
        for (ElectionThread t : electionThreadList){
            t.start();
        }

        /* Waits for all threads to finish their execution */
        for (ElectionThread thread : electionThreadList){
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("> Election finished");
        // The current taxi was elected by the others to take the ride
        Ride ride = new Ride(rideMsg);

        // TODO this condition fails when you add a taxi before the election is completed
        if (TaxiUtils.getInstance().isMaster()){
            try {
                System.out.println("> Taxi "+taxiInfo.getId()+" is taking the ride " + ride.getId());
                takeRide(ride);
                publishCompletedRide(rideMsg);
            } catch (MqttException e) {
                System.out.print("> An error occurred while taking the ride. ");
            }
        }
    }

    public void takeRide(Ride ride) throws MqttException {
        taxiUtils.setAvailable(false);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //Taxi subscribes to new distric topic if necessary
        if (Utils.getDistrictFromPosition(taxiUtils.getPosition()) != Utils.getDistrictFromPosition(ride.getFinish())) {
            unsubscribe(Utils.getDistrictTopicFromPosition(taxiUtils.getPosition()));
            publishChangeDistrict();
            subscribeToTopic(Utils.getDistrictTopicFromPosition(ride.getFinish()));
            publishAvailable();
        }
        // Taxi completed the ride; changes position
        taxiUtils.setPosition(ride.getFinish());
        // Taxi battery level decreases
        taxiUtils.setBatteryLevel(taxiUtils.getBatteryLevel() - (int) Math.floor(ride.getKmToTravel()));
        // Adds the stats of the current ride to the queue of stats to be sent to the Admin Server
        addStatsToQueue(ride.getKmToTravel());
        taxiUtils.setAvailable(true);
    }

    /* Sends back an mqtt message to SETA that the ride couldn't be taken by any taxi */
    public void publishCompletedRide(RideMsg ride) throws MqttException {
        //Seta starts creating the rides with a positive random id
        MqttMessage msg = new MqttMessage(ride.toByteArray());
        msg.setQos(qos);
        MQTTClient.publish(Utils.COMPLETED_RIDE, msg);
        System.out.print("Completed ride published:" + ride);
    }

    /* Notifies SETA that it is available */
    public void publishAvailable() throws MqttException {
        //Seta starts creating the rides with a positive random id
        MqttMessage msg = new MqttMessage(Empty.newBuilder().build().toByteArray());
        msg.setQos(qos);
        MQTTClient.publish(Utils.TAXI_AVAILABLE + Utils.getDistrictFromPosition(taxiUtils.getPosition()), msg);
        System.out.print("Correctly notified SETA");
    }

    /* Notifies SETA that taxi changed district */
    public void publishChangeDistrict() throws MqttException {
        //Seta starts creating the rides with a positive random id
        MqttMessage msg = new MqttMessage(Empty.newBuilder().build().toByteArray());
        msg.setQos(qos);
        MQTTClient.publish(Utils.CHANGED_DISTRICT + Utils.getDistrictFromPosition(taxiUtils.getPosition()), msg);
        System.out.print("Correctly notified SETA");
    }

    public void addStatsToQueue(double km){
        Stats stats = new Stats();
        stats.setTaxiId(taxiInfo.getId());
        stats.setKmDriven(km);
        statsThread.getStatsQueue().put(stats);
    }

    private void unsubscribe(String topic) throws MqttException {
        MQTTClient.unsubscribe(topic);
        System.out.print("> Taxi unsubscribed from topic : " + topic + "\n");
    }

    private void subscribeToTopic(String topic) throws MqttException {
        MQTTClient.subscribe(topic, qos);
        System.out.print("> Taxi subscribed to topic : " + topic + "\n");
    }

    public TaxiInfo getTaxiInfo() {
        return taxiInfo;
    }

    public void setTaxiInfo(TaxiInfo taxiInfo) {
        this.taxiInfo = taxiInfo;
    }
}
