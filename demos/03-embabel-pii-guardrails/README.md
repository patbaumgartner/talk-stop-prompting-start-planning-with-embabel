# Demo 3: Embabel PII Guardrails

## Overview

- Duration: 7 min
- Key concepts: `UserInputGuardRail`, Presidio integration, before-LLM validation
- Goal: block PII in user input before sending content to the LLM

## What To Show

- Guard rail enforcement before any LLM call happens
- Clear contrast between `/safe` and `/unsafe`
- Presidio as an external policy service, not embedded demo logic

## Demo Flow

1. Open `PIIUserInputGuardRail.java` and show:
   - `UserInputGuardRail` implementation
   - `validate(...)` calling Presidio `/analyze`
   - allow vs block behavior via `ValidationResult`
   Explanation: Guard rail validation runs before the LLM and can reject unsafe input.
   Expected: A clear allow/block decision path from Presidio findings.
2. Open `TextSummarizationAgent.java` and `GuardrailController.java`:
   - `/safe` path uses guard rails
   - `/unsafe` path bypasses guard rails
   Explanation: The same summarization use case is exposed through two execution paths.
   Expected: `/safe` and `/unsafe` behave differently for PII-heavy text.
3. Start Presidio with Docker Compose:
   - `docker compose up -d`
   - `docker compose ps`
   Explanation: Presidio runs as an external sidecar on port `5002`.
   Expected: `presidio-analyzer` is up before running requests.
   Note: With Spring Boot Docker Compose support enabled in this demo, `mvn spring-boot:run` can also start the sidecar automatically.
4. Run 4 request flows from `requests.http`:
   - clean text -> summarized
   - text with PII -> blocked
   - same text via `/unsafe` -> summarized with exposed PII
   - mixed business text with embedded PII -> blocked
   Explanation: These requests verify pre-LLM interception in the safe path.
   Expected: Clean text succeeds, safe PII is rejected, and unsafe PII still returns a summary.

## Architecture

| Component | Role |
| --- | --- |
| `PIIUserInputGuardRail` | Calls Presidio and blocks risky input |
| `PresidioAnalyzerClient` | HTTP client for Presidio analyzer API |
| `TextSummarizationAgent` | Summarization action with and without guard rails |
| `GuardrailController` | `/safe` and `/unsafe` endpoints |
| Presidio Analyzer | External PII detection service (Docker) |

## Prerequisites

- Java 21+
- Maven
- Docker
- OPENAI_API_KEY

## Build & Run

```bash
export OPENAI_API_KEY=your_key_here
mvn spring-boot:run
```

Spring Boot starts `docker-compose.yml` automatically for local runs (via `spring-boot-docker-compose`).

Application starts on `http://localhost:8080`.

## Requests

- Use `requests.http` for the live walkthrough.
- Recommended order: clean text, blocked PII, unsafe comparison, mixed-content block.
- Endpoints: `POST /api/summarize/safe` and `POST /api/summarize/unsafe`

## Acceptance

From the repository root:

```bash
./run-demo-acceptance.sh
```

## Tech Stack

- Embabel 0.4.x
- Spring Boot 3.5.x
- OpenAI gpt-5-mini
- Microsoft Presidio Analyzer
