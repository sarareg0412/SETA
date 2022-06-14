package RPC.services;

import com.google.protobuf.Empty;
import exceptions.taxi.TaxiNotFoundException;
import io.grpc.stub.StreamObserver;
import taxi.Taxi;
import taxi.TaxiInfo;
import taxi.TaxiUtils;
import unimi.dps.taxi.TaxiRPCServiceGrpc.TaxiRPCServiceImplBase;
import unimi.dps.taxi.TaxiRPCServiceOuterClass.*;
import utils.Position;
import utils.Utils;

public class TaxiRPCServiceImpl extends TaxiRPCServiceImplBase {

    /* Method used to add the new taxi to the one already present in the network */
    @Override
    public void addTaxi(TaxiInfoMsg request, StreamObserver<Empty> responseObserver) {
        //The new taxi is created from the response
        Taxi newTaxi = new Taxi(new TaxiInfo(request.getId(),request.getPort(), request.getAddress()));
                       //         new Position(request.getPosition().getX(), request.getPosition().getY()));

        //New taxi is added to the current taxi's list
        TaxiUtils.getInstance().addNewTaxiToList(newTaxi);
        System.out.println("Taxi " + TaxiUtils.getInstance().getTaxiInfo().getId() + " other taxis :" + Utils.printTaxiList(TaxiUtils.getInstance().getTaxisList()));

        Empty empty = Empty.newBuilder().build();
        responseObserver.onNext(empty);     //Sends nothing back to the caller
        responseObserver.onCompleted();
    }

    @Override
    public void removeTaxi(TaxiInfoMsg request, StreamObserver<Empty> responseObserver) {

        Empty empty = Empty.newBuilder().build();
        try {
            TaxiUtils.getInstance().removeTaxiFromList(request.getId());
            System.out.println("Taxi " + TaxiUtils.getInstance().getTaxiInfo().getId() + " other taxis :" + Utils.printTaxiList(TaxiUtils.getInstance().getTaxisList()));
            responseObserver.onNext(empty);     //Sends nothing back to the caller
        } catch (TaxiNotFoundException e) {
            responseObserver.onError(new TaxiNotFoundException());
        }

        responseObserver.onCompleted();
    }

    @Override
    public void startElection(ElectionMsg request, StreamObserver<OKElection> responseObserver) {
        OKElection response;
        System.out.println("> Taxi " + TaxiUtils.getInstance().getTaxiInfo().getId() + " got an election mgs for ride: " + request.getRide().getId());
        // Current taxi is not in the same district, sends back OK
        if (!TaxiUtils.getInstance().isInTheSameDistrict(new Position(request.getRide().getStart()))){
            response = OKElection.newBuilder().setOk("OK").build();
        }else{
            //Current taxi is available and not recharging
            if (TaxiUtils.getInstance().isAvailable() && !TaxiUtils.getInstance().isCharging()){
                Position start = new Position(request.getRide().getStart().getX(), request.getRide().getStart().getY());
                double distance = Utils.getDistanceBetweenPositions(TaxiUtils.getInstance().getPosition(), start);
                if (distance == request.getDistance() ){
                    if (TaxiUtils.getInstance().getTaxiInfo().getId().compareToIgnoreCase(request.getId()) < 0){
                        //Requesting taxi has greater id, returns KO
                        response =  OKElection.newBuilder().setOk("KO").build();
                    }else {
                        //Requesting taxi has higher id or is the same one who sent the request, returns ok
                        response = OKElection.newBuilder().setOk("OK").build();
                    }
                }else {
                    if (distance < request.getDistance() ){
                        //Requesting taxi has lower distance, returns ok
                        response = OKElection.newBuilder().setOk("OK").build();
                    }else {
                        //Requesting taxi has higher distance, returns KO
                        response =  OKElection.newBuilder().setOk("KO").build();
                    }
                }
            }else {
                //Current taxi is not available, returns ok
                response = OKElection.newBuilder().setOk("OK").build();
            }
        }
        System.out.println("> Taxi " + TaxiUtils.getInstance().getTaxiInfo().getId() + " response to election: " + response);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
