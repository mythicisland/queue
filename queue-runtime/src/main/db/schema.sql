CREATE TABLE IF NOT EXISTS queues (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    queue_type VARCHAR(255) NOT NULL,
    status INT NOT NULL DEFAULT 0,
    capacity BIGINT NOT NULL DEFAULT 0,
    waiting_countdown_remaining BIGINT NOT NULL DEFAULT 0,
    countdown_remaining BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS queue_players (
    queue_id VARCHAR(36) NOT NULL,
    player_id VARCHAR(36) NOT NULL PRIMARY KEY,
    position INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_queue_players_queue FOREIGN KEY (queue_id) REFERENCES queues(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_queue_players_queue_id ON queue_players(queue_id);