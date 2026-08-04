# SISVE

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

##  Ejecutar service desde la raíz

./gradlew :nombre_proyecto:quarkusDev
## Flujo de Trabajo (Gitflow)

Este proyecto sigue el modelo de ramas Gitflow para mantener un historial ordenado y visual:

- **main**: Rama de producción. Solo contiene código estable y probado. Cada fusión aquí lleva un tag de versión (ej. v1.0).
- **quality**: Rama de QA/pruebas. Aquí se valida la funcionalidad antes de pasar a producción.
- **development**: Rama de desarrollo base. De aquí nacen todas las tareas nuevas.
- **feature/**: Ramas temporales para tareas o características específicas. **Nunca se trabaja directo en development, quality o main.**

### 🛠️ Flujo de Trabajo para una Nueva Tarea

Cuando vayas a programar algo nuevo (una función, un fix, una documentación), sigue estos pasos para que el gráfico de Git se ramifique correctamente:

#### 1. Crear la rama de la tarea desde development
Asegúrate de estar en `development` actualizado y crea tu rama de tarea:
```bash
git checkout development
git pull origin development
git checkout -b feature/nombre-de-tu-tarea
```

#### 2. Desarrollar y hacer commits
Trabaja en tu código y guarda tus avances de forma normal:
```bash
git add -A
git commit -m "feat: descripción de lo que hiciste"
```

#### 3. Fusionar la tarea de vuelta a development (Genera la curva visual)
Cuando termines la tarea, regresa a `development` y fusiónala. **Es obligatorio USAR `--no-ff`** para que Git Graph dibuje el círculo de unión:
```bash
git checkout development
git merge feature/nombre-de-tu-tarea --no-ff
git push origin development
```
*Una vez fusionada, puedes borrar tu rama local si deseas:* `git branch -d feature/nombre-de-tu-tarea`

### 🚀 Pasar cambios a Calidad (Quality) y Producción (Main)

1. **A Quality para pruebas:** Cuando los cambios en `development` estén listos para ser probados por QA, se fusionan en `quality` (también con `--no-ff`):
```bash
git checkout quality
git pull origin quality
git merge development --no-ff
git push origin quality
```

2. **A Main para producción:** Cuando `quality` pasa las pruebas, se sube a `main` y se etiqueta:
```bash
git checkout main
git pull origin main
git merge quality --no-ff
git push origin main

# Etiquetar la versión
git tag -a v1.0 -m "Release v1.0"
git push origin v1.0
```

### 🚫 Reglas Estrictas
- **Prohibido** hacer commits directo sobre `main`, `quality` o `development`.
- Toda línea de código nueva debe nacer en una rama `feature/`.
- El parámetro `--no-ff` es obligatorio en todos los `git merge` para no perder la visualización del gráfico.

# SISVE Backend - Guía de Ejecución Local

Este documento explica cómo levantar el backend de SISVE en ambiente local para probar el flujo completo desde Swagger o desde el frontend.

## 1. Descripción general

SISVE es un sistema web de voto electrónico universitario basado en microservicios.

El backend está dividido en cinco servicios principales:

| Microservicio | Puerto | Descripción |
|---|---:|---|
| auth-service | 8081 | Autenticación, login, logout y validación de sesión |
| election-service | 8082 | Elecciones, cargos, candidatos y estado de participación |
| vote-service | 8083 | Emisión de votos, validación de voto e integridad |
| audit-service | 8084 | Registro y consulta de auditorías |
| polling-station-service | 8085 | Mesas electorales, padrón por mesa y habilitación de votantes |

La infraestructura de base de datos y herramientas de soporte se ejecuta con Docker Compose.

Los microservicios se ejecutan localmente con `quarkusDev`.

El flujo local recomendado ahora incluye la mesa electoral:

1. `auth-service`
2. `election-service`
3. `polling-station-service`
4. `vote-service`
5. `audit-service`

Para la mesa electoral, iniciar el servicio desde la raíz con:

```powershell
.\gradlew.bat :polling-station-service:quarkusDev "-Ddebug=false"
```

---

## 2. Requisitos previos

Antes de ejecutar el proyecto, verificar que estén instalados:

- Java 21
- Docker Desktop
- PowerShell
- Git
- Visual Studio Code o IntelliJ IDEA
- Gradle Wrapper incluido en el proyecto

Verificar Java:

```powershell
java -version
