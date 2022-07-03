package admin;

import com.sun.jersey.api.container.httpserver.HttpServerFactory;
import com.sun.net.httpserver.HttpServer;
import utils.Utils;

import java.io.IOException;

public class AdministratorServer{

    public static void main(String argv[]) throws IOException {
        HttpServer httpServer = HttpServerFactory.create(Utils.SERVICES_ADDRESS);
        httpServer.start();

        System.out.println("> Administrator Server running!");
        System.out.println("> Press ENTER to stop.");
        System.in.read();
        httpServer.stop(0);
        System.out.println("> Administrator Server stopped.");
    }
}
