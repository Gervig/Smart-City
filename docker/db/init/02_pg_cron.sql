-- pg_cron must be created in the database named by cron.database_name (set in docker-compose.yml).
\connect smart_city
CREATE EXTENSION IF NOT EXISTS pg_cron;
