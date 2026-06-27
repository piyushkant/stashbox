# CLAUDE.md — Stashbox

Guidance for Claude Code when working in this repo. These instructions override default behavior.

## What this is

Stashbox is a **learn-by-building full-stack project**, not production software. The owner is a **senior mobile developer (KMP/Android/iOS) moving into full-stack**. Mobile is home turf; Spring Boot, Vue, AWS, and GitHub Actions are the new territory being learned.

The goal is understanding, not just working code. When the work touches the new territory (backend, web, AWS, CI/CD), **explain the "why" and relate it to mobile concepts the owner already knows** (e.g. Spring DI vs. mobile DI, FastAPI vs. Spring, JPA vs. Room). Don't just hand over code — teach the concept behind it. For mobile work, skip the hand-holding.

See `README.md` (architecture, tech stack, data model) and `PLAN.md` (phased roadmap + AWS course mapping) for full context. Each sub-folder has its own README with run instructions and study notes.

## Model & usage (personal Claude Pro)

This project runs on a **personal Claude Pro subscription, Sonnet-first**. Stay on **Sonnet** — do not switch to Opus. Sonnet is the right tool for this learning work, and keeping Opus unused protects the Pro usage budget. If a task ever feels like it genuinely needs deeper reasoning, say so and let the owner decide — don't silently switch models.

## How to work here (keeps sessions focused and within Pro limits)

- **Work from the sub-folder, not the repo root.** This is a 5-part monorepo (~2k files). When working on one part, expect to be launched from that folder (`backend/`, `web/`, etc.) so context stays scoped to that project.
- **One phase ≈ one session.** PLAN.md is cut into clean phases. Focus a session on the current phase's goal; don't wander across phases.
- **Read only what's needed.** Search and read specific files rather than pulling in the whole tree. Don't ask the owner to paste large files — find them.
- **Follow PLAN.md's read-then-build rhythm.** Build a thin vertical slice, prove it works, then thicken. Don't scaffold ahead of the current phase.

## Current status (update as phases complete)

- **Done:** Phase 0–2 (Spring Boot backend, full CRUD, PostgreSQL via JPA, layered Controller→Service→Repository) and the AI track Phases 3–5 (local Llama via Ollama, Python FastAPI `/summarize` service, wired into the backend, summary persisted on the item).
- **Next:** Phase 6 — deploy the backend to AWS (free-tier RDS + Elastic Beanstalk). Set a billing alarm *before* creating any chargeable resource.
- **Not started:** Phase 7 (GitHub Actions CI/CD), Phase 8 (Vue web client), Phase 9 (KMP mobile), Phase 10 (auth, S3 attachments, store publishing).

## Repo & git setup

- **Home:** `github.com/piyushkant/stashbox` (public) — the `origin` remote, on the owner's **personal** GitHub portfolio account. Commits are attributed here.
- **Backup:** the `backup` remote points at the older `kant-piyush/stashbox` (private). Don't push there in normal work; it's an untouched safety copy.
- **Commit identity is repo-local:** name "Piyush Kant", email `piyush.kant.it@gmail.com` (verified on `piyushkant`, so commits show on the contribution graph). The global git identity is a *work* email and must stay untouched — never run `git config --global` here.
- **Push auth:** HTTPS via the `gh` CLI (active account `piyushkant`). No SSH key needed for this repo.

## Conventions

- **App / bundle ID (all platforms):** `io.github.kantpiyush.stashbox`
- **Backend:** Kotlin + Spring Boot 3.4.1, Java 21, layered architecture, Spring Data JPA + PostgreSQL.
- **AI:** Python 3.12 (pyenv) + FastAPI + Ollama (`llama3.1:8b`), local-only, never deployed to AWS (GPU cost).
- **Config via env vars / secrets, never hardcoded** — especially DB credentials and any AWS keys.
- **Cost discipline on AWS:** stay on free tier (single-instance, no load balancer, no NAT gateway); tear down resources when not in use; the billing alarm is the safety net.
