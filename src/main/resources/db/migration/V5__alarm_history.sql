-- Alarm transition log. Each row records the moment an alarm of a given
-- type starts firing (or transitions level). Cleared alarms are not
-- recorded; the absence of a recent row implies "not firing".
--
-- Used by the dashboard to show a recent alarm timeline and by future
-- analytics to count alarm bursts over a period.

CREATE TABLE alarm_history (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    type        TEXT NOT NULL,
    level       INT NOT NULL,
    title       TEXT NOT NULL,
    message     TEXT
);

CREATE INDEX idx_alarm_history_occurred_at ON alarm_history (occurred_at DESC);
CREATE INDEX idx_alarm_history_type ON alarm_history (type, occurred_at DESC);
