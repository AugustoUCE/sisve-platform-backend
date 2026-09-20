INSERT INTO auth_schema.votante
    (id_votante, cedula, correo_institucional, nombres, apellidos, estado, voto)
VALUES
    (2, '1712345678', 'juan.perez@uce.edu.ec', 'Juan', 'Pérez', true, false)
ON CONFLICT (cedula) DO NOTHING;

SELECT setval(
    pg_get_serial_sequence('auth_schema.votante', 'id_votante'),
    GREATEST((SELECT COALESCE(MAX(id_votante), 1) FROM auth_schema.votante), 1),
    true
);
