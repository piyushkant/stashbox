# Stashbox

A full-stack personal project. The idea is to stash Slack messages (and other links) that you need to act on later, either a task to do or a message to reply to, and see them synced across mobile, web, and a shared backend.

This is a learn-by-building project. The goal is to grow from senior mobile developer into full-stack by actually implementing every layer of a real product: a shared backend, two mobile clients, a web client, cloud hosting, and CI/CD, taken all the way to published apps.

---

## What it does

I collect a lot of Slack messages that are really to-dos: "I need to do this" or "I need to reply to this later." Stashbox lets me stash those as items, mark their status, and get to them from any device.

Core functionality (built up bit by bit):

- **Capture** an item: a free-text note plus a link (for example a Slack message URL).
- **Status** per item: `OPEN` (still to act on) or `DONE`. (A `REPLY_LATER` status may be added back later to distinguish "need to reply" from "need to do".)
- **List and filter** items by status across all clients.
- **Mark done or update** an item, and the state syncs to every client.
- **Delete** an item.

One backend, one database, and the Android, iOS, and web apps all read and write the same data.

### Planned extensions (the model is designed to grow into these)

- User accounts and login (auth), with items scoped per user
- Image or file attachments on an item, stored in cloud object storage
- Tags and categories (table relationships)
- Search, sorting, due dates, reminders and notifications

---

## Architecture

```
                     ┌─────────────────────────┐
                     │       Clients            │
                     │                          │
   ┌─────────────┐   │  Android   iOS    Web    │
   │  KMP shared │◄──┼─ (Compose) (SwiftUI) (Vue)│
   │   logic     │   │     │       │        │   │
   └─────────────┘   └─────┼───────┼────────┼───┘
                           │       │        │
                           ▼       ▼        ▼
                        ┌──────────────────────┐
                        │   REST API (HTTPS)    │
                        │  Kotlin + Spring Boot │
                        └──────────┬───────────┘
                                   │
                                   ▼
                        ┌──────────────────────┐
                        │   PostgreSQL (RDS)    │
                        └──────────────────────┘

           Hosted on AWS, CI/CD via GitHub Actions
```

- **Mobile (KMP):** shared Kotlin business logic, with Compose Multiplatform and native UI for Android and iOS.
- **Web:** Vue 3 plus Vite SPA consuming the same REST API.
- **Backend:** Kotlin plus Spring Boot REST API.
- **Database:** PostgreSQL (local via Docker, AWS RDS in the cloud).
- **Hosting:** AWS. Elastic Beanstalk or EC2 for the API, RDS for the database, S3 and CloudFront for the web app and file uploads.
- **CI/CD:** GitHub Actions to build, test, and deploy each part of the stack.

**Patterns & concepts:** the backend uses a layered structure (Controller -> Service -> Repository) with Spring's beans and dependency injection. That, plus how it compares to mobile DI/protocols, is explained in [backend/README.md, "Key concepts"](./backend/README.md#key-concepts-beans-dependency-injection-and-the-layers).

---

## Tech stack

| Layer | Technology |
|---|---|
| Mobile shared logic | Kotlin Multiplatform (KMP), shared logic only |
| Android UI | Jetpack Compose (native) |
| iOS UI | SwiftUI (native, plus KMP shared module) |
| Web | Vue 3 plus Vite |
| Backend | Kotlin plus Spring Boot |
| Persistence | Spring Data JPA plus PostgreSQL |
| AI service | Python plus FastAPI |
| Local LLM | Ollama running Llama (local only, not deployed) |
| Auth (later) | AWS Cognito or JWT |
| Hosting | AWS (Elastic Beanstalk/EC2, RDS, S3, CloudFront) |
| Object storage (later) | AWS S3 (image and file uploads) |
| CI/CD | GitHub Actions |
| Container (local DB) | Docker |

---

## Identity

- **App name:** Stashbox
- **Bundle / Application ID (all platforms):** `io.github.kantpiyush.stashbox`
  - Valid on Android (no hyphens or underscores, each segment starts with a letter), valid on iOS, and used as the backend base package.
  - Based on the GitHub identity so it stays globally unique without owning a domain.

---

## Repository layout (monorepo)

```
stashbox/
├── backend/            # Kotlin + Spring Boot REST API
├── ai/                 # Python + FastAPI service (local LLM via Ollama)
├── web/                # Vue 3 + Vite web app
├── mobile/             # KMP app (Android + iOS)
├── infra/              # AWS setup, hosting, deployment config
├── .github/workflows/  # GitHub Actions CI/CD pipelines
├── docs/               # Architecture notes, decisions, guides
├── PLAN.md             # Full phased development roadmap
└── README.md
```

Each part has its own README with run instructions and study notes. This root README is the map that connects them:

| Part | What it is | Status | Details |
|---|---|---|---|
| [backend](./backend/README.md) | Kotlin + Spring Boot REST API, PostgreSQL via JPA | CRUD + persistence done | [backend/README.md](./backend/README.md) |
| [ai](./ai/README.md) | Python + FastAPI service calling a local LLM (Llama via Ollama) | local LLM running (Phase 3 done); service next | [ai/README.md](./ai/README.md) |
| [web](./web/README.md) | Vue 3 + Vite web client | not started (Phase 8) | [web/README.md](./web/README.md) |
| [mobile](./mobile/README.md) | KMP app, Android (Compose) + iOS (SwiftUI) | not started (Phase 9) | [mobile/README.md](./mobile/README.md) |
| [infra](./infra/README.md) | AWS setup, hosting, deployment config | not started (Phases 6-7) | [infra/README.md](./infra/README.md) |

See [PLAN.md](./PLAN.md) for the full phased roadmap.

I went with a monorepo on purpose. It keeps the whole stack in one place, so the full flow (DB to API to clients to deploy) is easy to see and wire together while learning. Each folder's README stays focused on that part; this root README ties them together.

---

## Data model (starting point)

A single `StashItem` entity, designed to grow:

| Field | Type | Notes |
|---|---|---|
| `id` | UUID or Long | Primary key |
| `text` | String | The note or context you jot down |
| `link` | String? | The Slack message (or other) URL |
| `status` | Enum | `OPEN` or `DONE` (may add `REPLY_LATER` later) |
| `createdAt` | Timestamp | |
| `userId` | (later) | For per-user data once auth is added |
| `attachmentUrl` | (later) | For S3 image and file uploads |

---

## Development approach

Build a thin vertical slice end-to-end first (DB to API to one client screen to deployed), then thicken it. Learn each new piece by using it in the project rather than studying it in isolation. See [PLAN.md](./PLAN.md) for the full phased roadmap.

---

## Status

Early setup. See [PLAN.md](./PLAN.md) for the roadmap and current phase.
