# 0005. Run synchronization outside any database transaction

## Status

Accepted

## Context

A synchronization interleaves external HTTP calls with database writes: resolve an account, fetch a rank, fetch a page
of match history, import its matches, fetch the next page. The obvious reflex is to annotate the orchestrating service
`@Transactional` so the whole run commits or rolls back as one unit.

That reflex is wrong here for two reasons.

**Henrik calls are slow and unreliable.** A batch over six players fetches many pages under a shared rate limit. Held
inside one transaction, the database connection stays checked out for the entire duration, and a timeout at the last
page throws away every match imported before it.

**The per-season completion flag depends on independent commits.** `player_season_synchronization.complete` is the
mechanism that keeps an interrupted import from leaving a permanent hole in a player's history. It only works if
`complete = false` is committed *before* any match of that season is imported, and `complete = true` is committed
*after* the page that proved the season boundary was itself committed. Joining those writes into one transaction defers
every commit to the end of the batch, and a rollback then erases the `complete = false` row along with the matches -
silently abandoning a season that was being caught up.

## Decision

`DefaultSynchronizationCommandService`, `PlayerSynchronizationService` and `SeasonMatchHistoryWalker` are deliberately
**not** transactional. Each step commits on its own:

- the execution row is persisted before processing starts;
- each player's outcome is recorded independently;
- `SeasonSynchronizationStateService.startSeason` and `markSeasonComplete` each commit alone;
- match import commits per page.

A crash at any point therefore leaves `complete = false` with a partial history, which the next run repairs by
re-walking that season in full. The state `complete = true` with missing pages is unreachable.

Callers must not wrap the walk in a transaction. The constraint is stated in the Javadoc of all three classes, because
it is invisible at the call site and adding `@Transactional` would look like an improvement.

## Consequences

- A partial batch is a normal, recorded outcome rather than a rollback. `PARTIAL` is a first-class synchronization
  status.
- One player's failure isolates: its result row is written as failed and the remaining players are still processed.
- Correctness comes from idempotency rather than atomicity. Re-running an interrupted import is safe because
  `valorant_match.external_match_id` and `uk_player_match_player_match` make a duplicate impossible at the schema
  level - not because the previous attempt was undone.
- There is no single point where the whole run can be rolled back. A bad import is corrected by re-running the
  calculation over stored matches, which is what [0009](0009-recompute-instead-of-accumulate.md) makes possible.
- The weekly rollover is the exception and *is* transactional: it performs no external call, and finalizing the old
  week must not survive a failure to create the new pack.
