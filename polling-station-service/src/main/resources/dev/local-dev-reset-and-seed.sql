-- SOLO DESARROLLO LOCAL
-- Reset y seed reproducible para pruebas del flujo de mesa electoral.
-- No ejecutar en producción.

BEGIN;

TRUNCATE TABLE
    polling_station_schema.electoral_roll,
    polling_station_schema.polling_station_member,
    polling_station_schema.polling_station
RESTART IDENTITY CASCADE;

INSERT INTO polling_station_schema.polling_station (
    id_polling_station,
    id_election,
    code,
    name,
    location,
    status,
    opened_at,
    closed_at,
    created_at,
    updated_at
) VALUES (
    1,
    1,
    'MESA-001',
    'Mesa Electoral 001',
    'Facultad de Ingeniería',
    'OPEN',
    NOW(),
    NULL,
    NOW(),
    NULL
);

INSERT INTO polling_station_schema.polling_station_member (
    id_polling_station_member,
    id_polling_station,
    user_identifier,
    full_name,
    institutional_email,
    role,
    status,
    created_at
) VALUES (
    1,
    1,
    'PRESIDENTE001',
    'Presidente Mesa 001',
    'presidente.mesa001@uce.edu.ec',
    'POLLING_STATION_PRESIDENT',
    TRUE,
    NOW()
);

INSERT INTO polling_station_schema.electoral_roll (
    id_electoral_roll,
    id_polling_station,
    id_election,
    id_voter,
    cedula,
    full_name,
    institutional_email,
    participation_status,
    enabled_at,
    enabled_by,
    voted_at,
    blocked_at,
    blocked_by,
    block_reason,
    created_at,
    updated_at
) VALUES
(
    1,
    1,
    1,
    1,
    '1723456789',
    'Juan Carlos Pérez López',
    'juan.perez@uce.edu.ec',
    'PENDING',
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NOW(),
    NULL
),
(
    2,
    1,
    1,
    3,
    '1709876543',
    'Carlos Mora',
    'carlos.mora@uce.edu.ec',
    'PENDING',
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NOW(),
    NULL
);

SELECT setval(pg_get_serial_sequence('polling_station_schema.polling_station', 'id_polling_station'), 1, true);
SELECT setval(pg_get_serial_sequence('polling_station_schema.polling_station_member', 'id_polling_station_member'), 1, true);
SELECT setval(pg_get_serial_sequence('polling_station_schema.electoral_roll', 'id_electoral_roll'), 2, true);

COMMIT;
