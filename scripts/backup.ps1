$ErrorActionPreference = "Stop"

# Configuration
$Container = "db"
$Database = "smart_city"
$DbUser = "postgres"

# Find project root
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$BackupDirectory = Join-Path $ProjectRoot "backups"

# Create backup directory if it does not exist
New-Item -ItemType Directory -Force -Path $BackupDirectory | Out-Null

# Generate timestamp
$Timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$FileName = "${Database}_${Timestamp}.dump"
$BackupFile = Join-Path $BackupDirectory $FileName
$ContainerFile = "/tmp/$FileName"

try {
    Write-Host "Creating backup of $Database..."

    # Create backup inside PostgreSQL container
    docker exec $Container pg_dump `
        -U $DbUser `
        -d $Database `
        -Fc `
        -f $ContainerFile

    if ($LASTEXITCODE -ne 0) {
        throw "pg_dump failed."
    }

    # Validate backup
    Write-Host "Validating backup..."

    docker exec $Container pg_restore --list $ContainerFile | Out-Null

    if ($LASTEXITCODE -ne 0) {
        throw "Backup validation failed."
    }

    # Copy backup from container to project
    docker cp "${Container}:$ContainerFile" $BackupFile

    if ($LASTEXITCODE -ne 0) {
        throw "Failed to copy backup from container."
    }

    Write-Host ""
    Write-Host "Backup completed successfully!"
    Write-Host "Backup location: $BackupFile"
}
finally {
    # Remove temporary backup from container
    docker exec $Container rm -f $ContainerFile 2>$null | Out-Null
}
