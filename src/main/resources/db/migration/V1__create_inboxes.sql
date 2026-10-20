
CREATE TABLE inboxes (
                         id UUID PRIMARY KEY,
                         email_address VARCHAR(320) NOT NULL UNIQUE,
                         access_token_hash VARCHAR(64) NOT NULL,
                         created_at TIMESTAMPTZ NOT NULL,
                         expires_at TIMESTAMPTZ NOT NULL,
                         status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

                         CONSTRAINT chk_inbox_status
                             CHECK (status IN ('ACTIVE', 'EXPIRED', 'DELETED')),

                         CONSTRAINT chk_inbox_expiry
                             CHECK (expires_at > created_at)
);

CREATE INDEX idx_inboxes_expires_at
    ON inboxes (expires_at);
