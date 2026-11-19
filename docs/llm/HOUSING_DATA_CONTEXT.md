# Smart City housing data: what the LLM needs to know

Purpose: the LLM estimates `capacity` (maximum number of residents) for one housing record at a time.
This file is the reference for the model. It is meant to be loaded as RAG source material in the LLM project
and its field names match what `HousingCapacityContextBuilder` sends in the request `context`.

## 1. What Smart City is

Smart City / World Sim is a simulated city. Citizens live in a `Housing`. A housing sits at a `Location`,
and locations come from OpenStreetMap (OSM), fetched through the Overpass API. Only the capacity is currently
fake: it is generated randomly between 1 and 6. The goal is to replace that with an estimate based on real OSM data.

## 2. What `capacity` means

- `capacity` = the maximum number of citizens that can be assigned to this housing.
- It is a whole number of at least 1 (the API rejects 0 or negative values).
- The simulation fills a housing with citizens until the number of residents equals `capacity`.
- It is NOT the number of rooms, flats or square metres. It is people.

## 3. Fields in the request context

Each request describes exactly one housing. Every line has the form `key: value`.
A value of `unknown` means the data is missing. Missing does not mean zero.

| Field | Type | Meaning | Source |
|---|---|---|---|
| `housingType` | `house` or `apartment` | The only two housing types in Smart City | Smart City |
| `numberOfLevels` | integer | Number of above-ground floors (OSM tag `building:levels`) | OSM |
| `numberOfFlats` | integer | Number of separate dwellings in the building (OSM tag `building:flats`) | OSM |
| `heightMeters` | decimal | Building height in metres (OSM tag `height`) | OSM |
| `location.name` | text | Name of the OSM object | OSM |
| `location.streetAddress` | text | `addr:street` plus `addr:housenumber`, either may be missing | OSM |
| `location.latitude`, `location.longitude` | decimal | WGS84 coordinates | OSM |
| `location.osmType` | `node`, `way` or `relation` | OSM object type | OSM |
| `location.osmId` | integer | OSM object id (with `osmType` it identifies the object) | OSM |

Fields the model must not invent: if `numberOfFlats`, `numberOfLevels` or `heightMeters` is `unknown`,
say so in the reasoning and lower the confidence. OSM coverage of these tags is often incomplete.

## 4. Estimation guidance (project assumptions, not facts)

These are starting rules for the prototype. They are assumptions to be tuned and evaluated, not official statistics.

1. `numberOfFlats` known: capacity is about `numberOfFlats` x 2 persons per flat.
2. `housingType` is `house`: treat it as one dwelling. Use a small household size (the old fake generator used 1 to 6).
3. `housingType` is `apartment`, flats unknown, levels known: estimate flats per level from the levels, state the assumption, confidence `low`.
4. Only `heightMeters` known: derive an approximate number of levels from the height, state the assumption, confidence `low`.
5. Nothing but `housingType` known: give a conservative estimate and confidence `low`.
6. Never return less than 1.

## 5. Answer format

Answer with one JSON object and nothing else:

```json
{"capacity": 8, "confidence": "low", "reasoning": "Apartment with 4 levels, number of flats unknown, assumed 2 flats per level and 2 persons per flat."}
```

- `capacity`: integer, at least 1.
- `confidence`: `low`, `medium` or `high`. `high` only when `numberOfFlats` is known.
- `reasoning`: one or two sentences naming which fields were used and which assumptions were made.

## 6. Invented examples (for demonstration, not real data)

```
housingType: apartment
numberOfLevels: 5
numberOfFlats: 20
heightMeters: unknown
```
Expected: capacity 40, confidence high (flats known, 2 persons per flat).

```
housingType: house
numberOfLevels: 2
numberOfFlats: unknown
heightMeters: 7.5
```
Expected: a small household, for example 4, confidence medium.

```
housingType: apartment
numberOfLevels: unknown
numberOfFlats: unknown
heightMeters: unknown
```
Expected: a conservative estimate, confidence low, reasoning says almost no data is available.

## 7. Known limitations of the data

- Smart City currently attaches housings to random OSM locations that were imported by amenity
  (hospital, school, pharmacy, ...). Building tags like `building:levels` therefore often belong to an amenity object
  and are frequently missing.
- There is no ground truth for real capacity. Estimates can be compared with the old random values, with a
  simple rule-based baseline, or checked by hand, but not against official numbers.
- OSM data is volunteer-maintained and may be outdated.
