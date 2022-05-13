package admin;

import exceptions.taxi.TaxiAlreadyPresentException;
import taxi.*;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

@Path("taxis")
public class TaxiService {

    /* Return the list of taxis in the network  */
    @GET
    @Produces({"application/json", "application/xml"})
    public Response getTaxiList(){
        return Response.ok(TaxiNetwork.getInstance()).build();
    }

    /* Insert the new taxi in the network */
    @POST
    @Consumes({"application/json", "application/xml"})
    public Response addTaxi(Taxi taxi){
        try {
            TaxiNetwork.getInstance().addTaxi(taxi);
            return Response.ok().build();
        } catch (TaxiAlreadyPresentException e) {
            //Taxi already present
            return Response.status(Response.Status.CONFLICT).build();
        }
    }
}
