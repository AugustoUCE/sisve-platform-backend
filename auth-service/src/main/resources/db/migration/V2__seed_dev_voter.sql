INSERT INTO auth_schema.votante
    (id_votante, cedula, correo_institucional, nombres, apellidos, estado, voto)
VALUES
    (1, '1709876543', 'carlos.mora@uce.edu.ec', 'Carlos', 'Mora', true, false)
ON CONFLICT (cedula) DO NOTHING;

SELECT setval(
    pg_get_serial_sequence('auth_schema.votante', 'id_votante'),
    GREATEST((SELECT COALESCE(MAX(id_votante), 1) FROM auth_schema.votante), 1),
    true
);
