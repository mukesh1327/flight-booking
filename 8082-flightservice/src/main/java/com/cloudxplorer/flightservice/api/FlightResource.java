package com.cloudxplorer.flightservice.api;

import com.cloudxplorer.flightservice.application.FlightApplicationService;
import com.cloudxplorer.flightservice.domain.ActorType;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;

@Path("/api/v1")
@Produces(MediaType.APPLICATION_JSON)
public class FlightResource {

  private final FlightApplicationService service;

  public FlightResource(FlightApplicationService service) {
    this.service = service;
  }

  @GET
  @Path("/flights/search")
  public Response searchFlights(
      @QueryParam("from") String from,
      @QueryParam("to") String to,
      @QueryParam("date") String date,
      @HeaderParam("X-Actor-Type") String actorType) {
    return Response.ok(service.searchFlights(from, to, date, ActorType.fromHeader(actorType))).build();
  }

  @GET
  @Path("/flights/{flightId}")
  public Response getFlight(@PathParam("flightId") String flightId) {
    return Response.ok(service.getFlight(flightId)).build();
  }

  @GET
  @Path("/flights/{flightId}/availability")
  public Response availability(@PathParam("flightId") String flightId) {
    return Response.ok(service.availability(flightId)).build();
  }

  @POST
  @Path("/pricing/quote")
  public Response quote(Map<String, Object> request) {
    return Response.ok(service.quote(request)).build();
  }

  @POST
  @Path("/inventory/hold")
  public Response hold(Map<String, Object> request) {
    return Response.ok(service.hold(request)).build();
  }

  @POST
  @Path("/inventory/release")
  public Response release(Map<String, Object> request) {
    return Response.ok(service.release(request)).build();
  }

  @POST
  @Path("/inventory/commit")
  public Response commit(Map<String, Object> request) {
    return Response.ok(service.commit(request)).build();
  }
}
