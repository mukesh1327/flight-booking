using bookingservice.Api;
using bookingservice.Application;
using bookingservice.Domain;
using bookingservice.Infrastructure;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddSingleton<IBookingRepository, InMemoryBookingRepository>();
builder.Services.AddSingleton<BookingApplicationService>();

var app = builder.Build();

app.MapHealthEndpoints();
app.MapBookingEndpoints();

app.Run("http://0.0.0.0:8083");
