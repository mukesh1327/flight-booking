package com.cloudxplorer.bookingservice.model;

import java.util.List;

public class ConfirmBookingResponse {
    public String pnr;
    public String bookingStatus;
    public String paymentStatus;
    public List<String> ticketNumbers;

    public ConfirmBookingResponse() {
    }

    public ConfirmBookingResponse(String pnr, String bookingStatus, String paymentStatus, List<String> ticketNumbers) {
        this.pnr = pnr;
        this.bookingStatus = bookingStatus;
        this.paymentStatus = paymentStatus;
        this.ticketNumbers = ticketNumbers;
    }
}
