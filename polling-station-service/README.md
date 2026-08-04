# Polling Station Service — Cloud Native

Microservicio para gestión de mesas electorales, padrón y habilitación de votantes. Quarkus + Gradle + Java 21
**Esquema BD:** `polling_station_schema` | **Puerto:** `8085`
**Paquete base:** `sisve.ec.pollingstation`

## Contexto del sistema

Este servicio mantiene la información de mesas de votación, miembros de mesa y el padrón (electoral_roll). Está pensado para integrarse con `vote-service` y `election-service`:

Flujo de integración:
1. Admin crea/abre/cierra mesas y carga el padrón por mesa.
2. `vote-service` llama `GET /polling-stations/election/{idElection}/voters/{idVoter}/eligibility` antes de emitir un voto.
3. Tras persistir el voto, `vote-service` llama `POST /polling-stations/election/{idElection}/voters/{idVoter}/mark-voted` para marcar participación en la mesa.
4. El servicio publica eventos a `audit-service` para trazabilidad.

## Estructura de paquetes
```
src/main/java/sisve/ec/pollingstation/
├── db/
│   ├── PollingStationEntity.java
│   ├── PollingStationMemberEntity.java
│   └── ElectoralRollEntity.java
├── repository/
│   ├── PollingStationRepository.java
│   ├── PollingStationMemberRepository.java
│   └── ElectoralRollRepository.java
├── dto/
│   ├── PollingStationResponse.java
│   ├── PollingStationMemberResponse.java
│   ├── ElectoralRollResponse.java
│   ├── PollingStationEligibilityResponse.java
│   └── PollingStationSummaryResponse.java
├── mapper/
│   └── PollingStationMapper.java
├── service/
│   └── PollingStationService.java
├── resource/
│   └── PollingStationResource.java
├── client/
│   └── AuditClient.java
├── health/
│   ├── PollingStationLivenessCheck.java
│   └── PollingStationReadinessCheck.java
└── lifecycle/
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
quarkus.flyway.schemas=polling_station_schema
quarkus.flyway.default-schema=polling_station_schema
quarkus.flyway.baseline-on-migrate=true

quarkus.http.port=${QUARKUS_HTTP_PORT:8085}

quarkus.rest-client.audit-service.url=${AUDIT_SERVICE_URL}

quarkus.micrometer.enabled=true
quarkus.micrometer.export.prometheus.enabled=true
quarkus.micrometer.export.prometheus.path=/metrics

quarkus.smallrye-health.root-path=/health
quarkus.application.name=polling-station-service
quarkus.application.version=1.0.0
```

### `credentialDeploy.env`
```env
DB_URL=jdbc:postgresql://localhost:5432/votacion_uce?currentSchema=polling_station_schema
DB_USERNAME=postgres
DB_PASSWORD=tu_password
QUARKUS_HTTP_PORT=8085
CONSUL_HOST=localhost
CONSUL_PORT=8500
AUDIT_SERVICE_URL=http://localhost:8084
```

## Paso 1 — Script Flyway

`src/main/resources/db/migration/V1__create_polling_station_schema.sql`

Este script crea las tablas principales:
- `polling_station` (mesas)
- `polling_station_member` (miembros de mesa)
- `electoral_roll` (padrón por mesa)

Contiene índices y restricciones importantes: índice único `uq_electoral_roll_election_voter` para evitar duplicados de votante por elección y `CHECK` sobre estados.

## Paso 2 — Entidades (capa db)

### `db/PollingStationEntity.java`
- `@Entity`, `@Table(name = "polling_station", schema = "polling_station_schema")`
- Campos: `idPollingStation`, `idElection`, `code`, `name`, `location`, `status` (`OPEN|CLOSED|SUSPENDED`), `openedAt`, `closedAt`.

### `db/PollingStationMemberEntity.java`
- `@Entity`, `@Table(name = "polling_station_member", schema = "polling_station_schema")`
- Campos: `idPollingStationMember`, `idPollingStation`, `userIdentifier`, `fullName`, `institutionalEmail`, `role` (`POLLING_STATION_PRESIDENT|POLLING_STATION_MEMBER`), `status`.

### `db/ElectoralRollEntity.java`
- `@Entity`, `@Table(name = "electoral_roll", schema = "polling_station_schema")`
- Campos: `idElectoralRoll`, `idPollingStation`, `idElection`, `idVoter`, `cedula`, `fullName`, `institutionalEmail`, `participationStatus` (`PENDING|ENABLED|VOTED|BLOCKED`), timestamps y datos de bloqueo/habilitación.

## Paso 3 — Repositorios

`PollingStationRepository`, `PollingStationMemberRepository` y `ElectoralRollRepository` usan Panache para consultas comunes:
- `listarPorEleccion(idElection)`
- `obtenerPadron(idPollingStation)`
- `buscarVotantePorCedula(idPollingStation, cedula)`
- `findByPollingStationAndVoter(idPollingStation, idVoter)`
- contadores por `participationStatus`

## Paso 4 — DTOs

Incluye respuestas y requests para:
- `PollingStationResponse`, `PollingStationMemberResponse`, `ElectoralRollResponse`
- `PollingStationEligibilityResponse` (retorna `eligible: Boolean`, `participationStatus`, `pollingStationStatus`)
- `PollingStationSummaryResponse` (conteos: pending/enabled/voted/blocked)

## Paso 5 — Mapper

`PollingStationMapper` convierte entre entidades y DTOs.

## Paso 6 — Cliente REST

`client/AuditClient.java` — `@RegisterRestClient(configKey = "audit-service")` con `POST /auditoria/eventos`.

`vote-service` usa este servicio mediante dos endpoints expuestos:
- `GET /polling-stations/election/{idElection}/voters/{idVoter}/eligibility`
- `POST /polling-stations/election/{idElection}/voters/{idVoter}/mark-voted`

## Paso 7 — Service

`PollingStationService` provee operaciones principales:
1. `listarPorEleccion(Long idElection)` — lista mesas.
2. `obtenerPorId(Long idPollingStation)` — detalle de mesa.
3. `obtenerPadron(Long idPollingStation)` — devuelve padrón.
4. `buscarVotantePorCedula(Long idPollingStation, String cedula)` — búsqueda por cédula.
5. `habilitarVotante`, `bloquearVotante` — cambia `participationStatus` y registra `enabled_by` / `blocked_by`.
6. `validarElegibilidad(Long idElection, Long idVoter)` — usado por `vote-service` para permitir/denegar la emisión del voto.
7. `marcarVotanteComoVotado(Long idElection, Long idVoter)` — marca `participationStatus=VOTED` y `voted_at`.
8. `abrirMesa` / `cerrarMesa` — gestión del estado de la mesa.
9. `resumen(Long idPollingStation)` — devuelve conteos por estado.

Todas las operaciones no críticas con otros servicios (ej. `audit-service`) se llaman de forma segura (try/catch) para no romper la operación principal.

## Paso 8 — REST (endpoints)

`PollingStationResource` expone:
- `GET /polling-stations/election/{idElection}` → listar mesas por elección
- `GET /polling-stations/{idPollingStation}` → obtener mesa
- `GET /polling-stations/{idPollingStation}/electoral-roll` → obtener padrón
- `GET /polling-stations/{idPollingStation}/voters/search?cedula=...` → buscar votante
- `POST /polling-stations/{idPollingStation}/voters/{idVoter}/enable` → habilitar votante
- `POST /polling-stations/{idPollingStation}/voters/{idVoter}/block` → bloquear votante
- `GET /polling-stations/election/{idElection}/voters/{idVoter}/eligibility` → verificar elegibilidad (devuelve `eligible`)
- `POST /polling-stations/election/{idElection}/voters/{idVoter}/mark-voted` → marcar votado
- `POST /polling-stations/{idPollingStation}/open` → abrir mesa
- `POST /polling-stations/{idPollingStation}/close` → cerrar mesa
- `GET /polling-stations/{idPollingStation}/summary` → resumen de la mesa

## Datos de prueba local

Archivo: `src/main/resources/dev/local-dev-reset-and-seed.sql` (SOLO DESARROLLO)

El script deja un entorno listo para pruebas:
- Crea una mesa asociada a `id_election = 1` y algunos votantes con `participation_status = PENDING`.

## Paso 9 — Health Checks

`PollingStationLivenessCheck` — `@Liveness` retorna `up("polling-station-service-live")`.
`PollingStationReadinessCheck` — `@Readiness` verifica acceso a repositorios y la BD.

## Paso 10 — Lifecycle y Consul

`ConsulRegistration` registra el servicio en Consul: Name=`polling-station-service`, Port=8085. Health check: `http://localhost:8085/health/ready`.

## Garantías operativas

- Un votante por elección: `uq_electoral_roll_election_voter` garantiza que no existan duplicados a nivel de BD.
- Inmutabilidad parcial: Una vez marcado `VOTED`, la aplicación evita revertir el estado; los cambios se documentan en auditoría.
- Robustez: llamadas a `audit-service` se ejecutan de forma no bloqueante (no invalidan la operación si fallan).

## Dependencias `build.gradle.kts`
```kotlin
implementation(enforcedPlatform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}"))
implementation("io.quarkus:quarkus-rest")
implementation("io.quarkus:quarkus-rest-jackson")
implementation("io.quarkus:quarkus-hibernate-orm-panache")
implementation("io.quarkus:quarkus-jdbc-postgresql")
implementation("io.quarkus:quarkus-flyway")
implementation("io.quarkus:quarkus-rest-client")
implementation("io.quarkus:quarkus-rest-client-jackson")
implementation("io.quarkus:quarkus-arc")
implementation("io.quarkus:quarkus-hibernate-validator")
implementation("io.quarkus:quarkus-smallrye-health")
implementation("io.quarkus:quarkus-micrometer")
implementation("io.quarkus:quarkus-micrometer-registry-prometheus")
implementation("io.quarkus:quarkus-smallrye-openapi")
implementation("io.quarkus:quarkus-swagger-ui")
implementation("io.quarkus:quarkus-smallrye-stork")
implementation("io.smallrye.stork:stork-service-discovery-consul")
testImplementation("io.quarkus:quarkus-junit5")
testImplementation("io.rest-assured:rest-assured")
```

---

Si quieres, puedo:
- Ejecutar el script de seed local y mostrar los comandos PowerShell para probar el flujo.
- Añadir ejemplos de llamadas `curl`/PowerShell para login → validar → elegibilidad → votar.

# polling-station-service

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./gradlew quarkusDev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Packaging and running the application

The application can be packaged using:

```shell script
./gradlew build
```

It produces the `quarkus-run.jar` file in the `build/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `build/quarkus-app/lib/` directory.

The application is now runnable using `java -jar build/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./gradlew build -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar build/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./gradlew build -Dquarkus.native.enabled=true
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./gradlew build -Dquarkus.native.enabled=true -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./build/polling-station-service-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/gradle-tooling>.

## Related Guides

- REST ([guide](https://quarkus.io/guides/rest)): Build RESTful web services and APIs using Jakarta REST (formerly JAX-RS)
- Flyway ([guide](https://quarkus.io/guides/flyway)): Handle your database schema migrations
- JDBC Driver - PostgreSQL ([guide](https://quarkus.io/guides/datasource)): Connect to the PostgreSQL database via JDBC

## Provided Code

### REST

Easily start your REST Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)
