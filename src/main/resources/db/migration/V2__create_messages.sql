CREATE TABLE messages (
                          id               UUID PRIMARY KEY,
                          inbox_id         UUID         NOT NULL REFERENCES inboxes (id) ON DELETE CASCADE,
                          from_header      VARCHAR(998),
                          subject          VARCHAR(998),
                          text_body        TEXT,
                          html_body        TEXT,
                          attachment_count INT          NOT NULL DEFAULT 0,
                          size_bytes       INT          NOT NULL,
                          received_at      TIMESTAMPTZ  NOT NULL,

                          CONSTRAINT chk_message_size CHECK (size_bytes >= 0),
                          CONSTRAINT chk_message_attachments CHECK (attachment_count >= 0)
);

CREATE INDEX idx_messages_inbox_received
    ON messages (inbox_id, received_at DESC);
