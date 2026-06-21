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
