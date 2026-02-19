package com.cloudxplorer.bookingservice.controller;

import com.cloudxplorer.bookingservice.model.BookingSummary;
import com.cloudxplorer.bookingservice.model.ConfirmBookingResponse;
import com.cloudxplorer.bookingservice.model.LoginRequest;
import com.cloudxplorer.bookingservice.model.ReserveBookingResponse;
import com.cloudxplorer.bookingservice.model.User;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Path("/booking")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    private static final Map<String, BookingSummary> BOOKINGS = new ConcurrentHashMap<>();

    @GET
    @Path("/health")
    public Response health() {
        return Response.ok(Map.of(
                "service", "bookingservice",
                "status", "UP"
        )).build();
    }

    @GET
    @Path("/my-bookings")
    public Response getMyBookings() {
        return Response.ok(Map.of("bookings", new ArrayList<>(BOOKINGS.values()))).build();
    }

    @GET
    @Path("/{pnr}")
    public Response getBookingByPnr(@PathParam("pnr") String pnr) {
        BookingSummary booking = BOOKINGS.get(pnr);
        if (booking == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("message", "booking not found", "pnr", pnr))
                    .build();
        }
        return Response.ok(booking).build();
    }

    @POST
    @Path("/reserve")
    public Response reserve(User request) {
        if (request == null || request.flightId == null || request.flightId.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "flightId is required"))
                    .build();
        }

        String pnr = createPnr();
        String holdId = "hold_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        BOOKINGS.put(pnr, new BookingSummary(pnr, "RESERVED"));

        ReserveBookingResponse response = new ReserveBookingResponse(pnr, "RESERVED", holdId, 600);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @POST
    @Path("/confirm/{pnr}")
    public Response confirmBooking(@PathParam("pnr") String pnr, LoginRequest request) {
        BookingSummary booking = BOOKINGS.get(pnr);
        if (booking == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("message", "booking not found", "pnr", pnr))
                    .build();
        }

        if (request == null || request.amount <= 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "valid payment payload is required"))
                    .build();
        }

        booking.bookingStatus = "CONFIRMED";

        ConfirmBookingResponse response = new ConfirmBookingResponse(
                pnr,
                "CONFIRMED",
                "CAPTURED",
                List.of("0987654321123")
        );

        return Response.ok(response).build();
    }

    @PUT
    @Path("/{pnr}/cancel")
    public Response cancelBooking(@PathParam("pnr") String pnr) {
        BookingSummary booking = BOOKINGS.get(pnr);
        if (booking == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("message", "booking not found", "pnr", pnr))
                    .build();
        }

        booking.bookingStatus = "CANCELLED";
        return Response.ok(Map.of("pnr", pnr, "status", "CANCELLED")).build();
    }

    private String createPnr() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 5).toUpperCase();
        return "PNR" + suffix;
    }
}
