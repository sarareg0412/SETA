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
                otherTaxisList = new ArrayList<TaxiInfo>(taxiResponse.getTaxiInfoList());
                for (TaxiInfo taxiInfo : otherTaxisList)
                    System.out.print("Taxis present : " + taxiInfo.getId() +  "\n");
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

        stopTaxi();
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
        System.out.print("ok " + clientResponse +"\n");
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

    public static void stopTaxi(){
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
        boolean check = false;

        while(!check) {
            System.out.print("Write \"quit\" to terminate Taxi.\n");

            try {
                check = inFromUser.readLine().equalsIgnoreCase("quit");
                if (check )
                    deleteTaxi();
            }catch (Exception e){
                System.out.println("An error occurred.\n");
            }
        }
        System.exit(0);
    }

    private static void deleteTaxi() throws Exception {
        String path = Utils.taxiServiceAddress + taxiServicePath + "/delete/" + taxiInfo.getId();
        ClientResponse clientResponse = sendDELETERequest(client, path);
        System.out.print("ok " + clientResponse +"\n");
        if (clientResponse == null){
            //TODO
        }
        int statusInfo = clientResponse.getStatus();

        if (Status.OK.getStatusCode() == statusInfo) {
            //Taxi correctly deleted
            System.out.print("Taxi correctly deleted.\n");
        } else if (Status.CONFLICT.getStatusCode() == statusInfo) {
            //Taxi already added
            throw new TaxiNotFoundException();
        }else {
            throw new Exception("Status code: "+ statusInfo);
        }
    }

    /* Given a client, url and object, send a DELETE request with that object as parameter*/
    public static ClientResponse sendDELETERequest(Client client, String url){
        WebResource webResource = client.resource(url);
        try {
            return webResource.type("application/json").delete(ClientResponse.class);
        } catch (ClientHandlerException e) {
            System.out.println("Service unavailable");
            return null;
        }
    }

    public static void notifyOtherTaxi(TaxiInfoMsg request , String address, int port){
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(address+":" + port).usePlaintext().build();

        TaxiRPCServiceStub stub = TaxiRPCServiceGrpc.newStub(channel);

        stub.newTaxi(request, new StreamObserver<Empty>() {
            @Override
            public void onNext(Empty value) {

            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onCompleted() {

            }
        });
    }
}
