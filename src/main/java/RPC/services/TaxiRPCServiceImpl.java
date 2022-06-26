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
    public void startElection(ElectionMsg request, StreamObserver<OKMsg> responseObserver) {
        //StringBuilder s = new StringBuilder().append("> [ELEC] Got an election request from "+ request.getId() + " for ride: " + request.getRide().getId());
        //System.out.println(s);
        // Current taxi is the same who sent the request
        if (request.getId().equals(TaxiUtils.getInstance().getTaxiInfo().getId())){
            //System.out.println(s.append("> RESPONSE: OK"));
            responseObserver.onNext(OKMsg.newBuilder().setOk("OK").build());
            responseObserver.onCompleted();
        }else {
            // Current taxi is not in the same district of the request, sends back OK
            if (!TaxiUtils.getInstance().isInTheSameDistrict(new Position(request.getRide().getStart()))) {
                //System.out.println(s.append("> RESPONSE: OK"));
                responseObserver.onNext(OKMsg.newBuilder().setOk("OK").build());
                responseObserver.onCompleted();
            } else {
                //Current taxi is available and not recharging
                if (TaxiUtils.getInstance().isAvailable() && !TaxiUtils.getInstance().wantsToCharge()) {

                    Position start = new Position(request.getRide().getStart().getX(), request.getRide().getStart().getY());
                    double distance = Utils.getDistanceBetweenPositions(TaxiUtils.getInstance().getPosition(), start);
                    if (distance == request.getDistance()) {
                        if (TaxiUtils.getInstance().getTaxiInfo().getId().compareToIgnoreCase(request.getId()) >= 0) {
                            //Requesting taxi has higher id or is the same one who sent the request, returns ok
                            //System.out.println(s.append("> RESPONSE: OK"));
                            responseObserver.onNext(OKMsg.newBuilder().setOk("OK").build());
                            responseObserver.onCompleted();
                        }else {
                            //Requesting taxi has lower id, returns KO
                            //System.out.println(s.append("> RESPONSE: KO"));
                            responseObserver.onNext(OKMsg.newBuilder().setOk("KO").build());
                            responseObserver.onCompleted();
                        }
                    } else {
                        if (distance < request.getDistance()) {
                            //Requesting taxi has lower distance, returns ok
                            //System.out.println(s.append("> RESPONSE: OK"));
                            responseObserver.onNext(OKMsg.newBuilder().setOk("OK").build());
                            responseObserver.onCompleted();
                        }else {
                            //Requesting taxi has Higher distance, returns KO
                            //System.out.println(s.append("> RESPONSE: KO"));
                            responseObserver.onNext(OKMsg.newBuilder().setOk("KO").build());
                            responseObserver.onCompleted();
                        }
                    }
                } else {
                    //Current taxi is not available, returns ok
                    //System.out.println(s.append("> RESPONSE: OK"));
                    responseObserver.onNext(OKMsg.newBuilder().setOk("OK").build());
                    responseObserver.onCompleted();
                }
            }
        }
    }

    @Override
    public void askRecharge(RechargeMsg request, StreamObserver<OKMsg> responseObserver) {
        String s ="> [RECH] Got a request to recharge from "+ request.getTaxiInfoMsg().getId();
        if (request.getTaxiInfoMsg().getId().equals(TaxiUtils.getInstance().getTaxiInfo().getId())){
            System.out.println(s+" RESPONSE: OK");
            responseObserver.onNext(OKMsg.newBuilder().setOk("OK").build());
            responseObserver.onCompleted();
        }else {
            // Current Taxi is not charging and doesn't want to, sends OK
            if (!TaxiUtils.getInstance().wantsToCharge() && !TaxiUtils.getInstance().isCharging()) {
                System.out.println(s+" RESPONSE: OK");
                responseObserver.onNext(OKMsg.newBuilder().setOk("OK").build());
                responseObserver.onCompleted();
            } else if (TaxiUtils.getInstance().wantsToCharge() && !TaxiUtils.getInstance().isCharging()) {
                // Current taxi wants to charge but is not charging yet
                if (request.getTimestamp() <= TaxiUtils.getInstance().getRechargeTimestamp()) {
                    System.out.println(s+" RESPONSE: OK");
                    // Request has lesser or equal timestamp
                    responseObserver.onNext(OKMsg.newBuilder().setOk("OK").build());
                    responseObserver.onCompleted();
                } else {
                    System.out.println(s+" QUEUING REQUEST.");
                    // Request has greater timestamp, add it to the queue
                    TaxiUtils.getInstance().getRechargeRequests().put(new TaxiInfo(request.getTaxiInfoMsg().getId(), request.getTaxiInfoMsg().getPort(), request.getTaxiInfoMsg().getAddress()));
                }
            } else if (TaxiUtils.getInstance().isCharging()) {
                System.out.println(s+" QUEUING REQUEST.");
                // Current Taxi is currently charging and got a request, add it to the queue
                TaxiUtils.getInstance().getRechargeRequests().put(new TaxiInfo(request.getTaxiInfoMsg().getId(), request.getTaxiInfoMsg().getPort(), request.getTaxiInfoMsg().getAddress()));
            }
        }
    }

    @Override
    public void sendOkRecharge(OKMsg request, StreamObserver<Empty> responseObserver) {
        // Requesting taxi recharged: adds the missing response to the counter
        TaxiUtils.getInstance().getRechargeCounter().addResponse();
        System.out.println("> [RECH] Taxi got OK to recharge from " + request.getId() );
        Empty empty = Empty.newBuilder().build();
        responseObserver.onNext(empty);     //Sends nothing back to the caller
        responseObserver.onCompleted();
    }

    @Override
    public void finishElection(FinishElectionMsg request, StreamObserver<Empty> responseObserver) {
        // Election is finished
        TaxiUtils.getInstance().setInElection(false);
        System.out.println("> [ELEC] Ride " + request.getRideId() + " taken by Taxi " + request.getId());
        Empty empty = Empty.newBuilder().build();
        responseObserver.onNext(empty);     //Sends nothing back to the caller
        responseObserver.onCompleted();
    }
}
