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

# Local databases (Docker)

Based on the school setup (https://github.com/dat2Cph/2semDockerSetupLocal). Differences: PostgreSQL is built with `pg_cron` (`db/Dockerfile`), and MongoDB and Neo4j are added for the migrator.

| Service | Address | Login |
|---|---|---|
| PostgreSQL | localhost:5432, database `smart_city` | `postgres` / `postgres` (admin only) |
| pgAdmin | http://localhost:8080 | `admin@ek.dk` / `1234` |
| MongoDB | localhost:27017 | no login (development) |
| Neo4j | http://localhost:7474, bolt on 7687 | `neo4j` / `smartcity-dev` |

## Start

```powershell
cd docker
docker compose up -d --build
```

The first start creates the `smart_city` database and the `pg_cron` extension (`db/init/`). These scripts only run when `docker/data/` is empty. To start over: `docker compose down`, delete `docker/data/`, start again.

## Load the schema, create the users

The repo's `SQL/` folder is mounted in the database container as `/sql`, so the scripts can be run with psql:

```powershell
docker compose exec db psql -U postgres -d smart_city -f /sql/schema/database_schema.sql
```

Start the backend once as `postgres` so Hibernate creates the `users`, `roles` and `user_role` tables, then create the database users:

```powershell
docker compose exec db psql -U postgres -d smart_city -f /sql/users/08_users.sql
```

Then change `DB_USERNAME` and `DB_PASSWORD` in `config.properties` to `smartcity_app` / `app-dev-password`.

While `hibernate.hbm2ddl.auto` is `update` or `create`, also enable the commented `GRANT CREATE` line in the script.

Show the privileges (for the report):

```powershell
docker compose exec db psql -U postgres -d smart_city -f /sql/users/test_privileges.sql
```

## The daily statistics event

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

## Database backup and restore

The project includes PowerShell scripts for creating and restoring PostgreSQL backups.

Backups use `pg_dump` in custom format (`-Fc`) and include a timestamp in the filename. This makes it possible to keep multiple backup versions.

The scripts are located in the project's `scripts/` directory:

| Script | Purpose |
|---|---|
| `backup.ps1` | Creates and validates a timestamped database backup |
| `restore.ps1` | Restores a backup into a separate database |
| `compare.ps1` | Compares row counts between the original and restored databases |

### Create a backup

Make sure Docker Desktop is running and the PostgreSQL container is started.

Run the following command from the project root in PowerShell:

```powershell
.\scripts\backup.ps1
```

The backup is saved in the project's `backups/` directory.

Example filename:

```text
backups/smart_city_20261121_154401.dump
```

The timestamp is generated automatically.

### Restore a backup

A backup can be restored into a separate, empty database for testing.

Run the following command from the project root, replacing the filename with the backup you want to restore:

```powershell
.\scripts\restore.ps1 .\backups\smart_city_20261121_154401.dump smart_city_test
```

The restore script:

1. Checks that the backup file exists.
2. Checks that the target database does not already exist.
3. Creates the target database.
4. Restores the backup into the new database.

The original `smart_city` database is not overwritten.

The `pg_cron` extension and its `cron` schema objects are excluded from the test restore because `pg_cron` is configured for the original database.

If a previous restore attempt failed after creating the target database, remove the incomplete test database before trying again:

```powershell
docker exec db dropdb -U postgres smart_city_test
```

**Warning:** This command permanently deletes `smart_city_test`. Make sure it is only the test database you intend to remove.

### Compare restored data

After restoring the backup, compare the row counts in the original and restored databases:

```powershell
.\scripts\compare.ps1
```

The comparison script checks all base tables in the `public` schema and displays:

- Table name
- Original row count
- Restored row count
- Comparison result (`MATCH`, `MISMATCH` or `MISSING`)

A successful comparison displays:

```text
SUCCESS: All table row counts match!
```

A non-zero exit code is returned if a mismatch is found.

### Backup procedure

Backups are created manually when needed, for example before significant database changes or development work that could affect existing data.

There is no automatic backup schedule configured. Team members should create a backup manually using `backup.ps1` when a backup is required.

Important notes

The backups/ directory is excluded from Git to prevent database backup files from being committed to the repository.

Keep important backups somewhere safe outside the development environment as well.

A successful restore and matching row counts demonstrate that the backup can be restored and that the tables contain the expected number of records.

## Users

| User | Password | Rights |
|---|---|---|
| `smartcity_app` | `app-dev-password` | SELECT, INSERT, UPDATE, DELETE and sequences. No schema changes. Used by the backend |
| `smartcity_admin` | `admin-dev-password` | Everything in the database |
| `smartcity_readonly` | `readonly-dev-password` | SELECT on all tables |
| `smartcity_restricted` | `restricted-dev-password` | SELECT on the domain tables, `job` without `salary`; no users, roles or salary views |

All passwords are development passwords. Change them before any shared or deployed use.
