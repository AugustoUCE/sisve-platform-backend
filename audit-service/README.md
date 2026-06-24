# Audit Service

Microservicio de auditoría. Registro inmutable de eventos relevantes del sistema.

**Esquema:** `audit_schema` | **Puerto:** `8084`

## Entidad a crear

### Auditoria
| Campo | Tipo | Restricción |
|---|---|---|
| idAuditoria | Long | PK, autogenerado |
| tipoEvento | String | obligatorio |
| descripcion | String (TEXT) | opcional |
| fechaEvento | LocalDateTime | obligatorio |
| ipOrigen | String | opcional |
| servicioOrigen | String | obligatorio |

**Operaciones:** registrarEvento, listarPorServicio.
*(Nunca actualizar ni eliminar — inmutabilidad del log)*

## Pasos de implementación
1. Script Flyway `V1__create_audit_schema.sql`
2. Entidad Panache (Auditoria)
3. Repositorio
4. DTO: EventoAuditoriaRequest
5. AuditService (solo inserta)
6. AuditResource (endpoint interno consumido por los otros 3)

## Endpoints
| Método | Ruta | Descripción |
|---|---|---|
| POST | /auditoria/eventos | Registra un nuevo evento |
| GET | /auditoria/eventos?servicio=X&desde=Y | Consulta de eventos |