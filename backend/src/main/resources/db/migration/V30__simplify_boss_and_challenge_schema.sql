-- Removes the columns the boss no longer has, and two redundant classifications.
--
-- Boss difficulty is now a single number. The collective modifier and the carried-over hit points both
-- existed to keep a hardcoded fight winnable; hit points are now measured from what the roster actually
-- produced over the recent closed weeks, which regulates the same drift from real data. Keeping all
-- three would have stacked two feedback loops on one event: a survival lowered the modifier AND the
-- measured reference, easing the following week twice.
--
-- base_hp goes with them. With nothing multiplying or adding to it, it held the same value as
-- effective_hp on every row, and two columns that can never disagree are one column and a liability.
ALTER TABLE weekly_boss_encounter
    DROP COLUMN difficulty_modifier_percent,
    DROP COLUMN carried_over_hp,
    DROP COLUMN base_hp;

-- challenge.rule_type duplicated progress_mode: SINGLE/SUM, DISTINCT/DISTINCT_COUNT,
-- GROUPED/MAX_GROUP, OCCURRENCE/COUNT_MATCHES, STREAK/MAX_STREAK, COMPOSITE/ALL. Only COMPOSITE was
-- ever read, and only to assert that the two agreed with each other. Every new challenge had to set
-- both consistently, with nothing but that assertion to catch a mismatch.
ALTER TABLE challenge DROP COLUMN rule_type;

-- player_match.rank_rating was set to NULL on every import (Henrik's match history v4 does not expose
-- historical RR) and served to the client regardless, so the match history advertised a field that was
-- always empty. The player-level rank_rating, which is populated from the MMR endpoint, is untouched.
ALTER TABLE player_match DROP COLUMN rank_rating;
