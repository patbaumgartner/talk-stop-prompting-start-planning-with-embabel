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
   Expected: Clean text succeeds, safe PII returns HTTP 422 with
   `code=GUARDRAIL_VIOLATION`, `guard=PIIGuardRail`, and detected entity types in
   `detail`; unsafe PII still returns a summary.

The guarded action uses `ActionRetryPolicy.FIRE_ONCE`: repeating the same
PII-containing input cannot change the policy decision. Without this setting,
Embabel's default action retries add 10, 50, 60, and 60 seconds of backoff before
returning the failure. LLM-level transient retries remain enabled inside
`PromptRunner`; the policy rejection itself happens before any model call.

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

The Presidio health check uses Python's standard library because the analyzer image
does not include `curl`. A 120-second startup grace period allows its NLP model to
load; the container becomes healthy as soon as `/health` responds successfully.

To apply Compose changes and verify the sidecar independently:

```bash
docker compose up -d --wait --wait-timeout 180
docker compose ps
curl --fail http://localhost:5002/health
```

If the sidecar remains unhealthy, inspect `docker compose logs --tail=100 presidio-analyzer`.

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

The runner requires a structured HTTP 422 response and exactly one `PII Guard
Rail: BLOCKED` event per rejected request. Generic HTTP 500 errors and repeated
action attempts fail acceptance.

Regression checks without Docker or an OpenAI key:

```bash
mvn -f demos/03-embabel-pii-guardrails/pom.xml -Dtest=GuardrailRejectionIntegrationTest,GuardrailControllerIntegrationTest test
```

The rejection test uses real HTTP, the real Embabel planner and guardrail
execution, and a mocked Presidio analyzer. It requires a response within five
seconds and exactly one analyzer invocation.

## Tech Stack

- Embabel 1.5.x
- Spring Boot 4.1.x (Java 21+)
- OpenAI gpt-5-mini
- Presidio Analyzer
