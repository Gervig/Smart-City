// Smart City document database (MongoDB): collections, validators and indexes.
// The MongoDB counterpart of SQL/schema/database_schema.sql. It creates the structure only; the migrator fills it.
//
// Run against the database to set up (the database is created by the first collection):
//   mongosh smart_city 01_collections.js
// It can be run again: existing collections get the current validator (collMod), indexes are created if missing.
//
// The validators mirror the SQL NOT NULL and CHECK constraints: required fields, BSON types and allowed values.
// Fields that are not listed are allowed, so the documents can grow without changing this script.

const id = { bsonType: ['long', 'int'] };
const ids = { bsonType: 'array', items: id };
const text = { bsonType: 'string' };
const date = { bsonType: 'date' };

const location = {
  bsonType: 'object',
  required: ['name'],
  properties: {
    id: id,
    name: text,
    streetAddress: text,
    point: {
      bsonType: 'object',
      required: ['type', 'coordinates'],
      properties: {
        type: { enum: ['Point'] },
        // GeoJSON order: [longitude, latitude]
        coordinates: { bsonType: 'array', minItems: 2, maxItems: 2, items: { bsonType: 'double' } }
      }
    },
    osm: { bsonType: 'object', properties: { id: id, type: text } }
  }
};

const schemas = {
  locations: {
    bsonType: 'object',
    required: ['_id', 'name'],
    properties: Object.assign({ _id: id }, location.properties)
  },

  families: {
    bsonType: 'object',
    required: ['_id', 'familyName', 'memberIds'],
    properties: { _id: id, familyName: text, memberIds: ids }
  },

  housings: {
    bsonType: 'object',
    required: ['_id', 'housingType', 'capacity', 'location', 'residentIds'],
    properties: {
      _id: id,
      housingType: { enum: ['house', 'apartment'] },
      capacity: { bsonType: ['int', 'long'], minimum: 1 },
      numberOfLevels: { bsonType: ['int', 'long'] },
      numberOfFlats: { bsonType: ['int', 'long'] },
      height: { bsonType: 'double' },
      location: location,
      residentIds: ids
    }
  },

  companies: {
    bsonType: 'object',
    required: ['_id', 'name', 'foundedDate', 'status', 'location'],
    properties: {
      _id: id,
      name: text,
      foundedDate: date,
      dissolvedDate: date,
      status: { enum: ['active', 'dissolved'] },
      location: location
    }
  },

  institutions: {
    bsonType: 'object',
    required: ['_id', 'name', 'type', 'location'],
    properties: {
      _id: id,
      name: text,
      type: { enum: ['high_school', 'college', 'hospital'] },
      location: location
    }
  },

  citizens: {
    bsonType: 'object',
    required: ['_id', 'firstName', 'lastName', 'birthDate', 'status', 'family', 'jobs', 'education', 'relationships'],
    properties: {
      _id: id,
      firstName: text,
      lastName: text,
      birthDate: date,
      deathDate: date,
      status: { enum: ['alive', 'deceased'] },
      gender: text,
      family: { bsonType: 'object', required: ['id', 'familyName'], properties: { id: id, familyName: text } },
      housingId: id,
      jobs: {
        bsonType: 'array',
        items: {
          bsonType: 'object',
          required: ['id', 'title', 'salary', 'startDate', 'status', 'employer'],
          properties: {
            id: id,
            title: text,
            salary: { bsonType: 'decimal' },
            startDate: date,
            endDate: date,
            status: { enum: ['active', 'inactive'] },
            employer: {
              bsonType: 'object',
              required: ['type', 'id', 'name'],
              properties: { type: { enum: ['company', 'institution'] }, id: id, name: text }
            }
          }
        }
      },
      education: {
        bsonType: 'array',
        items: {
          bsonType: 'object',
          required: ['id', 'institution', 'startDate'],
          properties: {
            id: id,
            institution: { bsonType: 'object', required: ['id', 'name'], properties: { id: id, name: text, type: text } },
            startDate: date,
            endDate: date
          }
        }
      },
      relationships: {
        bsonType: 'array',
        items: {
          bsonType: 'object',
          required: ['id', 'type', 'citizenId', 'startDate'],
          properties: {
            id: id,
            type: { enum: ['parent_child', 'marriage'] },
            citizenId: id,
            name: text,
            startDate: date,
            endDate: date
          }
        }
      }
    }
  },

  users: {
    bsonType: 'object',
    required: ['_id', 'password', 'roles'],
    properties: { _id: text, password: text, roles: { bsonType: 'array', items: text } }
  }
};

const existing = db.getCollectionNames();
for (const [name, schema] of Object.entries(schemas)) {
  const validator = { $jsonSchema: schema };
  if (existing.includes(name)) {
    db.runCommand({ collMod: name, validator: validator, validationLevel: 'strict', validationAction: 'error' });
  } else {
    db.createCollection(name, { validator: validator, validationLevel: 'strict', validationAction: 'error' });
  }
}

// Creates the index; an index with the same keys under another name (for example made earlier without a name) is
// replaced, because MongoDB does not allow the same keys twice
function ensureIndex(collection, keys, name) {
  db.getCollection(collection).getIndexes()
      .filter(index => index.name !== name && JSON.stringify(index.key) === JSON.stringify(keys))
      .forEach(index => db.getCollection(collection).dropIndex(index.name));
  db.getCollection(collection).createIndex(keys, { name: name });
}

// Indexes for the questions the API asks most
ensureIndex('citizens', { lastName: 1, firstName: 1 }, 'citizens_name');
ensureIndex('citizens', { 'family.id': 1 }, 'citizens_family');
ensureIndex('citizens', { housingId: 1 }, 'citizens_housing');
ensureIndex('citizens', { 'jobs.employer.type': 1, 'jobs.employer.id': 1 }, 'citizens_employer');
ensureIndex('locations', { point: '2dsphere' }, 'locations_point');

print('Database ' + db.getName() + ': ' + Object.keys(schemas).length + ' collections with validators, 5 indexes');