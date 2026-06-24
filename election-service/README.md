# Election Service

Microservicio de gestión electoral. Carga del padrón, configuración de elecciones, cargos y candidatos.

**Esquema:** `election_schema` | **Puerto:** `8082`

## Entidades a crear

### Eleccion
| Campo | Tipo | Restricción |
|---|---|---|
| idEleccion | Long | PK, autogenerado |
| nombre | String | obligatorio |
| descripcion | String | opcional |
| fechaInicio | LocalDateTime | obligatorio |
| fechaFin | LocalDateTime | obligatorio |
| estado | String | obligatorio, default "planificado" |

**Operaciones:** crear, listarActivas, actualizarEstado.

### Cargo
| Campo | Tipo | Restricción |
|---|---|---|
| idCargo | Long | PK, autogenerado |
| idEleccion | Long | obligatorio |
| nombre | String | obligatorio |

**Operaciones:** crear, listarPorEleccion.

### Candidato
| Campo | Tipo | Restricción |
|---|---|---|
| idCandidato | Long | PK, autogenerado |
| idCargo | Long | obligatorio |
| nombres | String | obligatorio |
| apellidos | String | obligatorio |
| lista | String | opcional |
| estado | Boolean | obligatorio, default true |

**Operaciones:** crear, listarPorCargo, deshabilitar.

### VotanteEleccion
| Campo | Tipo | Restricción |
|---|---|---|
| idVotante | Long | PK compuesta |
| idEleccion | Long | PK compuesta |
| haVotado | Boolean | obligatorio, default false |
| fechaParticipacion | LocalDateTime | opcional |

**Operaciones:** registrarHabilitacion, marcarVotado, verificarHabilitado.

## Pasos de implementación
1. Script Flyway `V1__create_election_schema.sql`
2. Entidades Panache (4)
3. Repositorios
4. DTOs: EleccionRequest, CandidatoRequest, PadronCargaRequest
5. ElectionService
6. EleccionResource, CandidatoResource

## Endpoints
| Método | Ruta | Descripción |
|---|---|---|
| POST | /elecciones | Crea una nueva elección |
| POST | /elecciones/{id}/cargos | Registra un cargo |
| POST | /cargos/{id}/candidatos | Registra un candidato |
| GET | /elecciones/activas | Lista elecciones activas |
| GET | /elecciones/{id}/habilitado/{idVotante} | Verifica habilitación |
| PUT | /elecciones/{id}/marcar-votado/{idVotante} | Marca participación |