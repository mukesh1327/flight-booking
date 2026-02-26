from typing import Dict, Optional


class InMemoryPaymentRepository:
    def __init__(self):
        self._payments: Dict[str, dict] = {}

    def save(self, payment: dict) -> dict:
        self._payments[payment["paymentId"]] = payment
        return payment

    def find(self, payment_id: str) -> Optional[dict]:
        return self._payments.get(payment_id)
