CREATE TABLE members (
    id                 BIGSERIAL PRIMARY KEY,
    email              VARCHAR(255) NOT NULL UNIQUE,
    password           VARCHAR(255) NOT NULL,
    name               VARCHAR(100) NOT NULL,
    role               VARCHAR(20) NOT NULL,
    last_completed_at  TIMESTAMPTZ,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_members_role CHECK (role IN ('ADMIN', 'DEVELOPER', 'CLIENT'))
);

CREATE TABLE requests (
    id                     BIGSERIAL PRIMARY KEY,
    title                  VARCHAR(200) NOT NULL,
    description            TEXT,
    category               VARCHAR(20) NOT NULL,
    priority               VARCHAR(20) NOT NULL,
    status                 VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    client_id              BIGINT NOT NULL REFERENCES members(id) ON DELETE RESTRICT,
    assigned_developer_id  BIGINT REFERENCES members(id) ON DELETE RESTRICT,
    version                INTEGER NOT NULL DEFAULT 0,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_requests_category CHECK (category IN ('BUG', 'FEATURE', 'INQUIRY')),
    CONSTRAINT chk_requests_priority CHECK (priority IN ('HIGH', 'MEDIUM', 'LOW')),
    CONSTRAINT chk_requests_status CHECK (status IN ('PENDING', 'IN_PROGRESS', 'DONE')),
    CONSTRAINT chk_requests_version CHECK (version >= 0)
);

CREATE TABLE request_histories (
    id           BIGSERIAL PRIMARY KEY,
    request_id   BIGINT NOT NULL REFERENCES requests(id) ON DELETE RESTRICT,
    changed_by   BIGINT NOT NULL REFERENCES members(id) ON DELETE RESTRICT,
    from_status  VARCHAR(20),
    to_status    VARCHAR(20),
    changed_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    memo         VARCHAR(255),
    CONSTRAINT chk_histories_from_status
        CHECK (from_status IS NULL OR from_status IN ('PENDING', 'IN_PROGRESS', 'DONE')),
    CONSTRAINT chk_histories_to_status
        CHECK (to_status IS NULL OR to_status IN ('PENDING', 'IN_PROGRESS', 'DONE'))
);

CREATE TABLE alerts (
    id                BIGSERIAL PRIMARY KEY,
    request_id        BIGINT NOT NULL REFERENCES requests(id) ON DELETE RESTRICT,
    target_member_id  BIGINT NOT NULL REFERENCES members(id) ON DELETE RESTRICT,
    alert_type        VARCHAR(30) NOT NULL,
    message           VARCHAR(255),
    is_read           BOOLEAN NOT NULL DEFAULT false,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_alerts_type
        CHECK (alert_type IN ('ASSIGNED', 'STATUS_CHANGED', 'HIGH_PRIORITY_REGISTERED'))
);

CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY,
    member_id   BIGINT NOT NULL REFERENCES members(id) ON DELETE RESTRICT,
    token_hash  VARCHAR(64) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_refresh_expiry CHECK (expires_at > created_at),
    CONSTRAINT chk_refresh_revocation CHECK (revoked_at IS NULL OR revoked_at >= created_at)
);

CREATE INDEX idx_requests_status ON requests(status);
CREATE INDEX idx_requests_assigned_developer ON requests(assigned_developer_id);
CREATE INDEX idx_requests_client ON requests(client_id);
CREATE INDEX idx_histories_request_changed ON request_histories(request_id, changed_at);
CREATE INDEX idx_alerts_target_read ON alerts(target_member_id, is_read);
CREATE INDEX idx_refresh_tokens_member_active
    ON refresh_tokens(member_id, expires_at) WHERE revoked_at IS NULL;
