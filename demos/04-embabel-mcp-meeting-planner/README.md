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
   Expected: Basel and Zurich return normal booking results; Bern demonstrates constrained-capacity behavior.

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

## Requests

- Use `requests.http` for the live walkthrough.
- Recommended order: Basel happy path, Zurich video meeting, Bern capacity edge case.
- Endpoint: `POST /api/meetings/plan`

## Fast Validation

```bash
cd /path/to/presentation-stop-prompting-start-planning-with-embabel
./run-demo-acceptance.sh
```

This validates location-mcp packaging, planner startup, and both canonical meeting requests.

## Acceptance

From the repository root:

```bash
./run-demo-acceptance.sh
```

## Tech Stack

- Embabel 0.4.x
- Spring Boot 3.5.x
- Spring AI (MCP Server)
- OpenAI gpt-5-mini
- MCP (Model Context Protocol)
