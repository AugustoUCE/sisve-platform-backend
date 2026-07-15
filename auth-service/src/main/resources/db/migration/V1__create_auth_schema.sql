CREATE SCHEMA IF NOT EXISTS auth_schema;

CREATE TABLE auth_schema.votante (
    id_votante BIGSERIAL PRIMARY KEY,
    cedula VARCHAR(10) UNIQUE NOT NULL,
    correo_institucional VARCHAR(100) UNIQUE NOT NULL,
    nombres VARCHAR(80) NOT NULL,
    apellidos VARCHAR(80) NOT NULL,
    estado BOOLEAN NOT NULL DEFAULT true

);

CREATE TABLE auth_schema.sesion (
    id_sesion BIGSERIAL PRIMARY KEY,
    id_votante BIGINT  NOT NULL REFERENCES auth_schema.votante(id_votante),
    token_hash VARCHAR(256) NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT now(),
    fecha_expiracion TIMESTAMP NOT NULL,
    estado BOOLEAN NOT NULL DEFAULT true
);

/*mejorar a esto
CREATE SCHEMA IF NOT EXISTS auth_schema;

CREATE TABLE auth_schema.votante (
                                     id_votante BIGSERIAL NOT NULL,
                                     cedula VARCHAR(10) NOT NULL,
                                     correo_institucional VARCHAR(100) NOT NULL,
                                     nombres VARCHAR(80) NOT NULL,
                                     apellidos VARCHAR(80) NOT NULL,
                                     estado BOOLEAN NOT NULL DEFAULT TRUE,

                                     CONSTRAINT pk_votante
                                         PRIMARY KEY (id_votante),

                                     CONSTRAINT uk_votante_cedula
                                         UNIQUE (cedula),

                                     CONSTRAINT uk_votante_correo_institucional
                                         UNIQUE (correo_institucional)
);

CREATE TABLE auth_schema.sesion (
                                    id_sesion BIGSERIAL NOT NULL,
                                    id_votante BIGINT NOT NULL,
                                    token_hash VARCHAR(256) NOT NULL,
                                    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    fecha_expiracion TIMESTAMP NOT NULL,
                                    estado BOOLEAN NOT NULL DEFAULT TRUE,

                                    CONSTRAINT pk_sesion
                                        PRIMARY KEY (id_sesion),

                                    CONSTRAINT fk_sesion_votante
                                        FOREIGN KEY (id_votante)
                                            REFERENCES auth_schema.votante(id_votante)
);

CREATE INDEX idx_sesion_id_votante
    ON auth_schema.sesion(id_votante);

CREATE INDEX idx_sesion_estado
    ON auth_schema.sesion(estado);

CREATE INDEX idx_sesion_fecha_expiracion
    ON auth_schema.sesion(fecha_expiracion);*/