package admin;

import exceptions.taxi.TaxiAlreadyPresentException;
import exceptions.taxi.TaxiNotFoundException;
import taxi.*;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

@Path("taxis")
public class TaxiService {

    /* Return the list of taxis in the network  */
    @Path("getTaxiList")
    @GET
    @Produces({"application/json", "application/xml"})
    public Response getTaxiList(){
        TaxiResponse response = TaxiNetwork.getInstance().getTaxiInfoList();
        return Response.ok(response).build();
    }

    /* Insert the new taxi in the network */
    @Path("add")
    @POST
    @Consumes({"application/json", "application/xml"})
    @Produces({"application/json", "application/xml"})
    public Response addTaxi(TaxiInfo taxi){
        try {
            TaxiResponse response = TaxiNetwork.getInstance().addTaxiInfo(taxi);        // Add the new taxi to the network
            return Response.ok(response).build();
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
        } catch (TaxiNotFoundException e) {
            //Taxi not found
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }
}
