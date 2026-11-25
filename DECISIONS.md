# Decisions

Small behaviour choices, one bullet each, newest section last. Costly-to-reverse decisions are ADRs (`docs/adr/`).

## Version 1 skeleton (25 November 2026)

- **Status transitions** (`ItemStatus.next()`): clean → wash, repair, trip; wash → clean, repair (a hole found while
  washing); repair → clean, wash; trip → clean, wash. Wash → trip and repair → trip are refused: an item goes on a
  trip clean. Staying in the same status is not a change (refused). Undo: edit the table and `ItemStatusTest`'s
  copy of it in a new test.
- **Formality rule**: two items combine when their formality differs by at most one step of
  SPORT < CASUAL < SMART_CASUAL < BUSINESS < FORMAL (`FormalityRule.MAX_STEPS`).
- **Colours** are a fixed palette with `UNKNOWN` (no measured colours in version 1); pattern and fabric have
  `UNKNOWN` too, for cards a model answer left unclear (intent 01, invariant 5). Type and formality have no
  `UNKNOWN`: a confirmed item must have them.
- **JSON of an item** uses the enum names as they are (`IN_WASH`, `SMART_CASUAL`); the client maps them to Russian
  words. Seasons in calendar order of the enum (winter first).
- **The client sorts** clean items first, then wash, repair, trip, then by name — display order, not a rule.
- **CORS**: the server allows GET from `http://localhost:*` (the Expo web build); WR-008 adds PUT and WR-021 POST, each with a preflight test.
- **The backlog run is a copy** of `Aika8/learning-exchange`'s (`scripts/backlog/backlog.py` with its tests,
  workflows, prompts, `scripts/cloud/setup-env.sh`). Changed for this repository: repository name; Godot removed;
  `VISUAL` = only `docs/design/approved/**` (drafts of screens and the avatar are agent work, marked «вид
  проверяет Aika»); `APPEND_ONLY` for `client/test/**` and `*.test.*`; runs every 2 hours instead of hourly;
  automerge's safety sweep every 3 hours. **Later:** one shared library for the backlog rules used by all three
  projects (learning-exchange, signal-bot, wardrobe), so a fix lands once — its own task after the week.

## Backlog run (December 2026)

<!-- Agents: one bullet per decision taken without Aika — task id, question, choice, options not taken and why. -->
