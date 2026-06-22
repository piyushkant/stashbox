# ai

The AI service for Stashbox: a Python + FastAPI service (Phase 4) that calls a local LLM (Llama via Ollama) to summarize / categorize stashed items.

Flow: `Kotlin backend -> this Python AI service (FastAPI) -> local LLM (Ollama) -> text back`. Clients never call the LLM directly.

The LLM runs **locally only** (not deployed to AWS, since cloud GPUs are expensive), so this is a dev/local capability for learning how LLMs run, not for polished AI results.

See the root [PLAN.md](../PLAN.md), Phases 3-5.

---

## Phase 3: local LLM with Ollama (DONE)

Ollama is "Docker for LLMs": one tool to pull and run model "images" locally. It has a background **server** (listens on port `11434`) and a **CLI** that talks to it. Your code talks to it over plain HTTP, the same pattern as any other service.

### Setup on a new Mac (from scratch)

```bash
# 1. install Ollama (Homebrew; install Homebrew first if needed, see backend/README.md step 0)
brew install ollama

# 2. start the server as a background service (also restarts at login)
brew services start ollama

# 3. confirm the server is up (should return JSON, not an error)
curl -s http://localhost:11434/api/tags | jq

# 4. pull the model we use
ollama pull llama3.1:8b
```

### Everyday use

```bash
ollama list                          # show installed models + sizes
ollama run llama3.1:8b               # interactive chat in the terminal (type /bye to exit)
ollama run llama3.1:8b --verbose     # same, but prints tokens/sec after each reply
brew services stop ollama            # stop the server
brew services start ollama           # start it again
```

### Talk to it via the API (the pattern the backend will use)

```bash
# one-off generation; -r prints just the response text
curl -s http://localhost:11434/api/generate -d '{
  "model": "llama3.1:8b",
  "prompt": "Summarize in one sentence: reply to PM about the Q3 launch plan by Friday",
  "stream": false
}' | jq -r '.response'
```

- `"model"` — which installed model to use.
- `"prompt"` — the input text.
- `"stream": false` — return the whole answer at once (simpler for the backend) instead of token-by-token.

Note on speed: the **first** request after the model has been idle (~5 min) includes a load-into-memory delay (~1-2s). After that it's warm and fast. Not a bug.

---

## Models tried, and why we chose `llama3.1:8b`

On an M2 Max (32GB), same prompt ("summarize in one sentence: reply to PM about the Q3 launch plan by Friday"):

| Model | Family | Size | Speed | Notes |
|---|---|---|---|---|
| `llama3.2:3b` | Meta | 2.0 GB | ~85 tok/s | Fastest, but cut corners / lost nuance on the prompt |
| **`llama3.1:8b`** | Meta | 4.9 GB | ~47 tok/s | **Chosen default.** Accurate, concise, appropriate for summaries |
| `gemma2:9b` | Google | 5.4 GB | ~37 tok/s | Good, but too chatty/assistant-like (pleasantries, emoji) for a summary field |

**Decision: `llama3.1:8b`.** It hit the sweet spot, fast enough, accurate, and concise. The 3B was quicker but less reliable; Gemma's friendly chatbot tone is wrong for a one-line summary (we want just the summary, no preamble). All three stay installed for experimenting (`ollama rm <name>` to remove later; disk space is not a concern).

Aside: Google's **Gemini** (from a Google AI Pro subscription) is cloud-only and cannot be downloaded to run locally. The local Google model is **Gemma** (used above). Gemini's cloud API is a possible *later* comparison for the "swap local for a cloud LLM" optional step, not part of the local setup.

---

## Phase 4-5 (planned)

- **Phase 4:** a FastAPI service here with a `POST /summarize` endpoint that calls `llama3.1:8b` via the Ollama API above.
- **Phase 5:** the Kotlin backend calls this service to summarize/categorize an item.
