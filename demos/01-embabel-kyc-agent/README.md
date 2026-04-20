# Demo 1: Embabel KYC Verification Agent

## Overview

- Duration: 5 min
- Key concepts: domain objects as tools, property filtering, type-safe planning
- Goal: classify customer KYC risk with two Embabel actions

## What To Show

- Typed planning chain from `KycRequest` to `KycAssessment`
- Sensitive-field filtering with `.withoutProperties("customerId")`
- Thin REST entrypoint that asks Embabel for the goal type

## Demo Flow

1. Open `KycModels.java` and walk through `KycRequest -> KycScreening -> KycAssessment`.
   Explanation: These records define the planning graph from input to goal.
   Expected: A clear type chain with no workflow DSL.
2. Open `KycVerificationAgent.java` and show `screenCustomer`, `assessRisk`, and `.withoutProperties("customerId")`.
   Explanation: The planner routes through typed actions and excludes sensitive fields from prompts.
   Expected: `assessRisk` is the `@AchievesGoal` endpoint for planning.
3. Open `KycController.java` and show `AgentInvocation.create(agentPlatform, KycAssessment.class)`.
   Explanation: The endpoint asks Embabel for the goal type and lets the planner compute the steps.
   Expected: A thin controller with no manual orchestration.
4. Execute the 4 requests in `requests.http`.
   Explanation: Each payload exercises a different compliance scenario.
   Expected: `CUST-001` -> `LOW/APPROVE`, `CUST-002` -> `HIGH/ENHANCED_DUE_DILIGENCE`, `CUST-003` -> `CRITICAL/REJECT`, `CUST-004` -> `MEDIUM/ENHANCED_DUE_DILIGENCE`.

## Architecture

| Component | Role |
| --- | --- |
| `KycModels.java` | Input, intermediate, and goal records |
| `KycVerificationAgent.java` | Two `@Action` methods, one `@AchievesGoal` |
| `KycController.java` | REST endpoint and agent invocation |
| Embabel Planner | Builds plan from request type to goal type |
| OpenAI | Executes LLM-backed screening and assessment |

## Prerequisites

- Java 21+
- Maven
- OPENAI_API_KEY

## Build & Run

```bash
export OPENAI_API_KEY=your_key_here
mvn spring-boot:run
```

Application starts on `http://localhost:8080`.

## Requests

- Use `requests.http` for the live walkthrough.
- Recommended order: clean customer, PEP match, sanctions match, adverse media.
- Endpoint: `POST /api/kyc/verify`

## Acceptance

From the repository root:

```bash
./run-demo-acceptance.sh
```

## Tech Stack

- Embabel 0.4.x
- Spring Boot 3.5.x
- OpenAI gpt-5-mini
