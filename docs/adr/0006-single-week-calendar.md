# 0006. Anchor every weekly calculation on one shared calendar

## Status

Accepted

## Context

Five independent things need to agree on what a week is: challenge selection, challenge progress, the count of distinct
days played, ranking recalculation and the Monday rollover. Each of them needs two answers - where does the week start,
and which calendar day does a given instant fall on.

When each computes those answers separately, a single divergence moves a Sunday-night match into the wrong week. The
resulting ranking is wrong in a way nobody can explain, because every component individually looks correct.

The zone is also not one question but two. *When should the rollover job fire* is an operational preference. *Which
week does a match belong to* is a business rule that decides who wins.

## Decision

`WeekCalendar` owns the definition and is the only place that resolves it. A week runs from Monday 00:00 to the
following Monday 00:00 in the configured zone, and is identified throughout the application by that Monday's
`LocalDate`.

Two separate configuration keys reflect the two separate questions:

| Key                   | Default        | Governs                                            |
| --------------------- | -------------- | -------------------------------------------------- |
| `SCHEDULING_ZONE`     | `Europe/Paris` | When the synchronization job fires                  |
| `WEEK_ROLLOVER_ZONE`  | `UTC`          | Every weekly calculation and the rollover schedule   |

Instants remain stored in UTC. Only their calendar interpretation uses the configured zone. `WeekCalendar` is `final`
because its constructor validates its arguments, and an extensible class would let a subclass observe a partially
initialized instance.

## Consequences

- Changing the week zone is one configuration change, not a search for every place that computed a week boundary.
- Every service that needs a week boundary injects `WeekCalendar` rather than calling
  `LocalDate.now()` or `TemporalAdjusters` directly. A service that does its own week arithmetic is a defect.
- Week identifiers are validated: a method taking a `weekStart` rejects anything that is not a Monday, so an off-by-one
  caller fails immediately rather than writing a row into a week that does not exist.
- Tests inject a fixed `Clock` alongside the calendar, which makes weekly behavior deterministic without freezing
  system time.
- Changing `WEEK_ROLLOVER_ZONE` on a live database would reinterpret which week past matches belong to. Finalized weeks
  are immutable and would not be recalculated, so current and historical weeks could disagree about the same match.
  The value is a deployment-time decision, not a tunable.
