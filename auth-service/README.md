# Auth Service — Cloud Native

Microservicio de autenticación. Quarkus + Gradle + Java 21
**Esquema BD:** `auth_schema` | **Puerto:** `8081`
**Paquete base:** `sisve.ec.auth`

## Contexto del sistema

Primer servicio en el flujo. Todos los demás validan el JWT
que este servicio emite.

Flujo de integración:
1. Votante → `POST /auth/login` → recibe JWT
2. Vote Service → `GET /auth/validate` → verifica JWT antes de registrar voto
3. Vote Service → `POST /auth/logout` → invalida token después de votar

## Estructura de paquetes
```
src/main/java/sisve/ec/auth/
├── db/
│   ├── VotanteEntity.java
│   └── SesionEntity.java
├── repository/
│   ├── VotanteRepository.java
│   └── SesionRepository.java
├── dto/
│   ├── LoginRequest.java
│   └── LoginResponse.java
├── mapper/
│   └── AuthMapper.java
├── service/
│   └── AuthService.java
├── rest/
│   └── AuthRest.java
├── security/
│   └── JwtTokenGenerator.java
├── client/
│   └── AuditClient.java
├── health/
│   ├── AuthLivenessCheck.java
│   └── AuthReadinessCheck.java
└── lifecycle/
├── AuthStartup.java
└── ConsulRegistration.java
```
## Configuración

### `application.properties`
```properties
quarkus.datasource.db-kind=postgresql
quarkus.datasource.username=${DB_USERNAME}
quarkus.datasource.password=${DB_PASSWORD}
quarkus.datasource.jdbc.url=${DB_URL}

quarkus.flyway.migrate-at-start=true
quarkus.flyway.schemas=auth_schema
quarkus.flyway.default-schema=auth_schema
quarkus.flyway.baseline-on-migrate=true

quarkus.http.port=${QUARKUS_HTTP_PORT:8081}

mp.jwt.verify.publickey.location=publicKey.pem
mp.jwt.verify.issuer=sisve-auth
smallrye.jwt.sign.key.location=privateKey.pem

quarkus.rest-client.audit-service.url=${AUDIT_SERVICE_URL}

quarkus.stork.audit-service.service-discovery.type=consul
quarkus.stork.audit-service.service-discovery.consul-host=${CONSUL_HOST:localhost}

quarkus.micrometer.enabled=true
quarkus.micrometer.export.prometheus.enabled=true
quarkus.micrometer.export.prometheus.path=/metrics

quarkus.smallrye-health.root-path=/health
quarkus.smallrye-health.liveness-path=/health/live
quarkus.smallrye-health.readiness-path=/health/ready

quarkus.application.name=auth-service
quarkus.application.version=1.0.0
```

### `credentialDeploy.env`
```env
DB_URL=jdbc:postgresql://localhost:5432/votacion_uce?currentSchema=auth_schema
DB_USERNAME=postgres
DB_PASSWORD=tu_password
QUARKUS_HTTP_PORT=8081
CONSUL_HOST=localhost
CONSUL_PORT=8500
AUDIT_SERVICE_URL=http://localhost:8084
```

## Paso 1 — Script Flyway

`src/main/resources/db/migration/V1__create_auth_schema.sql`

```sql
CREATE SCHEMA IF NOT EXISTS auth_schema;

CREATE TABLE auth_schema.votante (
    id_votante SERIAL PRIMARY KEY,
    cedula VARCHAR(10) UNIQUE NOT NULL,
    correo_institucional VARCHAR(100) UNIQUE NOT NULL,
    nombres VARCHAR(80) NOT NULL,
    apellidos VARCHAR(80) NOT NULL,
    estado BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE auth_schema.sesion (
    id_sesion SERIAL PRIMARY KEY,
    id_votante INTEGER NOT NULL REFERENCES auth_schema.votante(id_votante),
    token_hash VARCHAR(256) NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT now(),
    fecha_expiracion TIMESTAMP NOT NULL,
    estado BOOLEAN NOT NULL DEFAULT true
);
```

## Paso 2 — Entidades (capa db)

### `db/VotanteEntity.java`
- `@Entity`, `@Table(name = "votante", schema = "auth_schema")`
- Extiende `PanacheEntityBase`
- Campos: `idVotante` (Long, PK IDENTITY), `cedula` (String, unique, not null), `correoInstitucional` (String, unique, not null), `nombres` (String), `apellidos` (String), `estado` (Boolean, default true)

### `db/SesionEntity.java`
- `@Entity`, `@Table(name = "sesion", schema = "auth_schema")`
- Extiende `PanacheEntityBase`
- Campos: `idSesion` (Long, PK IDENTITY), `idVotante` (Long, not null), `tokenHash` (String, not null), `fechaCreacion` (LocalDateTime), `fechaExpiracion` (LocalDateTime), `estado` (Boolean, default true)

## Paso 3 — Repositorios

### `repository/VotanteRepository.java`
- `@ApplicationScoped`, `PanacheRepository<VotanteEntity>`
- `Optional<VotanteEntity> findByCedulaAndCorreo(String cedula, String correo)`
    - `find("cedula = ?1 and correoInstitucional = ?2", cedula, correo).firstResultOptional()`

### `repository/SesionRepository.java`
- `@ApplicationScoped`, `PanacheRepository<SesionEntity>`
- `Optional<SesionEntity> findByTokenHash(String tokenHash)`
    - `find("tokenHash = ?1 and estado = true", tokenHash).firstResultOptional()`
- `void invalidarSesion(Long idSesion)` — `@Transactional`
    - `update("estado = false where idSesion = ?1", idSesion)`

## Paso 4 — DTOs

### `dto/LoginRequest.java`
```java
public record LoginRequest(
    @NotBlank String cedula,
    @NotBlank String correoInstitucional
) {}
```

### `dto/LoginResponse.java`
```java
public record LoginResponse(
    String token,
    Long idVotante,
    String nombres,
    String apellidos
) {}
```

## Paso 5 — Mapper

### `mapper/AuthMapper.java`
- `@ApplicationScoped`
- `SesionEntity toSesionEntity(Long idVotante, String tokenHash, LocalDateTime expiracion)`:
    - Crea entidad con `fechaCreacion = now()`, `estado = true`
- `LoginResponse toLoginResponse(VotanteEntity votante, String token)`:
    - Crea `LoginResponse` con datos del votante y el token

## Paso 6 — JWT Generator

### `security/JwtTokenGenerator.java`
- `@ApplicationScoped`
- Usa `io.smallrye.jwt.build.Jwt`
- `String generarToken(VotanteEntity votante)`:
    - `subject` = cedula
    - Claim `"idVotante"` = votante.idVotante
    - Claim `"upn"` = correoInstitucional
    - Issuer: `"sisve-auth"`
    - Expiración: 30 minutos
    - Retorna token firmado con `Jwt.claims().sign()`
- `String calcularHash(String token)`:
    - SHA-256 del token
    - Retorna hex string de 64 caracteres usando `MessageDigest.getInstance("SHA-256")`

## Paso 7 — Cliente REST hacia Audit Service

### `client/AuditClient.java`
- `@RegisterRestClient(configKey = "audit-service")`
- `@Path("/auditoria/eventos")`
- `@POST void registrarEvento(EventoAuditoriaDTO evento)`
- `EventoAuditoriaDTO` es un record: `String tipoEvento`, `String descripcion`, `String servicioOrigen`

## Paso 8 — Service

### `service/AuthService.java`
- `@ApplicationScoped`
- Inyecta: `VotanteRepository`, `SesionRepository`, `JwtTokenGenerator`, `AuthMapper`, `AuditClient`, `MeterRegistry`

#### `LoginResponse login(LoginRequest request)` — `@Transactional`
1. Busca votante con `findByCedulaAndCorreo` → si no existe lanza `WebApplicationException(401)`
2. Verifica `votante.estado == true` → si no lanza 403
3. Genera token: `String token = jwtGenerator.generarToken(votante)`
4. Calcula hash: `String tokenHash = jwtGenerator.calcularHash(token)`
5. Crea sesión: `mapper.toSesionEntity(votante.idVotante, tokenHash, now().plusMinutes(30))`
6. Persiste sesión
7. Llama `auditClient.registrarEvento("LOGIN_EXITOSO", "Votante " + votante.cedula, "auth-service")`
8. Incrementa métrica: `meterRegistry.counter("auth.login.exitoso").increment()`
9. Retorna `mapper.toLoginResponse(votante, token)`

#### `void logout(String tokenHash)` — `@Transactional`
1. Busca sesión con `findByTokenHash`
2. Si existe → `invalidarSesion`, registra auditoría `"LOGOUT"`

#### `boolean validarToken(String tokenHash)`
1. Busca sesión activa
2. Si no existe → retorna `false`
3. Si `fechaExpiracion.isBefore(now())` → invalida y retorna `false`
4. Retorna `true`

## Paso 9 — REST 

### `rest/AuthRest.java`
- `@Path("/auth")`, `@Produces(APPLICATION_JSON)`, `@Consumes(APPLICATION_JSON)`

#### `POST /auth/login` → `authService.login(request)`
#### `POST /auth/logout` → extrae token, calcula hash, `authService.logout(hash)`
#### `GET /auth/validate` → extrae token, calcula hash, retorna `Map.of("valido", authService.validarToken(hash))`

## Paso 10 — Health Checks

### `health/AuthLivenessCheck.java`
- `@Liveness`, implementa `HealthCheck`
- Retorna `HealthCheckResponse.up("auth-service-live")`

### `health/AuthReadinessCheck.java`
- `@Readiness`, implementa `HealthCheck`
- Inyecta `VotanteRepository`
- Intenta `votanteRepository.count()` → si funciona retorna `up`, si falla retorna `down` con mensaje de error

## Paso 11 — Lifecycle y Consul

### `lifecycle/AuthStartup.java`
- `@ApplicationScoped`
- `onStart(@Observes StartupEvent ev)`: loguea inicio del servicio
- `onStop(@Observes ShutdownEvent ev)`: loguea detención

### `lifecycle/ConsulRegistration.java`
- `@ApplicationScoped`
- `onStart(@Observes StartupEvent ev)`:
    - Registra en Consul: ID=`auth-service-{hostname}`, Name=`auth-service`, Port=8081
    - Health check URL: `http://localhost:8081/health/ready`
## Consideraciones de seguridad del token

### El tokenHash en la tabla sesion
La tabla `sesion` almacena el SHA-256 del token JWT, nunca el token
en texto plano. Esto garantiza que si la base de datos es comprometida,
los tokens no pueden ser reutilizados directamente.

### Invalidación automática por tiempo
El método `validarToken` verifica la fecha de expiración en cada llamada.
Si un token expiró, se invalida automáticamente en la BD (estado=false)
aunque nadie haya hecho logout explícitamente.

### Logs y secreto del voto
En ningún log ni evento de auditoría enviado desde auth-service
debe aparecer información sobre qué elección consultó el votante
ni qué candidato seleccionó. El auth-service solo conoce identidad,
nunca preferencia electoral.

### Rotación de claves JWT
Las claves `privateKey.pem` y `publicKey.pem` deben:
- Generarse antes del proceso electoral con `openssl genrsa`
- Almacenarse de forma segura (nunca en el repositorio Git)
- Incluirse en `.gitignore`
- Regenerarse para cada proceso electoral diferente

## Dependencias `build.gradle.kts`
```kotlin
implementation(enforcedPlatform("io.quarkus.platform:quarkus-bom:3.17.5"))
implementation("io.quarkus:quarkus-resteasy-reactive")
implementation("io.quarkus:quarkus-resteasy-reactive-jackson")
implementation("io.quarkus:quarkus-hibernate-orm-panache")
implementation("io.quarkus:quarkus-jdbc-postgresql")
implementation("io.quarkus:quarkus-flyway")
implementation("io.quarkus:quarkus-smallrye-jwt-build")
implementation("io.quarkus:quarkus-smallrye-jwt")
implementation("io.quarkus:quarkus-rest-client-reactive")
implementation("io.quarkus:quarkus-rest-client-reactive-jackson")
implementation("io.quarkus:quarkus-arc")
implementation("io.quarkus:quarkus-hibernate-validator")
implementation("io.quarkus:quarkus-smallrye-health")
implementation("io.quarkus:quarkus-micrometer")
implementation("io.quarkus:quarkus-micrometer-registry-prometheus")
implementation("io.quarkus:quarkus-smallrye-openapi")
implementation("io.quarkus:quarkus-swagger-ui")
implementation("io.quarkus:quarkus-stork")
implementation("io.smallrye.stork:stork-service-discovery-consul")
testImplementation("io.quarkus:quarkus-junit5")
testImplementation("io.rest-assured:rest-assured")
```