# TokenRevocationEventPublisher - Documentación

## 📋 Descripción General

El `TokenRevocationEventPublisher` es un componente que notifica al servicio de tiempo real sobre revocaciones de tokens para mantener la coherencia del estado de autenticación en tiempo real.

## 🎯 Casos de Uso

### 1. **Refresh de Token** (`RefreshTokenUseCase`)
- **Cuándo**: Cuando se genera un nuevo access token
- **Qué se notifica**: El access token anterior que será revocado
- **Método**: `publishAccessTokenRevoked()`
- **Tipo**: `ACCESS_TOKEN_REFRESH`

### 2. **Logout de Usuario** (`LogOutUseCase`)
- **Cuándo**: Cuando un usuario cierra sesión
- **Qué se notifica**: Sesión completa revocada (access + refresh tokens)
- **Método**: `publishSessionRevoked()`
- **Tipo**: `SESSION_LOGOUT`

### 3. **Revocación por Seguridad** (`RevokeTokenForSecurityUseCase`)
- **Cuándo**: Actividad sospechosa, compromiso de cuenta, políticas de seguridad
- **Qué se notifica**: Tokens revocados por motivos de seguridad
- **Método**: `publishTokenRevokedForSecurity()`
- **Tipo**: `SECURITY_REVOCATION`

## 🔧 Estructura del Evento

```json
{
  "eventType": "DELETE",
  "body": {
    "tokenUID": "uuid-del-token",
    "userUID": "uuid-del-usuario",
    "accessToken": "jwt-access-token",
    "refreshToken": "jwt-refresh-token", // opcional
    "revocationType": "ACCESS_TOKEN_REFRESH | SESSION_LOGOUT | SECURITY_REVOCATION",
    "reason": "Descripción de la razón"
  },
  "resourceUID": "uuid-del-token",
  "receiverUID": "uuid-del-usuario",
  "dataType": "token_revocation"
}
```

## 📡 Canal de Comunicación

- **Canal Redis**: `user:events`
- **Tipo de Dato**: `token_revocation`
- **Patrón**: Fire-and-forget (no bloquea la operación principal)

## 🏗️ Integración en Use Cases

### RefreshTokenUseCase
```java
// Antes de actualizar el token, notificar revocación del anterior
AuthTokenDataModel previousTokenState = new AuthTokenDataModel();
// ... configurar datos del token anterior ...

tokenRevocationEventPublisher.publishAccessTokenRevoked(previousTokenState)
    .doOnError(e -> log.warn("Failed to publish access token revocation event"))
    .subscribe(); // Fire-and-forget
```

### LogOutUseCase
```java
// Para cada token activo del usuario
tokenRevocationEventPublisher.publishSessionRevoked(token)
    .doOnError(e -> log.warn("Failed to publish session revocation event"))
    .subscribe(); // Fire-and-forget
```

### RevokeTokenForSecurityUseCase
```java
// Antes de revocar por seguridad
tokenRevocationEventPublisher.publishTokenRevokedForSecurity(tokenData, reason)
    .doOnError(e -> log.warn("Failed to publish security revocation event"))
    .then(/* continuar con revocación */)
```

## 🎛️ Servicio de Tiempo Real (Receptor)

El servicio de tiempo real debe:

1. **Escuchar el canal**: `user:events`
2. **Filtrar eventos**: `dataType == "token_revocation"`
3. **Procesar según tipo**:
   - `ACCESS_TOKEN_REFRESH`: Cerrar conexiones con el access token anterior
   - `SESSION_LOGOUT`: Cerrar todas las conexiones del usuario
   - `SECURITY_REVOCATION`: Cerrar conexiones y posiblemente bloquear reconexión temporal

## 📊 Logging y Monitoreo

- **Éxito**: Log INFO con detalles del token y número de subscribers notificados
- **Error**: Log WARN con detalles del error (no afecta la operación principal)
- **Métricas**: Número de eventos publicados por tipo de revocación

## ⚡ Características Clave

- **No Bloqueante**: Las notificaciones no afectan el flujo principal
- **Resiliente**: Errores en notificaciones no fallan la operación de revocación
- **Consistente**: Usa el mismo canal y formato que otros event publishers
- **Tipado**: Diferentes tipos de revocación para manejo específico
- **Reactivo**: Implementación completamente reactiva con Mono/Flux

## 🔒 Consideraciones de Seguridad

- Los tokens en los eventos están completos para identificación en el servicio de tiempo real
- Los logs no incluyen tokens completos para evitar exposición en logs
- Fire-and-forget evita timeouts que podrían afectar la revocación real
- Múltiples tipos de revocación permiten diferentes niveles de respuesta de seguridad
