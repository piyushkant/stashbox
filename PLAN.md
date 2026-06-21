# Stashbox — Development Plan

A full-stack learning roadmap, ordered so each phase produces something working and teaches the next. Built by a senior mobile developer transitioning to full-stack; mobile (KMP) is the area of strength, while Spring Boot, Vue, AWS, and GitHub Actions are the new territory being learned.

## Guiding principles

- **Build a thin vertical slice end-to-end first**, then thicken it. Get one tiny feature working DB → API → client screen → deployed, then repeat.
- **Learn by using, not by reading.** Watch just enough of the AWS course (Udemy: *Ultimate AWS Certified Developer Associate 2026 DVA-C02*), then apply each service in this project.
- **Backend first**, because the app and web need something real to talk to.
- **CI/CD comes after a manual deploy works** — you cannot automate a deploy you do not yet understand.
- **No rush.** The web client is the lowest priority.

## Product recap

Stashbox: stash Slack messages (or links) that are tasks or replies-to-do-later. One backend + DB, three clients (Android, iOS, web) sharing the same data. Item status: `TODO` / `REPLY_LATER` / `DONE`. See [README.md](./README.md) for architecture and tech stack.

- **App name:** Stashbox
- **Bundle/App ID (all platforms):** `io.github.kantpiyush.stashbox`
- **Repo:** private monorepo `kant-piyush/stashbox`

---

## Phase 0 — Foundations (ongoing, alongside the AWS course)

- [ ] Continue the AWS DVA-C02 course, but **use each service as you learn it** instead of binge-watching.
  - Priority sections early: IAM, EC2, S3, RDS, Elastic Beanstalk / ECS. Later: Lambda, API Gateway, Cognito.
- [x] GitHub account & private monorepo created.
- [ ] AWS free-tier account set up.
- [ ] AWS CLI installed and configured on the Mac (`aws configure`).
- [ ] Docker installed locally (for running Postgres).

---

## Phase 1 — Backend skeleton (Kotlin + Spring Boot)

Goal: understand the Spring Boot request flow. Should feel familiar as a Kotlin developer.

- [ ] Generate a Spring Boot project (Spring Initializr): Kotlin, Web, base package `io.github.kantpiyush.stashbox`.
- [ ] Define a `StashItem` model: `id`, `text`, `link`, `status` (`TODO`/`REPLY_LATER`/`DONE`), `createdAt`.
- [ ] Build a `StashItemController` with CRUD endpoints backed by an **in-memory list** (no DB yet):
  - `GET /items`, `GET /items/{id}`, `POST /items`, `PUT /items/{id}`, `DELETE /items/{id}`.
- [ ] Run locally, exercise every endpoint with curl / Postman.

**Milestone:** a working REST API in memory.

---

## Phase 2 — Persistence (PostgreSQL)

Goal: a real database behind the API.

- [ ] Run Postgres locally via Docker.
- [ ] Add Spring Data JPA + the Postgres driver.
- [ ] Make `StashItem` an `@Entity`; create a `StashItemRepository`.
- [ ] Swap the in-memory list for the repository.
- [ ] Verify data survives an app restart.

**Milestone:** DB-backed API running locally.

---

## Phase 3 — Deploy backend to AWS (first real cloud milestone)

Goal: the API reachable at a public URL. This is where the AWS course pays off.

- [ ] Provision a PostgreSQL database on **AWS RDS**.
- [ ] Deploy the API — start with **Elastic Beanstalk** (it hides most infra); EC2 + RDS as the alternative/next step.
- [ ] Configure DB connection via environment variables / secrets (not hardcoded).
- [ ] Hit the live public URL from curl / browser.

**Milestone:** Stashbox API live on the internet.

---

## Phase 4 — CI/CD with GitHub Actions (backend) ← *Actions enters here*

Goal: push code → it builds, tests, and deploys automatically. First non-Bitrise pipeline.

- [ ] **4a — Build & test:** workflow on every push that runs `./gradlew test` and builds the jar.
  - Learn: workflows, jobs, steps, triggers (`on: push`), Gradle caching.
- [ ] **4b — Deploy:** extend the workflow to deploy the jar to AWS on merge to `main`.
  - Learn: GitHub Secrets (AWS keys), environments, deploy steps.

**Milestone:** push to `main` auto-deploys the backend.

---

## Phase 5 — KMP mobile app (home turf)

Goal: connect Android + iOS to the live API; use the familiar mobile layer to learn the AWS/auth side.

- [ ] Create the KMP project under `mobile/`, application ID `io.github.kantpiyush.stashbox`.
- [ ] Shared networking layer calling the Stashbox API.
- [ ] List screen (all items), add screen, mark-done toggle, delete.
- [ ] **Optimistic UI** for mark-done; confirm it syncs across devices.
- [ ] **5a — Android CI:** GitHub Actions builds the APK/AAB (matrix builds, artifact upload, Android SDK setup).
- [ ] **5b — iOS CI:** GitHub Actions builds on a `macos-latest` runner with **manual code signing** — the biggest jump from Bitrise (no Workflow Editor doing signing for you).

**Milestone:** both mobile apps talk to the live API; both build in CI.

---

## Phase 6 — Vue web client (lowest priority)

Goal: a third client on the same API; more AWS practice.

- [ ] Vue 3 + Vite SPA under `web/` consuming the Stashbox API.
- [ ] List / add / mark-done / delete UI.
- [ ] Deploy to **S3 + CloudFront**.
- [ ] GitHub Actions job: Node setup, build, `aws s3 sync`, CloudFront cache invalidation.

**Milestone:** add an item on the phone, refresh the website, it's there.

---

## Phase 7 — Polish & publish (full lifecycle finish)

Goal: complete the dev → live lifecycle.

- [ ] **Auth** via AWS Cognito (or JWT): login, items scoped per user, protected endpoints — serves all three clients.
- [ ] **Image/file attachments**: upload to S3 via presigned URLs; display across Android/iOS/Vue.
- [ ] Publish the Android app to the **Google Play Store**.
- [ ] Publish the iOS app to the **Apple App Store** (check display-name uniqueness; add a subtitle if needed).

**Milestone:** Stashbox live on both stores — full lifecycle complete.

---

## CI/CD learning ladder (easy → hard)

| Stage | What you automate | New GitHub Actions concept |
|---|---|---|
| 4a | Backend: test + build | workflows, jobs, triggers, caching |
| 4b | Backend: deploy to AWS | secrets, environments, deploy |
| 5a | Android: build APK/AAB | matrix builds, artifacts, SDK setup |
| 5b | iOS: build on macOS | `runs-on: macos`, **manual code signing** |
| 6 | Web: deploy to S3/CloudFront | Node setup, `aws s3 sync`, cache invalidation |

---

## Stretch ideas (after the core is done)

- Tags / categories (table relationships).
- Search, sorting, due dates.
- Reminders / push notifications (Lambda + EventBridge).
- A second CI/CD platform (e.g. Codemagic) for the mobile apps, to compare with GitHub Actions.
- Share an item to Stashbox directly from the Slack app (share-sheet integration).
