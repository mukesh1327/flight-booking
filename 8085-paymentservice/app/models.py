from pydantic import BaseModel, Field


class PaymentIntentRequest(BaseModel):
    bookingId: str
    amount: float = Field(gt=0)
    currency: str = "INR"


class PaymentWebhookRequest(BaseModel):
    provider: str
    eventType: str
    payload: dict
