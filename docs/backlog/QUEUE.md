# Backlog queue

The work of the cloud agents, one task per run, in file order (`scripts/backlog/backlog.py next` picks the first
ready one). How the run works: `docs/backlog/README.md`. Only Aika edits this file by hand; the intake script
appends to the end.

Format of a task (the script parses it; `backlog.py lint` checks it):

- `### ID — title`, then fields, a blank line, the description (at most ~1500 tokens).
- `status`: `todo` | `waiting` (needs Aika; `waits` says what) | `done` | `dropped`. A task is *done* when the
  PR from its branch `claude/<id in lower case>` is merged; nobody edits the status for that.
- `depends`: ids that must be done first. `needs`: `docker` when the cloud image may lack it.
- `paths`: the only files the PR may change (plus `DECISIONS.md`). Globs: `*` within a folder, `**` across folders.
- `verify`: the command that proves the task; `expect`: what it prints or what must be true.
- A title ending in «вид проверяет Aika» builds a draft of something people look at: its logic is tested, its look
  is Aika's to check after the week. The PR says «draft — вид проверяет Aika» and describes the look in words.
  Screens are built only from the components and tokens of `docs/design/DESIGN.md` (theme «уютный блокнот»,
  mockups in `docs/design/mockups/`). Screen tasks stay `waiting` until Aika says «да» to the mockups; she changes
  their status by hand. Logic tasks never wait for the look.

Rules for every task (the executor prompt repeats them): existing tests are never changed or deleted, new tests are
new files; `domain/` stays plain Java (no Spring, Jackson, I/O, clock); sizes are integer millimetres; a field a
model answer lacks stays unknown; contract changes go to `api/openapi.yaml` first; a wardrobe rule lives on the
server, the client shows it; no model key, no photos, no real brands; `make test` stays green.

Read first, every task: `intent/01-wardrobe.md` (invariants), `docs/adr/0002-stack-and-hexagon.md`, the classes
named in the task. Test items are invented (`domain/src/test/java/wardrobe/domain/TestItems.java`; add your own
helper in a new file when you need other fields — never edit an existing test file).

## Combination rules (intent 01, invariants 3 and 4)

### WR-001 — A colour knows whether it is neutral
- status: todo
- size: S
- paths: `domain/src/main/java/wardrobe/domain/Colour.java`, `domain/src/test/java/wardrobe/domain/ColourTest.java`
- verify: `./gradlew :domain:test --tests 'wardrobe.domain.ColourTest'`
- expect: green; every colour of the enum is covered by the test

Add `public boolean neutral()` to `Colour`: BLACK, WHITE, GREY, NAVY, BEIGE, BROWN are neutral; every other colour
is not; UNKNOWN is **not** neutral (we cannot vouch for a colour we do not know — intent 01, invariant 5). Keep it
a field of the enum constants (constructor argument), not a switch somewhere else, so a new colour cannot be added
without deciding. `ColourTest` lists the expected value for **every** constant and fails if a constant is missing
from its table (iterate `Colour.values()`).

### WR-002 — Colour rule: neutrals go with anything, two colours only when they are friends
- status: todo
- depends: WR-001
- size: S
- paths: `domain/src/main/java/wardrobe/domain/match/ColourRule.java`, `domain/src/test/java/wardrobe/domain/match/ColourRuleTest.java`, `domain/src/test/java/wardrobe/domain/match/ColourItems.java`
- verify: `./gradlew :domain:test --tests 'wardrobe.domain.match.ColourRuleTest'`
- expect: green; symmetry and "an item goes with itself" are checked over every pair of colours

`ColourRule implements CombinationRule` (see `FormalityRule` for the shape). Verdict, in this order:
- the same item (same `ItemId`) → yes: an item always goes with itself (intent 01, invariant 3);
- either item's colour is UNKNOWN → no, reason says which item's colour is unknown;
- either colour is neutral → yes;
- same colour → yes;
- the unordered pair is in one constant `FRIENDS` → yes. Starting table (Aika revises it later; write this choice
  in `DECISIONS.md`): BLUE–YELLOW, BLUE–ORANGE, BLUE–PINK, GREEN–PINK, PURPLE–YELLOW, RED–PINK;
- otherwise no, reason names both colours.

Tests in `ColourRuleTest` (helper `ColourItems.item(id, Colour)` in its own new file): the cases above; over all
pairs of `Colour.values()` on two **different** items: `judge(a,b).goes() == judge(b,a).goes()`; two different items
of the same known colour go together, two different items of colour UNKNOWN do not; every item, UNKNOWN colour
included, goes with itself (same `ItemId`).

### WR-003 — Pattern rule: at most one patterned item in a pair
- status: todo
- size: S
- paths: `domain/src/main/java/wardrobe/domain/match/PatternRule.java`, `domain/src/test/java/wardrobe/domain/match/PatternRuleTest.java`, `domain/src/test/java/wardrobe/domain/match/PatternItems.java`
- verify: `./gradlew :domain:test --tests 'wardrobe.domain.match.PatternRuleTest'`
- expect: green; symmetric over every pair of patterns

`PatternRule implements CombinationRule`: PLAIN goes with anything; two non-plain patterns do not go together,
except an item with itself (the same `ItemId`: an item always goes with itself, invariant 3). UNKNOWN pattern counts
as non-plain. Reasons in plain words ("two patterns: STRIPED and FLORAL"). Tests: the cases, symmetry over all
pairs, self.

### WR-004 — Season rule: two items share at least one season
- status: todo
- size: S
- paths: `domain/src/main/java/wardrobe/domain/match/SeasonRule.java`, `domain/src/test/java/wardrobe/domain/match/SeasonRuleTest.java`, `domain/src/test/java/wardrobe/domain/match/SeasonItems.java`
- verify: `./gradlew :domain:test --tests 'wardrobe.domain.match.SeasonRuleTest'`
- expect: green; symmetric over all 15×15 non-empty season sets

`SeasonRule implements CombinationRule`: yes when the season sets intersect (reason names a shared season, the
first in enum order), no otherwise (reason names both sets in enum order). Test over every pair of non-empty
subsets of `Season` (15 × 15): symmetric, and goes ⇔ intersection non-empty.

### WR-005 — All rules together: a pair goes only when every rule says yes
- status: todo
- depends: WR-002, WR-003, WR-004
- size: S
- paths: `domain/src/main/java/wardrobe/domain/match/Combination.java`, `domain/src/test/java/wardrobe/domain/match/CombinationTest.java`
- verify: `./gradlew :domain:test --tests 'wardrobe.domain.match.CombinationTest'`
- expect: green; the verdict lists every failing rule's reason, in a fixed rule order

`Combination implements CombinationRule` holding a `List<CombinationRule>`; `Combination.standard()` = formality,
colour, pattern, season (in that order). `judge`: yes when all say yes (reason "goes: formality, colour, pattern,
season"); otherwise no, reason = the failing rules' reasons joined with "; " in rule order. Start the class doc with
`Pattern: Composite — …. See docs/adr/0002-stack-and-hexagon.md`. Tests: one pair failing two rules shows both
reasons in order; symmetry of `standard()` over a set of at least 20 invented items with varied fields.

### WR-006 — An outfit: one item per body slot, every pair goes, all clean
- status: todo
- depends: WR-005
- size: S
- paths: `domain/src/main/java/wardrobe/domain/outfit/**`, `domain/src/test/java/wardrobe/domain/outfit/**`
- verify: `./gradlew :domain:test --tests 'wardrobe.domain.outfit.*'`
- expect: green; each kind of problem has its own test

Package `wardrobe.domain.outfit`: `Outfit` (record, `List<Item>`) and `OutfitCheck.check(Outfit, CombinationRule)`
→ `List<OutfitProblem>` (empty = fine). Problems (a sealed interface of records, each with a plain-words
`message()`): `NotClean(item)` (invariant 1), `TwoOfAKind(type, a, b)` — at most one TOP, BOTTOM, DRESS, OUTERWEAR,
SHOES; ACCESSORY any number; `DressWithSeparates(dress, other)` — a DRESS excludes TOP and BOTTOM; `Clash(a, b,
reason)` for every pair whose verdict is no; `Empty`. An outfit with no BOTTOM and no DRESS is **not** a problem in
version 1 (write in `DECISIONS.md`). Order of the list: as above, pairs in input order.

## Statuses and the catalogue (intent 01, invariants 1 and 2)

### WR-007 — Change an item's status through the catalogue
- status: todo
- size: S
- paths: `domain/src/main/java/wardrobe/domain/ItemCatalog.java`, `domain/src/main/java/wardrobe/domain/ChangeStatus.java`, `domain/src/main/java/wardrobe/domain/UnknownItem.java`, `domain/src/test/java/wardrobe/domain/ChangeStatusTest.java`, `domain/src/test/java/wardrobe/domain/FakeCatalog.java`, `app/src/main/java/wardrobe/app/catalog/InMemoryItemCatalog.java`, `app/src/test/java/wardrobe/app/catalog/**`
- verify: `./gradlew test --tests 'wardrobe.domain.ChangeStatusTest' --tests 'wardrobe.app.catalog.*'`
- expect: green; an illegal change leaves the catalogue unchanged; concurrent changes of different items lose none

Extend the port `ItemCatalog` with `Optional<Item> find(ItemId)` and `void replace(Item)` (replace an existing item
by id; unknown id → `UnknownItem`). Use case `ChangeStatus` (domain, plain Java): `Item change(ItemId, ItemStatus)`
— find or throw `UnknownItem`, `withStatus` (throws `IllegalStatusChange`), replace, return the new item.
`InMemoryItemCatalog`: keep the seed items and their order; make it safe for concurrent requests (e.g. a
`LinkedHashMap` guarded by one lock, or `ConcurrentHashMap` plus an id list) — `all()` returns a snapshot. Tests:
`ChangeStatusTest` with a `FakeCatalog` (new test file); in `app/src/test/java/wardrobe/app/catalog/` a test that
8 threads × 100 alternating CLEAN↔IN_WASH changes on 8 different items end in a consistent state.

### WR-008 — REST: change a status, with clear errors
- status: todo
- depends: WR-007
- size: S
- paths: `api/openapi.yaml`, `app/src/main/java/wardrobe/app/web/**`, `app/src/test/java/wardrobe/app/StatusApiTest.java`, `app/src/test/java/wardrobe/app/StatusCorsTest.java`
- verify: `./gradlew :app:test --tests 'wardrobe.app.StatusApiTest'`
- expect: green; 200 / 404 / 409 / 400 each checked against the contract

Contract first: `PUT /items/{id}/status` with body `{"status": "IN_WASH"}` → 200 and the `Item`; unknown id → 404;
a change `ItemStatus` does not allow → 409; a status that is not one of the four → 400. Errors share one schema
`Error {code, message}` (`code`: `UNKNOWN_ITEM`, `ILLEGAL_STATUS_CHANGE`, `BAD_REQUEST`; `message` in plain English
naming the item and both statuses). Add `next` to the `Item` schema and to `ItemView`: the statuses this item may
change to (`ItemStatus.next()`, enum order) — so the client never re-implements the table. Implement with a
`@RestControllerAdvice` mapping `UnknownItem` and `IllegalStatusChange`. `StatusApiTest` (real HTTP like
`ItemsApiTest`) validates every response body against the contract schema and checks that a 409 leaves the item as
it was (`GET /items`). Extend `CorsConfig` to allow PUT (same local origins) and test the preflight (`OPTIONS` with
`Origin: http://localhost:8081` and `Access-Control-Request-Method: PUT` → allowed). Note: the seed catalogue is shared by tests in one Spring context — use items no other test
asserts on, or `@DirtiesContext`.

### WR-009 — REST: filter the list by status
- status: todo
- depends: WR-008
- size: S
- paths: `api/openapi.yaml`, `app/src/main/java/wardrobe/app/web/**`, `app/src/test/java/wardrobe/app/FilterApiTest.java`
- verify: `./gradlew :app:test --tests 'wardrobe.app.FilterApiTest'`
- expect: green; `?status=CLEAN` returns only clean items, an unknown status gives 400 with `Error`

`GET /items?status=CLEAN` (one optional query parameter, contract first). Without it, everything as before. Test
over real HTTP, bodies validated against the contract.

### WR-010 — REST: what goes with this item
- status: todo
- depends: WR-005, WR-008
- size: S
- paths: `api/openapi.yaml`, `app/src/main/java/wardrobe/app/web/**`, `app/src/main/java/wardrobe/app/MatchConfig.java`, `app/src/test/java/wardrobe/app/MatchesApiTest.java`
- verify: `./gradlew :app:test --tests 'wardrobe.app.MatchesApiTest'`
- expect: green; only clean items other than the item itself are listed, each with the rule's reason

`GET /items/{id}/matches` → `{ "item": Item, "goes": [ {item: Item, reason} ], "doesNot": [ {item, reason} ] }`,
both lists over the **clean** items except the item itself, in catalogue order; verdicts from
`Combination.standard()` (one rule in one place: the controller calls the domain, it does not decide). Unknown id →
404 `Error`. The item itself may be in any status (you may ask what goes with a shirt that is in the wash). Contract
first; test over real HTTP with schema validation; one assertion that a pair two rules reject shows both reasons.

## Item cards from saved model answers (intent 01, invariant 5)

### WR-011 — Saved model answers as text fixtures
- status: todo
- size: S
- paths: `domain/src/test/resources/cards/**`, `docs/cards.md`
- verify: `cd domain/src/test/resources/cards && n=0; for f in *.txt; do test -f "${f%.txt}.expected" || { echo "missing ${f%.txt}.expected"; exit 1; }; grep -q "$f" ../../../../../docs/cards.md || { echo "$f not in docs/cards.md"; exit 1; }; n=$((n+1)); done; echo "$n fixtures"`
- expect: prints `N fixtures` with N ≥ 10; every `.txt` has its `.expected` and is listed in `docs/cards.md`

There are no model calls in version 1. Write what a vision model **would** answer for an item photo, as saved text.
First `docs/cards.md`: the format we will ask the model for — one `key: value` per line, keys `type, colour, pattern,
fabric, seasons, formality, name`, `seasons` comma-separated, values in English enum words (case-insensitive) — and
what real answers look like around it. Then at least 10 fixtures `NN-short-name.txt`, invented items, no brands,
no people: 3 clean answers; one wrapped in a Markdown code fence with a sentence before and after; one with Russian
values («хлопок», «зима, весна», «синий»); one with a missing `fabric`; one with a garbled value (`colour: blueish
?`); one with two values for one key; one with extra keys (`brand: …` — must be ignored, and the value must be an
invented word, not a real brand); one empty answer. Each fixture has a sibling `NN-short-name.expected` with the
card a correct parser should produce, one `key: value` per key in the order of `docs/cards.md`: enum keys use the
enum word or `UNKNOWN`; for `name`, `type`, `formality` and `seasons` an unknown value is written `?` (a name may be
the word "Unknown"); `seasons` comma-separated in enum order. These files are append-only once merged and the next
tasks test against them: check each one twice against `docs/cards.md` before pushing.

### WR-012 — Parse a clean model answer into a card draft
- status: todo
- depends: WR-011
- size: S
- paths: `domain/src/main/java/wardrobe/domain/card/**`, `domain/src/test/java/wardrobe/domain/card/CardParserCleanTest.java`
- verify: `./gradlew :domain:test --tests 'wardrobe.domain.card.CardParserCleanTest'`
- expect: green on the three clean fixtures; plain Java, no JSON library

Package `wardrobe.domain.card`: `CardDraft` (record: name, type, colour, pattern, fabric, seasons, formality —
each may be unknown: use the enums' UNKNOWN where they have one, and `Optional` for `type`, `formality`, `name`,
an empty set for unknown seasons) and `CardParser.parse(String answer)` → `CardDraft`. Version 1: lines
`key: value`, keys and values case-insensitive, whitespace trimmed, enum words in English. The domain has no I/O:
the test reads fixtures with `getResourceAsStream` and compares with the `.expected` files.

### WR-013 — Tolerant parsing: code fences, prose around, Russian words
- status: todo
- depends: WR-012
- size: S
- paths: `domain/src/main/java/wardrobe/domain/card/**`, `domain/src/test/java/wardrobe/domain/card/CardParserTolerantTest.java`
- verify: `./gradlew :domain:test --tests 'wardrobe.domain.card.*'`
- expect: green on the fenced and the Russian fixtures, and the clean ones still pass

Skip lines that are not `key: value` with a known key (prose, fences, `brand:`). Map Russian words to enum values
through **one** table in `wardrobe.domain.card` (`хлопок`→COTTON, `синий`→BLUE, `зима`→WINTER, …; cover the words
in the fixtures and the obvious neighbours; write the table's scope in `DECISIONS.md`). A word in neither language
stays UNKNOWN — never the closest match.

### WR-014 — Garbled, missing and doubled fields stay unknown, with a list of doubts
- status: todo
- depends: WR-013
- size: S
- paths: `domain/src/main/java/wardrobe/domain/card/**`, `domain/src/test/java/wardrobe/domain/card/CardParserDoubtsTest.java`
- verify: `./gradlew :domain:test --tests 'wardrobe.domain.card.*'`
- expect: green on every fixture; each doubt names the key and the raw value; the earlier card tests pass unchanged

Add `CardParser.read(String answer)` → `ParsedCard(CardDraft draft, List<Doubt> doubts)`; `parse` keeps its
signature and returns `read(answer).draft()` (the tests of WR-012/013 must stay green untouched). `Doubt`: record
key, raw value, why: MISSING, UNREADABLE, CONFLICTING. Two different values for one key → UNKNOWN and CONFLICTING (never "the first one wins"). Missing →
MISSING. Empty answer → every field unknown, one doubt per key. Test against every fixture's `.expected` plus the
exact doubts for the garbled, missing, doubled and empty ones.

### WR-015 — Confirm a draft into an item
- status: todo
- depends: WR-014
- size: S
- paths: `domain/src/main/java/wardrobe/domain/card/**`, `domain/src/test/java/wardrobe/domain/card/ConfirmCardTest.java`
- verify: `./gradlew :domain:test --tests 'wardrobe.domain.card.ConfirmCardTest'`
- expect: green; a draft with an unknown field cannot become an item unless the person supplied that field

`ConfirmCard.confirm(ItemId id, CardDraft draft, Corrections corrections)` → `Item` in status CLEAN. `Corrections`
holds the fields the person set by hand; they win over the draft. If any field is still unknown after corrections
(type, formality, name missing; seasons empty; colour/pattern/fabric UNKNOWN) → `IncompleteCard` listing the keys.
Decide in `DECISIONS.md` whether UNKNOWN colour/pattern/fabric may be confirmed as such — recommendation: not in
version 1 (the rules cannot judge an unknown colour).

## Avatar draft (fit logic under tests; look checked by Aika)

### WR-016 — Measurements of a figure, in millimetres
- status: todo
- size: S
- paths: `domain/src/main/java/wardrobe/domain/avatar/**`, `domain/src/test/java/wardrobe/domain/avatar/MeasurementsTest.java`, `domain/src/test/java/wardrobe/domain/avatar/TestFigures.java`
- verify: `./gradlew :domain:test --tests 'wardrobe.domain.avatar.MeasurementsTest'`
- expect: green; out-of-range values are rejected with the field's name

Package `wardrobe.domain.avatar`: `Measurements` record, all `int` millimetres: height, shoulders (width), chest,
waist, hips (girths), arm (shoulder to wrist), inseam. Validation ranges (adult, generous): height 1300–2200;
shoulders 300–600; chest 600–1600; waist 450–1500; hips 650–1700; arm 450–800; inseam 600–1000. Also inseam <
height and arm < height. Three invented figures as constants in the test helper `TestFigures` (new file; the later
avatar tasks reuse it) — never Aika's real
measurements (invariant 7). No floating point anywhere (invariant 6).

### WR-017 — A flat figure from proportions: where the lines of the body are
- status: todo
- depends: WR-016
- size: S
- paths: `domain/src/main/java/wardrobe/domain/avatar/**`, `domain/src/test/java/wardrobe/domain/avatar/FigureTest.java`
- verify: `./gradlew :domain:test --tests 'wardrobe.domain.avatar.FigureTest'`
- expect: green; for every test figure the landmarks are ordered top to bottom and widths follow the girths

`Figure.of(Measurements)` — a front view, origin at the top of the head, y down, millimetres, integer arithmetic
only: landmarks y (head 0, neck, shoulders, chest, waist, hips, crotch = height − inseam, knee, ankle, floor =
height) and half-widths at shoulders, chest, waist, hips (a girth → front width: `girth * 100 / 314 `, rounded down;
write the approximation and its source of error in `DECISIONS.md`). Start the class doc «draft — вид проверяет
Aika». Tests: order of landmarks for the three figures; a wider waist gives a wider waist half-width; the same
input gives the same figure.

### WR-018 — Garment sizes and the fit check
- status: todo
- depends: WR-016
- size: S
- paths: `domain/src/main/java/wardrobe/domain/avatar/**`, `domain/src/test/java/wardrobe/domain/avatar/FitTest.java`
- verify: `./gradlew :domain:test --tests 'wardrobe.domain.avatar.FitTest'`
- expect: green; each fit note has a boundary test (exactly at the limit and one millimetre past it)

`GarmentSize` (record, `int` mm; each field optional as a size chart may lack it): chest, waist, hips, length,
sleeve, inseam. `Fit.check(Measurements, ItemType, GarmentSize)` → `List<FitNote>` (empty = fits). Notes:
TOO_TIGHT (garment girth < body girth + minimum ease), TOO_LOOSE (> body + maximum ease), SLEEVE_SHORT/LONG,
LEG_SHORT/LONG — per relevant field for the item type. Eases in one table of constants (starting values: minimum
ease chest 40, waist 20, hips 30; maximum ease 200; sleeve and inseam tolerance ±30; write them in `DECISIONS.md`
as Aika's to tune). An absent field gives no note. Integer only.

### WR-019 — Layers: the order clothes are drawn on the avatar
- status: todo
- depends: WR-006
- size: S
- paths: `domain/src/main/java/wardrobe/domain/avatar/**`, `domain/src/test/java/wardrobe/domain/avatar/LayersTest.java`
- verify: `./gradlew :domain:test --tests 'wardrobe.domain.avatar.LayersTest'`
- expect: green; the order is total and stable, and an outfit with problems is refused

`Layers.order(Outfit)` → the items in drawing order, bottom layer first: SHOES, BOTTOM, TOP or DRESS, OUTERWEAR,
ACCESSORY; within one type, input order. Refuse (`IllegalArgumentException` listing the problems) an outfit for
which `OutfitCheck.check` reports `TwoOfAKind` or `DressWithSeparates` — the avatar does not draw an impossible
outfit; clashes and dirty items are drawn (the person may want to see them). Doc comment «draft — вид проверяет
Aika».

### WR-020 — Where a garment's edges fall on the figure
- status: todo
- depends: WR-017, WR-018
- size: S
- paths: `domain/src/main/java/wardrobe/domain/avatar/**`, `domain/src/test/java/wardrobe/domain/avatar/PlacementTest.java`
- verify: `./gradlew :domain:test --tests 'wardrobe.domain.avatar.PlacementTest'`
- expect: green; a longer garment ends lower, never above where it starts, never below the floor

`Placement.of(Figure, ItemType, GarmentSize)` → a `Shape` record: top y, bottom y, half-width at top and bottom,
sleeve end y (tops, dresses, outerwear) — all mm. TOP and OUTERWEAR start at the shoulders, DRESS too, BOTTOM at the
waist, SHOES at the ankle; length from the size (absent → a default per type in one table: TOP to the hips, BOTTOM
to the ankle, DRESS to the knee, OUTERWEAR to mid-thigh). Clamp to the floor and say so in a `clamped` flag. Width
from the garment girth if given, else from the figure plus the minimum ease of WR-018. Doc comment «draft — вид
проверяет Aika».

### WR-021 — REST: dress the avatar
- status: todo
- depends: WR-008, WR-019, WR-020
- size: S
- paths: `api/openapi.yaml`, `app/src/main/java/wardrobe/app/web/**`, `app/src/test/java/wardrobe/app/AvatarApiTest.java`
- verify: `./gradlew :app:test --tests 'wardrobe.app.AvatarApiTest'`
- expect: green; the answer lists the figure's landmarks and one shape per item in drawing order, valid against the contract

`POST /avatar/dress` with `{ measurements: {...}, items: [ {id, size: {...}} ] }` → `{ figure: {landmarks…,
widths…}, layers: [ {itemId, type, shape: {...}, fit: [notes]} ] }`, all integers in mm. Unknown item → 404;
invalid measurements → 400 naming the field; an impossible outfit → 409 with the problems; errors use the `Error`
schema of WR-008. Extend `CorsConfig` to allow POST with a JSON body from the local origins and test the preflight
(`Access-Control-Request-Method: POST`, `Access-Control-Request-Headers: content-type`). Contract first; the
controller only calls the domain (`Layers`, `Placement`, `Fit`). Test over real HTTP with schema validation using
the invented figures. Sizes travel in the request in version 1 (no storage of sizes yet; write in `DECISIONS.md`).

## Client (Expo)

### WR-022 — Plain Russian labels for every value the API sends
- status: todo
- size: S
- paths: `client/src/labels.ts`, `client/test/labels.test.mjs`
- verify: `cd client && npm test`
- expect: green; the test fails if `api/openapi.yaml` has an enum value without a label

`client/src/labels.ts`: one table per enum (type, colour, pattern, fabric, season, formality, status) → plain
Russian («умеренно-официальный» is not plain: prefer «повседневный», «смарт-кэжуал» with a hint, etc.; write the
choices that are debatable in `DECISIONS.md`). Move nothing out of `items.ts` (its `STATUS_LABEL` stays; reuse it in
`labels.ts` for statuses — one table per thing). `labels.test.mjs` reads `../api/openapi.yaml` as text, extracts
the `enum: [...]` lists **inside `Item.properties` only** (from the line `    Item:` to the next line with the same
four-space indentation or the end of `components.schemas`; later tasks add other schemas with enums, such as
`Error.code`, that are not shown to people) with a regular expression, and checks every value has a non-empty
label. A test fixture with a second schema after `Item` shows those enums are ignored.

### WR-023 — The list screen: filter by status (вид проверяет Aika)
- status: waiting
- waits: Aika's «да» on the look (`docs/design/mockups/mockups.html`, `docs/design/DESIGN.md`)
- depends: WR-022, WR-028
- size: S
- paths: `client/App.tsx`, `client/src/filter.ts`, `client/test/filter.test.mjs`
- verify: `cd client && npm test`
- expect: green; the filter logic is tested; the PR says «draft — вид проверяет Aika»

Pure logic in `client/src/filter.ts`: `filterByStatus(items, status | 'ALL')` and `countsByStatus(items)` (every
status present, zero included). On the screen: a row of chips «Все · Чистое (n) · В стирке (n) · …» above the
list. Filtering is local (the list is small); the server's `?status=` is for later. No new dependency. The look is a
draft: describe it in words in the PR.

### WR-024 — Item details screen (вид проверяет Aika)
- status: waiting
- waits: Aika's «да» on the look (`docs/design/mockups/mockups.html`, `docs/design/DESIGN.md`)
- depends: WR-022, WR-028
- size: S
- paths: `client/App.tsx`, `client/src/details.ts`, `client/src/screens/**`, `client/test/details.test.mjs`
- verify: `cd client && npm test`
- expect: green; `describeItem` is tested for every field; the PR says «draft — вид проверяет Aika»

`client/src/details.ts`: `describeItem(item)` → ordered `{label, value}` rows in Russian (from `labels.ts`),
seasons in calendar order. Tapping a row of the list opens a details view (`client/src/screens/ItemDetails.tsx`;
simple state in `App.tsx`, no navigation library — no new dependency). Shows the rows and the status.

### WR-025 — Change the status from the details screen (вид проверяет Aika)
- status: waiting
- waits: Aika's «да» on the look (`docs/design/mockups/mockups.html`, `docs/design/DESIGN.md`)
- depends: WR-008, WR-024, WR-028
- size: S
- paths: `client/src/api.ts`, `client/src/items.ts`, `client/src/screens/**`, `client/App.tsx`, `client/test/api.test.mjs`
- verify: `cd client && npm test`
- expect: green; buttons come only from the server's `next`; an error's `message` is shown as is

`client/src/api.ts`: `changeStatus(baseUrl, id, status, fetchFn = fetch)` → the updated item, or throws with the
server's `Error.message`. Extend `parseItems` to read `next` (keep it tolerant of its absence for older servers:
then no buttons). Details screen: one button per status in `next` («В стирку», «В ремонт», «В поездку», «Чистое»).
The client does **not** know the transition table (one rule in one place). Test `api.ts` with a fake `fetch`.

### WR-026 — No key and no photo ever lands in the client
- status: todo
- size: S
- paths: `client/test/no-secrets.test.mjs`
- verify: `cd client && npm test`
- expect: green now; the test fails when a file under `client/` (outside node_modules) contains a key-like string or an image of a person

`no-secrets.test.mjs` walks `client/` (skipping `node_modules`, `.expo`, `dist`) and fails on: strings matching
`sk-ant-`, `sk-[A-Za-z0-9]{20,}`, `AIza[0-9A-Za-z_-]{30,}`, `api[_-]?key\s*[:=]`, `Authorization:\s*Bearer\s+[A-Za-z0-9]`
(case-insensitive), and on any `.jpg`, `.jpeg`, `.heic` file (version 1 has no photos; icons are `.png`). The test
must ignore its own file. Put a planted temporary file in `os.tmpdir()` to show the matcher catches each pattern
(test the matcher function, not the real tree, for the positive cases).

### WR-027 — The avatar screen: the dressed doll (вид проверяет Aika)
- status: waiting
- waits: Aika's decision on the avatar (the drawn doll, or a photo try-on later) and her «да» on the look
- depends: WR-021, WR-024, WR-028, WR-033
- size: S
- paths: `client/src/avatar/**`, `client/src/screens/**`, `client/App.tsx`, `client/test/avatar.test.mjs`
- verify: `cd client && npm test`
- expect: green; the scaling logic is tested; the PR says «draft — вид проверяет Aika»

The doll must look real, not boxy (`docs/design/DESIGN.md`, «Clothes and the doll»). Its template comes from
`python3 docs/design/mockups/garments.py --json`: `doll_bare` (the body), `doll_landmarks` (y of head, shoulders,
chest, waist, hips, crotch, knee, ankle, floor and the centre x) — already in `client/src/ui/garments.data.ts` after
WR-033; never type the landmarks a second time. `client/src/avatar/fit.ts`: `bands(answer, heightPx)` maps the
server's `POST /avatar/dress` landmarks (mm) to pixel bands, and `place(point, bands)` moves a template point
piecewise-linearly between the template's landmarks and the figure's — so a wider waist widens the drawn waist.
Version 1 draws on the doll only what the template has clothes for: a TOP as the shirt and a BOTTOM as the trousers
of the template `doll` (other kinds are listed beside the doll with their fit notes, not drawn on it — write this
in `DECISIONS.md`). FitNote markers sit at the end of the garment edge the note is about (sleeve end, hem), from
the server's shapes. Integer pixels, the drawing order of the answer kept; no fit geometry here — the server
computed it (one rule in one place). Screen `client/src/screens/Avatar.tsx`; one invented figure. Tests: a template
landmark maps exactly onto the figure's landmark; points between landmarks stay between; order is kept. Describe the
look in words in the PR.

## UI kit (after Aika's «да» on the look)

### WR-028 — Tokens and components of «уютный блокнот» as code (вид проверяет Aika)
- status: waiting
- waits: Aika's «да» on the look (`docs/design/mockups/mockups.html`, `docs/design/DESIGN.md`)
- size: S
- paths: `client/src/ui/**`, `client/test/ui-tokens.test.mjs`
- verify: `cd client && npm test`
- expect: green; the test fails when `client/src/ui/tokens.ts` differs from the JSON block of `docs/design/DESIGN.md`

`client/src/ui/tokens.ts`: the JSON block of `docs/design/DESIGN.md` as a typed constant (same names, same values).
`ui-tokens.test.mjs` reads `../docs/design/DESIGN.md`, takes the first ```json block, and compares it deep-equal with
the exported tokens — so a token changes in DESIGN.md first. Then the simple components of the DESIGN.md table as
React Native components in `client/src/ui/` (one file each, names as in the table): `ScreenTitle`, `SectionTitle`,
`PaperCard` (+ `Tape`), `Chip`, `StatusBadge`, `StatusDot`, `Button`, `WhyItFits` (not `ItemThumb`: it shows a
real-looking item and comes with WR-033). Only `View`, `Text`
and `StyleSheet`; no new dependency. Handwritten headings need fonts (`expo-font`): not in this task — use the
system font and write in `DECISIONS.md` that the font waits for an ADR. Pure logic (e.g. status → colour token) in
`client/src/ui/status.ts`, tested.

### WR-033 — Real-looking clothes on the phone: ClothingDrawing (вид проверяет Aika)
- status: waiting
- waits: Aika's «да» to ADR-0003 (`react-native-svg`; its status stays Proposed on main — this task's PR sets it Accepted) and her «да» on the look
- depends: WR-028
- size: S
- paths: `client/package.json`, `client/package-lock.json`, `client/src/ui/garments.data.ts`, `client/src/ui/garments.ts`, `client/src/ui/ClothingDrawing.tsx`, `client/src/ui/PaperDoll.tsx`, `client/src/ui/ItemThumb.tsx`, `client/test/garments.test.mjs`, `docs/adr/0003-draw-clothes-with-svg.md`
- verify: `cd client && npm test`
- expect: green; the test fails when `garments.data.ts` differs from what `docs/design/mockups/garments.py` draws

Implement ADR-0003 and set its status to Accepted in the same PR. `cd client && npx expo install react-native-svg`.
Generate `client/src/ui/garments.data.ts` from `python3 docs/design/mockups/garments.py --json` (SVG markup per
garment kind with a `{colour}` placeholder, and the doll with `{shirt}` / `{trousers}`; do not change the script —
it is Aika's). `ClothingDrawing({kind, colour, size})` fills the placeholder and renders the markup with
`SvgXml` of `react-native-svg`; `PaperDoll` does the same for the doll (`doll_bare`, `doll`, `doll_landmarks`).
`ItemThumb` (DESIGN.md) shows an item: its cut-out photo when it has one (none in version 1), else
`ClothingDrawing` — never a box. The drawings are the app's fallback; the mockups show the «cut-out photo» look that
real photos will have once background removal is decided (WR-034). Garment kinds
by `ItemType` + a default per type (TOP → shirt, BOTTOM → trousers, DRESS → dress, OUTERWEAR → coat, SHOES → shoes,
ACCESSORY → bag); the mapping is in one place in `client/src/ui/garments.ts` and tested. `garments.test.mjs` runs
`python3 ../docs/design/mockups/garments.py --json` and checks `garments.data.ts` holds exactly that data.

## Our difference: say why, keep statuses, no limits (intent 01, invariants 8 and 9)

### WR-029 — Explain an outfit rule by rule
- status: todo
- depends: WR-005, WR-006
- size: S
- paths: `domain/src/main/java/wardrobe/domain/match/**`, `domain/src/main/java/wardrobe/domain/outfit/**`, `domain/src/test/java/wardrobe/domain/outfit/ExplainTest.java`
- verify: `./gradlew :domain:test --tests 'wardrobe.domain.outfit.ExplainTest'`
- expect: green; every rule gives exactly one line for an outfit, ✓ or ✗, never an empty reason

The answer to the main complaint about wardrobe apps ("the outfits look random"): `Explain.outfit(Outfit)` →
`List<WhyLine>`, one per rule of `Combination.standard()` plus one for cleanliness, in that fixed order. `WhyLine`
(record): `rule` (FORMALITY, COLOUR, PATTERN, SEASON, CLEAN), `goes`, `code` (an enum per reason, e.g.
`CLOSE_ENOUGH`, `STEPS_APART`, `NEUTRAL`, `FRIENDS`, `SAME_COLOUR`, `NOT_FRIENDS`, `UNKNOWN_COLOUR`, `ONE_PATTERN`,
`TWO_PATTERNS`, `SHARED_SEASON`, `NO_SHARED_SEASON`, `ALL_CLEAN`, `NOT_CLEAN`), `items` (the ids the line is about)
and the English `reason`. A rule's line is ✓ when every pair passes it, else ✗ naming the first failing pair in
input order. The CLEAN line comes from `OutfitCheck`'s `NotClean` problems (✓ when there are none) — never a
second cleanliness check. The codes let the client say it in Russian without re-deciding anything (one rule in one
place). If a rule class has to expose its code, add it there — do not compute it a second time.

### WR-030 — REST: check an outfit and say why
- status: todo
- depends: WR-008, WR-029
- size: S
- paths: `api/openapi.yaml`, `app/src/main/java/wardrobe/app/web/**`, `app/src/test/java/wardrobe/app/OutfitCheckApiTest.java`
- verify: `./gradlew :app:test --tests 'wardrobe.app.OutfitCheckApiTest'`
- expect: green; the answer has one `why` line per rule and the problems of `OutfitCheck`, valid against the contract

`POST /outfits/check` with `{ "items": ["id", …] }` → `{ "ok": bool, "why": [WhyLine], "problems": [{code, message,
items}] }`. Unknown id → 404 `Error`; empty list → 400. Contract first (`WhyLine.rule` and `code` as enums). CORS:
POST from the local origins (if WR-021 has not added it yet, add it here with its preflight test). The controller only
calls `Explain` and `OutfitCheck`.

### WR-031 — The client says why in plain Russian
- status: todo
- depends: WR-022, WR-030
- size: S
- paths: `client/src/why.ts`, `client/test/why.test.mjs`
- verify: `cd client && npm test`
- expect: green; the test fails when a `WhyLine.code` of `api/openapi.yaml` has no Russian sentence

`client/src/why.ts`: `whyText(line, itemsById)` → `{ mark: '✓' | '✗', title, text }` in Russian from one template
per `code` (e.g. STEPS_APART → «{a} — деловая, {b} на {n} шага проще»), item names and Russian labels from
`labels.ts`. It never decides whether things go together — it only words the server's answer. The test reads the
`code` enum of `WhyLine` from `../api/openapi.yaml` (only that schema) and checks every code has a template; plus
three worded examples.

### WR-032 — No limit on items
- status: todo
- depends: WR-009
- size: S
- paths: `app/src/test/java/wardrobe/app/NoLimitApiTest.java`
- verify: `./gradlew :app:test --tests 'wardrobe.app.NoLimitApiTest'`
- expect: green; a catalogue of 2 000 items comes back whole from `GET /items` and from `GET /items?status=CLEAN`

A test only (intent 01, invariant 9): a `@TestConfiguration` with a `@Primary` `ItemCatalog` of 2 000 invented items
(generated in the test), `GET /items` returns all 2 000 and the clean filter returns exactly the clean ones. If the
code has a cap anywhere, the test fails and the task parks with where the cap is — do not remove a cap without Aika.

## Research (no code; Aika decides)

### WR-034 — How to cut out the background of an item photo for free
- status: todo
- size: S
- paths: `docs/backlog/reports/background-removal.md`
- verify: `f=docs/backlog/reports/background-removal.md; test -f $f && grep -c '^|' $f && grep -ci 'licen' $f`
- expect: a table of at least 5 options (at least 7 lines starting with `|`), every row with its licence; a recommendation; «Решение — за Aika»

Items will be shown like product photos: the item cut out of its background on a light backing (DESIGN.md). Write a
report — no code, no dependency, nothing installed in the project — on how to cut out the background **for free**:
on the phone or on our own server, with no paid API and no key. Compare at least: Apple Vision subject lifting on
iOS (`VNGenerateForegroundInstanceMaskRequest`), Google ML Kit Subject Segmentation on Android, `rembg` with its
open models (U²-Net, IS-Net, BiRefNet …) on our Java server's machine (as a separate process), background removal
in the browser/JS (e.g. transformers.js models, `@imgly/background-removal`), and one more you find. Columns: where
it runs (device / our server), what it needs (OS version, CPU/GPU, model size), licence of the code **and of the
model weights** (commercial use allowed? AGPL? non-commercial only?), quality on clothes (folds, white on white),
speed, how it fits Expo (works in Expo Go, needs a dev build, a native module), cost (must be 0). Say where each
fact comes from (link) and mark what you could not check from this environment. Remember intent 01: photos are
personal data — prefer options where the photo never leaves the phone or our own server. End with a recommendation
and 2–3 options, and the line «Решение — за Aika». Use only what you can read from here; if a page cannot be
reached, say so in the report.
