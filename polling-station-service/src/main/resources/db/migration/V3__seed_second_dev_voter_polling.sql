INSERT INTO polling_station_schema.electoral_roll
    (id_polling_station, id_election, id_voter, cedula, full_name, institutional_email, participation_status)
VALUES
    (1, 1, 2, '1712345678', 'Juan Pérez', 'juan.perez@uce.edu.ec', 'PENDING')
ON CONFLICT (id_election, id_voter) DO NOTHING;
