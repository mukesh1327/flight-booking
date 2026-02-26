from datetime import datetime, timezone
from uuid import uuid4


class PaymentService:
    def __init__(self, repository):
        self.repository = repository

    def create_intent(self, booking_id: str, amount: float, currency: str, actor_type: str, user_id: str):
        payment_id = f"PAY-{uuid4().hex[:12].upper()}"
        payment = {
            "paymentId": payment_id,
            "bookingId": booking_id,
            "amount": amount,
            "currency": currency,
            "status": "INTENT_CREATED",
            "actorType": actor_type,
            "userId": user_id,
            "updatedAt": datetime.now(timezone.utc).isoformat(),
        }
        return self.repository.save(payment)

    def transition(self, payment_id: str, status: str):
        payment = self.repository.find(payment_id)
        if payment is None:
            raise KeyError(f"payment not found: {payment_id}")

        payment["status"] = status
        payment["updatedAt"] = datetime.now(timezone.utc).isoformat()
        return self.repository.save(payment)

    @staticmethod
    def health(mode: str):
        return {
            "status": "UP",
            "details": {
                "mode": mode,
                "service": "payment-service",
                "storage": "in-memory",
            },
        }
