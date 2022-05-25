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
    @Path("add")
    @POST
    @Consumes({"application/json", "application/xml"})
    @Produces({"application/json", "application/xml"})
    public Response addTaxi(TaxiInfo taxi){
        try {
            TaxiNetwork prev = TaxiNetwork.getInstance();
            TaxiNetwork.getInstance().addTaxiInfo(taxi);
            return Response.ok(prev).build();
        } catch (TaxiAlreadyPresentException e) {
            //Taxi already present
            return Response.status(Response.Status.CONFLICT).entity(e.getMessage()).build();
        }
    }

    /* Delete a taxi by its id */
    @Path("delete/{id}")
    @DELETE
    public Response deleteTaxi(@PathParam("id") String id){
        try {
            TaxiNetwork.getInstance().deleteTaxiInfoById(id);
            return Response.ok().build();
        } catch (Exception e) {
            //Taxi not found
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }
}
