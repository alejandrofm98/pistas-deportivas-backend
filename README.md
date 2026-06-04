# Pistas Deportivas - Backend

API REST para el sistema de reserva de pistas deportivas con integración de pagos Redsys y notificaciones por correo electrónico.

## Stack

- **Lenguaje**: Java 21
- **Framework**: Spring Boot 4.0.6 (Spring Web, Spring Data JPA, Spring Security, Spring Validation, Spring Mail)
- **Build**: Maven
- **Base de datos**: PostgreSQL 16
- **ORM**: Hibernate (Spring Data JPA)
- **Migraciones**: Flyway (`flyway-core` + `flyway-database-postgresql`)
- **Autenticación**: JWT (jjwt 0.12.6) con filtro personalizado `JwtAuthenticationFilter`
- **Pagos**: Redsys (pasarela española) — endpoint de notificación y firma HM256
- **Email**: SMTP genérico + Brevo SDK (transaccional) para confirmaciones y cancelaciones
- **Utilidades**: Lombok
- **Tests**: JUnit 5 (incluido en `spring-boot-starter-test`) y `spring-security-test`

## Requisitos previos

- Java JDK 21
- Maven 3.9+
- PostgreSQL 13+ (la imagen oficial usada en `docker-compose` es `postgres:16-alpine`)
- Docker y Docker Compose (opcional, recomendado para desarrollo)
- Cuenta Redsys (entorno test o producción) y claves API de Brevo si se desea envío real de correos

## Instalación

```bash
# 1. Clonar el repositorio
git clone <url-del-repo>
cd pistas-deportivas-backend

# 2. Crear el archivo .env a partir del ejemplo
cp .env.example .env
# Editar .env con las credenciales reales

# 3. Levantar PostgreSQL y la app con Docker Compose
docker compose up -d --build

# O bien, ejecutar localmente contra una BD externa
mvn spring-boot:run
```

Flyway ejecuta automáticamente todas las migraciones al arrancar la aplicación (esquema inicial + seeds en `V3__seed_data.sql`).

## Comandos

| Comando | Descripción |
|---------|-------------|
| `mvn spring-boot:run` | Arranca la aplicación en local (puerto 8080) |
| `mvn clean package` | Compila y empaqueta el JAR (`target/*.jar`) |
| `mvn package -DskipTests` | Empaqueta sin ejecutar tests |
| `mvn test` | Ejecuta la suite de tests |
| `docker compose up -d --build` | Levanta BD + app en contenedores |
| `docker compose -f docker-compose.prod.yml up -d` | Despliegue productivo tras Traefik (red `dokploy-network`) |

## Estructura del proyecto

```
src/main/java/com/sportreserve/
├── SportReserveApplication.java   # Entry point + @EnableScheduling
├── admin/                         # Login admin + CRUD protegido de pistas + upload
│   └── dto/                       # LoginRequest, LoginResponse
├── config/                        # SecurityConfig, WebConfig, FlywayConfig, Dotenv post-processor
├── court/                         # Entidad, repositorio, servicio, controlador público de pistas
│   └── dto/                       # CourtRequest, CourtResponse, AvailabilityResponse, CourtMapper
├── exception/                     # GlobalExceptionHandler, BusinessException, ResourceNotFoundException
├── notification/                  # EmailService (Brevo + SMTP fallback)
├── payment/                       # RedsysService, PaymentService, PaymentController
│   └── dto/                       # PaymentInitiate/Confirm/Response
├── reservation/                   # Entidad, repositorio, servicio, controlador + tarea programada de limpieza
│   └── dto/                       # ReservationRequest, ReservationResponse, ReservationMapper
└── security/                      # JwtTokenProvider, JwtAuthenticationFilter

src/main/resources/
├── application.yml                # Configuración Spring + mapeo de variables
├── db/migration/                  # 8 migraciones Flyway (V1–V8)
├── static/                        # Iconos (icono.png, icono.webp, icono-email.png)
└── templates/email/               # Plantillas HTML de confirmación y cancelación
```

## API Endpoints

### Autenticación pública

| Método | Path | Auth | Descripción |
|--------|------|------|-------------|
| `POST` | `/api/auth/login` | No | Login de administrador. Devuelve JWT. |

### Pistas (público)

| Método | Path | Auth | Descripción |
|--------|------|------|-------------|
| `GET` | `/api/courts` | No | Lista las pistas activas. |
| `GET` | `/api/courts/{id}` | No | Detalle de una pista. |
| `GET` | `/api/courts/{id}/availability?date=YYYY-MM-DD` | No | Huecos libres / ocupados del día. |

### Reservas

| Método | Path | Auth | Descripción |
|--------|------|------|-------------|
| `POST` | `/api/reservations` | No | Crea una reserva (cliente). |
| `GET` | `/api/reservations` | JWT | Lista todas las reservas. |
| `GET` | `/api/reservations/{id}` | JWT | Detalle de una reserva. |
| `PUT` | `/api/reservations/{id}/cancel` | JWT | Cancela la reserva. |
| `PUT` | `/api/reservations/{id}/status?status=...` | JWT | Cambia estado (`PENDING_PAYMENT`, `CONFIRMED`, `CANCELLED`, `COMPLETED`). |
| `PATCH` | `/api/reservations/{id}/payment-status?paymentStatus=...` | JWT | Cambia estado de pago (`PENDING`, `PAID`, `FAILED`, `REFUNDED`). |

### Pagos (Redsys)

| Método | Path | Auth | Descripción |
|--------|------|------|-------------|
| `POST` | `/api/payments/initiate` | No | Inicia un pago y devuelve la URL/params Redsys. |
| `POST` | `/api/payments/notify` | No | Callback del SIS Redsys (form-encoded). |
| `POST` | `/api/payments/notify-json` | No | Callback del SIS Redsys (JSON). |
| `POST` | `/api/payments/confirm` | No | Confirmación adicional con `Ds_MerchantParameters` + `Ds_Signature`. |
| `GET` | `/api/payments/{id}/result` | No | Resultado final del pago (consulta cliente). |
| `GET` | `/api/payments/{id}` | JWT | Estado de un pago. |
| `GET` | `/api/payments/by-reservation/{reservationId}` | JWT | Pago asociado a una reserva. |

### Administración (requiere JWT con rol `ADMIN`)

| Método | Path | Descripción |
|--------|------|-------------|
| `GET` | `/api/admin/courts` | Lista todas las pistas (incluidas inactivas). |
| `POST` | `/api/admin/courts` | Crea una pista. |
| `PUT` | `/api/admin/courts/{id}` | Actualiza una pista. |
| `DELETE` | `/api/admin/courts/{id}` | Elimina una pista. |
| `PATCH` | `/api/admin/courts/{id}/toggle` | Activa / desactiva una pista. |
| `POST` | `/api/admin/upload` | Sube una imagen (multipart) a `UPLOAD_DIR`. |

### Recursos estáticos

| Path | Descripción |
|------|-------------|
| `/uploads/**` | Imágenes servidas desde `UPLOAD_DIR`. |
| `/icono.png`, `/icono.webp`, `/favicon.ico` | Iconos públicos. |

### Autenticación de la API

Todas las rutas marcadas con `JWT` requieren la cabecera:

```http
Authorization: Bearer <token>
```

El token se obtiene en `POST /api/auth/login` con `{ "username": "...", "password": "..." }` y tiene una expiración de 24 h (`app.jwt.expiration-ms: 86400000`).

Tipos de pista (`CourtType`): `TENIS`, `FUTBOL`, `PADEL`, `BALONCESTO`, `VOLEIBOL`, `FRONTON`.
Métodos de pago (`PaymentMethod`): `ONLINE`, `BIZUM`, `ONSITE`.

## Despliegue

El proyecto incluye configuración lista para dos escenarios:

- **Desarrollo**: `docker-compose.yml` con `postgres:16-alpine` + la aplicación construida desde el `Dockerfile` multi-stage (`maven:3.9-eclipse-temurin-21` → `eclipse-temurin:21-jre`). Persistencia en volúmenes `postgres_data` y `uploads_data`.

- **Producción**: `docker-compose.prod.yml` preparado para integrarse con **Traefik** (red externa `dokploy-network`, certificados Let's Encrypt, host `api.pistasvalleperdido.es`). Las variables se inyectan desde el entorno del host (sin `env_file`).

```bash
# Producción (asumiendo Traefik y la red dokploy-network ya creadas)
docker compose -f docker-compose.prod.yml up -d --build
```

Las migraciones Flyway se aplican automáticamente al arrancar; no se requiere paso manual.

## Tests

```bash
mvn test
```

El proyecto incluye las dependencias `spring-boot-starter-test` y `spring-security-test`, pero actualmente **no hay clases de test** (`src/test/` no existe). Los tests se deberán añadir siguiendo las convenciones estándar de Spring Boot (`*Test.java` o `*Tests.java` bajo `src/test/java/...`).
