# Test de Compra Consumible (NON_RENEWING_PURCHASE)

## Webhook de prueba para consumibles

```bash
curl -X POST http://localhost:8082/subscriptions-service/webhooks/revenuecat \
  -H "Content-Type: application/json" \
  -d '{
    "event": {
        "event_timestamp_ms": 1762094986090,
        "product_id": "2100_tokens", 
        "period_type": "NORMAL",
        "purchased_at_ms": 1658726519000,
        "expiration_at_ms": null,
        "environment": "SANDBOX",
        "entitlement_id": null,
        "entitlement_ids": ["pro"],
        "presented_offering_id": "coins",
        "transaction_id": "GPA.3382-9984-7968-99999",
        "original_transaction_id": "GPA.3382-9984-7968-99999",
        "is_family_share": false,
        "country_code": "ES",
        "app_user_id": "71385e26eb",
        "aliases": ["$RCAnonymousID:8069238d6049ce87cc529853916d624c"],
        "original_app_user_id": "$RCAnonymousID:87c6049c58069238dce29853916d624c",
        "currency": "EUR",
        "price": 4.99,
        "price_in_purchased_currency": 4.99,
        "subscriber_attributes": {
            "$email": {
                "updated_at_ms": 1662955084635,
                "value": "test@gmail.com"
            }
        },
        "store": "PLAY_STORE",
        "takehome_percentage": 0.85,
        "tax_percentage": 0.0,
        "commission_percentage": 0.15,
        "offer_code": null,
        "type": "NON_RENEWING_PURCHASE",
        "id": "12345678-1234-1234-1234-123456789012",
        "app_id": "1234567890",
        "experiments": [
            {
                "experiment_id": "prexp123",
                "experiment_variant": "b"
            }
        ]
    },
    "api_version": "1.0"
}'
```

## Respuesta esperada

El sistema debería:

1. **Reconocer el evento `NON_RENEWING_PURCHASE`**
2. **NO otorgar premium al usuario** (los consumibles no dan premium)
3. **Establecer subscriptionStatus como "NON_RENEWING_PURCHASE"**
4. **Logear información detallada del producto consumible**
5. **Enviar notificación al stream de eventos**

## Logs esperados

```
INFO  - Processing RevenueCat webhook event: NON_RENEWING_PURCHASE for user: 71385e26eb
INFO  - NON_RENEWING_PURCHASE processed for user: 71385e26eb - Product: 2100_tokens, Offering: coins, Price: 4.99 EUR
DEBUG - Revenue details - Takehome: 85.0%, Commission: 15.0%, Tax: 0.0%
DEBUG - Processed NON_RENEWING_PURCHASE - consumible purchased, no premium granted
INFO  - RevenueCat webhook processed and notification sent
```

## Diferencias con suscripciones

| Aspecto | Suscripción (INITIAL_PURCHASE/RENEWAL) | Consumible (NON_RENEWING_PURCHASE) |
|---------|---------------------------------------|----------------------------------|
| **Premium** | ✅ Otorga premium si no ha expirado | ❌ NO otorga premium |
| **Expiración** | ✅ Tiene `expiration_at_ms` | ❌ `expiration_at_ms` es null |
| **Renovación** | ✅ Se renueva automáticamente | ❌ Compra única, no se renueva |
| **Estado** | `ACTIVE`/`EXPIRED`/`CANCELLED` | `NON_RENEWING_PURCHASE` |
| **Producto** | Plan de suscripción | Producto consumible (tokens, coins, etc.) |
