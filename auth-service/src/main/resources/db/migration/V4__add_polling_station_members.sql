CREATE TABLE auth_schema.miembro_mesa (
    id_miembro_mesa BIGSERIAL PRIMARY KEY,
    user_identifier VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(128) NOT NULL,
    password_salt VARCHAR(64) NOT NULL,
    full_name VARCHAR(200) NOT NULL,
    institutional_email VARCHAR(150) NOT NULL,
    polling_station_id BIGINT NOT NULL,
    status BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE auth_schema.sesion_miembro_mesa (
    id_sesion_miembro_mesa BIGSERIAL PRIMARY KEY,
    id_miembro_mesa BIGINT NOT NULL REFERENCES auth_schema.miembro_mesa(id_miembro_mesa),
    token_hash VARCHAR(256) UNIQUE NOT NULL,
    fecha_expiracion TIMESTAMP NOT NULL,
    estado BOOLEAN NOT NULL DEFAULT true
);

INSERT INTO auth_schema.miembro_mesa
    (id_miembro_mesa, user_identifier, password_hash, password_salt, full_name, institutional_email, polling_station_id, status)
VALUES
    (1, 'mesa-001-presidente', 'e243dc397706f5ac7e4df33442547737cc7d076d0c887491f04aa047e6e384de', 'e350a5971622afcb934719652bcc57e8', 'Presidente Mesa 001', 'mesa001@uce.edu.ec', 1, true)
ON CONFLICT (user_identifier) DO NOTHING;

SELECT setval(pg_get_serial_sequence('auth_schema.miembro_mesa', 'id_miembro_mesa'), GREATEST((SELECT COALESCE(MAX(id_miembro_mesa), 1) FROM auth_schema.miembro_mesa), 1), true);
