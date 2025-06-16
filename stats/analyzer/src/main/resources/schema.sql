--DROP TABLE IF EXISTS event_similarity;
--DROP TABLE IF EXISTS user_action;

CREATE TABLE IF NOT EXISTS event_similarity (
    event_a BIGINT NOT NULL,
    event_b BIGINT NOT NULL,
    score DOUBLE PRECISION NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (event_a, event_b),
    CONSTRAINT ordered_ids CHECK (event_a < event_b)
);

CREATE TABLE IF NOT EXISTS user_action (
    user_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    action_type VARCHAR(20) NOT NULL,
    weight DOUBLE PRECISION NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (user_id, event_id)
);