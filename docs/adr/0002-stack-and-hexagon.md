# 0002. Java 25 + Spring Boot server with a framework-free domain; Expo client

- Status: Accepted
- Date: 2026-11-25
- Pattern(s): Hexagonal architecture / Ports & Adapters, Dependency Inversion

## Context
Version 1 is rules and data (item cards, statuses, combination rules, avatar fit) with no model calls; later
versions add a vision model, weather, and an avatar on screen. The model key must never reach the phone
(intent 01). Aika's other projects (`learning-exchange`, `signal-bot`) use Java 25 + Spring Boot and Expo, so one
stack and one shape are learned twice. Cloud agents work without a screen: whatever can be decided in code must be
testable without one.

## Decision
- `domain/` (Gradle module `:domain`, package `wardrobe.domain..`): items, statuses, combination rules, card
  parsing rules, avatar proportions and fit. Plain Java: no Spring, no Jackson, no I/O, no clock. Ports (interfaces
  the domain needs, such as `ItemCatalog`) live here.
- `app/` (`:app`, package `wardrobe.app..`): Spring Boot adapters — REST (`GET /items` …), in-memory catalogue,
  later the model client behind a port. Depends on `:domain`, never the other way round. ArchUnit enforces it
  (`app/src/test/java/wardrobe/app/ArchitectureTest.java`).
- `api/openapi.yaml` is the REST contract; the API tests in `app/src/test/java/wardrobe/app/` (`ItemsApiTest`
  and one per endpoint after it) check every response body against it over real HTTP. Contract first.
- `client/`: Expo (React Native, TypeScript). Pure logic lives in `client/src/*.ts` with no React import and is
  tested with `node --test`; screens are thin. The client never holds a model key.

## Why this and not the alternatives
| Alternative | Why not (now) |
|---|---|
| Logic in the client only (no server) | The model key would have to live in the app; nowhere to keep rules shared with later clients |
| Layered Spring everywhere | Rules coupled to Spring; slower, less deterministic tests in the cloud |
| Kotlin / another stack | A second stack to learn; the portfolio shows one line across projects |

## Pros
- Domain tests are fast and deterministic: cloud agents can check everything but the look.
- A model client, a database or a second screen is a new adapter, not a change in the rules.

## Risks and costs
- Mapping domain ↔ JSON is extra code. Port explosion: a port exists only with two implementations (prod + test).
- Two languages (Java, TypeScript): a rule shown on screen must not be re-implemented in the client — the server
  answers, the client displays (one rule in one place).

## How to explain it (teaching note)
"The wardrobe's rules know nothing about HTTP or Spring; REST, storage and the future model client are adapters.
That is why agents could build and test it for a week without a screen."
