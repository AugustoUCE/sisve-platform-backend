CREATE SCHEMA IF NOT EXISTS auth_schema;

CREATE TABLE auth_schema.votante (
    id_votante SERIAL PRIMARY KEY,
    cedula VARCHAR(10) UNIQUE NOT NULL,
    correo_institucional VARCHAR(100) UNIQUE NOT NULL,
    nombres VARCHAR(80) NOT NULL,
    apellidos VARCHAR(80) NOT NULL,
    estado BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE auth_schema.sesion (
    id_sesion SERIAL PRIMARY KEY,
    id_votante INTEGER NOT NULL REFERENCES auth_schema.votante(id_votante),
    token_hash VARCHAR(256) NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT now(),
    fecha_expiracion TIMESTAMP NOT NULL,
    estado BOOLEAN NOT NULL DEFAULT true
);