package admin;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.container.httpserver.HttpServerFactory;
import com.sun.net.httpserver.HttpServer;
import taxi.TaxiInfo;
import taxi.TaxiNetwork;
import utils.Utils;

import java.io.IOException;

public class AdministratorServer{

    public static void main(String argv[]) throws IOException {
        initialize();
        HttpServer server = HttpServerFactory.create(Utils.taxiServiceAddress);
        server.start();

        System.out.println("Administrator Server running!");
    }

    public static void initialize(){
    }
}
