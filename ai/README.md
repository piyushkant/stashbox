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

## Phase 4: Python + FastAPI service (DONE)

A small Python web service that exposes `POST /summarize`: it takes text, calls the local Llama model, and returns a one-line summary. FastAPI is Python's equivalent of Spring Boot.

### Files in this folder

Committed (the project):
- `main.py` — the FastAPI service (the `/summarize` and `/health` endpoints).
- `requirements.txt` — the dependency list with versions (the "recipe", like `build.gradle.kts`).
- `.python-version` — pins this folder to Python 3.12.11 for pyenv (see below).
- `README.md` — this file.

Ignored (generated, never committed — recreated from `requirements.txt`):
- `.venv/` — the virtual environment (~38 MB of installed packages).
- `__pycache__/` — Python bytecode cache.

### Setup on a new Mac (from scratch)

Important gotcha we hit: on current macOS, **Homebrew's prebuilt Python (3.12 and 3.14) was broken** — a `pyexpat` / system `libexpat` symbol mismatch (`Symbol not found: _XML_SetAllocTrackerActivationThreshold`). The Homebrew Python links against the macOS system expat, which is older than the build expects, so `python3 -m venv` and `pip` fail. The fix is **pyenv**, which compiles Python from source against its own libraries, sidestepping the system mismatch. (Expect to hit the same thing on the new Mac.)

```bash
# 1. install pyenv (skip if already installed: `which pyenv`)
brew install pyenv

# pyenv shell setup (one-time): add to ~/.zshrc, then restart the terminal
#   export PYENV_ROOT="$HOME/.pyenv"
#   export PATH="$PYENV_ROOT/bin:$PATH"
#   eval "$(pyenv init -)"

# 2. install a stable Python from source (compiles; takes a few minutes)
pyenv install 3.12.11

# 3. in the ai/ folder, pin this Python (creates/uses .python-version)
cd ai
pyenv local 3.12.11
python -c "import pyexpat; print('pyexpat OK')"   # sanity check, should print OK

# 4. create the virtual environment (per-project dependency isolation, like Gradle)
~/.pyenv/versions/3.12.11/bin/python -m venv .venv

# 5. install dependencies from the recipe into the venv
.venv/bin/pip install -r requirements.txt
```

Why a virtual environment: Python has no built-in per-project isolation; by default `pip` installs into one shared global location, causing version conflicts between projects. A venv gives `ai/` its own private Python + packages. The `.venv/bin/...` prefix runs the venv's Python/pip without having to "activate" it (activation also works: `source .venv/bin/activate`, then just `python`/`pip`; `deactivate` to exit).

### Run the service

```bash
cd ai
.venv/bin/uvicorn main:app --port 8000 --reload
```

- `uvicorn` is the server that runs the app (like the embedded Tomcat for Spring Boot).
- `main:app` means "in `main.py`, run the `app` object".
- `--reload` auto-restarts on code changes (handy while developing).
- The service listens on `http://localhost:8000`. Ollama must also be running (`brew services start ollama`).

Stop with `Ctrl+C`.

### Try the endpoints

```bash
# health check
curl -s http://localhost:8000/health | jq

# summarize some text (calls the local Llama under the hood)
curl -s -X POST http://localhost:8000/summarize \
  -H "Content-Type: application/json" \
  -d '{"text":"Hey, can you reply to the PM about the Q3 launch plan and confirm the timeline by Friday? Also need the budget numbers."}' | jq
# -> {"summary":"Please confirm the Q3 launch plan timeline by Friday and provide budget numbers."}
```

FastAPI also auto-generates interactive API docs at `http://localhost:8000/docs` while the service runs — open it in a browser to try endpoints without curl.

### How `main.py` maps to Spring Boot

| FastAPI / Python | Spring Boot / Kotlin |
|---|---|
| `app = FastAPI()` | the Spring Boot application |
| `uvicorn` | embedded Tomcat |
| `@app.post("/summarize")` | `@PostMapping("/summarize")` |
| Pydantic `BaseModel` (`SummarizeRequest`) | a request DTO / `@RequestBody` data class |
| `requirements.txt` | `build.gradle.kts` dependencies |
| `.venv/` | Gradle's per-project dependency isolation |

The service calls Ollama with the same `POST /api/generate` shown in the Phase 3 section. The prompt tells the model to "reply with only the summary, no preamble" — that wording (prompt engineering) is why the output is clean rather than chatty.

---

## Phase 5 (planned)

- The Kotlin backend calls this Python service over HTTP to summarize/categorize an item, then stores/returns the result. Teaches services in different languages talking to each other.
