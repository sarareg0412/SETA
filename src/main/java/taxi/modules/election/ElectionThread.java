package taxi.modules.election;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import taxi.TaxiInfo;
import unimi.dps.taxi.TaxiRPCServiceGrpc;
import unimi.dps.taxi.TaxiRPCServiceOuterClass.*;
import utils.Counter;

/* Thread in charge of notifying a taxi that an election is in place, and to
 * register the response */
public class ElectionThread extends Thread{

    TaxiInfo otherTaxiInfo;
    ElectionMsg electionMessage;
    Counter electionCounter;

    public ElectionThread(TaxiInfo otherTaxiInfo, ElectionMsg electionMessage, Counter electionCounter) {
        this.otherTaxiInfo = otherTaxiInfo;
        this.electionMessage = electionMessage;
        this.electionCounter = electionCounter;
    }

    @Override
    public void run() {
        reachOtherTaxi(otherTaxiInfo, electionMessage);
    }


    private void reachOtherTaxi(TaxiInfo other, ElectionMsg electionMessage){
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(other.getAddress()+":" + other.getPort()).usePlaintext().build();
        TaxiRPCServiceGrpc.TaxiRPCServiceBlockingStub stub = TaxiRPCServiceGrpc.newBlockingStub(channel);
        // Blocking stub for the thread waits for the other taxi's response
        OKMsg response = stub.startElection(electionMessage);
        if (response.getOk().equals("OK")) {
            electionCounter.addResponse();
        }
        channel.shutdown();
    }


}
