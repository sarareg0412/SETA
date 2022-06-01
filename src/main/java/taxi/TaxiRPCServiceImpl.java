package taxi;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import unimi.dps.taxi.TaxiRPCServiceGrpc.TaxiRPCServiceImplBase;
import unimi.dps.taxi.TaxiRPCServiceOuterClass;

public class TaxiRPCServiceImpl extends TaxiRPCServiceImplBase {

    /* Method used to add the new taxi to the one already present in the network */
    @Override
    public void newTaxi(TaxiRPCServiceOuterClass.TaxiInfoMsg request, StreamObserver<Empty> responseObserver) {
        Empty empty = Empty.newBuilder().build();
        responseObserver.onNext(empty);     //Sends nothing back to the caller
        responseObserver.onCompleted();
    }
}
