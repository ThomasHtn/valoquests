# 0009. Recompute derived data instead of accumulating it

## Status

Accepted

## Context

Challenge progress and weekly scores are derived from stored matches. Two ways to keep them current:

1. **Accumulate.** When a match is imported, add its kills to every challenge counting kills, increment the streak,
   update the score. Cheap, and the stored total becomes an independent source of truth that can drift from the matches
   that produced it.
2. **Recompute.** After importing, throw away the previous progress and recalculate it from the week's matches.

Accumulation fails in a specific and unfixable way here. An import that is retried, a match whose data is corrected, a
rule that is retuned, a mode that stops being imported - each leaves a stored total nobody can derive from the data,
and no amount of care makes an incremental counter reconcilable after the fact.

## Decision

Every derived value is recomputed from persisted matches.

- `ChallengeRecalculationService` rebuilds the current week's progress for every active player from the matches of that
  week.
- `RankingRecalculationService` rebuilds every weekly score from completed challenge progress.
- Both run after any synchronization that imported something, and both are exposed as administrative commands so an
  operator can trigger them alone.

Determinism is what makes this safe, and it is enforced rather than assumed:

- progress uses `NUMERIC` / `BigDecimal`, never floating point;
- ranking order ends on the player identifier, so genuine ties still produce stable positions;
- weekly challenge selection orders candidates by a hash of the week and the challenge, so the same week yields the same
  pack across restarts.

## Consequences

- A bad import is corrected by re-running the calculation, not by patching stored totals. There is no repair procedure
  to write and no reconciliation script to maintain.
- The same stored matches always produce the same progress, the same points and the same positions. That is what lets
  the release checklist assert reproducibility instead of hoping for it.
- Recalculation is O(matches in the week × active players). At roughly six players and one act of history, that is
  cheap enough to be the normal path. This decision does not scale to a public tracker, and it is not meant to.
- The cost is paid on every synchronization, including ones that changed little.
- Finalized weeks are excluded: once `finalized_at` is stamped, a week is never recalculated again. A match played
  during that week but imported later counts for nothing, which is precisely why the Monday rollover synchronizes and
  recalculates one last time *before* it freezes the week.
