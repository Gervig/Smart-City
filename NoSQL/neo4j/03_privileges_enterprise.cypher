// Smart City graph database: roles and privileges for Neo4j ENTERPRISE or AuraDB (not Community).
// Run after 02_users.cypher, against the system database:
//   cypher-shell -u neo4j -p <password> -d system -f 03_privileges_enterprise.cypher
//
// The same rules as SQL/users/08_users.sql, with Neo4j's built-in roles where they fit:
//   smartcity_app         read and write nodes and relationships; no schema changes, no user management
//   smartcity_admin       full rights on the database
//   smartcity_readonly    read everything
//   smartcity_restricted  read everything except salaries and login users
//
// NOTE: not tested in this project, because the local Docker image is Neo4j Community. On Community these
// commands fail with "Unsupported administration command".

// Backend: the built-in role "editor" may read and write data but not change constraints or indexes
CREATE ROLE smartcity_app_role IF NOT EXISTS AS COPY OF editor;
GRANT ROLE smartcity_app_role TO smartcity_app;

GRANT ROLE admin TO smartcity_admin;

GRANT ROLE reader TO smartcity_readonly;

// Restricted: a reader that cannot see the salary on WORKS_AT, nor the User nodes (password hashes)
CREATE ROLE smartcity_restricted_role IF NOT EXISTS AS COPY OF reader;
DENY READ {salary} ON GRAPH neo4j RELATIONSHIPS WORKS_AT TO smartcity_restricted_role;
DENY READ {*} ON GRAPH neo4j NODES User TO smartcity_restricted_role;
DENY TRAVERSE ON GRAPH neo4j NODES User TO smartcity_restricted_role;
GRANT ROLE smartcity_restricted_role TO smartcity_restricted;
