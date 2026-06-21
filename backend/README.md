# backend

Kotlin + Spring Boot REST API for Stashbox.

- Base package: `io.github.kantpiyush.stashbox`
- Spring Boot 3.4.1, Kotlin 1.9.25, Java 21
- Build tool: Gradle (wrapper included, no local Gradle needed)
- Data: PostgreSQL via Spring Data JPA (Postgres runs locally in Docker)

## 1. Start the database (Docker)

The app needs a PostgreSQL database. Run one in Docker:

```bash
docker run -d \
  --name stashbox-postgres \
  -e POSTGRES_DB=stashbox \
  -e POSTGRES_USER=stashbox \
  -e POSTGRES_PASSWORD=stashbox \
  -p 5432:5432 \
  -v stashbox-pgdata:/var/lib/postgresql/data \
  postgres:16
```

- `-v stashbox-pgdata:...` keeps the data in a named volume, so it survives container/app restarts.
- Stop / start later with `docker stop stashbox-postgres` / `docker start stashbox-postgres` (data persists either way).
- Connection details live in `src/main/resources/application.properties` and match the values above.

## 2. Run the app

```bash
cd backend
./gradlew bootRun
```

The API starts on `http://localhost:8080`. On first run, Hibernate auto-creates the `stash_item` table from the entity (`spring.jpa.hibernate.ddl-auto=update`).

## Try the endpoints (full CRUD)

```bash
# list all items
curl http://localhost:8080/items

# create
curl -X POST http://localhost:8080/items \
  -H "Content-Type: application/json" \
  -d '{"text":"reply to PM","link":"https://slack.com/...","status":"OPEN"}'

# get one by id (404 if missing)
curl http://localhost:8080/items/<id>

# update (keeps id + createdAt; 404 if missing)
curl -X PUT http://localhost:8080/items/<id> \
  -H "Content-Type: application/json" \
  -d '{"text":"replied - done","status":"DONE"}'

# delete (204, or 404 if missing)
curl -i -X DELETE http://localhost:8080/items/<id>
```

Status values: `OPEN` or `DONE`.

## Build and test

```bash
./gradlew build     # compile + run tests + build the jar
./gradlew test      # tests only
```

## Structure (Controller -> Service -> Repository)

- `StashItem.kt` — the JPA entity (`StashItem`, `StashStatus`) plus the `StashItemRequest` DTO for incoming JSON.
- `StashItemRepository.kt` — Spring Data JPA repository (interface); CRUD methods are generated for free.
- `StashItemService.kt` — `StashItemService` interface + `StashItemServiceImpl`. Business logic lives here; controllers depend on the interface.
- `StashItemController.kt` — the REST endpoints (HTTP only), delegates to the service.

## Inspect the database directly

```bash
docker exec -it stashbox-postgres psql -U stashbox -d stashbox -c "SELECT * FROM stash_item;"
```

---

## Docker cheat sheet (study notes)

Docker runs software in isolated **containers**. Here we run PostgreSQL in one so we don't install it on the Mac directly. Key idea: the container is throwaway, but the **data** lives in a named **volume** (`stashbox-pgdata`) on disk, so it survives.

Daily commands:

```bash
docker ps                          # list running containers
docker ps -a                       # include stopped ones
docker start stashbox-postgres     # start the DB (e.g. after a Mac reboot)
docker stop stashbox-postgres      # stop it (data is kept)
docker logs stashbox-postgres      # see the container's logs
docker exec -it stashbox-postgres psql -U stashbox -d stashbox   # open a SQL shell
```

One-time setup (only if the container does not exist yet, e.g. on a fresh machine):
use the `docker run ...` command from "1. Start the database" above.

What survives what:

| Action | Data kept? | Why |
|---|---|---|
| Restart the app (`bootRun`) | yes | data is in Postgres, not the app's memory |
| Restart the Mac | yes | just `docker start stashbox-postgres` again |
| `docker stop` / `docker start` | yes | stopping does not delete the volume |
| `docker rm stashbox-postgres` | yes | the volume still holds the data; a new container reattaches |
| `docker volume rm stashbox-pgdata` | NO | this deletes the actual data files |

Vocabulary: an **image** is the template (`postgres:16`); a **container** is a running copy of it; a **volume** is disk storage that outlives the container.

---

## Phase 2 explained (what we did beyond the entity, and why)

Phase 1 stored items in memory (a map), so data vanished on every app restart. Phase 2 puts a real PostgreSQL database behind the API. Here is everything that changed and the reason for each.

### 1. Dependencies (`build.gradle.kts`)
- `spring-boot-starter-data-jpa` — pulls in JPA + Hibernate (the engine that turns Kotlin objects into SQL and back) and Spring Data (which generates repositories).
- `runtimeOnly("org.postgresql:postgresql")` — the PostgreSQL driver; the actual code that talks to Postgres over the wire. `runtimeOnly` because our code never references it directly, it's only needed when the app runs.
- `kotlin("plugin.jpa")` — a Kotlin compiler plugin. JPA needs every entity to have a no-argument constructor and to be "open" (subclassable). Kotlin classes are final and have no no-arg constructor by default, so this plugin generates them. Without it you get confusing runtime errors.

### 2. Database config (`src/main/resources/application.properties`)
- `spring.datasource.url/username/password` — how Spring connects. The URL `jdbc:postgresql://localhost:5432/stashbox` and the credentials match the `docker run` values.
- `spring.jpa.hibernate.ddl-auto=update` — Hibernate auto-creates/updates tables from the entity classes at startup. Great for learning (no hand-written `CREATE TABLE`). In production you'd use migrations (Flyway/Liquibase) instead.
- `spring.jpa.show-sql=true` + `format_sql=true` — prints the SQL Hibernate runs, so the ORM "magic" is visible while learning. (Dev passwords live here in plain text; fine for local, will move to real secrets on AWS.)

### 3. The entity (`StashItem.kt`)
- Switched from `data class` to a regular `class`. JPA entities should NOT be data classes: a data class's auto-generated `equals`/`hashCode`/`copy` fight with how JPA tracks entity identity (well-known source of bugs). This is the standard Kotlin + JPA practice.
- Fields became `var` (mutable) with default values, because Hibernate creates an empty object and then fills the fields when loading a row.
- Annotations: `@Entity` (this class maps to a table), `@Table(name="stash_item")`, `@Id` (primary key), `@Column(nullable=false)` (DB-level not-null), `@Enumerated(EnumType.STRING)` (store the status enum as readable text `"OPEN"`/`"DONE"`, not a fragile number).
- Consequence: `update()` could no longer use `copy()`. It now sets fields directly on the existing entity; saving an entity with an existing id issues an UPDATE (same row), not an INSERT.

### 4. The repository (`StashItemRepository.kt`)
- An interface extending `JpaRepository<StashItem, String>` (entity type, id type). We write no implementation, Spring Data generates it at runtime and gives us `save`, `findById`, `findAll`, `deleteById`, `existsById`, `count`, etc. for free.
- `findAllByOrderByCreatedAtDesc()` is a "derived query": Spring reads the method NAME and writes the SQL (`... ORDER BY created_at DESC`). The method name is the query.

### 5. The service layer (`StashItemService.kt`)
- Renamed the old in-memory `StashItemStore` into a proper service, and split it into an `interface StashItemService` + `class StashItemServiceImpl` (like an iOS protocol + concrete type). The controller depends on the interface, so a fake can be injected in tests.
- `findById` converts JPA's `Optional<StashItem>` (a Java type) to a Kotlin `null` via `.orElse(null)`, so callers can use the Elvis operator.
- `deleteById` checks `existsById` first so it can report whether a row was actually removed (204 vs 404).
- Layering: **Controller (HTTP) -> Service (logic) -> Repository (DB).** Each layer has one job; this is also what makes the code testable.

### The proof it works
Create an item, restart the app, list again: the item is still there (in Phase 1 it would have been gone). The data lives in Postgres on disk, not in the app's memory.
