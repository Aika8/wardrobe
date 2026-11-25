# Гардероб (wardrobe)

Aika's virtual wardrobe. Version 1: item cards, statuses, rules of what goes together, a draft avatar — no model
calls. Intent: `intent/01-wardrobe.md`. Decisions: `docs/adr/`, `DECISIONS.md`. Rules for agents: `CLAUDE.md`.

| Part | Where | Stack |
|---|---|---|
| Rules (domain) | `domain/` | Java 25, no frameworks |
| Server | `app/` | Spring Boot 4 (REST, contract `api/openapi.yaml`) |
| Client | `client/` | Expo (React Native, TypeScript) |
| Cloud backlog run | `docs/backlog/` | queue, prompts, merge gate in GitHub Actions |

## Run

```bash
make test                       # everything CI runs
make dev                        # server on :8080; GET http://localhost:8080/items
cd client && npx expo start     # client (EXPO_PUBLIC_API_URL to point it elsewhere)
```

Needs JDK 25 (Gradle downloads it if missing) and Node ≥ 24.
