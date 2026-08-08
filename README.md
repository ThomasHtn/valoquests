<div align="center">

# Valorant Tracker

**A weekly Valorant competition for a group of friends, tracked automatically.**

[![Backend CI](https://github.com/ThomasHtn/valorant-tracker/actions/workflows/backend-ci.yml/badge.svg)](https://github.com/ThomasHtn/valorant-tracker/actions/workflows/backend-ci.yml)

</div>

---

## 1. What is this project?

Valorant Tracker follows a fixed group of seven Valorant players. Several times a day, it downloads their match
history from Riot Games (through a service called the Henrik API), then:

- builds each player's profile: rank, match history, win rate, KDA, headshots, and more;
- picks five weekly challenges, one per difficulty level (from a catalogue of 46), and tracks who completes them;
- pits the whole group against a shared weekly boss (Minor, Standard or Elite): every match played and every
  challenge completed deals damage to its health bar;
- turns completed challenges into points and keeps a live ranking, week after week.

It answers one simple question every week: **who actually had the best week?**

This is a personal, portfolio-style project. It is not designed to scale beyond a small, predefined group of players.

## 2. How the project is organized

The project has two applications that work together:

```text
valorant-tracker
├── backend    The server (API). Talks to the database and to the Henrik API.
└── frontend   The website. Shows player profiles, challenges and rankings.
```

- The **backend** must be running for the **frontend** to display any real data.
- Each application has its own setup guide:
  - [`backend/README.md`](backend/README.md)
  - [`frontend/README.md`](frontend/README.md)

## 3. Technology used (short version)

| Part     | Main technologies                                    |
| -------- | ----------------------------------------------------- |
| Backend  | Java 25, Spring Boot, PostgreSQL, Flyway              |
| Frontend | Angular, TypeScript, Tailwind CSS                     |
| Tests    | JUnit (backend), Vitest (frontend)                    |

See [`backend/README.md`](backend/README.md) and [`frontend/README.md`](frontend/README.md) for full setup details.

## 4. Getting started (quick overview)

Running the whole application means starting the backend and the frontend separately, in two terminals.

1. **Get the code**

   ```bash
   git clone <repository-url>
   cd valorant-tracker
   ```

2. **Start the backend** (API + database) — see [`backend/README.md`](backend/README.md) for full details:

   ```bash
   cd backend
   cp .env.example .env   # then fill in the required values, see backend/README.md
   docker compose up -d   # starts the PostgreSQL database
   ./mvnw spring-boot:run # starts the API on http://localhost:8080
   ```

3. **Start the frontend** (website), in a second terminal — see [`frontend/README.md`](frontend/README.md):

   ```bash
   cd frontend
   npm install
   npm start               # starts the website on http://localhost:4200
   ```

4. Open `http://localhost:4200` in a browser.

## 5. What you can do in the application

| Screen           | What it shows                                                                   |
| ----------------- | -------------------------------------------------------------------------------- |
| Overview          | The current week's challenges, team progress and the live ranking, at a glance   |
| Player list       | Every tracked player, with rank, win rate, KDA and match count                   |
| Player profile    | One player's rank, stats and full match history (filterable by mode and season)  |
| Ranking history   | The ranking of every completed week, most recent first                          |
| Weekly boss       | The current boss's category and health bar, and the damage dealt so far         |
| Rules             | How challenges, damage and the weekly boss battle work                          |

## 6. Common problems

| Problem                                             | Likely cause                                                                |
| ---------------------------------------------------- | ---------------------------------------------------------------------------- |
| The website loads but shows no data / errors         | The backend is not running, or is not reachable on `http://localhost:8080`  |
| The backend fails to start with a database error     | PostgreSQL is not running — check `docker compose ps` in `backend/`         |
| Player data never updates                            | The Henrik API key is missing or invalid — see `backend/README.md`         |
| An admin action returns an authorization error        | The `X-Admin-Key` header is missing or does not match `ADMIN_API_KEY`       |

Module-specific troubleshooting is in [`backend/README.md`](backend/README.md) and
[`frontend/README.md`](frontend/README.md).

## 7. Going further

This README only covers the essentials. For more detail on each part of the project:

| Scope    | Where to look                                |
| -------- | --------------------------------------------- |
| Backend  | [`backend/README.md`](backend/README.md)      |
| Frontend | [`frontend/README.md`](frontend/README.md)    |

---

<div align="center">

Built as a portfolio project, tested as a real application.

</div>
