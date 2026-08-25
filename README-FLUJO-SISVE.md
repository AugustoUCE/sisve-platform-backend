# Flujo de prueba end-to-end SISVE

Este documento explica cómo comprobar el flujo completo del backend SISVE, incluyendo el nuevo `polling-station-service`.

## Servicios

| Servicio | Puerto | Responsabilidad |
|---|---:|---|
| auth-service | 8081 | Login, validate y logout |
| election-service | 8082 | Elecciones, cargos, candidatos y padrón electoral |
| vote-service | 8083 | Emisión de votos, cifrado e integridad |
| audit-service | 8084 | Auditoría |
| polling-station-service | 8085 | Mesas, padrón de mesa y elegibilidad |

## Requisitos previos

- Java 21
- Docker Desktop
- PowerShell
- PostgreSQL levantado por Docker
- Los cinco microservicios compilados y en ejecución

## Levantar entorno

### 1. Iniciar infraestructura

```powershell
cd C:\Users\dell\Documents\sisve-tesis\sisve-platform-backend\Infraestructure\deployment
docker network create sisve-network
docker compose --env-file .\env\credentialDB.env -p sisve-databases -f .\compose_databases_deploy.yaml up -d
docker compose --env-file .\env\credentialOps.env -p sisve-stackops -f .\compose_stackops_deploy.yaml up -d
```

### 2. Compilar servicios

```powershell
cd C:\Users\dell\Documents\sisve-tesis\sisve-platform-backend
.\gradlew.bat :auth-service:compileJava :election-service:compileJava :vote-service:compileJava :audit-service:compileJava :polling-station-service:compileJava
```

### 3. Ejecutar servicios

En cinco terminales distintas:

```powershell
.\gradlew.bat :auth-service:quarkusDev "-Ddebug=false"
.\gradlew.bat :election-service:quarkusDev "-Ddebug=false"
.\gradlew.bat :vote-service:quarkusDev "-Ddebug=false"
.\gradlew.bat :audit-service:quarkusDev "-Ddebug=false"
.\gradlew.bat :polling-station-service:quarkusDev "-Ddebug=false"
```

## Verificaciones iniciales

### Health

```powershell
Invoke-RestMethod http://localhost:8081/health/ready
Invoke-RestMethod http://localhost:8082/health/ready
Invoke-RestMethod http://localhost:8083/health/ready
Invoke-RestMethod http://localhost:8084/health/ready
Invoke-RestMethod http://localhost:8085/health/ready
```

### Swagger

- Auth: http://localhost:8081/q/dev-ui/quarkus-smallrye-openapi/swagger-ui
- Election: http://localhost:8082/q/dev-ui/quarkus-smallrye-openapi/swagger-ui
- Vote: http://localhost:8083/q/dev-ui/quarkus-smallrye-openapi/swagger-ui
- Audit: http://localhost:8084/q/dev-ui/quarkus-smallrye-openapi/swagger-ui
- Polling station: http://localhost:8085/q/dev-ui/quarkus-smallrye-openapi/swagger-ui

### Consul

Abrir http://localhost:8500 y confirmar que aparezcan:

- auth-service
- election-service
- vote-service
- audit-service
- polling-station-service

## Preparar datos de prueba

> Solo para desarrollo local.

### 1. Activa la elección

```powershell
docker exec -it sisve-election-db psql -U election_app_user -d sisve_election -c "UPDATE election_schema.eleccion SET estado = 'activo', fecha_inicio = '2026-01-01 00:00:00', fecha_fin = '2026-12-31 23:59:59' WHERE id_eleccion = 1;"
```

### 2. Resetea el padrón electoral en election-service

```powershell
docker exec -it sisve-election-db psql -U election_app_user -d sisve_election -c "ALTER TABLE election_schema.votante_eleccion DISABLE TRIGGER USER;"
docker exec -it sisve-election-db psql -U election_app_user -d sisve_election -c "UPDATE election_schema.votante_eleccion SET ha_votado = false, fecha_participacion = NULL WHERE id_eleccion = 1 AND id_votante IN (1,3);"
docker exec -it sisve-election-db psql -U election_app_user -d sisve_election -c "ALTER TABLE election_schema.votante_eleccion ENABLE TRIGGER USER;"
```

### 3. Resetea votos previos

```powershell
docker exec -it sisve-vote-db psql -U vote_app_user -d sisve_vote -c "TRUNCATE TABLE vote_schema.voto RESTART IDENTITY CASCADE;"
```

### 4. Resetea el padrón de mesa en polling-station-service

```powershell
docker exec -it sisve-polling-station-db psql -U polling_station_app_user -d sisve_polling_station -c "UPDATE polling_station_schema.polling_station SET status = 'OPEN', opened_at = NOW(), closed_at = NULL WHERE id_polling_station = 1;"
docker exec -it sisve-polling-station-db psql -U polling_station_app_user -d sisve_polling_station -c "UPDATE polling_station_schema.electoral_roll SET participation_status = 'PENDING', enabled_at = NULL, enabled_by = NULL, voted_at = NULL, blocked_at = NULL, blocked_by = NULL, block_reason = NULL WHERE id_election = 1 AND id_voter IN (1,3);"
```

### 5. Verifica elegibilidad de mesa

```powershell
Invoke-RestMethod http://localhost:8085/polling-stations/election/1/voters/3/eligibility
```

Debe devolver algo parecido a:

```json
{
  "idElection": 1,
  "idVoter": 3,
  "idPollingStation": 1,
  "eligible": true,
  "participationStatus": "PENDING",
  "pollingStationStatus": "OPEN"
}
```

## Flujo completo de prueba

### 1. Login

```powershell
$loginBody = @{
  cedula = "1709876543"
  correoInstitucional = "carlos.mora@uce.edu.ec"
} | ConvertTo-Json

$loginResponse = Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8081/auth/login" `
  -ContentType "application/json" `
  -Body $loginBody

$token = $loginResponse.token
$loginResponse
```

### 2. Validar token

```powershell
Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8081/auth/validate" `
  -Headers @{ Authorization = "Bearer $token" }
```

### 3. Consultar elecciones

```powershell
Invoke-RestMethod http://localhost:8082/elecciones
Invoke-RestMethod http://localhost:8082/elecciones/1
Invoke-RestMethod http://localhost:8082/elecciones/1/cargos
Invoke-RestMethod http://localhost:8082/elecciones/1/candidatos
Invoke-RestMethod http://localhost:8082/elecciones/1/habilitado/3
Invoke-RestMethod http://localhost:8082/elecciones/1/votantes/3/estado
```

### 4. Consultar mesa y padrón

```powershell
Invoke-RestMethod http://localhost:8085/polling-stations/election/1
Invoke-RestMethod http://localhost:8085/polling-stations/1
Invoke-RestMethod http://localhost:8085/polling-stations/1/electoral-roll
Invoke-RestMethod http://localhost:8085/polling-stations/1/members
Invoke-RestMethod http://localhost:8085/polling-stations/1/voters/search?cedula=1709876543
Invoke-RestMethod http://localhost:8085/polling-stations/election/1/voters/3/eligibility
```

### 5. Emitir voto

```powershell
$votoBody = @{
  idVotante = 3
  idEleccion = 1
  idCargo = 1
  idCandidato = 1
} | ConvertTo-Json

Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8083/votos" `
  -ContentType "application/json" `
  -Headers @{ Authorization = "Bearer $token" } `
  -Body $votoBody
```

### 6. Verificar que el voto quedó guardado

```powershell
docker exec -it sisve-vote-db psql -U vote_app_user -d sisve_vote -c "SELECT id_voto, id_eleccion, voto_cifrado, hash_anterior, hash_actual, fecha_registro FROM vote_schema.voto ORDER BY id_voto DESC;"
```

### 7. Verificar que la participación cambió

```powershell
docker exec -it sisve-election-db psql -U election_app_user -d sisve_election -c "SELECT id_votante, id_eleccion, ha_votado, fecha_participacion FROM election_schema.votante_eleccion WHERE id_eleccion = 1 AND id_votante = 3;"
docker exec -it sisve-polling-station-db psql -U polling_station_app_user -d sisve_polling_station -c "SELECT id_voter, id_election, participation_status, voted_at FROM polling_station_schema.electoral_roll WHERE id_election = 1 AND id_voter = 3;"
```

### 8. Verificar integridad

```powershell
Invoke-RestMethod http://localhost:8083/votos/eleccion/1/verificar-integridad
```

## Prueba de doble voto

1. Hacer login nuevamente con el mismo votante.
2. Intentar enviar el mismo voto otra vez.
3. Esperar `409 Conflict`.

```powershell
$loginResponse2 = Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8081/auth/login" `
  -ContentType "application/json" `
  -Body $loginBody

$token2 = $loginResponse2.token

Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8083/votos" `
  -ContentType "application/json" `
  -Headers @{ Authorization = "Bearer $token2" } `
  -Body $votoBody
```

## Verificaciones adicionales

### Auth logout

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8081/auth/logout" `
  -ContentType "application/json" `
  -Headers @{ Authorization = "Bearer $token" } `
  -Body "{}"
```

### Polling station mark-voted

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8085/polling-stations/election/1/voters/3/mark-voted"
```

### Polling station summary

```powershell
Invoke-RestMethod http://localhost:8085/polling-stations/1/summary
```

## Qué debe quedar confirmado

- Auth autentica y valida token.
- Election expone elecciones, cargos, candidatos y estado de participación.
- Polling station valida elegibilidad y marca participación por mesa.
- Vote registra el voto, invalida el token y bloquea doble voto.
- Audit recibe eventos.
- Health, Swagger y Consul responden para los cinco servicios.

## Limpieza

Para apagar todo:

```powershell
cd C:\Users\dell\Documents\sisve-tesis\sisve-platform-backend\Infraestructure\deployment
docker compose --env-file .\env\credentialDB.env -p sisve-databases -f .\compose_databases_deploy.yaml down
docker compose --env-file .\env\credentialOps.env -p sisve-stackops -f .\compose_stackops_deploy.yaml down
```
