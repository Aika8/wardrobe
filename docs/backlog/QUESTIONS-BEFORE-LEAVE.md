# Aika's answers before leaving (25 November 2026)

Agents read this before deciding anything themselves (README, "Rule for every run").

- **Name**: `wardrobe`, «Гардероб».
- **Stack**: as in the other projects — server Java 25 + Spring Boot, client Expo (ADR-0002).
- **Week's scope**: version 1 with no model calls — the item model (type, colour, pattern, fabric, season,
  formality); statuses clean / in the wash / in repair / on a trip; combination rules by colour and formality with
  tests; parsing an item card from a **saved** model answer (text fixtures, no photos). Plus the Expo screen with
  the item list. Plus a draft avatar that can be dressed: a flat figure from proportions (measurements), clothing
  layers, the fit logic under tests.
- **Look**: the look of the avatar and of the screens is checked by Aika after the trip (after 8 December).
  Agents build drafts, test the logic behind them, and say «вид проверяет Aika» in the PR.
- **Photos**: Aika's own photos do not exist in the repository and never go to the cloud. No photo fixtures.
- **Money**: nothing beyond the subscription — no API keys, no purchases, no sign-ups; Actions in the free minutes.
- **Models**: Opus only for every run; never Sonnet.
- **Language of the daily summary**: Russian.
