# backend

Kotlin + Spring Boot REST API for Stashbox.

- Base package: `io.github.kantpiyush.stashbox`
- Spring Boot 3.4.1, Kotlin 1.9.25, Java 21
- Build tool: Gradle (wrapper included, no local Gradle needed)

## Run it

```bash
cd backend
./gradlew bootRun
```

The API starts on `http://localhost:8080`.

## Try the endpoints

```bash
# list all items (starts empty)
curl http://localhost:8080/items

# get one by id (404 if missing)
curl http://localhost:8080/items/some-id

# create (not implemented yet, returns 501 until you build it)
curl -X POST http://localhost:8080/items \
  -H "Content-Type: application/json" \
  -d '{"text":"reply to PM","link":"https://slack.com/...","status":"OPEN"}'
```

## Build and test

```bash
./gradlew build     # compile + run tests + build the jar
./gradlew test      # tests only
```

## What's here (Phase 1)

- `StashItem.kt` — the data model (`StashItem`, `StashStatus`, `StashItemRequest`).
- `StashItemStore.kt` — a simple in-memory store. Phase 2 swaps this for a real database (Spring Data JPA + Postgres).
- `StashItemController.kt` — the REST endpoints.
  - `GET /items` and `GET /items/{id}` are done as worked examples.
  - `POST`, `PUT`, and `DELETE` are stubs for you to implement (each has a hint comment and returns 501 until done).

## Learn-as-you-build

Open the Spring + Kotlin tutorial (https://spring.io/guides/tutorials/spring-boot-kotlin),
read just the small slice you need for one endpoint, implement that endpoint,
test it with curl, then move to the next. Skip the Mustache/HTML blog parts;
Stashbox is API-only. See the root [PLAN.md](../PLAN.md), Phase 1.
