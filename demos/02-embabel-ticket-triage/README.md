# Demo 2: Embabel Ticket Triage Agent

## Overview

- Duration: 9 min
- Key concepts: replanning, sealed interfaces, deterministic and LLM paths, traceability
- Goal: classify, prioritize, and route tickets with a typed planning model

## What To Show

- Sealed interfaces driving route selection in the type system
- Deterministic fast-paths versus low-confidence replanning
- Trace output that makes the planner behavior explainable

## Demo Flow

1. Open `TriageModels.java` and show the routed domain model:
	- `Signals` (`SignalsOk`, `SignalsNeedsDeep`)
	- `CategoryAssessment` (`ConfidentCategory`, `UncertainCategory`)
   Explanation: Sealed interfaces encode route decisions in the type system.
	Expected: A clear split between deterministic and uncertain paths.
2. Open `TicketTriageAgent.java` and show action split:
	- deterministic path: `extractSignals` -> `quickClassifyOk` -> `assessPriority` -> `route`
	- uncertain path: `quickClassifyNeedsDeep` -> `deepClassify`
	- `finalizeResponse` as `@AchievesGoal`
   Explanation: Fast-path logic avoids LLM calls unless confidence is low.
	Expected: `deepClassify` runs only for uncertain requests.
3. Open `TriageController.java` and show typed invocation via `AgentInvocation`.
   Explanation: API remains simple while planning stays internal to Embabel.
   Expected: Minimal REST glue code.
4. Run 3 request flows from `requests.http`:
	- `INC-101`: deterministic incident flow
	- `REQ-202` with `simulate=LOW_CONFIDENCE`: replanning and deep classify
	- `SEC-777`: deterministic security fast-path (`CRITICAL`)
   Explanation: These three cases show normal routing, replanning, and hard security rules.
	Expected: Trace omits deep classify for normal/security and includes it for low-confidence.

## Architecture

| Component | Role |
| --- | --- |
| `TriageModels.java` | Typed model and route selection |
| `TicketTriageAgent.java` | Seven `@Action` methods, one `@AchievesGoal` |
| `TriageController.java` | REST endpoint and agent invocation |
| Embabel Planner | Computes action chain to `TriageResponse` |
| OpenAI | Used only for uncertain classification and non-deterministic priority |

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
- Recommended order: normal incident, uncertain case, security fast-path, service request, outage plus VIP.
- Endpoint: `POST /api/v1/triage`

## Acceptance

From the repository root:

```bash
./run-demo-acceptance.sh
```

The runner covers all five HTTP flows and prints the framework plan/action
timeline alongside each response trace. For `LOW_CONFIDENCE`, compare the initial
plan containing `quickClassifyOk` with the revised plan containing
`quickClassifyNeedsDeep -> deepClassify`. That change follows the actual
`SignalsNeedsDeep` result; it is not an explicit application-level replan request.

For repeatable validation without an OpenAI key, from the repository root:

```bash
mvn -f demos/02-embabel-ticket-triage/pom.xml -Dtest=TriagePlanningIntegrationTest test
```

This starts a real HTTP server and real Embabel planner, mocking only LLM
operations. It asserts the changed framework plans, executed action, and goal
completion, not just application trace messages.

## Tech Stack

- Embabel 1.5.x
- Spring Boot 4.1.x (Java 21+)
- OpenAI gpt-5-mini
