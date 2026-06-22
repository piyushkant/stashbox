"""
Stashbox AI service.

A tiny FastAPI service that takes some text and returns a one-line summary,
produced by the local Llama model running in Ollama.

Flow: (caller) -> POST /summarize -> this service -> Ollama (llama3.1:8b) -> summary
"""

import httpx
from fastapi import FastAPI
from pydantic import BaseModel

# Where the local Ollama server listens (started via `brew services start ollama`).
OLLAMA_URL = "http://localhost:11434/api/generate"
MODEL = "llama3.1:8b"

# The FastAPI app object. Like a Spring Boot application: the thing the server runs.
app = FastAPI(title="Stashbox AI")


# Request body shape. Pydantic's BaseModel validates and parses incoming JSON,
# the same role StashItemRequest plays in the Kotlin backend. A POST with
# {"text": "..."} becomes a SummarizeRequest with request.text filled in.
class SummarizeRequest(BaseModel):
    text: str


# Response body shape.
class SummarizeResponse(BaseModel):
    summary: str


# A simple health check, handy to confirm the service is up.
@app.get("/health")
def health() -> dict:
    return {"status": "ok"}


# POST /summarize -> summarize the given text using the local LLM.
# `async` lets the server handle other requests while waiting on the (slow) LLM call.
@app.post("/summarize", response_model=SummarizeResponse)
async def summarize(request: SummarizeRequest) -> SummarizeResponse:
    # Build the prompt. We instruct the model to return ONLY the summary, no preamble,
    # which is why we picked llama3.1:8b (it follows this more tersely than chatty models).
    prompt = (
        "Summarize the following note in one short sentence. "
        "Reply with only the summary, no preamble.\n\n"
        f"{request.text}"
    )

    # Call Ollama's HTTP API, the same endpoint we tested with curl. stream=false
    # means "give the whole answer at once". We use an async HTTP client (httpx).
    async with httpx.AsyncClient(timeout=60.0) as client:
        response = await client.post(
            OLLAMA_URL,
            json={"model": MODEL, "prompt": prompt, "stream": False},
        )
        response.raise_for_status()
        data = response.json()

    # Ollama returns {"response": "...the generated text...", ...}.
    summary = data.get("response", "").strip()
    return SummarizeResponse(summary=summary)
