package taxi.modules;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import taxi.TaxiInfo;
import unimi.dps.taxi.TaxiRPCServiceGrpc;
import unimi.dps.taxi.TaxiRPCServiceOuterClass.*;

public class ElectionThread extends Thread{

    TaxiInfo otherTaxiInfo;
    ElectionMessage electionMessage;

    public ElectionThread(TaxiInfo otherTaxiInfo, ElectionMessage electionMessage) {
        this.otherTaxiInfo = otherTaxiInfo;
        this.electionMessage = electionMessage;
    }

    @Override
    public void run() {

        reachOtherTaxi(otherTaxiInfo, electionMessage);
    }


    private void reachOtherTaxi(TaxiInfo other, ElectionMessage electionMessage){
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(other.getAddress()+":" + other.getPort()).usePlaintext().build();
        TaxiRPCServiceGrpc.TaxiRPCServiceBlockingStub stub = TaxiRPCServiceGrpc.newBlockingStub(channel);

        OKElection response = stub.startElection(electionMessage);
    }
}
