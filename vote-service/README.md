# Vote Service

Microservicio de votación. Emisión, cifrado e integridad del voto.

**Esquema:** `vote_schema` | **Puerto:** `8083`

## Entidad a crear

### Voto
| Campo | Tipo | Restricción |
|---|---|---|
| idVoto | Long | PK, autogenerado |
| idEleccion | Long | obligatorio |
| votoCifrado | String (TEXT) | obligatorio |
| hashAnterior | String | obligatorio |
| hashActual | String | obligatorio, único |
| fechaRegistro | LocalDateTime | obligatorio |

**Operaciones:** crear, obtenerUltimoHash, verificarCadenaIntegridad.
*(Nunca actualizar ni eliminar — inmutabilidad del voto)*

## Pasos de implementación
1. Script Flyway `V1__create_vote_schema.sql`
2. Entidad Panache (Voto)
3. Repositorio
4. AesEncryptionUtil (cifrado/descifrado AES-256)
5. Sha256ChainUtil (cálculo hash encadenado)
6. VoteService (valida token con Auth, valida habilitación con Election, cifra, encadena, guarda, marca participación)
7. VoteResource

## Lógica del hash encadenado

Primer voto de cada elección usa hash semilla fijo como hash_anterior.

## Endpoints
| Método | Ruta | Descripción |
|---|---|---|
| POST | /votos | Registra un voto cifrado (requiere JWT válido) |
| GET | /votos/eleccion/{idEleccion}/verificar-integridad | Verifica la cadena de hashes |