# SISVE

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

##  Ejecutar service desde la raíz

./gradlew :nombre_proyecto:quarkusDev
## Flujo de trabajo (Gitflow)

Este proyecto sigue el modelo de ramas Gitflow:

- **main**: rama de producción. Solo contiene código estable y probado, listo para producción. Cada fusión a esta rama debería ir acompañada de un tag de versión (ej. v1.0).
- **quality**: rama de QA/pruebas. Aquí se valida la funcionalidad y se detectan errores antes de pasar a producción.
- **development**: rama de desarrollo. Aquí se programan y prueban los cambios nuevos antes de enviarlos a `quality`.

### Flujo de trabajo Ejemplo

1. Los cambios nuevos se desarrollan en la rama `development`.
2. Una vez listos, `development` se fusiona en `quality` para pruebas de QA:
```bash
   git checkout quality
   git pull origin quality
   git merge development
   git push origin quality
```
3. Cuando `quality` pasa las pruebas y está lista para producción, se fusiona en `main`:
```bash
   git checkout main
   git pull origin main
   git merge quality
   git push origin main
```
4. Se etiqueta la versión liberada:
```bash
   git tag -a v1.0 -m "Release v1.0"
   git push origin v1.0
```

### Reglas
- Nunca trabajar directamente sobre `main`.
- Todo cambio nuevo se desarrolla primero en `development` antes de llegar a `quality`.
- `main` solo recibe código ya validado desde `quality`.