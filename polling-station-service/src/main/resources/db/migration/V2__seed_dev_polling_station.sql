INSERT INTO polling_station_schema.polling_station
    (id_polling_station, id_election, code, name, location, status, opened_at, closed_at)
VALUES
    (1, 1, 'MESA-001', 'Mesa principal', 'Campus universitario', 'OPEN', NOW(), NULL)
ON CONFLICT (id_polling_station) DO UPDATE SET
    id_election = EXCLUDED.id_election,
    code = EXCLUDED.code,
    name = EXCLUDED.name,
    location = EXCLUDED.location,
    status = EXCLUDED.status,
    opened_at = EXCLUDED.opened_at,
    closed_at = EXCLUDED.closed_at,
    updated_at = NOW();

INSERT INTO polling_station_schema.polling_station_member
    (id_polling_station, user_identifier, full_name, institutional_email, role, status)
VALUES
    (1, 'mesa-001-presidente', 'Presidente Mesa 001', 'mesa001@uce.edu.ec', 'POLLING_STATION_PRESIDENT', true)
ON CONFLICT DO NOTHING;

INSERT INTO polling_station_schema.electoral_roll
    (id_polling_station, id_election, id_voter, cedula, full_name, institutional_email, participation_status, enabled_at, enabled_by)
VALUES
    (1, 1, 1, '1709876543', 'Carlos Mora', 'carlos.mora@uce.edu.ec', 'ENABLED', NOW(), 'seed-dev')
ON CONFLICT (id_election, id_voter) DO NOTHING;

SELECT setval(pg_get_serial_sequence('polling_station_schema.polling_station', 'id_polling_station'), GREATEST((SELECT COALESCE(MAX(id_polling_station), 1) FROM polling_station_schema.polling_station), 1), true);
SELECT setval(pg_get_serial_sequence('polling_station_schema.polling_station_member', 'id_polling_station_member'), GREATEST((SELECT COALESCE(MAX(id_polling_station_member), 1) FROM polling_station_schema.polling_station_member), 1), true);
SELECT setval(pg_get_serial_sequence('polling_station_schema.electoral_roll', 'id_electoral_roll'), GREATEST((SELECT COALESCE(MAX(id_electoral_roll), 1) FROM polling_station_schema.electoral_roll), 1), true);
