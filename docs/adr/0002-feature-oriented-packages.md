# 0002. Organize backend code by business feature

## Status

Accepted

## Context

The conventional Spring Boot layout groups classes by technical type: `controller`, `service`, `repository`, `entity`,
`dto`. It makes the framework's roles obvious, and it makes a feature invisible: adding a filter to match history means
touching five sibling packages, and nothing in the tree says which classes belong together.

## Decision

Top-level packages are business features. Each owns its controllers, services, repositories, entities, DTOs, models and
exceptions.

```text
io.github.thomashtn.valorant.tracker
├── challenge        Weekly selection, rule parsing, progress calculation
├── henrik           External client, mapping, retry, rate limiting
├── match            Seasons, matches, statistics, idempotent import
├── player           Tracked accounts, profiles, Riot account resolution
├── ranking          Weekly scores, positions, ranking history
├── synchronization  Import orchestration and monitoring
├── week             Week calendar and weekly rollover
└── shared           Security, error handling, auditing, common web types
```

`shared` holds only what genuinely has no feature owner: the security filter chain, the global exception handler, the
paginated response envelope and the auditable entity base class.

## Consequences

- A feature is one directory. Its boundary is visible in the tree, and a reviewer can tell at a glance what a change
  touches.
- Cross-feature dependencies are explicit imports, so a cycle is visible rather than hidden behind a shared `service`
  package.
- `shared` is a standing risk: it is the natural home for anything nobody wants to own, and it becomes a junk drawer
  the moment that is allowed. Something belongs there only when at least two features need it and it encodes no
  business rule.
- Interfaces are introduced only where a second implementation or a test seam justifies one. Several services are
  concrete classes on purpose; a `Default*` class paired with an interface means the seam was needed, not that the
  convention demanded it.
- The layout diverges from what many Spring examples show, so the rule is stated in `backend/CLAUDE.md` and in this
  record rather than left to be inferred.
