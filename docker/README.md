# Local databases (Docker)

This is the project's own Docker setup (`docker-compose.yml`). It started from the school's setup, but it is no longer
that one: PostgreSQL is built with `pg_cron` (`db/Dockerfile`), the schema and the database users come from the SQL
scripts in this repository, and MongoDB and Neo4j are added for the migrator.

| Service | Address | Login |
|---|---|---|
| PostgreSQL 16.2 (with `pg_cron`) | localhost:5432, database `smart_city` | `postgres` / `postgres` (admin only) |
| pgAdmin | <http://localhost:8080> | `admin@cphbusiness.dk` / `1234` |
| MongoDB 7 | localhost:27017 | no login (development) |
| Neo4j 5 | <http://localhost:7474>, bolt on 7687 | `neo4j` / `smartcity-dev` |

All passwords are development passwords and are not secrets. Change them, and do not expose the ports, before any
shared or deployed use.

Every service keeps its data in a folder next to `docker-compose.yml` (`data/`, `pgadmin-data/`, `mongo-data/`,
`neo4j-data/`). They are in `docker/.gitignore`, so they are not committed.

## Start and stop

```powershell
cd docker
docker compose up -d --build     # start everything (the first start builds the PostgreSQL image)
docker compose ps                # check that the containers are running
docker compose stop              # stop, keeping the data
docker compose down              # remove the containers, keeping the data folders
```

The first start creates the `smart_city` database and the `pg_cron` extension (`db/init/`). Those scripts only run
when `docker/data/` is empty. **To start over with an empty database:** `docker compose down`, delete `docker/data/`,
start again, and repeat the steps below.

To start only some of the services, name them, for example `docker compose up -d db pgadmin`.

## Load the schema and create the users

The repository's `SQL/` folder is mounted in the database container as `/sql`, so the scripts can be run with psql.
Run them from the `docker` folder, in this order.

1. **The schema** (all tables, including `users`, `roles` and `user_role`, plus functions, triggers and views):
   ```powershell
   docker compose exec db psql -U postgres -d smart_city -f /sql/schema/database_schema.sql
   ```
   The schema comes from this script, not from Hibernate. The backend only checks it on startup
   (`hibernate.hbm2ddl.auto` is `validate`), so the backend reports `missing table` if this step was skipped.
2. **The database users and their privileges:**
   ```powershell
   docker compose exec db psql -U postgres -d smart_city -f /sql/users/08_users.sql
   docker compose exec db psql -U postgres -d smart_city -f /sql/audit/09_citizen_status_audit.sql
   ```
3. **Point the backend at the application user.** In `src/main/resources/config.properties` set
   `DB_USERNAME=smartcity_app` and `DB_PASSWORD=app-dev-password` (and `DB_NAME=smart_city`). Start the backend; it
   creates the roles, the `admin` user and the test users on its first start.

Show the privileges of each user (for the report):

```powershell
docker compose exec db psql -U postgres -d smart_city -f /sql/users/test_privileges.sql
```

## Users

| User | Password | Rights |
|---|---|---|
| `smartcity_app` | `app-dev-password` | SELECT, INSERT, UPDATE, DELETE and sequences. No schema changes. Used by the backend |
| `smartcity_admin` | `admin-dev-password` | Everything in the database |
| `smartcity_readonly` | `readonly-dev-password` | SELECT on all tables |
| `smartcity_restricted` | `restricted-dev-password` | SELECT on the domain tables, `job` without `salary`; no users, roles or salary views |

## The daily statistics event

`pg_cron` runs `create_statistics_snapshot()` every day at 00:00 (pg_cron uses UTC unless configured otherwise). Install the function and schedule it:

```powershell
docker compose exec db psql -U postgres -d smart_city -f "/sql/stored functions/create_statistics_snapshot.sql"
docker compose exec db psql -U postgres -d smart_city -f /sql/events/pg_cron_generate_daily_stats.sql
docker compose exec db psql -U postgres -d smart_city -c "SELECT * FROM cron.job;"
```

Test it without waiting for midnight:

```sql
SELECT create_statistics_snapshot();
SELECT * FROM statistics_snapshot;
```

## MongoDB and Neo4j (for the migrator)

MongoDB and Neo4j start with the other services but are empty. The migrator fills them from PostgreSQL.

<!-- TODO(migrator): fill in when the migrator documentation is added, keeping this order:
1. Create the structure (collections with validators, constraints, indexes): the command for NoSQL/setup.ps1.
2. Run the migrator: the commands from the migrator folder, and the exit codes.
3. Look at the result: the Neo4j Browser query and the mongosh command.
4. The settings (environment variables) and their defaults.
Link to the Migration section of the main README. -->

**Structure:** _to be added_

**Run the migration:** _to be added_

**Look at the result:** _to be added_

## Database backup and restore

The project includes PowerShell scripts for creating and restoring PostgreSQL backups. Backups use `pg_dump` in custom
format (`-Fc`) with a timestamp in the filename, so several versions can be kept.

The scripts are in the project's `scripts/` folder and are run from the project root:

| Script | Purpose |
|---|---|
| `backup.ps1` | Creates and validates a timestamped database backup |
| `restore.ps1` | Restores a backup into a separate database |
| `compare.ps1` | Compares row counts between the original and restored databases |

### Create a backup

Docker Desktop must be running and the `db` container started.

```powershell
.\scripts\backup.ps1
```

The backup is saved in the `backups/` folder, for example `backups/smart_city_20261121_154401.dump`. The timestamp is
generated automatically. `backups/` is excluded from Git, so a dump is never committed.

### Restore a backup

A backup is restored into a separate, empty database for testing. Replace the filename with the backup you want:

```powershell
.\scripts\restore.ps1 .\backups\smart_city_20261121_154401.dump smart_city_test
```

The restore script checks that the backup file exists and that the target database does not already exist, creates
the target database, and restores the backup into it. The original `smart_city` database is not overwritten. The
`pg_cron` extension and its `cron` objects are left out of the test restore, because `pg_cron` is configured for the
original database only.

If a restore failed after creating the target database, remove the incomplete test database before trying again:

```powershell
docker exec db dropdb -U postgres smart_city_test
```

**Warning:** this permanently deletes `smart_city_test`. Make sure it is the test database you mean to remove.

### Compare restored data

```powershell
.\scripts\compare.ps1
```

The comparison checks every base table in the `public` schema and shows the table name, the original and restored row
counts, and the result (`MATCH`, `MISMATCH` or `MISSING`). A successful comparison ends with
`SUCCESS: All table row counts match!`. A non-zero exit code is returned if anything does not match.

### Backup procedure

- Backups are created manually when needed, for example before a significant database change. There is no automatic
  schedule.
- Keep important backups somewhere safe outside the development environment as well.
- A successful restore with matching row counts shows that the backup can be restored and that the tables hold the
  expected number of rows.

## Troubleshooting

| Problem | Check |
|---|---|
| `port is already allocated` or the database does not start | Another PostgreSQL (or a second copy of this setup) is using port 5432. Stop it, or stop the old containers with `docker compose down`. |
| `container name "/db" is already in use` | An old container with that name exists. Run `docker compose down` (or `docker rm -f db`) and start again. |
| Backend says `missing table [...]` | The schema was not loaded. Run `database_schema.sql` as described above. |
| Backend says `password authentication failed` | `DB_USERNAME` and `DB_PASSWORD` in `config.properties` do not match a user from `08_users.sql` (or `postgres` / `postgres`). |
| Changed `db/init` or the passwords but nothing happened | The init scripts and `POSTGRES_PASSWORD` only apply to an empty `docker/data/`. Remove the data folder to start over. |
| pgAdmin cannot connect to the database | Register the server with host `db` (the container name, not `localhost`), port 5432, user `postgres`. |
