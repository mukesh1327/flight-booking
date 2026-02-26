FROM registry.redhat.io/ubi8/dotnet-80:latest AS build
WORKDIR /src
COPY bookingservice.csproj .
RUN dotnet restore
COPY . .
RUN dotnet publish -c Release -o /app/publish

FROM registry.redhat.io/rhel8/dotnet-80-runtime:latest
WORKDIR /app
COPY --from=build /app/publish .
EXPOSE 8083
ENTRYPOINT ["dotnet", "bookingservice.dll"]
