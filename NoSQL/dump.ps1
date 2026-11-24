# Writes dump files of the MongoDB and Neo4j databases into the repository:
#   NoSQL/mongodb/dump/smart_city.archive.gz   (mongodump: documents, collection validators and indexes)
#   NoSQL/neo4j/dump/neo4j.dump                (neo4j-admin: nodes, relationships, constraints and indexes)
# Load them again with NoSQL\load.ps1.
#
#   powershell -ExecutionPolicy Bypass -File NoSQL\dump.ps1
#
# Neo4j Community can only dump a stopped database, so the neo4j container is stopped for a few seconds; the dump
# runs in a temporary container from the same image on the same data folder (docker/neo4j-data).
param(
    [string]$MongoContainer = "mongo",
    [string]$MongoDatabase = "smart_city",
    [string]$Neo4jContainer = "neo4j"
)

$ErrorActionPreference = "Stop"
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$mongoDump = Join-Path $here "mongodb\dump"
$neo4jDump = Join-Path $here "neo4j\dump"
New-Item -ItemType Directory -Force $mongoDump, $neo4jDump | Out-Null

Write-Host "MongoDB: dumping $MongoDatabase"
docker exec $MongoContainer mongodump --db $MongoDatabase --gzip --archive=/tmp/smart_city.archive.gz --quiet
if ($LASTEXITCODE -ne 0) { throw "mongodump failed" }
docker cp "${MongoContainer}:/tmp/smart_city.archive.gz" (Join-Path $mongoDump "smart_city.archive.gz")
if ($LASTEXITCODE -ne 0) { throw "copying the MongoDB dump failed" }

Write-Host "Neo4j: stopping $Neo4jContainer to dump the database"
$container = (docker inspect $Neo4jContainer | ConvertFrom-Json)[0]
$image = $container.Config.Image
$data = ($container.Mounts | Where-Object { $_.Destination -eq "/data" }).Source
docker stop $Neo4jContainer | Out-Null
try
{
    docker run --rm -v "${data}:/data" -v "${neo4jDump}:/dumps" $image `
        neo4j-admin database dump neo4j --to-path=/dumps --overwrite-destination=true
    if ($LASTEXITCODE -ne 0) { throw "neo4j-admin dump failed" }
}
finally
{
    docker start $Neo4jContainer | Out-Null
}

Get-ChildItem (Join-Path $mongoDump "smart_city.archive.gz"), (Join-Path $neo4jDump "neo4j.dump") |
    Select-Object Name, @{ Name = "KB"; Expression = { [math]::Round($_.Length / 1KB) } }
