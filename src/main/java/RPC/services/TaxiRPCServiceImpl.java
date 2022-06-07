package RPC.services;

import com.google.protobuf.Empty;
import exceptions.taxi.TaxiNotFoundException;
import io.grpc.stub.StreamObserver;
import taxi.Taxi;
import taxi.TaxiInfo;
import unimi.dps.taxi.TaxiRPCServiceGrpc.TaxiRPCServiceImplBase;
import unimi.dps.taxi.TaxiRPCServiceOuterClass.*;
import utils.Position;

import java.util.ArrayList;

public class TaxiRPCServiceImpl extends TaxiRPCServiceImplBase {

    private Taxi taxi;

    public TaxiRPCServiceImpl(Taxi taxi) {
        this.taxi = taxi;
    }

    /* Method used to add the new taxi to the one already present in the network */
    @Override
    public void addTaxi(TaxiInfoMsg request, StreamObserver<Empty> responseObserver) {
        //The new taxi is created from the response
        Taxi newTaxi = new Taxi(new TaxiInfo(request.getId(),request.getPort(), request.getAddress()),
                                new Position(request.getPosition().getX(), request.getPosition().getY()));

        if (taxi.getTaxisList() == null)
            taxi.setTaxisList(new ArrayList<>());
        //New taxi is added to the current taxi's list
        taxi.addNewTaxiToList(newTaxi);
        System.out.println("Taxi " + taxi.getTaxiInfo().getId() + " other taxis :" + taxi.printTaxiList());

        Empty empty = Empty.newBuilder().build();
        responseObserver.onNext(empty);     //Sends nothing back to the caller
        responseObserver.onCompleted();
    }

    @Override
    public void removeTaxi(TaxiInfoMsg request, StreamObserver<Empty> responseObserver) {

        Empty empty = Empty.newBuilder().build();
        try {
            taxi.removeTaxiFromList(request.getId());
            System.out.println("Taxi " + taxi.getTaxiInfo().getId() + " other taxis :" + taxi.printTaxiList());
            responseObserver.onNext(empty);     //Sends nothing back to the caller
        } catch (TaxiNotFoundException e) {
            responseObserver.onError(new TaxiNotFoundException());
        }

        responseObserver.onCompleted();
    }
}
