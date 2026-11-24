// Smart City document database (MongoDB): database users and privileges.
// The MongoDB counterpart of SQL/users/08_users.sql, with the same four users and the same idea:
//
//   smartcity_app         the backend: read and write documents, nothing else (no drop, no indexes, no users)
//   smartcity_admin       full rights on the database (dbOwner)
//   smartcity_readonly    read everything (built-in role read)
//   smartcity_restricted  read without sensitive data: no salaries, no login users
//
// Run after 01_collections.js, as a user that may create users (on a database with authentication):
//   mongosh "mongodb://root:<password>@localhost:27017/smart_city?authSource=admin" 02_users.js
// It can be run again: existing roles and users are updated. The passwords are development passwords, as in
// 08_users.sql; change them for a shared or deployed database (on MongoDB Atlas: Database Access).
//
// MongoDB only enforces this when authentication is on (always on Atlas; locally: mongod --auth or the
// MONGO_INITDB_ROOT_USERNAME / PASSWORD variables of the Docker image).

const dbName = db.getName();

function ensureRole(name, privileges, roles) {
  if (db.getRole(name)) {
    db.updateRole(name, { privileges: privileges, roles: roles });
  } else {
    db.createRole({ role: name, privileges: privileges, roles: roles });
  }
}

function ensureUser(name, password, roles) {
  if (db.getUser(name)) {
    db.updateUser(name, { pwd: password, roles: roles });
  } else {
    db.createUser({ user: name, pwd: password, roles: roles });
  }
}

// The restricted user reads citizens through a view without the salary (like the column-level GRANT in 08_users.sql).
// A view is read-only and computed on read, so it is always up to date.
if (!db.getCollectionNames().includes('citizens_restricted')) {
  db.createView('citizens_restricted', 'citizens', [{ $project: { 'jobs.salary': 0 } }]);
}

// Least privilege for the backend: CRUD on documents in this database only. No dropCollection, createIndex,
// collMod (validators) or user management: the structure comes from the setup scripts, not from the application.
ensureRole('smartcityApp', [
  { resource: { db: dbName, collection: '' }, actions: ['find', 'insert', 'update', 'remove'] }
], []);

// Reads everything except the collections with sensitive data: citizens (salaries in jobs) and users (password hashes)
ensureRole('smartcityRestrictedRead', ['locations', 'families', 'housings', 'companies', 'institutions',
  'citizens_restricted'].map(collection => ({ resource: { db: dbName, collection: collection }, actions: ['find'] })), []);

ensureUser('smartcity_app', 'app-dev-password', [{ role: 'smartcityApp', db: dbName }]);
ensureUser('smartcity_admin', 'admin-dev-password', [{ role: 'dbOwner', db: dbName }]);
ensureUser('smartcity_readonly', 'readonly-dev-password', [{ role: 'read', db: dbName }]);
ensureUser('smartcity_restricted', 'restricted-dev-password', [{ role: 'smartcityRestrictedRead', db: dbName }]);

print('Database ' + dbName + ': 4 users, 2 custom roles, view citizens_restricted');
