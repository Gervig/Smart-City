# Local databases (Docker)

Based on the school setup (https://github.com/dat2Cph/2semDockerSetupLocal). Differences: PostgreSQL is built with
`pg_cron` (`db/Dockerfile`), and MongoDB and Neo4j are added for the migrator.

| Service | Address | Login |
|---|---|---|
| PostgreSQL | localhost:5432, database `smart_city` | `postgres` / `postgres` (admin only) |
| pgAdmin | http://localhost:8080 | `admin@ek.dk` / `1234` |
| MongoDB | localhost:27017 | no login (development) |
| Neo4j | http://localhost:7474, bolt on 7687 | `neo4j` / `smartcity-dev` |

## Start

```text
cd docker
docker compose up -d --build
```

The first start creates the `smart_city` database and the `pg_cron` extension (`db/init/`). These scripts only run
when `docker/data/` is empty. To start over: `docker compose down`, delete `docker/data/`, start again.

## Load the schema, create the users

The repo's `SQL/` folder is mounted in the database container as `/sql`, so the scripts can be run with psql:

```text
docker compose exec db psql -U postgres -d smart_city -f /sql/schema/database_schema.sql
```

Start the backend once as `postgres` so Hibernate creates the `users`, `roles` and `user_role` tables, then create the
database users:

```text
docker compose exec db psql -U postgres -d smart_city -f /sql/users/08_users.sql
```

Then change `DB_USERNAME` and `DB_PASSWORD` in `config.properties` to `smartcity_app` / `app-dev-password`.
While `hibernate.hbm2ddl.auto` is `update` or `create`, also enable the commented `GRANT CREATE` line in the script.

Show the privileges (for the report):

```text
docker compose exec db psql -U postgres -d smart_city -f /sql/users/test_privileges.sql
```

## The daily statistics event

```text
docker compose exec db psql -U postgres -d smart_city -f "/sql/stored functions/create_statistics_snapshot.sql"
docker compose exec db psql -U postgres -d smart_city -f /sql/events/pg_cron_generate_daily_stats.sql
docker compose exec db psql -U postgres -d smart_city -c "SELECT * FROM cron.job;"
```

Test it without waiting for midnight: `SELECT create_statistics_snapshot(); SELECT * FROM statistics_snapshot;`.

## Users

| User | Password | Rights |
|---|---|---|
| `smartcity_app` | `app-dev-password` | SELECT, INSERT, UPDATE, DELETE and sequences. No schema changes. Used by the backend |
| `smartcity_admin` | `admin-dev-password` | Everything in the database |
| `smartcity_readonly` | `readonly-dev-password` | SELECT on all tables |
| `smartcity_restricted` | `restricted-dev-password` | SELECT on the domain tables, `job` without `salary`; no users, roles or salary views |

All passwords are development passwords. Change them before any shared or deployed use.
