// Smart City graph database (Neo4j Community): database users.
// The Neo4j counterpart of SQL/users/08_users.sql, with the same four users.
//
// Run against the system database:
//   cypher-shell -u neo4j -p <password> -d system -f 02_users.cypher
// It can be run again (IF NOT EXISTS). The passwords are development passwords, as in 08_users.sql.
//
// LIMITATION: Neo4j Community has authentication but no authorization. CREATE USER works, but every user has full
// rights: roles and GRANT / DENY are Enterprise features ("Unsupported administration command" in Community). The
// privileges below therefore apply on Neo4j Enterprise or AuraDB, in 03_privileges_enterprise.cypher. On Community
// the restrictions are kept by the application instead (route roles, admin-only salary in JobController).

CREATE USER smartcity_app IF NOT EXISTS SET PASSWORD 'app-dev-password' CHANGE NOT REQUIRED;
CREATE USER smartcity_admin IF NOT EXISTS SET PASSWORD 'admin-dev-password' CHANGE NOT REQUIRED;
CREATE USER smartcity_readonly IF NOT EXISTS SET PASSWORD 'readonly-dev-password' CHANGE NOT REQUIRED;
CREATE USER smartcity_restricted IF NOT EXISTS SET PASSWORD 'restricted-dev-password' CHANGE NOT REQUIRED;
