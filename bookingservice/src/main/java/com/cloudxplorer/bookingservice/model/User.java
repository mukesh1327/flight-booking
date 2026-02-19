package com.cloudxplorer.bookingservice.model;

import java.util.List;

public class User {
    public String userId;
    public String flightId;
    public String travelDate;
    public List<Passenger> passengers;
    public Contact contact;

    public User() {
    }
}
