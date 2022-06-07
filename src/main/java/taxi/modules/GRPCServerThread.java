package taxi.modules;

import io.grpc.ServerBuilder;
import taxi.Taxi;
import RPC.services.TaxiRPCServiceImpl;

import java.io.IOException;

public class GRPCServerThread extends Thread {
    private Taxi taxi;

    public GRPCServerThread(Taxi taxi) {
        this.taxi = taxi;
    }

    @Override
    public void run() {
        try {
            io.grpc.Server rpcServer = ServerBuilder.forPort(taxi.getTaxiInfo().getPort())
                                                    .addService(new TaxiRPCServiceImpl(taxi)).build();
            rpcServer.start();
            rpcServer.awaitTermination();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
