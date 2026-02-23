# Hotty Backend 🚀

Hotty es un motor de backend reactivo de alta disponibilidad diseñado para aplicaciones sociales que requieren geolocalización, gestión de interacciones y comunicación bidireccional masiva. El proyecto implementa una arquitectura de **Monolito Modular** donde la inteligencia de integración y la orquestación residen en el módulo `common`.

---

## 🏗️ Arquitectura: Common como Núcleo Orquestador

A diferencia de las arquitecturas tradicionales, el módulo **`common`** no es una librería de utilidades, sino el centro de control, orquestación y comunicación del sistema:

### 1. Orquestación Transversal
`common` actúa como el **mediador de alto nivel**. Aloja los casos de uso que coordinan flujos de negocio complejos que involucran a múltiples servicios de dominio. 
* **Desacoplamiento:** Permite que los servicios especializados (`auth`, `user`, `chat`, etc.) permanezcan atómicos y no tengan dependencias circulares entre ellos.
* **Consistencia:** El orquestador garantiza la integridad de los datos en flujos reactivos que afectan a distintos almacenes de persistencia.

### 2. Comunicación Orientada a Eventos (EDA)
El sistema utiliza un bus de eventos basado en **Redis Pub/Sub** centralizado en `common` para desacoplar la persistencia de la entrega en tiempo real:
* **Flujo de Notificación:** Cuando un servicio de dominio realiza una acción (ej. un "Like" o un mensaje nuevo), notifica a `common`.
* **Puente Realtime:** `common` procesa el evento y lo distribuye hacia el `realtime_service`, que se encarga de la emisión vía WebSockets hacia el cliente.

---

## 📂 Estructura de Módulos

| Módulo | Responsabilidad Principal |
| :--- | :--- |
| **`common`** | **Núcleo Orquestador**, gestión de eventos globales y contratos compartidos. |
| **`gateway_server`** | Seguridad perimetral, validación de JWT y enrutamiento. |
| **`auth_service`** | Gestión de identidad, autenticación y emisión de tokens. |
| **`user_service`** | Perfiles de usuario y motor de geolocalización avanzada. |
| **`likes_service`** | Lógica de interacciones, matches y descubrimiento. |
| **`chat_service`** | Gestión de canales y estados de las conversaciones. |
| **`message_service`** | Persistencia e historial de mensajes. |
| **`realtime_service`** | Gestión de WebSockets y entrega de eventos en tiempo real. |
| **`media_service`** | Interfaz con almacenamiento de objetos (S3/Minio). |
| **`subscriptions_service`** | Gestión de planes, pagos y estados de suscripción. |

---

## 🛠️ Stack Tecnológico

* **Core:** Java 21 & Spring Boot 3 (WebFlux).
* **Runtime:** Stack 100% no bloqueante basado en **Project Reactor**.
* **Persistencia:** MongoDB (Documental/Geoespacial) y Redis (Cache/Eventos).
* **Infraestructura:** Integración con sistemas compatibles con S3 para multimedia.

---

## 🧭 Decisiones de Diseño

### Estrategia de Persistencia: MongoDB vs PostgreSQL + PostGIS
Se seleccionó **MongoDB** como base de datos principal para la gestión de perfiles y geolocalización, asumiendo un compromiso técnico (*trade-off*) entre flexibilidad e integridad:

* **Por qué MongoDB:** La naturaleza dinámica de los perfiles sociales requiere modificaciones rápidas del esquema sin migraciones costosas. MongoDB ofrece una alta escalabilidad de escritura y un soporte nativo para índices `2dsphere` eficiente para consultas de proximidad.
* **Integridad Orquestada:** Se evaluó el uso de **PostgreSQL con PostGIS**, que habría simplificado la consistencia mediante relaciones y eliminaciones en cascada (fundamental en casos de uso como la eliminación de cuenta). Sin embargo, se optó por delegar esta responsabilidad a la **Capa Orquestadora** en `common`. 
* **Consistencia Manual:** El orquestador coordina las eliminaciones y actualizaciones transversales de forma asíncrona, garantizando que no queden datos huérfanos en colecciones relacionadas (likes, chats, media) sin sacrificar la velocidad de respuesta del sistema NoSQL.

### Reactividad End-to-End
Flujo de datos asíncrono desde el Gateway hasta la base de datos para maximizar el rendimiento y el aprovechamiento de recursos de la CPU.

---

## 📜 Licencia
Este proyecto es de código abierto. La documentación detallada de cada módulo se encuentra en sus respectivos directorios.
