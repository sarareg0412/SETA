package taxi.modules;

import io.grpc.ServerBuilder;
import taxi.Taxi;
import taxi.TaxiInfo;
import taxi.TaxiRPCServiceImpl;

import java.io.IOException;

public class GRPCServerThread extends Thread {
    TaxiInfo taxiInfo;

    public GRPCServerThread(TaxiInfo taxiInfo) {
        this.taxiInfo = taxiInfo;
    }

    @Override
    public void run() {
        try {
            io.grpc.Server rpcServer = ServerBuilder.forPort(taxiInfo.getPort()).addService(new TaxiRPCServiceImpl()).build();
            rpcServer.start();
            rpcServer.awaitTermination();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
