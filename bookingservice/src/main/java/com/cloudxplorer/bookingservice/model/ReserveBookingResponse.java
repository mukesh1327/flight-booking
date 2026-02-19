package com.cloudxplorer.bookingservice.model;

public class ReserveBookingResponse {
    public String pnr;
    public String bookingStatus;
    public String holdId;
    public int expiresIn;

    public ReserveBookingResponse() {
    }

    public ReserveBookingResponse(String pnr, String bookingStatus, String holdId, int expiresIn) {
        this.pnr = pnr;
        this.bookingStatus = bookingStatus;
        this.holdId = holdId;
        this.expiresIn = expiresIn;
    }
}
