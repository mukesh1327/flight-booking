# bookingservice

| Method | Endpoint               | Description                 | Auth |
| ------ | ---------------------- | --------------------------- | ---- |
| POST   | /booking/reserve       | Hold flight + seats (10min) | JWT  |
| POST   | /booking/confirm/{pnr} | Finalize booking            | JWT  |
| GET    | /booking/my-bookings   | User bookings               | JWT  |
| PUT    | /booking/{pnr}/cancel  | Cancel booking              | JWT  |



> curl -X GET http://localhost:8080/booking/my-bookings

> curl -X GET http://localhost:8080/booking/my-bookings/101

> curl -X POST http://localhost:8080/booking/reserve \
     -H "Content-Type: application/json" \
     -d '{"name":"Mukesh","email":"mukesh@test.com"}'

> curl -X POST http://localhost:8080/booking/confirm/PNR12345 \
     -H "Content-Type: application/json" \
     -d '{"name":"Mukesh","email":"mukesh@test.com"}'

> curl -X PUT http://localhost:8080/booking/PNR12345/cancel \
     -H "Content-Type: application/json" \
     -d '{"name":"Mukesh","email":"mukesh@test.com"}'
