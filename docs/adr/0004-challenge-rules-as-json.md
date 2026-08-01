# 0004. Store challenge rules as versioned JSON in PostgreSQL

## Status

Accepted

## Context

The catalogue holds 62 challenges spanning sums, occurrence counts, distinct values, grouped maximums, composite
objectives, ratios and consecutive streaks. Two shapes were available:

1. **One class per challenge.** Fully typed, fully testable, and 62 classes that differ only by a metric and a number.
   Adding a challenge means writing and deploying code.
2. **A rule interpreted at runtime.** One calculator per *mode* rather than per challenge, and a challenge becomes a
   row.

## Decision

A challenge's rule is a JSONB array of typed conditions stored in `challenge.conditions_json`, alongside a
`progress_mode` that selects the calculator and a `schema_version` that identifies the payload format.

```json
[{ "metric": "KILLS", "operator": "GTE", "target": 180, "gameMode": "COMPETITIVE" }]
```

The payload is parsed into a `ChallengeCondition` record whose components are enums, not strings, so an unknown metric
or game mode fails at parse time rather than producing silently wrong progress. Seven calculators cover every mode, and
a registry maps a mode to its calculator.

The catalogue is reference data owned by Flyway migrations. There is no administrative endpoint to create or edit a
challenge.

## Consequences

- Adding a challenge is a migration row, not a class. Retuning a target is a one-line change.
- A rule the calculators cannot execute would be a runtime failure during selection, so
  `ChallengeCatalogueCompatibilityTest` parses every production definition and asserts that the registry supports its
  mode. An unsupported catalogue change fails the build.
- Selection additionally filters on `calculatorRegistry.supports(...)`, so an unsupported challenge is skipped rather
  than drawn into a pack where it would sit permanently at zero.
- `JSONB` rather than `TEXT` keeps the payload queryable. Migration `V14` deleted every challenge filtered on a retired
  game mode by matching on the condition array itself, so a challenge added later with the same filter is caught by the
  same rule instead of surviving a hand-maintained list of codes.
- Rule changes are not hot-deployable. That is deliberate: challenge results feed rankings, and a rule editable at
  runtime would make a historical result impossible to justify.
- `schema_version` exists so a future payload format can coexist with the current one during a migration rather than
  requiring a big-bang rewrite of the catalogue.
