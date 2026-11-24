# Creates the structure of the MongoDB and Neo4j databases in the local Docker containers (docker/docker-compose.yml):
# collections with validators and indexes in MongoDB, constraints and indexes in Neo4j, and the four database users
# (smartcity_app, _admin, _readonly, _restricted). The data comes later, from the migrator. Safe to run again.
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
foreach ($script in "01_collections.js", "02_users.js")
{
    docker cp "$here\mongodb\$script" "${MongoContainer}:/tmp/$script"
    docker exec $MongoContainer mongosh $MongoDatabase --quiet --file "/tmp/$script"
    if ($LASTEXITCODE -ne 0) { throw "MongoDB $script failed" }
}

Write-Host "Neo4j ($Neo4jContainer)"
docker cp "$here\neo4j\01_constraints.cypher" "${Neo4jContainer}:/tmp/01_constraints.cypher"
docker exec $Neo4jContainer cypher-shell -u $Neo4jUser -p $Neo4jPassword -f /tmp/01_constraints.cypher
if ($LASTEXITCODE -ne 0) { throw "Neo4j setup failed" }
# Users live in the system database. Community has no roles: see 03_privileges_enterprise.cypher for AuraDB/Enterprise
docker cp "$here\neo4j\02_users.cypher" "${Neo4jContainer}:/tmp/02_users.cypher"
docker exec $Neo4jContainer cypher-shell -u $Neo4jUser -p $Neo4jPassword -d system -f /tmp/02_users.cypher
if ($LASTEXITCODE -ne 0) { throw "Neo4j users failed" }
docker exec $Neo4jContainer cypher-shell -u $Neo4jUser -p $Neo4jPassword --format plain "SHOW CONSTRAINTS YIELD name RETURN count(name) AS constraints"

Write-Host "Done. Fill the databases with the migrator: cd migrator; java -jar target/migrator.jar"
