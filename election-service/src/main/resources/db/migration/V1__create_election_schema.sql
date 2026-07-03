CREATE SCHEMA IF NOT EXISTS election_schema;

CREATE TABLE election_schema.eleccion (
    id_eleccion BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL,
    descripcion TEXT,
    fecha_inicio TIMESTAMP NOT NULL,
    fecha_fin TIMESTAMP NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'planificado'
);

CREATE TABLE election_schema.cargo (
    id_cargo BIGSERIAL PRIMARY KEY,
    id_eleccion BIGINT NOT NULL REFERENCES election_schema.eleccion(id_eleccion),
    nombre VARCHAR(80) NOT NULL
);

CREATE TABLE election_schema.candidato (
    id_candidato BIGSERIAL PRIMARY KEY,
    id_cargo BIGINT NOT NULL REFERENCES election_schema.cargo(id_cargo),
    nombres VARCHAR(80) NOT NULL,
    apellidos VARCHAR(80) NOT NULL,
    lista VARCHAR(50),
    estado BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE election_schema.votante_eleccion (
    id_votante BIGINT NOT NULL,
    id_eleccion BIGINT NOT NULL REFERENCES election_schema.eleccion(id_eleccion),
    ha_votado BOOLEAN NOT NULL DEFAULT false,
    fecha_participacion TIMESTAMP,
    PRIMARY KEY (id_votante, id_eleccion)
);

CREATE UNIQUE INDEX idx_votante_eleccion_unico
    ON election_schema.votante_eleccion (id_votante, id_eleccion)
    WHERE ha_votado = false;

CREATE OR REPLACE FUNCTION election_schema.prevent_unvote()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.ha_votado = true AND NEW.ha_votado = false THEN
        RAISE EXCEPTION 'No se puede revertir un voto emitido';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_prevent_unvote
    BEFORE UPDATE ON election_schema.votante_eleccion
    FOR EACH ROW EXECUTE FUNCTION election_schema.prevent_unvote();