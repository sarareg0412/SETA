package taxi.modules;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import exceptions.taxi.TaxiNotFoundException;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.eclipse.paho.client.mqttv3.MqttException;
import taxi.Taxi;
import taxi.TaxiUtils;
import unimi.dps.taxi.TaxiRPCServiceGrpc;
import unimi.dps.taxi.TaxiRPCServiceOuterClass.*;
import utils.Utils;

import javax.ws.rs.HttpMethod;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class StdInThread extends Thread {

    @Override
    public void run() {
        while (!TaxiUtils.getInstance().quit()) {
            BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
            boolean check = false;
            String line;
            int i = 0;
            while (!check) {
                System.out.println("> Write \"quit\" to terminate Taxi.");
                System.out.println("> Write \"recharge\" to recharge Taxi.");

                try {
                    line = inFromUser.readLine();
                    if (line.equalsIgnoreCase("quit")) {
                        check = true;
                        i = 1;
                    } else if (line.equalsIgnoreCase("recharge")) {
                        check = true;
                        i = 2;
                    }
                } catch (Exception e) {
                    System.out.println("> An error occurred." + e.getMessage());
                }
            }

            switch (i) {
                case 1:
                    try {

                        System.out.println("> [QUIT] Waiting for taxi to finish election ...");
                        while (TaxiUtils.getInstance().isInElection()) {
                            try {
                                synchronized (TaxiUtils.getInstance().getInElectionLock()) {
                                    TaxiUtils.getInstance().getInElectionLock().wait();
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        System.out.println("> [QUIT] Taxi is not in election anymore.");
                        // Waits until taxi is available
                        System.out.println("> [QUIT] Waiting for taxi to finish its work ...");
                        while (!TaxiUtils.getInstance().isAvailable()) {
                            try {
                                synchronized (TaxiUtils.getInstance().getAvailableLock()) {
                                    TaxiUtils.getInstance().getAvailableLock().wait();
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        System.out.println("> [QUIT] Taxi can now leave the network.");
                        stopTaxi();
                    } catch (Exception e) {
                        System.out.println("> An error occurred while trying to leave the network. Please try later. "
                                + e.getMessage());
                    }
                    break;
                case 2:
                    if (TaxiUtils.getInstance().wantsToCharge() || TaxiUtils.getInstance().isCharging()){
                        System.out.println("> [REC] Recharging process is already in place.");
                    }else {
                        System.out.println("> [RECH] Taxi wants to charge! ");
                        // Waits until taxi is available
                        while (!TaxiUtils.getInstance().isAvailable()) {
                            try {
                                synchronized (TaxiUtils.getInstance().getAvailableLock()) {
                                    TaxiUtils.getInstance().getAvailableLock().wait();
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        System.out.println("> [RECH] Taxi is now free to recharge ");
                        TaxiUtils.getInstance().setWantsToCharge(true);
                    }
                    break;
            }
        }
    }

    private void stopTaxi(){
        try {
            TaxiUtils.getInstance().setQuit(true);
            //Notifies Seta
            Utils.publishUnavailable(TaxiUtils.getInstance().getPosition(), TaxiUtils.getInstance().getMQTTClient(), TaxiUtils.getInstance().getQos());
            System.out.println("> [QUIT] Seta correctly notified. ");
            sendMessageToOthers();
            System.out.println("> [QUIT] Other taxis correctly notified. ");
            deleteTaxi();
            TaxiUtils.getInstance().getMQTTClient().disconnect();
            System.exit(0);
        } catch (InterruptedException e) {
            System.out.println("> Couldn't notify the other taxi while trying to leave the network.  Please try later. "
                    + e.getMessage());
        } catch (TaxiNotFoundException e) {
            System.out.println(e.getMessage() + " Please try later.");
        } catch (MqttException e) {
            System.out.println("> An error occurred while disconnecting the client. Please try later.");
        }
    }

    private void sendMessageToOthers() throws InterruptedException{
        TaxiInfoMsg taxiInfoMsg = TaxiInfoMsg.newBuilder()
                .setId(TaxiUtils.getInstance().getTaxiInfo().getId())
                .build();

        for (Taxi other : TaxiUtils.getInstance().getTaxisList()) {
                /* The taxi notifies the others present in its list */
                if(!other.getTaxiInfo().getId().equals(TaxiUtils.getInstance().getTaxiInfo().getId()))
                    notifyOtherTaxi(taxiInfoMsg, other);
        }
    }

    /* The taxi acts as a client and notifies the other taxis that it wants to leave the network. */
    public void notifyOtherTaxi(TaxiInfoMsg request , Taxi other){
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(other.getTaxiInfo().getAddress()+":" + other.getTaxiInfo().getPort()).usePlaintext().build();
        TaxiRPCServiceGrpc.TaxiRPCServiceBlockingStub stub = TaxiRPCServiceGrpc.newBlockingStub(channel);
        stub.removeTaxi(request);
        channel.shutdown();
    }

    private void deleteTaxi() throws TaxiNotFoundException{
        String path = Utils.SERVICES_ADDRESS + "taxis" + "/delete/" + TaxiUtils.getInstance().getTaxiInfo().getId();
        ClientResponse clientResponse = Utils.sendRequest(Client.create(), path, HttpMethod.DELETE);

        int statusInfo = clientResponse.getStatus();

        if (ClientResponse.Status.OK.getStatusCode() == statusInfo) {
            //Taxi correctly deleted
            System.out.println("> [QUIT] Taxi correctly deleted from the network.");
        } else if (ClientResponse.Status.CONFLICT.getStatusCode() == statusInfo) {
            //Taxi not present
            throw new TaxiNotFoundException();
        }else {
            System.out.println("> [QUIT] An error occurred while leaving the network. Status code: "+ statusInfo);
        }
    }
}
