# Reclaim — Database (SQL scripts + backup/export)

Everything needed to (re)create the Reclaim PostgreSQL database, and a
Docker-based backup image.

## Files

| File | What it is |
|---|---|
| `schema.sql` | The database schema only — all tables, primary/foreign keys, and indexes (Flyway migration `V1`). |
| `reclaim_full_backup.sql` | **Full backup/export** — schema **+** seed/demo data **+** data fixes, in one restorable script (ordered concatenation of migrations `V1`→`V8`, i.e. exactly what the app applies on boot). |
| `Dockerfile` | Builds a Postgres image with the backup pre-loaded — the DB delivered *as a Docker image*. |
| `docker-compose.yml` | One-command restore into a local Postgres container. |

The canonical, per-change SQL lives with the application as Flyway migrations in
[`../src/main/resources/db/migration`](../src/main/resources/db/migration).

## Restore options

### A. Docker Compose (simplest)
```bash
docker compose -f database/docker-compose.yml up
# Postgres is now on localhost:5432 with the data loaded
psql postgresql://postgres:reclaim@localhost:5432/reclaim -c "\dt"
```

### B. Build the backup image
```bash
docker build -t reclaim-db ./database
docker run --name reclaim-db -e POSTGRES_PASSWORD=reclaim -p 5432:5432 reclaim-db
```

### C. Plain psql (existing Postgres)
```bash
createdb reclaim
psql -d reclaim -f database/reclaim_full_backup.sql
```

## Make a fresh dump from a running instance
```bash
pg_dump "$DATABASE_URL" --no-owner --no-privileges > reclaim_full_backup.sql
```

## Seeded demo accounts
All seeded users share the password `Password123`.

| Role  | Email                    |
|-------|--------------------------|
| Admin | `admin@reclaim.app`      |
| User  | `ama.mensah@ug.edu.gh`   |
| User  | `kofi.boateng@ug.edu.gh` |
| User  | `kwame.asante@ug.edu.gh` |
