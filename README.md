# Stashbox

A full-stack personal project. The idea is to stash Slack messages (and other links) that you need to act on later, either a task to do or a message to reply to, and see them synced across mobile, web, and a shared backend.

This is a learn-by-building project. The goal is to grow from senior mobile developer into full-stack by actually implementing every layer of a real product: a shared backend, two mobile clients, a web client, cloud hosting, and CI/CD, taken all the way to published apps.

---

## What it does

I collect a lot of Slack messages that are really to-dos: "I need to do this" or "I need to reply to this later." Stashbox lets me stash those as items, mark their status, and get to them from any device.

Core functionality (built up bit by bit):

- **Capture** an item: a free-text note plus a link (for example a Slack message URL).
- **Status** per item: `TODO` (need to do something), `REPLY_LATER` (need to reply), or `DONE`.
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

---

## Tech stack

| Layer | Technology |
|---|---|
| Mobile shared logic | Kotlin Multiplatform (KMP) |
| Android UI | Jetbrains Compose Multiplatform |
| iOS UI | SwiftUI (plus KMP shared module) |
| Web | Vue 3 plus Vite |
| Backend | Kotlin plus Spring Boot |
| Persistence | Spring Data JPA plus PostgreSQL |
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
├── mobile/             # KMP app (Android + iOS)
├── web/                # Vue 3 + Vite web app
├── .github/workflows/  # GitHub Actions CI/CD pipelines
├── docs/               # Architecture notes, decisions, guides
├── PLAN.md             # Full phased development roadmap
└── README.md
```

I went with a monorepo on purpose. It keeps the whole stack in one place, so the full flow (DB to API to clients to deploy) is easy to see and wire together while learning.

---

## Data model (starting point)

A single `StashItem` entity, designed to grow:

| Field | Type | Notes |
|---|---|---|
| `id` | UUID or Long | Primary key |
| `text` | String | The note or context you jot down |
| `link` | String? | The Slack message (or other) URL |
| `status` | Enum | `TODO`, `REPLY_LATER`, or `DONE` |
| `createdAt` | Timestamp | |
| `userId` | (later) | For per-user data once auth is added |
| `attachmentUrl` | (later) | For S3 image and file uploads |

---

## Development approach

Build a thin vertical slice end-to-end first (DB to API to one client screen to deployed), then thicken it. Learn each new piece by using it in the project rather than studying it in isolation. See [PLAN.md](./PLAN.md) for the full phased roadmap.

---

## Status

Early setup. See [PLAN.md](./PLAN.md) for the roadmap and current phase.
