CREATE TABLE polling_station_schema.vote_attempt (
    id_vote_attempt BIGSERIAL PRIMARY KEY,
    id_election BIGINT NOT NULL,
    id_voter BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_vote_attempt_election_voter UNIQUE (id_election, id_voter),
    CONSTRAINT chk_vote_attempt_status CHECK (status IN ('RESERVED', 'COMPLETED'))
);
