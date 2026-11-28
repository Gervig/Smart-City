// Smart City graph database (Neo4j): constraints and indexes.
// The Neo4j counterpart of SQL/schema/database_schema.sql. It creates the structure only; the migrator fills it.
//
// Run:  cypher-shell -u neo4j -p <password> -f 01_constraints.cypher
// It can be run again (IF NOT EXISTS).
//
// Neo4j has no tables: a node label or relationship type exists as soon as data uses it. What can be declared up
// front is uniqueness (the primary keys) and indexes. Required-property and type constraints need Neo4j Enterprise;
// this project uses the Community edition, so those rules are kept by the migrator instead.
//
// Model:
//   (:Citizen)-[:MEMBER_OF]->(:Family)
//   (:Citizen)-[:LIVES_IN]->(:Housing)-[:LOCATED_AT]->(:Location)
//   (:Citizen)-[:WORKS_AT {id, title, salary, startDate, endDate, status}]->(:Company|:Institution)
//   (:Citizen)-[:STUDIED_AT {id, startDate, endDate}]->(:Institution)
//   (:Citizen)-[:RELATED_TO {id, type, startDate, endDate}]->(:Citizen)
//   (:Company|:Institution)-[:LOCATED_AT]->(:Location)
//   (:User)-[:HAS_ROLE]->(:Role)

// Primary keys: one uniqueness constraint per node label (each also creates an index)
CREATE CONSTRAINT citizen_id IF NOT EXISTS FOR (n:Citizen) REQUIRE n.id IS UNIQUE;
CREATE CONSTRAINT family_id IF NOT EXISTS FOR (n:Family) REQUIRE n.id IS UNIQUE;
CREATE CONSTRAINT housing_id IF NOT EXISTS FOR (n:Housing) REQUIRE n.id IS UNIQUE;
CREATE CONSTRAINT location_id IF NOT EXISTS FOR (n:Location) REQUIRE n.id IS UNIQUE;
CREATE CONSTRAINT company_id IF NOT EXISTS FOR (n:Company) REQUIRE n.id IS UNIQUE;
CREATE CONSTRAINT institution_id IF NOT EXISTS FOR (n:Institution) REQUIRE n.id IS UNIQUE;
CREATE CONSTRAINT user_username IF NOT EXISTS FOR (n:User) REQUIRE n.username IS UNIQUE;
CREATE CONSTRAINT role_name IF NOT EXISTS FOR (n:Role) REQUIRE n.name IS UNIQUE;
// The CRUD application's id counters (Neo4j has no sequences), one per entity, set by the migrator
CREATE CONSTRAINT sequence_name IF NOT EXISTS FOR (n:Sequence) REQUIRE n.name IS UNIQUE;

// Indexes for common lookups
CREATE INDEX citizen_name IF NOT EXISTS FOR (n:Citizen) ON (n.lastName, n.firstName);
CREATE INDEX related_to_type IF NOT EXISTS FOR ()-[r:RELATED_TO]-() ON (r.type);
CREATE INDEX works_at_status IF NOT EXISTS FOR ()-[r:WORKS_AT]-() ON (r.status);

// Jobs, education records and relationships are relationships; the API finds them by id (/neo4j/jobs/{id})
CREATE INDEX works_at_id IF NOT EXISTS FOR ()-[r:WORKS_AT]-() ON (r.id);
CREATE INDEX studied_at_id IF NOT EXISTS FOR ()-[r:STUDIED_AT]-() ON (r.id);
CREATE INDEX related_to_id IF NOT EXISTS FOR ()-[r:RELATED_TO]-() ON (r.id);
