# wardrobe («Гардероб») — rules for any agent working here

## What this is
Aika's virtual wardrobe: item cards, statuses (clean / in the wash / in repair / on a trip), rules of what goes
together, and a draft avatar that can be dressed. Version 1 has no model calls. Start with `intent/01-wardrobe.md`
and `docs/adr/`.

## Workflow
- Intent → ADR → contract (`api/openapi.yaml`) → code + tests → `DECISIONS.md` for small behaviour choices.
- Costly-to-reverse decisions get an ADR (`docs/adr/0000-template.md`) **before** the code. Never edit an Accepted
  ADR; supersede it.
- Agents commit, push to `claude/*` branches and open PRs without asking; only `.github/workflows/automerge.yml`
  merges, after green CI and an independent review. The cloud backlog run: `docs/backlog/README.md`.

## Code conventions
- `domain/` is plain Java: no Spring, Jackson, I/O or clock calls (ArchUnit test in `app/`). Spring only in `app/`.
- One rule lives in one place. A combination or fit rule is decided on the server (domain); the client shows the
  answer and never re-implements it.
- Sizes and measurements are integer millimetres; no floating point for anything compared.
- A field a model answer lacks or garbles stays unknown; never guess it (intent 01, invariant 5).
- A class implementing a named pattern starts with `Pattern: <name> — <why>. See docs/adr/NNNN-….md`.

## Look is Aika's
- Screens and the avatar are drafts until Aika has looked at them. Tasks that change how something looks are
  marked «вид проверяет Aika» in `docs/backlog/QUEUE.md`; their logic is tested, their look is not claimed to be
  right. Approved designs, once they exist, go to `docs/design/approved/` and nobody but Aika changes them.

## Running and checking
- Everything: `make test` (Gradle tests, client tests and type check, backlog rules). CI runs the same.
- Server: `make dev` (port 8080). Agents use another port (`make dev PORT=8091`) and stop it before finishing.
- Client: `cd client && npx expo start` (needs the server; `EXPO_PUBLIC_API_URL`, default `http://localhost:8080`).

## Don'ts
- No model API key in the client, in the repository, or in a fixture. When model calls come, the key lives only on
  the server, from the environment.
- Photos of Aika and of her items are personal data: never committed, never sent to the cloud. Fixtures are text.
- No real brands (names, logos, shop names) in code, fixtures or copy.
- No AI-attribution lines in commits or PR descriptions.
