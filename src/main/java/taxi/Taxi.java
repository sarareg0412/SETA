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
import io.grpc.ServerBuilder;
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
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@XmlRootElement
public class Taxi {
    private static TaxiInfo taxiInfo;                      // Taxi's info: id; port number and address

    private static List<TaxiInfo> otherTaxisList;

    private static int batteryLevel;                       // Taxi's battery level
    private static Position position = new Position();     // Taxi's current position in Cartesian coordinates
    private static String taxiServicePath;
    private static Client client;

    //Ride district subscription
    private static MqttClient mqttClient;
    private static String districtTopic;
    private static int qos;

    //RPC components
    private static GRPCServerThread grpcServerThread;

    public static void main(String argv[]){
        initGeneralComponents();
        boolean initComplete = false;
        do{
            initTaxi();

            try {
                // The taxi requests to join the network
                TaxiResponse taxiResponse = insertTaxi(taxiInfo);
                System.out.print("Taxi added with position: " + taxiResponse.getPosition() + "\n");
                position = taxiResponse.getPosition();
                if (taxiResponse.getTaxiInfoList() != null)
                    otherTaxisList = new ArrayList<TaxiInfo>(taxiResponse.getTaxiInfoList());
                else
                    otherTaxisList = new ArrayList<>();

                initGRPCComponents();

                for (TaxiInfo other : otherTaxisList) {
                    System.out.print("Taxis present : " + other.getId() + "\n");
                    TaxiInfoMsg newTaxi = TaxiInfoMsg.newBuilder()
                                                    .setId(other.getId())
                                                    .setAddress(other.getAddress())
                                                    .setPort(other.getPort())
                                                    .build();
                    notifyOtherTaxi(newTaxi,other.getAddress(), other.getPort());
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

    public static void initGeneralComponents(){
        taxiServicePath = "taxis";
        taxiInfo = new TaxiInfo();
        batteryLevel = 100;
        client = Client.create();
    }

    public static void initMqttComponents() throws MqttException {
        mqttClient = new MqttClient(Utils.MQTTBrokerAddress, taxiInfo.getId());
        districtTopic = Utils.getDistrictTopicFromPosition(position.getX(), position.getY());
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

    public static void initGRPCComponents(){
        grpcServerThread = new GRPCServerThread(taxiInfo);
        grpcServerThread.start();
    }

    public static void initTaxi(){

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
    public static TaxiResponse insertTaxi(TaxiInfo taxiInfo) throws Exception {
        String path = Utils.taxiServiceAddress + taxiServicePath + "/add";
        ClientResponse clientResponse = sendPOSTRequest(client, path, taxiInfo);
        System.out.print(clientResponse +"\n");
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
    public static ClientResponse sendPOSTRequest(Client client, String url, TaxiInfo t){
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
    public static void notifyOtherTaxi(TaxiInfoMsg request , String address, int port) throws InterruptedException {
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(address+":" + port).usePlaintext().build();

        TaxiRPCServiceStub stub = TaxiRPCServiceGrpc.newStub(channel);

        stub.newTaxi(request, new StreamObserver<Empty>() {
            @Override
            public void onNext(Empty value) {
                addNewTaxiToList(request);
                System.out.println("taxi " + taxiInfo.getId() + " list : \n");
                for (TaxiInfo taxiInfo : otherTaxisList)
                    System.out.print(taxiInfo.getId() +  ";");

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

    public static synchronized void addNewTaxiToList(TaxiInfoMsg taxiInfoMsg){
        Position newTaxiPosition = new Position(taxiInfoMsg.getPosition().getX(), taxiInfoMsg.getPosition().getY());
        TaxiInfo newTaxi = new TaxiInfo(taxiInfoMsg.getId(), taxiInfoMsg.getPort(), taxiInfoMsg.getAddress());
        otherTaxisList.add(newTaxi);
    }

    public static TaxiInfo getTaxiInfo() {
        return taxiInfo;
    }

    public static void setTaxiInfo(TaxiInfo taxiInfo) {
        Taxi.taxiInfo = taxiInfo;
    }
}
