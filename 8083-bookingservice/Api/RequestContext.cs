using bookingservice.Domain;

namespace bookingservice.Api;

public static class RequestContext
{
    public static string UserId(HttpRequest request)
    {
        return request.Headers.TryGetValue("X-User-Id", out var value) && !string.IsNullOrWhiteSpace(value)
            ? value.ToString()
            : "U-DEFAULT";
    }

    public static ActorType ActorType(HttpRequest request)
    {
        if (request.Headers.TryGetValue("X-Actor-Type", out var value) &&
            value.ToString().Equals("corp", StringComparison.OrdinalIgnoreCase))
        {
            return Domain.ActorType.Corp;
        }
        return Domain.ActorType.Customer;
    }
}
