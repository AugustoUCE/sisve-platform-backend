# Integracion End-to-End SISVE: autenticacion, mesa y voto

Este documento explica todo el flujo electoral de SISVE, desde el arranque de la infraestructura y el login hasta la habilitacion presencial, la seleccion de candidatos, la emision anonima del voto, el marcado de participacion, la auditoria y el certificado final del frontend.

El flujo implementado separa claramente:

- La identidad y autenticacion del usuario.
- La habilitacion presencial del elector.
- El estado de participacion electoral.
- El almacenamiento anonimo de la papeleta.

La regla principal es:

> `polling-station-service` conoce que un elector fue habilitado o que ya voto, pero `vote-service` nunca persiste una relacion entre elector y candidato.

---

## 1. Responsabilidad de cada servicio

| Servicio | Puerto | Responsabilidad |
|---|---:|---|
| `auth-service` | `8081` | Autenticar votantes y miembros de mesa; emitir JWT |
| `election-service` | `8082` | Elecciones, cargos, candidatos y participacion historica |
| `vote-service` | `8083` | Validar y guardar la papeleta cifrada |
| `audit-service` | `8084` | Registrar eventos operativos |
| `polling-station-service` | `8085` | Mesa, miembros, padron y estado de participacion |

---

## 2. Flujo completo

### 2.1 Miembro de mesa

1. El miembro abre `/member-login` en el frontend.
2. Envia usuario y contrasena a `POST /auth/member-login`.
3. `auth-service` verifica la credencial usando PBKDF2 con salt.
4. Se emite un JWT con rol y mesa asignada.
5. El frontend permite entrar a `/polling-station/roll`.
6. La vista consulta exclusivamente `/polling-stations/me/electoral-roll`.
7. El miembro busca al elector por cedula, nombre o apellido.
8. Si el estado es `PENDING`, confirma la verificacion fisica.
9. Se llama a `POST /polling-stations/me/voters/{idVoter}/enable`.
10. El backend cambia atomicamente `PENDING -> ENABLED`.

### 2.2 Votante

1. El votante inicia sesion con `POST /auth/login`.
2. `auth-service` emite un JWT con rol `VOTER`.
3. `ballotView.vue` consulta `GET /polling-stations/me/eligibility?electionId=...`.
4. La identidad se obtiene del JWT, no del frontend.
5. Solo un elector `ENABLED` puede continuar.
6. `election-service` entrega cargos y candidatos.
7. El votante selecciona un voto:
   - `VALIDO`: incluye candidato.
   - `BLANCO`: candidato `null`.
   - `NULO`: candidato `null`.
8. `vote-service` verifica que el JWT tenga rol `VOTER`.
9. `vote-service` consulta la elegibilidad en `polling-station-service`.
10. Se crea una reserva unica de voto.
11. Se valida eleccion, cargo y candidato cuando corresponde.
12. Se cifra y persiste la papeleta.
13. Se marca al elector como `VOTED` en `polling-station-service`.
14. Se completa la reserva.
15. Se cierra la sesion y el frontend navega al certificado.

---

## 3. Claims JWT

### JWT de votante

```json
{
  "iss": "sisve-auth",
  "sub": "1709876543",
  "role": "VOTER",
  "idVotante": 1,
  "upn": "carlos.mora@uce.edu.ec"
}
```

### JWT de miembro de mesa

```json
{
  "iss": "sisve-auth",
  "sub": "mesa-001-presidente",
  "role": "POLLING_STATION_MEMBER",
  "memberId": 1,
  "pollingStationId": 1,
  "upn": "mesa001@uce.edu.ec"
}
```

El JWT del miembro nunca se genera a partir de `auth_schema.votante`.

---

## 4. Autenticacion de miembros

Se agrego una identidad separada para miembros de mesa:

```text
auth_schema.miembro_mesa
```

Campos principales:

- `id_miembro_mesa`
- `user_identifier`
- `password_hash`
- `password_salt`
- `full_name`
- `institutional_email`
- `polling_station_id`
- `status`

Las contrasenas no se guardan en texto plano. Se usa `PBKDF2WithHmacSHA256` con:

- 120000 iteraciones.
- Salt aleatorio.
- Hash de 256 bits.

### Login

```http
POST http://localhost:8081/auth/member-login
Content-Type: application/json
```

```json
{
  "userIdentifier": "mesa-001-presidente",
  "password": "Mesa001!2026"
}
```

La cuenta anterior es solo para desarrollo local.

---

## 5. Endpoints protegidos de polling station

### Padron de la mesa autenticada

```http
GET /polling-stations/me/electoral-roll?search=juan
Authorization: Bearer <JWT_MEMBER>
```

El backend obtiene `pollingStationId` del JWT. El frontend no puede seleccionar libremente otra mesa.

La busqueda revisa:

- `cedula`
- `full_name`
- `institutional_email`

### Habilitar elector

```http
POST /polling-stations/me/voters/{idVoter}/enable
Authorization: Bearer <JWT_MEMBER>
```

No requiere enviar `enabledBy` desde Vue. El backend usa `memberId` del JWT.

La operacion solo permite:

```text
PENDING -> ENABLED
```

Si el registro ya esta `ENABLED`, `VOTED` o `BLOCKED`, responde `409 Conflict`.

### Elegibilidad del votante

```http
GET /polling-stations/me/eligibility?electionId=1
Authorization: Bearer <JWT_VOTER>
```

El backend obtiene `idVotante` desde el claim `idVotante`.

Respuesta conceptual:

```json
{
  "idElection": 1,
  "idVoter": 1,
  "idPollingStation": 1,
  "eligible": true,
  "participationStatus": "ENABLED",
  "pollingStationStatus": "OPEN"
}
```

### Marcar participacion

```http
POST /polling-stations/me/election/{idElection}/mark-voted
Authorization: Bearer <JWT_VOTER>
```

El elector tambien se obtiene desde el JWT.

---

## 6. Reserva contra doble voto

Se agrego la tabla:

```text
polling_station_schema.vote_attempt
```

Esta tabla no almacena candidato ni contenido de voto. Solo controla una reserva tecnica:

- `id_election`
- `id_voter`
- `status`: `RESERVED` o `COMPLETED`
- `created_at`

Existe una restriccion unica:

```text
UNIQUE (id_election, id_voter)
```

Esto impide dos reservas simultaneas aunque se repita el POST desde:

- doble clic;
- Postman;
- otro navegador;
- una peticion manipulada;
- una recarga de pagina.

Endpoints internos usados por `vote-service`:

```http
POST /polling-stations/me/election/{idElection}/reserve-vote
POST /polling-stations/me/election/{idElection}/complete-vote
POST /polling-stations/me/election/{idElection}/release-vote
```

---

## 7. Anonimato del voto

La entidad persistente de `vote-service` contiene solamente datos electorales y de integridad:

- `id_eleccion`
- `tipo_voto`
- `voto_cifrado`
- `hash_anterior`
- `hash_actual`
- `fecha_registro`

No contiene:

- `idVotante`;
- cedula;
- correo;
- nombre;
- `idVoter`;
- relacion persistente con el candidato.

Para un voto valido se cifra un contenido equivalente a:

```text
VALIDO:<idCandidato>
```

Para votos especiales:

```text
BLANCO
NULO
```

La identidad del elector solo se utiliza temporalmente para autenticar, verificar elegibilidad y actualizar el estado de participacion.

La auditoria puede registrar que un elector fue habilitado o que completo participacion, pero no registra el candidato elegido por ese elector.

---

## 8. Migraciones nuevas

No se modifican `V1`, `V2` ni `V3`.

### `auth-service`

```text
V4__add_polling_station_members.sql
```

Crea:

- `auth_schema.miembro_mesa`
- `auth_schema.sesion_miembro_mesa`

### `polling-station-service`

```text
V4__add_vote_attempt_reservation.sql
```

Crea:

- `polling_station_schema.vote_attempt`

Estas migraciones se aplican automaticamente al iniciar cada servicio con Flyway.

No se debe editar una migracion ya aplicada. Los cambios posteriores deben usar una nueva version `V5`, `V6`, etc.

---

## 9. Frontend

### Rutas

```text
/member-login
/polling-station/roll
/login
/ballot
/certificate
```

### Proteccion de rutas

- `/polling-station/roll` requiere `POLLING_STATION_MEMBER`.
- `/ballot` requiere `VOTER`.
- Un miembro no puede entrar al flujo de votacion.
- Un votante no puede entrar al padron.

La proteccion del router mejora la experiencia, pero la seguridad real se aplica en backend mediante la firma JWT, el rol y la mesa asignada.

### Vista del padron

Archivo:

```text
sisve-platform-frontend/src/views/polling-station/PollingStationRollView.vue
```

Incluye:

- carga del padron;
- busqueda sin recargar la pagina;
- estados `PENDING`, `ENABLED`, `VOTED` y `BLOCKED`;
- motivo de bloqueo;
- modal de confirmacion;
- bloqueo contra doble clic;
- actualizacion local de la fila habilitada.

La vista no recibe ni guarda informacion de candidatos o contenido de votos.

---

## 10. Datos de desarrollo

### Miembro de mesa

```text
Usuario: mesa-001-presidente
Contrasena: Mesa001!2026
Mesa: 1
```

### Votante Carlos

```text
Correo: carlos.mora@uce.edu.ec
Cedula: 1709876543
Id: 1
```

### Votante Juan

```text
Correo: juan.perez@uce.edu.ec
Cedula: 1712345678
Id: 2
Estado inicial: PENDING
```

La cuenta de miembro se crea mediante `V4` de `auth-service`. Juan se creo mediante las migraciones `V3` existentes de desarrollo.

---

## 11. Ejecucion local

### Infraestructura

```powershell
cd C:\Users\dell\Documents\sisve-tesis\sisve-platform-backend\Infraestructure\deployment
docker compose --env-file .\env\credentialDB.env -p sisve-databases -f .\compose_databases_deploy.yaml up -d
```

### Compilacion

```powershell
cd C:\Users\dell\Documents\sisve-tesis\sisve-platform-backend
.\gradlew.bat :auth-service:compileJava :election-service:compileJava :vote-service:compileJava :audit-service:compileJava :polling-station-service:compileJava
```

### Servicios

Ejecutar en terminales separadas:

```powershell
.\gradlew.bat :auth-service:quarkusDev "-Ddebug=false"
.\gradlew.bat :election-service:quarkusDev "-Ddebug=false"
.\gradlew.bat :vote-service:quarkusDev "-Ddebug=false"
.\gradlew.bat :audit-service:quarkusDev "-Ddebug=false"
.\gradlew.bat :polling-station-service:quarkusDev "-Ddebug=false"
```

### Frontend

```powershell
cd C:\Users\dell\Documents\sisve-tesis\sisve-platform-frontend
npm install
npm run dev
```

Abrir:

```text
http://localhost:5173
```

---

## 12. Prueba manual del flujo

### Paso 1: miembro inicia sesion

Abrir:

```text
http://localhost:5173/member-login
```

Usar:

```text
mesa-001-presidente
Mesa001!2026
```

### Paso 2: buscar a Juan

Abrir:

```text
http://localhost:5173/polling-station/roll
```

Buscar:

```text
1712345678
```

Debe aparecer:

```text
Juan Perez
Estado: Pendiente de verificacion
```

### Paso 3: habilitar

Confirmar el modal de identidad fisica.

Resultado esperado:

```text
Estado: Habilitado para votar
```

La fila se actualiza sin recargar la aplicacion.

### Paso 4: Juan inicia sesion

```text
Correo: juan.perez@uce.edu.ec
Cedula: 1712345678
```

Juan no debe entrar al ballot antes de ser habilitado.

### Paso 5: emitir voto

Cuando polling responda `ENABLED`, Juan puede emitir:

- voto valido;
- voto blanco;
- voto nulo.

### Paso 6: verificar estado

El estado debe quedar:

```text
polling_station_schema.electoral_roll.participation_status = VOTED
electoral_roll.voted_at = NOW()
```

La papeleta queda en `vote_schema.voto` sin identidad del elector.

---

## 13. Casos de seguridad cubiertos

| Caso | Resultado esperado |
|---|---|
| Miembro inicia sesion | JWT con rol y mesa |
| Miembro consulta padron | Solo ve su mesa |
| Busca elector `PENDING` | Puede solicitar habilitacion |
| Habilita elector | `PENDING -> ENABLED` |
| Otro miembro intenta modificar mesa ajena | `403 Forbidden` |
| Votante `PENDING` intenta votar | `403 Forbidden` |
| Votante `ENABLED` vota | Operacion permitida |
| Voto valido | Candidato cifrado, sin identidad persistida |
| Voto blanco | `tipo_voto=BLANCO`, candidato nulo |
| Voto nulo | `tipo_voto=NULO`, candidato nulo |
| Doble POST | `409 Conflict` |
| Elector `VOTED` regresa | No puede votar otra vez |
| Manipula `idPollingStation` | `403 Forbidden` |
| Manipula `idVoter` | `403 Forbidden` |
| VOTER entra al padron | `403 Forbidden` |
| MEMBER entra al ballot | Redireccion frontend y rechazo conceptual por rol |

---

## 14. Validaciones realizadas

Backend:

```text
:auth-service:compileJava
:polling-station-service:compileJava
:vote-service:compileJava
```

Resultado:

```text
BUILD SUCCESSFUL
```

Frontend:

```text
npm run type-check
npm run build-only
```

Resultado:

```text
Sin errores de TypeScript
Build de Vite generado correctamente
```

---

## 15. Nota importante sobre Flyway

Flyway calcula un checksum por migracion. Por eso nunca se debe editar una migracion que ya fue aplicada.

Si una migracion nueva falla durante su primera ejecucion, se puede corregir mientras no haya quedado aplicada.

Si una migracion ya aplicada fue modificada accidentalmente, se debe:

1. detener el servicio;
2. revisar el checksum aplicado y el local;
3. restaurar el archivo original o ejecutar `repair` de forma controlada;
4. crear una nueva migracion para cambios posteriores.

Las migraciones nuevas de este flujo son `V4` y no requieren modificar los scripts anteriores.

---

## 16. Flujo global completo

La siguiente secuencia resume la operacion completa del sistema:

```text
1. Infraestructura Docker
   |
   +--> PostgreSQL por microservicio
   +--> Consul, Prometheus, Grafana y Traefik
   |
2. auth-service
   |
   +--> Login de votante o miembro de mesa
   +--> Emision del JWT con rol
   |
3. Miembro de mesa
   |
   +--> Consulta su mesa mediante claims JWT
   +--> Busca al elector
   +--> Verifica identidad fisica
   +--> PENDING -> ENABLED
   |
4. Votante
   |
   +--> Consulta elegibilidad
   +--> Consulta eleccion activa
   +--> Consulta cargos y candidatos
   +--> Selecciona voto valido, blanco o nulo
   |
5. vote-service
   |
   +--> Valida JWT con rol VOTER
   +--> Valida mesa y estado ENABLED
   +--> Reserva el voto
   +--> Valida eleccion, cargo y candidato
   +--> Cifra y guarda la papeleta
   +--> Marca participacion como VOTED
   +--> Completa la reserva
   |
6. Frontend
   |
   +--> Navega al certificado
   +--> Limpia la sesion al finalizar el proceso
```

La identidad del elector participa en las validaciones, pero no se copia a la papeleta persistida.

---

## 17. Inicio de la infraestructura

El entorno local se compone de una base PostgreSQL por servicio. Los puertos de base usados habitualmente son:

| Base | Puerto local |
|---|---:|
| `sisve_auth` | `54321` |
| `sisve_election` | `54323` |
| `sisve_vote` | `54324` |
| `sisve_polling_station` | `54325` |

Los comandos de infraestructura son:

```powershell
cd C:\Users\dell\Documents\sisve-tesis\sisve-platform-backend\Infraestructure\deployment
docker network create sisve-network
docker compose --env-file .\env\credentialDB.env -p sisve-databases -f .\compose_databases_deploy.yaml up -d
docker compose --env-file .\env\credentialOps.env -p sisve-stackops -f .\compose_stackops_deploy.yaml up -d
```

Si la red ya existe, Docker puede informar que no es necesario crearla. Ese mensaje no impide continuar.

---

## 18. Arranque de los microservicios

Cada servicio aplica sus migraciones Flyway al iniciar. La compilacion completa es:

```powershell
cd C:\Users\dell\Documents\sisve-tesis\sisve-platform-backend
.\gradlew.bat :auth-service:compileJava :election-service:compileJava :vote-service:compileJava :audit-service:compileJava :polling-station-service:compileJava
```

Arranque local, una terminal por servicio:

```powershell
.\gradlew.bat :auth-service:quarkusDev "-Ddebug=false"
.\gradlew.bat :election-service:quarkusDev "-Ddebug=false"
.\gradlew.bat :vote-service:quarkusDev "-Ddebug=false"
.\gradlew.bat :audit-service:quarkusDev "-Ddebug=false"
.\gradlew.bat :polling-station-service:quarkusDev "-Ddebug=false"
```

Puertos HTTP:

```text
auth-service              http://localhost:8081
election-service          http://localhost:8082
vote-service              http://localhost:8083
audit-service             http://localhost:8084
polling-station-service  http://localhost:8085
```

Health checks:

```powershell
Invoke-RestMethod http://localhost:8081/health/ready
Invoke-RestMethod http://localhost:8082/health/ready
Invoke-RestMethod http://localhost:8083/health/ready
Invoke-RestMethod http://localhost:8084/health/ready
Invoke-RestMethod http://localhost:8085/health/ready
```

El hecho de que un puerto este escuchando no garantiza que Quarkus haya iniciado correctamente. Un servicio en recuperacion de Dev Mode puede responder `500`; siempre se debe revisar `/health/ready` y el log de Flyway.

---

## 19. Frontend: estructura y flujo de vistas

El frontend Vue se encuentra en `sisve-platform-frontend`.

Instalacion y ejecucion:

```powershell
cd C:\Users\dell\Documents\sisve-tesis\sisve-platform-frontend
npm install
npm run dev
```

URL:

```text
http://localhost:5173
```

Vistas principales:

| Vista | Funcion |
|---|---|
| `LoginView.vue` | Login del votante |
| `MemberLoginView.vue` | Login del miembro de mesa |
| `PollingStationRollView.vue` | Padron y habilitacion |
| `ballotView.vue` | Seleccion y confirmacion del voto |
| `CertificateView.vue` | Certificado posterior al voto |

Servicios frontend:

| Servicio | Funcion |
|---|---|
| `auth-service.ts` | Login, logout, validacion y login de miembro |
| `election-service.ts` | Eleccion activa, cargos, candidatos y participacion |
| `polling-station-service.ts` | Padron, elegibilidad, habilitacion y mesa |
| `vote-service.ts` | Emision de voto y verificacion de integridad |
| `audit-service.ts` | Consulta y registro de auditoria |
| `api.ts` | Instancias Axios, URLs y Authorization Bearer |

El interceptor de `api.ts` agrega el JWT automaticamente. Las rutas de login no reciben un token anterior.

Variables opcionales del frontend:

```env
VITE_AUTH_API_URL=http://localhost:8081
VITE_ELECTION_API_URL=http://localhost:8082
VITE_VOTE_API_URL=http://localhost:8083
VITE_AUDIT_API_URL=http://localhost:8084
VITE_POLLING_STATION_API_URL=http://localhost:8085
```

Si no se definen, el frontend usa esos mismos valores como fallback local.

---

## 20. Flujo de autenticacion del votante

Request:

```powershell
$loginBody = @{
  cedula = "1709876543"
  correoInstitucional = "carlos.mora@uce.edu.ec"
} | ConvertTo-Json

$login = Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8081/auth/login" `
  -ContentType "application/json" `
  -Body $loginBody

$voterToken = $login.token
```

Validacion:

```powershell
Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8081/auth/validate" `
  -Headers @{ Authorization = "Bearer $voterToken" }
```

Si las credenciales no coinciden, auth responde `401`. Si el usuario esta inactivo, responde `403`.

El frontend guarda temporalmente el token para completar la sesion. La papeleta no se guarda en `localStorage`, `sessionStorage`, cookies ni analytics.

---

## 21. Flujo de autenticacion del miembro

Request:

```powershell
$memberBody = @{
  userIdentifier = "mesa-001-presidente"
  password = "Mesa001!2026"
} | ConvertTo-Json

$memberLogin = Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8081/auth/member-login" `
  -ContentType "application/json" `
  -Body $memberBody

$memberToken = $memberLogin.token
```

La cuenta de desarrollo pertenece a la mesa `1`. En produccion las credenciales deben ser provisionadas de forma segura y no deben permanecer en scripts publicos.

---

## 22. Flujo del padron y habilitacion

Consultar el padron de la mesa autenticada:

```powershell
Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8085/polling-stations/me/electoral-roll?search=1712345678" `
  -Headers @{ Authorization = "Bearer $memberToken" }
```

Habilitar a Juan:

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8085/polling-stations/me/voters/2/enable" `
  -Headers @{ Authorization = "Bearer $memberToken" }
```

El backend no acepta la mesa desde Vue para este flujo. Usa `pollingStationId` del JWT y verifica adicionalmente que el `sub` exista como miembro activo de esa mesa.

La transicion se realiza mediante una actualizacion condicional:

```text
WHERE id_polling_station = mesaDelJWT
  AND id_voter = electorSolicitado
  AND participation_status = 'PENDING'
```

Esto evita que dos peticiones habiliten simultaneamente al mismo elector.

---

## 23. Flujo de eleccion, cargos y candidatos

La eleccion activa se consulta en:

```http
GET http://localhost:8082/elecciones/activas
```

Para una eleccion concreta:

```http
GET http://localhost:8082/elecciones/1
GET http://localhost:8082/elecciones/1/cargos
GET http://localhost:8082/cargos/1/candidatos
```

El frontend ejecuta esta secuencia desde `ballotView.vue`:

1. Obtiene la eleccion activa.
2. Consulta los cargos.
3. Selecciona el cargo mostrado.
4. Consulta candidatos del cargo.
5. Filtra candidatos inactivos.
6. Permite voto valido, blanco o nulo.

Un voto blanco o nulo no utiliza candidatos falsos.

---

## 24. Elegibilidad del votante

Antes de mostrar o confirmar la papeleta, el frontend consulta:

```powershell
Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8085/polling-stations/me/eligibility?electionId=1" `
  -Headers @{ Authorization = "Bearer $voterToken" }
```

Reglas:

| Estado | Resultado |
|---|---|
| `PENDING` | No puede votar |
| `ENABLED` | Puede continuar |
| `VOTED` | Debe ser rechazado como voto duplicado |
| `BLOCKED` | No puede votar |

Ademas, la mesa debe estar `OPEN`.

La identidad se obtiene del claim `idVotante`; el frontend no puede consultar elegibilidad de otra persona cambiando una URL.

---

## 25. Emision de voto

El endpoint es:

```http
POST http://localhost:8083/votos
Authorization: Bearer <JWT_VOTER>
Content-Type: application/json
```

### Voto valido

```json
{
  "idEleccion": 1,
  "idCargo": 1,
  "idCandidato": 1,
  "tipoVoto": "VALIDO"
}
```

### Voto blanco

```json
{
  "idEleccion": 1,
  "idCargo": 1,
  "idCandidato": null,
  "tipoVoto": "BLANCO"
}
```

### Voto nulo

```json
{
  "idEleccion": 1,
  "idCargo": 1,
  "idCandidato": null,
  "tipoVoto": "NULO"
}
```

El request no incluye `idVotante`. `vote-service` obtiene la identidad al validar el JWT.

Orden de validacion en `vote-service`:

1. JWT valido y firmado.
2. Claim `role=VOTER`.
3. Eleccion activa y dentro del periodo.
4. Mesa abierta y elector `ENABLED`.
5. Reserva unica por eleccion y elector.
6. Cargo perteneciente a la eleccion.
7. Candidato perteneciente al cargo si el voto es `VALIDO`.
8. Persistencia cifrada.
9. Marcado de participacion `VOTED`.
10. Completar reserva.

Si falla un paso posterior a la reserva, `vote-service` intenta liberar la reserva. Un segundo POST recibe `409 Conflict` si la reserva ya existe o el elector ya completo su participacion.

---

## 26. Persistencia y anonimato

### Estado de participacion

La fuente principal del estado presencial es:

```text
polling_station_schema.electoral_roll.participation_status
```

Los valores son:

```text
PENDING
ENABLED
VOTED
BLOCKED
```

`election-service` conserva su participacion historica para sus reglas internas, pero la habilitacion presencial y el paso efectivo por mesa se controlan en `polling-station-service`.

### Papeleta

`vote_schema.voto` no tiene columnas de elector. La papeleta guarda:

```text
id_eleccion
tipo_voto
voto_cifrado
hash_anterior
hash_actual
fecha_registro
```

El hash encadena los votos de una eleccion y permite verificar integridad:

```powershell
Invoke-RestMethod http://localhost:8083/votos/eleccion/1/verificar-integridad
```

El resultado no debe utilizarse para reconstruir la identidad del elector.

---

## 27. Auditoria

`audit-service` recibe eventos operativos como:

- login exitoso o fallido;
- miembro que habilita un elector;
- elector marcado como `VOTED`;
- intento de voto no habilitado;
- intento de voto duplicado;
- verificacion de integridad;
- apertura o cierre de mesa.

Endpoints:

```http
POST /auditoria/eventos
GET  /auditoria/eventos?servicio=vote-service
GET  /auditoria/eventos/tipo/VOTO_EMITIDO
```

No se debe registrar en auditoria:

- el candidato elegido por un elector especifico;
- una combinacion `idVotante + idCandidato`;
- el contenido de la papeleta asociado a una persona.

---

## 28. Certificado y finalizacion en frontend

Despues de recibir respuesta exitosa de `vote-service`, `ballotView.vue` navega a:

```text
/certificate
```

`CertificateView.vue` valida la sesion y muestra la constancia del proceso. El certificado puede mostrar que el usuario participo, pero no debe mostrar el candidato asociado al elector.

El flujo de frontend controla la experiencia con `emitiendo`, pero la seguridad contra doble voto no depende de ese estado visual. La proteccion real esta en la reserva unica y en el estado de polling.

---

## 29. Pruebas end-to-end recomendadas

### Caso A: PENDING no puede votar

1. Crear o resetear a Juan con estado `PENDING`.
2. Iniciar login como Juan.
3. Consultar `/polling-stations/me/eligibility`.
4. Esperar `eligible=false` y `participationStatus=PENDING`.
5. Confirmar que `POST /votos` responde `403`.

### Caso B: habilitacion presencial

1. Iniciar login como `mesa-001-presidente`.
2. Consultar `/polling-stations/me/electoral-roll`.
3. Buscar a Juan.
4. Ejecutar `POST /polling-stations/me/voters/2/enable`.
5. Confirmar `ENABLED`, `enabled_at` y `enabled_by`.

### Caso C: voto valido

1. Iniciar login de Juan.
2. Confirmar elegibilidad `ENABLED`.
3. Consultar cargos y candidatos.
4. Enviar voto con `tipoVoto=VALIDO`.
5. Confirmar `VOTED` en polling.
6. Confirmar una fila en `vote_schema.voto` sin `id_voter`.

### Caso D: voto blanco y nulo

1. Resetear un elector de prueba.
2. Habilitarlo desde la mesa.
3. Enviar `BLANCO` con candidato `null`.
4. Repetir con otro elector para `NULO`.
5. Confirmar el campo `tipo_voto`.

### Caso E: doble POST

Enviar dos peticiones casi simultaneas con el mismo JWT y eleccion. Una debe completar y la otra debe responder `409 Conflict`.

### Caso F: manipulacion de identidad

- Cambiar `idVoter` en una URL: debe producir `403`.
- Cambiar el ID de mesa: debe producir `403`.
- Usar JWT de votante en `/me/electoral-roll`: debe producir `403`.
- Usar JWT de miembro en `/me/eligibility`: debe producir `403`.

---

## 30. Reinicio controlado de datos de desarrollo

Los seeds Flyway crean datos iniciales, pero no deben resetear votos ya emitidos en cada reinicio. Para una nueva prueba completa se deben restablecer explicitamente los estados de desarrollo.

Ejemplo para eleccion:

```powershell
docker exec -it sisve-election-db psql -U election_app_user -d sisve_election -c "ALTER TABLE election_schema.votante_eleccion DISABLE TRIGGER USER; UPDATE election_schema.votante_eleccion SET ha_votado = false, fecha_participacion = NULL WHERE id_eleccion = 1 AND id_votante = 2; ALTER TABLE election_schema.votante_eleccion ENABLE TRIGGER USER;"
```

Ejemplo para polling:

```powershell
docker exec -it sisve-polling-station-db psql -U polling_station_app_user -d sisve_polling_station -c "UPDATE polling_station_schema.electoral_roll SET participation_status = 'PENDING', enabled_at = NULL, enabled_by = NULL, voted_at = NULL, blocked_at = NULL, blocked_by = NULL, block_reason = NULL WHERE id_election = 1 AND id_voter = 2;"
```

Ejemplo para votos y reservas:

```powershell
docker exec -it sisve-vote-db psql -U vote_app_user -d sisve_vote -c "TRUNCATE TABLE vote_schema.voto RESTART IDENTITY CASCADE;"
docker exec -it sisve-polling-station-db psql -U polling_station_app_user -d sisve_polling_station -c "TRUNCATE TABLE polling_station_schema.vote_attempt RESTART IDENTITY CASCADE;"
```

Estos comandos son exclusivamente para desarrollo local. No deben ejecutarse en produccion.

---

## 31. Diagnostico de errores frecuentes

### `401` en login

La combinacion de credenciales no existe o el usuario esta autenticado con otro tipo de cuenta.

### `403` al consultar padrón

El JWT no tiene `role=POLLING_STATION_MEMBER`, o el miembro no esta asignado a la mesa del claim.

### `403` al votar

El elector esta `PENDING`, `BLOCKED`, `VOTED` o la mesa esta cerrada.

### `409` al habilitar

El elector ya fue habilitado, ya voto o cambio de estado entre la carga y la confirmacion.

### `409` al emitir voto

Ya existe una reserva o una participacion completada para esa eleccion y elector.

### `503` desde `vote-service`

`vote-service` no pudo consultar `polling-station-service`. Revisar:

```powershell
Invoke-RestMethod http://localhost:8085/health/ready
```

Tambien revisar el log de polling station. Un `503` puede ser consecuencia de un fallo de arranque por Flyway.

### `FlywayValidateException`

El checksum local no coincide con el aplicado. No modificar una migracion aplicada. Revisar la version, restaurar el archivo original o ejecutar `repair` de manera controlada y crear una nueva migracion para cambios posteriores.

### Candidatos no visibles

Verificar en este orden:

```powershell
Invoke-RestMethod http://localhost:8082/elecciones/activas
Invoke-RestMethod http://localhost:8082/elecciones/1/cargos
Invoke-RestMethod http://localhost:8082/cargos/1/candidatos
```

Si alguno falla, el problema esta en `election-service` o sus datos, no en la tarjeta visual del frontend.

---

## 32. Estado de validacion del proyecto

Validacion backend:

```powershell
.\gradlew.bat :auth-service:compileJava :polling-station-service:compileJava :vote-service:compileJava
```

Resultado esperado:

```text
BUILD SUCCESSFUL
```

Validacion frontend:

```powershell
npm run type-check
npm run build-only
```

Resultado esperado:

```text
Sin errores de TypeScript
Build de Vite generado correctamente
```

Este README describe tanto el flujo funcional como las medidas de autorizacion, anonimato, consistencia y operacion necesarias para ejecutarlo localmente.
