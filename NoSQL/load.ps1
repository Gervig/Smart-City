# Loads the test data into MongoDB and Neo4j from the dump files that NoSQL\dump.ps1 wrote:
#   NoSQL/mongodb/dump/smart_city.archive.gz  -> MongoDB database smart_city (replaced: --drop)
#   NoSQL/neo4j/dump/neo4j.dump               -> Neo4j database neo4j (replaced: --overwrite-destination)
# The dumps hold the structure too (validators, indexes, constraints), so NoSQL\setup.ps1 is not needed first.
#
#   powershell -ExecutionPolicy Bypass -File NoSQL\load.ps1
#
# Without dump files, the same data comes from PostgreSQL: NoSQL\setup.ps1, then the migrator.
param(
    [string]$MongoContainer = "mongo",
    [string]$MongoDatabase = "smart_city",
    [string]$Neo4jContainer = "neo4j"
)

$ErrorActionPreference = "Stop"
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$mongoArchive = Join-Path $here "mongodb\dump\smart_city.archive.gz"
$neo4jDump = Join-Path $here "neo4j\dump"
if (-not (Test-Path $mongoArchive)) { throw "No MongoDB dump at $mongoArchive (run NoSQL\dump.ps1)" }
if (-not (Test-Path (Join-Path $neo4jDump "neo4j.dump"))) { throw "No Neo4j dump in $neo4jDump (run NoSQL\dump.ps1)" }

Write-Host "MongoDB: restoring $MongoDatabase"
docker cp $mongoArchive "${MongoContainer}:/tmp/smart_city.archive.gz"
docker exec $MongoContainer mongorestore --nsInclude "$MongoDatabase.*" --drop --gzip `
    --archive=/tmp/smart_city.archive.gz --quiet
if ($LASTEXITCODE -ne 0) { throw "mongorestore failed" }

Write-Host "Neo4j: stopping $Neo4jContainer to load the database"
$container = (docker inspect $Neo4jContainer | ConvertFrom-Json)[0]
$image = $container.Config.Image
$data = ($container.Mounts | Where-Object { $_.Destination -eq "/data" }).Source
docker stop $Neo4jContainer | Out-Null
try
{
    docker run --rm -v "${data}:/data" -v "${neo4jDump}:/dumps" $image `
        neo4j-admin database load neo4j --from-path=/dumps --overwrite-destination=true
    if ($LASTEXITCODE -ne 0) { throw "neo4j-admin load failed" }
}
finally
{
    docker start $Neo4jContainer | Out-Null
}

Write-Host "Loaded. Counts:"
docker exec $MongoContainer mongosh $MongoDatabase --quiet --eval `
    "db.getCollectionNames().sort().forEach(c => print('  mongodb ' + c + ': ' + db.getCollection(c).countDocuments()))"
Start-Sleep -Seconds 10   # Neo4j needs a moment to start
docker exec $Neo4jContainer cypher-shell -u neo4j -p smartcity-dev --format plain `
    "MATCH (n) RETURN labels(n)[0] AS label, count(*) AS nodes ORDER BY label"
