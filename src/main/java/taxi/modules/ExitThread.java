package taxi.modules;

import com.google.protobuf.Empty;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import exceptions.taxi.TaxiNotFoundException;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import taxi.Taxi;
import taxi.TaxiInfo;
import taxi.TaxiUtils;
import unimi.dps.taxi.TaxiRPCServiceGrpc;
import unimi.dps.taxi.TaxiRPCServiceOuterClass.*;
import utils.Utils;

import javax.ws.rs.HttpMethod;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

public class ExitThread extends Thread {

    @Override
    public void run() {
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
        boolean check = false;

        while(!check) {
            System.out.println("> Write \"quit\" to terminate Taxi.\n");

            try {
                check = inFromUser.readLine().equalsIgnoreCase("quit");
            }catch (Exception e){
                System.out.println("> An error occurred." + e.getMessage());
            }
        }

        try {
            stopTaxi();
        } catch (Exception e) {
            System.out.println("> An error occurred while trying to leave the network. "
                    + e.getMessage());
        }
        System.exit(0);
    }

    private void stopTaxi() throws Exception {
        TaxiInfoMsg taxiInfoMsg = TaxiInfoMsg.newBuilder()
                                .setId(TaxiUtils.getInstance().getTaxiInfo().getId())
                                .build();

        for (Taxi other : TaxiUtils.getInstance().getTaxisList()) {
            try {
                /* The taxi notifies the others present in its list */
                if(!other.getTaxiInfo().getId().equals(TaxiUtils.getInstance().getTaxiInfo().getId()))
                    notifyOtherTaxi(taxiInfoMsg, other);
            } catch (InterruptedException e) {
                System.out.println("> Couldn't notify the other taxi while trying to leave the network. "
                        + e.getMessage());
            }
        }

        deleteTaxi();
    }



    /* The taxi acts as a client and notifies the other taxis that it wants to leave the network. */
    public void notifyOtherTaxi(TaxiInfoMsg request , Taxi other) throws InterruptedException {
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(other.getTaxiInfo().getAddress()+":" + other.getTaxiInfo().getPort()).usePlaintext().build();

        TaxiRPCServiceGrpc.TaxiRPCServiceStub stub = TaxiRPCServiceGrpc.newStub(channel);

        stub.removeTaxi(request, new StreamObserver<Empty>() {
            @Override
            public void onNext(Empty value) {
                // The current taxi correctly notified the others
                System.out.println("> Other taxis correctly reached.");
            }

            @Override
            public void onError(Throwable t) {
                System.out.println("> An error occurred while trying to leave the network. Couldn't reach the other taxis.");

            }

            @Override
            public void onCompleted() {
                channel.shutdown();
            }
        });
        channel.awaitTermination(1, TimeUnit.SECONDS);
    }

    private void deleteTaxi() throws Exception {
        String path = Utils.servicesAddress + "taxis" + "/delete/" + TaxiUtils.getInstance().getTaxiInfo().getId();
        ClientResponse clientResponse = Utils.sendRequest(Client.create(), path, HttpMethod.DELETE);
        System.out.println("ok " + clientResponse +"\n");
        if (clientResponse == null){
            //TODO
        }
        int statusInfo = clientResponse.getStatus();

        if (ClientResponse.Status.OK.getStatusCode() == statusInfo) {
            //Taxi correctly deleted
            System.out.println("> Taxi correctly deleted from the network.");
        } else if (ClientResponse.Status.CONFLICT.getStatusCode() == statusInfo) {
            //Taxi not present
            throw new TaxiNotFoundException();
        }else {
            throw new Exception("> Status code: "+ statusInfo);
        }
    }
}
