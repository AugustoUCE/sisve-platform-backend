# Vote Service — Cloud Native

Microservicio de votación. Quarkus + Gradle + Java 21
**Esquema BD:** `vote_schema` | **Puerto:** `8083`
**Paquete base:** `sisve.ec.vote`

## Contexto del sistema

Microservicio más crítico. Orquesta la emisión del voto
validando con Auth y Election antes de registrar nada.

Flujo completo:
1. Recibe `POST /votos` con JWT en header e idCandidato+idEleccion en body
2. Valida JWT con Auth Service `GET /auth/validate`
3. Verifica habilitación con Election Service `GET /elecciones/{id}/habilitado/{idVotante}`
4. Cifra idCandidato con AES-256
5. Calcula hash encadenado SHA-256
6. Guarda voto cifrado (sin datos del votante)
7. Marca participación en Election Service
8. Invalida token en Auth Service
9. Registra evento en Audit Service

Después de un voto exitoso, `VoteService` llama a `/auth/logout` con el mismo `Authorization: Bearer ...`.
Impacto en frontend: después de la confirmación de voto, no se debe seguir reutilizando ese token; lo correcto es mostrar la pantalla de confirmación y luego regresar al login o reiniciar la sesión.

## Estructura de paquetes
```
src/main/java/sisve/ec/vote/
├── db/
│   └── VotoEntity.java
├── repository/
│   └── VotoRepository.java
├── dto/
│   ├── VotoRequest.java
│   └── VotoResponse.java
├── mapper/
│   └── VotoMapper.java
├── service/
│   └── VoteService.java
├── rest/
│   └── VoteRest.java
├── client/
│   ├── AuthClient.java
│   ├── ElectionClient.java
│   └── AuditClient.java
├── crypto/
│   ├── AesEncryptionUtil.java
│   └── Sha256ChainUtil.java
├── health/
│   ├── VoteLivenessCheck.java
│   └── VoteReadinessCheck.java
└── lifecycle/
├── VoteStartup.java
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
quarkus.flyway.schemas=vote_schema
quarkus.flyway.default-schema=vote_schema
quarkus.flyway.baseline-on-migrate=true

quarkus.http.port=${QUARKUS_HTTP_PORT:8083}

quarkus.rest-client.auth-service.url=${AUTH_SERVICE_URL}
quarkus.rest-client.election-service.url=${ELECTION_SERVICE_URL}
quarkus.rest-client.audit-service.url=${AUDIT_SERVICE_URL}
quarkus.rest-client.polling-station-service.url=${POLLING_STATION_SERVICE_URL:http://localhost:8085}

quarkus.stork.auth-service.service-discovery.type=consul
quarkus.stork.auth-service.service-discovery.consul-host=${CONSUL_HOST:localhost}
quarkus.stork.election-service.service-discovery.type=consul
quarkus.stork.election-service.service-discovery.consul-host=${CONSUL_HOST:localhost}
quarkus.stork.audit-service.service-discovery.type=consul
quarkus.stork.audit-service.service-discovery.consul-host=${CONSUL_HOST:localhost}

quarkus.micrometer.enabled=true
quarkus.micrometer.export.prometheus.enabled=true
quarkus.micrometer.export.prometheus.path=/metrics

quarkus.smallrye-health.root-path=/health
quarkus.application.name=vote-service
quarkus.application.version=1.0.0

vote.aes.secret.key=${AES_SECRET_KEY}
```

### `credentialDeploy.env`
```env
DB_URL=jdbc:postgresql://localhost:5432/votacion_uce?currentSchema=vote_schema
DB_USERNAME=postgres
DB_PASSWORD=tu_password
QUARKUS_HTTP_PORT=8083
CONSUL_HOST=localhost
CONSUL_PORT=8500
AUTH_SERVICE_URL=http://localhost:8081
ELECTION_SERVICE_URL=http://localhost:8082
AUDIT_SERVICE_URL=http://localhost:8084
POLLING_STATION_SERVICE_URL=http://localhost:8085
AES_SECRET_KEY=clave_de_32_bytes_en_base64_aqui=
```

## Paso 1 — Script Flyway

`src/main/resources/db/migration/V1__create_vote_schema.sql`

```sql
CREATE SCHEMA IF NOT EXISTS vote_schema;

CREATE TABLE vote_schema.voto (
    id_voto SERIAL PRIMARY KEY,
    id_eleccion INTEGER NOT NULL,
    voto_cifrado TEXT NOT NULL,
    hash_anterior VARCHAR(64) NOT NULL,
    hash_actual VARCHAR(64) NOT NULL UNIQUE,
    fecha_registro TIMESTAMP NOT NULL DEFAULT now()
);
```

## Paso 2 — Entidad

### `db/VotoEntity.java`
- `@Entity`, `@Table(name = "voto", schema = "vote_schema")`
- Extiende `PanacheEntityBase`
- Campos:
    - `idVoto` Long, PK IDENTITY
    - `idEleccion` Long, not null
    - `votoCifrado` String, columnDefinition="TEXT", not null
    - `hashAnterior` String, length=64, not null
    - `hashActual` String, length=64, not null, unique=true
    - `fechaRegistro` LocalDateTime, not null

## Paso 3 — Repositorio

### `repository/VotoRepository.java`
- `@ApplicationScoped`, `PanacheRepository<VotoEntity>`
- `Optional<VotoEntity> findUltimoVoto(Long idEleccion)`:
    - `find("idEleccion = ?1 order by fechaRegistro desc", idEleccion).firstResultOptional()`
- `List<VotoEntity> findByEleccionOrdenado(Long idEleccion)`:
    - `list("idEleccion = ?1 order by fechaRegistro asc", idEleccion)`
- REGLA: NO actualizar ni eliminar votos

## Paso 4 — DTOs

### `dto/VotoRequest.java`
```java
public record VotoRequest(
    @NotNull Long idEleccion,
    @NotNull Long idCandidato,
    @NotNull Long idVotante
) {}
```

### `dto/VotoResponse.java`
```java
public record VotoResponse(
    String mensaje,
    String hashActual,
    LocalDateTime fechaRegistro
) {}
```

## Paso 5 — Mapper

### `mapper/VotoMapper.java`
- `@ApplicationScoped`
- `VotoEntity toEntity(Long idEleccion, String votoCifrado, String hashAnterior, String hashActual)`:
    - Crea entidad con `fechaRegistro = LocalDateTime.now()`
- `VotoResponse toResponse(VotoEntity entity)`:
    - `new VotoResponse("Voto registrado exitosamente", entity.hashActual, entity.fechaRegistro)`

## Paso 6 — Utilitarios criptográficos

### `crypto/AesEncryptionUtil.java`
- `@ApplicationScoped`
- `@ConfigProperty(name = "vote.aes.secret.key") String aesKeyBase64`
- Al inicializar: `SecretKey key = new SecretKeySpec(Base64.decode(aesKeyBase64), "AES")`
- `String cifrar(String texto)`:
    - Genera IV aleatorio de 16 bytes
    - `Cipher.getInstance("AES/CBC/PKCS5Padding")`
    - Retorna `Base64(IV + cifrado)`
- `String descifrar(String textoCifrado)`:
    - Extrae IV (primeros 16 bytes del Base64 decodificado)
    - Descifra y retorna texto plano

### `crypto/Sha256ChainUtil.java`
- `@ApplicationScoped`
- Constante: `HASH_SEMILLA = "0000000000000000000000000000000000000000000000000000000000000000"` (64 ceros)
- `String calcularHash(String votoCifrado, String hashAnterior)`:
    - Input concatenado: `votoCifrado + hashAnterior`
    - `MessageDigest.getInstance("SHA-256")`
    - Retorna hex string de 64 caracteres
- `boolean verificarCadena(List<VotoEntity> votos)`:
    - Recorre votos en orden cronológico
    - Primer voto: hashAnterior debe ser HASH_SEMILLA
    - Cada voto: verifica `voto.hashActual == calcularHash(voto.votoCifrado, voto.hashAnterior)`
    - Si alguno falla retorna false

## Paso 7 — Clientes REST

### `client/AuthClient.java`
- `@RegisterRestClient(configKey = "auth-service")`
- `@Path("/auth")`
- `@GET @Path("/validate") Map<String,Boolean> validarToken(@HeaderParam("Authorization") String authHeader)`
- `@POST @Path("/logout") void logout(@HeaderParam("Authorization") String authHeader)`

### `client/ElectionClient.java`
- `@GET @Path("/{idEleccion}") EleccionResponse getEleccion(@PathParam Long idEleccion)`
- `EleccionResponse` es un record con: `Long idEleccion`, `String nombre`,
  `LocalDateTime fechaInicio`, `LocalDateTime fechaFin`, `String estado`
- Este método es necesario para la validación del período electoral en el Paso 0
- `@RegisterRestClient(configKey = "election-service")`
- `@Path("/elecciones")`
- `@GET @Path("/{idEleccion}/habilitado/{idVotante}") Map<String,Boolean> verificarHabilitado(@PathParam Long idEleccion, @PathParam Long idVotante)`
- `@PUT @Path("/{idEleccion}/marcar-votado/{idVotante}") void marcarVotado(@PathParam Long idEleccion, @PathParam Long idVotante)`

### `client/AuditClient.java`
- `@RegisterRestClient(configKey = "audit-service")`
- `@Path("/auditoria/eventos")`
- `@POST void registrarEvento(EventoAuditoriaDTO evento)`

## Paso 8 — Service

### `service/VoteService.java`
- `@ApplicationScoped`
- Inyecta: `VotoRepository`, `VotoMapper`, `AuthClient`, `ElectionClient`, `AuditClient`, `AesEncryptionUtil`, `Sha256ChainUtil`, `MeterRegistry`

#### `VotoResponse emitirVoto(VotoRequest request, String authHeader)` — `@Transactional`
**Paso 0 — Validar período electoral (agregar antes del paso 1):**

Llamar a `ElectionClient` para obtener los datos de la elección:
- `EleccionResponse eleccion = electionClient.getEleccion(request.idEleccion())`
- Si `eleccion.estado()` no es `"activo"` → lanzar `WebApplicationException(403)`
  con mensaje `"La elección no se encuentra activa"`
- Si `LocalDateTime.now().isBefore(eleccion.fechaInicio())` → lanzar 403
  con mensaje `"El período de votación aún no ha comenzado"`
- Si `LocalDateTime.now().isAfter(eleccion.fechaFin())` → lanzar 403
  con mensaje `"El período de votación ha finalizado"`

Esto garantiza que aunque el endpoint esté disponible técnicamente,
no se puede registrar ningún voto fuera del período electoral definido.
1. `Map<String,Boolean> validacion = authClient.validarToken(authHeader)`
   → si `valido == false` lanza `WebApplicationException(401)`
2. `Map<String,Boolean> habilitacion = electionClient.verificarHabilitado(request.idEleccion(), request.idVotante())`
   → si `habilitado == false` lanza `WebApplicationException(403)`
3. `String votoCifrado = aesUtil.cifrar(String.valueOf(request.idCandidato()))`
4. `Optional<VotoEntity> ultimo = votoRepository.findUltimoVoto(request.idEleccion())`
   → `hashAnterior = ultimo.map(v -> v.hashActual).orElse(sha256Util.HASH_SEMILLA)`
5. `String hashActual = sha256Util.calcularHash(votoCifrado, hashAnterior)`
6. `VotoEntity voto = mapper.toEntity(request.idEleccion(), votoCifrado, hashAnterior, hashActual)`
7. `votoRepository.persist(voto)`
8. `electionClient.marcarVotado(request.idEleccion(), request.idVotante())`
9. `authClient.logout(authHeader)`
10. `auditClient.registrarEvento("VOTO_EMITIDO", "Eleccion " + request.idEleccion(), "vote-service")`
11. `meterRegistry.counter("vote.votos.emitidos", "eleccion", request.idEleccion().toString()).increment()`
12. Retorna `mapper.toResponse(voto)`

Nota operativa:
- La auditoría y las métricas se manejan de forma segura para no romper el voto si fallan.
- El token se invalida al final del flujo, por lo que el frontend debe tratar el voto como cierre de sesión del votante.

#### `boolean verificarIntegridad(Long idEleccion)`
1. `List<VotoEntity> votos = votoRepository.findByEleccionOrdenado(idEleccion)`
2. `boolean resultado = sha256Util.verificarCadena(votos)`
3. `auditClient.registrarEvento("INTEGRIDAD_VERIFICADA", "Resultado: " + resultado, "vote-service")`
4. Retorna `resultado`

## Paso 9 — REST 

### `rest/VoteRest.java`
- `@Path("/votos")`, `@Produces(APPLICATION_JSON)`, `@Consumes(APPLICATION_JSON)`

#### `POST /votos`
- Recibe `@Valid VotoRequest` body y `@HeaderParam("Authorization") String authHeader`
- Retorna `Response.ok(voteService.emitirVoto(request, authHeader)).build()`

#### `GET /votos/eleccion/{idEleccion}/verificar-integridad`
- Retorna `Response.ok(Map.of("integridadValida", voteService.verificarIntegridad(idEleccion))).build()`

## Paso 10 — Health Checks

### `health/VoteLivenessCheck.java`
- `@Liveness` → retorna `up("vote-service-live")`

### `health/VoteReadinessCheck.java`
- `@Readiness`
- Inyecta `VotoRepository`
- Intenta `votoRepository.count()` → up si funciona
- También verifica que AES key esté configurada: si `aesKeyBase64` está vacía → down con mensaje

## Paso 11 — Lifecycle y Consul

### `lifecycle/VoteStartup.java`
- `onStart`: loguea inicio, verifica que AES key esté cargada, loguea confirmación
- `onStop`: loguea detención limpia

### `lifecycle/ConsulRegistration.java`
- Registra en Consul: Name=`vote-service`, Port=8083
- Health check: `http://localhost:8083/health/ready`

## Garantías de seguridad electoral

### Secreto del voto — verificación técnica
En ningún log, evento de auditoría ni mensaje de error
debe aparecer `idCandidato` junto a `idVotante` en el mismo registro.
La auditoría solo registra que el votante participó, nunca la opción elegida.
Verificar en `AuditClient` que los mensajes enviados NO incluyan idCandidato.

### Verificación de integridad por terceros
El endpoint `GET /votos/eleccion/{idEleccion}/verificar-integridad`
permite a cualquier auditor externo, con acceso de solo lectura,
verificar que ningún voto fue alterado después de su registro.
El resultado `true` o `false` es determinístico y reproducible.

### Qué pasa si el servicio cae entre el paso 7 y el paso 8
Si el voto se registró (paso 7) pero `marcarVotado` falló (paso 8):
- El voto quedó registrado en vote_schema.
- El votante aparece como no habilitado aún.
- En el siguiente intento, el voto se rechazará en el paso 2
  porque Election Service devolverá `habilitado = false`.
- PERO el voto ya existe en la BD, lo que genera un voto huérfano.

Para mitigar esto, agregar en `VotoRepository`:
Método `boolean existeVotoParaEleccion(Long idEleccion, String hashAnterior)`
que verifique si ya hay un voto con ese hash encadenado, evitando duplicados
aunque la transacción se reintente.

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
implementation("io.quarkus:quarkus-smallrye-jwt")
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