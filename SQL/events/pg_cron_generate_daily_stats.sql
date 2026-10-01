SELECT cron.schedule(
    'daily-statistics-snapshot',
    '0 0 * * *',
    'SELECT create_statistics_snapshot();'
);