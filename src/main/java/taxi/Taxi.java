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
import taxi.modules.GRPCServerThread;
import taxi.modules.StdInputThread;
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
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

@XmlRootElement
public class Taxi {
    private TaxiInfo taxiInfo;                      // Taxi's info: id; port number and address

    private List<Taxi> taxisList = new ArrayList<>();                   // List of other taxis

    private int batteryLevel;                       // Taxi's battery level
    private Position position = new Position();     // Taxi's current position in Cartesian coordinates
    private Client client;

    //Ride district subscription
    private MqttClient mqttClient;
    private String districtTopic;
    private int qos;

    //RPC components
    private GRPCServerThread grpcServerThread;

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
                System.out.print("Taxi added with position: " + taxiResponse.getPosition() + "\n");
                position = taxiResponse.getPosition();
                getTaxisList().add(this);           //The taxi's list now contains itself

                initGRPCComponents();
                if(taxiResponse.getTaxiInfoList() != null){
                    //There are other taxis in the network, the current taxi notifies them
                    //that it has been succesfully added to the network
                    for (TaxiInfo otherTaxiInfo : taxiResponse.getTaxiInfoList()) {
                        Taxi other = new Taxi(otherTaxiInfo);
                        getTaxisList().add(other);          // The taxi's list is updated

                        System.out.print("Taxis present : " + otherTaxiInfo.getId() + "\n");
                        TaxiInfoMsg newTaxiMsg = TaxiInfoMsg.newBuilder()
                                .setId(otherTaxiInfo.getId())
                                .setAddress(otherTaxiInfo.getAddress())
                                .setPort(otherTaxiInfo.getPort())
                                .build();
                        // The taxi notifies the others that it is now part of the network
                        notifyOtherTaxi(newTaxiMsg, this, other);
                    }
                }
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

        } catch (MqttException e) {
            e.printStackTrace();
        }

        StdInputThread exitThread = new StdInputThread(taxiInfo);
        exitThread.start();
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

    public void initGRPCComponents(){
        grpcServerThread = new GRPCServerThread(taxiInfo);
        grpcServerThread.start();
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
        String path = Utils.taxiServiceAddress + Utils.taxiServicePath + "/add";
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
    public void notifyOtherTaxi(TaxiInfoMsg request , Taxi current, Taxi other) throws InterruptedException {
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(other.getTaxiInfo().getAddress()+":" + other.getTaxiInfo().getPort()).usePlaintext().build();

        TaxiRPCServiceStub stub = TaxiRPCServiceGrpc.newStub(channel);

        stub.newTaxi(request, new StreamObserver<Empty>() {
            @Override
            public void onNext(Empty value) {
                //Adds the new taxi to the other one
                other.getTaxisList().add(current);
                other.getTaxisList().get(other.getTaxisList().indexOf(current))
                        .setPosition(new Position(request.getPosition().getX(), request.getPosition().getY()));
                //addNewTaxiToList(request);
                System.out.println("taxi " + taxiInfo.getId() + " list : \n");
                for (Taxi taxi : taxisList)
                    System.out.print(taxi.getTaxiInfo().getId() +  ";");

                System.out.println("\n");
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

    public synchronized void addNewTaxiToList(TaxiInfoMsg taxiInfoMsg){
        Position newTaxiPosition = new Position(taxiInfoMsg.getPosition().getX(), taxiInfoMsg.getPosition().getY());
        TaxiInfo newTaxi = new TaxiInfo(taxiInfoMsg.getId(), taxiInfoMsg.getPort(), taxiInfoMsg.getAddress());
        //getTaxisList().add(newTaxi);
    }

    public void startElection(){

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
}
