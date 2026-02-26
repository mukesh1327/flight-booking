using bookingservice.Application;

namespace bookingservice.Api;

public static class BookingEndpoints
{
    public static IEndpointRouteBuilder MapBookingEndpoints(this IEndpointRouteBuilder endpoints)
    {
        var group = endpoints.MapGroup("/api/v1/bookings");

        group.MapPost("/reserve", (HttpRequest request, ReserveBookingRequest payload, BookingApplicationService service) =>
        {
            var userId = RequestContext.UserId(request);
            return Results.Ok(service.Reserve(userId, payload));
        });

        group.MapPost("/{bookingId}/confirm", (HttpRequest request, string bookingId, BookingApplicationService service) =>
        {
            return Execute(request, service, () => service.Confirm(bookingId, RequestContext.UserId(request), RequestContext.ActorType(request)));
        });

        group.MapGet("/{bookingId}", (HttpRequest request, string bookingId, BookingApplicationService service) =>
        {
            return Execute(request, service, () => service.GetOne(bookingId, RequestContext.UserId(request), RequestContext.ActorType(request)));
        });

        group.MapGet("", (HttpRequest request, BookingApplicationService service) =>
        {
            return Results.Ok(service.GetMany(RequestContext.UserId(request), RequestContext.ActorType(request)));
        });

        group.MapPost("/{bookingId}/cancel", (HttpRequest request, string bookingId, BookingApplicationService service) =>
        {
            return Execute(request, service, () => service.Cancel(bookingId, RequestContext.UserId(request), RequestContext.ActorType(request)));
        });

        group.MapPost("/{bookingId}/change", (HttpRequest request, string bookingId, ChangeBookingRequest payload, BookingApplicationService service) =>
        {
            return Execute(request, service, () => service.Change(bookingId, RequestContext.UserId(request), RequestContext.ActorType(request), payload));
        });

        return endpoints;
    }

    private static IResult Execute(HttpRequest request, BookingApplicationService service, Func<object> action)
    {
        try
        {
            return Results.Ok(action());
        }
        catch (KeyNotFoundException ex)
        {
            return Results.NotFound(new { message = ex.Message });
        }
        catch (UnauthorizedAccessException ex)
        {
            return Results.Json(new { message = ex.Message }, statusCode: StatusCodes.Status403Forbidden);
        }
    }
}
