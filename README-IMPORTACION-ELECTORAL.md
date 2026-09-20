# SISVE: Integracion de Importacion Electoral XLSX

Este documento define la siguiente etapa funcional de SISVE: parametrizacion e importacion de la informacion electoral mediante un archivo XLSX.

La importacion debe alimentar los cinco microservicios sin acceder directamente a la base de datos de otro servicio.

> Este documento es una propuesta de integracion. No reemplaza ni reescribe los README historicos del proyecto.

---

## 1. Estado actual del sistema

SISVE actualmente esta compuesto por:

| Servicio | Puerto | Responsabilidad actual |
|---|---:|---|
| `auth-service` | `8081` | Votantes, miembros de mesa, sesiones y JWT |
| `election-service` | `8082` | Elecciones, cargos, candidatos y participacion historica |
| `vote-service` | `8083` | Almacenamiento anonimo de la papeleta |
| `audit-service` | `8084` | Auditoria operativa |
| `polling-station-service` | `8085` | Mesas, miembros, padron y estados electorales |

La prioridad para cualquier nueva funcionalidad es:

1. Codigo fuente actual.
2. Migraciones Flyway actuales.
3. Estructura real de las bases.
4. Contratos REST actuales.
5. README de la etapa correspondiente.
6. README antiguos como referencia historica.

---

## 2. Evolucion de la documentacion

### Etapas anteriores

Los README iniciales documentan partes del sistema cuando todavia existian cuatro servicios o cuando el contrato de voto era diferente.

Algunos documentos historicos describen:

- `POST /votos` con `idVotante` dentro del body.
- Control de participacion principalmente en `election-service`.
- Ausencia de `polling-station-service`.
- Ausencia de autenticacion separada para miembros de mesa.

Esos documentos siguen siendo utiles para entender la evolucion, pero no deben usarse como contrato actual.

### Etapa actual

El codigo vigente incorpora:

- cinco microservicios;
- autenticacion separada para votantes y miembros;
- claims JWT con roles;
- mesas y padrón electoral;
- estados `PENDING`, `ENABLED`, `VOTED` y `BLOCKED`;
- endpoints `/polling-stations/me/*`;
- voto valido, blanco y nulo;
- reserva `vote_attempt`;
- voto sin identidad persistida;
- control presencial de elegibilidad.

---

## 3. Estado actual por servicio

### 3.1 auth-service

Tablas principales:

```text
auth_schema.votante
auth_schema.sesion
auth_schema.miembro_mesa
auth_schema.sesion_miembro_mesa
```

Login de votante:

```http
POST /auth/login
```

Login de miembro:

```http
POST /auth/member-login
```

El JWT del votante incluye conceptualmente:

```json
{
  "role": "VOTER",
  "idVotante": 1,
  "sub": "cedula"
}
```

El JWT del miembro incluye:

```json
{
  "role": "POLLING_STATION_MEMBER",
  "memberId": 1,
  "pollingStationId": 1,
  "sub": "user-identifier"
}
```

Las contrasenas de miembros se guardan mediante hash PBKDF2 y salt.

### 3.2 election-service

Tablas actuales:

```text
election_schema.eleccion
election_schema.cargo
election_schema.candidato
election_schema.votante_eleccion
```

Endpoints reutilizables:

```http
POST /elecciones
GET  /elecciones/activas
GET  /elecciones/{id}
POST /elecciones/{id}/cargos
GET  /elecciones/{id}/cargos
POST /cargos/{id}/candidatos
GET  /cargos/{id}/candidatos
POST /elecciones/{id}/padron
```

El endpoint actual de padrón recibe IDs reales:

```json
{
  "idsVotantes": [1, 2, 3]
}
```

No recibe referencias `_ref` del XLSX.

### 3.3 polling-station-service

Tablas actuales:

```text
polling_station_schema.polling_station
polling_station_schema.polling_station_member
polling_station_schema.electoral_roll
polling_station_schema.vote_attempt
```

Estados del padrón:

```text
PENDING
ENABLED
VOTED
BLOCKED
```

Endpoints actuales reutilizables:

```http
GET  /polling-stations/me/electoral-roll
POST /polling-stations/me/voters/{idVoter}/enable
GET  /polling-stations/me/eligibility
POST /polling-stations/me/election/{id}/reserve-vote
POST /polling-stations/me/election/{id}/complete-vote
POST /polling-stations/me/election/{id}/release-vote
```

### 3.4 vote-service

El contrato actual de `POST /votos` no contiene `idVotante`:

```json
{
  "idEleccion": 1,
  "idCargo": 1,
  "idCandidato": 5,
  "tipoVoto": "VALIDO"
}
```

La identidad se obtiene del JWT y se utiliza solo para validar elegibilidad y marcar participacion.

La tabla `vote_schema.voto` no contiene identidad del elector.

### 3.5 audit-service

Endpoint actual:

```http
POST /auditoria/eventos
```

DTO actual:

```json
{
  "tipoEvento": "IMPORTACION_COMPLETADA",
  "descripcion": "Importacion IMP-001 completada",
  "ipOrigen": null,
  "servicioOrigen": "electoral-import-service"
}
```

---

## 4. Nueva etapa: parametrizacion electoral

El archivo XLSX tendra estas hojas obligatorias:

```text
ELECCION
VOTANTES
MESAS
MIEMBROS_MESA
CARGOS
CANDIDATOS
```

Los campos terminados en `_ref` son referencias internas del archivo. No deben insertarse directamente como IDs PostgreSQL.

Ejemplo:

```text
id_votante_ref = VOT-001
id_votante real = 27
```

El importador debe mantener el mapa:

```text
VOT-001 -> 27
```

---

## 5. Estructura propuesta del XLSX

### Hoja `ELECCION`

Columnas:

```text
id_eleccion_ref
nombre
descripcion
fecha_inicio
fecha_fin
estado
```

### Hoja `VOTANTES`

Columnas:

```text
id_votante_ref
cedula
correo_institucional
nombres
apellidos
estado
```

### Hoja `MESAS`

Columnas:

```text
id_mesa_ref
id_eleccion_ref
codigo
nombre
ubicacion
estado
```

Estados permitidos:

```text
OPEN
CLOSED
SUSPENDED
```

Para una importacion inicial se recomienda crear las mesas en `OPEN` solo si el proceso electoral esta listo para comenzar. En otro caso, usar `CLOSED` o un estado operativo definido por la administracion.

### Hoja `MIEMBROS_MESA`

Columnas:

```text
id_miembro_ref
id_mesa_ref
user_identifier
nombre_completo
correo_institucional
rol
password_inicial
es_votante
id_votante_ref
```

Roles permitidos:

```text
POLLING_STATION_PRESIDENT
POLLING_STATION_MEMBER
```

### Hoja `CARGOS`

Columnas:

```text
id_cargo_ref
id_eleccion_ref
nombre
```

### Hoja `CANDIDATOS`

Columnas:

```text
id_candidato_ref
id_cargo_ref
nombres
apellidos
lista
estado
```

---

## 6. Caso especial: miembro que tambien es votante

Un miembro de mesa puede aparecer tambien en `VOTANTES`.

La asociacion propuesta es:

```text
MIEMBROS_MESA.es_votante = true
MIEMBROS_MESA.id_votante_ref = VOT-001
```

Actualmente `polling_station_member` no contiene `id_voter`. Para soportar este caso se necesitara una migracion nueva en `polling-station-service`:

```text
V5__link_polling_member_to_voter.sql
```

Cambio propuesto:

```sql
ALTER TABLE polling_station_schema.polling_station_member
ADD COLUMN id_voter BIGINT NULL;
```

No se debe crear una FK fisica hacia `auth-service`, porque auth y polling tienen bases distintas.

La consistencia se valida mediante servicios:

```text
polling_station_member.id_voter
-> auth-service.votante.id_votante
```

Regla obligatoria:

```text
member.id_voter == targetVoterId
=> 403 Forbidden
```

Un miembro de mesa no puede habilitarse a si mismo.

---

## 7. Fase 1: VALIDATE

La fase `VALIDATE` recibe el XLSX y no crea datos de negocio.

Debe comprobar:

### Estructura

- hojas obligatorias;
- columnas obligatorias;
- columnas desconocidas;
- filas vacias;
- tipos de datos;
- fechas invalidas;
- errores de lectura del XLSX.

### Duplicados

- `id_eleccion_ref`;
- `id_votante_ref`;
- `id_mesa_ref`;
- `id_miembro_ref`;
- `id_cargo_ref`;
- `id_candidato_ref`;
- cedula;
- correo institucional;
- codigo de mesa;
- `user_identifier`.

### Referencias

- cada cargo debe referir a una eleccion existente;
- cada candidato debe referir a un cargo existente;
- cada mesa debe referir a una eleccion existente;
- cada miembro debe referir a una mesa existente;
- `id_votante_ref` de un miembro debe existir;
- un miembro marcado como votante debe tener `id_votante_ref`;
- un elector no debe estar repetido en la carga.

### Estados

Para una carga inicial:

```text
votantes.voto = false
votante_eleccion.ha_votado = false
electoral_roll.participation_status = PENDING
enabled_at = null
voted_at = null
blocked_at = null
```

Si el XLSX contiene electores ya votados, la carga debe rechazarse como carga inicial o declararse como migracion historica independiente.

### Fechas

```text
fecha_inicio < fecha_fin
```

Tambien se debe decidir si una eleccion importada puede quedar directamente en `activo`.

### Reglas de dominio

- mesa inexistente;
- cargo inexistente;
- candidato sin cargo;
- miembro sin mesa;
- votante inexistente referenciado por miembro;
- candidato duplicado dentro del mismo cargo;
- cargos de otra eleccion;
- mesas de otra eleccion;
- miembro inactivo;
- estado de mesa invalido;
- rol de miembro invalido.

### Resultado de VALIDATE

Ejemplo:

```json
{
  "valid": false,
  "importId": "IMP-2026-0001",
  "summary": {
    "elections": 1,
    "voters": 150,
    "stations": 5,
    "members": 10,
    "positions": 3,
    "candidates": 12
  },
  "errors": [
    {
      "sheet": "CANDIDATOS",
      "row": 8,
      "column": "id_cargo_ref",
      "code": "UNKNOWN_POSITION_REF",
      "message": "El cargo POS-99 no existe"
    }
  ],
  "warnings": []
}
```

`VALIDATE` no debe llamar a endpoints de creacion de usuarios, mesas o elecciones.

---

## 8. Fase 2: IMPORT

La fase `IMPORT` solo puede iniciarse si `VALIDATE` no tiene errores.

Orden conceptual:

1. Crear eleccion.
2. Guardar `id_eleccion_ref -> id_eleccion_real`.
3. Crear cargos.
4. Guardar `id_cargo_ref -> id_cargo_real`.
5. Crear candidatos.
6. Crear mesas.
7. Guardar `id_mesa_ref -> id_polling_station_real`.
8. Crear votantes.
9. Guardar `id_votante_ref -> id_votante_real`.
10. Crear `votante_eleccion`.
11. Crear `electoral_roll` en `PENDING`.
12. Crear miembros de mesa.
13. Crear credenciales de miembros.
14. Registrar auditoria.

Los `_ref` se usan unicamente para resolver dependencias dentro del archivo.

---

## 9. Orquestador recomendado

La importacion debe vivir en un nuevo microservicio:

```text
electoral-import-service
```

No debe vivir en el frontend ni acceder directamente a PostgreSQL de otros servicios.

Responsabilidades:

- recibir el XLSX;
- leer hojas con Apache POI o una libreria equivalente;
- validar estructura y reglas de dominio;
- generar reportes de errores por hoja y fila;
- mantener mapas de referencias;
- llamar los endpoints administrativos de los servicios;
- controlar reintentos;
- registrar estado de importacion;
- ejecutar compensaciones;
- auditar el resultado.

Endpoints propuestos:

```http
POST /electoral-imports/validate
POST /electoral-imports/import
GET  /electoral-imports/{importId}
GET  /electoral-imports/{importId}/errors
```

El archivo se recibira como `multipart/form-data`.

---

## 10. Estado de una importacion

El orquestador debe manejar estados:

```text
RECEIVED
VALIDATING
VALIDATED
IMPORTING
COMPLETED
FAILED
COMPENSATING
COMPENSATED
```

Datos minimos de una importacion:

```text
importId
archivo
usuario administrador
estado
fecha de inicio
fecha de fin
paso actual
errores
mapas de referencias
```

---

## 11. Importacion parcial e idempotencia

La importacion cruza cinco bases, por lo que no existe una transaccion ACID unica entre todos los microservicios.

No se debe resolver esto accediendo directamente a las bases de otros servicios.

Cada request administrativo debe incluir:

```text
importId
externalRef
idempotencyKey
```

Ejemplo:

```text
IMP-2026-0001 + VOT-001
```

Si el orquestador reintenta una operacion, el servicio debe devolver el recurso ya creado o rechazar el duplicado sin crear otro registro.

Si falla un paso:

1. detener los pasos siguientes;
2. registrar el error;
3. intentar compensacion en orden inverso;
4. registrar `IMPORTACION_FALLIDA`;
5. dejar la importacion como `FAILED` o `COMPENSATED`.

No se recomienda borrar automaticamente informacion preexistente. La compensacion debe afectar unicamente recursos creados por el `importId` actual.

---

## 12. Endpoints administrativos faltantes

Los endpoints actuales de votacion no deben reutilizarse directamente para importar grandes volumenes. Se necesitan contratos administrativos separados.

### auth-service

```http
POST /admin/voters
POST /admin/members
```

Crear votante:

```json
{
  "externalRef": "VOT-001",
  "cedula": "1712345678",
  "correoInstitucional": "juan.perez@uce.edu.ec",
  "nombres": "Juan",
  "apellidos": "Perez",
  "estado": true
}
```

Crear miembro:

```json
{
  "externalRef": "MEM-001",
  "userIdentifier": "mesa-001-presidente",
  "fullName": "Presidente Mesa 001",
  "institutionalEmail": "mesa001@uce.edu.ec",
  "pollingStationId": 1,
  "idVoter": null,
  "password": "generada-segura"
}
```

La respuesta debe devolver el ID real y aceptar idempotencia.

### election-service

```http
POST /admin/elecciones
POST /admin/elecciones/{id}/cargos
POST /admin/cargos/{id}/candidatos
POST /admin/elecciones/{id}/votantes
```

El endpoint existente `/elecciones/{id}/padron` puede reutilizarse despues de resolver los IDs reales, pero no debe recibir `_ref` del XLSX.

### polling-station-service

```http
POST /admin/polling-stations
POST /admin/polling-stations/{id}/members
POST /admin/polling-stations/{id}/electoral-roll
```

Para una carga inicial el backend debe forzar:

```text
participationStatus = PENDING
```

aunque el cliente intente enviar otro estado.

### audit-service

El endpoint actual es reutilizable:

```http
POST /auditoria/eventos
```

Se recomienda agregar `importId` como campo estructurado en una version posterior del DTO.

---

## 13. Migraciones necesarias

### `polling-station-service`

Para soportar miembro que tambien es votante:

```text
V5__link_polling_member_to_voter.sql
```

Cambio propuesto:

```sql
ALTER TABLE polling_station_schema.polling_station_member
ADD COLUMN id_voter BIGINT NULL;
```

No debe existir una FK fisica hacia `auth-service`.

### `electoral-import-service`

Migracion inicial:

```text
V1__create_import_schema.sql
```

Tablas propuestas:

```text
electoral_import
electoral_import_error
electoral_import_reference
```

### Idempotencia futura

Si se decide conservar referencias externas en cada dominio, seran necesarias migraciones nuevas para:

- `auth-service`: `external_ref` de votantes y miembros;
- `election-service`: `external_ref` de elecciones, cargos y candidatos;
- `polling-station-service`: `external_ref` de mesas, miembros y registros del padrón;
- `audit-service`: `import_id` estructurado.

---

## 14. Auditoria de importacion

Eventos recomendados:

```text
IMPORTACION_RECIBIDA
IMPORTACION_VALIDADA
IMPORTACION_RECHAZADA
IMPORTACION_INICIADA
IMPORTACION_COMPLETADA
IMPORTACION_FALLIDA
IMPORTACION_COMPENSADA
```

La auditoria debe incluir:

- `importId`;
- usuario administrador;
- cantidad de filas;
- etapa fallida;
- fecha y hora;
- resultado.

No debe incluir:

- candidatos asociados a un elector;
- contenido de votos;
- relaciones elector-candidato.

---

## 15. Respuestas de la API de importacion

### VALIDATE

```json
{
  "valid": true,
  "importId": "IMP-2026-0001",
  "summary": {
    "elections": 1,
    "voters": 150,
    "stations": 5,
    "members": 10,
    "positions": 3,
    "candidates": 12
  },
  "errors": [],
  "warnings": []
}
```

### IMPORT iniciada

```json
{
  "importId": "IMP-2026-0001",
  "status": "IMPORTING"
}
```

### IMPORT completada

```json
{
  "importId": "IMP-2026-0001",
  "status": "COMPLETED",
  "created": {
    "elections": 1,
    "voters": 150,
    "stations": 5,
    "members": 10,
    "positions": 3,
    "candidates": 12,
    "electoralRollEntries": 150
  },
  "errors": []
}
```

### IMPORT compensada

```json
{
  "importId": "IMP-2026-0001",
  "status": "COMPENSATED",
  "failedStep": "CREATE_ELECTORAL_ROLL",
  "errors": [
    {
      "code": "POLLING_STATION_UNAVAILABLE",
      "message": "polling-station-service no respondio"
    }
  ]
}
```

---

## 16. Decisiones pendientes antes de implementar

Antes de escribir codigo para XLSX se deben fijar estas reglas:

1. Si una eleccion importada puede iniciar directamente en `activo`.
2. Si las credenciales de miembros vienen en el archivo o se generan.
3. Si se permite importar datos existentes.
4. Como funcionara la idempotencia.
5. Si los `_ref` se conservaran para futuras cargas.
6. Si se permiten actualizaciones o solo carga inicial.
7. Que usuario puede ejecutar la importacion.
8. Si `VALIDATE` conserva el archivo temporalmente.
9. Cuanto dura una reserva `RESERVED` abandonada.
10. Si `votante_eleccion` seguira siendo un espejo historico.
11. Si la importacion sera sincrona o asincrona.
12. Que politica de compensacion se aplicara.

---

## 17. Checklist de aceptacion

### VALIDATE

- [ ] Detecta hojas faltantes.
- [ ] Detecta columnas faltantes.
- [ ] Detecta duplicados.
- [ ] Detecta referencias invalidas.
- [ ] Valida fechas.
- [ ] Valida roles.
- [ ] Valida estados.
- [ ] Valida mesas, cargos y candidatos.
- [ ] Valida miembros que tambien son votantes.
- [ ] Rechaza estados distintos de `PENDING` en carga inicial.
- [ ] No modifica bases de datos de negocio.

### IMPORT

- [ ] Solo inicia despues de `VALIDATE` exitoso.
- [ ] Crea recursos en el orden de dependencias.
- [ ] Traduce todos los `_ref` a IDs reales.
- [ ] Usa APIs, nunca bases cruzadas.
- [ ] Es idempotente.
- [ ] Registra `importId`.
- [ ] Puede reintentar pasos seguros.
- [ ] Compensa recursos creados por la importacion.
- [ ] Crea el padron en `PENDING`.
- [ ] Impide que un miembro-votante se habilite a si mismo.
- [ ] Audita el resultado.

---

## 18. Conclusion

La integracion XLSX debe implementarse como una etapa administrativa separada del flujo de votacion.

La recomendacion es crear `electoral-import-service` como orquestador. Este servicio debe validar el archivo, resolver referencias internas, coordinar APIs de los cinco microservicios, controlar idempotencia, manejar errores parciales y registrar auditoria.

No se debe modificar el contrato anonimo de `vote-service` ni introducir datos del candidato en `electoral_roll`.

Tampoco se deben editar migraciones ya aplicadas. Las modificaciones de base requeridas deben usar nuevas versiones Flyway, especialmente:

```text
polling-station-service: V5__link_polling_member_to_voter.sql
electoral-import-service: V1__create_import_schema.sql
```

Esta etapa queda documentada como diagnostico y propuesta. La implementacion del XLSX debe comenzar solo despues de aprobar los contratos administrativos, reglas de idempotencia y politica de compensacion.
