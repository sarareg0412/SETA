package taxi.modules;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import taxi.Taxi;
import taxi.TaxiInfo;
import taxi.TaxiUtils;
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
        // Blocking stub for the thread waits for the other taxi's response
        OKElection response = stub.startElection(electionMessage);

        if (response.getOk().equals("KO")) {
            TaxiUtils.getInstance().setMaster(false);
        }else{   // Election counter updated
            TaxiUtils.getInstance().setElectionCounter(TaxiUtils.getInstance().getElectionCounter() + 1);
        }
        System.out.println("> Taxi " + TaxiUtils.getInstance().getTaxiInfo().getId() + " counter: " + TaxiUtils.getInstance().getElectionCounter());
    }


}
