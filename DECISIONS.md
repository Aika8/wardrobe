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

- **27 November — nothing waits for Aika, also later**: the analyst, user and summary runs decide every open
  question (recommendation, else the option easiest to undo) and turn it into a proposal → task; the summary lists
  them under «Решено без тебя — посмотри после отпуска» and may add `proposals/<date>-summary.md`. Undo: the prompts
  and the summary rule in `check_paths`.
- **27 November — `check-paths` does not read the text of binary files** (a screenshot PNG made git mode crash;
  option б of the summary of 26 November). Not taken: decoding with `errors="replace"`; keeping the `--files`
  workaround. Undo: `added_text` in `backlog.py`.
- **27 November — the race of two status changes of the same item** (tech debt `2026-11-26-WR-007-1.md`): nothing
  now; optimistic versions come with real storage and its ADR (the recommendation). Not taken: `replace(expected,
  changed)` on the port now. Risk accepted: one user, data in memory.
- **27 November — tech debt into tasks**: the web padding bug first (WR-159); the chip's status dot (WR-160, the
  mockup's look) and the list screen from the components (WR-161) at the end.

- **27 November — tech debt becomes tasks by itself** (ported from learning-exchange PR #112): intake reads the
  day's proposals, every `docs/backlog/tech-debt/*.md` and open `tech-debt` issues (owner/collaborators) in the
  proposal format; `kind: bug` → `priority: bug`, picked first; an issue is closed once all its proposals are tasks.
  The automerge sweep runs it inline (kept instead of dispatching intake.yml). Maven 429: Gradle retries 8 times from
  3 s (`gradle.properties`). Undo: `intake_candidates` / `--issues` in the workflows; delete `gradle.properties`.

### After the holiday (not in the queue: needs money, keys, accounts or Aika herself)
- Optimistic versions against concurrent status changes (with storage). A weather service; a photo try-on avatar (paid model or GPU); adopting a background-removal option (after the
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
- **WR-009, empty or blank `?status=`** means no filter (every item), as Spring binds a blank enum parameter to
  absent; `?status=clean` (other case) and `?status=CLEAN,IN_WASH` (several) are 400 `BAD_REQUEST`. Not taken: 400 for
  a blank value (a custom converter for one parameter); several statuses at once (not asked). Undo: a check in
  `ItemsController.items`.
- **WR-009, where the filter lives**: a one-line equality filter in `ItemsController`, not a domain method — it is a
  query, not a wardrobe rule. Undo: an `ItemCatalog.inStatus(...)` if a second caller appears.
- **WR-010, blank id on `/items/{id}/matches`** answers 400 `BAD_REQUEST`, as `PUT /items/{id}/status` does; the
  contract lists that 400. Not taken: 404 (a blank id cannot name an item, and `ItemId` refuses it). Undo: the check
  in `MatchesController.matches` and the `'400'` response in `api/openapi.yaml`.
- **WR-010, "clean" means `Item.availableForOutfit()`**, the domain's existing rule, not a status comparison in the
  controller. `MatchConfig` exposes `Combination.standard()` as a `CombinationRule` bean, so a test or a later rule
  set swaps it in one place. Undo: `wardrobe.app.MatchConfig`.
- **WR-011, `.expected` seasons** are written `WINTER,AUTUMN` (enum order, comma, no space); "two values for one
  key" is the same key on two lines with different values (fixture 08), not `colour: red, green` on one line. Not
  taken: `, ` with a space (one more thing to trim when comparing); a one-line pair (seasons already use commas, so
  the parser would read a comma two ways). Undo: a new fixture of the other shape in `cards/` (the files are
  append-only) and a line in `docs/cards.md`.
- **WR-012, seasons with one unreadable word** (`winter, monsoon`) are unknown as a whole (empty set), not the
  readable part — a partial list would be a guess. A key given twice alike (ignoring case) counts once; given twice
  differently it is unknown already here, so no run ever has "the first one wins" (WR-014 adds the doubts list).
  Not taken: keeping the readable seasons. Undo: `CardParser.seasons` and `CardParser.single`.
- **WR-013, scope of the Russian word table** (`wardrobe.domain.card.RussianWords`, the only place Russian card
  words live): for every enum word of `docs/cards.md`, one Russian word or phrase, matched exactly after trimming,
  lower-casing and reading «ё» as «е». Colours in all four adjective forms (`синий/синяя/синее/синие`); `NAVY` is
  `тёмно-синий`. Types: `верх, низ, платье, верхняя одежда, обувь, аксессуар`. Patterns: `однотонный, в полоску,
  в клетку, цветочный, в горошек, с принтом`. Fabrics as nouns: `хлопок, лён, шерсть, шёлк, деним, кожа, трикотаж,
  синтетика`. Seasons: `зима, весна, лето, осень`. Formality: `спортивный, повседневный, смарт-кэжуал, деловой,
  официальный`. Not taken: stems or fuzzy matching (`хлопковый`, `синеватый` would be guesses — they stay unknown),
  other case/gender forms of the non-colour words (the model is asked for one word; add a line when a real answer
  needs it). English and Russian may mix in one `seasons` line. A key given twice as `blue` and `синий` is still
  "two different values" → unknown (WR-012's raw comparison; WR-014 owns doubts). Undo: edit the map.
- **WR-014, shape of a doubt.** `Doubt(key, raw, why)` with `key` the lower-case card key and `raw` the trimmed text
  the answer gave; for `MISSING` `raw` is empty; for `CONFLICTING` it is every given value in answer order joined
  by ` | ` (a comma would clash with `seasons`). A key present with an empty value (`name:`) is `MISSING`, not
  `UNREADABLE` — there is nothing to read. `blue` and `синий` for one key stay `CONFLICTING` (raw text compared,
  as in WR-012/013), not merged by meaning. Doubts come in the key order of `docs/cards.md`. `CardParser.single`
  became `CardParser.field`, which also records the doubt. Not taken: `List<String>` for raw (one shape for all
  three reasons is simpler to show); unreadable seasons word-by-word (the whole line is one doubt, like the whole
  line is unknown). Undo: `Doubt`, `CardParser.field`.
- **WR-015, UNKNOWN colour/pattern/fabric and confirming.** Not confirmed as such in version 1: `ConfirmCard`
  throws `IncompleteCard` listing them, like a missing type or name, because the combination rules cannot judge an
  unknown colour, pattern or fabric. A correction of `UNKNOWN`, or a blank name, counts as "not set" (`Corrections`
  drops it), so the draft value is kept; an empty seasons correction keeps the draft's seasons. Keys in
  `IncompleteCard` come in the key order of `docs/cards.md`; names are trimmed. Not taken: an `allowUnknown` flag
  (no caller needs it yet). Undo: the `UNKNOWN` checks in `ConfirmCard.confirm`.
- WR-016: how is an out-of-range measurement rejected "with the field's name"? Choice: `InvalidMeasurement`
  (extends `IllegalArgumentException`) with `field()` equal to the record component's name (`"waist"`), message
  `"<field>: <value> mm is outside <min>–<max> mm"`. Ranges are inclusive at both ends. The "arm/inseam shorter
  than height" check is unreachable with today's ranges, so it is a package-private guard tested directly.
  Not taken: a plain `IllegalArgumentException` (callers would parse the message for the field); collecting all bad
  fields at once (no caller needs it yet). Undo: `Measurements` and `InvalidMeasurement` in `domain/avatar`.
- **WR-017, where the lines of a flat figure are** (`wardrobe.domain.avatar.Figure`, draft — вид проверяет Aika).
  Neck at 130‰ and shoulder line at 180‰ of the height from the top of the head; chest, waist, hips at 250‰, 550‰,
  850‰ of the torso (shoulder line to crotch) below the shoulder line; knee and ankle at 380‰ and 920‰ of the
  inseam below the crotch (rough adult proportions). Shares of the torso and of the inseam, not of the height, so
  the landmarks stay ordered for every valid `Measurements` (shortest torso: height 1300, inseam 1000). Half-width
  of a girth = `girth * 100 / 314 / 2`, both divisions rounding down: the girth is read as a circle (π ≈ 3.14).
  Source of error: a real torso is an ellipse wider than deep, so the circle gives a front width roughly 10–20 %
  too narrow at chest and hips; rounding loses at most 1 mm. Shoulders are already a width: half of it. Not taken:
  an ellipse with a fixed depth ratio (a second invented number to tune before Aika has seen the figure); fixed
  shares of the height for chest/waist/hips (could put the hips below the crotch for a long inseam). Undo: the
  constants and `halfWidthOfGirth` in `Figure`.
- **WR-018, garment sizes and the fit check** (`wardrobe.domain.avatar.Fit`, `GarmentSize`, `FitNote`). Eases and
  tolerances, starting values for Aika to tune, all as constants in `Fit`: minimum ease chest 40, waist 20, hips 30;
  maximum ease 200 for every girth; sleeve (against the arm) and inseam tolerance ±30 mm. Exactly at a limit fits;
  one millimetre past it gives the note. Parts checked per type: TOP chest, waist, sleeve; BOTTOM waist, hips,
  inseam; DRESS and OUTERWEAR chest, waist, hips, sleeve; SHOES and ACCESSORY nothing. `length` is stored but gives
  no note (the task names no length note and the body has no matching measure). A note names its kind and its part
  (`FitNote(kind, part)`), so "too tight" says where. Absent fields are `OptionalInt.empty()`; a present value must
  be positive. Not taken: nullable `Integer` fields (easy to unbox a null by mistake); a separate ease table class
  (one table in one place is enough). Undo: the three files in `domain/avatar`.
- **WR-019, drawing order on the avatar** (`wardrobe.domain.avatar.Layers`). `OutfitCheck.check` needs a
  combination rule, but the two refusing problems (`TwoOfAKind`, `DressWithSeparates`) do not depend on it, so
  `Layers` passes a private rule that lets every pair go and keeps only those two kinds; the outfit check stays the
  one place that decides them. An empty outfit is not refused: it gives an empty order (nothing to draw). TOP and
  DRESS share one layer (an outfit never has both). Not taken: `Layers.order(Outfit, CombinationRule)` (the task
  names one argument, and a clash is drawn anyway); refusing an empty outfit (the avatar can stand undressed).
  Undo: `Layers.java`.
- **WR-020, where a garment's edges fall** (`wardrobe.domain.avatar.Placement`). Questions the task left open:
  ACCESSORY has no placement (throws; it is not drawn on a body line). SHOES go from the ankle to the floor and
  ignore `length` (a shoe's chart length is the foot), as wide as the figure's hips. Width at an edge comes from the
  body part at that height (above the waist line chest, above the hip line waist, else hips); a hem below the hips
  keeps the hip width (flat draft). `Figure` holds half-widths, not girths, so without a garment girth the width is
  body half-width + `Figure.halfWidthOfGirth(min ease)` — at most 1 mm off reading the eased girth (tested). Sleeve
  end is shoulders + the size's sleeve, clamped to the floor (sets `clamped`); without a sleeve it stays unknown
  (`OptionalInt.empty()`), because the figure has no arm length and invariant 5 forbids guessing. Mid-thigh is
  `(crotch + knee) / 2`. Not taken: a sleeve default from the arm (would need `Measurements`, not in the task's
  signature); adding girths to `Figure` (changes WR-017's record). Undo: `Placement.java`.
- **WR-021, `POST /avatar/dress`** (`wardrobe.app.web.AvatarController`). Sizes travel in the request
  (`items[].size`), as the task says: version 1 stores no sizes, so nothing about storage is decided here; an
  absent `size` is a size with every field unknown. Contract choices: the figure is `{landmarks: {head…floor},
  halfWidths: {shoulders, chest, waist, hips}}`; a layer is `{itemId, type, shape, fit}`; `shape` is left out for an
  ACCESSORY (`Placement` has none) and `sleeveEnd` is left out when unknown, rather than `null`. A new `Error` code
  `IMPOSSIBLE_OUTFIT` for the 409 (the existing codes mean other things); its message is `Layers`' own list of
  problems. 400s keep `BAD_REQUEST` and the message starts with the field's path (`measurements.waist`,
  `items[1].size.inseam`) instead of adding a `field` property to `Error`. Numbers are read as JSON numbers and a
  fraction or a value beyond `int` is a 400 (Jackson would otherwise cut 1650.5 to 1650). The same item twice is two
  of a kind (409), from `Layers`. Validation order: measurements, then each item (id, size, known), then the outfit.
  CORS now allows POST; request headers stay Spring's default (any), which covers `Content-Type`. Not taken: storing
  sizes with the item (a storage format, needs its own task); `null` for absent fields (the contract would have to
  allow null everywhere). Undo: `AvatarController.java`, the `/avatar/dress` path and its schemas in
  `api/openapi.yaml`, `IMPOSSIBLE_OUTFIT` in `ErrorView`/`ApiErrors`, `"POST"` in `CorsConfig`.
- **WR-022, debatable Russian labels** (`client/src/labels.ts`): `SMART_CASUAL` → «смарт-кэжуал» with the hint
  «аккуратно, но не строго: между повседневным и деловым» (`FORMALITY_HINT`), not «умеренно-официальный» (not
  plain); `FORMAL` → «торжественный» (not «официальный», too close to «деловой»); `BLUE` → «синий», `NAVY` →
  «тёмно-синий» (not «голубой»: lighter than the colour friends assume); `DENIM` → «джинса» (not «деним»);
  `TOP`/`BOTTOM` → «верх»/«низ»; `UNKNOWN` → «цвет не указан», «узор не указан», «ткань не указана» (says the
  field is unknown, guesses nothing). A value without a word is shown as the API sent it (`label()`). Undo: edit
  the tables in `client/src/labels.ts`.
- **WR-023, chip words and the «Все» count**: the status chips read «Чистое», «В стирке», «В ремонте», «В поездке» —
  `STATUS_LABEL` with a capital first letter (`filterChips` in `client/src/filter.ts`), so the words live in one
  place; «Все» carries the total count, as in the mockup's «Где вещи сейчас». The chips wrap onto a second line
  (`flexWrap`, like the mockup's `.chips`) rather than scroll sideways. Not taken: a second table of capitalised words
  (two places for one word); «Все» without a count (the task's line omits it, the mockup has it). Undo:
  `filterChips` and the `chips` style in `client/App.tsx`.
- **WR-024, rows of the item details** (`describeItem` in `client/src/details.ts`): type, colour, pattern, fabric,
  seasons, formality — in that order, labelled «Тип, Цвет, Узор, Ткань, Сезоны, Стиль». The name is the screen
  title and the status the `StatusBadge` at the top (as in the mockup's item card), so neither is a row; `id` is not
  shown. Formality is labelled «Стиль» (plainer than «Формальность») and carries `FORMALITY_HINT` as an optional
  `hint` under the value. Unknown season words follow the known ones as they came. Not taken: a status row too (the
  badge already says it); the mockup's attribute chips (the task asks for label/value rows). Undo: `describeItem`.
- **WR-024, the way back and the background**: a quiet `Button` «‹ Назад» (DESIGN.md has no back control; the
  mockup's bare «‹» is not a component), and the details screen sits in the same `Screen` as the list (WR-159:
  `paper`, 16 px margins). Opening is plain state in `App.tsx` (`openId`), no navigation library. Undo: `App.tsx`.
- **WR-024, screenshot of the details**: `npm run screenshot` only shows the list and cannot tap, so the PNG was
  taken with a throwaway copy of the script that taps the last row of the list first (not committed). Undo: nothing.
