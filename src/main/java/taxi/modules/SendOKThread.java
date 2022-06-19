package taxi.modules;

import com.google.protobuf.Empty;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import taxi.TaxiInfo;
import taxi.TaxiUtils;
import unimi.dps.taxi.TaxiRPCServiceGrpc;
import unimi.dps.taxi.TaxiRPCServiceOuterClass.*;
import utils.Utils;

import java.util.concurrent.TimeUnit;

public class SendOKThread extends Thread{
    TaxiInfo otherTaxiInfo;
    int service;
    public SendOKThread(TaxiInfo otherTaxiInfo, int service) {
        this.otherTaxiInfo = otherTaxiInfo;
        this.service = service;
    }

    @Override
    public void run() {
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(otherTaxiInfo.getAddress()+":" + otherTaxiInfo.getPort()).usePlaintext().build();
        TaxiRPCServiceGrpc.TaxiRPCServiceStub stub = TaxiRPCServiceGrpc.newStub(channel);
        OKMsg response = OKMsg.newBuilder().setOk("OK").setId(TaxiUtils.getInstance().getTaxiInfo().getId()).build();
        switch (service){
            case Utils.RECHARGE:
                stub.sendOkRecharge(response, new StreamObserver<Empty>() {
                            @Override
                            public void onNext(Empty value) {

                            }

                            @Override
                            public void onError(Throwable t) {
                                System.out.println("> [RECH] ERR: "+ t.getMessage());
                            }

                            @Override
                            public void onCompleted() {
                                channel.shutdown();
                            }
                        }
                );
                break;
            case Utils.ELECTION:
                stub.finishElection(response, new StreamObserver<Empty>() {
                            @Override
                            public void onNext(Empty value) {
                            }

                            @Override
                            public void onError(Throwable t) {
                                System.out.println("> [RECH] ERR: "+ t.getMessage());
                            }

                            @Override
                            public void onCompleted() {
                                channel.shutdown();
                            }
                        }
                );
        }
        try {
            channel.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            System.out.println("> [RECH] [ERR] An error occurred while waiting for the election channel to shutdown");
        }
    }
}
