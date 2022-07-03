package taxi.modules.recharge;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import taxi.TaxiInfo;
import taxi.TaxiUtils;
import unimi.dps.taxi.TaxiRPCServiceGrpc;
import unimi.dps.taxi.TaxiRPCServiceOuterClass.*;

import java.util.concurrent.TimeUnit;

/* Thread used to ask each taxi if the current one can recharge */
public class AskRechargeThread extends Thread{
    TaxiInfo otherTaxiInfo;
    RechargeMsg rechargeMsg;

    public AskRechargeThread(TaxiInfo otherTaxiInfo, RechargeMsg rechargeMsg) {
        this.otherTaxiInfo = otherTaxiInfo;
        this.rechargeMsg = rechargeMsg;
    }

    @Override
    public void run() {
        reachOtherTaxi(otherTaxiInfo, rechargeMsg);
    }

    private void reachOtherTaxi(TaxiInfo other, RechargeMsg rechargeMsg){
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(other.getAddress()+":" + other.getPort()).usePlaintext().build();
        TaxiRPCServiceGrpc.TaxiRPCServiceStub stub = TaxiRPCServiceGrpc.newStub(channel);
        // Asyncronous stub for the thread can receive no response
        stub.askRecharge(rechargeMsg, new StreamObserver<OKMsg>() {
            @Override
            public void onNext(OKMsg response) {
                if (response.getOk().equals("OK")) {
                    TaxiUtils.getInstance().getRechargeCounter().addResponse();
                }
            }

            @Override
            public void onError(Throwable t) {
                channel.shutdown();
            }

            @Override
            public void onCompleted() {
                channel.shutdown();
            }
        });

        try {
            channel.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
