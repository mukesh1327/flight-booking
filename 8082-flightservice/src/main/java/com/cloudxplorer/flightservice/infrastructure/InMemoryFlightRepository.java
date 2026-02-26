package com.cloudxplorer.flightservice.infrastructure;

import com.cloudxplorer.flightservice.domain.Flight;
import com.cloudxplorer.flightservice.domain.FlightRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@ApplicationScoped
public class InMemoryFlightRepository implements FlightRepository {

  private static final List<Flight> FLIGHTS =
      List.of(
          new Flight("FL-1001", "SkyFly", "DEL", "BOM", "2026-02-25T09:00:00Z", "2026-02-25T11:10:00Z", 5800, 18),
          new Flight("FL-1002", "SkyFly", "DEL", "BLR", "2026-02-25T12:00:00Z", "2026-02-25T14:50:00Z", 6400, 9),
          new Flight("FL-1003", "CloudAir", "BOM", "DEL", "2026-02-25T15:30:00Z", "2026-02-25T17:40:00Z", 5600, 24));

  @Override
  public List<Flight> search(String from, String to, String date) {
    String normFrom = normalize(from);
    String normTo = normalize(to);
    return FLIGHTS.stream()
        .filter(f -> normFrom == null || f.from().equalsIgnoreCase(normFrom))
        .filter(f -> normTo == null || f.to().equalsIgnoreCase(normTo))
        .toList();
  }

  @Override
  public Optional<Flight> findById(String flightId) {
    return FLIGHTS.stream().filter(f -> f.flightId().equalsIgnoreCase(flightId)).findFirst();
  }

  private String normalize(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim().toUpperCase(Locale.ROOT);
  }
}
