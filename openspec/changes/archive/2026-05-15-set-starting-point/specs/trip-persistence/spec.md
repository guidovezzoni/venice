## MODIFIED Requirements

### Requirement: Room database exists with trips table
The system SHALL provide a Room database (`AppDatabase`) containing a `trips` table and a `stops` table. The database SHALL be a singleton accessible via the application context. The database version SHALL be `2`, with `MIGRATION_1_2` registered via `addMigrations()`. The database SHALL expose a `stopDao()` abstract method.

#### Scenario: Database initialisation
- **WHEN** the application starts
- **THEN** the Room database is initialised with both `trips` and `stops` tables available for read/write operations

#### Scenario: Migration from version 1 to 2
- **WHEN** the app is upgraded from database version 1 to version 2
- **THEN** `MIGRATION_1_2` creates the `stops` table and its `index_stops_tripId` index without data loss in the `trips` table

## ADDED Requirements

### Requirement: MIGRATION_1_2 creates stops table
`MIGRATION_1_2` SHALL be a `Migration(1, 2)` that executes SQL to create the `stops` table with columns (`id TEXT NOT NULL PRIMARY KEY`, `tripId TEXT NOT NULL`, `placeName TEXT NOT NULL`, `latitude REAL NOT NULL`, `longitude REAL NOT NULL`, `order INTEGER NOT NULL`, `status TEXT NOT NULL`) and a foreign key on `tripId` referencing `trips(id)` with `ON DELETE CASCADE`. It SHALL also create the `index_stops_tripId` index.

#### Scenario: Migration SQL creates correct schema
- **WHEN** `MIGRATION_1_2.migrate` is executed
- **THEN** the `stops` table exists with all required columns, constraints, and the `index_stops_tripId` index

### Requirement: DatabaseModule provides StopDao
`DatabaseModule` SHALL provide a `StopDao` instance by calling `appDatabase.stopDao()`.

#### Scenario: StopDao is injectable
- **WHEN** a class requests `StopDao` via dependency injection
- **THEN** Hilt provides the instance from `AppDatabase.stopDao()`

### Requirement: RepositoryModule binds StopRepository
`RepositoryModule` SHALL bind `StopRepositoryImpl` to `StopRepository`.

#### Scenario: StopRepository is injectable
- **WHEN** a class requests `StopRepository` via dependency injection
- **THEN** Hilt provides an instance of `StopRepositoryImpl`
