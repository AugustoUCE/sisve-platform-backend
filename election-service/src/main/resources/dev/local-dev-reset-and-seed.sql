-- SOLO DESARROLLO LOCAL
-- Preparación reproducible para probar SISVE con elección activa, padrón y voto limpio.
-- No ejecutar en producción.

BEGIN;

ALTER TABLE election_schema.votante_eleccion DISABLE TRIGGER USER;

INSERT INTO election_schema.eleccion (
    id_eleccion,
    nombre,
    descripcion,
    fecha_inicio,
    fecha_fin,
    estado
) VALUES (
    1,
    'Elección de prueba SISVE',
    'Elección local para validar el flujo de voto del frontend',
    TIMESTAMP '2026-01-01 00:00:00',
    TIMESTAMP '2026-12-31 23:59:59',
    'activo'
)
ON CONFLICT (id_eleccion) DO UPDATE SET
    nombre = EXCLUDED.nombre,
    descripcion = EXCLUDED.descripcion,
    fecha_inicio = EXCLUDED.fecha_inicio,
    fecha_fin = EXCLUDED.fecha_fin,
    estado = EXCLUDED.estado;

INSERT INTO election_schema.cargo (
    id_cargo,
    id_eleccion,
    nombre
) VALUES (
    1,
    1,
    'Presidencia'
)
ON CONFLICT (id_cargo) DO UPDATE SET
    id_eleccion = EXCLUDED.id_eleccion,
    nombre = EXCLUDED.nombre;

INSERT INTO election_schema.candidato (
    id_candidato,
    id_cargo,
    nombres,
    apellidos,
    lista,
    estado
) VALUES (
    1,
    1,
    'Carlos',
    'Mora',
    'Lista 1',
    true
)
ON CONFLICT (id_candidato) DO UPDATE SET
    id_cargo = EXCLUDED.id_cargo,
    nombres = EXCLUDED.nombres,
    apellidos = EXCLUDED.apellidos,
    lista = EXCLUDED.lista,
    estado = EXCLUDED.estado;

INSERT INTO election_schema.votante_eleccion (
    id_votante,
    id_eleccion,
    ha_votado,
    fecha_participacion
) VALUES
    (1, 1, false, NULL),
    (3, 1, false, NULL)
ON CONFLICT (id_votante, id_eleccion) DO UPDATE SET
    ha_votado = EXCLUDED.ha_votado,
    fecha_participacion = EXCLUDED.fecha_participacion;

TRUNCATE TABLE vote_schema.voto RESTART IDENTITY CASCADE;

SELECT setval(pg_get_serial_sequence('election_schema.eleccion', 'id_eleccion'), GREATEST((SELECT COALESCE(MAX(id_eleccion), 1) FROM election_schema.eleccion), 1), true);
SELECT setval(pg_get_serial_sequence('election_schema.cargo', 'id_cargo'), GREATEST((SELECT COALESCE(MAX(id_cargo), 1) FROM election_schema.cargo), 1), true);
SELECT setval(pg_get_serial_sequence('election_schema.candidato', 'id_candidato'), GREATEST((SELECT COALESCE(MAX(id_candidato), 1) FROM election_schema.candidato), 1), true);

ALTER TABLE election_schema.votante_eleccion ENABLE TRIGGER USER;

COMMIT;

-- Nota:
-- Si solo necesitas resetear el padrón sin volver a sembrar, ejecuta manualmente:
-- ALTER TABLE election_schema.votante_eleccion DISABLE TRIGGER USER;
-- UPDATE election_schema.votante_eleccion
-- SET ha_votado = false,
--     fecha_participacion = NULL
-- WHERE id_eleccion = 1
--   AND id_votante IN (1, 3);
-- ALTER TABLE election_schema.votante_eleccion ENABLE TRIGGER USER;
-- TRUNCATE TABLE vote_schema.voto RESTART IDENTITY CASCADE;
