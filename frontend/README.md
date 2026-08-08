# Frontend

The website for Valorant Tracker, built with Angular. It displays the data provided by the
[backend](../backend/README.md): player profiles, weekly challenges and the ranking.

See the [root README](../README.md) for what the whole project does.

## 1. What the frontend does

The frontend is a **read-only website**: it fetches data from the backend API and displays it. It never calculates statistics itself — all numbers come directly from the backend.

It has six screens:

| Screen          | What it shows                                                                       |
|-----------------|-------------------------------------------------------------------------------------|
| Overview        | The current week's challenges, team progress and the live ranking, at a glance      |
| Player list     | Every tracked player, with rank, win rate, KDA and match count                      |
| Player profile  | One player's rank, stats and full match history, filterable by game mode and season |
| Ranking history | The ranking of every completed week, most recent first                              |
| Weekly boss     | The current boss's category and health bar, and the damage dealt so far             |
| Rules           | How challenges, damage and the weekly boss battle work                              |

A "page not found" screen is shown for any other address.

## 2. Requirements

- **Node.js** and **npm** installed. The project is developed with npm `10.9.8` (see `package.json`); a recent Node.js LTS version is expected to work *(exact minimum Node.js version: to confirm)*.
- The [backend](../backend/README.md) running and reachable, so the website has data to display.

## 3. Installation

From the `frontend/` folder:

```bash
npm install
```

## 4. Configuration

The frontend does not need a `.env` file. Its configuration lives in two files under `src/environments/`:

- `environment.development.ts` — used automatically by `npm start` (development);
- `environment.ts` — used by `npm run build` (production).

Both currently point the website to `/api` (a relative address). In development, `proxy.conf.json` forwards any
`/api/...` request to `http://localhost:8080`, so the backend must be running there. In production, the backend is expected to serve the API at the same address as the website, so no extra configuration is needed.

## 5. Running the frontend

Make sure the backend is already running (see [`backend/README.md`](../backend/README.md)), then:

```bash
npm start
```

The website opens at `http://localhost:4200` and reloads automatically when you edit the source code.

## 6. Building for production

```bash
npm run build
```

This produces a ready-to-deploy version of the website in the `dist/frontend/browser/` folder.

## 7. Tests and code quality

```bash
npm test              # runs the automated tests
npm run lint            # checks the code style
npm run format           # automatically fixes formatting
npm run format:check     # checks formatting without changing files
```

Run `npm run format` and `npm run lint` after making changes.

*Note: automated test coverage is currently limited (a single test file exists so far). This is a known, open gap rather than a deliberate choice.*

## 8. Common problems

| Problem                                                      | Likely cause / fix                                                                                                                        |
|--------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------|
| The website loads but every screen shows an error or nothing | The backend is not running, or is not reachable on `http://localhost:8080`.                                                               |
| `npm start` fails right away                                 | Dependencies were not installed — run `npm install` first.                                                                                |
| Changes to `.ts` or `.html` files don't appear               | Check the terminal running `npm start` for a compilation error.                                                                           |
| `npm run build` produces a warning about bundle size         | The production build has a size budget (500 kB warning / 1 MB error). This is expected behavior, not a bug, unless it grows unexpectedly. |

## 9. Going further

This README covers setup and day-to-day commands. For the big picture (what the whole project does, how the
backend and frontend fit together), see the [root README](../README.md). The API this website consumes is
documented through the backend's Swagger UI (see [`backend/README.md`](../backend/README.md)).
