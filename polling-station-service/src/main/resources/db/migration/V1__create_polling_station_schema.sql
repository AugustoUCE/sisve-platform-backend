CREATE SCHEMA IF NOT EXISTS polling_station_schema;

CREATE TABLE polling_station_schema.polling_station (
    id_polling_station BIGSERIAL PRIMARY KEY,
    id_election BIGINT NOT NULL,
    code VARCHAR(30) NOT NULL,
    name VARCHAR(150) NOT NULL,
    location VARCHAR(250),
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    opened_at TIMESTAMP NULL,
    closed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NULL,
    CONSTRAINT chk_polling_station_status
        CHECK (status IN ('OPEN', 'CLOSED', 'SUSPENDED'))
);

CREATE INDEX idx_polling_station_id_election
    ON polling_station_schema.polling_station (id_election);

CREATE TABLE polling_station_schema.polling_station_member (
    id_polling_station_member BIGSERIAL PRIMARY KEY,
    id_polling_station BIGINT NOT NULL REFERENCES polling_station_schema.polling_station(id_polling_station),
    user_identifier VARCHAR(50) NOT NULL,
    full_name VARCHAR(200) NOT NULL,
    institutional_email VARCHAR(150),
    role VARCHAR(50) NOT NULL,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_polling_station_member_role
        CHECK (role IN ('POLLING_STATION_PRESIDENT', 'POLLING_STATION_MEMBER'))
);

CREATE INDEX idx_polling_station_member_id_polling_station
    ON polling_station_schema.polling_station_member (id_polling_station);

CREATE INDEX idx_polling_station_member_user_identifier
    ON polling_station_schema.polling_station_member (user_identifier);

CREATE TABLE polling_station_schema.electoral_roll (
    id_electoral_roll BIGSERIAL PRIMARY KEY,
    id_polling_station BIGINT NOT NULL REFERENCES polling_station_schema.polling_station(id_polling_station),
    id_election BIGINT NOT NULL,
    id_voter BIGINT NOT NULL,
    cedula VARCHAR(20) NOT NULL,
    full_name VARCHAR(200) NOT NULL,
    institutional_email VARCHAR(150),
    participation_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    enabled_at TIMESTAMP NULL,
    enabled_by VARCHAR(50) NULL,
    voted_at TIMESTAMP NULL,
    blocked_at TIMESTAMP NULL,
    blocked_by VARCHAR(50) NULL,
    block_reason VARCHAR(250) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NULL,
    CONSTRAINT uq_electoral_roll_election_voter UNIQUE (id_election, id_voter),
    CONSTRAINT chk_electoral_roll_status
        CHECK (participation_status IN ('PENDING', 'ENABLED', 'VOTED', 'BLOCKED'))
);

CREATE INDEX idx_electoral_roll_id_polling_station
    ON polling_station_schema.electoral_roll (id_polling_station);

CREATE INDEX idx_electoral_roll_id_election
    ON polling_station_schema.electoral_roll (id_election);

CREATE INDEX idx_electoral_roll_id_voter
    ON polling_station_schema.electoral_roll (id_voter);

CREATE INDEX idx_electoral_roll_cedula
    ON polling_station_schema.electoral_roll (cedula);

CREATE INDEX idx_electoral_roll_participation_status
    ON polling_station_schema.electoral_roll (participation_status);
