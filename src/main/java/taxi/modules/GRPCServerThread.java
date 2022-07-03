package taxi.modules;

import io.grpc.ServerBuilder;
import services.taxi.TaxiRPCServiceImpl;
import taxi.TaxiUtils;

import java.io.IOException;

/* Thread used to start the GRPC server */
public class GRPCServerThread extends Thread {

    @Override
    public void run() {
        try {
            io.grpc.Server rpcServer = ServerBuilder.forPort(TaxiUtils.getInstance().getTaxiInfo().getPort())
                                                    .addService(new TaxiRPCServiceImpl()).build();
            rpcServer.start();
            rpcServer.awaitTermination();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
