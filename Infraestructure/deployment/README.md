# SISVE Platform Backend — Despliegue con Docker Compose

Este directorio contiene la configuración necesaria para desplegar la plataforma **SISVE Backend** mediante Docker Compose.

La infraestructura se divide en tres grupos:

1. Bases de datos y exporters.
2. Servicios de plataforma, monitoreo y observabilidad.
3. Microservicios de aplicación.

Esta separación permite iniciar, detener, actualizar y supervisar cada grupo de servicios de forma independiente.

---

## Estructura del directorio

```text
sisve-platform-backend/
└── Infraestructure/
    └── deployment/
        ├── env/
        │   ├── apps.env
        │   ├── databases.env
        │   └── stackops.env
        ├── compose_apps_deploy.yaml
        ├── compose_databases_deploy.yaml
        ├── compose_stackops_deploy.yaml
        └── README.md
```

> La carpeta actualmente se llama `Infraestructure`. Para mantener la nomenclatura en inglés podría renombrarse posteriormente como `Infrastructure`, pero los comandos de este documento utilizan la ruta actual.

---

## Distribución de archivos

| Archivo                         | Responsabilidad                                       |
| ------------------------------- | ----------------------------------------------------- |
| `compose_databases_deploy.yaml` | Bases PostgreSQL y exporters de métricas              |
| `compose_stackops_deploy.yaml`  | Consul, Prometheus, Grafana y servicios transversales |
| `compose_apps_deploy.yaml`      | Microservicios Quarkus                                |
| `env/databases.env`             | Variables de las bases de datos                       |
| `env/stackops.env`              | Variables de observabilidad e infraestructura         |
| `env/apps.env`                  | Variables de los microservicios                       |

Cada archivo Compose utiliza su propio archivo de variables:

```text
compose_databases_deploy.yaml → env/databases.env
compose_stackops_deploy.yaml  → env/stackops.env
compose_apps_deploy.yaml      → env/apps.env
```

---

# Arquitectura inicial

```text
                         ┌──────────────────────┐
                         │       Consul         │
                         │     consul:8500      │
                         └──────────┬───────────┘
                                    │
                   ┌────────────────┴────────────────┐
                   │                                 │
          ┌────────▼────────┐               ┌────────▼────────┐
          │  auth-service   │               │  audit-service  │
          │      :8081      │──────────────▶│      :8084      │
          └────────┬────────┘   REST audit  └────────┬────────┘
                   │                                 │
          ┌────────▼────────┐               ┌────────▼────────┐
          │     auth-db     │               │    audit-db     │
          │ PostgreSQL:5432 │               │ PostgreSQL:5432 │
          └────────┬────────┘               └────────┬────────┘
                   │                                 │
          ┌────────▼────────┐               ┌────────▼────────┐
          │ auth exporter   │               │ audit exporter  │
          │      :9187      │               │      :9187      │
          └─────────────────┘               └─────────────────┘
```

Cada microservicio es propietario de su base de datos:

```text
auth-service  → auth-db
audit-service → audit-db
```

Un microservicio no debe acceder directamente a las tablas de otro microservicio.

Por ejemplo, `auth-service` no debe insertar datos directamente en `audit-db`. Debe enviar el evento a `audit-service` mediante REST:

```text
POST http://audit-service:8084/auditoria/eventos
```

`audit-service` será el único responsable de guardar los eventos en su base.

---

# 1. Bases de datos

Archivo:

```text
compose_databases_deploy.yaml
```

Este archivo contiene las bases PostgreSQL independientes de cada microservicio y sus exporters para Prometheus.

## Servicios iniciales

| Servicio Docker           | Propietario            | Base de datos | Esquema        |
| ------------------------- | ---------------------- | ------------- | -------------- |
| `auth-db`                 | `auth-service`         | `sisve_auth`  | `auth_schema`  |
| `audit-db`                | `audit-service`        | `sisve_audit` | `audit_schema` |
| `auth-postgres-exporter`  | Métricas de `auth-db`  | No aplica     | No aplica      |
| `audit-postgres-exporter` | Métricas de `audit-db` | No aplica     | No aplica      |

## Comunicación interna

Los contenedores utilizan el nombre del servicio Docker como host:

```text
auth-db:5432
audit-db:5432
```

### URL de Auth Database

```text
jdbc:postgresql://auth-db:5432/sisve_auth?currentSchema=auth_schema
```

### URL de Audit Database

```text
jdbc:postgresql://audit-db:5432/sisve_audit?currentSchema=audit_schema
```

Dentro de los contenedores no se debe utilizar:

```text
localhost:54321
localhost:54322
```

`localhost` representa al mismo contenedor desde el que se realiza la conexión.

## Acceso desde Windows

Los puertos publicados permiten conectarse desde DBeaver, pgAdmin o IntelliJ.

| Base  | Desde Windows     | Desde Docker    |
| ----- | ----------------- | --------------- |
| Auth  | `localhost:54321` | `auth-db:5432`  |
| Audit | `localhost:54322` | `audit-db:5432` |

---

# 2. Servicios de plataforma

Archivo:

```text
compose_stackops_deploy.yaml
```

Este archivo contiene los servicios transversales de la plataforma.

## Servicios previstos

| Servicio      | Función                                |              Puerto |
| ------------- | -------------------------------------- | ------------------: |
| `consul`      | Registro y descubrimiento de servicios |              `8500` |
| `prometheus`  | Recolección de métricas                |              `9090` |
| `grafana`     | Visualización de métricas              |              `3000` |
| `keycloak`    | Administración de identidades y roles  | Según configuración |
| `api-gateway` | Punto de entrada del sistema           | Según configuración |

Para la primera ejecución, el servicio obligatorio es:

```text
consul
```

Prometheus podrá consultar:

```text
auth-service:8081/metrics
audit-service:8084/metrics
auth-postgres-exporter:9187/metrics
audit-postgres-exporter:9187/metrics
```

Grafana debe utilizar Prometheus como fuente de datos:

```text
http://prometheus:9090
```

---

# 3. Microservicios de aplicación

Archivo:

```text
compose_apps_deploy.yaml
```

Este archivo contiene los microservicios Quarkus desarrollados para SISVE.

## Servicios iniciales

| Servicio        | Puerto interno | Base de datos |
| --------------- | -------------: | ------------- |
| `auth-service`  |         `8081` | `auth-db`     |
| `audit-service` |         `8084` | `audit-db`    |

## Comunicación entre servicios

`auth-service` debe comunicarse con auditoría mediante:

```text
http://audit-service:8084
```

No debe utilizar:

```text
http://localhost:8084
```

Dentro del contenedor de autenticación, `localhost` representa al propio `auth-service`.

---

# 4. Variables de entorno

## `env/databases.env`

```env
# ==========================================
# AUTH DATABASE
# ==========================================

AUTH_DB_NAME=sisve_auth
AUTH_DB_USERNAME=auth_app_user
AUTH_DB_PASSWORD=change_this_password
AUTH_DB_HOST_PORT=54321

# ==========================================
# AUDIT DATABASE
# ==========================================

AUDIT_DB_NAME=sisve_audit
AUDIT_DB_USERNAME=audit_app_user
AUDIT_DB_PASSWORD=change_this_password
AUDIT_DB_HOST_PORT=54322

# ==========================================
# POSTGRES EXPORTERS
# ==========================================

AUTH_POSTGRES_EXPORTER_PORT=9187
AUDIT_POSTGRES_EXPORTER_PORT=9188
```

## `env/apps.env`

```env
# ==========================================
# AUTH SERVICE
# ==========================================

AUTH_SERVICE_PORT=8081
AUTH_DB_URL=jdbc:postgresql://auth-db:5432/sisve_auth?currentSchema=auth_schema
AUTH_DB_USERNAME=auth_app_user
AUTH_DB_PASSWORD=change_this_password

# ==========================================
# AUDIT SERVICE
# ==========================================

AUDIT_SERVICE_PORT=8084
AUDIT_DB_URL=jdbc:postgresql://audit-db:5432/sisve_audit?currentSchema=audit_schema
AUDIT_DB_USERNAME=audit_app_user
AUDIT_DB_PASSWORD=change_this_password

# ==========================================
# COMUNICACIÓN INTERNA
# ==========================================

AUDIT_SERVICE_URL=http://audit-service:8084

# ==========================================
# CONSUL
# ==========================================

CONSUL_HOST=consul
CONSUL_PORT=8500
```

## `env/stackops.env`

```env
# Consul
CONSUL_PORT=8500

# Prometheus
PROMETHEUS_PORT=9090

# Grafana
GRAFANA_PORT=3000
GRAFANA_ADMIN_USER=admin
GRAFANA_ADMIN_PASSWORD=change_this_password
```

## Reglas de los archivos `.env`

La sintaxis correcta es:

```env
VARIABLE=valor
```

Correcto:

```env
AUTH_DB_NAME=sisve_auth
```

Incorrecto:

```env
AUTH_DB_NAME = sisve_auth
```

No deben existir espacios alrededor del signo `=`.

Las contraseñas reales no deben guardarse en Git.

Agregar al `.gitignore`:

```gitignore
Infraestructure/deployment/env/*.env
!Infraestructure/deployment/env/*.env.example
```

Se recomienda crear archivos de ejemplo:

```text
env/apps.env.example
env/databases.env.example
env/stackops.env.example
```

Los archivos `.env.example` deben contener las mismas variables, pero nunca contraseñas reales.

---

# 5. Red Docker compartida

Los tres archivos Compose deben utilizar la misma red externa:

```text
sisve-network
```

Crear la red una sola vez:

```powershell
docker network create sisve-network
```

En cada archivo Compose se debe declarar:

```yaml
networks:
  sisve-network:
    external: true
```

Cada servicio debe conectarse a la red:

```yaml
services:
  nombre-servicio:
    networks:
      - sisve-network
```

Esto permite la comunicación entre servicios levantados desde diferentes archivos Compose.

Ejemplos:

```text
auth-service → auth-db:5432
auth-service → audit-service:8084
auth-service → consul:8500

audit-service → audit-db:5432
audit-service → consul:8500

prometheus → auth-service:8081
prometheus → audit-service:8084
prometheus → auth-postgres-exporter:9187
prometheus → audit-postgres-exporter:9187
```

---

# 6. Requisitos

Antes de levantar la plataforma:

* Docker Desktop debe estar instalado.
* Docker Compose v2 debe estar disponible.
* La red `sisve-network` debe existir.
* Los tres archivos `.env` deben estar configurados.
* Los Dockerfiles de los microservicios deben existir.
* Los proyectos Quarkus deben poder compilarse con Java 21.
* Los puertos definidos deben estar disponibles.

Verificar Docker:

```powershell
docker --version
docker compose version
```

---

# 7. Validar los archivos Compose

## Bases de datos

```powershell
docker compose `
  --env-file .\env\databases.env `
  -p sisve-databases `
  -f .\compose_databases_deploy.yaml `
  config
```

## StackOps

```powershell
docker compose `
  --env-file .\env\stackops.env `
  -p sisve-stackops `
  -f .\compose_stackops_deploy.yaml `
  config
```

## Aplicaciones

```powershell
docker compose `
  --env-file .\env\apps.env `
  -p sisve-apps `
  -f .\compose_apps_deploy.yaml `
  config
```

> El comando `config` puede mostrar las contraseñas sustituidas en la terminal. No compartir su salida cuando contenga credenciales.

---

# 8. Orden de despliegue

El orden recomendado es:

```text
1. Crear la red
2. Levantar las bases de datos
3. Levantar StackOps
4. Levantar los microservicios
```

## Crear la red

```powershell
docker network create sisve-network
```

## Levantar las bases de datos

```powershell
docker compose `
  --env-file .\env\databases.env `
  -p sisve-databases `
  -f .\compose_databases_deploy.yaml `
  up -d
```

En una sola línea:

```powershell
docker compose --env-file .\env\databases.env -p sisve-databases -f .\compose_databases_deploy.yaml up -d
```

## Levantar StackOps

```powershell
docker compose `
  --env-file .\env\stackops.env `
  -p sisve-stackops `
  -f .\compose_stackops_deploy.yaml `
  up -d
```

## Levantar las aplicaciones

```powershell
docker compose `
  --env-file .\env\apps.env `
  -p sisve-apps `
  -f .\compose_apps_deploy.yaml `
  up -d --build
```

---

# 9. Verificar contenedores

Listar contenedores activos:

```powershell
docker ps
```

Listar todos los contenedores:

```powershell
docker ps -a
```

## Bases de datos

```powershell
docker compose `
  --env-file .\env\databases.env `
  -p sisve-databases `
  -f .\compose_databases_deploy.yaml `
  ps
```

## StackOps

```powershell
docker compose `
  --env-file .\env\stackops.env `
  -p sisve-stackops `
  -f .\compose_stackops_deploy.yaml `
  ps
```

## Aplicaciones

```powershell
docker compose `
  --env-file .\env\apps.env `
  -p sisve-apps `
  -f .\compose_apps_deploy.yaml `
  ps
```

---

# 10. Logs

## Auth Service

```powershell
docker compose `
  -p sisve-apps `
  -f .\compose_apps_deploy.yaml `
  logs -f auth-service
```

## Audit Service

```powershell
docker compose `
  -p sisve-apps `
  -f .\compose_apps_deploy.yaml `
  logs -f audit-service
```

## Auth Database

```powershell
docker compose `
  -p sisve-databases `
  -f .\compose_databases_deploy.yaml `
  logs -f auth-db
```

## Audit Database

```powershell
docker compose `
  -p sisve-databases `
  -f .\compose_databases_deploy.yaml `
  logs -f audit-db
```

## Consul

```powershell
docker compose `
  -p sisve-stackops `
  -f .\compose_stackops_deploy.yaml `
  logs -f consul
```

---

# 11. Endpoints de comprobación

## Auth Service

| Recurso    | URL                                  |
| ---------- | ------------------------------------ |
| Health     | `http://localhost:8081/health`       |
| Liveness   | `http://localhost:8081/health/live`  |
| Readiness  | `http://localhost:8081/health/ready` |
| Swagger UI | `http://localhost:8081/swagger-ui`   |
| Métricas   | `http://localhost:8081/metrics`      |

## Audit Service

| Recurso    | URL                                  |
| ---------- | ------------------------------------ |
| Health     | `http://localhost:8084/health`       |
| Liveness   | `http://localhost:8084/health/live`  |
| Readiness  | `http://localhost:8084/health/ready` |
| Swagger UI | `http://localhost:8084/swagger-ui`   |
| Métricas   | `http://localhost:8084/metrics`      |

## Servicios de infraestructura

| Servicio                  | URL                             |
| ------------------------- | ------------------------------- |
| Consul                    | `http://localhost:8500`         |
| Prometheus                | `http://localhost:9090`         |
| Grafana                   | `http://localhost:3000`         |
| Auth PostgreSQL Exporter  | `http://localhost:9187/metrics` |
| Audit PostgreSQL Exporter | `http://localhost:9188/metrics` |

---

# 12. Detener la plataforma

Se recomienda detener los servicios en orden inverso.

## Aplicaciones

```powershell
docker compose `
  --env-file .\env\apps.env `
  -p sisve-apps `
  -f .\compose_apps_deploy.yaml `
  down
```

## StackOps

```powershell
docker compose `
  --env-file .\env\stackops.env `
  -p sisve-stackops `
  -f .\compose_stackops_deploy.yaml `
  down
```

## Bases de datos

```powershell
docker compose `
  --env-file .\env\databases.env `
  -p sisve-databases `
  -f .\compose_databases_deploy.yaml `
  down
```

El comando `down` no elimina los volúmenes de PostgreSQL.

---

# 13. Eliminar los datos locales

Para eliminar los contenedores y volúmenes de bases de datos:

```powershell
docker compose `
  --env-file .\env\databases.env `
  -p sisve-databases `
  -f .\compose_databases_deploy.yaml `
  down -v
```

> Este comando elimina permanentemente los datos locales. Debe utilizarse solamente durante desarrollo o cuando se desea reinicializar las bases.

---

# 14. Persistencia

Cada base debe utilizar un volumen independiente:

```text
auth_pgdata
audit_pgdata
```

Ejemplo:

```yaml
volumes:
  auth_pgdata:
    name: sisve-auth-postgres-data

  audit_pgdata:
    name: sisve-audit-postgres-data
```

No se debe compartir un volumen entre dos contenedores PostgreSQL.

---

# 15. Health checks

Cada base de datos debe tener un health check.

Ejemplo para Auth Database:

```yaml
healthcheck:
  test:
    [
      "CMD-SHELL",
      "pg_isready -U ${AUTH_DB_USERNAME} -d ${AUTH_DB_NAME}"
    ]
  interval: 10s
  timeout: 5s
  retries: 10
  start_period: 10s
```

El exporter puede esperar a que la base esté disponible:

```yaml
depends_on:
  auth-db:
    condition: service_healthy
```

Como las aplicaciones y bases se encuentran en archivos Compose diferentes, no se debe depender únicamente de `depends_on`. Los microservicios también deben implementar readiness checks y reintentos de conexión.

---

# 16. Seguridad

* No guardar contraseñas reales en Git.
* No utilizar el usuario `postgres` desde los microservicios en producción.
* Crear un usuario de aplicación por microservicio.
* `auth-service` solamente debe acceder a `auth-db`.
* `audit-service` solamente debe acceder a `audit-db`.
* No exponer PostgreSQL públicamente en producción.
* No registrar JWT completos en los logs.
* No guardar JWT en texto plano.
* No guardar las claves privadas JWT en el repositorio.
* El endpoint de escritura de auditoría debe mantenerse interno.
* Las consultas administrativas deben protegerse mediante JWT y roles.
* Solamente deben publicarse los puertos necesarios.
* En producción se recomienda utilizar Docker Secrets, Vault o Kubernetes Secrets.

---

# 17. Incorporar nuevos microservicios

Cada nuevo microservicio debe seguir el mismo patrón.

Ejemplo:

```text
vote-service → vote-db
```

Pasos:

1. Agregar la base en `compose_databases_deploy.yaml`.
2. Agregar su exporter de PostgreSQL.
3. Agregar las variables en `env/databases.env`.
4. Agregar el microservicio en `compose_apps_deploy.yaml`.
5. Agregar sus variables en `env/apps.env`.
6. Conectarlo a `sisve-network`.
7. Configurar health checks.
8. Exponer métricas.
9. Registrarlo en Consul.
10. Evitar acceso directo a bases de otros servicios.

Ejemplo de URL interna:

```text
jdbc:postgresql://vote-db:5432/sisve_vote?currentSchema=vote_schema
```

---

# 18. Problemas frecuentes

## El microservicio no encuentra PostgreSQL

Debe utilizar el nombre Docker:

```text
jdbc:postgresql://auth-db:5432/sisve_auth
```

No debe utilizar:

```text
jdbc:postgresql://localhost:54321/sisve_auth
```

## Auth Service no encuentra Audit Service

Debe utilizar:

```text
http://audit-service:8084
```

No debe utilizar:

```text
http://localhost:8084
```

## Las variables aparecen vacías

Comprobar que se esté usando el archivo correspondiente:

```powershell
docker compose --env-file .\env\apps.env -f .\compose_apps_deploy.yaml config
```

## El puerto está ocupado

Cambiar solamente el puerto del host:

```env
AUTH_DB_HOST_PORT=54331
```

El puerto interno de PostgreSQL continúa siendo:

```text
5432
```

## PostgreSQL mantiene las credenciales anteriores

Las variables `POSTGRES_USER`, `POSTGRES_PASSWORD` y `POSTGRES_DB` se aplican al crear el volumen por primera vez.

Para reinicializar en desarrollo:

```powershell
docker compose `
  --env-file .\env\databases.env `
  -p sisve-databases `
  -f .\compose_databases_deploy.yaml `
  down -v
```

Después se levantan nuevamente las bases.

---

# Resumen

La plataforma queda dividida de la siguiente manera:

```text
compose_databases_deploy.yaml
    ├── auth-db
    ├── audit-db
    ├── auth-postgres-exporter
    └── audit-postgres-exporter

compose_stackops_deploy.yaml
    ├── consul
    ├── prometheus
    └── grafana

compose_apps_deploy.yaml
    ├── auth-service
    └── audit-service
```

Todos los servicios se comunican mediante:

```text
sisve-network
```

El orden de despliegue es:

```text
Bases de datos → StackOps → Aplicaciones
```

Los servicios mínimos de la primera etapa son:

```text
auth-db
audit-db
auth-postgres-exporter
audit-postgres-exporter
consul
auth-service
audit-service
```
