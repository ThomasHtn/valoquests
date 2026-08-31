-- Lets an operator stop a campaign and immediately open a clean one, in the same week.
--
-- `uk_run_first_week_start` made that impossible: the replacement run opens on the current Monday,
-- which the run just stopped already holds. The insert opening a run is written `ON CONFLICT DO
-- NOTHING`, so it did not fail — it silently did nothing and the read that follows handed back the
-- *closed* run as if it had just been opened. The campaign then reported itself as started while
-- nothing was open at all.
--
-- The week was never the invariant worth protecting. Two runs may legitimately share a Monday when
-- one of them was cut short on it; what may never happen is two runs being open at once, which is
-- what every reader assumes when it asks for "the run in progress". That is the constraint this
-- puts in its place, and it arbitrates the same race the old one did: several endpoints open a run
-- lazily and a page fires them in parallel, so the loser of the race still writes nothing instead of
-- failing an ordinary page load.
ALTER TABLE run DROP CONSTRAINT uk_run_first_week_start;

-- The indexed expression is `true` for every row the predicate keeps, so at most one run can have a
-- NULL `closed_at`. Closed runs are not indexed at all and stay free to share anything.
CREATE UNIQUE INDEX uk_run_single_open ON run ((closed_at IS NULL)) WHERE closed_at IS NULL;
