package taxi;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import unimi.dps.taxi.TaxiRPCServiceGrpc.TaxiRPCServiceImplBase;
import unimi.dps.taxi.TaxiRPCServiceOuterClass;

public class TaxiRPCServiceImpl extends TaxiRPCServiceImplBase {

    /* Method used to notify the taxis already present that a new one
    * has been added to the network */
    @Override
    public void newTaxi(TaxiRPCServiceOuterClass.TaxiInfoMsg request, StreamObserver<Empty> responseObserver) {
        super.newTaxi(request, responseObserver);
    }
}
