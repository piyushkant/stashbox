# ai

The AI service for Stashbox: a Python + FastAPI service that calls a local LLM (Llama via Ollama) to summarize / categorize stashed items.

Kept separate from the Kotlin backend. The flow is:
`Kotlin backend -> this Python AI service (FastAPI) -> local LLM (Ollama / Llama) -> text back`.
Clients never call the LLM directly.

See the root [PLAN.md](../PLAN.md), Phases 3-5 (AI track). Nothing set up yet.

## Planned

- **Phase 3:** install Ollama, run a Llama model locally (`llama3.2:3b` to start, then `llama3.1:8b`), talk to it from the terminal. Ollama serves on `http://localhost:11434`.
- **Phase 4:** a FastAPI service here with a `POST /summarize` endpoint that calls the local model.
- **Phase 5:** the Kotlin backend calls this service to summarize/categorize an item.

Note: the LLM runs locally only (not deployed to AWS, since cloud GPUs are expensive), so this is a dev/local capability for learning. This is for learning how LLMs run, not for polished AI results.
