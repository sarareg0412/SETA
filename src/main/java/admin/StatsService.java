package admin;

import exceptions.taxi.TaxiNotFoundException;
import statistics.Statistics;
import statistics.StatsResponse;
import statistics.Stats;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

@Path("stats")
public class StatsService {

    /* Adds the statistics of a given taxi to the map */
    @Path("add")
    @POST
    @Consumes({"application/json", "application/xml"})
    @Produces({"application/json", "application/xml"})
    public Response addStats(Stats stats){
        Statistics.getInstance().addStatistics(stats);
        return Response.ok().build();
    }

    /* Return the list of the statistics of a taxi  */
    @Path("getTaxiStats/{id}")
    @GET
    @Produces({"application/json", "application/xml"})
    public Response getTaxiStats(@PathParam("id") String id){
        try {
            StatsResponse response = new StatsResponse(Statistics.getInstance().getStatsBytaxiId(id));
            return Response.ok(response).build();
        } catch (TaxiNotFoundException e) {
            //Taxi not found
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }


}
