$ErrorActionPreference = "Stop"

# Configuration
$Container = "db"
$DbUser = "postgres"
$OriginalDatabase = "smart_city"
$RestoredDatabase = "smart_city_test"

# Retrieve row counts for all public tables in a database
function Get-TableCounts {
    param(
        [string]$Database
    )

    Write-Host "Reading tables from $Database..."

    $Query = @"
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
AND table_type = 'BASE TABLE'
ORDER BY table_name;
"@

    $TableNames = @(
        & docker exec $Container psql `
            -U $DbUser `
            -d $Database `
            -X -A -t `
            -c $Query
    )

    if ($LASTEXITCODE -ne 0) {
        throw "Could not retrieve tables from $Database."
    }

    $Counts = @{}

    foreach ($Table in $TableNames) {
        $Table = "$Table".Trim()

        if ([string]::IsNullOrWhiteSpace($Table)) {
            continue
        }

        # Escape double quotes in table names
        $SafeTable = $Table.Replace('"', '""')

        $CountQuery = "SELECT COUNT(*) FROM public.`"$SafeTable`";"

        $CountResult = & docker exec $Container psql `
            -U $DbUser `
            -d $Database `
            -X -A -t `
            -c $CountQuery

        if ($LASTEXITCODE -ne 0) {
            throw "Could not count rows in table '$Table' in $Database."
        }

        $CountText = ($CountResult | Out-String).Trim()

        $Counts[$Table] = [long]$CountText
    }

    return ,$Counts
}

try {
    Write-Host "Starting database comparison..."
    Write-Host ""

    # Check that the container is running
    docker inspect $Container | Out-Null

    if ($LASTEXITCODE -ne 0) {
        throw "Container '$Container' is not running."
    }

    # Get row counts from both databases
    $OriginalCounts = Get-TableCounts -Database $OriginalDatabase
    $RestoredCounts = Get-TableCounts -Database $RestoredDatabase

    # Combine table names from both databases
    $AllTables = @(
        @($OriginalCounts.Keys) + @($RestoredCounts.Keys) |
        Sort-Object -Unique
    )

    if ($AllTables.Count -eq 0) {
        throw "No public tables were found in either database."
    }

    # Display comparison
    Write-Host ""
    Write-Host "DATABASE ROW COUNT COMPARISON"
    Write-Host "Original: $OriginalDatabase"
    Write-Host "Restored: $RestoredDatabase"
    Write-Host ""

    Write-Host ("{0,-30} {1,12} {2,12} {3,12}" -f `
        "TABLE", "ORIGINAL", "RESTORED", "RESULT")

    Write-Host ("-" * 70)

    $MismatchCount = 0

    foreach ($Table in $AllTables) {

        $OriginalCount = $null
        $RestoredCount = $null

        $HasOriginal = $OriginalCounts.ContainsKey($Table)
        $HasRestored = $RestoredCounts.ContainsKey($Table)

        if ($HasOriginal) {
            $OriginalCount = $OriginalCounts[$Table]
        }

        if ($HasRestored) {
            $RestoredCount = $RestoredCounts[$Table]
        }

        if (-not $HasOriginal -or -not $HasRestored) {
            $Result = "MISSING"
            $MismatchCount++
        }
        elseif ($OriginalCount -eq $RestoredCount) {
            $Result = "MATCH"
        }
        else {
            $Result = "MISMATCH"
            $MismatchCount++
        }

        $OriginalDisplay = if ($HasOriginal) {
            "$OriginalCount"
        } else {
            "N/A"
        }

        $RestoredDisplay = if ($HasRestored) {
            "$RestoredCount"
        } else {
            "N/A"
        }

        Write-Host ("{0,-30} {1,12} {2,12} {3,12}" -f `
            $Table, $OriginalDisplay, $RestoredDisplay, $Result)
    }

    Write-Host ("-" * 70)
    Write-Host ""

    # Final result
    if ($MismatchCount -eq 0) {
        Write-Host "SUCCESS: All table row counts match!"
    }
    else {
        Write-Host "WARNING: Found $MismatchCount table mismatch(es)."
        exit 1
    }
}
catch {
    Write-Host "ERROR: $_" -ForegroundColor Red
    exit 1
}
