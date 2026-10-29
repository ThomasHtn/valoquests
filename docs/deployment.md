# Deploying ValoQuests

ValoQuests ships as two Docker images and a short list of requirements. The server brings the rest: a
domain, HTTPS, the secrets and the backups.
This repository holds no production deployment on purpose: each server writes its own compose (or
chart, or PaaS settings) from this page. The root `compose.yaml` is a working example for your machine.

## The images

```bash
docker pull ghcr.io/thomashtn/valoquests-backend:sha-<commit>
docker pull ghcr.io/thomashtn/valoquests-frontend:sha-<commit>
```

Every push to main that passes the tests, the image scan and the browser tests publishes
**sha-&lt;commit&gt;** and moves **main**. A release adds **x.y.z** and **x.y**. Deploy a sha or a
release: a commit that failed its checks has no image, so it cannot reach production.

Both images run as a regular user, carry their own healthcheck and accept a read-only root filesystem
plus a tmpfs on /tmp. Give the backend 1 GB: its heap takes 60 % of the limit, and it exits cleanly on an OutOfMemoryError so your restart policy
brings a fresh one.

Run **one** backend. The match-history lock is in process, and the schedulers would fire twice.

## The layout

```text
browser --HTTPS--> your proxy --> frontend :8080 --/api--> backend :8080 --> postgres
                                                           backend :8081  actuator, never routed
                                                           backend ------> api.henrikdev.xyz
```

One rule on your proxy: the domain goes to the frontend on port 8080. nginx serves the app, relays the
API and answers a JSON 503 while the backend is down, so every screen shows its error state.

- The backend and the database stay on the stack's private network. The backend needs outbound HTTPS to
  the Henrik API.
- Your proxy sets HSTS and may add a rate limit (an open tab polls the synchronization status once a
  minute). nginx sets every other security header, Content-Security-Policy included. The site stays
  indexable: it has link previews.

## A database of its own

PostgreSQL 17, with a dedicated role and database:

```sql
CREATE ROLE valorant LOGIN PASSWORD 'change-me';
CREATE DATABASE valo_quests OWNER valorant;
```

Flyway creates the tables on the first start and migrates them on every update. Rolling an image back
after a migration needs the dump taken before it.

## Settings

The backend reads its environment. A Compose .env to start from, kept at mode 600 and never committed:

```properties
# Database
DB_URL=jdbc:postgresql://postgres:5432/valo_quests
DB_USERNAME=valorant
DB_PASSWORD=change-me

# Backoffice key, 32 characters or more: `openssl rand -base64 36`. The backend refuses to start without one.
ADMIN_API_KEY=change-me

# Personal HenrikDev key: without a real one, no match is ever imported.
HENRIK_API_KEY=change-me
```

These already have the right default. Leave them alone unless you know why; `backend/.env.example`
explains each of them:

```properties
# Day and week boundaries, and the zone every scheduler fires in: never two different values
CALENDAR_ZONE=Europe/Paris
# Synchronization every five minutes, daily tick 00:10, weekly rollover Monday 02:05, history purge 03:30
STANDARD_SYNC_ENABLED=true
WEEK_ROLLOVER_ENABLED=true
CAMPAIGN_TICK_ENABLED=true
SYNC_HISTORY_PURGE_ENABLED=true
# Henrik pacing: just under the key's limit, with a large budget for 429 answers
HENRIK_API_REQUESTS_PER_MINUTE=28
HENRIK_API_RATE_LIMIT_MAX_ATTEMPTS=25
# Wrong keys allowed per address, then the lockout
ADMIN_RATE_LIMIT_MAX_FAILURES=5
ADMIN_RATE_LIMIT_LOCKOUT_DURATION=PT1M
# Swagger maps every administrative route: keep it off
API_DOCS_ENABLED=false
# Never "framework" behind a proxy: a client could pick its own address and dodge the lockout
FORWARD_HEADERS_STRATEGY=native
```

The frontend reads a single setting. Set it when the frontend sits on a network shared with other
apps, where "backend" may well be somebody else's:

```properties
BACKEND_URL=http://valoquests-backend:8080
```

After the first start, open `/admin/login` and type a wrong key. It should be refused. Then add the
squad and launch the campaign from the backoffice.

## Backups

The database holds everything: the roster, every imported match, the challenges and the campaign. The
campaign itself is rebuilt from matches and challenges on every replay, so the matches are what must
never be lost. Dump it every night, copy the dumps off the machine, and practise a restore now and then.

Dump:

```bash
docker compose exec -T postgres pg_dump -U valorant -d valo_quests --format=custom > valoquests-$(date +%F).dump
```

Restore:

```bash
docker compose stop backend
docker compose exec -T postgres pg_restore -U valorant -d valo_quests --clean --if-exists --no-owner < valoquests-<date>.dump
docker compose start backend
```

## Monitoring

Who to ask how things are going (`valoquests-backend` stands for whatever name your stack gives the
backend):

```text
https://<your-domain>/readyz                             API and database: the public uptime probe
https://<your-domain>/healthz                            proxy, TLS and nginx, without the backend
http://valoquests-backend:8080/livez                     the JVM is alive, even without the database
http://valoquests-backend:8081/actuator/health           the same as readyz, from inside the network
http://valoquests-backend:8081/actuator/info             version and build time of what runs
http://valoquests-backend:8081/actuator/prometheus       metrics: JVM, HTTP, connection pool
```

For Prometheus, from a container on the backend's network:

```yaml
scrape_configs:
  - job_name: valoquests
    metrics_path: /actuator/prometheus
    static_configs:
      - targets: ["valoquests-backend:8081"]
```
