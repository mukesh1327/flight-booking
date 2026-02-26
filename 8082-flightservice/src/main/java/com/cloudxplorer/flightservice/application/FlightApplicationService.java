package com.cloudxplorer.flightservice.application;

import com.cloudxplorer.flightservice.domain.ActorType;
import com.cloudxplorer.flightservice.domain.Flight;
import com.cloudxplorer.flightservice.domain.FlightRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class FlightApplicationService {

  private final FlightRepository repository;

  public FlightApplicationService(FlightRepository repository) {
    this.repository = repository;
  }

  public Map<String, Object> searchFlights(String from, String to, String date, ActorType actorType) {
    List<Flight> flights = repository.search(from, to, date);
    return Map.of(
        "actorType", actorType.name().toLowerCase(),
        "criteria", Map.of("from", safe(from), "to", safe(to), "date", safe(date)),
        "count", flights.size(),
        "flights", flights);
  }

  public Flight getFlight(String flightId) {
    return repository.findById(flightId).orElseThrow(() -> new IllegalArgumentException("flight not found: " + flightId));
  }

  public Map<String, Object> availability(String flightId) {
    Flight flight = getFlight(flightId);
    return Map.of("flightId", flight.flightId(), "availableSeats", flight.availableSeats(), "status", "AVAILABLE");
  }

  public Map<String, Object> quote(Map<String, Object> request) {
    Object flightId = request.get("flightId");
    Object seats = request.getOrDefault("seatCount", 1);
    Flight flight = getFlight(String.valueOf(flightId));
    int seatCount = Integer.parseInt(String.valueOf(seats));
    int total = seatCount * flight.baseFare();
    return Map.of("flightId", flight.flightId(), "currency", "INR", "seatCount", seatCount, "totalAmount", total);
  }

  public Map<String, Object> hold(Map<String, Object> request) {
    return Map.of("holdId", "HOLD-" + System.currentTimeMillis(), "status", "HELD", "request", request);
  }

  public Map<String, Object> release(Map<String, Object> request) {
    return Map.of("status", "RELEASED", "request", request);
  }

  public Map<String, Object> commit(Map<String, Object> request) {
    return Map.of("status", "COMMITTED", "request", request);
  }

  public Map<String, Object> health(String mode) {
    Map<String, Object> details = new LinkedHashMap<>();
    details.put("mode", mode);
    details.put("service", "flight-service");
    details.put("db", "configured");
    return Map.of("status", "UP", "details", details);
  }

  private String safe(String value) {
    return value == null ? "" : value;
  }
}
