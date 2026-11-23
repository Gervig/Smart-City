# Creates the structure of the MongoDB and Neo4j databases in the local Docker containers (docker/docker-compose.yml):
# collections with validators and indexes in MongoDB, constraints and indexes in Neo4j. The data comes later, from
# the migrator. Safe to run again.
#
#   powershell -ExecutionPolicy Bypass -File NoSQL\setup.ps1
#
# Defaults match docker-compose.yml; override with -MongoDatabase / -Neo4jPassword.
param(
    [string]$MongoContainer = "mongo",
    [string]$MongoDatabase = "smart_city",
    [string]$Neo4jContainer = "neo4j",
    [string]$Neo4jUser = "neo4j",
    [string]$Neo4jPassword = "smartcity-dev"
)

$ErrorActionPreference = "Stop"
$here = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host "MongoDB ($MongoContainer / $MongoDatabase)"
docker cp "$here\mongodb\01_collections.js" "${MongoContainer}:/tmp/01_collections.js"
docker exec $MongoContainer mongosh $MongoDatabase --quiet --file /tmp/01_collections.js
if ($LASTEXITCODE -ne 0) { throw "MongoDB setup failed" }

Write-Host "Neo4j ($Neo4jContainer)"
docker cp "$here\neo4j\01_constraints.cypher" "${Neo4jContainer}:/tmp/01_constraints.cypher"
docker exec $Neo4jContainer cypher-shell -u $Neo4jUser -p $Neo4jPassword -f /tmp/01_constraints.cypher
if ($LASTEXITCODE -ne 0) { throw "Neo4j setup failed" }
docker exec $Neo4jContainer cypher-shell -u $Neo4jUser -p $Neo4jPassword --format plain "SHOW CONSTRAINTS YIELD name RETURN count(name) AS constraints"

Write-Host "Done. Fill the databases with the migrator: cd migrator; java -jar target/migrator.jar"
