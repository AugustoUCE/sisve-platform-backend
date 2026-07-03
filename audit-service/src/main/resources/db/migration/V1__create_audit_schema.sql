CREATE SCHEMA IF NOT EXISTS audit_schema;

CREATE TABLE IF NOT EXISTS audit_schema.auditoria (
    id_auditoria BIGSERIAL PRIMARY KEY,
    tipo_evento VARCHAR(50) NOT NULL,
    descripcion TEXT,
    fecha_evento TIMESTAMP NOT NULL DEFAULT now(),
    ip_origen VARCHAR(45),
    servicio_origen VARCHAR(50) NOT NULL
);