package services.taxi;

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
        System.out.println("> [INS] New taxi added. Taxi " + TaxiUtils.getInstance().getTaxiInfo().getId() + " other taxis :" + Utils.printTaxiList(TaxiUtils.getInstance().getTaxisList()));

        Empty empty = Empty.newBuilder().build();
        responseObserver.onNext(empty);     //Sends nothing back to the caller
        responseObserver.onCompleted();
    }

    @Override
    public void removeTaxi(TaxiInfoMsg request, StreamObserver<Empty> responseObserver) {

        Empty empty = Empty.newBuilder().build();
        try {
            TaxiUtils.getInstance().removeTaxiFromList(request.getId());
            System.out.println("> [QUIT] Taxi " + request.getId() +" left the network. Taxi " + TaxiUtils.getInstance().getTaxiInfo().getId() + " other taxis :" + Utils.printTaxiList(TaxiUtils.getInstance().getTaxisList()));
            responseObserver.onNext(empty);     //Sends nothing back to the caller
        } catch (TaxiNotFoundException e) {
            responseObserver.onError(new TaxiNotFoundException());
        }
        responseObserver.onCompleted();
    }

    @Override
    public void startElection(ElectionMsg request, StreamObserver<OKMsg> responseObserver) {
        StringBuilder s = new StringBuilder().append("> [ELEC] Got an election request from "+ request.getId() + " for ride: " + request.getRide().getId());
        OKMsg msg = null;
        //System.out.println(s);
        if (TaxiUtils.getInstance().isAvailable() && !TaxiUtils.getInstance().wantsToCharge()) {
            //Current taxi is available and doesn't want to recharge
            Position start = new Position(request.getRide().getStart().getX(), request.getRide().getStart().getY());
            if(TaxiUtils.getInstance().isInTheSameDistrict(start)) {

                double distance = Utils.getDistanceBetweenPositions(TaxiUtils.getInstance().getPosition(), start);
                if (distance == request.getDistance()) {
                    if (TaxiUtils.getInstance().getTaxiInfo().getId().compareToIgnoreCase(request.getId()) >= 0) {
                        //Requesting taxi has higher id or is the same one who sent the request, returns ok
                        //System.out.println(s.append(" Equal distance. id:" + (TaxiUtils.getInstance().getTaxiInfo().getId() + " RESPONSE: OK")));
                        msg = OKMsg.newBuilder().setOk("OK").build();
                    } else {
                        //Requesting taxi has lower id, returns KO
                        //System.out.println(s.append(" Equal distance. id:" + (TaxiUtils.getInstance().getTaxiInfo().getId() + " RESPONSE: KO")));
                        msg = OKMsg.newBuilder().setOk("KO").build();
                    }
                } else {
                    if (distance < request.getDistance()) {
                        //Requesting taxi has lower distance, returns ok
                        //System.out.println(s.append(" Lower distance. RESPONSE: OK"));
                        msg = OKMsg.newBuilder().setOk("OK").build();
                    } else {
                        //Requesting taxi has Higher distance, returns KO
                        //System.out.println(s.append(" Higher distance. RESPONSE: KO"));
                        msg = OKMsg.newBuilder().setOk("KO").build();
                    }
                }
            }else {
                // Current taxi is in another district, returns ok
                //System.out.println(s.append(" different district. RESPONSE: OK"));
                msg = OKMsg.newBuilder().setOk("OK").build();
            }

        } else {
            //Current taxi is not available or wants to recharge, returns ok if id is different
            if (!TaxiUtils.getInstance().wantsToCharge()) {
                //Current taxi doesn't want to recharge, check on the ride because of delays
                if (TaxiUtils.getInstance().getCurrentRide().equals(request.getRide().getId())) {
                    //System.out.println(s.append(" Not available, same ride id:" + request.getRide().getId() + ". RESPONSE: KO"));
                    msg = OKMsg.newBuilder().setOk("KO").build();
                } else {
                    //System.out.println(s.append(" Not available, different ride. RESPONSE: OK"));
                    msg = OKMsg.newBuilder().setOk("OK").build();
                }
            }else {
                //Current taxi wants to recharge, returns OK
                //System.out.println(s.append(" Not available I want to recharge. RESPONSE: OK"));
                msg = OKMsg.newBuilder().setOk("OK").build();
            }
        }

        responseObserver.onNext(msg);
        responseObserver.onCompleted();
    }

    @Override
    public void askRecharge(RechargeMsg request, StreamObserver<OKMsg> responseObserver) {
        if (request.getTaxiInfoMsg().getId().equals(TaxiUtils.getInstance().getTaxiInfo().getId())){
            responseObserver.onNext(OKMsg.newBuilder().setOk("OK").build());
            responseObserver.onCompleted();
        }else {
            // Current taxi is not in the same district of the request, sends back OK
            if (!TaxiUtils.getInstance().isInTheSameDistrict(new Position(request.getTaxiInfoMsg().getPosition()))) {
                responseObserver.onNext(OKMsg.newBuilder().setOk("OK").build());
                responseObserver.onCompleted();
            } else {
                // Current Taxi is not charging and doesn't want to, sends OK
                if (!TaxiUtils.getInstance().wantsToCharge() && !TaxiUtils.getInstance().isCharging()) {
                    responseObserver.onNext(OKMsg.newBuilder().setOk("OK").build());
                    responseObserver.onCompleted();
                } else if (TaxiUtils.getInstance().wantsToCharge() && !TaxiUtils.getInstance().isCharging()) {
                    // Current taxi wants to charge but is not charging yet
                    if (request.getTimestamp() <= TaxiUtils.getInstance().getRechargeTimestamp()) {
                        // Request has lesser or equal timestamp
                        responseObserver.onNext(OKMsg.newBuilder().setOk("OK").build());
                        responseObserver.onCompleted();
                    } else {
                        System.out.println("> [RECH] Got a request to recharge from " + request.getTaxiInfoMsg().getId() + " QUEUING REQUEST.");
                        // Request has greater timestamp, add it to the queue
                        TaxiUtils.getInstance().getRechargeRequests().put(new TaxiInfo(request.getTaxiInfoMsg().getId(), request.getTaxiInfoMsg().getPort(), request.getTaxiInfoMsg().getAddress()));
                    }
                } else if (TaxiUtils.getInstance().isCharging()) {
                    System.out.println("> [RECH] Got a request to recharge from " + request.getTaxiInfoMsg().getId() + " QUEUING REQUEST.");
                    // Current Taxi is currently charging and got a request, add it to the queue
                    TaxiUtils.getInstance().getRechargeRequests().put(new TaxiInfo(request.getTaxiInfoMsg().getId(), request.getTaxiInfoMsg().getPort(), request.getTaxiInfoMsg().getAddress()));
                }
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
}
