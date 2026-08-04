-- The consecutive-victory win streak never drove any gameplay behavior (a new boss is already
-- drawn every week regardless of the previous outcome); it was only ever displayed. Dropped in
-- favor of keeping the weekly boss simple: a different boss each week, HP auto-adjusted by the
-- existing difficulty modifier.
ALTER TABLE weekly_boss_encounter DROP COLUMN win_streak;
