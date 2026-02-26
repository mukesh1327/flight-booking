namespace bookingservice.Domain;

public record Booking(
    string BookingId,
    string UserId,
    string FlightId,
    int SeatCount,
    string Status,
    DateTimeOffset UpdatedAt);
