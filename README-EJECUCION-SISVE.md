# README de ejecución local - SISVE Backend

Este documento explica cómo encender la infraestructura Docker, ejecutar los microservicios del backend SISVE y realizar una prueba completa del flujo que ya fue validado en local.

El objetivo es validar:

```text
Login del votante
Validación del token
Consulta de elecciones, cargos y candidatos
Preparación de datos de prueba local
Emisión del voto
Bloqueo de doble voto
Verificación de integridad
Consulta de auditoría
```

> Estado actual validado:
>
> - `auth-service` funciona con login, validate y logout.
> - `vote-service` ya valida el token emitido por `auth-service`.
> - La elección debe tener `estado = 'activo'`.
> - `POST /votos` requiere `idVotante`, `idEleccion`, `idCargo` e `idCandidato`.
> - Después de votar, `vote-service` invalida el token con `authClient.logout(authHeader)`.
> - Para probar doble voto se debe hacer login nuevamente con el mismo votante.
> - El error de métricas Prometheus fue corregido en `VoteService`.

---

## Índice

1. [Requisitos previos](#1-requisitos-previos)
2. [Estructura general](#2-estructura-general)
3. [Encender Docker Desktop](#3-encender-docker-desktop)
4. [Ir a la carpeta de infraestructura](#4-ir-a-la-carpeta-de-infraestructura)
5. [Crear red Docker](#5-crear-red-docker)
6. [Levantar bases de datos](#6-levantar-bases-de-datos)
7. [Levantar StackOps](#7-levantar-stackops)
8. [Volver a la raíz del backend](#8-volver-a-la-raíz-del-backend)
9. [Compilar microservicios](#9-compilar-microservicios)
10. [Ejecutar microservicios](#10-ejecutar-microservicios)
11. [Verificar health de los servicios](#11-verificar-health-de-los-servicios)
12. [Verificar registro en Consul](#12-verificar-registro-en-consul)
13. [URLs de Swagger](#13-urls-de-swagger)
14. [Preparar datos locales de prueba](#14-preparar-datos-locales-de-prueba)
15. [Prueba completa del flujo SISVE por PowerShell](#15-prueba-completa-del-flujo-sisve-por-powershell)
16. [Prueba de doble voto](#16-prueba-de-doble-voto)
17. [Verificar datos en PostgreSQL](#17-verificar-datos-en-postgresql)
18. [Consultar auditoría](#18-consultar-auditoría)
19. [Apagar servicios](#19-apagar-servicios)
20. [Comandos resumidos de encendido](#20-comandos-resumidos-de-encendido)
21. [Comandos resumidos de prueba](#21-comandos-resumidos-de-prueba)
22. [Backend listo para frontend](#22-backend-listo-para-frontend)
23. [Notas importantes del flujo](#23-notas-importantes-del-flujo)
24. [Pruebas adicionales obligatorias](#24-pruebas-adicionales-obligatorias)
25. [Nota sobre llaves JWT](#25-nota-sobre-llaves-jwt)
26. [Problemas comunes](#26-problemas-comunes)

---

# 1. Requisitos previos

Antes de ejecutar el backend, verificar que estén instalados:

```text
Java 21
Docker Desktop
PowerShell
Git
Visual Studio Code o IntelliJ IDEA
```

Verificar Java:

```powershell
java -version
```

Verificar Docker:

```powershell
docker --version
docker compose version
```

---

# 2. Estructura general

El backend tiene cuatro microservicios:

| Microservicio | Puerto | Función |
|---|---:|---|
| auth-service | 8081 | Login, logout, JWT y validación de sesión |
| election-service | 8082 | Elecciones, cargos, candidatos y estado de participación |
| vote-service | 8083 | Emisión de votos, cifrado, hash e integridad |
| audit-service | 8084 | Registro y consulta de auditorías |

La infraestructura Docker se encuentra en:

```text
sisve-platform-backend/Infraestructure/deployment
```

---

# 3. Encender Docker Desktop

Primero abrir **Docker Desktop** en Windows.

Esperar hasta que Docker indique que está corriendo.

Luego abrir PowerShell y validar:

```powershell
docker ps
```

Si Docker está encendido correctamente, el comando no debe mostrar error.

---

# 4. Ir a la carpeta de infraestructura

Desde PowerShell:

```powershell
cd C:\Users\dell\Documents\sisve-tesis\sisve-platform-backend\Infraestructure\deployment
```

---

# 5. Crear red Docker

Ejecutar una sola vez:

```powershell
docker network create sisve-network
```

Si aparece un mensaje indicando que la red ya existe, no hay problema.

---

# 6. Levantar bases de datos

Desde esta carpeta:

```text
C:\Users\dell\Documents\sisve-tesis\sisve-platform-backend\Infraestructure\deployment
```

Ejecutar:

```powershell
docker compose --env-file .\env\credentialDB.env -p sisve-databases -f .\compose_databases_deploy.yaml up -d
```

Verificar:

```powershell
docker ps
```

Deben aparecer contenedores similares a:

```text
sisve-auth-db
sisve-audit-db
sisve-election-db
sisve-vote-db
```

Puertos esperados:

| Base de datos | Puerto local | Usuario | Base |
|---|---:|---|---|
| sisve-auth-db | 54321 | auth_app_user | sisve_auth |
| sisve-audit-db | 54322 | audit_app_user | sisve_audit |
| sisve-election-db | 54323 | election_app_user | sisve_election |
| sisve-vote-db | 54324 | vote_app_user | sisve_vote |

---

# 7. Levantar StackOps

StackOps incluye:

```text
Consul
Traefik
Prometheus
Grafana
```

Ejecutar:

```powershell
docker compose --env-file .\env\credentialOps.env -p sisve-stackops -f .\compose_stackops_deploy.yaml up -d
```

Verificar:

```powershell
docker ps
```

Deben aparecer servicios como:

```text
sisve-consul
sisve-traefik
sisve-prometheus
sisve-grafana
```

URLs de herramientas:

| Herramienta | URL |
|---|---|
| Consul | http://localhost:8500 |
| Traefik Dashboard | http://localhost:8080 |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 |

---

# 8. Volver a la raíz del backend

```powershell
cd C:\Users\dell\Documents\sisve-tesis\sisve-platform-backend
```

---

# 9. Compilar microservicios

Antes de ejecutar, compilar los cuatro servicios:

```powershell
.\gradlew.bat :auth-service:compileJava :election-service:compileJava :vote-service:compileJava :audit-service:compileJava
```

Resultado esperado:

```text
BUILD SUCCESSFUL
```

Si solo se modificó `vote-service` y `election-service`, se puede compilar:

```powershell
.\gradlew.bat :vote-service:compileJava :election-service:compileJava
```

---

# 10. Ejecutar microservicios

Cada microservicio debe ejecutarse en una terminal diferente.

---

## 10.1 Ejecutar Auth Service

```powershell
cd C:\Users\dell\Documents\sisve-tesis\sisve-platform-backend
.\gradlew.bat :auth-service:quarkusDev "-Ddebug=false"
```

URLs:

```text
Servicio:
http://localhost:8081

Swagger:
http://localhost:8081/q/dev-ui/quarkus-smallrye-openapi/swagger-ui

Health:
http://localhost:8081/health/ready
```

---

## 10.2 Ejecutar Election Service

```powershell
cd C:\Users\dell\Documents\sisve-tesis\sisve-platform-backend
.\gradlew.bat :election-service:quarkusDev "-Ddebug=false"
```

URLs:

```text
Servicio:
http://localhost:8082

Swagger:
http://localhost:8082/q/dev-ui/quarkus-smallrye-openapi/swagger-ui

Health:
http://localhost:8082/health/ready
```

---

## 10.3 Ejecutar Vote Service

```powershell
cd C:\Users\dell\Documents\sisve-tesis\sisve-platform-backend
.\gradlew.bat :vote-service:quarkusDev "-Ddebug=false"
```

URLs:

```text
Servicio:
http://localhost:8083

Swagger:
http://localhost:8083/q/dev-ui/quarkus-smallrye-openapi/swagger-ui

Health:
http://localhost:8083/health/ready
```

---

## 10.4 Ejecutar Audit Service

```powershell
cd C:\Users\dell\Documents\sisve-tesis\sisve-platform-backend
.\gradlew.bat :audit-service:quarkusDev "-Ddebug=false"
```

URLs:

```text
Servicio:
http://localhost:8084

Swagger:
http://localhost:8084/q/dev-ui/quarkus-smallrye-openapi/swagger-ui

Health:
http://localhost:8084/health/ready
```

---

# 11. Verificar health de los servicios

En una terminal adicional:

```powershell
Invoke-RestMethod http://localhost:8081/health/ready
Invoke-RestMethod http://localhost:8082/health/ready
Invoke-RestMethod http://localhost:8083/health/ready
Invoke-RestMethod http://localhost:8084/health/ready
```

Cada servicio debe responder con estado:

```text
UP
```

---

# 12. Verificar registro en Consul

Abrir en navegador:

```text
http://localhost:8500
```

Deben aparecer registrados:

```text
auth-service
election-service
vote-service
audit-service
```

También se puede verificar con PowerShell:

```powershell
Invoke-RestMethod http://localhost:8500/v1/agent/services
```

Los health checks deben apuntar a:

```text
http://host.docker.internal:8081/health/ready
http://host.docker.internal:8082/health/ready
http://host.docker.internal:8083/health/ready
http://host.docker.internal:8084/health/ready
```

---

# 13. URLs de Swagger

Abrir cada Swagger:

```text
Auth Service:
http://localhost:8081/q/dev-ui/quarkus-smallrye-openapi/swagger-ui

Election Service:
http://localhost:8082/q/dev-ui/quarkus-smallrye-openapi/swagger-ui

Vote Service:
http://localhost:8083/q/dev-ui/quarkus-smallrye-openapi/swagger-ui

Audit Service:
http://localhost:8084/q/dev-ui/quarkus-smallrye-openapi/swagger-ui
```

---

# 14. Preparar datos locales de prueba

Antes de probar `POST /votos`, la elección debe estar activa y el votante debe estar habilitado.

En este proyecto, `vote-service` valida el estado así:

```java
"activo".equalsIgnoreCase(eleccion.estado())
```

Por eso el estado correcto para votar es:

```text
activo
```

No usar para esta prueba:

```text
ACTIVA
activa
planificado
```

---

## 14.1 Activar elección local

```powershell
docker exec -it sisve-election-db psql -U election_app_user -d sisve_election -c "UPDATE election_schema.eleccion SET estado = 'activo', fecha_inicio = '2026-01-01 00:00:00', fecha_fin = '2026-12-31 23:59:59' WHERE id_eleccion = 1;"
```

Verificar:

```powershell
docker exec -it sisve-election-db psql -U election_app_user -d sisve_election -c "SELECT id_eleccion, nombre, fecha_inicio, fecha_fin, estado FROM election_schema.eleccion WHERE id_eleccion = 1;"
```

Resultado esperado:

```text
estado = activo
```

---

## 14.2 Resetear votantes de prueba

En producción no se debe revertir un voto emitido. La base tiene un trigger que protege esta regla.

Para pruebas locales, se puede desactivar temporalmente los triggers de usuario, resetear los votantes de prueba y volver a activarlos.

> Solo usar en desarrollo local.

```powershell
docker exec -it sisve-election-db psql -U election_app_user -d sisve_election -c "ALTER TABLE election_schema.votante_eleccion DISABLE TRIGGER USER;"
```

```powershell
docker exec -it sisve-election-db psql -U election_app_user -d sisve_election -c "UPDATE election_schema.votante_eleccion SET ha_votado = false, fecha_participacion = NULL WHERE id_eleccion = 1 AND id_votante IN (1,3);"
```

```powershell
docker exec -it sisve-election-db psql -U election_app_user -d sisve_election -c "ALTER TABLE election_schema.votante_eleccion ENABLE TRIGGER USER;"
```

Verificar:

```powershell
docker exec -it sisve-election-db psql -U election_app_user -d sisve_election -c "SELECT id_votante, id_eleccion, ha_votado, fecha_participacion FROM election_schema.votante_eleccion WHERE id_eleccion = 1 AND id_votante IN (1,3);"
```

Resultado esperado:

```text
ha_votado = f
```

---

## 14.3 Limpiar votos de prueba

> Solo usar en desarrollo local.

```powershell
docker exec -it sisve-vote-db psql -U vote_app_user -d sisve_vote -c "TRUNCATE TABLE vote_schema.voto RESTART IDENTITY CASCADE;"
```

---

# 15. Prueba completa del flujo SISVE por PowerShell

Esta prueba usa el votante `3`, porque fue el caso que funcionó correctamente en la prueba local.

Datos del votante:

```text
idVotante = 3
cedula = 1709876543
correoInstitucional = carlos.mora@uce.edu.ec
```

Datos del voto:

```text
idEleccion = 1
idCargo = 1
idCandidato = 1
```

---

## 15.1 Login del votante 3

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

Respuesta esperada:

```text
Debe devolver token, idVotante, correoInstitucional, nombres y apellidos.
```

---

## 15.2 Validar token

```powershell
Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8081/auth/validate" `
  -Headers @{
    Authorization = "Bearer $token"
  }
```

Respuesta esperada:

```text
valido = True
idVotante = 3
```

---

## 15.3 Emitir voto

Actualmente el DTO `VotoRequest` requiere `idVotante`. Por eso el body correcto es:

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
  -Headers @{
    Authorization = "Bearer $token"
  } `
  -Body $votoBody
```

Respuesta esperada:

```text
Voto registrado correctamente.
Debe devolver idEleccion, idVotante, fechaRegistro y hashActual.
```

---

## 15.4 Verificar voto guardado

```powershell
docker exec -it sisve-vote-db psql -U vote_app_user -d sisve_vote -c "SELECT id_voto, id_eleccion, voto_cifrado, hash_anterior, hash_actual, fecha_registro FROM vote_schema.voto ORDER BY id_voto DESC;"
```

Resultado esperado:

```text
Debe existir un voto registrado.
El voto debe estar cifrado.
Debe existir hash_anterior y hash_actual.
```

---

## 15.5 Verificar participación del votante

```powershell
docker exec -it sisve-election-db psql -U election_app_user -d sisve_election -c "SELECT id_votante, id_eleccion, ha_votado, fecha_participacion FROM election_schema.votante_eleccion WHERE id_eleccion = 1 AND id_votante IN (1,3);"
```

Resultado esperado:

```text
El votante 3 debe tener ha_votado = t.
```

---

## 15.6 Verificar integridad

```powershell
Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8083/votos/eleccion/1/verificar-integridad"
```

Respuesta esperada:

```json
{
  "integridadValida": true
}
```

---

# 16. Prueba de doble voto

Después de votar, `vote-service` ejecuta:

```java
authClient.logout(authHeader);
```

Por eso el token usado para votar queda invalidado automáticamente.

Para probar doble voto se debe iniciar sesión otra vez con el mismo votante.

---

## 16.1 Login nuevamente con el mismo votante

```powershell
$loginBody = @{
  cedula = "1709876543"
  correoInstitucional = "carlos.mora@uce.edu.ec"
} | ConvertTo-Json

$loginResponse2 = Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8081/auth/login" `
  -ContentType "application/json" `
  -Body $loginBody

$token2 = $loginResponse2.token
```

---

## 16.2 Intentar votar nuevamente

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8083/votos" `
  -ContentType "application/json" `
  -Headers @{
    Authorization = "Bearer $token2"
  } `
  -Body $votoBody
```

Resultado esperado:

```text
409 Conflict
```

o una respuesta JSON controlada:

```json
{
  "codigo": "VOTO_DUPLICADO",
  "mensaje": "El votante ya registró su voto en esta elección."
}
```

Este resultado es correcto, porque el sistema debe impedir que un votante vote dos veces en la misma elección.

---

# 17. Verificar datos en PostgreSQL

---

## 17.1 Ver votantes

```powershell
docker exec -it sisve-auth-db psql -U auth_app_user -d sisve_auth -c "SELECT id_votante, cedula, correo_institucional, nombres, apellidos, estado FROM auth_schema.votante;"
```

---

## 17.2 Ver elecciones

```powershell
docker exec -it sisve-election-db psql -U election_app_user -d sisve_election -c "SELECT * FROM election_schema.eleccion;"
```

---

## 17.3 Ver cargos

```powershell
docker exec -it sisve-election-db psql -U election_app_user -d sisve_election -c "SELECT * FROM election_schema.cargo;"
```

---

## 17.4 Ver candidatos

```powershell
docker exec -it sisve-election-db psql -U election_app_user -d sisve_election -c "SELECT * FROM election_schema.candidato;"
```

---

## 17.5 Ver participación de votantes

```powershell
docker exec -it sisve-election-db psql -U election_app_user -d sisve_election -c "SELECT * FROM election_schema.votante_eleccion;"
```

---

## 17.6 Ver votos

```powershell
docker exec -it sisve-vote-db psql -U vote_app_user -d sisve_vote -c "SELECT * FROM vote_schema.voto;"
```

---

## 17.7 Ver auditorías

```powershell
docker exec -it sisve-audit-db psql -U audit_app_user -d sisve_audit -c "SELECT * FROM audit_schema.auditoria ORDER BY fecha_evento DESC;"
```

---

# 18. Consultar auditoría

Abrir Swagger de Audit:

```text
http://localhost:8084/q/dev-ui/quarkus-smallrye-openapi/swagger-ui
```

Buscar endpoint relacionado con auditoría.

También se puede consultar directamente en base:

```powershell
docker exec -it sisve-audit-db psql -U audit_app_user -d sisve_audit -c "SELECT id_auditoria, tipo_evento, descripcion, servicio_origen, fecha_evento FROM audit_schema.auditoria ORDER BY fecha_evento DESC;"
```

Eventos esperados:

```text
LOGIN_EXITOSO
LOGOUT
VOTO_EMITIDO
VOTO_DUPLICADO
INTEGRIDAD_VERIFICADA
```

---

# 19. Apagar servicios

Para detener un microservicio en `quarkusDev`, presionar en su terminal:

```text
Ctrl + C
```

Para detener Docker:

```powershell
cd C:\Users\dell\Documents\sisve-tesis\sisve-platform-backend\Infraestructure\deployment

docker compose --env-file .\env\credentialDB.env -p sisve-databases -f .\compose_databases_deploy.yaml down

docker compose --env-file .\env\credentialOps.env -p sisve-stackops -f .\compose_stackops_deploy.yaml down
```

---

# 20. Comandos resumidos de encendido

```powershell
cd C:\Users\dell\Documents\sisve-tesis\sisve-platform-backend\Infraestructure\deployment

docker network create sisve-network

docker compose --env-file .\env\credentialDB.env -p sisve-databases -f .\compose_databases_deploy.yaml up -d

docker compose --env-file .\env\credentialOps.env -p sisve-stackops -f .\compose_stackops_deploy.yaml up -d
```

```powershell
cd C:\Users\dell\Documents\sisve-tesis\sisve-platform-backend

.\gradlew.bat :auth-service:compileJava :election-service:compileJava :vote-service:compileJava :audit-service:compileJava
```

Abrir cuatro terminales separadas:

```powershell
.\gradlew.bat :auth-service:quarkusDev "-Ddebug=false"
```

```powershell
.\gradlew.bat :election-service:quarkusDev "-Ddebug=false"
```

```powershell
.\gradlew.bat :vote-service:quarkusDev "-Ddebug=false"
```

```powershell
.\gradlew.bat :audit-service:quarkusDev "-Ddebug=false"
```

---

# 21. Comandos resumidos de prueba

Preparar datos:

```powershell
docker exec -it sisve-election-db psql -U election_app_user -d sisve_election -c "UPDATE election_schema.eleccion SET estado = 'activo', fecha_inicio = '2026-01-01 00:00:00', fecha_fin = '2026-12-31 23:59:59' WHERE id_eleccion = 1;"

docker exec -it sisve-election-db psql -U election_app_user -d sisve_election -c "ALTER TABLE election_schema.votante_eleccion DISABLE TRIGGER USER;"

docker exec -it sisve-election-db psql -U election_app_user -d sisve_election -c "UPDATE election_schema.votante_eleccion SET ha_votado = false, fecha_participacion = NULL WHERE id_eleccion = 1 AND id_votante IN (1,3);"

docker exec -it sisve-election-db psql -U election_app_user -d sisve_election -c "ALTER TABLE election_schema.votante_eleccion ENABLE TRIGGER USER;"

docker exec -it sisve-vote-db psql -U vote_app_user -d sisve_vote -c "TRUNCATE TABLE vote_schema.voto RESTART IDENTITY CASCADE;"
```

Login, validate y voto:

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

Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8081/auth/validate" `
  -Headers @{
    Authorization = "Bearer $token"
  }

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
  -Headers @{
    Authorization = "Bearer $token"
  } `
  -Body $votoBody

Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8083/votos/eleccion/1/verificar-integridad"
```

---

# 22. Backend listo para frontend

El backend está listo para conectar con frontend cuando:

```text
Los contenedores Docker están levantados.
Los cuatro microservicios están ejecutándose.
Los health checks responden UP.
Consul muestra los servicios saludables.
Swagger abre correctamente en los cuatro servicios.
Login devuelve token.
Validate acepta el token.
Election devuelve elecciones, cargos y candidatos.
Vote registra el voto con Authorization Bearer.
Vote bloquea doble voto.
Vote verifica integridad.
Audit registra eventos.
CORS permite llamadas desde http://localhost:5173 o http://localhost:4200.
```

Frontend Vue/Vite:

```text
http://localhost:5173
```

Frontend Angular:

```text
http://localhost:4200
```

---

# 23. Notas importantes del flujo

---

## 23.1 Estado canónico de elección activa

El valor canónico para permitir votar es:

```text
activo
```

Esto se debe a que `VoteService` valida:

```java
"activo".equalsIgnoreCase(eleccion.estado())
```

---

## 23.2 Body actual de `POST /votos`

Actualmente el body correcto es:

```json
{
  "idVotante": 3,
  "idEleccion": 1,
  "idCargo": 1,
  "idCandidato": 1
}
```

`VoteService` valida que el `idVotante` del body coincida con el votante autenticado por el token.

---

## 23.3 Logout automático después de votar

Después de registrar el voto, `vote-service` ejecuta:

```java
authClient.logout(authHeader);
```

Por eso el token queda invalidado después del voto.

Impacto en frontend:

```text
Después de votar, mostrar pantalla de confirmación.
No seguir usando el mismo token para acciones protegidas.
Redirigir al login o limpiar sesión local.
Para probar doble voto, hacer login nuevamente con el mismo votante.
```

---

## 23.4 Métricas y auditoría no deben romper el voto

El error que se corrigió en `VoteService` fue:

```text
Prometheus requires that all meters with the same name have the same set of tag keys.
```

La causa era el choque entre estas métricas:

```java
meterRegistry.counter("vote.votos.emitidos", "eleccion", request.idEleccion().toString()).increment();
meterRegistry.counter("vote.votos.emitidos.total").increment();
```

Prometheus transforma nombres y ambas podían terminar chocando como:

```text
vote_votos_emitidos_total
```

La corrección recomendada es dejar solo la métrica por elección:

```java
meterRegistry.counter("vote.votos.emitidos", "eleccion", request.idEleccion().toString()).increment();
```

o manejar cualquier métrica no crítica con un wrapper seguro para que no rompa la operación.

---

# 24. Pruebas adicionales obligatorias

---

## 24.1 Probar login correcto

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

Resultado esperado:

```text
Debe devolver token, idVotante, correoInstitucional, nombres y apellidos.
```

---

## 24.2 Probar validate correcto

```powershell
Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8081/auth/validate" `
  -Headers @{
    Authorization = "Bearer $token"
  }
```

Resultado esperado:

```text
valido = True
idVotante = 3
```

---

## 24.3 Probar logout

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8081/auth/logout" `
  -ContentType "application/json" `
  -Headers @{
    Authorization = "Bearer $token"
  } `
  -Body "{}"
```

Resultado esperado:

```text
204 No Content
```

---

## 24.4 Probar validate después de logout

Después de hacer logout, volver a ejecutar:

```powershell
Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8081/auth/validate" `
  -Headers @{
    Authorization = "Bearer $token"
  }
```

Resultado esperado:

```text
401 Unauthorized
TOKEN_INVALIDO
```

Este resultado es correcto, porque el token ya fue invalidado por logout.

---

## 24.5 Probar login con credenciales incorrectas

```powershell
$loginBody = @{
  cedula = "0000000000"
  correoInstitucional = "noexiste@uce.edu.ec"
} | ConvertTo-Json

Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8081/auth/login" `
  -ContentType "application/json" `
  -Body $loginBody
```

Resultado esperado:

```text
401 Unauthorized
```

---

## 24.6 Probar votar sin token

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
  -Body $votoBody
```

Resultado esperado:

```text
401 Unauthorized
```

Esto confirma que no se puede votar sin autenticación.

---

## 24.7 Probar idVotante diferente al token

Hacer login con votante 3, pero mandar `idVotante = 1` en el body:

```powershell
$votoBody = @{
  idVotante = 1
  idEleccion = 1
  idCargo = 1
  idCandidato = 1
} | ConvertTo-Json

Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8083/votos" `
  -ContentType "application/json" `
  -Headers @{
    Authorization = "Bearer $token"
  } `
  -Body $votoBody
```

Resultado esperado:

```text
403 Forbidden
```

o mensaje:

```text
El votante autenticado no coincide con el cuerpo de la solicitud.
```

---

## 24.8 Probar candidato inválido

Usar token nuevo, porque después de votar el token queda invalidado.

```powershell
$votoBody = @{
  idVotante = 3
  idEleccion = 1
  idCargo = 1
  idCandidato = 999
} | ConvertTo-Json

Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8083/votos" `
  -ContentType "application/json" `
  -Headers @{
    Authorization = "Bearer $token"
  } `
  -Body $votoBody
```

Resultado esperado:

```text
404 Not Found
```

o una respuesta controlada indicando que el candidato no existe.

---

## 24.9 Probar cargo inválido

Usar token nuevo, porque después de votar el token queda invalidado.

```powershell
$votoBody = @{
  idVotante = 3
  idEleccion = 1
  idCargo = 999
  idCandidato = 1
} | ConvertTo-Json

Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8083/votos" `
  -ContentType "application/json" `
  -Headers @{
    Authorization = "Bearer $token"
  } `
  -Body $votoBody
```

Resultado esperado:

```text
404 Not Found
```

o una respuesta controlada indicando que el cargo no existe o no pertenece a la elección.

---

## 24.10 Probar integridad

```powershell
Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8083/votos/eleccion/1/verificar-integridad"
```

Resultado esperado:

```json
{
  "integridadValida": true
}
```

Si devuelve `false`, revisar si existen votos insertados manualmente en base de datos. La integridad puede fallar si los votos no fueron generados por el servicio.

---

# 25. Nota sobre llaves JWT

En ambiente de desarrollo, `auth-service` genera las llaves JWT:

```text
privateKey.pem
publicKey.pem
```

`auth-service` usa:

```text
privateKey.pem para firmar tokens
publicKey.pem para validar tokens
```

`vote-service` debe tener una copia del `publicKey.pem` de `auth-service`, porque necesita validar los tokens emitidos por `auth-service`.

Cuando se regeneren las llaves de `auth-service`, copiar la llave pública nuevamente a `vote-service`:

```powershell
Copy-Item .\auth-service\src\main\resources\publicKey.pem .\vote-service\src\main\resources\publicKey.pem -Force
```

Después de copiar la llave pública, reiniciar `vote-service`.

Importante:

```text
No se debe copiar privateKey.pem a vote-service.
Solo auth-service debe tener privateKey.pem.
```

Si `auth-service` regenera las llaves en cada arranque, los tokens generados antes del reinicio dejarán de ser válidos.

Lo recomendable es que la tarea `generateDevJwtKeys` solo genere llaves si no existen, para evitar invalidar tokens en cada reinicio.

---

# 26. Problemas comunes

---

## 26.1 Error TOKEN_INVALIDO en validate

Error:

```json
{
  "codigo": "TOKEN_INVALIDO",
  "mensaje": "Token inválido",
  "status": 401
}
```

Posibles causas:

```text
El token expiró.
El token fue generado antes de reiniciar auth-service.
El logout ya invalidó la sesión.
El token no se envió con Bearer.
La sesión no existe en auth_schema.sesion.
```

Forma correcta de enviar el token:

```powershell
Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8081/auth/validate" `
  -Headers @{
    Authorization = "Bearer $token"
  }
```

---

## 26.2 Error 401 al votar

Causa probable:

```text
No se envió Authorization Bearer.
El publicKey.pem de vote-service no coincide con el publicKey.pem de auth-service.
El token fue invalidado porque ya se emitió un voto.
```

Solución:

```powershell
Copy-Item .\auth-service\src\main\resources\publicKey.pem .\vote-service\src\main\resources\publicKey.pem -Force
```

Luego reiniciar `vote-service`.

---

## 26.3 Error "La elección no se encuentra activa"

Causa probable:

```text
La elección no tiene estado = activo.
La fecha actual no está dentro de fecha_inicio y fecha_fin.
```

Solución local:

```powershell
docker exec -it sisve-election-db psql -U election_app_user -d sisve_election -c "UPDATE election_schema.eleccion SET estado = 'activo', fecha_inicio = '2026-01-01 00:00:00', fecha_fin = '2026-12-31 23:59:59' WHERE id_eleccion = 1;"
```

Verificar:

```powershell
docker exec -it sisve-election-db psql -U election_app_user -d sisve_election -c "SELECT id_eleccion, fecha_inicio, fecha_fin, estado FROM election_schema.eleccion WHERE id_eleccion = 1;"
```

---

## 26.4 Error "No se puede revertir un voto emitido"

Causa:

```text
El trigger prevent_unvote impide cambiar ha_votado de true a false.
```

Esto es correcto para producción.

Solución solo para desarrollo local:

```powershell
docker exec -it sisve-election-db psql -U election_app_user -d sisve_election -c "ALTER TABLE election_schema.votante_eleccion DISABLE TRIGGER USER;"

docker exec -it sisve-election-db psql -U election_app_user -d sisve_election -c "UPDATE election_schema.votante_eleccion SET ha_votado = false, fecha_participacion = NULL WHERE id_eleccion = 1 AND id_votante IN (1,3);"

docker exec -it sisve-election-db psql -U election_app_user -d sisve_election -c "ALTER TABLE election_schema.votante_eleccion ENABLE TRIGGER USER;"
```

---

## 26.5 Error 409 al votar

Este error es correcto si el usuario ya votó.

Significa:

```text
El sistema bloqueó el doble voto.
```

---

## 26.6 Error Prometheus de métricas duplicadas

Error:

```text
Prometheus requires that all meters with the same name have the same set of tag keys.
```

Causa:

```text
Existían dos métricas que terminaban con el mismo nombre Prometheus:
vote_votos_emitidos_total
```

Corrección aplicada:

```text
Eliminar o renombrar el counter vote.votos.emitidos.total.
Mantener la métrica por elección.
Manejar métricas no críticas de forma segura.
```

Después de cambiar `VoteService`, reiniciar `vote-service`.

---

## 26.7 Integridad devuelve false

Posibles causas:

```text
Existen votos insertados manualmente en la base.
Los hashes no fueron generados por vote-service.
Se modificó algún registro en vote_schema.voto.
```

Para revisar votos:

```powershell
docker exec -it sisve-vote-db psql -U vote_app_user -d sisve_vote -c "SELECT * FROM vote_schema.voto;"
```

Para limpiar votos solo en desarrollo:

```powershell
docker exec -it sisve-vote-db psql -U vote_app_user -d sisve_vote -c "TRUNCATE TABLE vote_schema.voto RESTART IDENTITY CASCADE;"
```

---

## 26.8 Consul muestra servicios unhealthy

Posible causa:

```text
Consul está dentro de Docker y no puede llegar a localhost del host.
```

Los health checks deben usar:

```text
host.docker.internal
```

Ejemplo:

```text
http://host.docker.internal:8081/health/ready
```

---

## 26.9 Advertencia sobre CORS

Si aparece una advertencia parecida a:

```text
Unrecognized configuration key "quarkus.http.cors" was provided; it will be ignored
```

Revisar la configuración CORS del `application.properties` y validar la propiedad correcta usada por la versión actual de Quarkus.

El frontend necesita que el backend permita solicitudes desde:

```text
http://localhost:5173
http://localhost:4200
```

También debe permitir el header:

```text
Authorization
```

---

# 27. Resultado esperado final

Al finalizar las pruebas, el sistema debe cumplir:

```text
Login correcto.
Validate correcto.
Voto registrado.
Voto cifrado en vote_schema.voto.
Hash generado correctamente.
Participación marcada con ha_votado = true.
Token invalidado después de votar.
Doble voto bloqueado.
Integridad de votos válida.
Auditoría registrada.
Servicios saludables.
Consul con servicios registrados.
```
