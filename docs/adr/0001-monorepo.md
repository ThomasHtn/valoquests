# 0001. Keep backend and frontend in one repository

## Status

Accepted

## Context

The application is one product with two deployables: a Spring Boot API and an Angular SPA. They share a single REST
contract and change together - adding a filter to the match-history endpoint means touching both sides in the same
change.

The alternative was two repositories with an independent release cadence, which is what a larger team would need in
order to let each side ship on its own schedule.

## Decision

Backend and frontend live in one repository, each in its own top-level directory with its own build, tooling and
documentation. Nothing is shared at build time: no shared package, no generated client, no cross-module import. The
REST API is the only coupling.

```text
valorant-tracker
├── backend     Maven project
├── frontend    npm project
├── scripts     Operational scripts
└── docs        Project-wide documentation
```

## Consequences

- A change spanning both sides is one commit, one diff and one review. The API contract and its consumer cannot drift
  out of sync across repositories.
- Cloning the repository gives a complete, runnable system.
- Continuous integration must scope itself: the backend workflow declares `working-directory: backend` and is triggered
  independently rather than rebuilding everything on every push.
- Tooling stays duplicated on purpose. The backend uses Maven and Checkstyle, the frontend uses npm, ESLint and
  Prettier, and no attempt is made to unify them behind a single task runner - the unification would cost more than it
  saves at this size.
- Should one side ever need an independent release cadence, extracting it means moving a directory and its workflow.
  Nothing else has to be untangled, because nothing else is shared.
