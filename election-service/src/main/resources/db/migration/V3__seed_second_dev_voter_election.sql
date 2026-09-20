INSERT INTO election_schema.votante_eleccion
    (id_votante, id_eleccion, ha_votado, fecha_participacion)
VALUES
    (2, 1, false, NULL)
ON CONFLICT (id_votante, id_eleccion) DO NOTHING;
