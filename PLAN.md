# Stashbox, Development Plan

A full-stack learning roadmap, ordered so each phase produces something working and sets up the next. Built by a senior mobile developer moving into full-stack, so mobile (KMP) is the area of strength, while Spring Boot, Vue, AWS, and GitHub Actions are the new territory being learned.

## Guiding principles

- **Build a thin vertical slice end-to-end first**, then thicken it. Get one tiny feature working from DB to API to client screen to deployed, then repeat.
- **Learn by using, not by reading.** Watch just enough of the AWS course (Udemy: *Ultimate AWS Certified Developer Associate 2026 DVA-C02*), then apply each service here.
- **Backend first**, because the app and web need something real to talk to.
- **CI/CD comes after a manual deploy works.** You can't automate a deploy you don't understand yet.
- **No rush.** The web client is the lowest priority.

## Product recap

Stashbox: stash Slack messages (or links) that are tasks or replies to deal with later. One backend and database, three clients (Android, iOS, web) sharing the same data. Item status: `TODO`, `REPLY_LATER`, or `DONE`. See [README.md](./README.md) for architecture and tech stack.

- **App name:** Stashbox
- **Bundle/App ID (all platforms):** `io.github.kantpiyush.stashbox`
- **Repo:** private monorepo `kant-piyush/stashbox`

---

## Phase 0, Foundations (ongoing, alongside the AWS course)

- [ ] Continue the AWS DVA-C02 course, but use each service as you learn it instead of binge-watching.
  - Priority sections early: IAM, EC2, S3, RDS, Elastic Beanstalk or ECS. Later: Lambda, API Gateway, Cognito.
- [x] GitHub account and private monorepo created.
- [ ] AWS free-tier account set up.
- [ ] AWS CLI installed and configured on the Mac (`aws configure`).
- [ ] Docker installed locally (for running Postgres).

---

## Phase 1, Backend skeleton (Kotlin + Spring Boot)

Goal: get comfortable with the Spring Boot request flow. Should feel familiar as a Kotlin developer.

- [x] Generate a Spring Boot project (Kotlin, Web, base package `io.github.kantpiyush.stashbox`). Scaffolded under `backend/`, Spring Boot 3.4.1 / Kotlin 1.9.25 / Java 21, builds and runs.
- [x] Define a `StashItem` model: `id`, `text`, `link`, `status` (`TODO`/`REPLY_LATER`/`DONE`), `createdAt`.
- [x] `GET /items` and `GET /items/{id}` done as worked examples.
- [ ] **Your exercises:** implement `POST /items`, `PUT /items/{id}`, `DELETE /items/{id}`. They're stubs in `StashItemController.kt` (each returns 501 with a hint comment until you build it).
- [ ] Run locally and exercise every endpoint with curl or Postman.

**How to work this phase (read-then-build rhythm):**

- Don't read the whole tutorial first. Skim the intro once (~5 min) just for the shape.
- Generate/boot the app, prove it runs, then go one endpoint at a time: read only the slice you need, implement that endpoint, test with curl, stop.
- Rule of thumb: if you've read for more than ~15 min without writing code, stop and start typing. Learn the gap by hitting the error, then read the specific fix.
- Skip the tutorial's Mustache/HTML blog parts. Stashbox is API-only.

**Milestone:** a working REST API in memory (all five CRUD endpoints).

---

## Phase 2, Persistence (PostgreSQL)

Goal: a real database behind the API.

- [ ] Run Postgres locally via Docker.
- [ ] Add Spring Data JPA and the Postgres driver.
- [ ] Make `StashItem` an `@Entity` and create a `StashItemRepository`.
- [ ] Swap the in-memory list for the repository.
- [ ] Verify data survives an app restart.

**Milestone:** DB-backed API running locally.

---

## Phase 3, Deploy backend to AWS (first real cloud milestone)

Goal: the API reachable at a public URL. This is where the AWS course pays off.

- [ ] Provision a PostgreSQL database on AWS RDS.
- [ ] Deploy the API. Start with Elastic Beanstalk since it hides most of the infra. EC2 plus RDS is the alternative or next step.
- [ ] Configure the DB connection via environment variables and secrets, not hardcoded.
- [ ] Hit the live public URL from curl or the browser.

**Milestone:** Stashbox API live on the internet.

---

## Phase 4, CI/CD with GitHub Actions (backend). Actions enters here.

Goal: push code and it builds, tests, and deploys automatically. This is the first non-Bitrise pipeline.

- [ ] **4a, Build and test:** a workflow on every push that runs `./gradlew test` and builds the jar.
  - Learn: workflows, jobs, steps, triggers (`on: push`), Gradle caching.
- [ ] **4b, Deploy:** extend the workflow to deploy the jar to AWS on merge to `main`.
  - Learn: GitHub Secrets (AWS keys), environments, deploy steps.

**Milestone:** push to `main` auto-deploys the backend.

---

## Phase 5, KMP mobile app (home turf)

Goal: connect Android and iOS to the live API, and use the familiar mobile layer to learn the AWS and auth side.

- [ ] Create the KMP project under `mobile/`, application ID `io.github.kantpiyush.stashbox`.
- [ ] Shared networking layer calling the Stashbox API.
- [ ] List screen (all items), add screen, mark-done toggle, delete.
- [ ] Optimistic UI for mark-done, and confirm it syncs across devices.
- [ ] **5a, Android CI:** GitHub Actions builds the APK or AAB (matrix builds, artifact upload, Android SDK setup).
- [ ] **5b, iOS CI:** GitHub Actions builds on a `macos-latest` runner with manual code signing. This is the biggest jump from Bitrise, since there's no Workflow Editor doing signing for you.

**Milestone:** both mobile apps talk to the live API, and both build in CI.

---

## Phase 6, Vue web client (lowest priority)

Goal: a third client on the same API, plus more AWS practice.

- [ ] Vue 3 plus Vite SPA under `web/` consuming the Stashbox API.
- [ ] List, add, mark-done, and delete UI.
- [ ] Deploy to S3 and CloudFront.
- [ ] GitHub Actions job: Node setup, build, `aws s3 sync`, CloudFront cache invalidation.

**Milestone:** add an item on the phone, refresh the website, and it's there.

---

## Phase 7, Polish and publish (full lifecycle finish)

Goal: complete the dev-to-live lifecycle.

- [ ] Auth via AWS Cognito (or JWT): login, items scoped per user, protected endpoints, serving all three clients.
- [ ] Image and file attachments: upload to S3 via presigned URLs, then display across Android, iOS, and Vue.
- [ ] Publish the Android app to the Google Play Store.
- [ ] Publish the iOS app to the Apple App Store (check display-name uniqueness and add a subtitle if needed).

**Milestone:** Stashbox live on both stores. Full lifecycle complete.

---

## CI/CD learning ladder (easy to hard)

| Stage | What you automate | New GitHub Actions concept |
|---|---|---|
| 4a | Backend: test and build | workflows, jobs, triggers, caching |
| 4b | Backend: deploy to AWS | secrets, environments, deploy |
| 5a | Android: build APK or AAB | matrix builds, artifacts, SDK setup |
| 5b | iOS: build on macOS | `runs-on: macos`, manual code signing |
| 6 | Web: deploy to S3 and CloudFront | Node setup, `aws s3 sync`, cache invalidation |

---

## AWS course mapping (read a section, then use it in the app)

Using the Udemy AWS DVA-C02 course (34 sections total). The course is built for the certification, so it covers far more than Stashbox needs. Watch a section right before the phase that uses it, then apply it immediately. Don't watch ahead.

**Watch these, mapped to phases:**

| When (phase) | Sections to watch | What you do in Stashbox |
|---|---|---|
| Setup (Phase 0) | S1 Intro, S3 Getting Started, S4 IAM & AWS CLI | Create AWS account, set up IAM users/roles (never use root), install + configure AWS CLI |
| Deploy backend (Phase 3) | S5 EC2 Fundamentals, S8 RDS + Aurora + ElastiCache, S17 Elastic Beanstalk | RDS for managed Postgres; Elastic Beanstalk to deploy the Spring Boot jar (watch EC2 first, EB sits on top) |
| Deploy web (Phase 6) | S11 S3 Intro, S14 S3 Security, S15 CloudFront | Host the Vue static site on S3, serve via CloudFront |
| File uploads (Phase 7) | S13 Advanced S3, S12 CLI/SDK/IAM Roles | Presigned URLs to upload attachments to S3 |
| Auth (Phase 7) | S27 Cognito | Login + per-user data across all three clients |
| Optional, once live | S20 CloudWatch / X-Ray / CloudTrail | Logs and monitoring (nice to have, not required to ship) |

**Skip for building Stashbox (watch later, cert-only):**

- S6 EC2 Storage, S7 ELB + ASG (scaling, not needed at this size)
- S9 Route 53, S10 VPC (networking depth, EB handles enough)
- S16 ECS/ECR/Fargate (Docker on AWS, alternative to EB to explore later)
- S18 CloudFormation, S26 CDK (infra-as-code, overkill for now)
- S19 SQS/SNS/Kinesis, S28 Step Functions/AppSync (messaging/orchestration)
- S21 Lambda, S22 DynamoDB, S23 API Gateway, S25 SAM (the serverless backend path; we chose Spring Boot + RDS instead, so cert-only here)
- S24 AWS CICD (CodePipeline etc.; we chose GitHub Actions, so cert-only)
- S29 Advanced Identity, S30 Security/KMS, S31 Other Services, S32-S34 (exam prep and cleanup)

Note: the serverless half of the course (Lambda, DynamoDB, API Gateway, SAM) is a completely different way to build a backend than our Spring Boot + RDS path. You'll need it for the cert, not for Stashbox. Circle back to it for exam prep after the app is built.

---

## Stretch ideas (after the core is done)

- Tags and categories (table relationships).
- Search, sorting, due dates.
- Reminders and push notifications (Lambda plus EventBridge).
- A second CI/CD platform (for example Codemagic) for the mobile apps, to compare with GitHub Actions.
- Share an item to Stashbox directly from the Slack app (share-sheet integration).
