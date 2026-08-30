-- Gives the campaign an explicit lifecycle: a run can now be stopped early by an operator, its score
-- frozen at the day it was stopped rather than waiting for its natural settlement day, and the weekly
-- rollover can be told not to open the next run on its own once the current one closes.

-- `stopped_on` is `NULL` for a run that ran its full course (or is still running); set to the calendar
-- day an operator stopped it early. A run is closed (`closed_at` is set) either way — this column only
-- says *why*, and gives the replay a day to stop at instead of the run's own settlement day, which a
-- stopped run never reaches.
ALTER TABLE run ADD COLUMN stopped_on DATE;

-- Single-row switch: whether the weekly rollover is allowed to open a new run once the current one
-- closes. On by default, preserving the always-on behaviour every run before this one relied on; an
-- operator flips it off to make "no campaign" a real, deliberate state instead of one that only ever
-- lasted between deployments. A dedicated table rather than a flag on `run` itself: the setting
-- outlives any one run, including the gap where none is open at all.
CREATE TABLE campaign_settings (
    id SMALLINT PRIMARY KEY DEFAULT 1,
    auto_renew_enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_campaign_settings_single_row CHECK (id = 1)
);

INSERT INTO campaign_settings (id, auto_renew_enabled) VALUES (1, true);
