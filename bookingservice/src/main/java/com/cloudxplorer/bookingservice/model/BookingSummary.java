package com.cloudxplorer.bookingservice.model;

public class BookingSummary {
    public String pnr;
    public String bookingStatus;

    public BookingSummary() {
    }

    public BookingSummary(String pnr, String bookingStatus) {
        this.pnr = pnr;
        this.bookingStatus = bookingStatus;
    }
}
