# Reclaim — pre-loaded PostgreSQL database image (DB backup as a Docker image)
#
# Building this image produces a Postgres server that already contains the full
# Reclaim schema + seed data. Any .sql placed in /docker-entrypoint-initdb.d is
# executed automatically the first time the container initialises its data dir.
#
#   docker build -t reclaim-db ./database
#   docker run --name reclaim-db -e POSTGRES_PASSWORD=reclaim -p 5432:5432 reclaim-db
#
# Then connect:  psql postgresql://postgres:reclaim@localhost:5432/reclaim
FROM postgres:16-alpine

ENV POSTGRES_DB=reclaim
ENV POSTGRES_USER=postgres
# Override at runtime with -e POSTGRES_PASSWORD=...
ENV POSTGRES_PASSWORD=reclaim

# Auto-loaded on first container start (alphabetical order).
COPY reclaim_full_backup.sql /docker-entrypoint-initdb.d/01_reclaim_full_backup.sql
