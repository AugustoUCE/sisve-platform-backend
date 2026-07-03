# Audit Service — Cloud Native

Microservicio de auditoría. Registro inmutable de eventos del sistema.
Quarkus + Gradle + Java 21 | Cloud Native | MicroProfile

**Esquema BD:** `audit_schema` | **Puerto:** `8084`
**Paquete base:** `com.uce.sisve.audit`

## Estructura de paquetes (separación de capas cloud native)

```
src/main/java/sisve/audit/
├── db/
│   └── AuditoriaEntity.java
├── repository/
│   └── AuditoriaRepository.java
├── dto/
│   ├── EventoAuditoriaRequest.java
│   └── EventoAuditoriaResponse.java
├── mapper/
│   └── AuditoriaMapper.java
├── service/
│   └── AuditService.java
├── rest/
│   └── AuditRest.java
├── health/
│   ├── AuditLivenessCheck.java
│   ├── AuditReadinessCheck.java
│   └── AuditDatabaseHealthCheck.java
└── lifecycle/
└── AuditStartup.java
```
## Configuración

### `application.properties`
```properties
# Base de datos
quarkus.datasource.db-kind=postgresql
quarkus.datasource.username=${DB_USERNAME}
quarkus.datasource.password=${DB_PASSWORD}
quarkus.datasource.jdbc.url=${DB_URL}

# Flyway
quarkus.flyway.migrate-at-start=true
quarkus.flyway.schemas=audit_schema
quarkus.flyway.default-schema=audit_schema
quarkus.flyway.baseline-on-migrate=true

# Puerto
quarkus.http.port=${QUARKUS_HTTP_PORT:8084}

# Service Discovery - Consul
quarkus.consul-config.enabled=true
quarkus.consul.host=${CONSUL_HOST:localhost}
quarkus.consul.port=${CONSUL_PORT:8500}
quarkus.stork.audit-service.service-discovery.type=consul
quarkus.stork.audit-service.service-discovery.consul-host=${CONSUL_HOST:localhost}
quarkus.stork.audit-service.service-discovery.consul-port=${CONSUL_PORT:8500}

# Metricas Prometheus
quarkus.micrometer.enabled=true
quarkus.micrometer.export.prometheus.enabled=true
quarkus.micrometer.export.prometheus.path=/metrics

# Health checks
quarkus.smallrye-health.root-path=/health
quarkus.smallrye-health.liveness-path=/health/live
quarkus.smallrye-health.readiness-path=/health/ready

# OpenAPI
quarkus.swagger-ui.always-include=true
quarkus.swagger-ui.path=/swagger-ui

# Nombre del servicio (para registrarse en Consul)
quarkus.application.name=audit-service
quarkus.application.version=1.0.0
```

### `credentialDeploy.env`
```env
DB_URL=jdbc:postgresql://localhost:5432/votacion_uce?currentSchema=audit_schema
DB_USERNAME=postgres
DB_PASSWORD=tu_password
QUARKUS_HTTP_PORT=8084
CONSUL_HOST=localhost
CONSUL_PORT=8500
```

## Paso 1 — Script Flyway

Crear: `src/main/resources/db/migration/V1__create_audit_schema.sql`

```sql
CREATE SCHEMA IF NOT EXISTS audit_schema;

CREATE TABLE audit_schema.auditoria (
    id_auditoria SERIAL PRIMARY KEY,
    tipo_evento VARCHAR(50) NOT NULL,
    descripcion TEXT,
    fecha_evento TIMESTAMP NOT NULL DEFAULT now(),
    ip_origen VARCHAR(45),
    servicio_origen VARCHAR(50) NOT NULL
);
```
## Política de permisos de base de datos (inmutabilidad del log)

Esta es la garantía técnica de que los registros de auditoría
no pueden ser modificados ni eliminados, ni siquiera por el
administrador de la base de datos de la aplicación.

### Crear usuario específico para audit-service en PostgreSQL

Ejecutar esto UNA SOLA VEZ en PostgreSQL antes de desplegar:

```sql
-- Crear usuario con permisos mínimos
CREATE USER audit_app_user WITH PASSWORD 'password_seguro';

-- Solo puede usar el esquema
GRANT USAGE ON SCHEMA audit_schema TO audit_app_user;

-- Solo puede insertar y leer, NUNCA actualizar ni eliminar
GRANT SELECT, INSERT ON ALL TABLES IN SCHEMA audit_schema 
TO audit_app_user;

-- Para tablas futuras creadas por Flyway
ALTER DEFAULT PRIVILEGES IN SCHEMA audit_schema
GRANT SELECT, INSERT ON TABLES TO audit_app_user;
```

### En credentialDeploy.env
Cambiar `DB_USERNAME` para que use `audit_app_user`,
no el usuario administrador de PostgreSQL. El usuario
`postgres` (superusuario) NO debe ser el usuario de la
aplicación en producción.

### Verificación
Si alguien intenta ejecutar directamente en PostgreSQL:
`DELETE FROM audit_schema.auditoria WHERE id_auditoria = 1;`
Debe obtener: `ERROR: permission denied for table auditoria`

## Paso 2 — Entidad (capa db)

### `db/AuditoriaEntity.java`
- `@Entity`, `@Table(name = "auditoria", schema = "audit_schema")`
- Extiende `PanacheEntityBase`
- Nombre de clase: `AuditoriaEntity` (distingue la entidad JPA del DTO)
- Campos:
    - `idAuditoria` Long, PK IDENTITY
    - `tipoEvento` String, length=50, nullable=false
    - `descripcion` String, columnDefinition="TEXT"
    - `fechaEvento` LocalDateTime, nullable=false
    - `ipOrigen` String, length=45
    - `servicioOrigen` String, length=50, nullable=false

## Paso 3 — Repositorio

### `repository/AuditoriaRepository.java`
- `@ApplicationScoped`, `PanacheRepository<AuditoriaEntity>`
- `List<AuditoriaEntity> findByServicio(String servicioOrigen)`
    - `list("servicioOrigen", servicioOrigen)`
- `List<AuditoriaEntity> findByServicioYFecha(String servicioOrigen, LocalDateTime desde)`
    - `list("servicioOrigen = ?1 and fechaEvento >= ?2 order by fechaEvento asc", servicioOrigen, desde)`
- `List<AuditoriaEntity> findByTipoEvento(String tipoEvento)`
    - `list("tipoEvento order by fechaEvento desc", tipoEvento)`
- REGLA: NO implementar update ni delete — solo insert y select

## Paso 4 — DTOs

### `dto/EventoAuditoriaRequest.java`
```java
public record EventoAuditoriaRequest(
    @NotBlank String tipoEvento,
    String descripcion,
    String ipOrigen,
    @NotBlank String servicioOrigen
) {}
```

### `dto/EventoAuditoriaResponse.java`
```java
public record EventoAuditoriaResponse(
    Long idAuditoria,
    String tipoEvento,
    String descripcion,
    LocalDateTime fechaEvento,
    String ipOrigen,
    String servicioOrigen
) {}
```

## Paso 5 — Mapper

### `mapper/AuditoriaMapper.java`
- `@ApplicationScoped`
- Mapeo manual (sin librerías externas para mantener simplicidad en Quarkus)
- Método `AuditoriaEntity toEntity(EventoAuditoriaRequest request)`:
    - Crea `AuditoriaEntity`, asigna campos, `fechaEvento = LocalDateTime.now()`
- Método `EventoAuditoriaResponse toResponse(AuditoriaEntity entity)`:
    - Crea `EventoAuditoriaResponse` desde la entidad
- Método `List<EventoAuditoriaResponse> toResponseList(List<AuditoriaEntity> entities)`:
    - `entities.stream().map(this::toResponse).toList()`

## Paso 6 — Service

### `service/AuditService.java`
- `@ApplicationScoped`
- Inyecta: `AuditoriaRepository`, `AuditoriaMapper`

#### `EventoAuditoriaResponse registrarEvento(EventoAuditoriaRequest request)` — `@Transactional`
1. `AuditoriaEntity entity = mapper.toEntity(request)`
2. `auditoriaRepository.persist(entity)`
3. `return mapper.toResponse(entity)`

#### `List<EventoAuditoriaResponse> listarPorServicio(String servicioOrigen, LocalDateTime desde)`
1. Si `desde == null` → `findByServicio(servicioOrigen)`
2. Si no → `findByServicioYFecha(servicioOrigen, desde)`
3. `return mapper.toResponseList(resultado)`

#### `List<EventoAuditoriaResponse> listarPorTipoEvento(String tipoEvento)`
1. `return mapper.toResponseList(repository.findByTipoEvento(tipoEvento))`

## Paso 7 — REST 

### `rest/AuditRest.java`
- `@Path("/auditoria")`, `@Produces(APPLICATION_JSON)`, `@Consumes(APPLICATION_JSON)`
- Inyecta `AuditService`

#### `POST /auditoria/eventos`
- Recibe `@Valid EventoAuditoriaRequest`
- Retorna `Response.status(201).entity(auditService.registrarEvento(request)).build()`

#### `GET /auditoria/eventos?servicio=X&desde=Y`
- `@QueryParam("servicio") String servicio`
- `@QueryParam("desde") String desde` → convertir a `LocalDateTime` si no es null
- Retorna `Response.ok(auditService.listarPorServicio(servicio, desdeDate)).build()`

#### `GET /auditoria/eventos/tipo/{tipoEvento}`
- Retorna lista de eventos de ese tipo

## Paso 8 — Health Checks

### `health/AuditLivenessCheck.java`
- Implementa `HealthCheck`
- Anotación `@Liveness`
- `call()` retorna `HealthCheckResponse.up("audit-service-live")`
- Este check siempre pasa si el proceso está corriendo

### `health/AuditReadinessCheck.java`
- Implementa `HealthCheck`
- Anotación `@Readiness`
- Inyecta `AuditoriaRepository`
- `call()`:
    1. Intenta `auditoriaRepository.count()`
    2. Si funciona → `HealthCheckResponse.up("audit-service-ready")`
    3. Si lanza excepción → `HealthCheckResponse.down("audit-service-ready")` con mensaje del error

### `health/AuditDatabaseHealthCheck.java`
- Implementa `HealthCheck`
- Anotación `@Liveness` y `@Readiness`
- Verifica conexión a PostgreSQL con un query simple
- Retorna estado con nombre `"audit-database"` y dato `"schema" = "audit_schema"`
#### IMPORTANTE — Acceso al endpoint de consulta
`GET /auditoria/eventos` y `GET /auditoria/eventos/tipo/{tipo}`
deben estar protegidos. Solo el administrador electoral debe
poder consultarlos. En producción agregar validación de JWT
con rol `ADMIN` usando `@RolesAllowed("admin")` de MicroProfile.

El endpoint `POST /auditoria/eventos` es interno — solo debe
aceptar peticiones desde los otros 3 microservicios. En
producción restringir acceso por IP o red interna de Docker/K8s,
no exponerlo al exterior a través del API Gateway.

## Paso 9 — Lifecycle

### `lifecycle/AuditStartup.java`
- `@ApplicationScoped`
- Método `onStart(@Observes StartupEvent ev)`:
    - Loguea con `Logger.getLogger(AuditStartup.class)`
    - Mensaje: `"Audit Service iniciado en puerto 8084"`
    - Loguea la versión de la aplicación
- Método `onStop(@Observes ShutdownEvent ev)`:
    - Mensaje: `"Audit Service deteniéndose — cerrando conexiones"`

## Paso 10 — Registro en Consul

### `lifecycle/ConsulRegistration.java`
- `@ApplicationScoped`
- Inyecta `@ConfigProperty(name = "quarkus.application.name") String serviceName`
- Inyecta `@ConfigProperty(name = "quarkus.http.port") int port`
- Método `onStart(@Observes StartupEvent ev)`:
    - Registra el servicio en Consul con:
        - ID: `audit-service-{hostname}`
        - Name: `audit-service`
        - Address: hostname del contenedor
        - Port: 8084
        - Health check: `http://localhost:8084/health/ready` cada 10 segundos

## Paso 11 — Métricas

### En `AuditService.java` agregar:
- Inyecta `MeterRegistry meterRegistry`
- En `registrarEvento()` agregar:
    - `meterRegistry.counter("audit.eventos.registrados", "tipo", request.tipoEvento()).increment()`
- Esto expone en `/metrics` el conteo de eventos por tipo para Prometheus/Grafana

## Dependencias `build.gradle.kts`
```kotlin
implementation(enforcedPlatform("io.quarkus.platform:quarkus-bom:3.17.5"))
implementation("io.quarkus:quarkus-resteasy-reactive")
implementation("io.quarkus:quarkus-resteasy-reactive-jackson")
implementation("io.quarkus:quarkus-hibernate-orm-panache")
implementation("io.quarkus:quarkus-jdbc-postgresql")
implementation("io.quarkus:quarkus-flyway")
implementation("io.quarkus:quarkus-arc")
implementation("io.quarkus:quarkus-hibernate-validator")
implementation("io.quarkus:quarkus-smallrye-health")
implementation("io.quarkus:quarkus-micrometer")
implementation("io.quarkus:quarkus-micrometer-registry-prometheus")
implementation("io.quarkus:quarkus-swagger-ui")
implementation("io.quarkus:quarkus-smallrye-openapi")
implementation("io.quarkus:quarkus-stork")
implementation("io.smallrye.stork:stork-service-discovery-consul")
testImplementation("io.quarkus:quarkus-junit5")
testImplementation("io.rest-assured:rest-assured")
```