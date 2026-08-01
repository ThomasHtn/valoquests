# 0008. Bound match-history import to the current season

## Status

Accepted

## Context

Henrik returns match history newest-first, ten matches per page, under a rate limit that applies to the API key rather
than to a player or an endpoint. Importing a player's full lifetime history would take hundreds of pages per player,
and re-checking for gaps would take them again.

The application does not need lifetime history. Challenges are weekly, rankings are weekly, and the statistics shown on
a profile are scoped to a season by the UI anyway.

## Decision

A synchronization walks backwards from the newest match and stops when it crosses into an older season. The scope is
the season of the newest match: everything that season holds is imported, and nothing older.

Two rules make the result trustworthy across interruptions and act changes:

1. **A season is only marked complete once the walk proved it reached that season's oldest match**, either by crossing
   into an older season or by exhausting the available history. Until then the stored history may have holes, so the
   next run re-walks the season in full rather than stopping at the first already-stored match.
2. **An older season is only walked when the player already has an unfinished state for it.** That finishes what a
   previous run started, without ever widening the scope: on an empty database no older state exists, so a first run is
   bounded by the current season.

Stop conditions are evaluated on the raw Henrik page, never on the subset actually imported: a page holding nothing but
ignored game modes proves nothing about the history behind it and must not read as a boundary.

A safety limit of 300 pages bounds any single walk, and the reason a walk stopped is persisted on every player result.

## Consequences

- A player's stored history begins at the current act. Match counts read lower than the lifetime totals shown by public
  trackers such as tracker.gg, and no command backfills them. `stop_reason = SEASON_BOUNDARY` is what makes that
  legible to an operator without a database inspection.
- Once an act has been walked in full, later runs stop at the first already-stored match, so the steady-state cost of a
  synchronization is a page or two per player.
- A run truncated by the page limit records `PAGE_LIMIT_REACHED` and deliberately does **not** mark the season complete,
  so the truncation is repaired rather than frozen into a permanent hole.
- The mechanism depends on each step committing independently - see
  [0005](0005-non-transactional-synchronization.md).
- **Known limitation.** Seasons interleaved across a page boundary are not detected: if the last match of a page belongs
  to an older season and the first match of the next page belongs to the current one again, the walk stops early. This
  would require Riot to have tagged an older match with a newer act, while Henrik orders matches strictly by descending
  start instant.
- Combined with the game-mode filter, this decision is why migration `V13` wiped every derived table: the previously
  stored history predated both rules and could not be repaired in place, only rebuilt.
