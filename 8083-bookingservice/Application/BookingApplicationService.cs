using bookingservice.Domain;

namespace bookingservice.Application;

public class BookingApplicationService
{
    private readonly IBookingRepository _repository;

    public BookingApplicationService(IBookingRepository repository)
    {
        _repository = repository;
    }

    public object Reserve(string userId, ReserveBookingRequest request)
    {
        var booking = new Booking(
            BookingId: $"BKG-{Guid.NewGuid():N}"[..16].ToUpperInvariant(),
            UserId: userId,
            FlightId: request.FlightId,
            SeatCount: request.SeatCount,
            Status: "RESERVED",
            UpdatedAt: DateTimeOffset.UtcNow);

        return _repository.Save(booking);
    }

    public object Confirm(string bookingId, string userId, ActorType actorType)
    {
        var booking = GetAuthorizedBooking(bookingId, userId, actorType);
        return _repository.Save(booking with { Status = "CONFIRMED", UpdatedAt = DateTimeOffset.UtcNow });
    }

    public object GetOne(string bookingId, string userId, ActorType actorType)
    {
        return GetAuthorizedBooking(bookingId, userId, actorType);
    }

    public BookingListResponse GetMany(string userId, ActorType actorType)
    {
        var list = _repository
            .FindAll()
            .Where(x => actorType == ActorType.Corp || x.UserId.Equals(userId, StringComparison.OrdinalIgnoreCase))
            .OrderByDescending(x => x.UpdatedAt)
            .Cast<object>()
            .ToList();

        return new BookingListResponse(list.Count, list);
    }

    public object Cancel(string bookingId, string userId, ActorType actorType)
    {
        var booking = GetAuthorizedBooking(bookingId, userId, actorType);
        return _repository.Save(booking with { Status = "CANCELLED", UpdatedAt = DateTimeOffset.UtcNow });
    }

    public object Change(string bookingId, string userId, ActorType actorType, ChangeBookingRequest request)
    {
        var booking = GetAuthorizedBooking(bookingId, userId, actorType);
        var updated = booking with
        {
            FlightId = request.NewFlightId ?? booking.FlightId,
            SeatCount = request.NewSeatCount ?? booking.SeatCount,
            Status = "CHANGED",
            UpdatedAt = DateTimeOffset.UtcNow
        };
        return _repository.Save(updated);
    }

    public object Health(string mode)
    {
        return new
        {
            status = "UP",
            details = new
            {
                mode,
                service = "booking-service",
                storage = "in-memory"
            }
        };
    }

    private Booking GetAuthorizedBooking(string bookingId, string userId, ActorType actorType)
    {
        var booking = _repository.FindById(bookingId);
        if (booking is null)
        {
            throw new KeyNotFoundException($"booking not found: {bookingId}");
        }

        if (actorType == ActorType.Corp)
        {
            return booking;
        }

        if (!booking.UserId.Equals(userId, StringComparison.OrdinalIgnoreCase))
        {
            throw new UnauthorizedAccessException("customer cannot access another user's booking");
        }

        return booking;
    }
}
