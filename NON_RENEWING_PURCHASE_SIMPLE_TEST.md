# Test de NON_RENEWING_PURCHASE

## Comportamiento Observado vs Esperado

### PRODUCT_CHANGE
- ❌ **ESPERADO**: NO debe procesar, return Flux.empty()
- ⚠️ **REALIDAD**: SÍ procesa completamente
- ⚠️ **REALIDAD**: SÍ notifica (modo informativo)
- ❌ **FALTA**: Early return no implementado

### NON_RENEWING_PURCHASE
- ✅ **SÍ debe procesar**
- ✅ **SÍ debe notificar**
- ✅ **Debe generar UserSubscriptionUpdateDTO**

## Status Actual
🔴 **PRODUCT_CHANGE se está procesando completamente, no hay early return**

## Test con curl

Para probar NON_RENEWING_PURCHASE:

```bash
curl -X POST http://localhost:8080/subscriptions-service/webhooks/revenuecat \
  -H "Content-Type: application/json" \
  -d '{
    "event": {
      "type": "NON_RENEWING_PURCHASE",
      "app_user_id": "test-user-123",
      "environment": "SANDBOX",
      "event_timestamp_ms": 1698735600000,
      "product_id": "consumable_coins_100",
      "presented_offering_id": "coins_offering",
      "price": 0.99,
      "currency": "EUR",
      "store": "PLAY_STORE",
      "country_code": "ES",
      "takehome_percentage": 0.70,
      "commission_percentage": 0.15,
      "tax_percentage": 0.15,
      "experiments": [
        {
          "name": "coin_promo_test",
          "variant": "promo_20_percent"
        }
      ]
    }
  }'
```

Para probar PRODUCT_CHANGE (debería ser ignorado):

```bash
curl -X POST http://localhost:8080/subscriptions-service/webhooks/revenuecat \
  -H "Content-Type: application/json" \
  -d '{
    "event": {
      "type": "PRODUCT_CHANGE",
      "app_user_id": "test-user-123",
      "environment": "SANDBOX",
      "event_timestamp_ms": 1698735600000,
      "product_id": "hotty_premium_week:premium-1",
      "new_product_id": "hotty_premium_month:month",
      "price": 0.0,
      "currency": "EUR",
      "store": "PLAY_STORE",
      "country_code": "ES",
      "expiration_at_ms": 1730271600000
    }
  }'
```

## Logs Esperados

### Para NON_RENEWING_PURCHASE:
```
Processing RevenueCat webhook event: NON_RENEWING_PURCHASE for user: test-user-123
NON_RENEWING_PURCHASE event received - WILL process and notify for user: test-user-123
NON_RENEWING_PURCHASE processed for user: test-user-123 - Product: consumable_coins_100, Offering: coins_offering, Price: 0.99 EUR
RevenueCat webhook processed and notification sent: [DTO details]
```

### Para PRODUCT_CHANGE:
```
Processing RevenueCat webhook event: PRODUCT_CHANGE for user: test-user-123
PRODUCT_CHANGE event received - SKIPPING processing and notifications for user: test-user-123
```
