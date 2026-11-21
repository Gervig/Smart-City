# PostgreSQL 16.2 (same version as the school setup) with the pg_cron extension added.
# pg_cron runs SQL on a cron schedule inside the database (used for the daily statistics snapshot).
FROM postgres:16.2

RUN apt-get update \
    && apt-get install -y --no-install-recommends postgresql-16-cron \
    && rm -rf /var/lib/apt/lists/*
