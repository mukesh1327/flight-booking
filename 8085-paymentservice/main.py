from typing import Optional

from fastapi import FastAPI, Header, HTTPException

from app.models import PaymentIntentRequest, PaymentWebhookRequest
from app.repository import InMemoryPaymentRepository
from app.service import PaymentService

app = FastAPI(title="SkyFly Payment Service", version="1.0.0")
service = PaymentService(InMemoryPaymentRepository())


@app.get("/api/v1/health")
def health():
    return service.health("health")


@app.get("/api/v1/health/live")
def health_live():
    return service.health("live")


@app.get("/api/v1/health/ready")
def health_ready():
    return service.health("ready")


@app.post("/api/v1/payments/intent")
def create_intent(
    payload: PaymentIntentRequest,
    x_user_id: Optional[str] = Header(default="U-CUSTOMER-1"),
    x_actor_type: Optional[str] = Header(default="customer"),
):
    return service.create_intent(payload.bookingId, payload.amount, payload.currency, x_actor_type, x_user_id)


@app.post("/api/v1/payments/{payment_id}/authorize")
def authorize(payment_id: str):
    try:
        return service.transition(payment_id, "AUTHORIZED")
    except KeyError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc


@app.post("/api/v1/payments/{payment_id}/capture")
def capture(payment_id: str):
    try:
        return service.transition(payment_id, "CAPTURED")
    except KeyError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc


@app.post("/api/v1/payments/{payment_id}/refund")
def refund(payment_id: str):
    try:
        return service.transition(payment_id, "REFUNDED")
    except KeyError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc


@app.post("/api/v1/payments/webhooks/provider")
def provider_webhook(payload: PaymentWebhookRequest):
    return {
        "accepted": True,
        "provider": payload.provider,
        "eventType": payload.eventType,
    }
