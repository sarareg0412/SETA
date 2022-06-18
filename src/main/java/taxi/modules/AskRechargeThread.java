package taxi.modules;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import taxi.TaxiInfo;
import taxi.TaxiUtils;
import unimi.dps.taxi.TaxiRPCServiceGrpc;
import unimi.dps.taxi.TaxiRPCServiceOuterClass.*;
import utils.Counter;

import java.util.concurrent.TimeUnit;

public class AskRechargeThread extends Thread{
    Counter counter;
    TaxiInfo otherTaxiInfo;
    RechargeMsg rechargeMsg;

    public AskRechargeThread(TaxiInfo otherTaxiInfo, RechargeMsg rechargeMsg) {
        this.otherTaxiInfo = otherTaxiInfo;
        this.rechargeMsg = rechargeMsg;
    }

    @Override
    public void run() {
        reachOtherTaxi(otherTaxiInfo, rechargeMsg, counter);
    }
    private void reachOtherTaxi(TaxiInfo other, RechargeMsg electionMessage, Counter counter){
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(other.getAddress()+":" + other.getPort()).usePlaintext().build();
        TaxiRPCServiceGrpc.TaxiRPCServiceBlockingStub stub = TaxiRPCServiceGrpc.newBlockingStub(channel);
        // Blocking stub for the thread waits for the other taxi's response
        OKMsg response = stub.askRecharge(electionMessage);

        try {
            channel.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            System.out.println("> [ERR] An error occurred while waiting for the recharging channel request to shutdown");
        }

        System.out.println("> Taxi " + TaxiUtils.getInstance().getTaxiInfo().getId() + " counter: " + TaxiUtils.getInstance().getRechargeCounter().getResponses());
    }
}
