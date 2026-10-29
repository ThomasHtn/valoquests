# Contributing

This guide is for whoever takes over the project: where everything lives, which rules the code follows
and which tools enforce them. The game rules are the specification: [`docs/GAMEPLAY.md`](docs/GAMEPLAY.md)
holds every constant and the order things happen in, [`docs/CHALLENGES.md`](docs/CHALLENGES.md) and
[`docs/CHALLENGES-CATALOGUE.md`](docs/CHALLENGES-CATALOGUE.md) the challenges, and
[`docs/DESIGN.md`](docs/DESIGN.md) the visual direction. All four are written in French. A change to a
formula, a constant or a screen starts by reading the matching one.

## Up and running

You need Java 25, Node 22 and Docker. Then, the first time:

```bash
make setup
```

And every day:

```bash
make dev
```

That starts PostgreSQL in Docker, the API on http://localhost:8080 and the app on http://localhost:4200.
The backoffice (`/admin/login`) takes the demo key **valoquests-demo-admin-key-0123456789**. Ctrl+C stops
the API and the app, the database keeps your squad.

No match is imported until `backend/.env` holds a real `HENRIK_API_KEY` (free at
https://docs.henrikdev.xyz). With an empty squad the app never calls the Henrik API.

Type `make` alone to see every shortcut. Each one wraps a plain command, so make is a convenience, never
a requirement.

## Try the real thing

The whole app from its Docker images, exactly as a server runs it, on http://localhost:8000:

```bash
make reset up
make e2e
```

The browser tests open every public page on a desktop and a phone, with axe-core on each. They start
from an empty squad, hence the `reset`.

Done:

```bash
make down
```

## The daily loop

1. Read the section of the spec that covers the task.
2. Write a test that fails, then make it pass.
3. Tidy up, then run every check the CI runs:

```bash
make fix
make verify
```

Green here means green on GitHub. Coverage floors only ever go up: 90 % of lines and 70 % of branches on
the backend, and the floors of `frontend/angular.json` on the frontend.

## What lives where

```text
backend/            the API (Spring Boot): Henrik import, campaign replay, challenges, ranking, backoffice
frontend/           the app and its backoffice (Angular), served by nginx, which relays /api
docs/               game rules, challenge catalogue, design direction, deployment contract, README media
tools/tour/         films the README tour and screens from the real app (tools/tour/README.md)
compose.yaml        the whole app on your machine, also started by every delivery
```

The backend and frontend READMEs hold the day-to-day commands and the code map of each side.

## How the game runs

Everything converges on a **replay**: the campaign is never incremented, it is rebuilt from day one out of
its matches and challenges. Four loops feed it:

| Loop            | Cadence      | What happens                                                    |
| --------------- | ------------ | --------------------------------------------------------------- |
| Synchronization | every 5 min  | Henrik matches arrive, stocks rise, the guardian takes damage   |
| Daily tick      | 00:10        | Challenge drawn, campaign replayed                              |
| Weekly rollover | Monday 02:05 | Previous week finalized, new week opened, finished campaign closed |
| Campaign        | 10 weeks     | The rocket is built, the final score freezes                    |

Two invariants hold across the whole system:

- Replay steps stay **idempotent** and keep the order the engine encodes: the base grows, stocks fill,
  the base eats, on Sunday the ship leaves, the rescued arrive after the guardian has struck.
- Day and week boundaries resolve in **Europe/Paris** (`WeekCalendar`, `CALENDAR_ZONE`), and every
  scheduler fires in that same zone: otherwise a day closes on boundaries that did not produce its gains.

A closed campaign is frozen and never replayed again.

## House rules

- The interface speaks French, then English. Everything else is English: code, comments, tests, logs,
  commit messages.
- Small functions, names that explain themselves, no dead code, no magic numbers.
- Something moves to shared only when a second place needs it.
- A game constant lives once, matches `docs/GAMEPLAY.md`, and changes there first.
- Never log the administrator key or the Henrik key.

## Comments

Every declaration gets a one-sentence comment that says why, private ones included:

```ts
/**
 * Last good value of each screen, kept through a failed background reload.
 */
```

The format follows the file:

```text
.java .ts .js .mjs                /** ... */ above every declaration
.html                             <!-- ... --> above each meaningful block
.scss .css                        /* ... */ file header, one line above each rule
.sql                              -- ... above each statement
.properties .env.example .yml     # === section headers, # ... above keys that need it
```

Backend test methods are named by their @DisplayName instead, written as a behaviour sentence. Checkstyle
and ESLint fail on a missing comment.

## Backend rules

- One package per feature, split into controller, dto, entity, repository and service. Shared code goes
  in shared, which depends on no feature.
- Packages only use the tiers below them (see the backend README), and controllers never touch a
  repository. ArchitectureTest checks it.
- A service a controller calls is an interface plus its Default implementation, documented on the
  interface. Internal collaborators (calculators, readers, writers) stay plain classes.
- DTOs are records with @Schema. Controllers carry @Tag, @Operation and @ApiResponse.
- Errors are thrown (ResourceNotFoundException, InvalidRequestException, ConflictException) and only
  GlobalExceptionHandler renders them.
- Constructor injection and typed settings (ApplicationProperties, HenrikApiProperties), never @Value.
- Every Henrik call goes through HenrikRequestExecutor (rate limiter and retries), outside any
  transaction: NonTransactionalGuard fails fast otherwise.
- Every job that writes the match history takes MatchHistoryLock.
- Flyway owns the schema. Never edit an applied migration: add the next V<n>__<subject>.sql.
- A SpotBugs false positive gets a justified @SuppressFBWarnings next to it.

## Frontend rules

- Standalone OnPush components, signals, zoneless. Services use @Service() and inject().
- Imports go core, then shared, then layout, then pages, never upwards, and a page never imports another
  page (the tour excepted). Other folders are reached through the @core, @shared, @layout and @pages
  aliases. ESLint checks it.
- Data comes through httpResource, read with `resourceValue()`, URLs through API_ENDPOINTS.
- One suffix per role: x.ts, x.html, x.scss, x.spec.ts, x.model.ts, x.constants.ts, x.utils.ts, and
  x.fixtures.ts for tests.
- Tailwind for layout only, in the templates. Everything visual goes in the component's .scss, in BEM
  classes on the tokens of `src/styles/`. Read `docs/DESIGN.md` §8 before restyling anything.
- No user-facing literal in a template: every text is a key of `public/i18n/fr.json` and `en.json`.

## Recipes

**A new endpoint**: a DTO, the service interface and its implementation, a documented controller method,
and an API test extending PostgreSqlIntegrationTest. On the frontend, its URL in API_ENDPOINTS and its
type in a .model.ts.

**A new table**: a migration, an entity, a repository, an integration test.

**A new challenge**: the row in `docs/CHALLENGES-CATALOGUE.md` first, then a migration that inserts it
(the catalogue is data, see `V47__hand_written_challenge_catalogue.sql`). A new shape of progress is a new
`ProgressMode` calculator, never controller code.

**Refresh the README tour and screens**: `tools/tour/tour.sh`
([tools/tour/README.md](tools/tour/README.md)).

## Shipping a release

Set the version on both sides:

```bash
make version VERSION=1.1.0
```

Commit, then tag:

```bash
git tag v1.1.0 && git push origin v1.1.0
```

The delivery checks that the tag sits on main and matches both versions, publishes the images 1.1.0 and
1.1, and writes the GitHub release from the commits. Commit messages follow Conventional Commits
(fix(overview): ...), so the notes sort themselves.

## What the CI does

- **A pull request** runs the checks of the side it touches.
- **A push to main** runs both, builds both images, starts them, scans them, drives them with the
  Playwright smoke tests, and only then pushes them to GHCR. A failed commit has no image, so it can
  never be deployed.
- **CodeQL** reads the Java, TypeScript and workflows on every push and once a week.
- **Dependabot** groups the weekly bumps of each ecosystem, base images included.

## Pitfalls

**npm install crashes**: npm 10.9 trips on jsdom's optional canvas dependency. Use npm 11:

```bash
npx npm@11 install
```

**Playwright cannot find its browser**: install it once, from the frontend folder:

```bash
npx playwright install chromium
```

**Port 5432 already taken**: the development database listens on 5433 precisely so a native PostgreSQL
can keep 5432.

**A day looks wrong around midnight**: the browser or the JVM is not in Europe/Paris. Go through
`WeekCalendar` on the backend and `core/campaign/calendar/` on the frontend, never the local zone.

**The image scan fails a delivery**: it names the package. Bump the base image or the Spring Boot managed
version in pom.xml, never silence the scan.

**Henrik answers 429**: expected on a long walk, the executor waits and retries. Never raise
`HENRIK_API_REQUESTS_PER_MINUTE` above what the key allows.
