# backend

Kotlin + Spring Boot REST API for Stashbox.

- Base package: `io.github.kantpiyush.stashbox`
- Spring Boot 3.4.1, Kotlin 1.9.25, Java 21
- Build tool: Gradle (wrapper included, no local Gradle needed)
- Data: PostgreSQL via Spring Data JPA (Postgres runs locally in Docker)

## 0. From scratch on a brand-new Mac

If nothing is installed yet, do these once, in order. (Skip any step where the tool is already present.)

### a. Install Homebrew (the Mac package manager)
Open Terminal and run:
```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```
After it finishes it prints two `echo ... >> ~/.zprofile` + `eval` lines, run those so `brew` is on your PATH. Verify:
```bash
brew --version
```

### b. Install Git (if not already there)
macOS may prompt to install the Xcode Command Line Tools the first time you run `git`. Either accept that, or:
```bash
brew install git
git --version
```

### c. Install a Java 21 JDK
The build targets Java 21 (via a Gradle toolchain). Gradle itself also needs a JDK to run, so this must be installed before `./gradlew` will work.
```bash
brew install --cask temurin@21
java -version          # should report a version 21.x
```
(If `java -version` still shows a different version, the Temurin install registered a JDK 21 that Gradle's toolchain will pick up for the build; that's fine.)

### d. Install Docker Desktop (for the PostgreSQL database)
```bash
brew install --cask docker
```
Then open Docker Desktop once from Applications (or `open -a Docker`) and wait until the whale icon in the menu bar is steady. Verify the daemon is up:
```bash
docker --version
docker info        # should print server info, not an error
```

### e. Install jq (for pretty-printing API responses) — optional but handy
```bash
brew install jq
```

### f. Get the code (clone the private repo)
The repo is private, so you need access. Easiest is the GitHub CLI:
```bash
brew install gh
gh auth login                       # follow the browser prompt to sign in
gh repo clone kant-piyush/stashbox   # clones into ./stashbox
cd stashbox/backend
```
(Alternatively, if SSH keys are set up on the new Mac: `git clone git@github.com:kant-piyush/stashbox.git`.)

Now continue with step 1 below.

---

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

From the `backend/` folder:
```bash
cd backend          # if not already there
./gradlew bootRun
```

The first run downloads Gradle (via the wrapper) and all dependencies, so it takes a few minutes. The API starts on `http://localhost:8080`. On first run, Hibernate auto-creates the `stash_item` table from the entity (`spring.jpa.hibernate.ddl-auto=update`).

Stop the app with `Ctrl+C` in the terminal (or the stop button in IntelliJ).

### Whole sequence at a glance (after the one-time step 0)
```bash
docker start stashbox-postgres      # or the `docker run ...` below if it doesn't exist yet
cd stashbox/backend
./gradlew bootRun                   # API at http://localhost:8080
```

## Try the endpoints (full CRUD)

We pipe responses through `jq` for clean, readable JSON (`-s` makes curl quiet so only the JSON shows). Install jq once with `brew install jq`.

```bash
# list all items
curl -s http://localhost:8080/items | jq

# create
curl -s -X POST http://localhost:8080/items \
  -H "Content-Type: application/json" \
  -d '{"text":"reply to PM","link":"https://slack.com/...","status":"OPEN"}' | jq

# get one by id (404 if missing)
curl -s http://localhost:8080/items/<id> | jq

# update (keeps id + createdAt; 404 if missing)
curl -s -X PUT http://localhost:8080/items/<id> \
  -H "Content-Type: application/json" \
  -d '{"text":"replied - done","status":"DONE"}' | jq

# delete (204 with no body, or 404 if missing) - use -i to see the status line
curl -i -X DELETE http://localhost:8080/items/<id>
```

Status values: `OPEN` or `DONE`. (The DELETE call keeps `-i` and no `jq`, since a successful delete returns no body, just the 204 status.)

## Build and test

```bash
./gradlew build     # compile + run tests + build the jar
./gradlew test      # tests only
```

## Structure (Controller -> Service -> Repository)

- `StashItem.kt` — the JPA entity (`StashItem`, `StashStatus`) plus the `StashItemRequest` DTO for incoming JSON. Has a nullable `summary` field, filled by the AI summarize endpoint.
- `StashItemRepository.kt` — Spring Data JPA repository (interface); CRUD methods are generated for free.
- `StashItemService.kt` — `StashItemService` interface + `StashItemServiceImpl`. Business logic lives here; controllers depend on the interface.
- `StashItemController.kt` — the REST endpoints (HTTP only), delegates to the service.
- `AiSummaryClient.kt` — `AiSummaryClient` interface + `AiSummaryClientImpl`. Calls the Python AI service (Phase 5).

## AI summarize (Phase 5): calling the Python service

The new idea here: the backend makes an **outbound** HTTP call. Until now it only *received* requests; the summarize feature makes it *send* one to the Python AI service (the same way `ai/main.py` calls Ollama). The chain is:

`POST /items/{id}/summarize` -> backend -> Python AI service (`:8000/summarize`) -> Ollama (`:11434`) -> summary -> stored on the item.

```bash
# all three must be running: backend (8080), python ai (8000), ollama (11434)
ID=$(curl -s -X POST http://localhost:8080/items \
  -H "Content-Type: application/json" \
  -d '{"text":"reply to PM about Q3 launch by Friday, need budget approved","status":"OPEN"}' | jq -r '.id')

curl -s -X POST "http://localhost:8080/items/$ID/summarize" | jq   # fills + stores .summary
curl -s "http://localhost:8080/items/$ID" | jq '{id, summary}'      # persists across calls/restarts
```

How it's wired:
- `application.properties` holds `stashbox.ai.base-url=http://localhost:8000` (not hardcoded, so it can point elsewhere later, e.g. an LLM box on another machine).
- `AiSummaryClientImpl` uses Spring's **`RestClient`** (the modern synchronous HTTP client, included with `spring-boot-starter-web`, no extra dependency) to POST to `/summarize`.
- It's annotated **`@Component`**, not `@Service`. `@Component` is the generic "Spring-managed bean" annotation; `@Service`/`@Repository`/`@RestController` are specializations of it that document intent. This class is integration plumbing (an outbound HTTP adapter), not domain logic, so the generic `@Component` fits. Functionally it registers + injects the same way.
- It's behind an `interface` (per our convention) so a fake can be injected in tests, letting the controller be tested without a real LLM.

Two integration gotchas we hit (worth remembering):
- Use Spring's injected `RestClient.Builder`, not the static `RestClient.builder()`, so the JSON serializer is properly configured.
- We force `SimpleClientHttpRequestFactory` on the `RestClient`. The default JDK HttpClient sent a connection-upgrade header that uvicorn rejected ("Invalid HTTP request"); the simple factory sends a clean request.

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

---

## Key concepts: beans, dependency injection, and the layers

These are the core Spring ideas behind the Controller -> Service -> Repository structure. Worth understanding once, because they apply to every Spring class.

### What is a "bean"?
A **bean** is just **an object that Spring creates and manages for you**, instead of you writing `new` / calling the constructor yourself.

On startup, Spring runs a container called the **application context**. It scans your code, creates one shared instance of each managed class, and keeps them in that container. Those managed instances are "beans". When one class needs another, Spring hands over the existing bean rather than you wiring it by hand.

So "bean" = "a Spring-managed object living in the container, available to be injected."

### What is dependency injection (DI)?
Instead of a class creating its own dependencies, it just **declares what it needs in its constructor**, and Spring passes them in. Example:

```kotlin
class StashItemController(private val service: StashItemService)   // "I need a service"
class StashItemServiceImpl(private val repository: StashItemRepository)  // "I need a repository"
```

We never write `StashItemServiceImpl(...)` or `StashItemController(...)` ourselves. Spring sees the constructors, finds the matching beans, and wires the whole chain together. This is **constructor injection**, and it's what makes the code testable (in a test you pass in a fake instead).

### How a class becomes a bean (two mechanisms)
1. **Annotation + component scan** — Spring finds classes marked with a stereotype annotation and registers them:
   - `@RestController` — a web/HTTP class (our controller).
   - `@Service` — a business-logic class (our service impl). Functionally same as `@Component`, but the name documents intent: "this holds business logic."
   - `@Component` — the generic "Spring-managed class"; the others are specializations of it.
   - `@Repository` — a data-access class (adds DB exception translation).
2. **Spring Data repository detection** — a *different* scanner. Any interface that **extends `Repository` / `JpaRepository`** is auto-detected; Spring Data generates the implementation and registers it as a bean. No annotation needed. That's why `StashItemRepository` works without `@Repository` — extending `JpaRepository` is itself the signal.

Note: only the **implementation** (`StashItemServiceImpl`) is annotated, not the `StashItemService` interface. Spring needs a concrete class to instantiate; the interface is just the contract. When the controller asks for the interface, Spring injects the one class that implements it.

### Why `JpaRepository` has `@NoRepositoryBean`
Spring Data's rule is "for every interface extending `Repository`, generate an implementation + bean." But the framework's own base interfaces (`Repository`, `CrudRepository`, `JpaRepository`) also extend `Repository`, and generating beans for *those* makes no sense (they aren't tied to a specific entity). `@NoRepositoryBean` marks an interface as "a base to be extended, skip it." So:
- `JpaRepository` has `@NoRepositoryBean` -> skipped.
- `StashItemRepository` (extends `JpaRepository`, no `@NoRepositoryBean`) -> bean generated.

You'd only add `@NoRepositoryBean` yourself if you made your own shared base repository interface for several entities to extend.

### Comparison to mobile (so it clicks)
The same ideas exist in mobile, just by different names:

| Concept | Spring (backend) | iOS / mobile |
|---|---|---|
| Managed object | **bean** (in the application context) | a singleton/instance in a DI container (e.g. Koin, Hilt, Swinject) or one you pass around |
| Depend on an abstraction | inject the `interface` | inject the **protocol** (iOS) / interface |
| Wiring | Spring auto-wires via annotations + constructors | DI framework (Hilt/Koin/Swinject) or manual constructor passing |
| Layering | Controller -> Service -> Repository | View/ViewModel -> Service/UseCase -> Repository/API |

So Spring's "bean + constructor injection + depend on the interface" is the same instinct as "inject a protocol-typed dependency" in iOS. The big difference: Spring does the wiring **automatically** from annotations, whereas in mobile you often register dependencies more explicitly. The layered structure (UI -> logic -> data) is essentially identical on both sides.
