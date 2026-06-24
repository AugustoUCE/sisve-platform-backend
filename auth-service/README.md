# Auth Service

Microservicio de autenticación. Responsable de validar la identidad del votante, generar tokens JWT y administrar sesiones.

**Esquema:** `auth_schema` | **Puerto:** `8081`

## Entidades a crear

### Votante
| Campo | Tipo | Restricción |
|---|---|---|
| idVotante | Long | PK, autogenerado |
| cedula | String | único, obligatorio |
| correoInstitucional | String | único, obligatorio |
| nombres | String | obligatorio |
| apellidos | String | obligatorio |
| estado | Boolean | obligatorio, default true |

**Operaciones:** buscarPorCedulaYCorreo, actualizarEstado.
*(No requiere crear individual ni eliminar — carga masiva desde padrón)*

### Sesion
| Campo | Tipo | Restricción |
|---|---|---|
| idSesion | Long | PK, autogenerado |
| idVotante | Long | obligatorio |
| tokenHash | String | obligatorio |
| fechaCreacion | LocalDateTime | obligatorio |
| fechaExpiracion | LocalDateTime | obligatorio |
| estado | Boolean | obligatorio, default true |

**Operaciones:** crear, invalidar.
*(No requiere eliminar ni listar todas)*

## Pasos de implementación
1. Script Flyway `V1__create_auth_schema.sql`
2. Entidades Panache (Votante, Sesion)
3. Repositorios
4. DTOs: LoginRequest, LoginResponse
5. JwtTokenGenerator (firma de tokens)
6. AuthService (lógica de negocio)
7. AuthResource (endpoints REST)

## Endpoints
| Método | Ruta | Descripción |
|---|---|---|
| POST | /auth/login | Autentica votante y devuelve token JWT |
| POST | /auth/logout | Invalida la sesión activa |
| GET | /auth/validate | Valida si un token sigue activo |