using System.Diagnostics;
using System.Collections.Concurrent;

var builder = WebApplication.CreateBuilder(args);
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();

var app = builder.Build();
var startedAt = Stopwatch.StartNew();

if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

app.UseHttpsRedirection();

// In-memory demo inventory state for Track 1.
var availableSeats = new ConcurrentDictionary<string, int>(StringComparer.OrdinalIgnoreCase)
{
    ["AI202"] = 14,
    ["6E310"] = 10,
    ["UK811"] = 8
};

var holds = new ConcurrentDictionary<string, SeatHold>(StringComparer.OrdinalIgnoreCase);

// Health endpoints.
app.MapGet("/inventory/health/live", () => Results.Ok(new
{
    service = "inventoryservice",
    version = "1.0.0",
    environment = app.Environment.EnvironmentName,
    status = "UP",
    timestamp = DateTime.UtcNow,
    uptimeSeconds = startedAt.Elapsed.TotalSeconds
}));

app.MapGet("/inventory/health/ready", () =>
{
    // Minimal readiness for Track 1 (no external dependency configured yet).
    return Results.Ok(new
    {
        service = "inventoryservice",
        version = "1.0.0",
        environment = app.Environment.EnvironmentName,
        status = "UP",
        timestamp = DateTime.UtcNow,
        uptimeSeconds = startedAt.Elapsed.TotalSeconds,
        checks = new[]
        {
            new { name = "in-memory-store", status = "UP", latencyMs = 0 }
        }
    });
});

app.MapGet("/inventory/health", () => Results.Ok(new
{
    service = "inventoryservice",
    version = "1.0.0",
    environment = app.Environment.EnvironmentName,
    status = "UP",
    timestamp = DateTime.UtcNow,
    uptimeSeconds = startedAt.Elapsed.TotalSeconds,
    checks = new[]
    {
        new { name = "in-memory-store", status = "UP", latencyMs = 0 }
    }
}));

// Inventory APIs aligned to README contract.
app.MapGet("/inventory/flights/{flightId}/seats", (string flightId) =>
{
    var seats = availableSeats.GetValueOrDefault(flightId, 0);
    return Results.Ok(new
    {
        flightId,
        availableSeats = seats,
        status = seats > 0 ? "AVAILABLE" : "SOLD_OUT"
    });
});

app.MapPost("/inventory/hold", (HoldRequest request) =>
{
    if (request.SeatCount <= 0)
    {
        return Results.BadRequest(new { message = "seatCount must be greater than 0" });
    }

    if (!availableSeats.TryGetValue(request.FlightId, out var seats) || seats < request.SeatCount)
    {
        return Results.Conflict(new
        {
            flightId = request.FlightId,
            status = "REJECTED",
            reason = "INSUFFICIENT_SEATS"
        });
    }

    var holdId = $"hold_{Guid.NewGuid():N}"[..13];
    var expiresAt = DateTime.UtcNow.AddSeconds(request.TtlSeconds <= 0 ? 600 : request.TtlSeconds);

    availableSeats[request.FlightId] = seats - request.SeatCount;
    holds[holdId] = new SeatHold(holdId, request.FlightId, request.SeatCount, expiresAt, false);

    return Results.Ok(new
    {
        holdId,
        flightId = request.FlightId,
        status = "HELD",
        expiresAt
    });
});

app.MapPost("/inventory/confirm-hold", (ConfirmHoldRequest request) =>
{
    if (!holds.TryGetValue(request.HoldId, out var hold))
    {
        return Results.NotFound(new { message = "hold not found" });
    }

    if (DateTime.UtcNow > hold.ExpiresAt)
    {
        // Expired hold returns seats back.
        availableSeats.AddOrUpdate(hold.FlightId, hold.SeatCount, (_, old) => old + hold.SeatCount);
        holds.TryRemove(hold.HoldId, out _);
        return Results.Conflict(new { holdId = request.HoldId, status = "EXPIRED" });
    }

    holds[hold.HoldId] = hold with { Confirmed = true };
    return Results.Ok(new { holdId = request.HoldId, status = "CONFIRMED" });
});

app.MapPost("/inventory/release", (ReleaseHoldRequest request) =>
{
    if (!holds.TryGetValue(request.HoldId, out var hold))
    {
        return Results.NotFound(new { message = "hold not found" });
    }

    // For demo: release always returns held seats unless already confirmed then still release (cancel flow).
    availableSeats.AddOrUpdate(hold.FlightId, hold.SeatCount, (_, old) => old + hold.SeatCount);
    holds.TryRemove(hold.HoldId, out _);

    return Results.Ok(new { holdId = request.HoldId, status = "RELEASED" });
});

app.Run();

record HoldRequest(string FlightId, int SeatCount, string Cabin, int TtlSeconds);
record ConfirmHoldRequest(string HoldId);
record ReleaseHoldRequest(string HoldId);
record SeatHold(string HoldId, string FlightId, int SeatCount, DateTime ExpiresAt, bool Confirmed);
