# Demo 4: Embabel MCP Meeting Planner

## Overview

- Duration: 8 min
- Key concepts: MCP integration, tool groups, multi-module setup, stdio tool calling
- Goal: plan and book meeting rooms through MCP tools
- Reliability note: the MCP server process writes logs to stderr so stdio remains valid JSON-RPC

## What To Show

- MCP tools exposed by one module and consumed by another
- Tool-group wiring from Spring AI MCP into Embabel planning
- Why stdout versus stderr matters for stdio-based MCP reliability

## Demo Flow

1. Open `location-mcp/LocationService.java` and show the three MCP tools:
   - `all-locations`
   - `check-room-availability`
   - `book-room`
   Explanation: This module is the external MCP server that exposes room operations.
   Expected: Tool signatures map directly to location lookup, capacity check, and booking.
2. Open `planner-agent/MeetingPlannerAgent.java` and show the three actions:
   - `findLocation`
   - `findRoomAtLocation`
   - `bookRoom` as `@AchievesGoal`
   Explanation: The agent composes MCP tool calls through typed Embabel actions.
   Expected: `bookRoom` is the terminal planning goal.
3. Open `planner-agent/McpToolsConfig.java` and show `ToolGroup` registration.
   Explanation: MCP clients are grouped and attached to AI calls by name.
   Expected: The planner agent references the `location` tool group.
4. Run 3 request flows from `requests.http`:
   - Basel (6 people): location -> room -> booking
   - Zurich (4 people, video): location -> room -> booking
   - Bern (15 people): constrained capacity path
   Explanation: Two happy paths and one capacity-constrained path show real tool-driven behavior.
   Expected: Basel and Zurich return normal booking results; Bern's only room is
   too small, so the room-finding action must use the tools to find Basel's
   20-person Dreiländereck room. This is tool recovery within an action, not a
   changed GOAP action plan.

## Architecture

| Component | Module | Role |
| --- | --- | --- |
| `LocationService` | location-mcp | MCP tool provider |
| `McpToolConfig` | location-mcp | Tool callback registration |
| `MeetingPlannerAgent` | planner-agent | Embabel actions using MCP tools |
| `McpToolsConfig` | planner-agent | MCP clients grouped as `location` |
| `MeetingController` | planner-agent | REST endpoint and invocation |

## Prerequisites

- Java 21+
- Maven
- OPENAI_API_KEY

## Build & Run

```bash
cd demos/04-embabel-mcp-meeting-planner/location-mcp
mvn package -DskipTests

cd ../planner-agent
export OPENAI_API_KEY=your_key_here
mvn spring-boot:run
```

Application starts on `http://localhost:8080`.

## Logging

The planner console shows Embabel planning, replanning reasons, action execution,
tool calls, and goal completion through the `Embabel` logger at `INFO`, with
framework diagnostics under `com.embabel` at `DEBUG`.
MCP client/server lifecycle logs remain at `INFO` to avoid verbose JSON-RPC tracing.

The location server logs tool invocations to stderr through `logback-spring.xml`.
The planner's `StdioClientTransport` logger forwards these at `INFO` as
`STDERR Message received` entries, so both sides are visible in one console
without corrupting the JSON-RPC stream on stdout.

Each REST invocation enables detailed planning with `Verbosity.showPlanning()`.
Prompt/response verbosity remains unchanged. Use synthetic meeting data because
tool and diagnostic logs can include request/response details.

## Requests

- Use `requests.http` for the live walkthrough.
- Recommended order: Basel happy path, Zurich video meeting, Bern capacity edge case.
- Endpoint: `POST /api/meetings/plan`

## Fast Validation

```bash
cd /path/to/presentation-stop-prompting-start-planning-with-embabel
./run-demo-acceptance.sh
```

This validates location-mcp packaging, planner startup, and all three canonical
meeting requests, including a real-capacity alternative for the Bern workshop.
The runner checks framework plans, action execution, and goal achievement.
The room-finding action rejects undersized suggestions; the MCP booking tool
rejects nonexistent rooms instead of generating a success-shaped booking.
Booking responses preserve display names and scheduling details from the
selected `SuggestedRoom`; the booking tool returns IDs rather than those names,
so the final LLM response must not be trusted to reconstruct them.

## Acceptance

From the repository root:

```bash
./run-demo-acceptance.sh
```

## Tech Stack

- Embabel 1.5.x
- Spring Boot 4.1.x (Java 21+)
- Spring AI 2.0.1 (MCP Server)
- OpenAI gpt-5-mini
- MCP (Model Context Protocol)
