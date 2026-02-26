FROM mcr.microsoft.com/dotnet/sdk:8.0 AS build
WORKDIR /src
COPY bookingservice.csproj .
RUN dotnet restore
COPY . .
RUN dotnet publish -c Release -o /app/publish

FROM mcr.microsoft.com/dotnet/aspnet:8.0
WORKDIR /app
COPY --from=build /app/publish .
EXPOSE 8083
ENTRYPOINT ["dotnet", "bookingservice.dll"]
