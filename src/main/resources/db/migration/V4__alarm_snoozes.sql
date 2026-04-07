-- Persistent alarm snoozes. Each row mutes a single alarm type
-- ("high", "low", "rise", "fall", "predicted", "timeago") until the
-- given timestamp. Rows are upserted on snooze and lazily ignored when
-- expired; a periodic cleanup is not necessary at this volume.

CREATE TABLE alarm_snoozes (
    type        TEXT PRIMARY KEY,
    snoozed_until TIMESTAMPTZ NOT NULL,
    snoozed_by  TEXT NOT NULL,
    snoozed_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_alarm_snoozes_until ON alarm_snoozes (snoozed_until);
