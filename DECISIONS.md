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

## The week's queue: decided without Aika (26 November 2026)

Aika: «меня ничего не должно ждать» — every open question of the queue was decided by the recommended option (or the
one easiest to undo). Each line: the choice — what was not taken — how to undo.

- **Scope** (intent 01): wear log, statuses over time, trips, the «стоит ли покупать» check of an item the person
  describes, suggestions by occasion and by weather given as values, and the approved screens are in scope; a weather
  service and choosing purchases for the person are not. Undo: edit «Out of scope» in intent 01.
- **Daily intake cap removed**: every proposal that passes the filter becomes a task at once (automerge's sweep also
  runs intake). Not taken: the cap of 5. Undo: restore `MAX_INTAKE_PER_DAY` in `backlog.py` and its test.
- **Dates in the domain** are `wardrobe.domain.Day` (epoch day, WR-066), not `java.time`. Not taken: allowing
  `LocalDate` in the ArchUnit rule. Undo: allow it and replace `Day` (one type).
- **"Today"** is the server clock in Asia/Almaty (WR-077); requests may pass a date. Not taken: the phone always
  sends its date. Undo: change the `Clock` bean.
- **Wearing does not change the status** (WR-071 only logs). Not taken: auto-wash after N wears; the client asking
  «в стирку?». Undo: a rule in `LogWear`.
- **Logs start empty in memory** (wear, status history, prices); everything is lost on restart. Not taken: a demo
  seed; persistence (needs an ADR — after the holiday). Undo: seed or an ADR.
- **Starting tables are the tasks' values**, to be tuned by Aika: occasion ranges (OFFICE SMART_CASUAL–BUSINESS,
  WEEKEND CASUAL–SMART_CASUAL, EVENING SMART_CASUAL–FORMAL, SPORT SPORT–CASUAL; four occasions), weather bands
  (≤0, 1–10, 11–18, 19–25, ≥26 °C), at most 2 accent colours and 3 accessories, ranking points, trip counts and
  temperature bands, colour families, «стоит ли покупать» thresholds. Undo: the constants named in each task.
- **Patterned accessory**: `PatternRule` stays as is (two patterned items never pair, accessories included). Not
  taken: an accessory exemption. Undo: a new outfit rule (later).
- **Fabric weight** is used only against the weather, not as a pair rule. Undo: add a pair rule.
- **A dress** sits under «Верх» in the builder's tabs. Not taken: a sixth tab «Платья» (changes the look). Undo:
  `client` tab table (WR-104).
- **Archive instead of delete** (WR-108, WR-118). Not taken: a real delete. Undo: add a delete endpoint.
- **«Похожие уже есть»** on adding an item reuses the similar-items rule of «стоит ли покупать» (WR-068, WR-157):
  one rule, a warning, never a refusal.
- **A confirmed item may not keep an UNKNOWN colour, pattern or fabric** (as WR-015 recommends). Undo: allow it in
  `ConfirmCard`.
- **Item ids are readable slugs** from the name (WR-096). Not taken: random ids. Costly to change once real items
  exist — revisit before that.
- **Weather on the phone** is picked by the person from presets on `WeatherStrip`. Not taken: a weather service
  (may need an account — after the holiday). Undo: replace the presets with a call.
- **`TextField` added to the component set** (DESIGN.md and the component sheet), so the add-item screen and naming
  a collection can be built (WR-151…). Not taken: chips only. Undo: drop the component and those tasks.
- **The central «+»** opens the outfit builder until the add-item screen lands. **«Хочу»** opens «Нужно ли?».
  **«Сохранить»** in the builder logs the outfit as worn today. **The builder's tool row** is left out. **No name**
  in the greeting. The doll gets **CategoryTabs** to choose what to try on (WR-156).

### After the holiday (not in the queue: needs money, keys, accounts or Aika herself)
- A weather service; a photo try-on avatar (paid model or GPU); adopting a background-removal option (after the
  WR-034 report); persistence (ADR); the shared backlog library for the three projects.

## Backlog run (December 2026)

<!-- Agents: one bullet per decision taken without Aika — task id, question, choice, options not taken and why. -->

- **WR-002, colour friends** (`ColourRule.FRIENDS`): which two non-neutral colours go together? Starting table
  from the task: BLUE–YELLOW, BLUE–ORANGE, BLUE–PINK, GREEN–PINK, PURPLE–YELLOW, RED–PINK; any other pair of
  different non-neutral colours does not go. Not taken: a colour-wheel formula (harder for Aika to read and edit).
  Undo: edit the set and the same list in `ColourRuleTest` (it pins every pair of different non-neutral colours).
- **WR-005, rule names in a "goes" verdict** (`Combination.name`): the task fixes the reason "goes: formality,
  colour, pattern, season" but rules carry no name. Choice: the class name without "Rule", lower case
  (`FormalityRule` → "formality"); a nested combination → "combination"; a lambda → "rule N". Not taken: a `name()`
  on `CombinationRule` (outside the task's paths) or a name map in the constructor (the task asks for a plain
  `List<CombinationRule>`). Undo: change `Combination.name`.
- **WR-006, an outfit with no BOTTOM and no DRESS** (`OutfitCheck`): not a problem in version 1, as the task
  says (a top and shoes, or only accessories, pass). Undo: add a problem kind in `OutfitProblem` and a check.
- **WR-006, three or more of one type** (`OutfitCheck`): each extra item is reported as `TwoOfAKind(type, first,
  extra)`, in input order, so three shoes give two problems. Not taken: one problem per type (the record holds two
  items) or every pair (noisier). Undo: change the loop in `OutfitCheck.check`.
- **WR-006, messages** of `OutfitProblem` are English plain words naming items by `name()`, like the rules'
  reasons; the client's Russian copy is not decided here. Undo: change `message()`.
- **WR-035, `playwright-core` version**: pinned exactly (`1.65.1`, installed with `--ignore-scripts`, so nothing
  downloads a browser), dev dependency only. Not taken: a caret range (a minor bump can change the CDP protocol the
  local Chrome speaks). Undo: edit the version in `client/package.json`.
- **WR-035, "waits for the list"**: the screenshot is taken once a status label (`чистое`, `в стирке`, …) shows; if
  the load-error text shows instead, the script fails without writing a PNG. Not taken: a `testID` on the list (would
  touch `App.tsx`, outside the task's paths). Undo: change the `waitForFunction` in `client/scripts/screenshot.mjs`.
- **WR-035, server log**: when the script starts `make dev PORT=8091` itself, its output goes to a temp log whose last
  25 lines are printed if the server dies before answering. Not taken: inheriting stdout (floods the output). Undo:
  `stdio` in `startServerIfNeeded`.
- **WR-028, fonts**: the handwritten `heading` (Caveat) and the `body` font (Nunito) need `expo-font`, a new dependency
  that waits for an ADR. Until then `ScreenTitle` and `SectionTitle` use the system font in bold, and no component
  sets `fontFamily`. Not taken: bundling fonts now (new dependency without an ADR). Undo: set `fontFamily` in
  `client/src/ui/ScreenTitle.tsx`, `SectionTitle.tsx` once the ADR lands.
- **WR-028, values the token block lacks**: the badge fill is the status colour at 20 % (`BADGE_FILL_ALPHA` in
  `client/src/ui/status.ts`; the mockup uses 18–22 %); the 1.5 px outline of a selected `Chip` and a quiet `Button`
  follows the mockup's CSS; `Tape` keeps the mockup's size (84 × 20), offset and turn as part of the drawing and drops
  its paper stripes (a gradient needs more than `View`); `Button` text is `md` (the mockup's 14 px is no token).
  Not taken: new tokens (DESIGN.md is Aika's). Undo: the constants in those files.
- **WR-028, pressing with only View and Text**: `Button` and `Chip` take presses through the View responder props
  (`client/src/ui/press.ts`), since the task allows only `View`, `Text` and `StyleSheet`. Not taken: `Pressable`
  (outside the allowed set). Undo: swap `pressProps` for `Pressable` in those two files.
- **WR-008, 409 message names the item by id**: the message is `IllegalStatusChange`'s own ("item green-dress
  cannot go from ON_TRIP to IN_REPAIR"), so the wording lives in the domain only. Not taken: a second message built in
  `ApiErrors` with the item's display name (two places for one sentence; the exception carries no name). Undo:
  `ApiErrors.illegalStatusChange`.
- **WR-008, same status (CLEAN → CLEAN) is a 409**, as the domain table says ("staying is not a change"). Not taken:
  200 as a no-op (would be a second rule in the web layer). Undo: a check in `StatusController.change`.
- **WR-008, 400 cases**: an unknown status name, `null`, a missing `status`, a body that is not JSON, an empty body and
  a blank id all answer `BAD_REQUEST`. Extra properties in the body are ignored (Spring's default), although the
  schema says `additionalProperties: false`. Not taken: turning on strict JSON reading app-wide (changes every future
  endpoint). Undo: `spring.jackson` deserialization setting.
- **WR-008, `ChangeStatus` wiring**: `StatusController` builds `new ChangeStatus(catalog)` itself. Not taken: a
  `@Bean` (domain stays free of Spring; a config class would be the only user). Undo: move it to a `@Configuration`.
