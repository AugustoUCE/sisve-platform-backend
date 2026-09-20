INSERT INTO election_schema.eleccion
    (id_eleccion, nombre, descripcion, fecha_inicio, fecha_fin, estado)
VALUES
    (1, 'Elección estudiantil 2026', 'Elección de representantes estudiantiles', '2026-01-01 00:00:00', '2026-12-31 23:59:59', 'activo')
ON CONFLICT (id_eleccion) DO UPDATE SET
    nombre = EXCLUDED.nombre,
    descripcion = EXCLUDED.descripcion,
    fecha_inicio = EXCLUDED.fecha_inicio,
    fecha_fin = EXCLUDED.fecha_fin,
    estado = EXCLUDED.estado;

INSERT INTO election_schema.cargo (id_cargo, id_eleccion, nombre)
VALUES (1, 1, 'Presidente de la Asociación de Estudiantes')
ON CONFLICT (id_cargo) DO UPDATE SET nombre = EXCLUDED.nombre;

INSERT INTO election_schema.candidato
    (id_candidato, id_cargo, nombres, apellidos, lista, estado)
VALUES
    (1, 1, 'Ana', 'Pérez', 'Lista A', true),
    (2, 1, 'Luis', 'Gómez', 'Lista B', true),
    (3, 1, 'María', 'Rojas', 'Lista C', true)
ON CONFLICT (id_candidato) DO UPDATE SET
    id_cargo = EXCLUDED.id_cargo,
    nombres = EXCLUDED.nombres,
    apellidos = EXCLUDED.apellidos,
    lista = EXCLUDED.lista,
    estado = EXCLUDED.estado;

INSERT INTO election_schema.votante_eleccion
    (id_votante, id_eleccion, ha_votado, fecha_participacion)
VALUES (1, 1, false, NULL)
ON CONFLICT (id_votante, id_eleccion) DO NOTHING;

SELECT setval(pg_get_serial_sequence('election_schema.eleccion', 'id_eleccion'), GREATEST((SELECT COALESCE(MAX(id_eleccion), 1) FROM election_schema.eleccion), 1), true);
SELECT setval(pg_get_serial_sequence('election_schema.cargo', 'id_cargo'), GREATEST((SELECT COALESCE(MAX(id_cargo), 1) FROM election_schema.cargo), 1), true);
SELECT setval(pg_get_serial_sequence('election_schema.candidato', 'id_candidato'), GREATEST((SELECT COALESCE(MAX(id_candidato), 1) FROM election_schema.candidato), 1), true);
