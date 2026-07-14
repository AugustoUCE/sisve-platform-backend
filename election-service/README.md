pu# Election Service — Cloud Native

Microservicio de gestión electoral. Quarkus + Gradle + Java 21
**Esquema BD:** `election_schema` | **Puerto:** `8082`
**Paquete base:** `sisve.ec.election`

## Contexto del sistema

Gestiona elecciones, cargos, candidatos y el padrón electoral.
Vote Service lo consulta para verificar habilitación y marcar participación.

Flujo de integración:
1. Admin configura elección, cargos, candidatos y carga padrón
2. Vote Service llama `GET /elecciones/{id}/habilitado/{idVotante}` antes de votar
3. Vote Service llama `PUT /elecciones/{id}/marcar-votado/{idVotante}` después de votar
4. Este servicio envía todos los eventos a Audit Service

## Estructura de paquetes
```
src/main/java/sisve/ec/election/
├── db/
│   ├── EleccionEntity.java
│   ├── CargoEntity.java
│   ├── CandidatoEntity.java
│   └── VotanteEleccionEntity.java
├── repository/
│   ├── EleccionRepository.java
│   ├── CargoRepository.java
│   ├── CandidatoRepository.java
│   └── VotanteEleccionRepository.java
├── dto/
│   ├── EleccionRequest.java
│   ├── EleccionResponse.java
│   ├── CargoRequest.java
│   ├── CandidatoRequest.java
│   ├── CandidatoResponse.java
│   └── PadronCargaRequest.java
├── mapper/
│   └── ElectionMapper.java
├── service/
│   └── ElectionService.java
├── rest/
│   ├── EleccionRest.java
│   ├── CargoRest.java
│   └── CandidatoRest.java
├── client/
│   └── AuditClient.java
├── health/
│   ├── ElectionLivenessCheck.java
│   └── ElectionReadinessCheck.java
└── lifecycle/
├── ElectionStartup.java
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
quarkus.flyway.schemas=election_schema
quarkus.flyway.default-schema=election_schema
quarkus.flyway.baseline-on-migrate=true

quarkus.http.port=${QUARKUS_HTTP_PORT:8082}

quarkus.rest-client.audit-service.url=${AUDIT_SERVICE_URL}
quarkus.stork.audit-service.service-discovery.type=consul
quarkus.stork.audit-service.service-discovery.consul-host=${CONSUL_HOST:localhost}

quarkus.micrometer.enabled=true
quarkus.micrometer.export.prometheus.enabled=true
quarkus.micrometer.export.prometheus.path=/metrics

quarkus.smallrye-health.root-path=/health
quarkus.application.name=election-service
quarkus.application.version=1.0.0
```

### `credentialDeploy.env`
```env
DB_URL=jdbc:postgresql://localhost:5432/votacion_uce?currentSchema=election_schema
DB_USERNAME=postgres
DB_PASSWORD=tu_password
QUARKUS_HTTP_PORT=8082
CONSUL_HOST=localhost
CONSUL_PORT=8500
AUDIT_SERVICE_URL=http://localhost:8084
```

## Paso 1 — Script Flyway

`src/main/resources/db/migration/V1__create_election_schema.sql`

```sql
CREATE SCHEMA IF NOT EXISTS election_schema;

CREATE TABLE election_schema.eleccion (
    id_eleccion SERIAL PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL,
    descripcion TEXT,
    fecha_inicio TIMESTAMP NOT NULL,
    fecha_fin TIMESTAMP NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'planificado'
);

CREATE TABLE election_schema.cargo (
    id_cargo SERIAL PRIMARY KEY,
    id_eleccion INTEGER NOT NULL REFERENCES election_schema.eleccion(id_eleccion),
    nombre VARCHAR(80) NOT NULL
);

CREATE TABLE election_schema.candidato (
    id_candidato SERIAL PRIMARY KEY,
    id_cargo INTEGER NOT NULL REFERENCES election_schema.cargo(id_cargo),
    nombres VARCHAR(80) NOT NULL,
    apellidos VARCHAR(80) NOT NULL,
    lista VARCHAR(50),
    estado BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE election_schema.votante_eleccion (
    id_votante INTEGER NOT NULL,
    id_eleccion INTEGER NOT NULL REFERENCES election_schema.eleccion(id_eleccion),
    ha_votado BOOLEAN NOT NULL DEFAULT false,
    fecha_participacion TIMESTAMP,
    PRIMARY KEY (id_votante, id_eleccion)
);
-- Garantía de un voto por votante por elección a nivel de base de datos
-- Esto previene race conditions aunque dos peticiones lleguen simultáneas
CREATE UNIQUE INDEX idx_votante_eleccion_unico
    ON election_schema.votante_eleccion (id_votante, id_eleccion)
    WHERE ha_votado = false;

-- Impide que ha_votado regrese de true a false
-- Se implementa a nivel de aplicación pero se refuerza con trigger
CREATE OR REPLACE FUNCTION election_schema.prevent_unvote()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.ha_votado = true AND NEW.ha_votado = false THEN
        RAISE EXCEPTION 'No se puede revertir un voto emitido';
END IF;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_prevent_unvote
    BEFORE UPDATE ON election_schema.votante_eleccion
    FOR EACH ROW EXECUTE FUNCTION election_schema.prevent_unvote();
```

## Paso 2 — Entidades (capa db)

### `db/EleccionEntity.java`
- `@Entity`, `@Table(name = "eleccion", schema = "election_schema")`
- Extiende `PanacheEntityBase`
- Campos: `idEleccion` (Long, PK IDENTITY), `nombre`, `descripcion`, `fechaInicio` (LocalDateTime), `fechaFin` (LocalDateTime), `estado` (String, default "planificado")

### `db/CargoEntity.java`
- `@Entity`, `@Table(name = "cargo", schema = "election_schema")`
- Campos: `idCargo` (Long, PK IDENTITY), `idEleccion` (Long, not null), `nombre` (String)

### `db/CandidatoEntity.java`
- `@Entity`, `@Table(name = "candidato", schema = "election_schema")`
- Campos: `idCandidato` (Long, PK IDENTITY), `idCargo` (Long, not null — FK a Cargo, NO a Eleccion), `nombres`, `apellidos`, `lista` (nullable), `estado` (Boolean, default true)

### `db/VotanteEleccionEntity.java`
- `@Entity`, `@Table(name = "votante_eleccion", schema = "election_schema")`
- Clave compuesta `@EmbeddedId` con clase interna `VotanteEleccionId`: `idVotante` Long, `idEleccion` Long
- Campos adicionales: `haVotado` (Boolean, default false), `fechaParticipacion` (LocalDateTime, nullable)

## Paso 3 — Repositorios

### `repository/EleccionRepository.java`
- `PanacheRepository<EleccionEntity>`
- `List<EleccionEntity> findActivas()` → estado=activo y fechas vigentes

### `repository/CargoRepository.java`
- `List<CargoEntity> findByEleccion(Long idEleccion)` → `list("idEleccion", idEleccion)`

### `repository/CandidatoRepository.java`
- `List<CandidatoEntity> findByCargo(Long idCargo)` → `list("idCargo = ?1 and estado = true", idCargo)`
- `void deshabilitar(Long idCandidato)` → `update("estado = false where idCandidato = ?1", idCandidato)`

### `repository/VotanteEleccionRepository.java`
- `boolean estaHabilitado(Long idVotante, Long idEleccion)` → busca por PK compuesta, retorna true si existe y haVotado=false
- `void marcarVotado(Long idVotante, Long idEleccion)` → actualiza haVotado=true, fechaParticipacion=now()

## Paso 4 — DTOs

### `dto/EleccionRequest.java`
```java
public record EleccionRequest(
    @NotBlank String nombre,
    String descripcion,
    @NotNull LocalDateTime fechaInicio,
    @NotNull LocalDateTime fechaFin
) {}
```

### `dto/EleccionResponse.java`
```java
public record EleccionResponse(
    Long idEleccion, String nombre,
    String descripcion, LocalDateTime fechaInicio,
    LocalDateTime fechaFin, String estado
) {}
```

### `dto/CargoRequest.java`
```java
public record CargoRequest(@NotBlank String nombre) {}
```

### `dto/CandidatoRequest.java`
```java
public record CandidatoRequest(
    @NotBlank String nombres,
    @NotBlank String apellidos,
    String lista
) {}
```

### `dto/CandidatoResponse.java`
```java
public record CandidatoResponse(
    Long idCandidato, Long idCargo,
    String nombres, String apellidos,
    String lista, Boolean estado
) {}
```

### `dto/PadronCargaRequest.java`
```java
public record PadronCargaRequest(
    @NotEmpty List<Long> idsVotantes
) {}
```

## Paso 5 — Mapper

### `mapper/ElectionMapper.java`
- `@ApplicationScoped`
- `EleccionEntity toEntity(EleccionRequest request)` → estado default "planificado"
- `EleccionResponse toResponse(EleccionEntity entity)`
- `CargoEntity toCargoEntity(Long idEleccion, CargoRequest request)`
- `CandidatoEntity toCandidatoEntity(Long idCargo, CandidatoRequest request)`
- `CandidatoResponse toCandidatoResponse(CandidatoEntity entity)`
- `List<CandidatoResponse> toCandidatoResponseList(List<CandidatoEntity> entities)`

## Paso 6 — Cliente REST

### `client/AuditClient.java`
- `@RegisterRestClient(configKey = "audit-service")`
- `@Path("/auditoria/eventos")`
- `@POST void registrarEvento(EventoAuditoriaDTO evento)`

## Paso 7 — Service

### `service/ElectionService.java`
- `@ApplicationScoped`
- Inyecta todos los repositorios, mapper, auditClient, meterRegistry

#### `EleccionResponse crearEleccion(EleccionRequest request)` — `@Transactional`
1. `entity = mapper.toEntity(request)`
2. Persiste
3. Auditoría: `"ELECCION_CREADA"`
4. Métrica: `meterRegistry.counter("election.elecciones.creadas").increment()`
5. Retorna `mapper.toResponse(entity)`

#### `CargoEntity crearCargo(Long idEleccion, CargoRequest request)` — `@Transactional`
1. Verifica que exista elección → si no lanza 404
2. Crea y persiste cargo
3. Auditoría: `"CARGO_CREADO"`

#### `CandidatoResponse crearCandidato(Long idCargo, CandidatoRequest request)` — `@Transactional`
1. Verifica que exista cargo → si no lanza 404
2. Crea y persiste candidato
3. Auditoría: `"CANDIDATO_REGISTRADO"`
4. Retorna `mapper.toCandidatoResponse(entity)`

#### `void cargarPadron(Long idEleccion, PadronCargaRequest request)` — `@Transactional`
1. Para cada idVotante crea VotanteEleccionEntity con haVotado=false
2. Persiste todos en lote
3. Auditoría: `"PADRON_CARGADO"` con descripción = cantidad de votantes cargados

#### `boolean verificarHabilitado(Long idEleccion, Long idVotante)`
1. Retorna `votanteEleccionRepository.estaHabilitado(idVotante, idEleccion)`

#### `void marcarVotado(Long idEleccion, Long idVotante)` — `@Transactional`
1. Verifica habilitado → si no lanza 403
2. `votanteEleccionRepository.marcarVotado(idVotante, idEleccion)`
3. Auditoría: `"PARTICIPACION_REGISTRADA"`
4. Métrica: `meterRegistry.counter("election.votos.marcados").increment()`

## Paso 8 — REST 

### `rest/EleccionRest.java` — `@Path("/elecciones")`
- `POST /elecciones` → crearEleccion
- `GET /elecciones/activas` → listar activas
- `POST /elecciones/{id}/cargos` → crearCargo
- `POST /elecciones/{id}/padron` → cargarPadron
- `GET /elecciones/{id}/habilitado/{idVotante}` → `Map.of("habilitado", verificarHabilitado)`
- `PUT /elecciones/{id}/marcar-votado/{idVotante}` → marcarVotado

### `rest/CargoRest.java` — `@Path("/cargos")`
- `POST /cargos/{id}/candidatos` → crearCandidato
- `GET /cargos/{id}/candidatos` → listarPorCargo

## Paso 9 — Health Checks

### `health/ElectionLivenessCheck.java`
- `@Liveness` → retorna `up("election-service-live")`

### `health/ElectionReadinessCheck.java`
- `@Readiness`
- Inyecta `EleccionRepository`
- Intenta `eleccionRepository.count()` → up si funciona, down si falla

## Paso 10 — Lifecycle y Consul

### `lifecycle/ElectionStartup.java`
- `onStart`: loguea inicio, cuenta elecciones activas en BD
- `onStop`: loguea detención

### `lifecycle/ConsulRegistration.java`
- Registra en Consul: Name=`election-service`, Port=8082
- Health check: `http://localhost:8082/health/ready`
## Garantías electorales a nivel de base de datos

Esta sección es crítica para la integridad del proceso electoral.

### Un votante, un voto (anti race condition)
El índice único `idx_votante_eleccion_unico` en la tabla
`votante_eleccion` garantiza que aunque dos peticiones del
mismo votante lleguen simultáneamente, la base de datos
rechazará la segunda con un error de violación de restricción.
La capa de servicio debe capturar ese error y retornar 403.

### Irreversibilidad del voto
El trigger `trg_prevent_unvote` impide a nivel de base de datos
que `ha_votado` cambie de `true` a `false`, incluso si alguien
accede directamente a PostgreSQL. Ninguna operación de la
aplicación ni del administrador puede revertir un voto emitido.

### En VotanteEleccionRepository.java agregar:
Método `void marcarVotado(Long idVotante, Long idEleccion)`
debe capturar `PersistenceException` y lanzar
`WebApplicationException(409)` con mensaje
"El votante ya emitió su voto en este proceso electoral".

## Dependencias `build.gradle.kts`
```kotlin
implementation(enforcedPlatform("io.quarkus.platform:quarkus-bom:3.17.5"))
implementation("io.quarkus:quarkus-resteasy-reactive")
implementation("io.quarkus:quarkus-resteasy-reactive-jackson")
implementation("io.quarkus:quarkus-hibernate-orm-panache")
implementation("io.quarkus:quarkus-jdbc-postgresql")
implementation("io.quarkus:quarkus-flyway")
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