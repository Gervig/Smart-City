param(
    [Parameter(Mandatory = $true, Position = 0)]
    [string]$BackupFile,

    [Parameter(Mandatory = $true, Position = 1)]
    [string]$TargetDatabase
)

$ErrorActionPreference = "Stop"

# Configuration
$Container = "db"
$DbUser = "postgres"
$OriginalDatabase = "smart_city"

# Validate backup file
if (-not (Test-Path -LiteralPath $BackupFile -PathType Leaf)) {
    throw "Backup file not found: $BackupFile"
}

$BackupFile = (Resolve-Path -LiteralPath $BackupFile).Path

# Protect original database
if ($TargetDatabase -eq $OriginalDatabase) {
    throw "Cannot restore over the original database."
}

# Validate database name
if ($TargetDatabase -notmatch '^[A-Za-z0-9_]+$') {
    throw "Invalid database name. Use letters, numbers and underscores only."
}

# Generate temporary filenames
$Id = [guid]::NewGuid().ToString("N")
$ContainerBackup = "/tmp/restore_$Id.dump"
$ContainerList = "/tmp/restore_$Id.list"
$LocalList = Join-Path $env:TEMP "restore_$Id.list"

try {
    # Check Docker container
    Write-Host "Checking PostgreSQL container..."

    docker inspect $Container | Out-Null

    if ($LASTEXITCODE -ne 0) {
        throw "Container '$Container' is not running or does not exist."
    }

    # Check whether target database already exists
    Write-Host "Checking target database..."

    $Exists = docker exec $Container psql `
        -U $DbUser `
        -d $OriginalDatabase `
        -tAc "SELECT 1 FROM pg_database WHERE datname = '$TargetDatabase'"

    if ($LASTEXITCODE -ne 0) {
        throw "Could not check PostgreSQL databases."
    }

    $ExistsText = ($Exists | Out-String).Trim()

    if ($ExistsText -eq "1") {
        throw "Database '$TargetDatabase' already exists. Choose another name."
    }

    # Copy backup into container
    Write-Host "Copying backup into container..."

    docker cp $BackupFile "${Container}:$ContainerBackup"

    if ($LASTEXITCODE -ne 0) {
        throw "Could not copy backup into container."
    }

    # Read archive contents
    Write-Host "Reading backup contents..."

    $ArchiveList = docker exec $Container pg_restore --list $ContainerBackup

    if ($LASTEXITCODE -ne 0) {
        throw "Could not read backup archive."
    }

    # Exclude pg_cron extension and ALL objects belonging
    # to the cron schema, including cron.job table data.
    $FilteredList = @(
        $ArchiveList | Where-Object {
            $_ -notmatch '^\s*\d+;\s+\d+\s+\d+\s+EXTENSION\s+-\s+pg_cron\b' -and
            $_ -notmatch '^\s*\d+;\s+\d+\s+\d+\s+COMMENT\s+-\s+EXTENSION\s+pg_cron\b' -and
            $_ -notmatch '\scron\s'
        }
    )

    # Save filtered restore list
    Set-Content -Path $LocalList -Value $FilteredList -Encoding ASCII

    # Copy restore list into container
    docker cp $LocalList "${Container}:$ContainerList"

    if ($LASTEXITCODE -ne 0) {
        throw "Could not copy restore list into container."
    }

    # Create target database
    Write-Host "Creating empty database '$TargetDatabase'..."

    docker exec $Container createdb -U $DbUser $TargetDatabase

    if ($LASTEXITCODE -ne 0) {
        throw "Could not create target database."
    }

    # Restore backup
    Write-Host "Restoring database..."

    docker exec $Container pg_restore `
        -U $DbUser `
        --exit-on-error `
        --no-owner `
        --no-acl `
        --use-list=$ContainerList `
        --dbname=$TargetDatabase `
        $ContainerBackup

    if ($LASTEXITCODE -ne 0) {
        throw "Database restore failed. The target database may be partially restored."
    }

    Write-Host ""
    Write-Host "Restore completed successfully!"
    Write-Host "Target database: $TargetDatabase"
}
finally {
    # Remove temporary local file
    Remove-Item -LiteralPath $LocalList -Force -ErrorAction SilentlyContinue

    # Remove temporary files from container
    docker exec $Container rm -f $ContainerBackup $ContainerList 2>$null |
        Out-Null
}
