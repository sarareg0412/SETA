package taxi.modules.recharge;

import com.google.protobuf.Empty;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import taxi.TaxiInfo;
import unimi.dps.taxi.TaxiRPCServiceGrpc;
import unimi.dps.taxi.TaxiRPCServiceOuterClass.*;

import java.util.concurrent.TimeUnit;

public class SendOKThread extends Thread{
    TaxiInfo otherTaxiInfo;
    OKMsg msg;

    public SendOKThread(TaxiInfo otherTaxiInfo,OKMsg msg) {
        this.otherTaxiInfo = otherTaxiInfo;
        this.msg = msg;
    }

    @Override
    public void run() {
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(otherTaxiInfo.getAddress()+":" + otherTaxiInfo.getPort()).usePlaintext().build();
        TaxiRPCServiceGrpc.TaxiRPCServiceStub stub = TaxiRPCServiceGrpc.newStub(channel);

        stub.sendOkRecharge(msg, new StreamObserver<Empty>() {
                    @Override
                    public void onNext(Empty value) {

                    }

                    @Override
                    public void onError(Throwable t) {
                        System.out.println("> [RECH] ERR: " + t.getMessage());
                        channel.shutdown();
                    }

                    @Override
                    public void onCompleted() {
                        channel.shutdown();
                    }
                }
        );
        try {
            channel.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            System.out.println("> [RECH] [ERR] An error occurred while waiting for the election channel to shutdown");
        }
    }
}
