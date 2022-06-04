package admin;

import com.sun.jersey.api.container.httpserver.HttpServerFactory;
import com.sun.net.httpserver.HttpServer;
import utils.Utils;

import java.io.IOException;

public class AdministratorServer{

    public static void main(String argv[]) throws IOException {
        initialize();
        HttpServer httpServer = HttpServerFactory.create(Utils.servicesAddress);
        httpServer.start();

        System.out.println("Administrator Server running!");
    }

    public static void initialize(){
    }
}
