# Stashbox, Development Plan

A full-stack learning roadmap, ordered so each phase produces something working and sets up the next. Built by a senior mobile developer moving into full-stack, so mobile (KMP) is the area of strength, while Spring Boot, Vue, AWS, and GitHub Actions are the new territory being learned.

## Guiding principles

- **Build a thin vertical slice end-to-end first**, then thicken it. Get one tiny feature working from DB to API to client screen to deployed, then repeat.
- **Learn by using, not by reading.** Watch just enough of the AWS course (Udemy: *Ultimate AWS Certified Developer Associate 2026 DVA-C02*), then apply each service here.
- **Backend first**, because the app and web need something real to talk to.
- **CI/CD comes after a manual deploy works.** You can't automate a deploy you don't understand yet.
- **Web before mobile.** Web is the genuinely new skill (no prior web experience), so it comes first. Mobile is the area of strength and is saved for last.

## Product recap

Stashbox: stash Slack messages (or links) that are tasks or replies to deal with later. One backend and database, three clients (Android, iOS, web) sharing the same data. Item status: `OPEN` or `DONE` for now (a `REPLY_LATER` status can be added back later). See [README.md](./README.md) for architecture and tech stack.

- **App name:** Stashbox
- **Bundle/App ID (all platforms):** `io.github.kantpiyush.stashbox`
- **Repo:** private monorepo `kant-piyush/stashbox`

---

## Phase 0, Foundations

Goal: the basics needed to start building.

- [x] GitHub account and private monorepo created.
- [x] Local toolchain ready: Java 21, IntelliJ IDEA Ultimate (main IDE for backend + web), Android Studio + Xcode for mobile.

---

## Phase 1, Backend skeleton (Kotlin + Spring Boot)

Goal: get comfortable with the Spring Boot request flow. Should feel familiar as a Kotlin developer.

- [x] Generate a Spring Boot project (Kotlin, Web, base package `io.github.kantpiyush.stashbox`). Scaffolded under `backend/`, Spring Boot 3.4.1 / Kotlin 1.9.25 / Java 21, builds and runs.
- [x] Define a `StashItem` model: `id`, `text`, `link`, `status` (`OPEN`/`DONE`), `createdAt`.
- [x] `GET /items` and `GET /items/{id}` (Read).
- [x] `POST /items` (Create), `PUT /items/{id}` (Update), `DELETE /items/{id}` (Delete).
- [x] Run locally and exercise every endpoint with curl. All five CRUD endpoints verified working.

**How to work this phase (read-then-build rhythm):**

- Don't read the whole tutorial first. Skim the intro once (~5 min) just for the shape.
- Boot the app, prove it runs, then go one endpoint at a time: read only the slice you need, understand the code, test with curl, stop.
- Skip the tutorial's Mustache/HTML blog parts. Stashbox is API-only.

**Milestone:** DONE. A working REST API in memory (all five CRUD endpoints).

---

## Phase 2, Persistence (PostgreSQL)

Goal: a real database behind the API.

- [x] Install Docker locally; run Postgres 16 in a container (`stashbox-postgres`, named volume for persistence).
- [x] Add Spring Data JPA, the Kotlin JPA plugin, and the Postgres driver.
- [x] Make `StashItem` a JPA `@Entity` (regular class, not data class; `var` fields; `@Enumerated(STRING)` for status) and create `StashItemRepository`.
- [x] Introduce a service layer behind an interface (`StashItemService` + `StashItemServiceImpl`); controller depends on the interface. Swap the in-memory store for the repository.
- [x] Verify data survives an app restart (confirmed: item persisted across a full restart).

**Milestone:** DONE. DB-backed API running locally on Postgres.

---

## AI track (Python + local LLM), Phases 3-5

Goal: learn Python, the AI ecosystem, and how LLMs actually run, by adding AI features to the backend. This is for learning, not for polished/complex AI results. It comes right after persistence and before the frontend, because the AI work is backend-like (the LLM is just a service the backend calls) and runs fully locally. The natural Stashbox feature: summarize / categorize / suggest-a-reply for a stashed item.

How it fits the architecture: an LLM is just another service the backend calls. Flow:
`Kotlin backend -> Python AI service (FastAPI) -> LLM (local Ollama / Llama) -> text back -> stored on the item`.
Clients never call the LLM directly (keeps keys/control server-side).

**LLM stays local, not on AWS (decided 2026-06-21).** Running an LLM in the cloud needs an expensive GPU instance (not free-tier), so the model runs only on the Mac via Ollama. Consequence: when the backend is deployed to AWS (Phase 6), the AI feature is a local-only / dev-only capability. That's an accepted limitation for a learning project, the point is to learn how LLMs run, not to serve AI in production.

### Phase 3, Local LLM basics

- [x] Install **Ollama** (the "Docker for LLMs", one command to pull + run a model locally). Installed via Homebrew, server runs on port 11434.
- [x] Pull models and talk to them from the terminal + via the API. Tried `llama3.2:3b`, `llama3.1:8b`, and Google's `gemma2:9b`; compared speed/quality/personality. **Decided on `llama3.1:8b` as the default** (accurate + concise for summaries, ~47 tok/s; the 3B cut corners, Gemma was too chatty/emoji-y for a summary).
- [x] Understand the local API: Ollama serves on `http://localhost:11434`, called with the same HTTP/JSON pattern as any other service.

**Milestone:** DONE. Llama running locally, answering prompts; default model chosen. Setup + comparison documented in [ai/README.md](./ai/README.md).

### Phase 4, Python AI service (FastAPI)

- [x] Create a Python service in `ai/` using **FastAPI** (Python's equivalent of Spring Boot), run with uvicorn. Python managed via pyenv 3.12.11 (Homebrew Python was broken on this macOS — expat mismatch; see ai/README.md).
- [x] `POST /summarize` endpoint that takes text and returns a one-line summary by calling the local `llama3.1:8b` via Ollama. Plus a `/health` check.
- [x] Learned: virtualenv/pip (vs Gradle), FastAPI structure (vs Spring), Pydantic models (vs DTOs), async, prompt engineering.

**Milestone:** DONE. Python service summarizes text via the local LLM. Verified end to end. Setup documented in [ai/README.md](./ai/README.md).

### Phase 5, Wire AI into the backend

- [x] Kotlin backend calls the Python AI service (`AiSummaryClient` using Spring `RestClient`) via `POST /items/{id}/summarize`. Learned: outbound HTTP from the backend, `@Component`, and two integration gotchas (use injected `RestClient.Builder`; force `SimpleClientHttpRequestFactory` so uvicorn accepts the request).
- [x] Store the AI result on the item (`summary` field on `StashItem`) and expose it through the API. Verified the summary persists.
- [ ] Later/optional: swap local Ollama for a cloud LLM API and compare; add semantic search with embeddings + a vector database (the more advanced AI skill).

**Milestone:** DONE. AI summarize working end to end on real stashed items (backend -> Python service -> Ollama), summary stored on the item.

Note on hardware: local models need RAM; the M2 Max is well-suited (unified memory lets the GPU use system RAM). 3B-8B Llama models run comfortably. A cloud API is the fallback if a model is too slow.

---

## Phase 6, Deploy backend to AWS (first real cloud milestone)

Goal: the API reachable at a public URL. This is where the AWS course pays off, and where AWS first enters the project. (The AI/LLM feature stays local, see the AI track note, so what gets deployed is the core CRUD backend.)

- [x] Set up an AWS free-tier account. The Udemy course covers this (S3 Getting Started), so create the account there, not separately. A card is required, but charges only apply above free-tier limits.
- [x] **FIRST, before creating any chargeable resource: set up a billing budget + alarm** (threshold ~$1-$5). The course covers this in S4. Do it the instant the account exists, billing data lags by hours, so an alarm set "after I notice a charge" is too late.
- [x] Set up an IAM user (not root) for day-to-day use. (Course S4 IAM & AWS CLI.)
- [x] Install and configure the AWS CLI on the Mac (`aws configure`). Personal profile set up as `aws-p` to avoid conflicts with company AWS.
- [x] Provision a PostgreSQL database on AWS RDS (free-tier `db.t3.micro` + 20 GB). Instance: `stashbox-db`, endpoint: `stashbox-db.cl2o0cs4s975.ap-northeast-1.rds.amazonaws.com`.
- [x] Deploy the API. Start with Elastic Beanstalk since it hides most of the infra. Use a single-instance environment (no load balancer, an ELB is NOT free tier). EC2 plus RDS is the alternative or next step. Live at: `stashbox.eba-mshjk9yb.ap-northeast-1.elasticbeanstalk.com`
- [x] Configure the DB connection via environment variables and secrets, not hardcoded. (`SPRING_PROFILES_ACTIVE`, `DB_PASSWORD`, `SERVER_PORT` set in Beanstalk env properties.)
- [x] Hit the live public URL from curl or the browser. `/items` returns `[]`.

**Cost plan:** Stay on free tier (EC2/RDS `micro`, single instance, no load balancer or NAT gateway) so the first 12 months are effectively $0. The billing alarm is the safety net. Tear down resources when not actively using them, and watch the 12-month free-tier expiry. (Lightsail at ~$5/month fixed is the predictable-cost fallback, but we use free-tier EC2/RDS/EB because that is what the DVA-C02 cert actually tests.)

**Milestone:** DONE. Stashbox API live on the internet at `stashbox.eba-mshjk9yb.ap-northeast-1.elasticbeanstalk.com`.

---

## Phase 7, CI/CD with GitHub Actions (backend). Actions enters here.

Goal: push code and it builds, tests, and deploys automatically. This is the first non-Bitrise pipeline.

- [ ] **7a, Build and test:** a workflow on every push that runs `./gradlew test` and builds the jar.
  - Learn: workflows, jobs, steps, triggers (`on: push`), Gradle caching.
- [ ] **7b, Deploy:** extend the workflow to deploy the jar to AWS on merge to `main`.
  - Learn: GitHub Secrets (AWS keys), environments, deploy steps.

**Milestone:** push to `main` auto-deploys the backend.

---

## Phase 8, Vue web client (first client, the new territory)

Goal: build the first client on the API. Web comes before mobile because it's the genuinely new skill to learn (no prior web experience); mobile is saved for last as the area of strength.

- [ ] Vue 3 plus Vite SPA under `web/` consuming the Stashbox API.
- [ ] List, add, mark-done, and delete UI. Add the "summarize this" AI action here (calls the backend, which uses the local AI service when available).
- [ ] Deploy to S3 and CloudFront.
- [ ] GitHub Actions job: Node setup, build, `aws s3 sync`, CloudFront cache invalidation.

**Milestone:** a working web app reading and writing the same data as the API.

---

## Phase 9, KMP mobile app (home turf, saved for last)

Goal: connect Android and iOS to the live API. Mobile is the area of strength, so it comes last and reuses everything already standing.

- [ ] Create the KMP project under `mobile/`, application ID `io.github.kantpiyush.stashbox`.
- [ ] Shared networking layer calling the Stashbox API.
- [ ] List screen (all items), add screen, mark-done toggle, delete.
- [ ] Optimistic UI for mark-done, and confirm it syncs across devices.
- [ ] **9a, Android CI:** GitHub Actions builds the APK or AAB (matrix builds, artifact upload, Android SDK setup).
- [ ] **9b, iOS CI:** GitHub Actions builds on a `macos-latest` runner with manual code signing. This is the biggest jump from Bitrise, since there's no Workflow Editor doing signing for you.

**Milestone:** add an item on the web app, open the phone, and it's there. Both mobile apps build in CI.

---

## Phase 10, Polish and publish (full lifecycle finish)

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
| 7a | Backend: test and build | workflows, jobs, triggers, caching |
| 7b | Backend: deploy to AWS | secrets, environments, deploy |
| 8 | Web: deploy to S3 and CloudFront | Node setup, `aws s3 sync`, cache invalidation |
| 9a | Android: build APK or AAB | matrix builds, artifacts, SDK setup |
| 9b | iOS: build on macOS | `runs-on: macos`, manual code signing |

---

## AWS course mapping (read a section, then use it in the app)

Using the Udemy AWS DVA-C02 course (34 sections total). The course is built for the certification, so it covers far more than Stashbox needs. Watch a section right before the phase that uses it, then apply it immediately. Don't watch ahead.

**Watch these, mapped to phases:**

| When (phase) | Sections to watch | What you do in Stashbox |
|---|---|---|
| AWS setup (Phase 6) | S1 Intro, S3 Getting Started, S4 IAM & AWS CLI | Create AWS account, set up IAM users/roles (never use root), install + configure AWS CLI |
| Deploy backend (Phase 6) | S5 EC2 Fundamentals, S8 RDS + Aurora + ElastiCache, S17 Elastic Beanstalk | RDS for managed Postgres; Elastic Beanstalk to deploy the Spring Boot jar (watch EC2 first, EB sits on top) |
| Deploy web (Phase 8) | S11 S3 Intro, S14 S3 Security, S15 CloudFront | Host the Vue static site on S3, serve via CloudFront |
| File uploads (Phase 10) | S13 Advanced S3, S12 CLI/SDK/IAM Roles | Presigned URLs to upload attachments to S3 |
| Auth (Phase 10) | S27 Cognito | Login + per-user data across all three clients |
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
