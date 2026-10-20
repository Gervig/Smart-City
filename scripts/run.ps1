[CmdletBinding()]
param(
    # Skip starting the PostgreSQL container
    [switch]$NoDb
)

$ErrorActionPreference = "Stop"

# Resolve paths from this script, so it can be launched from any working directory.
$repoRoot = Split-Path -Parent $PSScriptRoot
$venvPath = Join-Path $repoRoot ".venv"
$fastapiExe = Join-Path $venvPath "Scripts\fastapi.exe"
$mainPath = Join-Path $repoRoot "app\main.py"
$envFile = Join-Path $repoRoot ".env"

if (-not (Test-Path $mainPath -PathType Leaf)) {
    throw "Could not find '$mainPath'. Make sure run.ps1 is in the project root, next to the app folder."
}

if (-not (Test-Path $fastapiExe -PathType Leaf)) {
    throw "Could not find '$fastapiExe'. Create the venv and install the packages first: py -m venv .venv; .venv\Scripts\Activate.ps1; pip install -r requirements.txt"
}

if (-not (Test-Path $envFile -PathType Leaf)) {
    Write-Warning ".env was not found in the project root. The API needs DATABASE_URL to start."
}

# Start the database container (does nothing if it is already running).
if (-not $NoDb) {
    $docker = Get-Command docker -ErrorAction SilentlyContinue
    if ($docker -and (Test-Path (Join-Path $repoRoot "docker-compose.yml"))) {
        Write-Host "Starting the database container..." -ForegroundColor Cyan
        Push-Location $repoRoot
        try {
            & docker compose up -d db
            if ($LASTEXITCODE -ne 0) {
                Write-Warning "Could not start the database container. Is Docker Desktop running?"
            }
        }
        finally {
            Pop-Location
        }
    }
    else {
        Write-Warning "Docker or docker-compose.yml not found, skipping the database container."
    }
}

Write-Host "Starting the API..." -ForegroundColor Cyan
Write-Host "Swagger UI: http://127.0.0.1:8000/docs" -ForegroundColor DarkGray
Write-Host "Stop the server with Ctrl+C.`n" -ForegroundColor DarkGray

# Run from the project root so app/main.py and the .env file resolve correctly.
Set-Location $repoRoot
& $fastapiExe dev app/main.py
exit $LASTEXITCODE