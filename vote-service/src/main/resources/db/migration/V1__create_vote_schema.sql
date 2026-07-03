CREATE SCHEMA IF NOT EXISTS vote_schema;

CREATE TABLE vote_schema.voto (
    id_voto BIGSERIAL PRIMARY KEY,
    id_eleccion BIGINT NOT NULL,
    voto_cifrado TEXT NOT NULL,
    hash_anterior VARCHAR(64) NOT NULL,
    hash_actual VARCHAR(64) NOT NULL UNIQUE,
    fecha_registro TIMESTAMP NOT NULL DEFAULT now()
);