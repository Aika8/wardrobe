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
  mockups in `docs/design/mockups/`). Aika approved the look on 25 November 2026; the screen
  tasks follow the mockups. Logic tasks never wait for the look.

Rules for every task (the executor prompt repeats them): existing tests are never changed or deleted, new tests are
new files; `domain/` stays plain Java (no Spring, Jackson, I/O, clock); sizes are integer millimetres; a field a
model answer lacks stays unknown; contract changes go to `api/openapi.yaml` first; a wardrobe rule lives on the
server, the client shows it; no model key, no photos, no real brands; `make test` stays green.

Read first, every task: `intent/01-wardrobe.md` (invariants), `docs/adr/0002-stack-and-hexagon.md`, the classes
named in the task. Test items are invented (`domain/src/test/java/wardrobe/domain/TestItems.java`; add your own
helper in a new file when you need other fields — never edit an existing test file).

## Bugs (taken first)

### WR-159 — The item list's screen padding is lost in the web build
- status: todo
- depends: WR-035
- size: S
- paths: `client/App.tsx`, `client/src/screens/ItemList.tsx`, `client/src/ui/Screen.tsx`, `docs/backlog/reports/screens/WR-159.png`
- verify: `make client/node_modules/.ci-stamp && cd client && npm test && npm run screenshot -- WR-159`
- expect: green; in `docs/backlog/reports/screens/WR-159.png` the title, chips and rows keep a 16 px gap from the left and right edges of the 390 px page

Tech debt `docs/backlog/tech-debt/2026-11-26-WR-035-1.md`: the list screen sets `padding: 16` on a `SafeAreaView`,
and on the web export the content touches the page edges (see `docs/backlog/reports/screens/WR-035.png`). Fix it
the way that works on every platform: keep `SafeAreaView` for the safe area only and put the padding on an inner
`View` — as one small component `client/src/ui/Screen.tsx` (`SafeAreaView` + inner `View` with
`padding: tokens.space.lg`, `flex: 1`, background `paper`), used by the list screen. If the router (WR-126) has
already moved the list to `client/src/screens/ItemList.tsx`, fix it there; otherwise in `client/App.tsx`. Change
nothing else on the screen (its look moves to the components in WR-161). Take the screenshot and look at it before
pushing; name it in the PR («draft — вид проверяет Aika» stays true for the screen).

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

## Foundations for the screens (taken early so screen work never runs dry)

### WR-035 — Screenshot of the Expo web build in a headless browser
- status: todo
- size: S
- paths: `client/package.json`, `client/package-lock.json`, `client/scripts/screenshot.mjs`, `client/test/screenshot-args.test.mjs`, `docs/backlog/reports/screens/WR-035.png`
- verify: `make client/node_modules/.ci-stamp && cd client && npm test`
- expect: green; `npm run screenshot -- WR-035` writes `docs/backlog/reports/screens/WR-035.png` showing the item list; the PNG shows the list, not the load error

So screen tasks can look at what they built (the trial of learning-exchange showed that Chrome for Testing is at
`$CHROME_PATH` in the cloud, npm installs `playwright-core`, and `npx expo export --platform web` builds). Add
`playwright-core` as a **dev** dependency (no browser download: it uses `$CHROME_PATH`; locally, the Chrome the
person has). `client/scripts/screenshot.mjs <id> [--route /path]`: builds the web export into a temp folder
(exported with `EXPO_PUBLIC_API_URL=http://localhost:<port in use>`, default 8091, so the page calls the server the
script started), serves
it on a free local port with Node's `http` module (no new server package), starts the server of this repo on port
8091 if nothing answers `GET /items` there (`make dev PORT=8091`) and stops it at the end, opens the page at
390 × 844 (phone width, device scale 2), waits for the list, saves `docs/backlog/reports/screens/<id>.png`, and
exits non-zero with a clear message when Chrome is missing. `npm run screenshot` in `package.json`. The argument
parsing and the output path are a pure function, tested in `screenshot-args.test.mjs`. CI does not run the
screenshot (no server there); say so in the script's header. Never commit anything but the PNG it names.

### WR-028 — Tokens and components of «уютный блокнот» as code (вид проверяет Aika)
- status: todo
- size: S
- paths: `client/src/ui/**`, `client/test/ui-tokens.test.mjs`
- verify: `make client/node_modules/.ci-stamp && cd client && npm test`
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


### WR-126 — A tiny router, a component sheet and a screenshot of any screen
- status: todo
- depends: WR-023, WR-024, WR-025, WR-028, WR-035
- size: S
- paths: `client/App.tsx`, `client/src/nav.ts`, `client/src/screens/**`, `client/scripts/screenshot.mjs`, `client/test/nav.test.mjs`, `docs/backlog/reports/screens/WR-126.png`
- verify: `make client/node_modules/.ci-stamp && cd client && npm test`
- expect: green; `nav.test.mjs` covers parse/format round trips, unknown routes, push/back/reset; `npm run screenshot -- WR-126 --route '/#/sheet'` waits for the sheet, not for the list

The client will have about ten screens; no navigation library (no new dependency, ADR-0002). Build the smallest
router that lets every later screen task add **one line**.

1. `client/src/nav.ts` (pure, no React/RN imports; Node runs it by stripping types — erasable syntax only, imports
   with the `.ts` extension): `type Route = { name: string; param?: string }`; `parseHash(hash)` — `'#/item/white-shirt'`
   → `{name:'item', param:'white-shirt'}`, `decodeURIComponent` on the param, `''`/`'#'`/`'#/'` → `null`;
   `toHash(route)` (param encoded with `encodeURIComponent`, so `parseHash(toHash(r))` equals `r`); a history as an
   immutable array: `push(h, r)`, `back(h)` (never empties: at the root it returns `h` unchanged), `reset(h, r)` (a
   tab switch: `[r]`), `current(h)`. `type ScreenProps = { route: Route; go(r: Route): void; back(): void;
   reset(r: Route): void }`. `MAIN_ROUTES = ['home', 'wardrobe', 'calendar', 'me']` (the four places of `BottomNav`).
2. `client/src/screens/routes.tsx`: `ROUTES: Record<string, (p: ScreenProps) => React.JSX.Element>` and `DEFAULT_ROUTE`.
   Register `list` (the item list) and `sheet`; set `DEFAULT_ROUTE = { name: 'list' }` (the home task changes it).
   An unknown name renders the default route.
3. Move the item list out of `App.tsx` into `client/src/screens/ItemList.tsx` **without changing its look or
   logic**. Register the `item` (ItemDetails of WR-024/WR-025, param = item id) and `list` (with the status filter
   param of WR-023's chips: `'#/list/IN_WASH'`) routes, replacing their state in `App.tsx`. If the avatar of WR-027
   already exists, register it as `me` the same way. Never edit their tests.
4. `App.tsx`: holds the history in `useState`, renders `ROUTES[current.name]` on the `paper` background. On the
   web (guard `typeof window !== 'undefined' && window.location`) the first route comes from `location.hash`, every
   move writes the hash, and a `hashchange` listener resets to the parsed route (browser back works).
5. `client/src/screens/Sheet.tsx` (route `sheet`): a scrollable page titled «Компоненты» that renders
   `SHEET: { name: string; render: () => React.JSX.Element }[]`, each under its name in small `ink-soft` text. Start it
   with the components of WR-028 (one sample each, invented texts). Later component tasks append one entry. It is a
   developer page; no link leads to it.
6. Every screen's root `View` gets `testID="screen"` once its data has loaded (`ItemList` and `Sheet` too). Change
   `client/scripts/screenshot.mjs` (WR-035) to wait for `[data-testid="screen"]` (react-native-web renders `testID`
   so) instead of the list; keep its argument parsing — `client/test/screenshot-args.test.mjs` stays green untouched.

Convention (write it in `DECISIONS.md`): the «‹» back mark of the mockups is a `Pressable` with a `Text` in
`ink-soft`, size `section`, calling `back()` — part of a screen header as drawn, not a new component.
`nav.test.mjs` (new): round trips with spaces, «/» and Cyrillic in the param; null cases; push/back/reset; `back`
at the root. Run `cd client && npm run screenshot -- WR-126 --route '/#/sheet'` once to see that the wait works, and
describe what it shows in the PR; commit it as the first look at the WR-028 components (the component sheet
`#/sheet`).

### WR-127 — The drawn icons of the mockups as data
- status: todo
- size: S
- paths: `client/src/ui/icons.ts`, `client/test/icons.test.mjs`
- verify: `make client/node_modules/.ci-stamp && cd client && npm test`
- expect: green; the test fails when an icon in `icons.ts` differs from the same icon in `docs/design/mockups/mockups.html`

`HelperTile`, `RoundAction`, `BottomNav` and `StatusSwitch` show small drawn line icons (24 × 24, stroke only). They
are in the approved mockups as `<svg class="ic" viewBox="0 0 24 24"><path d="…"/></svg>` with the style
`fill:none; stroke:currentColor; stroke-width:1.8; stroke-linecap:round; stroke-linejoin:round`. Copy them, do not
redraw them.

`client/src/ui/icons.ts` (pure; erasable TypeScript only): `ICONS` — a readonly record name → path data `d`, for:
`outfit` (Собрать образ), `pairs` (Что с чем носить), `suitcase` (Сборы в поездку), `wash` (Сейчас в стирке /
В стирке), `home`, `wardrobe`, `plus`, `calendar`, `me` (the bottom bar), `wear` (Носка), `want` (Хочу), `trip`
(Поездка / В поездку), `shouldBuy` (Нужно ли?), `clean` (Чистое), `repair` (В ремонт). Also
`iconXml(name, colour)` → the complete SVG markup string (viewBox 0 0 24 24, the stroke style above as attributes,
`stroke` = the given colour) for `SvgXml` of `react-native-svg`; an unknown name throws.

`icons.test.mjs` (new) reads `../docs/design/mockups/mockups.html` as text, finds each icon by the label that
follows its `<svg class="ic"…>…</svg>` (the text up to the next tag, e.g. «Носка», «Главная»; the «+» of the bottom
bar is the svg inside `class="add"`) and checks its `d` equals `ICONS[name]`; it also checks `iconXml` contains the
path and the colour, and escapes nothing it should not. Never edit the mockups (Aika's).

## Statuses and the catalogue (intent 01, invariants 1 and 2)

### WR-007 — Change an item's status through the catalogue
- status: todo
- size: S
- paths: `domain/src/main/java/wardrobe/domain/ItemCatalog.java`, `domain/src/main/java/wardrobe/domain/ChangeStatus.java`, `domain/src/main/java/wardrobe/domain/UnknownItem.java`, `domain/src/test/java/wardrobe/domain/ChangeStatusTest.java`, `domain/src/test/java/wardrobe/domain/FakeCatalog.java`, `app/src/main/java/wardrobe/app/catalog/InMemoryItemCatalog.java`, `app/src/test/java/wardrobe/app/catalog/**`
- verify: `./gradlew :domain:test --tests 'wardrobe.domain.ChangeStatusTest' && ./gradlew :app:test --tests 'wardrobe.app.catalog.*'`
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
- verify: `./gradlew :domain:test --tests 'wardrobe.domain.card.CardParserTolerantTest' && ./gradlew :domain:test --tests 'wardrobe.domain.card.*'`
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
- verify: `./gradlew :domain:test --tests 'wardrobe.domain.card.CardParserDoubtsTest' && ./gradlew :domain:test --tests 'wardrobe.domain.card.*'`
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
- verify: `make client/node_modules/.ci-stamp && cd client && npm test`
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
- status: todo
- depends: WR-022, WR-028, WR-035
- size: S
- paths: `client/App.tsx`, `client/src/filter.ts`, `client/test/filter.test.mjs`, `docs/backlog/reports/screens/WR-023.png`
- verify: `make client/node_modules/.ci-stamp && cd client && npm test`
- expect: green; the filter logic is tested; the PR says «draft — вид проверяет Aika»

Pure logic in `client/src/filter.ts`: `filterByStatus(items, status | 'ALL')` and `countsByStatus(items)` (every
status present, zero included). On the screen: a row of chips «Все · Чистое (n) · В стирке (n) · …» above the
list. Filtering is local (the list is small); the server's `?status=` is for later. No new dependency. The look is a
draft: describe it in words in the PR.

Screenshot (after WR-035): `cd client && npm run screenshot -- WR-023` writes `docs/backlog/reports/screens/WR-023.png`; read it, compare with the mockup of this screen in `docs/design/mockups/mockups.html`, commit the PNG and name it in the PR. The look is still Aika's to approve.

### WR-024 — Item details screen (вид проверяет Aika)
- status: todo
- depends: WR-022, WR-028, WR-035
- size: S
- paths: `client/App.tsx`, `client/src/details.ts`, `client/src/screens/**`, `client/test/details.test.mjs`, `docs/backlog/reports/screens/WR-024.png`
- verify: `make client/node_modules/.ci-stamp && cd client && npm test`
- expect: green; `describeItem` is tested for every field; the PR says «draft — вид проверяет Aika»

`client/src/details.ts`: `describeItem(item)` → ordered `{label, value}` rows in Russian (from `labels.ts`),
seasons in calendar order. Tapping a row of the list opens a details view (`client/src/screens/ItemDetails.tsx`;
simple state in `App.tsx`, no navigation library — no new dependency). Shows the rows and the status.

Screenshot (after WR-035): `cd client && npm run screenshot -- WR-024` writes `docs/backlog/reports/screens/WR-024.png`; read it, compare with the mockup of this screen in `docs/design/mockups/mockups.html`, commit the PNG and name it in the PR. The look is still Aika's to approve.

### WR-025 — Change the status from the details screen (вид проверяет Aika)
- status: todo
- depends: WR-008, WR-024, WR-028, WR-035
- size: S
- paths: `client/src/api.ts`, `client/src/items.ts`, `client/src/screens/**`, `client/App.tsx`, `client/test/api.test.mjs`, `docs/backlog/reports/screens/WR-025.png`
- verify: `make client/node_modules/.ci-stamp && cd client && npm test`
- expect: green; buttons come only from the server's `next`; an error's `message` is shown as is

`client/src/api.ts`: `changeStatus(baseUrl, id, status, fetchFn = fetch)` → the updated item, or throws with the
server's `Error.message`. Extend `parseItems` to read `next` (keep it tolerant of its absence for older servers:
then no buttons). Details screen: one button per status in `next` («В стирку», «В ремонт», «В поездку», «Чистое»).
The client does **not** know the transition table (one rule in one place). Test `api.ts` with a fake `fetch`.

Screenshot (after WR-035): `cd client && npm run screenshot -- WR-025` writes `docs/backlog/reports/screens/WR-025.png`; read it, compare with the mockup of this screen in `docs/design/mockups/mockups.html`, commit the PNG and name it in the PR. The look is still Aika's to approve.

### WR-026 — No key and no photo ever lands in the client
- status: todo
- size: S
- paths: `client/test/no-secrets.test.mjs`
- verify: `make client/node_modules/.ci-stamp && cd client && npm test`
- expect: green now; the test fails when a file under `client/` (outside node_modules) contains a key-like string or an image of a person

`no-secrets.test.mjs` walks `client/` (skipping `node_modules`, `.expo`, `dist`) and fails on: strings matching
`sk-ant-`, `sk-[A-Za-z0-9]{20,}`, `AIza[0-9A-Za-z_-]{30,}`, `api[_-]?key\s*[:=]`, `Authorization:\s*Bearer\s+[A-Za-z0-9]`
(case-insensitive), and on any `.jpg`, `.jpeg`, `.heic` file (version 1 has no photos; icons are `.png`). The test
must ignore its own file. Put a planted temporary file in `os.tmpdir()` to show the matcher catches each pattern
(test the matcher function, not the real tree, for the positive cases).

### WR-027 — The avatar screen: the dressed doll (вид проверяет Aika)
- status: todo
- depends: WR-021, WR-024, WR-028, WR-033, WR-035
- size: S
- paths: `client/src/avatar/**`, `client/src/screens/**`, `client/App.tsx`, `client/test/avatar.test.mjs`, `docs/backlog/reports/screens/WR-027.png`
- verify: `make client/node_modules/.ci-stamp && cd client && npm test`
- expect: green; the scaling logic is tested; the PR says «draft — вид проверяет Aika»

The avatar is the drawn doll (Aika, 25 November 2026; a photo try-on is for after the holiday). The doll must look real, not boxy (`docs/design/DESIGN.md`, «Clothes and the doll»). Its template comes from
`python3 docs/design/mockups/garments.py --json`: `doll_bare` (the body), `doll_landmarks` (y of head, shoulders,
chest, waist, hips, crotch, knee, ankle, floor and the centre x) — already in `client/src/ui/garments.data.ts` after
WR-033; never type the landmarks a second time. `client/src/avatar/fit.ts`: `bands(answer, heightPx)` maps the
server's `POST /avatar/dress` landmarks (mm) to pixel bands, and `place(point, bands)` moves a template point
piecewise-linearly between the template's landmarks and the figure's — so a wider waist widens the drawn waist.
Version 1 draws on the doll only what the template has clothes for: a TOP as the shirt and a BOTTOM as the trousers
of the template `doll` (other kinds are listed beside the doll with their fit notes, not drawn on it — write this
in `DECISIONS.md`). FitNote markers sit at the end of the garment edge the note is about (sleeve end, hem), from
the server's shapes. Integer pixels, the drawing order of the answer kept; no fit geometry here — the server
computed it (one rule in one place). Screen `client/src/screens/Avatar.tsx`; one invented figure. If
`client/src/screens/routes.tsx` exists, register the avatar as route `me` there instead of state in `App.tsx`. Tests: a template
landmark maps exactly onto the figure's landmark; points between landmarks stay between; order is kept. Describe the
look in words in the PR.

Screenshot (after WR-035): `cd client && npm run screenshot -- WR-027` writes `docs/backlog/reports/screens/WR-027.png`; read it, compare with the mockup of this screen in `docs/design/mockups/mockups.html`, commit the PNG and name it in the PR. The look is still Aika's to approve.

## UI kit

### WR-033 — Real-looking clothes on the phone: ClothingDrawing (вид проверяет Aika)
- status: todo
- depends: WR-028, WR-035
- size: S
- paths: `client/package.json`, `client/package-lock.json`, `client/src/ui/garments.data.ts`, `client/src/ui/garments.ts`, `client/src/ui/ClothingDrawing.tsx`, `client/src/ui/PaperDoll.tsx`, `client/src/ui/ItemThumb.tsx`, `client/test/garments.test.mjs`, `docs/backlog/reports/screens/WR-033.png`
- verify: `make client/node_modules/.ci-stamp && cd client && npm test`
- expect: green; the test fails when `garments.data.ts` differs from what `docs/design/mockups/garments.py` draws

Implement ADR-0003 (Accepted). `cd client && npx expo install react-native-svg`.
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

Screenshot (after WR-035): `cd client && npm run screenshot -- WR-033` writes `docs/backlog/reports/screens/WR-033.png`; read it, compare with the mockup of this screen in `docs/design/mockups/mockups.html`, commit the PNG and name it in the PR. The look is still Aika's to approve.

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
The reason codes live in `wardrobe.domain.match` (an enum there); `outfit` uses them — never the other way round
(no package cycle, WR-100).

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
- verify: `make client/node_modules/.ci-stamp && cd client && npm test`
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
- verify: `f=docs/backlog/reports/background-removal.md; test -f $f && test $(grep -c '^|' $f) -ge 7 && grep -qi 'licen' $f && grep -q 'Решение — за Aika' $f && echo ok`
- expect: prints `ok`; a table of at least 5 options (at least 7 lines starting with `|`), every row with its licence; a recommendation; «Решение — за Aika»

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

## Outfits: more rules, suggestions, occasions, weather

### WR-036 — Property tests: every pair rule over random invented items
- status: todo
- size: S
- paths: `domain/src/test/java/wardrobe/domain/RandomItems.java`, `domain/src/test/java/wardrobe/domain/match/PairRulePropertiesTest.java`
- verify: `./gradlew :domain:test --tests 'wardrobe.domain.match.PairRulePropertiesTest'`
- expect: green; every property runs over all pairs of 200 random items for 3 fixed seeds; a failure names the seed and both items

A test-only task (intent 01, invariants 3, 4 and 8). Read `domain/src/main/java/wardrobe/domain/match/*.java` and
`domain/src/test/java/wardrobe/domain/TestItems.java` first.

`RandomItems` (new public test helper in package `wardrobe.domain`; later tasks reuse it):
- `static Item random(java.util.Random rnd, String id)`: every enum field picked uniformly from all its values
  (`UNKNOWN` included for colour, pattern and fabric), 1–4 distinct seasons, status CLEAN with probability 7/10,
  else one of the other three; name `"random " + id`.
- `static List<Item> catalogue(long seed, int n)`: ids `r0 … r{n-1}`, one `new Random(seed)`. Never an unseeded
  `Random`, never the clock: the same seed gives the same items.

`PairRulePropertiesTest` (package `wardrobe.domain.match`): for each of `FormalityRule`, `ColourRule`,
`PatternRule`, `SeasonRule`, over all ordered pairs of `RandomItems.catalogue(seed, 200)` for seeds 1, 2, 3:
- symmetry: `judge(a, b).goes() == judge(b, a).goes()`;
- self: `judge(a, a).goes()` for every item, `UNKNOWN` fields included;
- determinism: two calls give equal `Verdict`s (record equality, the reason included);
- a reason is never blank, for yes and for no (invariant 8);
- oracles: formality goes ⇔ `stepsTo ≤ 1`; season goes ⇔ the season sets intersect; pattern goes ⇔ same id or at
  least one `PLAIN`; colour, two different items: any `UNKNOWN` → no, otherwise either neutral → yes.

Collect the first 5 failures (rule, property, seed, both items' fields) and fail once with all of them — not tens
of thousands of separate assertions. `Combination` is not part of this task (WR-005 has its own symmetry test).

If a property fails on `main`, a rule has a bug: main code is not in this task's paths, so do not fix it — park the
task with the seed and the failing pair.

### WR-037 — Every fabric says whether it is light, medium or heavy
- status: todo
- size: S
- paths: `domain/src/main/java/wardrobe/domain/Fabric.java`, `domain/src/main/java/wardrobe/domain/FabricWeight.java`, `domain/src/test/java/wardrobe/domain/FabricWeightTest.java`
- verify: `./gradlew :domain:test --tests 'wardrobe.domain.FabricWeightTest'`
- expect: green; every constant of `Fabric` is covered and the test fails if one is missing from its table

The weather check (WR-041) needs to know how warm a fabric is. New enum `FabricWeight { LIGHT, MEDIUM, HEAVY,
UNKNOWN }` with `boolean known()` (false only for `UNKNOWN`). `Fabric` gets a constructor argument and
`public FabricWeight weight()` — the same shape as `Colour.neutral()` (read `Colour.java`), so a new fabric cannot be
added without deciding its weight. Starting table:
- LIGHT: COTTON, LINEN, SILK
- MEDIUM: DENIM, KNIT, SYNTHETIC
- HEAVY: WOOL, LEATHER
- UNKNOWN: UNKNOWN (intent 01, invariant 5: never guessed)

Keep the names and the order of the `Fabric` constants (the JSON uses the names; `ItemsApiTest` reads them).
`FabricWeightTest` holds the expected weight of **every** constant in its own table, iterates `Fabric.values()` and
fails when a constant is missing from the table. Write the table in `DECISIONS.md` as Aika's to tune, and the known
coarseness: one weight per fabric, not per item (a thick cotton sweatshirt counts as LIGHT).

### WR-038 — A bigger invented sample wardrobe: 60 items
- status: todo
- size: S
- paths: `app/src/main/java/wardrobe/app/catalog/SampleWardrobe.java`, `app/src/test/java/wardrobe/app/catalog/SampleWardrobeTest.java`
- verify: `./gradlew :app:test --tests 'wardrobe.app.catalog.SampleWardrobeTest'`
- expect: green; exactly 60 items, every count the description lists is pinned

The 7 seed items of `InMemoryItemCatalog` cannot show suggestions, occasions or weather. Add `final class
SampleWardrobe` with `public static List<Item> items()`: 60 invented items, an immutable list in a stable order
(grouped by `ItemType` order). It is plain data: no Spring annotation, not wired anywhere (WR-048 puts it behind a
profile). Do not touch `InMemoryItemCatalog`.

Rules for the data: invented descriptive English names ("Sage linen shirt"), no brands, no shop or person names;
ids kebab-case `[a-z0-9-]+`, unique. Build it from four capsules so that later suggestions find outfits:
- office (SMART_CASUAL / BUSINESS, mostly neutral and plain): ≥ 4 tops (cotton or silk shirts, a wool knit),
  ≥ 3 bottoms (wool trousers, a skirt), ≥ 2 dresses, ≥ 2 leather shoes (one pair with WINTER), ≥ 2 outerwear (a wool
  coat AUTUMN+WINTER, a cotton trench SPRING+AUTUMN);
- weekend (CASUAL / SMART_CASUAL): ≥ 4 tops (one striped, one floral), ≥ 3 bottoms (jeans, chinos), ≥ 2 dresses (one
  summer linen), ≥ 2 shoes (sneakers; boots with WINTER), ≥ 2 outerwear (a synthetic puffer WINTER, a denim jacket
  SPRING+SUMMER+AUTUMN);
- sport (SPORT / CASUAL): ≥ 2 tops, ≥ 2 bottoms, ≥ 2 shoes (one with WINTER), ≥ 1 synthetic jacket AUTUMN+WINTER;
- evening (BUSINESS / FORMAL): ≥ 2 dresses (silk SUMMER, wool WINTER), ≥ 2 shoes, ≥ 1 wool coat, ≥ 2 accessories.
Every capsule has things for SUMMER and for WINTER; neutral colours dominate, each capsule has a few accents that
are friends by `ColourRule.FRIENDS` (read it).

`SampleWardrobeTest` (plain JUnit, no Spring context) pins: exactly 60; ids unique and kebab-case; per type at least
TOP 14, BOTTOM 10, DRESS 6, OUTERWEAR 7, SHOES 9, ACCESSORY 8; at least 44 CLEAN and at least 3 of each other status;
every `Colour`, `Pattern` and `Fabric` except `UNKNOWN` appears; every `Formality` at least 3 times; every `Season`
on at least 15 items; exactly one item with colour `UNKNOWN`, one with pattern `UNKNOWN`, one with fabric `UNKNOWN`
(three different items, none of them CLEAN — they show "never vouched for" without blocking suggestions).

### WR-039 — Whole-outfit rules: accent colours, a complete outfit, accessories
- status: todo
- size: S
- paths: `domain/src/main/java/wardrobe/domain/outfit/rule/**`, `domain/src/test/java/wardrobe/domain/outfit/rule/WholeRulesTest.java`, `domain/src/test/java/wardrobe/domain/outfit/rule/WholeItems.java`
- verify: `./gradlew :domain:test --tests 'wardrobe.domain.outfit.rule.*'`
- expect: green; each limit has a boundary test (exactly at it and one past it), every code has a test

Pair rules judge two items; these judge an outfit as a whole. New package `wardrobe.domain.outfit.rule`. The rules
take the outfit's items as `List<Item>` (so this task does not wait for WR-006's `Outfit`; callers pass its list).

- `WholeRule` (interface): `WholeVerdict judge(List<Item> items)`. Class doc starts `Pattern: Strategy — one rule
  per way of judging a whole outfit. See docs/adr/0002-stack-and-hexagon.md`.
- `WholeVerdict` (record): `Check check, boolean goes, Code code, List<ItemId> items, String reason`; reason never
  blank (invariant 8); `items` copied. `Check`: ACCENTS, COMPLETE, ACCESSORIES. `Code`: FEW_ACCENTS,
  TOO_MANY_ACCENTS, UNKNOWN_COLOUR, COMPLETE, MISSING_BODY, MISSING_TOP, MISSING_BOTTOM, MISSING_SHOES,
  FEW_ACCESSORIES, TOO_MANY_ACCESSORIES. These names go into `api/openapi.yaml` (WR-049): do not rename them.
- `AccentRule`, `MAX_ACCENTS = 2`: accents are the distinct non-neutral colours (`Colour.neutral()`); one colour on
  two items counts once. Any item with colour `UNKNOWN` → no, UNKNOWN_COLOUR, items = those ids (invariant 5).
  At most 2 → FEW_ACCENTS (reason names the colours, or says all are neutral); more → TOO_MANY_ACCENTS, items = the
  first item of each accent colour in input order, reason names the colours and the limit.
  Public static `List<Colour> AccentRule.accents(List<Item>)` — distinct non-neutral colours in input order
  (UNKNOWN excluded).
- `CompleteRule`: an outfit needs a body (a TOP and a BOTTOM, or a DRESS) and SHOES. Missing ones are checked in
  the order MISSING_BODY (no TOP, BOTTOM or DRESS), MISSING_TOP (a BOTTOM, no TOP, no DRESS), MISSING_BOTTOM (a TOP,
  no BOTTOM, no DRESS), MISSING_SHOES; the code is the first that applies, the reason names everything missing
  ("no bottom and no shoes"), items empty. Otherwise COMPLETE, items = the body and the shoes. This does not change
  `OutfitCheck` (WR-006 decided a missing bottom is not a problem there): this rule is for suggestions and for a
  check the person asks for — write that in `DECISIONS.md`.
- `AccessoryRule`, `MAX_ACCESSORIES = 3`: at most 3 ACCESSORY items → FEW_ACCESSORIES, more →
  TOO_MANY_ACCESSORIES with all accessories as items.
- `WholeRules.standard()` → the three rules in the order accent, complete, accessory; `WholeRules.judgeAll(items)` →
  one verdict per rule in that order.

Status is not judged here (`OutfitCheck` does it: one rule in one place). An empty list: FEW_ACCENTS,
MISSING_BODY, FEW_ACCESSORIES. Tests (helper `WholeItems.item(id, type, colour)` in its own new file): every code;
boundaries 2/3 accents and 3/4 accessories; the order of `judgeAll`; `goes` and `code` do not change over 20
shuffles of the same list (fixed seed). The two limits go in `DECISIONS.md` as Aika's to tune.

### WR-040 — Occasions: office, weekend, evening, sport and the formality each allows
- status: todo
- size: S
- paths: `domain/src/main/java/wardrobe/domain/occasion/**`, `domain/src/test/java/wardrobe/domain/occasion/OccasionTest.java`
- verify: `./gradlew :domain:test --tests 'wardrobe.domain.occasion.*'`
- expect: green; the full table occasion × formality (4 × 5) is pinned, and every code has a test

The formality of the outfit as a whole, against what the day asks for. New package `wardrobe.domain.occasion`.
- `Occasion` enum with constructor arguments `min`, `max` (`Formality`, inclusive) and `allows(Formality)`:
  OFFICE SMART_CASUAL–BUSINESS, WEEKEND CASUAL–SMART_CASUAL, EVENING SMART_CASUAL–FORMAL, SPORT SPORT–CASUAL.
  `allows` is the only place this table is read; later tasks (suggestions prefilter, REST) call it.
- `OccasionCheck.judge(List<Item> items, Occasion occasion)` → `OccasionVerdict` (record: `boolean goes, Code code,
  List<ItemId> items, String reason`). `Code`: FITS_OCCASION, TOO_CASUAL, TOO_FORMAL (names go into
  `api/openapi.yaml`, WR-049 — do not rename). Every item counts, shoes and accessories included. Any item below
  `min` → TOO_CASUAL; else any above `max` → TOO_FORMAL; items = every item outside the range (both sides) in input
  order; the reason names each with its formality and the occasion's range. Otherwise FITS_OCCASION, items empty,
  reason "everything is between SMART_CASUAL and BUSINESS, as OFFICE asks". An empty list → FITS_OCCASION with the
  reason "nothing to judge".

It takes `List<Item>`, not `Outfit`, so it does not wait for WR-006. Tests: the 20 cells of the table, each written
out; each code; one item too casual and one too formal → TOO_CASUAL listing both; reasons never blank. Write the
table in `DECISIONS.md` as Aika's to tune.

### WR-041 — Weather as plain arguments: what the day needs, and a check of an outfit
- status: todo
- depends: WR-037
- size: S
- paths: `domain/src/main/java/wardrobe/domain/weather/**`, `domain/src/test/java/wardrobe/domain/weather/WeatherNeedsTest.java`, `domain/src/test/java/wardrobe/domain/weather/WeatherCheckTest.java`
- verify: `./gradlew :domain:test --tests 'wardrobe.domain.weather.WeatherNeedsTest' && ./gradlew :domain:test --tests 'wardrobe.domain.weather.WeatherCheckTest'`
- expect: green; every band boundary is tested on both sides and every code has a test

No weather service, no API, no clock: the caller passes the weather in. New package `wardrobe.domain.weather`.
- `Weather(int celsius, boolean rain, boolean snow)`: celsius in −60…60 inclusive, else `IllegalArgumentException`
  naming the value.
- `Outerwear` enum: REQUIRED, OPTIONAL, NOT_WANTED. `WeatherBand` enum with constructor arguments (seasons,
  outerwear) and `static of(int celsius)`: FREEZING ≤ 0 {WINTER} REQUIRED; COLD 1…10 {AUTUMN, WINTER, SPRING}
  REQUIRED; MILD 11…18 {SPRING, AUTUMN} OPTIONAL; WARM 19…25 {SPRING, SUMMER} OPTIONAL; HOT ≥ 26 {SUMMER} NOT_WANTED.
- `WeatherNeeds.of(Weather)` → record (`int celsius, WeatherBand band, Set<Season> seasons, Outerwear outerwear,
  boolean snowShoes`): rain or snow make outerwear REQUIRED in every band (a raincoat in the heat too);
  `snowShoes = snow`. `boolean allowsSeason(Item)` = the item's seasons meet `seasons`.
- `WeatherCheck.judge(List<Item> items, Weather)` → exactly four `WeatherVerdict`s in the order SEASON, OUTERWEAR,
  FABRIC, SHOES. `WeatherVerdict` (record): `Check check, boolean goes, Code code, List<ItemId> items, String
  reason`. `Code`: SEASON_FITS, WRONG_SEASON, OUTERWEAR_OK, OUTERWEAR_MISSING, OUTERWEAR_NOT_WANTED, FABRIC_OK,
  TOO_HEAVY_FOR_HEAT, TOO_LIGHT_FOR_COLD, UNKNOWN_FABRIC, SHOES_OK, SHOES_NOT_FOR_SNOW (names go into
  `api/openapi.yaml`, WR-049 — do not rename).
  - SEASON: every item (accessories too) passes `allowsSeason`, else WRONG_SEASON with the items that do not.
  - OUTERWEAR: REQUIRED and no OUTERWEAR → OUTERWEAR_MISSING (the reason says why: the temperature, or rain/snow);
    NOT_WANTED and some OUTERWEAR → OUTERWEAR_NOT_WANTED with those items; else OUTERWEAR_OK.
  - FABRIC (uses `Fabric.weight()` of WR-037 — the one table): in HOT, TOP/BOTTOM/DRESS/OUTERWEAR that are HEAVY →
    TOO_HEAVY_FOR_HEAT; in FREEZING, OUTERWEAR that is LIGHT → TOO_LIGHT_FOR_COLD; failing that, a judged item whose
    weight is UNKNOWN → UNKNOWN_FABRIC (never vouched for); else FABRIC_OK. In other bands → FABRIC_OK, reason
    "fabric weight is not judged at 15 °C". Shoes and accessories are never judged by fabric.
  - SHOES: snow and a SHOES item without WINTER → SHOES_NOT_FOR_SNOW; else SHOES_OK (the reason says which case:
    no snow, winter shoes, or no shoes in the list).

Every reason names the temperature. Tests: boundaries 0/1, 10/11, 18/19, 25/26, and −60/60 accepted, −61/61
rejected; rain makes outerwear REQUIRED in WARM and HOT; each code. Takes `List<Item>`, not `Outfit`. Write the
tables in `DECISIONS.md` as Aika's to tune.

### WR-042 — Varied suggestions: at most two with the same main piece
- status: todo
- size: S
- paths: `domain/src/main/java/wardrobe/domain/suggest/Diversify.java`, `domain/src/test/java/wardrobe/domain/suggest/DiversifyTest.java`
- verify: `./gradlew :domain:test --tests 'wardrobe.domain.suggest.DiversifyTest'`
- expect: green; diversity reorders but never returns fewer than `limit` when there are enough candidates

Generated outfits come in a fixed order, so the first ten can all share one shirt. New class in a new package
`wardrobe.domain.suggest`: `static <T> List<T> Diversify.pick(List<T> ordered, Function<T, List<Item>> itemsOf,
int limit, int maxPerMain)`. Generic over `T` so it works for outfits and for ranked outfits (it does not need
WR-006).

- The main piece of a candidate: its first DRESS, else its first TOP, else none.
- Walk `ordered`; take a candidate while its main piece (by `ItemId`) has been taken fewer than `maxPerMain` times;
  candidates without a main piece are never limited; stop at `limit`.
- If fewer than `limit` were taken, append skipped candidates in their original order until `limit`: diversity
  changes the order, it never shrinks the answer.
- `limit < 0` or `maxPerMain < 1` → `IllegalArgumentException`. The input is not changed. Same input → same output.

Tests with `T = List<Item>` (`itemsOf` = identity; invented items in the test): 5 candidates with the same top and
limit 3, maxPerMain 2 → two of them, then the fill; a dress counts as main before a top; candidates with no main
piece; limit 0; limit larger than the input.

### WR-043 — Do not repeat an outfit worn in the last days
- status: todo
- depends: WR-006, WR-066
- size: S
- paths: `domain/src/main/java/wardrobe/domain/suggest/RepeatCheck.java`, `domain/src/main/java/wardrobe/domain/suggest/RepeatVerdict.java`, `domain/src/main/java/wardrobe/domain/suggest/RepeatContext.java`, `domain/src/test/java/wardrobe/domain/suggest/RepeatCheckTest.java`
- verify: `./gradlew :domain:test --tests 'wardrobe.domain.suggest.RepeatCheckTest'`
- expect: green; the window boundary (a wear exactly `windowDays − 1` days back counts, `windowDays` back does not) is tested

Read `Outfit` (WR-006), `Day`, `WearEvent` and `WearLog` (WR-066) first. A worn outfit is the item ids of one
`WearEvent` on its `Day`; this task only reads such events, it does not log anything (that is WR-071/WR-082).
Package `wardrobe.domain.suggest`; the domain has no clock, so "today" is an argument.

- `RepeatContext` (record): `List<Item> catalogue, List<WearEvent> wears, Day today, int windowDays` (≥ 0, else
  `IllegalArgumentException`; lists copied).
- `RepeatCheck.judge(Outfit candidate, RepeatContext ctx)` → `RepeatVerdict` (record: `boolean goes, Code code,
  List<ItemId> items, String reason`; `Code`: FRESH, REPEATS_OUTFIT — names go into `api/openapi.yaml`, WR-049, do
  not rename).
- A wear counts when `0 ≤ wear.on().daysUntil(today) < windowDays` (today included, future days ignored).
- Compare the sets of non-ACCESSORY item ids: the candidate's from its items; the wear's by looking its ids up in
  `catalogue` (ids not in the catalogue are ignored; accessories are dropped). The same scarf with another outfit
  is not a repeat; the order of items does not matter.
- A match → REPEATS_OUTFIT, items = the repeated ids in the candidate's order, reason "worn together on 2026-11-24,
  2 days ago" (the newest matching wear by day). No match, `windowDays == 0`, no wears, or a candidate with no
  non-accessory item → FRESH with a reason that says which.

Item status is not judged here. Tests: each case above, the window boundary, a future wear, the same items in
another order, an extra accessory, an id missing from the catalogue.

### WR-046 — Outfit generator: clean items into complete outfits, in a fixed order
- status: todo
- depends: WR-005, WR-006
- size: S
- paths: `domain/src/main/java/wardrobe/domain/suggest/OutfitGenerator.java`, `domain/src/main/java/wardrobe/domain/suggest/GenerateRequest.java`, `domain/src/main/java/wardrobe/domain/suggest/ItemNotClean.java`, `domain/src/test/java/wardrobe/domain/suggest/OutfitGeneratorTest.java`, `domain/src/test/java/wardrobe/domain/suggest/GeneratorItems.java`
- verify: `./gradlew :domain:test --tests 'wardrobe.domain.suggest.OutfitGeneratorTest'`
- expect: green; same input gives the same outfits in the same order; every outfit passes `OutfitCheck`; the limit is kept

Read `Combination` (WR-005), `Outfit` and `OutfitCheck` (WR-006) first. Package `wardrobe.domain.suggest`.

`OutfitGenerator(CombinationRule pairs)`; `List<Outfit> generate(List<Item> items, GenerateRequest request)`.
`GenerateRequest` (record): `int limit` (1…1000, else `IllegalArgumentException`), `Optional<ItemId> anchor`,
`boolean outerwearRequired`.

Shape of an outfit: a body (one TOP + one BOTTOM, or one DRESS), one SHOES, then at most one OUTERWEAR and at most
one ACCESSORY. Only items with `availableForOutfit()` (invariant 1). Items inside an outfit are in `ItemType` order
(TOP, BOTTOM, DRESS, OUTERWEAR, SHOES, ACCESSORY).

Order (depth-first, the answer must be reproducible): body choices walk the list in catalogue order — at a TOP,
each BOTTOM in catalogue order; at a DRESS, the dress alone; then SHOES in catalogue order; then OUTERWEAR: "none"
first (unless `outerwearRequired`), then catalogue order; then ACCESSORY: "none" first, then catalogue order.
Prune: when an item is added, `pairs.judge` it with every item already chosen; any no → skip that branch. A full
candidate is kept only when `OutfitCheck.check(outfit, pairs)` is empty — the generator never re-implements a check.
Stop as soon as `limit` outfits are found.

Anchor ("build around this item"): the anchor must be in `items` (else `IllegalArgumentException`) and clean (else
`ItemNotClean`, a `RuntimeException` naming the item and its status — the REST layer maps it to 409). The anchor's
slot is fixed to it: a TOP anchor → bodies are that top with each bottom (no dresses); BOTTOM → each top with it;
DRESS → only it; SHOES → only it; OUTERWEAR → always it ("none" is gone); ACCESSORY → always it.

No item from outside `items` is added; the input list is not changed; an empty answer is a normal answer. Tests
(helper `GeneratorItems` in its own new file, rule `Combination.standard()` and a test rule that says yes to
everything): the exact list of outfits for a 7-item catalogue; determinism; the limit; dirty items never appear;
`outerwearRequired`; each anchor type; an unclean anchor; nothing goes → empty.

### WR-047 — Rank outfits, with a reason for every point
- status: todo
- depends: WR-006, WR-039
- size: S
- paths: `domain/src/main/java/wardrobe/domain/suggest/Ranking.java`, `domain/src/main/java/wardrobe/domain/suggest/RankCriterion.java`, `domain/src/main/java/wardrobe/domain/suggest/Ranked.java`, `domain/src/main/java/wardrobe/domain/suggest/ScoreLine.java`, `domain/src/test/java/wardrobe/domain/suggest/RankingTest.java`
- verify: `./gradlew :domain:test --tests 'wardrobe.domain.suggest.RankingTest'`
- expect: green; every criterion is tested earned and not earned; ties keep the input order

Ranking only orders outfits that already go (the rules decide goes / does not go); it never rejects one.
- `RankCriterion` enum with a constructor argument `points`: SAME_FORMALITY(2) — all non-ACCESSORY items have the
  same formality; AT_MOST_ONE_ACCENT(1) — at most one distinct non-neutral colour over all items, and no `UNKNOWN`
  colour (uses `AccentRule.accents` and AccentRule's UNKNOWN check of WR-039; no second count); COMMON_SEASON(1) — one season shared by every item; ONE_STATEMENT_PATTERN(1) — exactly one item has a
  pattern other than PLAIN, and no item's pattern is `UNKNOWN`. Names go into `api/openapi.yaml` (WR-049): do not
  rename.
- `ScoreLine` (record): `RankCriterion criterion, boolean earned, int points, String reason` (points =
  `criterion.points()` when earned, else 0; reason never blank: "all are BUSINESS" / "formality ranges from CASUAL
  to SMART_CASUAL").
- `Ranked` (record): `Outfit outfit, int score, List<ScoreLine> lines` — one line per criterion in enum order;
  score = the sum.
- `Ranking.rank(List<Outfit>)` → `List<Ranked>`, score descending, ties in input order (a stable sort).

Use `Colour.neutral()` and `Formality` as they are — no second table of neutrals. Tests: each criterion both
ways, the total, the stable order of ties, an empty list. Write the points in `DECISIONS.md` as Aika's to tune.

### WR-048 — Pick the seed catalogue by profile: the sample wardrobe behind `sample`
- status: todo
- depends: WR-007, WR-038
- size: S
- paths: `app/src/main/java/wardrobe/app/catalog/InMemoryItemCatalog.java`, `app/src/main/java/wardrobe/app/catalog/CatalogConfig.java`, `app/src/test/java/wardrobe/app/SampleProfileApiTest.java`
- verify: `./gradlew :app:test --tests 'wardrobe.app.SampleProfileApiTest' && ./gradlew :app:test --tests 'wardrobe.app.ItemsApiTest' --tests 'wardrobe.app.catalog.*'`
- expect: green; with profile `sample` `GET /items` returns the 60 sample items; without it nothing changes

Read `InMemoryItemCatalog` as WR-007 left it, and `SampleWardrobe` (WR-038).
- `InMemoryItemCatalog` gets a public constructor `InMemoryItemCatalog(List<Item> seed)` (copies the list; two
  items with one id → `IllegalArgumentException`). The no-arg constructor stays and seeds the current 7 items
  (existing tests construct it). Remove `@Component` from the class.
- New `CatalogConfig` (`@Configuration`): `@Bean @Profile("!sample") ItemCatalog` → `new InMemoryItemCatalog()`;
  `@Bean @Profile("sample") ItemCatalog` → `new InMemoryItemCatalog(SampleWardrobe.items())`.
- Later API tests build their own small catalogue with `new InMemoryItemCatalog(list)` as a `@Primary` bean in a
  `@TestConfiguration`; make sure that works next to `CatalogConfig` (WR-032's test does the same).

`SampleProfileApiTest`: `@ActiveProfiles("sample")`, real HTTP like `ItemsApiTest`: 60 items, each valid against
the `Item` schema, ids in the order of `SampleWardrobe.items()`. Check by hand that `SPRING_PROFILES_ACTIVE=sample
make dev PORT=8091` serves the sample (stop it after); write how to run it in `DECISIONS.md` (the Makefile is not
yours to change).

### WR-049 — Contract first: suggestions, occasions, weather
- status: todo
- depends: WR-030
- size: S
- paths: `api/openapi.yaml`, `app/src/test/java/wardrobe/app/SuggestionContractTest.java`
- verify: `./gradlew :app:test --tests 'wardrobe.app.SuggestionContractTest'`
- expect: green; good example bodies validate against each new schema, and the bad ones (unknown code, extra property, celsius 61) do not

Only the contract and a test of it; the endpoints come in WR-052…WR-055 and WR-057 (logging wears is
`/wear`, WR-082). Read `api/openapi.yaml` (`Item`, `Error` of WR-008, `WhyLine` of WR-030). Every new object: `additionalProperties: false`, all properties required.

Schemas: `Occasion` enum [OFFICE, WEEKEND, EVENING, SPORT]; `OccasionRange {occasion, min, max}` (formality enum);
`Weather {celsius: integer −60…60, rain, snow}`; `WeatherNeeds {celsius, band: [FREEZING, COLD, MILD, WARM, HOT],
seasons: [season] uniqueItems, outerwear: [REQUIRED, OPTIONAL, NOT_WANTED], snowShoes}`; `CheckName` enum
[PREFILTER, ACCENTS, COMPLETE, ACCESSORIES, OCCASION, SEASON, OUTERWEAR, FABRIC, SHOES, REPEAT, PAIRS]; `CheckCode` enum
[FEW_ACCENTS, TOO_MANY_ACCENTS, UNKNOWN_COLOUR, COMPLETE, MISSING_BODY, MISSING_TOP, MISSING_BOTTOM, MISSING_SHOES,
FEW_ACCESSORIES, TOO_MANY_ACCESSORIES, FITS_OCCASION, TOO_CASUAL, TOO_FORMAL, SEASON_FITS, WRONG_SEASON,
OUTERWEAR_OK, OUTERWEAR_MISSING, OUTERWEAR_NOT_WANTED, FABRIC_OK, TOO_HEAVY_FOR_HEAT, TOO_LIGHT_FOR_COLD,
UNKNOWN_FABRIC, SHOES_OK, SHOES_NOT_FOR_SNOW, FRESH, REPEATS_OUTFIT, NOT_CLEAN, WRONG_FORMALITY, NO_OUTFIT]; `CheckLine {check,
goes, code, items: [string], reason: minLength 1}`; `ScoreLine {criterion: [SAME_FORMALITY, AT_MOST_ONE_ACCENT,
COMMON_SEASON, ONE_STATEMENT_PATTERN], earned, points ≥ 0, reason}`; `Suggestion {items: [Item] minItems 1, score
≥ 0, scoreLines, checks: [CheckLine], why: [WhyLine]}`; `Rejection {check, code, count ≥ 1}`; `SuggestResult
{suggestions, candidatesTried ≥ 0, rejections, missing: [item type enum]}`;
`FitAnswer {ok, checks: [CheckLine], why: [WhyLine]}`. Add `ITEM_NOT_CLEAN` to `Error.code`.

Paths (errors use `Error`): `GET /occasions` → [OccasionRange]. `GET /weather/needs?celsius&rain&snow` (celsius
required) → WeatherNeeds; 400. `GET /outfits/suggestions?limit&occasion&celsius&rain&snow` (limit 1…20, default
10; all optional; rain/snow default false and need celsius) → SuggestResult; 400. `GET /items/{id}/outfits` with
the same query → SuggestResult; 400, 404, 409 (`ITEM_NOT_CLEAN`). `GET /outfits/today?occasion&celsius&rain&snow`
(occasion and celsius required; the day is the server's today) → SuggestResult with at most 3 suggestions,
none repeating a wear of the last 7 days; 400. `POST /outfits/fit` with `{items:
[id] minItems 1, occasion?, weather?: Weather}` → FitAnswer; 400, 404. Describe in words what each answer means (e.g. `missing`: the item types the wardrobe lacks after the
filters, so nothing could be built).

`SuggestionContractTest` (schemas from `classpath:openapi.yaml`, as `ItemsApiTest` does): hand-written JSON
examples — a full SuggestResult with one Suggestion (valid `Item`s and a valid `WhyLine`), an empty one with
rejections and `missing`, each other new schema once — validate; negative examples (code `MAYBE`, an extra property
on Suggestion, celsius 61, Error with an unknown code) fail.

### WR-050 — Suggest outfits: generator, rules, occasion, weather, repeats, ranking in one place
- status: todo
- depends: WR-039, WR-040, WR-041, WR-042, WR-043, WR-046, WR-047, WR-049
- size: S
- paths: `domain/src/main/java/wardrobe/domain/suggest/Suggest.java`, `domain/src/main/java/wardrobe/domain/suggest/SuggestRequest.java`, `domain/src/main/java/wardrobe/domain/suggest/SuggestResult.java`, `domain/src/main/java/wardrobe/domain/suggest/Suggestion.java`, `domain/src/main/java/wardrobe/domain/suggest/CheckLine.java`, `domain/src/main/java/wardrobe/domain/suggest/Rejection.java`, `domain/src/test/java/wardrobe/domain/suggest/SuggestTest.java`, `domain/src/test/java/wardrobe/domain/suggest/SuggestItems.java`
- verify: `./gradlew :domain:test --tests 'wardrobe.domain.suggest.SuggestTest'`
- expect: green; every suggestion's check lines all say yes; an empty answer says why (rejections or missing types)

The one place that composes the pieces; REST tasks only call it. Read the classes of the depends first.

`Suggest(CombinationRule pairs)`, `Suggest.standard()` = `new Suggest(Combination.standard())`. Constants:
`CANDIDATES = 500`, `MAX_PER_MAIN = 2`, `DEFAULT_REPEAT_DAYS = 7`, `TODAY_LIMIT = 3` (Aika's to tune;
`DECISIONS.md`). `SuggestRequest` (record): `int limit` (1…20), `Optional<ItemId> anchor`, `Optional<Occasion>
occasion`, `Optional<Weather> weather`, `Optional<RepeatContext> repeat` (WR-043: catalogue, wears from the
`WearLog` port (WR-066), today, window in days; empty → no repeat check).

`List<CheckLine> checks(Outfit, Optional<Occasion>, Optional<Weather>, Optional<RepeatContext>)`: in this order —
`WholeRules.judgeAll`, then `OccasionCheck` (if an occasion), then `WeatherCheck` (if weather), then `RepeatCheck`
(if a repeat context). `CheckLine` (record): `String check, boolean goes, String code, List<ItemId> items, String reason`
— check and code are the enum names of the source verdicts (names as in `api/openapi.yaml`, WR-049); check =
`OCCASION` for the occasion check, `REPEAT` for the repeat check.

`SuggestResult suggest(List<Item> catalogue, SuggestRequest r)`:
1. Anchor: find it in the catalogue (absent → `IllegalArgumentException`); not clean → `ItemNotClean` (WR-046).
2. Prefilter the pool, first reason wins: not clean → NOT_CLEAN; occasion given and `!occasion.allows(formality)`
   → WRONG_FORMALITY; weather given and `!needs.allowsSeason(item)` → WRONG_SEASON; outerwear NOT_WANTED and the
   item is OUTERWEAR → OUTERWEAR_NOT_WANTED. Count each as a `Rejection("PREFILTER", code, n)`. If the anchor is
   filtered out → no suggestions, that one rejection, nothing else.
3. `missing`: item types needed but absent from the pool, in `ItemType` order — SHOES if none; OUTERWEAR if
   required and none; when there is no body: TOP and/or BOTTOM (whichever is absent) and DRESS.
4. `OutfitGenerator` with `CANDIDATES`, the anchor, `outerwearRequired` from the weather needs. Without an anchor,
   run the generator once per clean TOP/DRESS of the pool as anchor (mains = their number), limit
   `max(MAX_PER_MAIN, CANDIDATES / mains)`, the candidates concatenated in catalogue order. If the generator returns
   no candidate and `missing` is empty → `Rejection("PAIRS", "NO_OUTFIT", 1)`.
5. Keep candidates whose `checks(...)` all say yes; for each rejected one count its first failing line
   (check, code).
6. `Ranking.rank`, then `Diversify.pick(…, r.limit(), MAX_PER_MAIN)`.

`SuggestResult` (record): `List<Suggestion> suggestions, int candidatesTried, List<Rejection> rejections,
List<ItemType> missing`; rejections by count descending, then check order, then code. `Suggestion` (record):
`Outfit outfit, int score, List<ScoreLine> scoreLines, List<CheckLine> checks`. Tests (helper `SuggestItems` in its
own new file): a small catalogue with an expected best outfit; an occasion and a weather that filter; a repeat
skipped; anchor cases; each `missing` case; rejection counts; no candidate with nothing missing →
PAIRS/NO_OUTFIT; 3 tops that each complete an outfit, limit 3 → 3 different main pieces; determinism.

### WR-051 — Items that go into no outfit, and why
- status: todo
- depends: WR-046
- size: S
- paths: `domain/src/main/java/wardrobe/domain/suggest/Orphans.java`, `domain/src/main/java/wardrobe/domain/suggest/Orphan.java`, `domain/src/test/java/wardrobe/domain/suggest/OrphansTest.java`
- verify: `./gradlew :domain:test --tests 'wardrobe.domain.suggest.OrphansTest'`
- expect: green; an item with a partner for every slot but no whole outfit is reported with that reason

"Things bought that go with nothing" (intent 01, problem). `Orphans(CombinationRule pairs)`;
`List<Orphan> find(List<Item> catalogue)`: every CLEAN item for which `new OutfitGenerator(pairs)` with that item
as anchor and limit 1 finds nothing — in catalogue order. `Orphan` (record): `Item item, List<ItemType> missing,
String reason`. `missing`: of TOP, BOTTOM, DRESS, SHOES (other than the item's own type), those for which no clean
item of that type goes with it by `pairs`, in `ItemType` order; a TOP or BOTTOM never lists DRESS, and a DRESS
never lists TOP or BOTTOM. Reason names them ("no clean shoes go with it"); when `missing` is empty, "each part goes
with it, but no whole outfit does". Use the generator; do not write a second outfit search. Tests: a lonely
FORMAL item among SPORT things; a top whose only bottom is in the wash; the empty-missing case; a complete
catalogue → no orphans.

### WR-052 — REST: the occasions and their formality ranges
- status: todo
- depends: WR-040, WR-049
- size: S
- paths: `api/openapi.yaml`, `app/src/main/java/wardrobe/app/web/OccasionsController.java`, `app/src/test/java/wardrobe/app/OccasionsApiTest.java`
- verify: `./gradlew :app:test --tests 'wardrobe.app.OccasionsApiTest'`
- expect: green; one entry per `Occasion`, in enum order, valid against the contract

`GET /occasions` (contract of WR-049) → one `OccasionRange` per `Occasion` constant in enum order, from
`Occasion.min()` / `max()` — so the client never keeps a copy of the table (one rule in one place). A new
controller class; it only reads the domain. `OccasionsApiTest` over real HTTP like `ItemsApiTest`: every body valid
against the schema; the answer has exactly as many entries as `Occasion.values()` and matches them. Fix the
contract only if it is wrong, and say so in the PR.

### WR-053 — REST: what the weather asks for
- status: todo
- depends: WR-041, WR-049
- size: S
- paths: `api/openapi.yaml`, `app/src/main/java/wardrobe/app/web/WeatherController.java`, `app/src/main/java/wardrobe/app/web/WeatherErrors.java`, `app/src/test/java/wardrobe/app/WeatherApiTest.java`
- verify: `./gradlew :app:test --tests 'wardrobe.app.WeatherApiTest'`
- expect: green; −5 with snow, 15, 30 with rain each answer as the domain says; bad input gives 400 `Error`

`GET /weather/needs?celsius=-5&rain=false&snow=true` → `WeatherNeeds` (contract of WR-049) from
`WeatherNeeds.of(new Weather(...))`; seasons in enum order. Missing celsius, a non-number, a value outside
−60…60 → 400 with the `Error` schema, code `BAD_REQUEST`, a message naming the parameter. If the existing advice
(WR-008) does not already turn these into that `Error`, add `WeatherErrors` as `@RestControllerAdvice(assignableTypes
= WeatherController.class)`. Over real HTTP, bodies validated against the contract; one test per band.

### WR-054 — REST: suggested outfits, and outfits built around one item
- status: todo
- depends: WR-048, WR-049, WR-050
- size: S
- paths: `api/openapi.yaml`, `app/src/main/java/wardrobe/app/web/SuggestionsController.java`, `app/src/main/java/wardrobe/app/web/SuggestionViews.java`, `app/src/main/java/wardrobe/app/web/SuggestionErrors.java`, `app/src/test/java/wardrobe/app/SuggestionsApiTest.java`
- verify: `./gradlew :app:test --tests 'wardrobe.app.SuggestionsApiTest'`
- expect: green; both endpoints answer as the contract says, 400/404/409 included, and every domain code is in the contract

`GET /outfits/suggestions` and `GET /items/{id}/outfits` (contract of WR-049). Both call
`Suggest.standard().suggest(catalog.all(), request)` with no repeat context; the around-endpoint
passes the id as anchor. The around-endpoint first calls `catalog.find(id)` and throws `UnknownItem` (WR-007) when
absent; only then calls `Suggest`. `SuggestionViews` maps `SuggestResult` to the contract; a suggestion's `why` comes from
`Explain.outfit(outfit)` (WR-029) through the view WR-030 made for `WhyLine` — reuse it. The controller decides
nothing itself (one rule in one place).

Errors with the `Error` schema: limit outside 1…20, an unknown occasion, celsius bad or outside −60…60, rain or snow
without celsius → 400 `BAD_REQUEST`; unknown id → 404 `UNKNOWN_ITEM`; `ItemNotClean` → 409 `ITEM_NOT_CLEAN`. Add
the handlers the existing advice lacks in `SuggestionErrors` (`@RestControllerAdvice(assignableTypes =
SuggestionsController.class)`).

`SuggestionsApiTest`: a `@TestConfiguration` with a `@Primary ItemCatalog` = `new InMemoryItemCatalog(items)`
(WR-048) holding about 12 invented items built in the test, so the expected best outfit is known. Over real HTTP,
every body validated against the contract: plain suggestions; with an occasion; with weather; around a top; around
an item in the wash (409); unknown id (404); each 400. Plus one test that every constant of the domain's code enums
(`WholeVerdict.Code`, `OccasionVerdict.Code`, `WeatherVerdict.Code`, `RepeatVerdict.Code`, `RankCriterion`) is
accepted by the contract: validate a `CheckLine` / `ScoreLine` built with each name against its schema.

### WR-055 — REST: check an outfit for an occasion and the weather
- status: todo
- depends: WR-048, WR-049, WR-050
- size: S
- paths: `api/openapi.yaml`, `app/src/main/java/wardrobe/app/web/FitController.java`, `app/src/main/java/wardrobe/app/web/FitErrors.java`, `app/src/test/java/wardrobe/app/FitApiTest.java`
- verify: `./gradlew :app:test --tests 'wardrobe.app.FitApiTest'`
- expect: green; the answer lists every check line and the pair-rule `why`; `ok` is true only when all of them say yes

The builder screen of the mockups: the person puts items together and asks "does this do for today?".
`POST /outfits/fit` with `{items, occasion?, weather?}` (contract of WR-049) → `FitAnswer`: `checks` from
`Suggest.standard().checks(outfit, occasion, weather, Optional.empty())` (WR-050), `why` from `Explain.outfit`
(WR-029, reuse WR-030's `WhyLine` view), `ok` = every check line and every why line says yes, and `OutfitCheck`
reports no problems. The controller only calls the domain. CORS for POST exists since WR-030.

Errors with `Error`: an empty list, a malformed body, a bad occasion or weather → 400; an unknown id → 404. Add the
handlers the existing advice lacks in `FitErrors` (`@RestControllerAdvice(assignableTypes = FitController.class)`).
`FitApiTest` over real HTTP with bodies validated against the contract. The test builds its catalogue as a
`@TestConfiguration` with a `@Primary new InMemoryItemCatalog(items)` (WR-048) holding a complete outfit (top,
bottom, shoes, coat) of invented items: the complete outfit without occasion or weather → `ok:true`; top, bottom
and shoes with OFFICE and −5 °C, where outerwear is missing (✗ line); an item in the wash (`ok` false); each error.
Note that a seed shirt + trousers pair gives `ok:false` with MISSING_SHOES (the COMPLETE check), so a pair alone
never proves `ok:true`.

### WR-057 — REST: the outfit of the day
- status: todo
- depends: WR-054, WR-072, WR-077
- size: S
- paths: `api/openapi.yaml`, `app/src/main/java/wardrobe/app/web/TodayController.java`, `app/src/main/java/wardrobe/app/web/TodayErrors.java`, `app/src/test/java/wardrobe/app/TodayApiTest.java`
- verify: `./gradlew :app:test --tests 'wardrobe.app.TodayApiTest'`
- expect: green; at most 3 suggestions; an outfit logged as worn in the last 7 days is not suggested again

The home screen of the mockups: «Образ на сегодня» with the weather. `GET /outfits/today?occasion=OFFICE&celsius=-4
&snow=true` (contract of WR-049) → `SuggestResult` from `Suggest.standard().suggest(...)` with `limit =
Suggest.TODAY_LIMIT` and a `RepeatContext` (WR-043) of `catalog.all()`, `wearLog.all()` (the `WearLog` bean,
WR-072), today = `new Day(LocalDate.now(clock).toEpochDay())` with the `Clock` bean of WR-077, and
`Suggest.DEFAULT_REPEAT_DAYS`. Map with `SuggestionViews` (WR-054). The 400s (occasion and celsius required) come from
`TodayErrors`: a `@RestControllerAdvice` limited to `TodayController` mapping a missing or invalid parameter to 400
`Error`. The controller decides nothing.

`TodayApiTest`: a `@TestConfiguration` with a `@Primary ItemCatalog` (`new InMemoryItemCatalog(items)`, WR-048) of
invented items where at least two outfits fit OFFICE at −4 °C with snow, and a `@Primary Clock` fixed on one day.
Over real HTTP with schema validation: the first suggestion; then add a `WearEvent` of its item ids on yesterday's
`Day` through the `WearLog` bean and ask again — it is not suggested; the same wear 8 days back — it may be
suggested again; missing occasion → 400; a day nothing fits (HOT and only wool) → empty suggestions with
rejections. One scenario per test method, `@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)`.

### WR-059 — REST: items that go into no outfit
- status: todo
- depends: WR-048, WR-051
- size: S
- paths: `api/openapi.yaml`, `app/src/main/java/wardrobe/app/web/OrphansController.java`, `app/src/test/java/wardrobe/app/OrphansApiTest.java`
- verify: `./gradlew :app:test --tests 'wardrobe.app.OrphansApiTest'`
- expect: green; the orphans of a known test catalogue are listed with their missing types and reasons

Contract first: `GET /items/orphans` → `[Orphan {item: Item, missing: [item type enum], reason: minLength 1}]`
(new schema, `additionalProperties: false`). Mind the path order: `/items/orphans` must not be taken for
`/items/{id}`-style routes added by other tasks — test that it is not. The controller calls
`new Orphans(Combination.standard()).find(catalog.all())` (WR-051) and maps it; it decides nothing.
`OrphansApiTest`: a `@TestConfiguration` with a `@Primary ItemCatalog` (`new InMemoryItemCatalog(items)`, WR-048)
of invented items with one lonely FORMAL item and one top whose only bottom is in the wash; over real HTTP with
schema validation.

### WR-060 — Property tests: suggestions over random wardrobes, 2 000 items included
- status: todo
- depends: WR-036, WR-050
- size: S
- paths: `domain/src/test/java/wardrobe/domain/suggest/SuggestPropertiesTest.java`
- verify: `./gradlew :domain:test --tests 'wardrobe.domain.suggest.SuggestPropertiesTest'`
- expect: green; the properties hold for 20 random wardrobes; 2 000 items are answered within 10 seconds

Test only. Wardrobes from `RandomItems.catalogue(seed, n)` (WR-036), seeds 1…20, n = 80, every `Occasion` and
the weathers −10 with snow, 5 with rain, 15, 30:
- every suggested item is clean and from the catalogue; `OutfitCheck.check(outfit, Combination.standard())` is
  empty; every check line says yes; no more than `limit` suggestions;
- the same request twice → equal results (determinism);
- with an anchor, every suggestion contains it;
- an empty answer always has a rejection or a `missing` type (it says why — invariant 8);
- scores are non-increasing within the first run of suggestions before `Diversify` fills (check against
  `Ranking.rank` of the same outfits).
No limit on items (invariant 9): one wardrobe of 2 000 random items answers `suggest` with limit 20 inside
`assertTimeoutPreemptively(Duration.ofSeconds(10), …)`. If a property or the time fails, do not change main code
(not in paths): park with the seed and the case.

### WR-061 — The sample wardrobe has an outfit for every occasion and weather
- status: todo
- depends: WR-038, WR-050
- size: S
- paths: `app/src/main/java/wardrobe/app/catalog/SampleWardrobe.java`, `app/src/test/java/wardrobe/app/catalog/SampleSuggestionsTest.java`
- verify: `./gradlew :app:test --tests 'wardrobe.app.catalog.SampleSuggestionsTest' && ./gradlew :app:test --tests 'wardrobe.app.catalog.SampleWardrobeTest'`
- expect: green; each of the 4 occasions × 5 weathers gets at least one suggestion from the sample wardrobe

So a demo of the app with the `sample` profile never shows an empty "outfit of the day". `SampleSuggestionsTest`
(plain JUnit): for every `Occasion` × weather in {−10 snow, 5 rain, 15, 22, 30}, `Suggest.standard().suggest(
SampleWardrobe.items(), …)` with limit 3 returns at least one suggestion; on failure the message prints the
rejections and `missing` of that case. If a case fails, change the **fields** of existing items in `SampleWardrobe`
(seasons, colour, formality, fabric) — never the rules, and keep everything `SampleWardrobeTest` pins (exactly 60
items, the counts). List the items you changed and why in the PR.


## Wear log, trips and «стоит ли покупать»

### WR-066 — Days and logs in the domain: Day, wear event, status change
- status: todo
- size: S
- paths: `domain/src/main/java/wardrobe/domain/Day.java`, `domain/src/main/java/wardrobe/domain/wear/WearEvent.java`, `domain/src/main/java/wardrobe/domain/wear/WearLog.java`, `domain/src/main/java/wardrobe/domain/history/StatusChange.java`, `domain/src/main/java/wardrobe/domain/history/StatusHistory.java`, `domain/src/test/java/wardrobe/domain/DayTest.java`, `domain/src/test/java/wardrobe/domain/LogRecordsTest.java`
- verify: `./gradlew :domain:test --tests 'wardrobe.domain.DayTest' && ./gradlew :domain:test --tests 'wardrobe.domain.LogRecordsTest'`
- expect: green; `Day` agrees with `java.time.LocalDate` on every day from 1900-01-01 to 2100-12-31; `./gradlew :app:test --tests 'wardrobe.app.ArchitectureTest'` stays green

The wear log, the status history and trips need calendar days. **The domain may not use `java.time`**: the ArchUnit
rule `domainIsPlainJava` in `app/src/test/java/wardrobe/app/ArchitectureTest.java` allows only `java.lang..` and
`java.util..` (and no clock). So the domain gets its own value type; adapters convert with `LocalDate.toEpochDay()`.

1. `wardrobe.domain.Day` — `record Day(long epochDay)` (days since 1970-01-01, like `LocalDate.toEpochDay`),
   `Comparable<Day>`. `static Day of(int year, int month, int dayOfMonth)` (years 1–9999, month 1–12, day valid for
   the month, Gregorian leap years; otherwise `IllegalArgumentException` naming the bad part) using integer
   arithmetic only (e.g. the days-from-civil algorithm); `year()`, `month()`, `dayOfMonth()`; `plusDays(long)`;
   `long daysUntil(Day later)` (= later.epochDay − epochDay, may be negative); `static Day parse(String)` — strict
   `YYYY-MM-DD` (exactly 4-2-2 digits: `2026-2-1`, `2026-02-30`, `2026-02-01T00:00`, blank → IAE); `toString()` is
   the same ISO form, so `parse(d.toString()).equals(d)`.
2. `wardrobe.domain.wear.WearEvent` — record `(Day on, List<ItemId> items)`: not null, items non-empty, no id
   twice (IAE naming the id), list copied (immutable). One event = "these items were worn together on this day".
3. `wardrobe.domain.wear.WearLog` — port (ADR-0002): `void add(WearEvent)`, `List<WearEvent> all()` (in the order
   added, a snapshot).
4. `wardrobe.domain.history.StatusChange` — record `(ItemId item, ItemStatus from, ItemStatus to, Day on)`: not
   null, `from != to` (IAE). Whether the change is allowed is **not** checked here — that is `ItemStatus`'s table,
   one rule in one place.
5. `wardrobe.domain.history.StatusHistory` — port: `void record(StatusChange)`, `List<StatusChange> all()` (in
   the order recorded, a snapshot), and `default List<StatusChange> of(ItemId id)` filtering `all()`.

Tests: `DayTest` — for every day 1900-01-01..2100-12-31, `Day.of(y,m,d).epochDay() == LocalDate.of(y,m,d)
.toEpochDay()` and `toString()` equals `LocalDate.toString()` (tests may use `java.time`; ArchUnit ignores tests);
2024-02-29 valid, 2100-02-29 and 2026-04-31 refused; parse refusals above; `daysUntil` both directions; ordering.
`LogRecordsTest` — `WearEvent` and `StatusChange` validation, immutability of the copied list. No adapters here.

### WR-067 — Price of an item and cost per wear
- status: todo
- size: S
- paths: `domain/src/main/java/wardrobe/domain/wear/Price.java`, `domain/src/main/java/wardrobe/domain/wear/PriceBook.java`, `domain/src/main/java/wardrobe/domain/wear/CostPerWear.java`, `domain/src/test/java/wardrobe/domain/wear/CostPerWearTest.java`
- verify: `./gradlew :domain:test --tests 'wardrobe.domain.wear.CostPerWearTest'`
- expect: green; rounding and "never worn" each have their own test; no floating point anywhere

Money is `long` in whole tenge (₸; minor units = whole tenge in version 1, no tiyn — write this in `DECISIONS.md`).
The price is **not** a field of `Item` (adding a component to the record would break every existing `new Item(…)`
in tests); it lives beside it.

- `Price` — record `(long tenge)`, 0 ≤ tenge ≤ 100 000 000 (IAE otherwise). 0 is allowed (a gift).
- `PriceBook` — port: `Optional<Price> of(ItemId)`, `void set(ItemId, Price)`. An item without a price has an
  unknown price — never 0 (intent 01, invariant 5 in spirit: unknown stays unknown).
- `CostPerWear.of(Price price, int timesWorn)` → `Optional<Price>`: empty when `timesWorn == 0` (never worn: the cost
  per wear is not "the full price", it is not defined yet); otherwise `price / timesWorn` rounded half up in
  integer arithmetic (`(tenge + times / 2) / times`); `timesWorn < 0` → IAE.

Tests: 10 000 ₸ worn 3 times → 3 333; 10 000 worn 6 → 1 667 (half up); 5 worn 2 → 3; 0 worn 4 → 0; never worn →
empty; negative times → IAE; Price bounds (−1, 0, 100 000 000, 100 000 001). Times worn is an argument here: who
counts wears is `WearStats` (another task) — do not count wears in this class.

### WR-068 — Candidate item: what I already own that is like it
- status: todo
- size: S
- paths: `domain/src/main/java/wardrobe/domain/buy/Candidate.java`, `domain/src/main/java/wardrobe/domain/buy/ColourFamily.java`, `domain/src/main/java/wardrobe/domain/buy/Similar.java`, `domain/src/test/java/wardrobe/domain/buy/SimilarTest.java`, `domain/src/test/java/wardrobe/domain/buy/BuyItems.java`
- verify: `./gradlew :domain:test --tests 'wardrobe.domain.buy.SimilarTest'`
- expect: green; every `Colour` has a family (the test iterates `Colour.values()`); similar items come in catalogue order

Package `wardrobe.domain.buy` («стоит ли покупать»). Scope note: intent 01 puts "what to buy" (suggesting purchases)
out of version 1. This feature suggests nothing: it only answers about an item **the person describes herself**
(no shop, no catalogue of products, no model call).

- `Candidate` — record of a card: optional `name` (`Optional<String>`), `type`, `colour`, `pattern`, `fabric`,
  `seasons` (non-empty), `formality`. The person knows what she is looking at: `UNKNOWN` colour, pattern or fabric
  is refused (IAE naming the field). `Item asItem()` → an `Item` with id `ItemId("candidate:new")`, the name or
  "Candidate", status CLEAN — so the existing rules (which take `Item`s) judge it without a second copy of any rule.
- `ColourFamily` — enum DARK_NEUTRAL (BLACK, NAVY, GREY, BROWN), LIGHT_NEUTRAL (WHITE, BEIGE), WARM (RED, PINK,
  ORANGE, YELLOW), COOL (GREEN, BLUE, PURPLE), UNKNOWN (UNKNOWN). `static ColourFamily of(Colour)` is an exhaustive
  `switch` expression **without** `default`, so a new colour does not compile until someone decides its family.
  Write the table in `DECISIONS.md` as Aika's to tune.
- `Similar.to(Candidate, List<Item> owned)` → the owned items with the **same type**, the **same colour family**
  and the **same formality**, in catalogue order. All owned items count, whatever their status (an item in the
  wash is still owned). An owned item of family UNKNOWN is never similar (we cannot vouch for its colour).

Tests (`BuyItems` — a new helper with invented items; do not edit `TestItems`): same type+family+formality →
similar; each of the three differing alone → not similar; an IN_WASH item is similar; NAVY and BLACK are similar,
NAVY and BLUE are not; every `Colour` maps to a family; Candidate refuses UNKNOWN fields; `asItem()` keeps the card.

### WR-069 — Wardrobe gaps: no shoes for BUSINESS
- status: todo
- size: S
- paths: `domain/src/main/java/wardrobe/domain/buy/Gap.java`, `domain/src/main/java/wardrobe/domain/buy/Gaps.java`, `domain/src/test/java/wardrobe/domain/buy/GapsTest.java`, `domain/src/test/java/wardrobe/domain/buy/GapItems.java`
- verify: `./gradlew :domain:test --tests 'wardrobe.domain.buy.GapsTest'`
- expect: green; the seed-like example yields exactly "no SHOES for SMART_CASUAL" and "no SHOES for BUSINESS"

A gap is a kind of item the wardrobe lacks to complete an outfit at some formality. `Gaps.find(List<Item> owned)`
→ `List<Gap>`; `Gap` record `(ItemType slot, Formality formality, Item example)` with `message()` like
"no SHOES that go with BUSINESS (e.g. White shirt)".

Rule: for each owned item `x` whose type is TOP, BOTTOM or DRESS, the slots it needs are TOP → {BOTTOM, SHOES},
BOTTOM → {TOP, SHOES}, DRESS → {SHOES} (one table, a constant). A need is met when some owned item `y` of that slot
passes the existing formality rule: `new FormalityRule().judge(x, y).goes()` — do **not** compare formality steps
yourself (one rule in one place; `FormalityRule.MAX_STEPS` is package-private on purpose). An unmet need is a gap
`(slot, x.formality(), x)`. One gap per `(slot, formality)` — the first example in catalogue order. Order: by
formality (enum order), then slot (enum order). All owned items count, whatever their status (a gap is about owning,
not about today's laundry — write in `DECISIONS.md`). OUTERWEAR and ACCESSORY neither need nor cause gaps in
version 1.

Tests (`GapItems` helper, new file): a copy of the seed catalogue's cards (`InMemoryItemCatalog`: White shirt TOP
BUSINESS, Navy trousers BOTTOM BUSINESS, Blue jeans BOTTOM CASUAL, Striped T-shirt TOP CASUAL, Beige coat
OUTERWEAR, Green dress DRESS SMART_CASUAL, White sneakers SHOES SPORT) gives exactly [SHOES/SMART_CASUAL (Green
dress), SHOES/BUSINESS (White shirt)]; adding BUSINESS shoes removes both; an empty wardrobe has no gaps; a single
TOP gives a BOTTOM gap and a SHOES gap; dedupe keeps the first example.

### WR-070 — Trip needs: how many of each kind to pack
- status: todo
- size: S
- paths: `domain/src/main/java/wardrobe/domain/trip/Trip.java`, `domain/src/main/java/wardrobe/domain/trip/TripNeeds.java`, `domain/src/test/java/wardrobe/domain/trip/TripNeedsTest.java`
- verify: `./gradlew :domain:test --tests 'wardrobe.domain.trip.TripNeedsTest'`
- expect: green; every count rule and every temperature band boundary has a test (exactly at it and one degree past)

Package `wardrobe.domain.trip`. `Trip` — record `(int days, int minTemp, int maxTemp, boolean laundry)`:
temperatures are whole degrees Celsius (expected lows and highs of the whole trip), days 1–60, −50 ≤ minTemp ≤
maxTemp ≤ 50 (IAE naming the field). `TripNeeds.of(Trip)` → record `(Set<Season> seasons, Set<Season> coldestSeasons,
Map<ItemType, Integer> counts)` (EnumSet / EnumMap, enum order).

Seasons from temperatures (one table of bands): t ≤ 0 → WINTER; 1 ≤ t ≤ 15 → SPRING and AUTUMN; t ≥ 16 → SUMMER.
The trip's seasons are the union over every whole degree from minTemp to maxTemp. `coldestSeasons` = the seasons of
minTemp's band alone. Outerwear must suit the coldest day. These trip bands are deliberately a separate table from
WR-041's `WeatherBand` (decision of 26 November: trip counts and temperature bands are their own starting table). Do
not reuse or change `WeatherBand`.

Counts (starting table, write it in `DECISIONS.md` as Aika's to tune; constants in one place):
- TOP: `days` without laundry; `min(days, 4)` with laundry.
- BOTTOM: `ceil(days / 3)` without laundry; `min(ceil(days / 3), 2)` with laundry. Integer arithmetic.
- SHOES: 1.
- OUTERWEAR: 1 when minTemp ≤ 15, else 0.
- DRESS and ACCESSORY: not planned in version 1 (not in the map). Write that in `DECISIONS.md`.

Tests: 1, 3, 4, 7 days with and without laundry; bands at −1/0/1, 15/16; a trip from −5 to 20 has all four
seasons; minTemp 15 vs 16 for outerwear; validation of every field. No items here — choosing items is another task.

### WR-071 — Log what was worn on a day
- status: todo
- depends: WR-066, WR-007
- size: S
- paths: `domain/src/main/java/wardrobe/domain/wear/LogWear.java`, `domain/src/main/java/wardrobe/domain/wear/WearRefused.java`, `domain/src/test/java/wardrobe/domain/wear/LogWearTest.java`, `domain/src/test/java/wardrobe/domain/wear/FakeWearLog.java`
- verify: `./gradlew :domain:test --tests 'wardrobe.domain.wear.LogWearTest'`
- expect: green; a refused log leaves the wear log unchanged

Use case `LogWear(ItemCatalog catalog, WearLog log)` with `WearEvent log(Day on, List<ItemId> items, Day today)`.
Read `WearEvent`, `WearLog`, `Day` (WR-066) and `ItemCatalog.find` / `UnknownItem` (WR-007) first and reuse them.

- `on` after `today` → `WearRefused` ("cannot log wearing on 2026-12-01, it is after today 2026-11-26"). Today is
  an argument: the domain never reads the clock.
- Empty list or an id twice → `WearRefused` (catch the `WearEvent` validation and rethrow, or check first — but
  the rule itself stays in `WearEvent`).
- Any unknown id → `UnknownItem` for the first unknown id in input order; nothing is logged.
- Otherwise append one `WearEvent` and return it.
- The item's status is **not** checked and **not** changed: a past day's status is unknown, and whether wearing
  sends an item to the wash is Aika's open question. Write this in `DECISIONS.md`.
- Logging the same item twice on one day (two events) is allowed; statistics count distinct days.

`FakeWearLog` — a new test helper (a list). Tests: each refusal, with the log still empty afterwards; logging on
today and on a past day; two events on one day both stored, in order.

### WR-072 — In-memory wear log
- status: todo
- depends: WR-066
- size: S
- paths: `app/src/main/java/wardrobe/app/wear/InMemoryWearLog.java`, `app/src/test/java/wardrobe/app/wear/InMemoryWearLogTest.java`
- verify: `./gradlew :app:test --tests 'wardrobe.app.wear.InMemoryWearLogTest'`
- expect: green; 8 threads × 500 adds lose none and `all()` keeps each thread's order

Adapter for the `WearLog` port (WR-066) in `app`: `@Component InMemoryWearLog implements WearLog`, starts empty
(no seed in version 1), safe for concurrent requests (a list guarded by one lock, or `CopyOnWriteArrayList`);
`all()` returns an immutable snapshot that later adds do not change. Memory only — persistence is out of scope for
version 1 (intent 01). Test: plain JUnit (no Spring context needed): concurrency as in `expect`; snapshot does not
change after a later add; order of adds kept for a single thread.

### WR-073 — Wear statistics: times worn, last worn, forgotten, most worn per type
- status: todo
- depends: WR-066
- size: S
- paths: `domain/src/main/java/wardrobe/domain/wear/WearStats.java`, `domain/src/main/java/wardrobe/domain/wear/ItemWear.java`, `domain/src/test/java/wardrobe/domain/wear/WearStatsTest.java`, `domain/src/test/java/wardrobe/domain/wear/WearItems.java`
- verify: `./gradlew :domain:test --tests 'wardrobe.domain.wear.WearStatsTest'`
- expect: green; events outside the period and for unknown ids are ignored; every tie-break has a test

`WearStats.of(List<Item> items, List<WearEvent> events, Optional<Day> from, Day to)` — the period is inclusive;
an empty `from` means "since the first event". `from` after `to` → IAE. Result:

- `List<ItemWear> items()` — one per item, catalogue order: `ItemWear(Item item, int timesWorn, Optional<Day>
  lastWorn, OptionalLong daysSinceLastWorn)`. `timesWorn` = number of **distinct days** in the period on which the
  item was worn (two events on one day count once — write in `DECISIONS.md`); `daysSinceLastWorn` =
  `lastWorn.daysUntil(to)`. Events for ids not in `items` are ignored. All statuses are counted.
- `List<ItemWear> forgotten(int days)` — **clean** items (only they could have been worn: intent 01, invariant 1)
  not worn in the period, or with `daysSinceLastWorn ≥ days`. Order: never worn first (catalogue order), then the
  longest unworn first, ties in catalogue order. `days < 0` → IAE.
- `Map<ItemType, ItemWear> mostWornPerType()` — EnumMap in enum order, only types that have at least one item worn
  at least once in the period: the most worn; tie → the one worn more recently; tie → catalogue order.

This class is the one place that counts wears; cost per wear and REST reuse it. `WearItems` — a new test helper.
Tests: distinct days; period edges (an event exactly on `from` and on `to` counts, one day outside does not); unknown
id ignored; forgotten with an IN_WASH item (excluded), a never-worn clean item (first), the boundary `days` exactly;
most-worn ties both ways; a type with no wear is absent.

### WR-074 — Status timeline: since when, and items stuck in a status
- status: todo
- depends: WR-066
- size: S
- paths: `domain/src/main/java/wardrobe/domain/history/StatusTimeline.java`, `domain/src/main/java/wardrobe/domain/history/Stuck.java`, `domain/src/test/java/wardrobe/domain/history/StatusTimelineTest.java`
- verify: `./gradlew :domain:test --tests 'wardrobe.domain.history.StatusTimelineTest'`
- expect: green; an item without a matching change is "since unknown", never given a guessed day

Read `StatusChange` and `StatusHistory` (WR-066). Queries over a list of changes (plain functions, no port calls):

- `StatusTimeline.since(Item item, List<StatusChange> changes)` → `Optional<Day>`: the day of the **last** change of
  this item; present only when that change's `to` equals the item's current status. No change, or the last change
  disagrees with the current status → empty (the seed items start in their status with no history: since unknown,
  not guessed — intent 01, invariant 5 in spirit).
- `StatusTimeline.longIn(List<Item> items, List<StatusChange> changes, ItemStatus status, int days, Day asOf)` →
  `Stuck(List<Entry> known, List<Item> sinceUnknown)`; `Entry(Item item, Day since, long days)`. Only items now in
  `status`. Known: `since.daysUntil(asOf) ≥ days`, longest first, ties in catalogue order. Items in `status` with an
  unknown since go to `sinceUnknown` in catalogue order (the client can say «давно, точно неизвестно»). `days < 0`
  → IAE. A change dated after `asOf` makes that item's since unknown for this question (do not count negative
  days).

Tests: since after one, two and three changes; disagreeing last change; longIn boundary (`days` exactly and one
less); ordering and ties; future-dated change; CLEAN works the same as the others.

### WR-075 — ChangeStatus records each change in the status history
- status: todo
- depends: WR-066, WR-007
- size: S
- paths: `domain/src/main/java/wardrobe/domain/ChangeStatus.java`, `domain/src/test/java/wardrobe/domain/ChangeStatusHistoryTest.java`, `domain/src/test/java/wardrobe/domain/FakeStatusHistory.java`
- verify: `./gradlew :domain:test --tests 'wardrobe.domain.ChangeStatusHistoryTest' && ./gradlew :domain:test --tests 'wardrobe.domain.ChangeStatusTest'`
- expect: green; WR-007's `ChangeStatusTest` passes untouched; a refused change records nothing

`ChangeStatus` (WR-007) is the one place where a status changes, so it is the one place that records history.
Add a second constructor `ChangeStatus(ItemCatalog catalog, StatusHistory history, Supplier<Day> today)`
(`java.util.function.Supplier` is allowed in the domain). Keep every constructor WR-007 gave it, with the same
behaviour; they record into a no-op history (a private constant), so `ChangeStatusTest` and the app wiring keep
compiling and passing unchanged.

On a successful change: after `replace`, record `StatusChange(id, old status, new status, today.get())` — exactly
once. Unknown item (`UnknownItem`) or a change the table refuses (`IllegalStatusChange`): nothing is recorded and
`today` is not even asked. Do not check the transition table here — `Item.withStatus` does.

`FakeStatusHistory` — new test helper (a list). Use WR-007's `FakeCatalog`. Tests: one change → one record with the
supplied day; two changes on different days in order; refused change and unknown item record nothing; the old
constructor still changes the status. Do not wire anything in `app` — another task does that.

### WR-076 — In-memory status history
- status: todo
- depends: WR-066
- size: S
- paths: `app/src/main/java/wardrobe/app/history/InMemoryStatusHistory.java`, `app/src/test/java/wardrobe/app/history/InMemoryStatusHistoryTest.java`
- verify: `./gradlew :app:test --tests 'wardrobe.app.history.InMemoryStatusHistoryTest'`
- expect: green; 8 threads × 500 records lose none; snapshots do not change afterwards

`@Component InMemoryStatusHistory implements StatusHistory` (port of WR-066): starts empty (seed items have no
history: their "since" is unknown), thread-safe, `all()` an immutable snapshot in record order; `of(id)` keeps that
order. Memory only. Plain JUnit test, no Spring context: concurrency, snapshot, `of` filtering.

### WR-077 — One ChangeStatus bean and the server's clock
- status: todo
- depends: WR-008
- size: S
- paths: `app/src/main/java/wardrobe/app/StatusConfig.java`, `app/src/main/java/wardrobe/app/ClockConfig.java`, `app/src/main/java/wardrobe/app/web/*.java`, `app/src/test/java/wardrobe/app/StatusWiringTest.java`
- verify: `./gradlew :app:test --tests 'wardrobe.app.StatusWiringTest' && ./gradlew :app:test --tests 'wardrobe.app.StatusApiTest'`
- expect: green; the context holds exactly one `ChangeStatus` bean and one `Clock` in zone Asia/Almaty; WR-008's API test unchanged and green

Wiring only, no behaviour change. Later tasks (status history, trip departures) need every status change in the app
to go through **one** `ChangeStatus` instance built in **one** place.

1. `StatusConfig` (`@Configuration`): `@Bean ChangeStatus changeStatus(ItemCatalog catalog)`. Find how WR-008 got
   its `ChangeStatus` (`grep -rn "ChangeStatus" app/src/main`): if its status controller builds one itself, change
   that controller (a `web/Status*.java` file) to take the bean by constructor injection. If WR-008 declared a bean
   elsewhere, move it into `StatusConfig`. If the code that builds it is in a file these paths do not cover, park
   with "missing: path <file>" — do not widen the task.
2. `ClockConfig`: `@Bean Clock clock()` = `Clock.system(ZoneId.of("Asia/Almaty"))`. "Today" on the server is the
   local date in Almaty (Aika's time zone; write in `DECISIONS.md`). Tests that need a fixed day declare their own
   `@TestConfiguration` with a `@Bean @Primary Clock` (`Clock.fixed(…)`); do not add a property for it.

`StatusWiringTest` (`@SpringBootTest`): exactly one bean of type `ChangeStatus`; the `Clock` bean's zone is
Asia/Almaty; a status change over `PUT /items/{id}/status` (real HTTP, an item no other test asserts on, or
`@DirtiesContext`) is visible through the `ItemCatalog` bean.

### WR-078 — Pack for a trip: choose clean items that suit it
- status: todo
- depends: WR-070
- size: S
- paths: `domain/src/main/java/wardrobe/domain/trip/PackTrip.java`, `domain/src/main/java/wardrobe/domain/trip/PackingList.java`, `domain/src/test/java/wardrobe/domain/trip/PackTripTest.java`, `domain/src/test/java/wardrobe/domain/trip/TripItems.java`
- verify: `./gradlew :domain:test --tests 'wardrobe.domain.trip.PackTripTest'`
- expect: green; only clean items are packed; every shortage is reported, none hidden; the same input gives the same list

`PackTrip.pack(Trip trip, List<Item> items, CombinationRule rule)` → `PackingList(TripNeeds needs, Map<ItemType,
List<Item>> chosen, List<Shortage> shortages)`; `Shortage(ItemType slot, int needed, int found)` with `message()`
("need 5 TOP for this trip, found 3 clean that suit it"). Counts and seasons come from `TripNeeds.of(trip)`
(WR-070) — do not recompute them.

- Only CLEAN items (intent 01, invariant 1: only clean items go on a trip — `ItemStatus` refuses wash → trip).
- An item suits the trip when its seasons intersect `needs.seasons()`; OUTERWEAR must have a season in
  `needs.coldestSeasons()`.
- Order of choosing: BOTTOM first (catalogue order); then TOP — first the tops that go with at least one chosen
  bottom by `rule`, catalogue order; then SHOES the same way against the chosen bottoms; then OUTERWEAR (catalogue
  order). If no bottom was chosen, tops and shoes are taken in catalogue order. A slot with fewer suitable items
  than needed gets all of them and a `Shortage`; a slot needing 0 is absent from `chosen`.
- The rule is an argument (the REST task passes `Combination.standard()`); tests pass a small stub rule. Do not
  judge colours or formality here.

`TripItems` — new helper. Tests: a 3-day trip, no laundry, enough items; an IN_WASH top never chosen; a summer-only
top not chosen for a winter trip; outerwear for a trip from 5 to 25 degrees needs SPRING or AUTUMN; tops that go
with the chosen bottom come first; shortage counts; determinism (two calls → equal lists).

### WR-079 — Go on a trip and come back: all items or none
- status: todo
- depends: WR-007
- size: S
- paths: `domain/src/main/java/wardrobe/domain/trip/TripMoves.java`, `domain/src/main/java/wardrobe/domain/trip/TripRefused.java`, `domain/src/test/java/wardrobe/domain/trip/TripMovesTest.java`, `domain/src/test/java/wardrobe/domain/trip/TripCatalog.java`
- verify: `./gradlew :domain:test --tests 'wardrobe.domain.trip.TripMovesTest'`
- expect: green; when one item cannot move, no item moves

`TripMoves(ItemCatalog catalog, ChangeStatus changeStatus)` (WR-007's use case and port).

- `List<Item> depart(List<ItemId> ids)`: every item goes to ON_TRIP. Empty list or an id twice → IAE. First check
  **all**: unknown id → `UnknownItem` (first in input order); an item whose status cannot become ON_TRIP
  (`ItemStatus.canBecome` — the table, not a copy of it) → collected; if any → `TripRefused(List<String> problems)`
  naming each item and its status ("Blue jeans is IN_WASH: only clean items go on a trip"), nothing changed. Then
  change each through `changeStatus.change(id, ON_TRIP)` — so the history (another task) is recorded in its one
  place.
- `List<Item> comeBack(Map<ItemId, ItemStatus> returns)` (a `LinkedHashMap`, input order kept): each target must be
  CLEAN or IN_WASH (IAE otherwise), each item must be ON_TRIP now (else collected into `TripRefused`); all checked
  first, then all changed.
- Version 1 is one person on one phone: the check-then-change is not atomic against concurrent requests; say so in
  the class doc and in `DECISIONS.md`.

`TripCatalog` — a new in-memory test catalogue (do not edit WR-007's `FakeCatalog`; you may use it if it fits).
Tests: depart three clean items; one IN_WASH among them → refused and all three unchanged; unknown id; duplicates;
come back mixed CLEAN/IN_WASH; a target of IN_REPAIR refused; an item not on a trip refused, others unchanged.

### WR-080 — Pairs and outfits a candidate would open
- status: todo
- depends: WR-068, WR-006
- size: S
- paths: `domain/src/main/java/wardrobe/domain/buy/Opens.java`, `domain/src/test/java/wardrobe/domain/buy/OpensTest.java`
- verify: `./gradlew :domain:test --tests 'wardrobe.domain.buy.OpensTest'`
- expect: green; a pair or outfit counts only when `OutfitCheck` finds no problem; counts are exact on a hand-checked example

`Opens.with(Candidate c, List<Item> owned, CombinationRule rule)` → record `(List<Pair> pairs, OptionalInt
outfits, List<List<Item>> outfitExamples)`. Only **clean** owned items take part (intent 01, invariant 1). Use the
candidate's `asItem()` (WR-068) and the existing `OutfitCheck.check(Outfit, CombinationRule)` (WR-006): this class
decides nothing itself.

- A pair: the candidate and one clean owned item such that `OutfitCheck.check(new Outfit(List.of(cand, item)),
  rule)` is empty (so two shoes, a dress with a top, or a clash are not pairs). `Pair(Item item, String reason)`
  with the rule's verdict reason. Catalogue order.
- Basic outfits: {TOP, BOTTOM, SHOES} and {DRESS, SHOES}. If the candidate's type is in a basic outfit, `outfits`
  is the number of distinct basic outfits containing the candidate plus clean owned items that `OutfitCheck` passes;
  `outfitExamples` the first three in catalogue order (nested loops in catalogue order). For OUTERWEAR and
  ACCESSORY `outfits` is empty (not part of a basic outfit in version 1 — write in `DECISIONS.md`).
- No limit on items (invariant 9): count without building all outfits in memory at once.
- Do not use `OutfitGenerator` (WR-046): it caps at 1000 outfits (invariant 9 needs an exact count) and adds
  outerwear/accessory variants. Here basic outfits are counted, not generated. Write this in `DECISIONS.md`.

Tests with a stub rule (goes unless both items are in a "clash" set) and with `Combination.standard()` (WR-005) on a
small invented wardrobe whose counts you work out by hand in a comment.

### WR-081 — Should I buy it: a verdict with reasons
- status: todo
- depends: WR-068, WR-069
- size: S
- paths: `domain/src/main/java/wardrobe/domain/buy/ShouldBuy.java`, `domain/src/main/java/wardrobe/domain/buy/BuyVerdict.java`, `domain/src/test/java/wardrobe/domain/buy/ShouldBuyTest.java`
- verify: `./gradlew :domain:test --tests 'wardrobe.domain.buy.ShouldBuyTest'`
- expect: green; every answer comes with at least three reasons (similar, pairs, gaps); each verdict rule has its own test

`ShouldBuy.judge(Candidate c, List<Item> similar, int pairs, List<Gap> gaps)` → `BuyVerdict(Answer answer,
List<Gap> fills, List<BuyReason> reasons)`. `Answer`: BUY, THINK, SKIP. `BuyReason(Code code, String text)`;
codes: NO_SIMILAR, ALREADY_HAVE_SIMILAR, OPENS_PAIRS, OPENS_NO_PAIRS, FILLS_GAP, FILLS_NO_GAP. The inputs come from
`Similar.to` (WR-068), the pair count of `Opens` (another task; here a plain `int`) and `Gaps.find` (WR-069) — this
class does not recompute them.

- Which gaps it fills: `gap.slot() == c.type()` and `new FormalityRule().judge(c.asItem(), gap.example()).goes()`.
- The answer, rules in this order (one table of thresholds; write it in `DECISIONS.md` as Aika's to tune):
  1. `pairs == 0` → SKIP ("nothing clean to wear it with");
  2. fills at least one gap → BUY;
  3. `similar.size() ≥ 2` → SKIP;
  4. otherwise THINK.
- Reasons (invariant 8: no verdict without reasons): always one about similar items (count and names, or
  NO_SIMILAR), one about pairs (the count), and one per filled gap or a single FILLS_NO_GAP — in that order.
- `pairs < 0` → IAE.

Tests: one per rule, including order (a candidate that fills a gap but opens no pairs is SKIP; one that fills a gap
with two similar owned is BUY); reasons always ≥ 3 and never blank; a SHOES candidate of BUSINESS fills the
"no SHOES for BUSINESS" gap, one of SPORT does not.

### WR-082 — REST: log a wear and read the wear log
- status: todo
- depends: WR-071, WR-072, WR-077
- size: S
- paths: `api/openapi.yaml`, `app/src/main/java/wardrobe/app/web/WearController.java`, `app/src/main/java/wardrobe/app/web/WearErrors.java`, `app/src/main/java/wardrobe/app/web/CorsConfig.java`, `app/src/test/java/wardrobe/app/WearApiTest.java`, `app/src/test/java/wardrobe/app/WearCorsTest.java`
- verify: `./gradlew :app:test --tests 'wardrobe.app.WearApiTest'`
- expect: green; 201 / 400 / 404 checked against the contract; a refused request logs nothing

Contract first (`api/openapi.yaml`, new schemas under `components.schemas`): `POST /wear` with `{ "on":
"2026-11-26", "items": ["white-shirt", "navy-trousers"] }` → 201 `WearEvent {on, items}`; `on` optional (default:
today from the `Clock` bean of WR-077, `LocalDate.now(clock)`); `GET /wear?from=&to=` (both optional, inclusive,
`format: date`) → `[WearEvent]` in the order logged. Errors use WR-008's `Error` schema and codes: unknown id → 404
`UNKNOWN_ITEM` (WR-008's advice already maps `UnknownItem`); `WearRefused`, a bad date, an empty list → 400
`BAD_REQUEST` with the reason in `message` — map `WearRefused` in `WearErrors`
(`@RestControllerAdvice(assignableTypes = WearController.class)`) if no existing advice does.

`WearController` builds `new LogWear(catalog, wearLog)` from the injected beans and converts dates with
`Day.parse` / `new Day(localDate.toEpochDay())` — no date rules in the controller. CORS: if `CorsConfig` does not
allow POST yet, add it for the same local origins with a preflight test in `WearCorsTest` (`OPTIONS`, `Origin:
http://localhost:8081`, `Access-Control-Request-Method: POST`, `Access-Control-Request-Headers: content-type`).

`WearApiTest` (real HTTP like `ItemsApiTest`, bodies validated against the contract): log with and without `on`
(fixed clock via a `@TestConfiguration` `@Primary Clock` in this test), a future date → 400, unknown item → 404 and
`GET /wear` unchanged, filter by `from`/`to`. The Spring context is shared: use `@DirtiesContext` or assert only on
events this test logged.

### WR-083 — REST: wear statistics per item
- status: todo
- depends: WR-072, WR-073, WR-077
- size: S
- paths: `api/openapi.yaml`, `app/src/main/java/wardrobe/app/web/WearStatsController.java`, `app/src/test/java/wardrobe/app/WearStatsApiTest.java`
- verify: `./gradlew :app:test --tests 'wardrobe.app.WearStatsApiTest'`
- expect: green; the body matches the contract; never-worn items have `lastWorn: null`

Contract first: `GET /stats/wear?from=&to=` (`format: date`, both optional; `to` defaults to today from the `Clock`
bean of WR-077) → `{ "from": date|null, "to": date, "items": [ ItemWear ] }`, `ItemWear = { item: Item, timesWorn:
integer ≥ 0, lastWorn: date|null, daysSinceLastWorn: integer|null }`, catalogue order. `from` after `to` or a bad
date → 400 `Error` (`BAD_REQUEST`). The controller calls `WearStats.of(catalog.all(), wearLog.all(), …)` (WR-073)
and maps; it counts nothing itself (one rule in one place). Reuse `ItemView` for `item` (if `ItemView.of` is
package-private, the controller is in the same package `wardrobe.app.web`).

`WearStatsApiTest`: autowire the `WearLog` bean and add invented events directly (no dependency on the POST
endpoint); fixed clock in a `@TestConfiguration`; check counts of distinct days, `lastWorn`, a never-worn item, the
period filter; validate bodies against the contract. Shared context: `@DirtiesContext`.

### WR-084 — REST: forgotten items and most worn per type
- status: todo
- depends: WR-072, WR-073, WR-077, WR-083
- size: S
- paths: `api/openapi.yaml`, `app/src/main/java/wardrobe/app/web/WearHighlightsController.java`, `app/src/test/java/wardrobe/app/WearHighlightsApiTest.java`
- verify: `./gradlew :app:test --tests 'wardrobe.app.WearHighlightsApiTest'`
- expect: green; bodies match the contract; the order is the domain's

Contract first: `GET /stats/forgotten?days=30&to=` (`days` integer ≥ 0, default 30; `to` default today from the
`Clock` of WR-077) → `{ "to": date, "days": n, "items": [ItemWear] }` — the clean items not worn for `days` days, in
`WearStats.forgotten` order (never worn first). `GET /stats/most-worn?from=&to=` → `{ "to": date, "types": [ {
"type": ItemType, "item": ItemWear } ] }` in enum order. Reuse the `ItemWear` schema if WR-083 already added it
(read `api/openapi.yaml` first); if not, add it with exactly the shape `{ item: Item, timesWorn, lastWorn,
daysSinceLastWorn }` so the two tasks agree. Bad parameters → 400 `Error`.

The controller only calls `WearStats` (WR-073). Test over real HTTP with events added to the `WearLog` bean, a fixed
clock, `@DirtiesContext`: an item worn long ago comes after never-worn ones; an IN_WASH seed item (`blue-jeans`) is
never forgotten; most-worn tie broken by the later day.

### WR-085 — REST: set and read an item's price
- status: todo
- depends: WR-067, WR-008
- size: S
- paths: `api/openapi.yaml`, `app/src/main/java/wardrobe/app/price/InMemoryPriceBook.java`, `app/src/main/java/wardrobe/app/web/PriceController.java`, `app/src/test/java/wardrobe/app/PriceApiTest.java`
- verify: `./gradlew :app:test --tests 'wardrobe.app.PriceApiTest'`
- expect: green; 200 / 400 / 404 against the contract; a price never set reads as null, not 0

`@Component InMemoryPriceBook implements PriceBook` (WR-067), thread-safe map, empty at start. Contract first:
`PUT /items/{id}/price` with `{ "tenge": 12000 }` → 200 `ItemPrice { itemId, tenge: integer|null }`;
`GET /items/{id}/price` → 200 `ItemPrice` (`tenge: null` when unknown). Unknown item → 404 `UNKNOWN_ITEM` (check the
id against `ItemCatalog` — use `find` from WR-007, throw `UnknownItem`); negative, too large or non-integer tenge →
400 `BAD_REQUEST`. `tenge` is whole tenge (₸), `format: int64` in the contract. PUT is already allowed by CORS after
WR-008.

Test over real HTTP, bodies validated: set then read; read unknown price → null; 404; 400 for −1 and 100 000 001;
`@DirtiesContext`.

### WR-086 — REST: cost per wear
- status: todo
- depends: WR-085, WR-073, WR-072, WR-077
- size: S
- paths: `api/openapi.yaml`, `app/src/main/java/wardrobe/app/web/CostPerWearController.java`, `app/src/test/java/wardrobe/app/CostPerWearApiTest.java`
- verify: `./gradlew :app:test --tests 'wardrobe.app.CostPerWearApiTest'`
- expect: green; an item without a price or never worn has `perWear: null`; numbers are integers

Contract first: `GET /stats/cost-per-wear?to=` (default today from the `Clock` bean of WR-077) → `{ "to": date, "items": [ { itemId, tenge:
integer|null, timesWorn, perWear: integer|null } ] }`, catalogue order. `timesWorn` from `WearStats.of(…,
Optional.empty(), to)` (WR-073, all wears up to `to`); `perWear` from `CostPerWear.of` (WR-067) only when the price
is known. Nothing is computed in the controller beyond calling these two.

Test: set prices through the `PriceBook` bean, add events through the `WearLog` bean, fixed clock: 10 000 ₸ worn on
3 distinct days → 3 333; worn twice on one day counts once; no price → null; never worn → null. `@DirtiesContext`.

### WR-087 — Record status history in the app and show it
- status: todo
- depends: WR-074, WR-075, WR-076, WR-077
- size: S
- paths: `api/openapi.yaml`, `app/src/main/java/wardrobe/app/StatusConfig.java`, `app/src/main/java/wardrobe/app/web/StatusHistoryController.java`, `app/src/test/java/wardrobe/app/StatusHistoryApiTest.java`
- verify: `./gradlew :app:test --tests 'wardrobe.app.StatusHistoryApiTest'`
- expect: green; a PUT of a status adds exactly one history entry dated by the server clock; the body matches the contract

1. In `StatusConfig` (WR-077) build the `ChangeStatus` bean with the recording constructor of WR-075:
   `new ChangeStatus(catalog, history, () -> new Day(LocalDate.now(clock).toEpochDay()))`, `history` = the
   `InMemoryStatusHistory` bean (WR-076), `clock` = the `Clock` bean (WR-077). Nothing else changes: every status
   change (PUT, trips) now goes through this one bean.
2. Contract first: `GET /items/{id}/history` → `{ "item": Item, "since": date|null, "changes": [ { from, to, on } ]
   }` (statuses as `Item.status` enum, `on` `format: date`), changes oldest first. `since` from
   `StatusTimeline.since` (WR-074). Unknown id → 404 `UNKNOWN_ITEM`.

`StatusHistoryApiTest` (real HTTP, fixed clock via `@TestConfiguration @Primary Clock`, `@DirtiesContext`): a seed
item has `since: null` and no changes; `PUT /items/{id}/status` CLEAN → IN_WASH then IN_WASH → CLEAN gives two
entries with the clock's day and `since` = that day; a refused PUT (409) adds nothing.

### WR-088 — REST: items stuck in the wash (or repair) for N days
- status: todo
- depends: WR-074, WR-076, WR-077
- size: S
- paths: `api/openapi.yaml`, `app/src/main/java/wardrobe/app/web/StuckController.java`, `app/src/test/java/wardrobe/app/StuckApiTest.java`
- verify: `./gradlew :app:test --tests 'wardrobe.app.StuckApiTest'`
- expect: green; known and since-unknown items are separate lists; bodies match the contract

Contract first: `GET /stats/stuck?status=IN_WASH&days=3&asOf=` (`status` one of the four, default IN_WASH; `days`
integer ≥ 0, default 3; `asOf` date, default today from the `Clock` of WR-077) → `{ "status", "days", "asOf",
"known": [ { item: Item, since: date, days: integer } ], "sinceUnknown": [Item] }`. Bad parameters → 400 `Error`.
The controller only calls `StatusTimeline.longIn(catalog.all(), history.all(), …)` (WR-074).

Test: record `StatusChange`s directly into the `StatusHistory` bean (do not depend on the PUT wiring), fixed clock,
`@DirtiesContext`: the seed's `blue-jeans` (IN_WASH, no history) is in `sinceUnknown`; a recorded change into
IN_WASH 5 days ago for an item that is now IN_WASH (change its status through the `ItemCatalog` bean's `replace`)
appears in `known` with `days: 5` for `days=3` and not for `days=6`.

### WR-089 — REST: plan what to pack for a trip
- status: todo
- depends: WR-078, WR-005, WR-008
- size: S
- paths: `api/openapi.yaml`, `app/src/main/java/wardrobe/app/web/TripPlanController.java`, `app/src/main/java/wardrobe/app/web/CorsConfig.java`, `app/src/test/java/wardrobe/app/TripPlanApiTest.java`, `app/src/test/java/wardrobe/app/TripCorsTest.java`
- verify: `./gradlew :app:test --tests 'wardrobe.app.TripPlanApiTest'`
- expect: green; only clean items in the answer; shortages listed; 400 for an invalid trip, all against the contract

Contract first: `POST /trips/plan` with `{ "days": 4, "minTemp": -5, "maxTemp": 8, "laundry": false }` →
`{ "seasons": [Season], "slots": [ { "type", "needed", "items": [Item] } ], "shortages": [ { "type", "needed",
"found", "message" } ] }`, slots in enum order. A plan changes nothing (no status change). Invalid fields → 400
`Error` `BAD_REQUEST` naming the field. The controller calls `PackTrip.pack(trip, catalog.all(),
Combination.standard())` (WR-078, WR-005) and maps. CORS: if `CorsConfig` does not allow POST yet, add it with a
preflight test in `TripCorsTest` (as in WR-021).

Test over the seed catalogue (real HTTP, bodies validated): a 3-day summer trip lists only CLEAN items (never
`blue-jeans`, `beige-coat`, `green-dress`); a long winter trip reports shortages; `days: 0` → 400.

### WR-090 — REST: depart on a trip and come back
- status: todo
- depends: WR-079, WR-077
- size: S
- paths: `api/openapi.yaml`, `app/src/main/java/wardrobe/app/web/TripMovesController.java`, `app/src/main/java/wardrobe/app/web/TripErrors.java`, `app/src/main/java/wardrobe/app/web/CorsConfig.java`, `app/src/test/java/wardrobe/app/TripMovesApiTest.java`, `app/src/test/java/wardrobe/app/TripMovesCorsTest.java`
- verify: `./gradlew :app:test --tests 'wardrobe.app.TripMovesApiTest'`
- expect: green; a refused departure changes no item; 200 / 400 / 404 / 409 against the contract

Contract first: `POST /trips/departures` `{ "items": ["white-shirt", "striped-tee"] }` → 200 `{ "items": [Item] }`
(all now ON_TRIP); `POST /trips/returns` `{ "items": [ { "id": "white-shirt", "status": "IN_WASH" }, … ] }` → 200
`{ "items": [Item] }`. Errors (`Error` of WR-008): unknown id → 404 `UNKNOWN_ITEM`; `TripRefused` → 409
`ILLEGAL_STATUS_CHANGE` with every problem in `message` (map it in `TripErrors`, scoped to this controller); empty
list, duplicates, a return status other than CLEAN/IN_WASH → 400 `BAD_REQUEST`.

The controller builds `new TripMoves(catalog, changeStatus)` (WR-079) with the **`ChangeStatus` bean** of WR-077 —
never `new ChangeStatus(…)` — so history (when wired) records trips too. CORS POST as in WR-089 if not yet allowed
(`TripMovesCorsTest`).

Test over real HTTP, `@DirtiesContext`: depart two clean seed items → ON_TRIP in `GET /items`; depart with
`blue-jeans` (IN_WASH) → 409 and every listed item unchanged; return one CLEAN and one IN_WASH; return an item not
on a trip → 409.

### WR-091 — REST: what the wardrobe lacks
- status: todo
- depends: WR-069, WR-008
- size: S
- paths: `api/openapi.yaml`, `app/src/main/java/wardrobe/app/web/GapsController.java`, `app/src/test/java/wardrobe/app/GapsApiTest.java`
- verify: `./gradlew :app:test --tests 'wardrobe.app.GapsApiTest'`
- expect: green; on the seed catalogue the answer is SHOES for SMART_CASUAL and SHOES for BUSINESS, valid against the contract

Contract first: `GET /wardrobe/gaps` → `{ "gaps": [ { "slot": ItemType, "formality": Formality, "example": Item,
"message": string } ] }` in the domain's order. The controller only calls `Gaps.find(catalog.all())` (WR-069).
Test over real HTTP with schema validation on the seed catalogue (`InMemoryItemCatalog`): exactly the two gaps of
`expect`, in that order. If the seed catalogue has changed since this task was written, work the expected gaps out
from the seed and the rule in WR-069 and write them in the test with a comment — do not change the rule.

### WR-092 — REST: should I buy this?
- status: todo
- depends: WR-080, WR-081, WR-091
- size: S
- paths: `api/openapi.yaml`, `app/src/main/java/wardrobe/app/web/ShouldBuyController.java`, `app/src/main/java/wardrobe/app/web/CorsConfig.java`, `app/src/test/java/wardrobe/app/ShouldBuyApiTest.java`, `app/src/test/java/wardrobe/app/ShouldBuyCorsTest.java`
- verify: `./gradlew :app:test --tests 'wardrobe.app.ShouldBuyApiTest'`
- expect: green; every answer has a verdict, its reasons and the numbers it is based on, valid against the contract; nothing is stored

Contract first: `POST /buy/check` with a candidate card `{ "name"?, "type", "colour", "pattern", "fabric",
"seasons", "formality" }` (enums as in `Item`; UNKNOWN colour/pattern/fabric → 400) → `{ "answer": BUY|THINK|SKIP,
"reasons": [ { code, text } ], "similar": [Item], "pairs": [ { item: Item, reason } ], "outfits": integer|null,
"outfitExamples": [[itemId]], "fills": [Gap] }`. Reuse WR-091's `Gap` schema.
Nothing is stored; the candidate never joins the catalogue.

The controller composes the domain and decides nothing: `Similar.to` (WR-068), `Opens.with(…,
Combination.standard())` (WR-080), `Gaps.find` (WR-069), then `ShouldBuy.judge(c, similar, pairs.size(), gaps)`
(WR-081). CORS POST as in WR-089 if not yet allowed (`ShouldBuyCorsTest`).

Test on the seed catalogue (real HTTP, bodies validated): black plain leather BUSINESS shoes for autumn–spring fill
the BUSINESS shoes gap and pair with the clean White shirt and Navy trousers → BUY (work it out from the seed and
the rules, write the reasoning in a comment; if the result differs, check the rules, not the test data); a second
white business shirt has the White shirt as similar; UNKNOWN colour → 400.


## Items, cards, collections and client logic

### WR-096 — Readable ids from names: one slug rule for items and collections
- status: todo
- size: S
- paths: `domain/src/main/java/wardrobe/domain/Slugs.java`, `domain/src/test/java/wardrobe/domain/SlugsTest.java`
- verify: `./gradlew :domain:test --tests 'wardrobe.domain.SlugsTest'`
- expect: green; Russian, Kazakh and Latin names give stable latin ids, a taken id gets the first free suffix

New items (WR-116) and collections (WR-107) need ids made from what the person typed, like the seed ids
`white-shirt`, `navy-trousers` in `app/src/main/java/wardrobe/app/catalog/InMemoryItemCatalog.java`. This task
writes the one rule for that; it changes no existing class.

`wardrobe.domain.Slugs` (plain Java; the ArchUnit test in `app/` allows only `java.lang..` and `java.util..` in
the domain, so no `java.text.Normalizer`): `static String next(String name, Set<String> taken)`.
- Lower-case with `Locale.ROOT`. Latin letters and digits stay. Cyrillic is transliterated through **one** table
  in the class: а→a, б→b, в→v, г→g, д→d, е→e, ё→e, ж→zh, з→z, и→i, й→y, к→k, л→l, м→m, н→n, о→o, п→p, р→r, с→s,
  т→t, у→u, ф→f, х→kh, ц→ts, ч→ch, ш→sh, щ→shch, ъ→(nothing), ы→y, ь→(nothing), э→e, ю→yu, я→ya, and the Kazakh
  letters ә→a, ғ→g, қ→k, ң→n, ө→o, ұ→u, ү→u, һ→h, і→i.
- Every other character (spaces, punctuation, emoji, other alphabets) becomes a hyphen; runs of hyphens collapse
  to one; no hyphen at either end.
- At most 40 characters: cut, then drop a trailing hyphen.
- Nothing left (blank name, only symbols) → `item`.
- If the result is in `taken`, append `-2`, `-3`, … and return the first one not taken (the suffix counts within
  the 40 characters: cut the base so base + suffix ≤ 40).
- Deterministic: the same name and the same `taken` always give the same id. No random, no clock.

Tests in `SlugsTest` (new file): «Белая рубашка» → `belaya-rubashka`; «Қызыл көйлек» → `kyzyl-koylek`;
"Navy  Trousers!!" → `navy-trousers`; "  " → `item`; "★★★" → `item`; a 60-character name is cut to ≤ 40 without a
trailing hyphen; taken {`white-shirt`, `white-shirt-2`} → `white-shirt-3`; a 40-character base that is taken gets a
shorter base and `-2`, total ≤ 40; every result matches `[a-z0-9]+(-[a-z0-9]+)*`. Write the transliteration choice
(simple table, not a standard, Kazakh letters included) in `DECISIONS.md` under «Backlog run (December 2026)».

### WR-097 — Filter and search items in the domain: type, colour, season, status, words of the name
- status: todo
- size: S
- paths: `domain/src/main/java/wardrobe/domain/ItemQuery.java`, `domain/src/test/java/wardrobe/domain/ItemQueryTest.java`, `domain/src/test/java/wardrobe/domain/QueryItems.java`
- verify: `./gradlew :domain:test --tests 'wardrobe.domain.ItemQueryTest'`
- expect: green; each filter alone, two filters together, the empty query and Russian text search are tested

The wardrobe screen will search and filter («Шкаф» in `docs/design/mockups/mockups.html`). The rule of what a
filter means lives here once; the REST task WR-113 only reads query parameters into an `ItemQuery`.

`wardrobe.domain.ItemQuery` (record): `Set<ItemType> types`, `Set<Colour> colours`, `Set<Season> seasons`,
`Set<ItemStatus> statuses`, `String text` (null or blank = no text filter). Defensive copies; `ItemQuery.all()` =
everything empty. `boolean matches(Item)` and `List<Item> filter(List<Item>)` (keeps the input order, returns a new
list). Meaning:
- an empty set does not filter; within one field the values are alternatives (TOP or DRESS); different fields must
  all hold (AND);
- seasons: an item matches when it shares **at least one** season with the set (a winter-and-spring coat matches
  «winter»);
- text: trimmed, compared case-insensitively with `Locale.ROOT` lower-casing, «ё» treated as «е» on both sides; the
  item matches when **every** word of the text (split on whitespace) is a substring of the name. No fuzzy matching,
  no transliteration («рубашка» does not find "shirt").

Test helper `QueryItems` (new file) with 8–10 invented items covering the fields (Russian and English names, e.g.
«Тёмно-синие брюки», "Striped T-shirt"). Tests: `all()` returns everything in order; each field alone; type + colour
together; a season set that only overlaps; «темно синие» finds «Тёмно-синие брюки»; "SHIRT" finds "Striped T-shirt";
blank text = no filter; the input list is not changed. Write the text rule (all words, substring, ё = е) in
`DECISIONS.md`.

### WR-098 — The contract's item enums are exactly the Java enums
- status: todo
- size: S
- paths: `app/src/test/java/wardrobe/app/ContractEnumsTest.java`
- verify: `./gradlew :app:test --tests 'wardrobe.app.ContractEnumsTest'`
- expect: green now; it fails (naming the property and the differing values) when `api/openapi.yaml` and a domain enum disagree

A test only. Today nothing checks that `api/openapi.yaml` (schema `Item`) lists the same values as the domain
enums; a new colour added on one side only would pass every test. `ContractEnumsTest` (new file in `app/`):
- read the contract as text (it is on the test classpath as `openapi.yaml`, see `app/build.gradle`; or
  `Path.of("../api/openapi.yaml")` — Gradle runs `:app` tests in `app/`). No YAML library: find the block of the
  `Item` schema (from the line `    Item:` to the next line with exactly four spaces of indentation, or the end of the
  file) and, inside it, for each property the first `enum: [ … ]` after the property's name (one line in the current
  file; split on commas, trim);
- compare, value by value and **in order**: `type` ↔ `ItemType`, `colour` ↔ `Colour`, `pattern` ↔ `Pattern`,
  `fabric` ↔ `Fabric`, `seasons` ↔ `Season`, `formality` ↔ `Formality`, `status` ↔ `ItemStatus`. The failure
  message names the property and both lists;
- properties not in this table are ignored (a later task may add enum fields; this test must not block it);
- one small test of the extraction itself on an inline YAML string with a second schema after `Item`, to show
  that enums outside `Item` are not read.

Do not change the contract or the enums in this task; if they disagree today, the test is right — park with the
difference.

### WR-100 — ArchUnit: controllers use ports, domain packages have no cycles
- status: todo
- size: S
- paths: `app/src/test/java/wardrobe/app/PackageRulesTest.java`
- verify: `./gradlew :app:test --tests 'wardrobe.app.PackageRulesTest'`
- expect: green on the current code; rules 1 and 4 are not vacuous

The queue adds packages (`domain.card`, `domain.avatar`, `domain.outfit`, `domain.collection`, controllers in
`app.web`). `ArchitectureTest` covers domain ← app only and must not be edited. New `PackageRulesTest` (ArchUnit
is already a test dependency; same `@AnalyzeClasses(packages = "wardrobe", importOptions =
DoNotIncludeTests)` style):
1. classes in `wardrobe.app.web..` do not depend on `wardrobe.app.catalog..` — a controller talks to a domain port
   (`ItemCatalog`), never to the in-memory adapter;
2. `slices().matching("wardrobe.domain.(*)..")` are free of cycles;
3. `wardrobe.domain.card..` does not depend on `wardrobe.domain.match..`, `..outfit..` or `..avatar..` (a card is
   parsed without knowing the rules);
4. classes annotated `@RestController` reside in `wardrobe.app.web..` and their simple names end with `Controller`.
Use `.allowEmptyShould(true)` only for rules 2 and 3 (their packages may not exist yet); rules 1 and 4 check
classes that exist today (`ItemsController`), so they must not pass vacuously. If a rule fails on code already merged
(for example a `match` ↔ `outfit` cycle), do not weaken it: park with the finding.

### WR-101 — Invented items for client tests
- status: todo
- size: S
- paths: `client/src/fixtures/items.ts`, `client/test/fixtures.test.mjs`
- verify: `make client/node_modules/.ci-stamp && cd client && node --test test/fixtures.test.mjs && npx tsc --noEmit`
- expect: green; every fixture passes `parseItems`, every value is in the contract, every value of today's enums appears at least once

Client logic tests and later view-models need a shared set of believable items. `client/src/fixtures/items.ts`
exports `FIXTURE_ITEMS` and `fixtureById(id)`. Build the fixtures as `export const FIXTURE_ITEMS: readonly Item[] =
parseItems([...raw objects...])` (`Item` and `parseItems` from `client/src/items.ts`), so a field added to `Item`
later does not break them:
24–30 invented items with Russian names in the spirit of the mockups («Белая рубашка», «Тёмно-синие брюки»,
«Шалфейный джемпер», «Горчичная сумка»), ids in the style `white-shirt`, no brands, no people. Cover every status
(at least 3 not clean), every type (at least two dresses and two accessories), every colour including `UNKNOWN`,
every pattern and fabric including `UNKNOWN`, every formality, items with one season and with all four. Seasons in
calendar order (WINTER, SPRING, SUMMER, AUTUMN), as the server sends them.

`fixtures.test.mjs` (new file): `parseItems(FIXTURE_ITEMS)` succeeds and returns the same length; ids unique; every
value of type, colour, pattern, fabric, seasons, formality and status is one of the `enum` lists of the `Item`
schema read as text from `../api/openapi.yaml` (from the line `    Item:` to the next line with four spaces of
indentation); coverage against a **fixed list in the test** of today's values (so a value added to the contract
later does not turn this red).
Node runs `.ts` by stripping types: use only erasable syntax (no `enum`, no namespaces, no parameter properties).

### WR-102 — Client: one HTTP helper with typed errors, and listItems
- status: todo
- size: S
- paths: `client/src/api/http.ts`, `client/src/api/items.ts`, `client/test/api-http.test.mjs`
- verify: `make client/node_modules/.ci-stamp && cd client && node --test test/api-http.test.mjs && npx tsc --noEmit`
- expect: green; success, the server's `Error`, a non-JSON error body, a network failure and 204 are each tested with a fake fetch

The client will call many endpoints; each must read errors the same way. `client/src/api/http.ts` (no React):
- `class ApiError extends Error` with readonly fields `status: number` (0 for a network failure), `code: string`,
  `fields: string[]` (empty unless the server sent `fields`), set in the constructor body (no parameter
  properties: Node strips types and accepts only erasable syntax);
- `request(baseUrl, method, path, body?, fetchFn = fetch): Promise<unknown>`: JSON body with
  `content-type: application/json` when `body` is given; 2xx → the parsed JSON, or `undefined` for 204 or an empty
  body; non-2xx → `ApiError` with the server's `code` and `message` when the body is an object with string `code`
  and `message` (the `Error` schema of WR-008), else code `HTTP_<status>` and a message naming the status;
  `fetchFn` rejecting → `ApiError` status 0, code `NETWORK`. No retries, no timers.
- `client/src/api/items.ts`: `listItems(baseUrl, fetchFn = fetch)` = `request(..., 'GET', '/items')` then
  `parseItems` from `client/src/items.ts` (reuse it; do not write a second parser).

Do not touch `App.tsx` (screens belong to other tasks) or `client/src/api.ts` (WR-025 may create it). Tests with a
fake `fetch` (records method, URL, headers and body; returns a `Response` built with `new Response(...)`): the path
is appended to the base URL without a double slash; success; 404 with an `Error` body → `ApiError` code
`UNKNOWN_ITEM`; 500 with an HTML body → code `HTTP_500`; network failure → `NETWORK`; 204 → `undefined`; listItems
rejects a broken item through `parseItems`.

### WR-103 — Client: Russian plurals for counts («1 вещь», «3 вещи», «17 вещей»)
- status: todo
- size: S
- paths: `client/src/plural.ts`, `client/test/plural.test.mjs`
- verify: `make client/node_modules/.ci-stamp && cd client && node --test test/plural.test.mjs && npx tsc --noEmit`
- expect: green; 0, 1, 2, 4, 5, 11–14, 21, 22, 25, 101, 111 and 1 000 000 give the right form

The mockups show counts everywhere («64 вещи · без ограничений», «17 вещей», «8 вещей», «образов записано»).
`client/src/plural.ts`: `plural(n, one, few, many)` with the Russian rule (n % 10 == 1 and n % 100 != 11 → one;
n % 10 in 2..4 and n % 100 not in 12..14 → few; else many); `countText(n, forms)` → «17 вещей» (number, space,
word); a ready `ITEM_FORMS = ['вещь', 'вещи', 'вещей']` and `itemsCount(n)`; `OUTFIT_FORMS = ['образ', 'образа',
'образов']`. A negative or non-integer `n` throws (a count is never fractional). No `Intl.PluralRules` (its
availability in the phone runtime is not guaranteed; write that in `DECISIONS.md`). Tests: the cases in `expect`
for both word sets, and the throw.

### WR-104 — Client: which builder tab an item belongs to
- status: todo
- size: S
- paths: `client/src/tabs.ts`, `client/test/tabs.test.mjs`
- verify: `make client/node_modules/.ci-stamp && cd client && node --test test/tabs.test.mjs && npx tsc --noEmit`
- expect: green; every `ItemType` of the contract has exactly one tab; counts and order tested

The outfit builder (mockup «Конструктор образа») has `CategoryTabs`: «Все · Верх · Низ · Верхняя · Обувь ·
Аксессуары». This is display grouping, not a wardrobe rule. `client/src/tabs.ts`:
- `TABS` (readonly, in mockup order): `{ key: 'ALL' | 'TOP' | 'BOTTOM' | 'OUTER' | 'SHOES' | 'ACCESSORIES', title }`
  with the Russian titles above;
- `tabOf(type: string)` → the tab key: TOP → TOP, BOTTOM → BOTTOM, OUTERWEAR → OUTER, SHOES → SHOES, ACCESSORY →
  ACCESSORIES, **DRESS → TOP** (the mockup has no dress tab; adding one changes the look, which is Aika's — write
  this in `DECISIONS.md` as reversible); an unknown type → `null` (shown only under «Все»);
- `itemsInTab(items, key)` keeps the input order (ALL = everything); `tabCounts(items)` → a count for every tab,
  zero included.
Tests (new file): read the `type` enum of the `Item` schema from `../api/openapi.yaml` as text (from `    Item:` to the
next line with four spaces of indentation) and check every value has a tab; DRESS lands in TOP; an unknown type
only in ALL; counts with an empty list are all zero; input order kept. Use `FIXTURE_ITEMS` from
`client/src/fixtures/items.ts` if it exists, else inline items. Erasable TypeScript only (no `enum`).

### WR-105 — Client: centimetres for people, millimetres inside
- status: todo
- size: S
- paths: `client/src/units.ts`, `client/test/units.test.mjs`
- verify: `make client/node_modules/.ci-stamp && cd client && node --test test/units.test.mjs && npx tsc --noEmit`
- expect: green; round trips for 0–3000 mm, every refused input tested; no floating-point arithmetic in units.ts

The server speaks integer millimetres (intent 01, invariant 6); `MeasureRow` and fit notes show centimetres
(«рукав короче на 2 см», DESIGN.md). `client/src/units.ts`, string and integer arithmetic only (no `parseFloat`,
no division producing fractions, no `toFixed`):
- `mmToCmText(mm)` → «165» for 1650, «165,5» for 1655, «0,5» for 5, «−2» for −20 (a minus sign for differences);
  the decimal comma, no trailing «,0»; a non-integer `mm` throws;
- `cmTextToMm(text)` → integer mm or `null`: accepts «165», «165,5», «165.5», surrounding spaces and an optional
  «см» / «cm» suffix; refuses (null) an empty string, letters, two separators, more than one decimal digit
  («165,55» — we do not round what a person typed), a negative value, and anything above 3000 mm.
- `diffText(mm)` → «на 2 см» / «на 0,5 см» (absolute value), for fit notes.
Tests: the examples above; for every mm 0..3000, `cmTextToMm(mmToCmText(mm)) === mm`; a test that `units.ts`
source contains no `parseFloat`, `toFixed` or `Math.round` (read the file as text).

### WR-106 — Saved model answers for more kinds of garment, with the garment words they use
- status: todo
- depends: WR-011, WR-013
- size: S
- paths: `domain/src/test/resources/card-kinds/**`, `docs/cards.md`
- verify: `cd domain/src/test/resources/card-kinds && n=0; for f in *.txt; do test -f "${f%.txt}.expected" || { echo "missing ${f%.txt}.expected"; exit 1; }; grep -q "$f" ../../../../../docs/cards.md || { echo "$f not in docs/cards.md"; exit 1; }; n=$((n+1)); done; test "$n" -ge 8 || exit 1; echo "$n fixtures"`
- expect: prints `N fixtures` with N ≥ 8; every `.txt` has its `.expected` and is listed in `docs/cards.md`

WR-011's fixtures use the enum words for `type`. A real model says "skirt", "hoodie", «ботинки». This task writes
fixtures for that and the table they are judged by; WR-114 implements the table in the parser.

Read `docs/cards.md` and the fixtures of WR-011 in `domain/src/test/resources/cards/` first and keep exactly their
format (`NN-short-name.txt` + `NN-short-name.expected`, the same keys and order, `?` and `UNKNOWN` as there). Put
the new files in the **new folder** `domain/src/test/resources/card-kinds/` — never in `cards/`: the tests of
WR-012..014 read that folder and must not see answers their parser was not built for.

In `docs/cards.md` add a section «Garment words»: one table word → `ItemType`, English and Russian, singular and
plural, e.g. shirt, blouse, t-shirt, sweater, jumper, hoodie, cardigan, рубашка, блузка, футболка, свитер, джемпер,
худи, кардиган → TOP; trousers, jeans, skirt, shorts, брюки, джинсы, юбка, шорты → BOTTOM; dress, платье → DRESS;
coat, jacket, trench, raincoat, пальто, куртка, тренч, плащ → OUTERWEAR; shoes, boots, sneakers, sandals, туфли,
ботинки, кроссовки, сапоги → SHOES; bag, scarf, belt, hat, сумка, шарф, ремень, шапка → ACCESSORY. And a list of
words that stay unknown because they are ambiguous: jumpsuit, overalls, set, комбинезон, костюм (two items or one?).
Then at least 8 fixtures: a skirt, a hoodie in Russian («худи»), ankle boots ("ankle boots" — two words; say in
the table whether the last word decides), a scarf, a cardigan, a jumpsuit (type `?`, the doubt is the point), a
trench coat with `type: Trench`, plural «джинсы». In every key except `type` use only English enum words or words already in WR-013's table; the
garment words are the only new vocabulary. Invented items only, no brands. Each `.expected` is what a correct
parser with this table gives. Check every file twice against the table before pushing: they are append-only.

### WR-107 — Collections («подборки») in the domain
- status: todo
- depends: WR-007, WR-096
- size: S
- paths: `domain/src/main/java/wardrobe/domain/collection/**`, `domain/src/test/java/wardrobe/domain/collection/**`
- verify: `./gradlew :domain:test --tests 'wardrobe.domain.collection.*'`
- expect: green; every rule below has a test; plain Java

The wardrobe mockup has «Подборки»: named sets of items («Офис — 17 вещей»). Package
`wardrobe.domain.collection` (avoid the names `Collection`/`Collections`, they clash with `java.util`):
- `CollectionId` (record, not blank); `ItemCollection` (record: id, name, `List<ItemId> items`): name trimmed, 1–40
  characters; items in the order they were added, no duplicates; immutable copies.
- Port `CollectionStore`: `List<ItemCollection> all()` (stable order), `Optional<ItemCollection> find(CollectionId)`,
  `void save(ItemCollection)` (insert or replace by id), `void delete(CollectionId)`.
- Use case `CollectionBook(CollectionStore, ItemCatalog)`: `create(name)` → id from `Slugs.next(name, taken ids)`
  (WR-096); `rename(id, name)`; `addItem(id, itemId)` — the item must exist (`ItemCatalog.find` of WR-007, else
  `UnknownItem`), adding an item already there changes nothing; `removeItem(id, itemId)` — not there → nothing
  changes; `delete(id)`; `view(id)` → `CollectionView(collection, List<Item> items)` with the items resolved from
  the catalogue in collection order, **skipping ids the catalogue no longer has** (archived or gone), so a count is
  always of real items. Unknown collection → `UnknownCollection` (new exception). Two collections may not share a
  name ignoring case → `DuplicateCollectionName`.
- An item may be in many collections; items of any status may be in one (a collection is not an outfit); no limit
  on collections or items (intent 01, invariant 9).
Tests with an in-memory fake store (new test file) and a fake catalogue (reuse WR-007's `FakeCatalog` if it fits;
never edit it). Write the choices (case-insensitive unique names, idempotent add, skipped missing items) in
`DECISIONS.md`.

### WR-108 — Archive an item instead of deleting it
- status: todo
- depends: WR-007
- size: S
- paths: `domain/src/main/java/wardrobe/domain/ItemCatalog.java`, `app/src/main/java/wardrobe/app/catalog/InMemoryItemCatalog.java`, `app/src/test/java/wardrobe/app/catalog/ArchiveCatalogTest.java`
- verify: `./gradlew :app:test --tests 'wardrobe.app.catalog.ArchiveCatalogTest'`
- expect: green; an archived item leaves `all()` and `find()`, comes back on restore with its status, and nothing is ever deleted

Items are never deleted in version 1 (a mistaken delete loses a card for good); they are archived and can come
back. Add to the port `ItemCatalog` (WR-007 extended it with `find`/`replace`):
`Item archive(ItemId)`, `Item restore(ItemId)`, `List<Item> archived()` — as **`default` methods that throw
`UnsupportedOperationException("read-only catalogue")`**: test fakes of earlier tasks (WR-007's `FakeCatalog`,
WR-032's catalogue of 2 000 items) implement this interface and can never be edited, so an abstract method would
break their compilation. `InMemoryItemCatalog` overrides them:
- `all()` and `find()` see only active items (so outfits, matches and status changes never see an archived one);
  `archived()` lists archived items in the order they were archived;
- `archive(id)`: not an active item → `UnknownItem`; keeps the item's status; `restore(id)`: not archived →
  `UnknownItem`; the item goes back to the end of `all()` with the status it had;
- under the same lock as WR-007's changes; `replace` of an archived id → `UnknownItem`.
Archiving is allowed in any status (an item on a trip can be archived; write it in `DECISIONS.md`). Test (new file):
archive, restore, the errors, order, status kept, and that the seed list without archiving is unchanged.

### WR-109 — REST: one item by id
- status: todo
- depends: WR-007, WR-008
- size: S
- paths: `api/openapi.yaml`, `app/src/main/java/wardrobe/app/web/ItemController.java`, `app/src/test/java/wardrobe/app/ItemApiTest.java`
- verify: `./gradlew :app:test --tests 'wardrobe.app.ItemApiTest'`
- expect: green; 200 with an `Item` (including `next`) and 404 with `Error`, both valid against the contract

Contract first: `GET /items/{id}` (operationId `getItem`) → 200 `Item`; unknown id → 404 `Error` with code
`UNKNOWN_ITEM`. New controller `ItemController` (not `ItemsController`, which WR-009 and WR-113 change): it calls
`ItemCatalog.find` (WR-007) and maps with `ItemView.of` (WR-008 added `next`); throw `UnknownItem` and let WR-008's
`@RestControllerAdvice` answer — do not write a second mapping. `ItemApiTest` (real HTTP like `ItemsApiTest`):
validates both bodies against the schemas; the item equals the same item in `GET /items`; an id with URL-encoded
characters (`%20`) gives 404, not 500.

### WR-110 — The API says whether an item can be worn today
- status: todo
- depends: WR-008
- size: S
- paths: `api/openapi.yaml`, `app/src/main/java/wardrobe/app/web/ItemView.java`, `app/src/test/java/wardrobe/app/WearableApiTest.java`
- verify: `./gradlew :app:test --tests 'wardrobe.app.WearableApiTest'`
- expect: green; every item of `GET /items` carries `wearable`, true exactly for the clean ones

Invariant 1 ("only a clean item is offered for an outfit") is `Item.availableForOutfit()`. Screens fade items that
cannot be worn (builder `ItemStrip`); without this field the client would write `status === 'CLEAN'` itself — a
second copy of the rule. Add a required boolean `wearable` to the `Item` schema (description: "from the server's
rule; the client never derives it from status") and to `ItemView` (`item.availableForOutfit()`). Read WR-008's
`ItemView` first and keep `next`. `WearableApiTest` (new): every item valid against the schema; `wearable` equals
`availableForOutfit()` of the catalogue's item (compare with the `ItemCatalog` bean, not with a status string).
`ItemsApiTest` must stay green untouched.

### WR-111 — Bad requests answer with the Error schema; the web build may POST and DELETE
- status: todo
- depends: WR-008
- size: S
- paths: `api/openapi.yaml`, `app/src/main/java/wardrobe/app/web/*.java`, `app/src/main/java/wardrobe/app/web/CorsConfig.java`, `app/src/test/java/wardrobe/app/BadInputApiTest.java`, `app/src/test/java/wardrobe/app/WriteCorsTest.java`
- verify: `./gradlew :app:test --tests 'wardrobe.app.BadInputApiTest' && ./gradlew :app:test --tests 'wardrobe.app.WriteCorsTest'`
- expect: green; malformed JSON, an unknown path and a wrong method each give the `Error` schema; POST and DELETE preflights are allowed

Read WR-008's `@RestControllerAdvice` and extend **that** class (one place maps errors; no second advice). Contract:
add `NOT_FOUND` and `METHOD_NOT_ALLOWED` to `Error.code`, and an optional `fields: [string]` (keys a request got
wrong; later tasks fill it); also add `List<String> fields` to WR-008's error record (left out of the JSON when
empty). Map: unreadable body (`HttpMessageNotReadableException`) → 400 `BAD_REQUEST` (if WR-008
already maps it, keep that); an unknown path (`NoResourceFoundException`) → 404 `NOT_FOUND`; a wrong method
(`HttpRequestMethodNotSupportedException`) → 405 `METHOD_NOT_ALLOWED`; unsupported content type → 415 with
`BAD_REQUEST`. Messages in plain English, never a stack trace or a class name. `BadInputApiTest`: `PUT
/items/white-shirt/status` with body `{` → 400; `GET /nowhere` → 404; `DELETE /items` → 405 — each body valid against
`Error`. CORS: the later tasks (WR-115, WR-118, WR-119, WR-120, WR-121) POST and DELETE from the Expo web build.
Allow POST and DELETE with header `content-type` from the same local origins (`CorsConfig`; if WR-021 or WR-030
already allowed POST, keep one mapping). `WriteCorsTest`: preflight `OPTIONS` with `Origin: http://localhost:8081`
for POST (with `Access-Control-Request-Headers: content-type`) and DELETE → allowed; from `http://example.com` →
refused.

### WR-112 — Client: Russian text for every error the server can send
- status: todo
- depends: WR-008, WR-102
- size: S
- paths: `client/src/errors.ts`, `client/test/errors.test.mjs`
- verify: `make client/node_modules/.ci-stamp && cd client && node --test test/errors.test.mjs && npx tsc --noEmit`
- expect: green; no stale code in the table; an unknown code falls back to the server's message

`client/src/errors.ts`: `errorText(e: ApiError)` (from `client/src/api/http.ts`, WR-102) → `{ title, detail }` in
Russian: a title per code from one table (`UNKNOWN_ITEM` «Такой вещи нет», `ILLEGAL_STATUS_CHANGE` «Так статус
поменять нельзя», `BAD_REQUEST` «Сервер не понял запрос», `NETWORK` «Нет связи с сервером», and any code the
contract has when you work); `detail` is the server's own `message` as is (the client never re-words the reason
of a rule). An unknown code → title «Что-то пошло не так», detail the server's message — so a code added later never
breaks the screen. Test (new file): every key of the table except client-only ones (`NETWORK`, `HTTP_*`) is in the
`code` enum of the `Error` schema read as text from `../api/openapi.yaml`; the fallback; `fields` (when present)
listed in `detail` after the message. Do not assert completeness against the contract (other tasks add codes
without touching the client).

### WR-113 — REST: filter the list by type, colour, season and words
- status: todo
- depends: WR-097, WR-009
- size: S
- paths: `api/openapi.yaml`, `app/src/main/java/wardrobe/app/web/ItemsController.java`, `app/src/test/java/wardrobe/app/SearchApiTest.java`
- verify: `./gradlew :app:test --tests 'wardrobe.app.SearchApiTest'`
- expect: green; each parameter alone and together, an unknown value → 400 `Error` naming the parameter

Contract first: `GET /items` gets optional repeatable query parameters `type`, `colour`, `season` (enum values;
`?type=TOP&type=DRESS`) and `q` (text, at most 100 characters), next to WR-009's `status`. The controller builds an
`ItemQuery` (WR-097) and calls `filter` — it decides nothing itself. If WR-009 filtered by status in the controller,
move that into the same `ItemQuery` (`statuses`) so all filters live in one place; `FilterApiTest` must stay green
untouched. An unknown enum value or a `q` over 100 characters → 400 `BAD_REQUEST` naming the parameter (through
WR-008's advice when it already maps it, otherwise an `@ExceptionHandler` inside `ItemsController` building WR-008's
error record; put the parameter's name in `fields` if WR-111 has added it). `SearchApiTest` (real HTTP, bodies
validated against `Item`): `type=BOTTOM` gives only bottoms; `season=WINTER&type=OUTERWEAR`; `q=navy` finds "Navy
trousers"; `q` with Cyrillic URL-encoded works; `colour=TEAL` → 400.

### WR-114 — The parser knows garment words
- status: todo
- depends: WR-014, WR-106
- size: S
- paths: `domain/src/main/java/wardrobe/domain/card/**`, `domain/src/test/java/wardrobe/domain/card/CardParserKindsTest.java`
- verify: `./gradlew :domain:test --tests 'wardrobe.domain.card.CardParserKindsTest' && ./gradlew :domain:test --tests 'wardrobe.domain.card.*'`
- expect: green on every fixture in `card-kinds/` (draft and doubts) and the earlier card tests unchanged

Implement the «Garment words» table of `docs/cards.md` (WR-106) in the parser: extend the **one** word table WR-013
created in `wardrobe.domain.card` (do not start a second table) with the garment words → `ItemType`, English and
Russian, case-insensitive. The ambiguous words of the doc stay unknown and give a doubt `UNREADABLE` with the raw
value; a word in no table stays unknown as before (never the closest match). For multi-word values follow what
`docs/cards.md` says (e.g. the last word decides «ankle boots»). `CardParserKindsTest` (new) reads every
`card-kinds/*.txt` with `getResourceAsStream` (list the names in the test; the domain has no I/O) and compares with
its `.expected`, plus the doubts for the jumpsuit fixture. If an earlier fixture in `cards/` expects `?` for a word
the table now maps, the earlier tests break: then do not change them — park with the word and the fixture.

### WR-115 — REST: read a saved model answer into a card draft
- status: todo
- depends: WR-014, WR-008, WR-111
- size: S
- paths: `api/openapi.yaml`, `app/src/main/java/wardrobe/app/web/CardsController.java`, `app/src/main/java/wardrobe/app/web/CardDraftView.java`, `app/src/test/java/wardrobe/app/CardsApiTest.java`
- verify: `./gradlew :app:test --tests 'wardrobe.app.CardsApiTest'`
- expect: green; a clean answer, a garbled one and an empty one each come back valid against the contract with the parser's doubts

Version 1 has no model calls; a person (or a test) pastes the text a model would answer. Contract first:
`POST /cards/parse` (operationId `parseCard`) with `{ "answer": string }` (at most 10 000 characters) → 200
`{ draft: CardDraft, doubts: [Doubt] }`. `CardDraft`: `name`, `type`, `formality` — string or `null` when unknown;
`colour`, `pattern`, `fabric` — the enum including `UNKNOWN`; `seasons` — array in enum order, may be empty.
`Doubt`: `key`, `raw` (string or null, as the domain gives), `why` (`MISSING`, `UNREADABLE`, `CONFLICTING`). Nothing
is stored: a draft becomes an item only through `POST /items` (WR-121). Over-long or missing `answer` → 400 `Error`.
`CardsController` calls `CardParser.read` (WR-014) and maps with `CardDraftView`; it decides nothing. `CardsApiTest`
(real HTTP, inline invented answers — the domain's test fixtures are not on `app`'s classpath): a clean answer gives
no doubts; `colour: blueish ?` → `UNKNOWN` with an `UNREADABLE` doubt; an empty answer → every field unknown and
one doubt per key; every body validated against the schemas.

### WR-116 — Add an item: a confirmed card gets an id and joins the catalogue
- status: todo
- depends: WR-007, WR-015, WR-096, WR-108
- size: S
- paths: `domain/src/main/java/wardrobe/domain/ItemCatalog.java`, `domain/src/main/java/wardrobe/domain/DuplicateItem.java`, `domain/src/main/java/wardrobe/domain/card/AddItem.java`, `domain/src/test/java/wardrobe/domain/card/AddItemTest.java`, `domain/src/test/java/wardrobe/domain/card/AddingCatalog.java`, `app/src/main/java/wardrobe/app/catalog/InMemoryItemCatalog.java`, `app/src/test/java/wardrobe/app/catalog/AddCatalogTest.java`
- verify: `./gradlew :domain:test --tests 'wardrobe.domain.card.AddItemTest' && ./gradlew :app:test --tests 'wardrobe.app.catalog.AddCatalogTest'`
- expect: green; an incomplete card is refused with its keys, a complete one is added CLEAN at the end with a fresh id

One rule in one place: what makes a card complete is `ConfirmCard` (WR-015). Adding an item goes through it.
- Port: `void add(Item)` on `ItemCatalog` as a **`default` method throwing `UnsupportedOperationException`** (test
  fakes of earlier tasks implement the interface and are never edited). An id already present (active or archived,
  WR-108) → `DuplicateItem` (new, in `wardrobe.domain`).
- `InMemoryItemCatalog.add`: appends at the end of `all()`, under the lock of WR-007. No cap (invariant 9).
- Use case `AddItem(ItemCatalog)` in `wardrobe.domain.card`: `Item add(CardDraft draft, Corrections corrections)`:
  name = the corrected or drafted name (blank or unknown → let `ConfirmCard` refuse it); id =
  `Slugs.next(name or "item", taken)`, taken = ids of `all()` plus ids of `archived()`; `ConfirmCard.confirm(id, draft, corrections)` (throws `IncompleteCard`
  untouched); `catalog.add`. On `DuplicateItem` (another request took the id between the two steps) add the refused
  id to taken and recompute (at most 3 times), then rethrow.
Tests: `AddItemTest` with a new fake `AddingCatalog` (incomplete card → `IncompleteCard` and nothing added; two
items named «Белая рубашка» → `belaya-rubashka` and `belaya-rubashka-2`; a fake that reports a duplicate once →
retried; archive `belaya-rubashka`, then add «Белая рубашка» → `belaya-rubashka-2`); `AddCatalogTest`: added at the end, duplicate id refused (active or archived), 8 threads × 50 adds of
items with distinct ids end with all 400 present, none lost. WR-108 changed the same two files first: keep its archive methods intact.

### WR-117 — Edit an item's card fields
- status: todo
- depends: WR-007, WR-015
- size: S
- paths: `domain/src/main/java/wardrobe/domain/card/EditCard.java`, `domain/src/main/java/wardrobe/domain/card/CardDraft.java`, `domain/src/test/java/wardrobe/domain/card/EditCardTest.java`
- verify: `./gradlew :domain:test --tests 'wardrobe.domain.card.EditCardTest'`
- expect: green; an edit keeps id and status, refuses what `ConfirmCard` refuses, and leaves the catalogue unchanged on refusal

A wrong colour on a card must be fixable without archiving and re-adding. `EditCard(ItemCatalog)`:
`Item edit(ItemId id, Corrections corrections)`:
- find the item (`UnknownItem` when absent — WR-007);
- build a draft from the item (`CardDraft.of(Item)`, a small static factory added to `CardDraft` if WR-012 has
  none), and run `ConfirmCard.confirm(id, draft, corrections)` — the same completeness rule as for a new card, never
  a second check (UNKNOWN colour set by an edit is refused the same way);
- the result keeps the **old status** (an edit is not a status change: build the item with the confirmed card and the
  old status — do not go through `withStatus`, the transition table does not apply) and the old id even when the name
  changes (ids are stable; write in `DECISIONS.md`);
- `catalog.replace` (WR-007).
Tests (new file, a fake catalogue — WR-007's `FakeCatalog` may be reused, never edited): change colour and name;
status IN_WASH stays IN_WASH; setting `UNKNOWN` colour → `IncompleteCard` and the stored item is unchanged; unknown id
→ `UnknownItem`; empty corrections → the same card.

### WR-118 — REST: archive and restore items
- status: todo
- depends: WR-108, WR-008, WR-111
- size: S
- paths: `api/openapi.yaml`, `app/src/main/java/wardrobe/app/web/ArchiveController.java`, `app/src/test/java/wardrobe/app/ArchiveApiTest.java`
- verify: `./gradlew :app:test --tests 'wardrobe.app.ArchiveApiTest'`
- expect: green; archive → gone from `GET /items`, listed in `GET /archive`; restore → back; unknown → 404; all valid against the contract

Contract first: `POST /items/{id}/archive` → 200 `Item`; `GET /archive` → array of `Item` (archived, in archive
order); `POST /archive/{id}/restore` → 200 `Item`. (`/archive` and not `/items/archived`, so no item id can clash with
a path word.) Unknown id → 404 `UNKNOWN_ITEM` through WR-008's advice. There is no DELETE of an item in version 1
(write in the contract description: items are archived, never deleted). New `ArchiveController` calls the port
methods of WR-108. `ArchiveApiTest` (real HTTP; tests that change the shared catalogue use `@DirtiesContext`, not "restore in
the same test"): bodies validated; the item keeps its status through archive and
restore; archiving twice → 404 the second time.

### WR-119 — REST: list, open and create collections
- status: todo
- depends: WR-107, WR-008, WR-111
- size: S
- paths: `api/openapi.yaml`, `app/src/main/java/wardrobe/app/web/CollectionsController.java`, `app/src/main/java/wardrobe/app/web/CollectionView.java`, `app/src/main/java/wardrobe/app/catalog/InMemoryCollectionStore.java`, `app/src/main/java/wardrobe/app/CollectionConfig.java`, `app/src/test/java/wardrobe/app/CollectionsApiTest.java`
- verify: `./gradlew :app:test --tests 'wardrobe.app.CollectionsApiTest'`
- expect: green; list, one collection, create, duplicate name and unknown id each checked against the contract

Contract first: `GET /collections` → `[{ id, name, count, preview: [Item] }]` (`preview` = the first 4 resolved
items, for the 2×2 collage of `CollectionCard`); `GET /collections/{id}` → `{ id, name, items: [Item] }`;
`POST /collections` `{ name }` → 201 with the collection and a `Location` header. Errors (`Error`, add the codes to
its enum): blank or over-long name → 400 `BAD_REQUEST`; a name already used (ignoring case) → 409
`DUPLICATE_NAME`; unknown id → 404 `UNKNOWN_COLLECTION`. Map the new domain exceptions with `@ExceptionHandler`
methods **inside `CollectionsController`** (several queued tasks add exceptions; they must not all edit the shared
advice); build the body with
the error record WR-008 created. `InMemoryCollectionStore` (thread-safe like WR-007's catalogue) seeded with three
invented collections over the seed items: «Офис» (white-shirt, navy-trousers, beige-coat), «Выходные» (blue-jeans,
striped-tee, white-sneakers), «Лето» (striped-tee, green-dress). `CollectionConfig` wires `CollectionBook`.
Controllers call `CollectionBook` only (WR-107). `CollectionsApiTest` (real HTTP, bodies validated): the seeds;
`count` equals the length of `items`; create then open; duplicate «офис» → 409; unknown → 404.

### WR-120 — REST: rename a collection, add and remove items, delete it
- status: todo
- depends: WR-119
- size: S
- paths: `api/openapi.yaml`, `app/src/main/java/wardrobe/app/web/CollectionsController.java`, `app/src/test/java/wardrobe/app/CollectionChangesApiTest.java`
- verify: `./gradlew :app:test --tests 'wardrobe.app.CollectionChangesApiTest'`
- expect: green; each change and each error checked against the contract; deleting a collection never touches items

Contract first, in the same `CollectionsController`: `PUT /collections/{id}` `{ name }` → 200 (rename; same errors
as create); `PUT /collections/{id}/items/{itemId}` → 200 the collection (add; already there → 200, unchanged);
`DELETE /collections/{id}/items/{itemId}` → 200 (remove; not there → 200, unchanged); `DELETE /collections/{id}` →
204. Unknown collection → 404 `UNKNOWN_COLLECTION`; unknown item on add → 404 `UNKNOWN_ITEM`. CORS for DELETE comes
from WR-111. Every rule is `CollectionBook`'s (WR-107); the controller only maps. `CollectionChangesApiTest` (real
HTTP; create its own collection first so the seeds stay as WR-119's test expects): rename, add twice (one entry),
remove, delete then 404; after deleting a collection `GET /items` still has every item.

### WR-121 — REST: add an item from a card
- status: todo
- depends: WR-116, WR-008, WR-111
- size: S
- paths: `api/openapi.yaml`, `app/src/main/java/wardrobe/app/web/AddItemController.java`, `app/src/main/java/wardrobe/app/AddItemConfig.java`, `app/src/test/java/wardrobe/app/AddItemApiTest.java`
- verify: `./gradlew :app:test --tests 'wardrobe.app.AddItemApiTest'`
- expect: green; 201 with `Location` and an `Item`; an incomplete card → 400 `INCOMPLETE_CARD` with the missing keys in `fields`

Contract first: `POST /items` (operationId `addItem`) with `NewItem` — the card fields `name, type, colour, pattern,
fabric, seasons, formality`, **every one optional** in the schema (enum values as in `Item`, `UNKNOWN` allowed), so
the domain, not the schema, decides what is missing (one rule in one place: `ConfirmCard`). The client sends what the
person confirmed or typed (usually a draft from `POST /cards/parse` with corrections). → 201, `Location:
/items/{id}`, body `Item` in status CLEAN. Incomplete → 400 with code `INCOMPLETE_CARD` (add to the enum) and
`fields` = the keys `IncompleteCard` lists (add `fields` to `Error` if WR-111 has not). The controller turns the
body into `Corrections` over an empty draft and calls `AddItem` (WR-116); handle `IncompleteCard` with an
`@ExceptionHandler` inside `AddItemController`. `AddItemConfig` wires `AddItem`. `AddItemApiTest` (real HTTP, bodies
validated; tests that change the shared catalogue use `@DirtiesContext`): a complete card → 201 and the item is last in `GET /items`; the same name twice → two ids; missing
`fabric` and `colour: UNKNOWN` → 400 with both keys; an unknown enum word → 400 `BAD_REQUEST`.

### WR-122 — REST: edit an item's card
- status: todo
- depends: WR-117, WR-008, WR-111, WR-121
- size: S
- paths: `api/openapi.yaml`, `app/src/main/java/wardrobe/app/web/ItemCardController.java`, `app/src/test/java/wardrobe/app/ItemCardApiTest.java`
- verify: `./gradlew :app:test --tests 'wardrobe.app.ItemCardApiTest'`
- expect: green; 200 with the edited `Item` (same id and status), 404 for an unknown id, 400 `INCOMPLETE_CARD` for an unknown value

Contract first: `PUT /items/{id}/card` (operationId `editCard`) with `CardFields` — all seven card fields
**required** (a full replacement of the card; status is not part of it: statuses change only through `PUT
/items/{id}/status`). → 200 `Item`. Unknown id → 404 `UNKNOWN_ITEM` (WR-008's advice). A field set to `UNKNOWN` → 400
`INCOMPLETE_CARD` with `fields`; map
`IncompleteCard` with an `@ExceptionHandler` inside `ItemCardController`. The controller calls `EditCard` (WR-117)
only. PUT is already allowed by CORS (WR-008). `ItemCardApiTest` (real HTTP, bodies validated; edit an item no other
test asserts on and restore it at the end, or use `@DirtiesContext`): change the colour of `beige-coat` → status
IN_REPAIR kept, id kept; `colour: UNKNOWN` → 400 and the item unchanged in `GET /items`; a missing field → 400
`BAD_REQUEST` (the controller refuses a null field before calling the domain).

### WR-123 — Client: a parsed card draft in plain Russian
- status: todo
- depends: WR-022, WR-102, WR-115
- size: S
- paths: `client/src/api/cards.ts`, `client/src/cardDraft.ts`, `client/test/card-draft.test.mjs`
- verify: `make client/node_modules/.ci-stamp && cd client && node --test test/card-draft.test.mjs && npx tsc --noEmit`
- expect: green; every field, every doubt kind and the empty answer are worded; no wardrobe rule in the client

The «add an item» flow will show what was read from a model answer and what the person must fill in.
`client/src/api/cards.ts`: `parseCard(baseUrl, answer, fetchFn = fetch)` → `{ draft, doubts }` via `request` of
WR-102, with a shape check like `parseItems` (throws on a body the contract does not allow; `null` allowed where the
contract allows it). `client/src/cardDraft.ts`: `draftRows(result)` → one row per card key in the order of
`docs/cards.md` (name, type, colour, pattern, fabric, seasons, formality): `{ key, title }` with a Russian title per
key («Название», «Вид», «Цвет», «Узор», «Ткань», «Сезоны», «Строгость»), `value` — the Russian label from
`client/src/labels.ts` (WR-022) or `null` when unknown (`null`, `UNKNOWN`, empty seasons), and `doubt` — the Russian
sentence for the key's doubt: MISSING «нет в ответе — заполните», UNREADABLE «не разобрать: „{raw}“», CONFLICTING
«несколько значений: „{raw}“». `unknownKeys(result)` lists the keys whose value is null — display only; whether the
card can be saved is decided by the server at `POST /items` (WR-121). Tests with a fake fetch and three answers (clean,
one garbled, empty).

### WR-124 — Client: collections from the server, ready for CollectionCard
- status: todo
- depends: WR-102, WR-103, WR-119
- size: S
- paths: `client/src/api/collections.ts`, `client/src/collections.ts`, `client/test/collections.test.mjs`
- verify: `make client/node_modules/.ci-stamp && cd client && node --test test/collections.test.mjs && npx tsc --noEmit`
- expect: green; API functions tested with a fake fetch; the card view tested for 0, 1, 4, 5 and 23 items

`client/src/api/collections.ts`: `listCollections`, `getCollection`, `createCollection` (as in the contract of
WR-119) via `request` of WR-102; items parsed with `parseItems` (reuse; no second parser); a body the contract does
not allow → throws. `client/src/collections.ts` (pure): `collectionCard(c)` → `{ id, name, countText, thumbs, more }`
for `CollectionCard` (2×2 collage, mockup «Шкаф»): `countText` from `itemsCount` of WR-103 («17 вещей»); when
`count ≤ 4`, `thumbs` = all preview items and `more = null`; otherwise `thumbs` = the first 3 and `more = '+' +
(count − 3)` in the fourth cell (the mockup: 17 → «+14», 23 → «+20», 8 → «+5»). `count` is the server's, never the
length of `preview`. Tests: the counts in `expect`; «+N» matches the mockup numbers; an empty collection → no thumbs,
«0 вещей»; `createCollection` sends `{name}` and surfaces a 409 as `ApiError` code `DUPLICATE_NAME`.

### WR-125 — Client: typed calls for one item, adding, editing and the archive
- status: todo
- depends: WR-102, WR-109, WR-118, WR-121, WR-122
- size: S
- paths: `client/src/api/items.ts`, `client/test/api-items.test.mjs`
- verify: `make client/node_modules/.ci-stamp && cd client && node --test test/api-items.test.mjs && npx tsc --noEmit`
- expect: green; each function's method, path and body checked with a fake fetch; errors arrive as `ApiError` with `fields`

Extend `client/src/api/items.ts` (WR-102 created it with `listItems`): `getItem(baseUrl, id)` (`GET /items/{id}`),
`addItem(baseUrl, card)` (`POST /items`, returns the new `Item`), `editCard(baseUrl, id, card)` (`PUT
/items/{id}/card`), `archiveItem(baseUrl, id)` (`POST /items/{id}/archive`), `listArchive(baseUrl)` (`GET /archive`),
`restoreItem(baseUrl, id)` (`POST /archive/{id}/restore`) — each with `fetchFn = fetch` last, ids URL-encoded with
`encodeURIComponent`, answers parsed with `parseItems` from `client/src/items.ts` (one item: `parseItems([body])[0]`).
Types `NewItem` (all fields optional) and `CardFields` (all required) as in the contract. No retries, no caching, no
rule: an `INCOMPLETE_CARD` answer is passed on as `ApiError` with its `fields`, never pre-checked in the client.
Tests (new file): method, URL and JSON body of every function; an id with a space and «/» encoded; 404 → `ApiError`
`UNKNOWN_ITEM`; 400 `INCOMPLETE_CARD` keeps `fields`; a malformed item in the answer throws.


## Screens and components from the approved mockups

### WR-128 — Dates in Russian and the WeatherStrip (вид проверяет Aika)
- status: todo
- depends: WR-028, WR-035, WR-126
- size: S
- paths: `client/src/dates.ts`, `client/src/weather.ts`, `client/src/ui/WeatherStrip.tsx`, `client/src/screens/Sheet.tsx`, `client/test/dates.test.mjs`, `docs/backlog/reports/screens/WR-128.png`
- verify: `make client/node_modules/.ci-stamp && cd client && npm test`
- expect: green; dates agree with JavaScript's `Date` (UTC) for every day of 2000–2040; the PR says «draft — вид проверяет Aika»

`WeatherStrip` (DESIGN.md): «Чт, 3 декабря · Алматы · −4° снег» (mockups: home and builder).

1. `client/src/dates.ts` (pure, integer arithmetic, no `Intl`, no `toLocale*`; ISO `YYYY-MM-DD` strings in and
   out): `isoDate(y, m, d)`, `parseIso(s)` (strict, throws), `weekday(iso)` (0 = Monday), `daysInMonth(y, m)`,
   `addDays(iso, n)`, `dayTitle(iso)` → «Чт, 3 декабря», `dayMonth(iso)` → «1 декабря» (genitive months),
   `monthTitle(y, m)` → «Декабрь» (nominative), `WEEKDAYS_SHORT` «Пн…Вс», and `monthGrid(y, m)` → weeks (Monday
   first) of `{ iso, day, inMonth }`, padded with the neighbour months' days to whole weeks.
2. `client/src/weather.ts` (pure): `type Weather = { celsius: number; rain: boolean; snow: boolean }` (the
   contract's `Weather`, WR-049); `temperatureText(c)` → «−4°» with a real minus «−», «0°», «12°»; `weatherWord(w)`
   → «снег» / «дождь» / «без осадков» (snow wins over rain); `WEATHER_PRESETS` — five choices a person taps until a
   weather source exists: «мороз» −10 snow, «холодно» 0, «прохладно» 8, «тепло» 18, «жарко» 28 (rain and snow are
   separate toggles) and `DEFAULT_PRESET` = «прохладно»; `CITY = 'Алматы'`. These are display defaults, not a
   wardrobe rule — what the weather asks for is the server's (WR-041); write them in `DECISIONS.md` as Aika's to
   change.
3. `client/src/ui/WeatherStrip.tsx`: props `{ date: string, city: string, weather: Weather, onPress? }`; one line as
   in the mockup (bold date and temperature, small dots between); only DESIGN.md tokens; `Pressable` when `onPress`.
   Append a sample to `SHEET` in `client/src/screens/Sheet.tsx`.

`dates.test.mjs` (new): every day 2000-01-01…2040-12-31 — `weekday` and `addDays` agree with `Date.UTC`; leap
years; `monthGrid(2026, 12)` starts on Mon 30 November and has 5 weeks; titles for each month; bad ISO strings throw;
the weather texts and preset table.

Screenshot: `cd client && npm run screenshot -- WR-128 --route '/#/sheet'` writes
`docs/backlog/reports/screens/WR-128.png`; read it, compare with `WeatherStrip` in `docs/design/mockups/mockups.html`,
commit the PNG and name it in the PR. The PR says «draft — вид проверяет Aika» and describes the look in words.

### WR-129 — StatTile (вид проверяет Aika)
- status: todo
- depends: WR-028, WR-035, WR-126
- size: S
- paths: `client/src/ui/StatTile.tsx`, `client/src/ui/statTile.ts`, `client/src/screens/Sheet.tsx`, `client/test/stat-tile.test.mjs`, `docs/backlog/reports/screens/WR-129.png`
- verify: `make client/node_modules/.ci-stamp && cd client && npm test`
- expect: green; the label splitting is tested; the PR says «draft — вид проверяет Aika»

`StatTile` (DESIGN.md: number + label on a `PaperCard`; mockup «Календарь образов», «Итоги месяца»: «14 / образов
записано», a drawing / «чаще всего: тёмные брюки», «6 / вещей не надевались»). `client/src/ui/StatTile.tsx`: props
`{ value: string | number | ReactNode, label: string }` — a number is shown with `numberText`, a node (e.g. a
`ClothingDrawing` passed by a screen) is shown as is; the label in `xs`, `ink-soft`, at most two lines, centred.
Card look of the mockup's `.stat` (card colour, radius `card`, shadow), tokens only, `View`/`Text`/`StyleSheet`.

`client/src/ui/statTile.ts` (pure): `numberText(n)` → «14», «1 234» (thin no-break space U+202F between groups of
three, no `toLocaleString`), negative numbers and non-integers throw (a count is whole). Test (new file): 0, 7, 999,
1000, 1 234 567, the throws. Append two samples to `SHEET` (a number and a short text value).

Screenshot: `cd client && npm run screenshot -- WR-129 --route '/#/sheet'` writes
`docs/backlog/reports/screens/WR-129.png`; read it, compare with `StatTile` in `docs/design/mockups/mockups.html`,
commit the PNG and name it in the PR. The PR says «draft — вид проверяет Aika» and describes the look in words.

### WR-130 — CategoryTabs (вид проверяет Aika)
- status: todo
- depends: WR-028, WR-035, WR-126
- size: S
- paths: `client/src/ui/CategoryTabs.tsx`, `client/src/ui/categoryTabs.ts`, `client/src/screens/Sheet.tsx`, `client/test/category-tabs.test.mjs`, `docs/backlog/reports/screens/WR-130.png`
- verify: `make client/node_modules/.ci-stamp && cd client && npm test`
- expect: green; the tab model is tested; the PR says «draft — вид проверяет Aika»

`CategoryTabs` (DESIGN.md: text tabs with an underline in `terracotta`; used in the builder «Все · Верх · Низ ·
Верхняя · Обувь · Аксессуары» and on the doll «Верх · Низ · Платья · Верхняя»). The component is generic: the
screen passes the tabs (the builder will pass `TABS` of WR-104; do not import it here).

`client/src/ui/CategoryTabs.tsx`: props `{ tabs: { key: string; title: string; count?: number }[], selected:
string, onSelect(key) }`; a horizontal row (scrolls sideways when it does not fit at 390 px) of `Pressable` texts,
`sm` bold, `ink-faint`, the selected one `ink` with a 2.5 px `terracotta` underline, a hairline `rule` under the
row — as `.tabs` in the mockup. An optional count is shown small after the title. Tokens only.

`client/src/ui/categoryTabs.ts` (pure): `tabCells(tabs, selected)` → `{ key, title, selected }[]` in input order;
a `selected` key that is not among the tabs selects the first; an empty list → `[]`; duplicate keys throw. Test (new
file) for these. Append a sample to `SHEET` (the builder's six titles, «Верх» selected).

Screenshot: `cd client && npm run screenshot -- WR-130 --route '/#/sheet'` writes
`docs/backlog/reports/screens/WR-130.png`; read it, compare with `CategoryTabs` in `docs/design/mockups/mockups.html`,
commit the PNG and name it in the PR. The PR says «draft — вид проверяет Aika» and describes the look in words.

### WR-131 — NewCard (вид проверяет Aika)
- status: todo
- depends: WR-028, WR-035, WR-126
- size: S
- paths: `client/src/ui/NewCard.tsx`, `client/src/screens/Sheet.tsx`, `client/test/new-card.test.mjs`, `docs/backlog/reports/screens/WR-131.png`
- verify: `make client/node_modules/.ci-stamp && cd client && npm test`
- expect: green; the PR says «draft — вид проверяет Aika»

`NewCard` (DESIGN.md: dashed card with «+» and a label; mockup «Шкаф»: «+ Новая подборка» beside the collection
cards). `client/src/ui/NewCard.tsx`: props `{ label: string, onPress?, minHeight?: number }`: a 1.5 dashed border
in `ink-faint`, radius `card`, no fill and no shadow, «+» in size `title` above the label in `sm` bold `ink-soft`,
centred — as `.newc` in the mockup. Without `onPress` it is a plain `View` (no pressed state); with it, a
`Pressable`. Tokens only; `View`/`Text`/`Pressable`/`StyleSheet`.

There is no pure logic here; `new-card.test.mjs` (new) reads `client/src/ui/NewCard.tsx` as text and checks it uses
no colour literal (`#…`, `rgb(`) and imports `tokens.ts` — colours come from the tokens only. Append a sample to
`SHEET`.

Screenshot: `cd client && npm run screenshot -- WR-131 --route '/#/sheet'` writes
`docs/backlog/reports/screens/WR-131.png`; read it, compare with `NewCard` in `docs/design/mockups/mockups.html`,
commit the PNG and name it in the PR. The PR says «draft — вид проверяет Aika» and describes the look in words.

### WR-132 — HelperTile (вид проверяет Aika)
- status: todo
- depends: WR-028, WR-033, WR-035, WR-126, WR-127
- size: S
- paths: `client/src/ui/HelperTile.tsx`, `client/src/ui/helperTile.ts`, `client/src/screens/Sheet.tsx`, `client/test/helper-tile.test.mjs`, `docs/backlog/reports/screens/WR-132.png`
- verify: `make client/node_modules/.ci-stamp && cd client && npm test`
- expect: green; label and counter logic tested; the PR says «draft — вид проверяет Aika»

`HelperTile` (DESIGN.md: square tile, drawn icon, two-line label, optional counter; mockup «Главная»: «Собрать
образ», «Что с чем носить», «Сборы в поездку», «Сейчас в стирке» with «3»). `client/src/ui/HelperTile.tsx`: props
`{ icon: keyof ICONS, accent: 'terracotta' | 'sage' | 'mustard' | 'dusty-blue' | 'plum', label: string, count?:
number, onPress? }`. The icon is `iconXml` of WR-127 rendered with `SvgXml` (react-native-svg, ADR-0003), 30 × 30
in the accent colour; the label `sm` bold, two lines; the counter a small `dusty-blue` pill with `card`-coloured
text in the top right corner (as `.tile .count`). Card look of `.tile`: `card` fill, radius `card`, shadow,
padding `md`, min height 88. Tokens only.

`client/src/ui/helperTile.ts` (pure): `splitLabel(label)` → two lines: an explicit `\n` wins; otherwise break at
the space nearest the middle («Что с чем носить» → «Что с чем» / «носить», as the mockup); one word → one line.
`counterText(n)` → `null` for `undefined` or 0 (no pill), «3», «99+» above 99; negative or fractional throws. Test
(new file) for both. Append the four mockup tiles to `SHEET`.

Screenshot: `cd client && npm run screenshot -- WR-132 --route '/#/sheet'` writes
`docs/backlog/reports/screens/WR-132.png`; read it, compare with `HelperTile` in `docs/design/mockups/mockups.html`,
commit the PNG and name it in the PR. The PR says «draft — вид проверяет Aika» and describes the look in words.

### WR-133 — RoundAction (вид проверяет Aika)
- status: todo
- depends: WR-028, WR-033, WR-035, WR-126, WR-127
- size: S
- paths: `client/src/ui/RoundAction.tsx`, `client/src/screens/Sheet.tsx`, `client/test/round-action.test.mjs`, `docs/backlog/reports/screens/WR-133.png`
- verify: `make client/node_modules/.ci-stamp && cd client && npm test`
- expect: green; the PR says «draft — вид проверяет Aika»

`RoundAction` (DESIGN.md: round button with a drawn icon and a word under it; mockup «Шкаф»: «Носка», «Хочу»,
«Поездка», «Нужно ли?»). `client/src/ui/RoundAction.tsx`: props `{ icon, accent, label, onPress? }` (same `icon` and
`accent` types as `HelperTile` if WR-132 is merged — import its types rather than repeating them; else declare them
in this file and let the later task import them). A 56 px circle in `card` with the shadow, the icon of WR-127 via
`iconXml` + `SvgXml`, 26 px, in the accent colour; the word under it `xs` bold `ink`, centred, width 76 — as
`.ract` in the mockup. Without `onPress` it is shown at `ink-faint` for the word and does not react (a place that
has no screen yet). Tokens only.

`round-action.test.mjs` (new): reads the component as text and checks there is no colour literal and that every
icon name used in `SHEET` exists in `ICONS` (import `icons.ts`). Append the four mockup actions to `SHEET`.

Screenshot: `cd client && npm run screenshot -- WR-133 --route '/#/sheet'` writes
`docs/backlog/reports/screens/WR-133.png`; read it, compare with `RoundAction` in `docs/design/mockups/mockups.html`,
commit the PNG and name it in the PR. The PR says «draft — вид проверяет Aika» and describes the look in words.

### WR-134 — StatusSwitch: only the moves the server allows (вид проверяет Aika)
- status: todo
- depends: WR-025, WR-028, WR-033, WR-035, WR-126, WR-127
- size: S
- paths: `client/src/ui/StatusSwitch.tsx`, `client/src/ui/statusSwitch.ts`, `client/src/screens/Sheet.tsx`, `client/src/screens/ItemDetails.tsx`, `client/test/status-switch.test.mjs`, `docs/backlog/reports/screens/WR-134.png`
- verify: `make client/node_modules/.ci-stamp && cd client && npm test`
- expect: green; every status × every `next` subset gives the right cells; the PR says «draft — вид проверяет Aika»

`StatusSwitch` (DESIGN.md: four-way control of an item's status; only the moves the server allows are active;
mockup «Карточка вещи»: «Чистое · В стирке (current) · В ремонт · В поездку (off)»). The client never knows the
transition table: the allowed moves are the item's `next` from the server (WR-008) — one rule in one place.

`client/src/ui/statusSwitch.ts` (pure): `switchCells(status, next)` → four cells in the order CLEAN, IN_WASH,
IN_REPAIR, ON_TRIP: `{ status, label, icon, state: 'current' | 'move' | 'off' }` — current when it is the item's
status (label = `STATUS_LABEL` of `client/src/items.ts` with a capital first letter: «В стирке»); move when it is in
`next` (label = the move word: «Чистое», «В стирку», «В ремонт», «В поездку»); otherwise off (move word). The
move-words table moves from WR-025's code to this component (`statusSwitch.ts`) and is imported there
(`ItemDetails.tsx`; one table). Icons: `clean`, `wash`, `repair`, `trip` of WR-127. A `next` holding the current status
or an unknown status throws.

`client/src/ui/StatusSwitch.tsx`: props `{ status, next, onMove(status), busy? }`; the `.sw` look: a `paper-deep`
track, radius `card`; the current cell raised (`card`, shadow, `ink`), move cells `ink-soft` and `Pressable`, off
cells at opacity .38 and not pressable; while `busy` nothing is pressable. Each cell shows its status colour
(`status` token) as the icon colour — colour together with the word (DESIGN.md rule). Tokens only.

Test (new file): for each of the four statuses and every subset of the other three as `next`, the states and labels;
the throws. Append three samples to `SHEET` (the mockup case: IN_WASH with next CLEAN, IN_REPAIR).

Screenshot: `cd client && npm run screenshot -- WR-134 --route '/#/sheet'` writes
`docs/backlog/reports/screens/WR-134.png`; read it, compare with `StatusSwitch` in `docs/design/mockups/mockups.html`,
commit the PNG and name it in the PR. The PR says «draft — вид проверяет Aika» and describes the look in words.

### WR-135 — BottomNav on the main screens (вид проверяет Aika)
- status: todo
- depends: WR-028, WR-033, WR-035, WR-126, WR-127
- size: S
- paths: `client/src/ui/BottomNav.tsx`, `client/src/ui/bottomNav.ts`, `client/App.tsx`, `client/src/screens/Sheet.tsx`, `client/test/bottom-nav.test.mjs`, `docs/backlog/reports/screens/WR-135.png`
- verify: `make client/node_modules/.ci-stamp && cd client && npm test`
- expect: green; the place model is tested; the bar shows on the main routes only; the PR says «draft — вид проверяет Aika»

`BottomNav` (DESIGN.md: five places, the middle one a raised round «+»; mockups «Главная», «Шкаф», «Календарь»:
Главная · Шкаф · (+) · Календарь · Я).

`client/src/ui/bottomNav.ts` (pure): `PLACES` — `home` «Главная», `wardrobe` «Шкаф», `add` (no word), `calendar`
«Календарь», `me` «Я», with their icons of WR-127 (`home`, `wardrobe`, `plus`, `calendar`, `me`); `placeOf(route)`
→ the active place for a route name (`MAIN_ROUTES` of `client/src/nav.ts`), `null` elsewhere; `routeOfPlace(key)` →
the route a place opens: the four main routes and `add` → `{ name: 'builder' }` (a new outfit: there is no
add-an-item screen yet, see the questions for Aika; write this in `DECISIONS.md` as reversible).
`showsBar(route)` → true only on `MAIN_ROUTES`.

`client/src/ui/BottomNav.tsx`: props `{ active, onPlace(key) }`; the `.nav` look: `card` bar with a `rule` hairline
on top, five equal columns, icon 24 + word `xs` bold, `ink-faint`, the active one `ink`; the middle place a 56 px
`terracotta` circle raised by `xl` with a `paper` ring and the `plus` icon in `on-accent`. Tokens only (the
shadow of the round button may use `terracotta` at low opacity as in the mockup — derive it from the token).
In `App.tsx` show it under the screen when `showsBar(current)`; a place calls `reset(routeOfPlace(key))` (a tab
switch clears the back stack). A place whose route is not registered yet in `ROUTES` still renders and opens the
default route (unknown names fall back, WR-126).

Test (new file): five places in order, `placeOf` for each main route and `null` for `item`, `routeOfPlace('add')`.

Append a sample bar (active «Шкаф») to `SHEET`. Screenshot: `cd client && npm run screenshot -- WR-135 --route
'/#/sheet'` writes `docs/backlog/reports/screens/WR-135.png`; read it, compare with
`BottomNav` in `docs/design/mockups/mockups.html`, commit the PNG and name it in the PR. The PR says «draft — вид
проверяет Aika» and describes the look in words.

### WR-136 — OutfitCanvas: items as a flat lay on ruled paper (вид проверяет Aika)
- status: todo
- depends: WR-028, WR-033, WR-035, WR-126
- size: S
- paths: `client/src/ui/OutfitCanvas.tsx`, `client/src/ui/flatLay.ts`, `client/src/screens/Sheet.tsx`, `client/test/flat-lay.test.mjs`, `docs/backlog/reports/screens/WR-136.png`
- verify: `make client/node_modules/.ci-stamp && cd client && npm test`
- expect: green; the layout is deterministic, inside the sheet, integer pixels; the PR says «draft — вид проверяет Aika»

`OutfitCanvas` (DESIGN.md: ruled paper sheet where items lie as a flat lay; mockups «Главная» (coat, shirt,
trousers, shoes, scarf) and «Конструктор образа»). Positions and sizes of drawings on a canvas are not tokens
(DESIGN.md); they come from one pure function.

`client/src/ui/flatLay.ts` (pure): `flatLay(items: {id, type}[], width, height)` → `{ id, x, y, size, z }[]` in
integer pixels. Zones by type, as in the mockups: OUTERWEAR large at the left behind (lowest `z`), TOP upper
middle, DRESS upper-middle taller, BOTTOM lower middle, SHOES bottom right, ACCESSORY right; two items of the same
type step aside by a fixed offset; an unknown type goes to the right column. Every box lies inside the canvas; the
same input gives the same output; input order is kept within a type; no layout rule about whether the items go
together (that is the server's).

`client/src/ui/OutfitCanvas.tsx`: props `{ items: Item[], height: number, onItemPress?(id) }`; measures its width
(`onLayout`), draws the ruled paper (`paper` with `rule` hairlines every `xl`, radius `chip`) and each item with
`ClothingDrawing` of WR-033 (kind from the mapping in `client/src/ui/garments.ts`, colour by the item's `Colour` from
the `item` tokens), absolutely placed by `flatLay`, with the soft contact shadow the drawing already has. An empty
list shows the sheet with a centred `ink-faint` line «Пока пусто». Tokens for everything but positions.

Test (new file): the mockup's five items in a 340 × 182 canvas are all inside and do not share a box; two TOPs do
not overlap fully; determinism; z order puts OUTERWEAR first; an empty list → `[]`. Append the home mockup's outfit
(invented items) to `SHEET`.

Screenshot: `cd client && npm run screenshot -- WR-136 --route '/#/sheet'` writes
`docs/backlog/reports/screens/WR-136.png`; read it, compare with `OutfitCanvas` in `docs/design/mockups/mockups.html`,
commit the PNG and name it in the PR. The PR says «draft — вид проверяет Aika» and describes the look in words.

### WR-137 — CalendarMonth (вид проверяет Aika)
- status: todo
- depends: WR-028, WR-033, WR-035, WR-126
- size: S
- paths: `client/src/ui/CalendarMonth.tsx`, `client/src/ui/calendarMonth.ts`, `client/src/screens/Sheet.tsx`, `client/test/calendar-month.test.mjs`, `docs/backlog/reports/screens/WR-137.png`
- verify: `make client/node_modules/.ci-stamp && cd client && npm test`
- expect: green; the cell model is tested; the PR says «draft — вид проверяет Aika»

`CalendarMonth` (DESIGN.md: month grid; a day holds the thumb of that day's outfit; mockup «Календарь образов»).
The grid itself (weeks, Monday first, neighbour days) is computed by the caller with `monthGrid` of WR-128 and
passed in — this component does no date arithmetic (one rule in one place; if WR-128 is merged, the sample on the
sheet uses it, otherwise a hand-written grid).

`client/src/ui/calendarMonth.ts` (pure): `cellModels(weeks, thumbs: Record<iso, Item>, today: iso)` → rows of
`{ iso, day, kind: 'out' | 'day' | 'today', item: Item | null }` — `out` for days outside the month (no thumb even if
one is given), `today` for the given day inside the month. `weeks` that are not 7 wide throw.

`client/src/ui/CalendarMonth.tsx`: props `{ weeks, thumbs, today, onDay?(iso) }`; the `.cal` look: a header row
«Пн … Вс» (`xs`, extra bold, `ink-faint`), day cells with aspect .78, `card` fill, radius `chip`, shadow, the day
number top centre (`xs` bold `ink-soft`); `out` cells transparent with `ink-faint`; `today` with an inset 2 px
`terracotta` ring; a day's item as a small `ClothingDrawing` (WR-033) at the bottom of the cell. Tokens only (the
drawing size is not a token).

Test (new file): kinds for a month starting on Monday and one starting on Sunday; a thumb on an `out` day is
dropped; `today` outside the month gives no `today`; the throw. Append December 2026 with three invented thumbs to
`SHEET`.

Screenshot: `cd client && npm run screenshot -- WR-137 --route '/#/sheet'` writes
`docs/backlog/reports/screens/WR-137.png`; read it, compare with `CalendarMonth` in `docs/design/mockups/mockups.html`,
commit the PNG and name it in the PR. The PR says «draft — вид проверяет Aika» and describes the look in words.

### WR-138 — CollectionCard (вид проверяет Aika)
- status: todo
- depends: WR-028, WR-033, WR-035, WR-124, WR-126
- size: S
- paths: `client/src/ui/CollectionCard.tsx`, `client/src/screens/Sheet.tsx`, `client/test/collection-card.test.mjs`, `docs/backlog/reports/screens/WR-138.png`
- verify: `make client/node_modules/.ci-stamp && cd client && npm test`
- expect: green; the PR says «draft — вид проверяет Aika»

`CollectionCard` (DESIGN.md: 2×2 collage of `ItemThumb`s, «+N», name, count; mockup «Шкаф»: «Офис · 17 вещей ·
+14»). The view model is already made by WR-124: `collectionCard(c)` in `client/src/collections.ts` → `{ id, name,
countText, thumbs, more }`. Reuse it; do not compute «+N» or the count text here.

`client/src/ui/CollectionCard.tsx`: props `{ card: ReturnType<typeof collectionCard>, onPress? }`; a `PaperCard`
(WR-028) with padding `sm`; a 2 × 2 grid of square cells (`paper` fill, radius `chip`, gap `xs`) holding the
`thumbs` as item drawings (use `ItemThumb` of WR-033 in its square form, or `ClothingDrawing` inside the cell if
`ItemThumb` cannot be square — read its props), the fourth cell «+N» (`sm`, extra bold, `ink-soft`) when `more` is
not null, empty cells left empty; then the name `sm` bold and `countText` `xs` `ink-soft`. Tokens only.

`collection-card.test.mjs` (new): builds the three mockup collections with `collectionCard` (counts 17, 23, 8) and
checks what the component will be given: 3 thumbs and «+14», «+20», «+5»; a collection of 2 → 2 thumbs, no «+N»
(this pins the use of WR-124's model, not a second one). Append the three mockup collections (invented items) to
`SHEET`.

Screenshot: `cd client && npm run screenshot -- WR-138 --route '/#/sheet'` writes
`docs/backlog/reports/screens/WR-138.png`; read it, compare with `CollectionCard` in `docs/design/mockups/mockups.html`,
commit the PNG and name it in the PR. The PR says «draft — вид проверяет Aika» and describes the look in words.

### WR-139 — ItemStrip: items that cannot be worn are faded, by the server's word (вид проверяет Aika)
- status: todo
- depends: WR-025, WR-028, WR-033, WR-035, WR-110, WR-126, WR-101
- size: S
- paths: `client/src/items.ts`, `client/src/fixtures/items.ts`, `client/src/ui/ItemStrip.tsx`, `client/src/ui/itemStrip.ts`, `client/src/screens/Sheet.tsx`, `client/test/item-strip.test.mjs`, `docs/backlog/reports/screens/WR-139.png`
- verify: `make client/node_modules/.ci-stamp && cd client && npm test`
- expect: green; `wearable` is read from the server and drives the fading; the PR says «draft — вид проверяет Aika»

`ItemStrip` (DESIGN.md: horizontal row of `ItemThumb`s; items not clean are faded with their status word; mockup
«Конструктор образа»: «Рубашка · в стирке (faded) · Брюки · в ремонте (faded) · Юбка»). Whether an item can be worn
is the server's rule (invariant 1, `Item.availableForOutfit()`), sent as `wearable` since WR-110; the client never
writes `status === 'CLEAN'` for it.

1. `client/src/items.ts`: `Item` gets `wearable: boolean | null`; `parseItems` reads it when present (not a boolean
   → throw, like the other fields) and sets `null` when absent (older server). Keep everything WR-025 added
   (`next`). `client/test/items.test.mjs` must stay green untouched. In `client/src/fixtures/items.ts` (WR-101)
   give every fixture a `wearable` matching the server rule for its status.
2. `client/src/ui/itemStrip.ts` (pure): `stripEntries(items)` → `{ id, item, caption, faded }`: `faded` =
   `wearable === false`; caption = the status word (`STATUS_LABEL`) when faded, otherwise the item's name; with
   `wearable === null` nothing is faded (we do not guess the rule). Input order kept.
3. `client/src/ui/ItemStrip.tsx`: props `{ items, selected?: string[], onItemPress?(id) }`; a horizontal
   `ScrollView` of 64-wide columns: a square `ItemThumb` (WR-033) and the caption (`xs` bold `ink-soft`, one line,
   ellipsis); faded entries at opacity .45 (as `.strip .it.away`); a selected entry has an `ink` outline on its
   thumb. Faded items stay pressable — the server says ✗ when an outfit holds them (the mockup's «Чистота» line).
   Tokens only.

Test (new file): `parseItems` with `wearable` true/false/absent/garbled; `stripEntries` fading and captions;
order. Append the mockup strip (invented items, two not wearable) to `SHEET`.

Screenshot: `cd client && npm run screenshot -- WR-139 --route '/#/sheet'` writes
`docs/backlog/reports/screens/WR-139.png`; read it, compare with `ItemStrip` in `docs/design/mockups/mockups.html`,
commit the PNG and name it in the PR. The PR says «draft — вид проверяет Aika» and describes the look in words.

### WR-140 — Client: outfit suggestions, the fit check and their reasons in Russian
- status: todo
- depends: WR-022, WR-030, WR-049, WR-102, WR-103
- size: S
- paths: `client/src/api/outfits.ts`, `client/src/checks.ts`, `client/test/outfits-client.test.mjs`
- verify: `make client/node_modules/.ci-stamp && cd client && npm test`
- expect: green; every `CheckCode` and `CheckName` of `api/openapi.yaml` has Russian text; every call checked with a fake fetch

The home and builder screens show the server's suggestions and verdicts; invariant 8 — every answer says why, the
empty one included. Read `api/openapi.yaml` (schemas of WR-049: `SuggestResult`, `Suggestion`, `CheckLine`,
`Rejection`, `FitAnswer`, `Weather`, `Occasion`; `WhyLine` of WR-030).

1. `client/src/api/outfits.ts` (through `request` of WR-102; items parsed with `parseItems`, no second parser;
   a body the contract does not allow throws): `getToday(baseUrl, {occasion, weather})` (`GET /outfits/today`),
   `getSuggestions(baseUrl, {limit?, occasion?, weather?})` (`GET /outfits/suggestions`), `getAround(baseUrl, id,
   {…})` (`GET /items/{id}/outfits`, id URL-encoded), `fitOutfit(baseUrl, {items, occasion?, weather?})` (`POST
   /outfits/fit`), `checkOutfit(baseUrl, items)` (`POST /outfits/check`, WR-030) — each with `fetchFn = fetch`
   last. Query parameters only when given; `rain`/`snow` only with `celsius`.
2. `client/src/checks.ts` (pure): `OCCASION_LABEL` («офис», «выходной», «вечер», «спорт»); `checkText(line)` →
   `{ mark: '✓' | '✗', title, text }` — title per `CheckName` («Акценты», «Комплект», «Аксессуары», «Повод»,
   «Сезон», «Верхняя одежда», «Ткань», «Обувь», «Повтор», «Отбор»), text from one Russian template per `CheckCode`
   (e.g. OUTERWEAR_MISSING «на такую погоду нужна верхняя одежда», REPEATS_OUTFIT «этот образ уже был на днях»), item
   names when the template needs them; `emptyText(result)` → Russian lines for an empty `SuggestResult`: one per
   `missing` type («нет чистой обуви»), then one per rejection with its count (`countText` of WR-103: «3 вещи в
   стирке»). It only words the server's answer — it never decides anything.

`outfits-client.test.mjs` (new): read the `CheckCode`, `CheckName` and `Occasion` enums from `../api/openapi.yaml` as
text (only those schemas) and check each has a label/template; method, path, query and body of every call with a
fake fetch; a 409 `ITEM_NOT_CLEAN` arrives as `ApiError`; three worded examples; an empty result is never worded
as an empty list. Erasable TypeScript only.

### WR-141 — Client: the wear log and wear statistics
- status: todo
- depends: WR-082, WR-083, WR-084, WR-086, WR-102, WR-103, WR-129
- size: S
- paths: `client/src/api/wear.ts`, `client/src/wearText.ts`, `client/test/wear-client.test.mjs`
- verify: `make client/node_modules/.ci-stamp && cd client && npm test`
- expect: green; every call checked with a fake fetch; `null` values stay `null`; money and counts worded in Russian

The calendar and statistics screens read the wear log (WR-082) and the statistics (WR-083, WR-084, WR-086). Read
their schemas in `api/openapi.yaml` first (`WearEvent`, `ItemWear`, the answers of `/stats/*`).

1. `client/src/api/wear.ts` (through `request` of WR-102; items parsed with `parseItems`; a body the contract does
   not allow throws): `logWear(baseUrl, items, on?)` (`POST /wear`), `listWear(baseUrl, {from?, to?})` (`GET
   /wear`), `wearStats(baseUrl, {from?, to?})` (`GET /stats/wear`), `forgotten(baseUrl, {days?, to?})`, `mostWorn(
   baseUrl, {from?, to?})`, `costPerWear(baseUrl, {to?})` — each with `fetchFn = fetch` last; dates as ISO strings
   (`YYYY-MM-DD`), passed through unchanged; `lastWorn`, `daysSinceLastWorn`, `tenge`, `perWear` keep `null`
   (unknown is never 0 — invariant 5).
2. `client/src/wearText.ts` (pure): `tengeText(n)` → «3 333 ₸» (groups of three with U+202F: use `numberText` of
   `client/src/ui/statTile.ts` (WR-129) for thousands grouping, no `toLocaleString`, integers only, throws otherwise); `timesText(n)` → «1 раз», «2 раза», «5 раз» (with `plural` of
   WR-103); `sinceText(days)` → «сегодня», «вчера», «12 дней назад», `null` → «ни разу»; `perWearText(row)` →
   «3 333 ₸ за носку», «цена не указана» (no price), «ещё не носили» (never worn). No statistic is computed here:
   the numbers are the server's.

Test (new file): method, path, query of every call; 400 `BAD_REQUEST` arrives as `ApiError`; nulls kept; the
texts for 0, 1, 2, 5, 11, 21, 1 000, 1 234 567 and the throws.

### WR-142 — Home screen: the outfit of the day and helper tiles (вид проверяет Aika)
- status: todo
- depends: WR-023, WR-028, WR-031, WR-033, WR-035, WR-057, WR-102, WR-126, WR-128, WR-132, WR-136, WR-140
- size: S
- paths: `client/src/screens/Home.tsx`, `client/src/home.ts`, `client/src/screens/routes.tsx`, `client/test/home.test.mjs`, `docs/backlog/reports/screens/WR-142.png`
- verify: `make client/node_modules/.ci-stamp && cd client && npm test`
- expect: green; greeting, occasion default and recent items tested; the PR says «draft — вид проверяет Aika»

Mockup «1 · Главная». Route `home`; set `DEFAULT_ROUTE` to it in `client/src/screens/routes.tsx`. Built only from
DESIGN.md components with its tokens; RN core only (`ScrollView`, `Pressable`); no new dependency.

Top to bottom: `ScreenTitle` with `greeting(hour)` («Доброе утро» 5–11, «Добрый день» 12–17, «Добрый вечер» 18–22,
«Доброй ночи» otherwise; no name in the code); `WeatherStrip` (WR-128) with today's date (`isoDate` from the
device clock), `CITY` and the chosen weather — tapping it shows a row of `Chip`s with `WEATHER_PRESETS`, «дождь»,
«снег» and the occasions (`OCCASION_LABEL` of WR-140), state kept in the screen only. `SectionTitle` «Образ на
сегодня» with the link «другой» (next suggestion, round robin; hidden with one or none). A `PaperCard` with `Tape`
holding `OutfitCanvas` (WR-136, height 182) of the suggestion's items and `WhyItFits` of its `why` lines worded by
`whyText` (WR-031). The data: `getToday` of WR-140 with the occasion (`defaultOccasion(isoDate)`: OFFICE Monday–Friday,
WEEKEND Saturday–Sunday — a display default; write it in `DECISIONS.md`) and the weather. No suggestion → the card
shows `emptyText(result)` lines instead (never an empty card: invariant 8). `SectionTitle` «Помощник» and a 2 × 2
grid of `HelperTile`s (WR-132): «Собрать образ» → `builder`; «Что с чем носить» → `list`; «Сборы в поездку» →
`trip`; «Сейчас в стирке» with the IN_WASH count from `countsByStatus` of WR-023 (`client/src/filter.ts`) →
`{name:'list', param:'IN_WASH'}`. `SectionTitle` «Недавно добавлены» with «все» → `list`, and a row of up to five
round `ItemThumb`s with `StatusDot`: `recentItems(items, 5)` = the last five of `GET /items` (the server appends new
items at the end), newest first; a thumb opens `{name:'item', param:id}`. An error shows the server's message.

`client/src/home.ts` (pure): `greeting`, `defaultOccasion`, `recentItems`, `nextIndex(i, n)`. Test (new file) each,
at the boundaries. Root `View` `testID="screen"` once loaded.

Screenshot: `cd client && npm run screenshot -- WR-142 --route '/#/home'` writes
`docs/backlog/reports/screens/WR-142.png`; read it, compare with «1 · Главная» in `docs/design/mockups/mockups.html`,
commit the PNG and name it in the PR. The PR says «draft — вид проверяет Aika» and describes the look in words.

### WR-143 — Wardrobe screen: statuses and collections (вид проверяет Aika)
- status: todo
- depends: WR-023, WR-028, WR-033, WR-035, WR-102, WR-103, WR-124, WR-126, WR-131, WR-133, WR-138
- size: S
- paths: `client/src/screens/Wardrobe.tsx`, `client/src/wardrobe.ts`, `client/src/screens/routes.tsx`, `client/test/wardrobe.test.mjs`, `docs/backlog/reports/screens/WR-143.png`
- verify: `make client/node_modules/.ci-stamp && cd client && npm test`
- expect: green; the subtitle and the status chips are tested; the PR says «draft — вид проверяет Aika»

Mockup «2 · Шкаф». Route `wardrobe`. Built only from DESIGN.md components with its tokens; RN core only; no new
dependency.

`ScreenTitle` «Мой шкаф» with the subtitle `subtitle(n)` → «64 вещи · без ограничений» (`itemsCount` of WR-103;
n = the length of `GET /items` via `listItems` of WR-102 — no limit on items, invariant 9). A row of four
`RoundAction`s (WR-133): «Носка» → `wear`, «Хочу» without `onPress` (no wish list exists; see the questions for
Aika), «Поездка» → `trip`, «Нужно ли?» → `buy`. `SectionTitle` «Где вещи сейчас» and a wrapping row of `Chip`s:
«Все 64», then each status with its `StatusDot` colour, its word and its count from `countsByStatus` of WR-023
(`client/src/filter.ts`, zero included — reuse, do not count again); a chip opens `{name:'list', param: status}`
(«Все» → `list`). `SectionTitle` «Подборки» and a two-column grid of `CollectionCard`s (WR-138) from
`listCollections` + `collectionCard` of WR-124, then a `NewCard` (WR-131) «Новая подборка» without `onPress`
(naming a collection needs a text field, which DESIGN.md has not got yet). Collections that fail to load show the
server's message in place of the grid; the status part still shows.

`client/src/wardrobe.ts` (pure): `subtitle(n)`, `statusChips(counts, total)` → `{ key, label, count }` in the order
Все, CLEAN, IN_WASH, IN_REPAIR, ON_TRIP with the words of `STATUS_LABEL` capitalised. Test (new file): 0, 1, 64
items; chips with zero counts present. Root `View` `testID="screen"` once loaded.

Screenshot: `cd client && npm run screenshot -- WR-143 --route '/#/wardrobe'` writes
`docs/backlog/reports/screens/WR-143.png`; read it, compare with «2 · Шкаф» in `docs/design/mockups/mockups.html`,
commit the PNG and name it in the PR. The PR says «draft — вид проверяет Aika» and describes the look in words.

### WR-144 — Outfit builder: put items together, the server says why (вид проверяет Aika)
- status: todo
- depends: WR-028, WR-031, WR-033, WR-035, WR-055, WR-104, WR-126, WR-128, WR-130, WR-136, WR-139, WR-140, WR-141
- size: S
- paths: `client/src/screens/Builder.tsx`, `client/src/builder.ts`, `client/src/screens/routes.tsx`, `client/test/builder.test.mjs`, `docs/backlog/reports/screens/WR-144.png`
- verify: `make client/node_modules/.ci-stamp && cd client && npm test`
- expect: green; choosing, the fit request and the reason lines are tested; the PR says «draft — вид проверяет Aika»

Mockup «3 · Конструктор образа». Route `builder`, optional param = an item id chosen at the start. Built only from
DESIGN.md components with its tokens; RN core only; no new dependency.

Header: «‹» back (WR-126 convention), `ScreenTitle` «Новый образ», `Button` «Сохранить». `WeatherStrip` (WR-128)
with today and the chosen preset (tap → `Chip`s of `WEATHER_PRESETS`, «дождь», «снег», as on the home screen). A
`PaperCard` with `Tape` holding `OutfitCanvas` (WR-136, height 250) of the chosen items — tapping a drawn item removes
it — and `WhyItFits` of the answer of `fitOutfit` (WR-140) for the chosen items and weather: first the `why` lines
worded by `whyText` (WR-031), then the check lines that say ✗ worded by `checkText` (WR-140); nothing chosen →
the line «Выберите вещи снизу». Under the card: `CategoryTabs` (WR-130) with `TABS` and `tabCounts` of WR-104,
then `ItemStrip` (WR-139) of `itemsInTab(items, tab)`; tapping an entry toggles it in the outfit (`selected`
outlined). Any item may be chosen: a dirty or doubled one is the server's ✗, never refused by the client (one rule
in one place). The fit request is sent after each change (the latest answer wins: ignore an older reply that arrives
late). «Сохранить» calls `logWear` of WR-141 for today with the chosen ids, then `reset({name:'calendar'})`; an
error shows the server's message (`client/src/errors.ts` of WR-112 if it exists, else the `ApiError` message).
Saving an outfit = recording it as worn today in the calendar: write this in `DECISIONS.md` as reversible.

`client/src/builder.ts` (pure): `toggle(chosen, id)` (order kept, an id once), `fitRequest(chosen, weather)` →
the body of `POST /outfits/fit` (`null` when nothing is chosen), `latest(seq)` helper for "the newest reply wins".
Test (new file) each. Root `View` `testID="screen"` once the items have loaded.

Screenshot: `cd client && npm run screenshot -- WR-144 --route '/#/builder/white-shirt'` (an item id of the seed;
check `GET /items`) writes `docs/backlog/reports/screens/WR-144.png`; read it, compare with «3 · Конструктор образа»
in `docs/design/mockups/mockups.html`, commit the PNG and name it in the PR. The PR says «draft — вид проверяет
Aika» and describes the look in words.

### WR-145 — Calendar of outfits with the month's totals (вид проверяет Aika)
- status: todo
- depends: WR-028, WR-033, WR-035, WR-126, WR-128, WR-129, WR-137, WR-139, WR-141
- size: S
- paths: `client/src/screens/Calendar.tsx`, `client/src/calendarScreen.ts`, `client/src/screens/routes.tsx`, `client/test/calendar-screen.test.mjs`, `docs/backlog/reports/screens/WR-145.png`
- verify: `make client/node_modules/.ci-stamp && cd client && npm test`
- expect: green; day thumbs and month totals are tested; the PR says «draft — вид проверяет Aika»

Mockup «4 · Календарь образов». Route `calendar`, optional param `YYYY-MM` (default: this month by the device
clock). Built only from DESIGN.md components with its tokens; RN core only; no new dependency.

`ScreenTitle` with `monthTitle` (WR-128) and «‹ 2026 ›» on the right (‹ › move one month: `go` with the new param).
`CalendarMonth` (WR-137) with `monthGrid(y, m)` and the day thumbs from `listWear` of WR-141 for the month (`from`
= the 1st, `to` = the last day). `SectionTitle` «Итоги месяца» and three `StatTile`s (WR-129): the number of wear
events of the month with «образов записано» (`OUTFIT_FORMS` of WR-103 for the word: «1 образ записан» / «14 образов
записано»); «чаще всего:» + the name and a small drawing of the item with the highest `timesWorn` in `wearStats` for
the month (ties: the first in the server's order; none worn → «—»); the count of items with `timesWorn` 0 with
«вещей не надевались» (plural). Then a `PaperCard`: the first item of `wearStats` with `timesWorn` 0 and `wearable`
false — its `ItemThumb` with `StatusDot`, «{name} ни разу за месяц — сейчас {status word}» and its `StatusBadge`;
no such item → no card. An empty month shows the grid with no thumbs and the tiles with 0 (logs start empty).

`client/src/calendarScreen.ts` (pure): `dayThumbs(events, itemsById)` → `Record<iso, Item>`: for each day the
**last** event of that day; its item shown is the first DRESS, else the first TOP, else the first item (a display
choice: write it in `DECISIONS.md`); `monthTotals(events, stats)` → the three tile values; `shiftMonth('2026-12',
±1)`. Test (new file): two events on a day, a day with only shoes, year boundaries, ties, the empty month.
Root `View` `testID="screen"` once loaded.

Screenshot: `cd client && npm run screenshot -- WR-145 --route '/#/calendar'` writes
`docs/backlog/reports/screens/WR-145.png`; read it, compare with «4 · Календарь образов» in
`docs/design/mockups/mockups.html`, commit the PNG and name it in the PR. The PR says «draft — вид проверяет Aika»
and describes the look in words (the log is empty on a fresh server: say so).

### WR-146 — Item card as in the mockup: status since, switch, what goes with it (вид проверяет Aika)
- status: todo
- depends: WR-010, WR-024, WR-025, WR-028, WR-031, WR-033, WR-035, WR-087, WR-109, WR-126, WR-128, WR-134, WR-140
- size: S
- paths: `client/src/screens/ItemDetails.tsx`, `client/src/itemCard.ts`, `client/src/api/itemCard.ts`, `client/src/api/items.ts`, `client/test/item-card.test.mjs`, `docs/backlog/reports/screens/WR-146.png`
- verify: `make client/node_modules/.ci-stamp && cd client && npm test`
- expect: green; chips, the badge text and the hint from `next` are tested; the PR says «draft — вид проверяет Aika»

Mockup «5 · Карточка вещи — статусы». Route `item` (param = id), the screen of WR-024/WR-025, reworked to the
mockup. Read `client/src/screens/ItemDetails.tsx` first; keep its logic tests green untouched.

Top: «‹» back and a `StatusBadge` «в стирке с 1 декабря» (`badgeText(status, since)`; `since` from `GET
/items/{id}/history` of WR-087 formatted by `dayMonth` of WR-128; `since` null → only the status word). A
`PaperCard` with `Tape` and the item's large drawing (`ClothingDrawing` of WR-033, about 150 px). `ScreenTitle`
with the name; a row of `Chip`s `attributeChips(item)`: type, fabric, formality (labels of WR-022) and seasons
(«Круглый год» when all four, else the season labels joined by «, »). `SectionTitle` «Где она сейчас»,
`StatusSwitch` (WR-134) with the item's `next`; a move calls `changeStatus` of WR-025 and reloads; a refusal shows
the server's message. Under it a small `ink-soft` line `nextHint(next)` («Отсюда можно: в шкаф, в ремонт.»; empty
`next` → «Сейчас статус не меняется.») — built from `next` only, never from a table of the client.
`SectionTitle` «С чем носить» with «все N» (N = `goes.length`) → `{name:'builder', param:id}`, and a `PaperCard` with
`WhyItFits` of two lines: the first item of `goes` with ✓ and the first of `doesNot` with ✗ (from `GET
/items/{id}/matches`, WR-010); each line's text is `whyText` (WR-031) of one line of `checkOutfit([id, partner])`
(WR-140): for ✗ the first failing line, for ✓ the COLOUR line. Empty lists → no line for that side.

Data: `getItem` (`GET /items/{id}`, WR-109) — if `client/src/api/items.ts` has it (WR-125), use it; otherwise add it
there exactly as WR-125 describes. `client/src/api/itemCard.ts`: `getMatches`, `getHistory` through `request` of
WR-102. `client/src/itemCard.ts` (pure): `badgeText`, `attributeChips`, `nextHint`. Test (new file): each, every
status, a fake fetch for both calls. Root `View` `testID="screen"` once loaded.

Screenshot: `cd client && npm run screenshot -- WR-146 --route '/#/item/blue-jeans'` (a seed item in the wash; check
`GET /items`) writes `docs/backlog/reports/screens/WR-146.png`; read it, compare with «5 · Карточка вещи» in
`docs/design/mockups/mockups.html`, commit the PNG and name it in the PR. The PR says «draft — вид проверяет Aika»
and describes the look in words.

### WR-147 — Wear statistics screen: forgotten, most worn, cost per wear (вид проверяет Aika)
- status: todo
- depends: WR-022, WR-028, WR-033, WR-035, WR-126, WR-128, WR-129, WR-141
- size: S
- paths: `client/src/screens/WearStats.tsx`, `client/src/wearScreen.ts`, `client/src/screens/routes.tsx`, `client/test/wear-screen.test.mjs`, `docs/backlog/reports/screens/WR-147.png`
- verify: `make client/node_modules/.ci-stamp && cd client && npm test`
- expect: green; the period and the rows are tested; the PR says «draft — вид проверяет Aika»

There is no mockup of this screen: build it only from DESIGN.md components with its tokens, in the manner of the
calendar's «Итоги месяца» (no new look, no new component; if something is missing, park with a proposal). Route
`wear` (the «Носка» action of the wardrobe screen). RN core only; no new dependency.

«‹» back; `ScreenTitle` «Носка» with the subtitle of the period; a row of `Chip`s «30 дней · 90 дней · всё время»
(selected = ink outline). Three `StatTile`s: wear events in the period (`listWear`), items worn at least once and
items never worn in the period (`wearStats` of WR-141). `SectionTitle` «Давно не надевала» — up to 10 rows from
`forgotten` (`days` = the period, 30 for «всё время»), each on a `PaperCard`: round `ItemThumb` with `StatusDot`,
the name and `sinceText`. `SectionTitle` «Чаще всего» — one row per type from `mostWorn` («Верх» label of WR-022,
the item's name, `timesText`). `SectionTitle` «Цена за носку» — rows from `costPerWear` with `perWearText`; items
without a price come last as one line «у N вещей не указана цена» (prices are set elsewhere; this screen only reads).
Empty logs show zeros and the line «Пока ничего не записано» — never a blank screen.

`client/src/wearScreen.ts` (pure): `periodRange(key, todayIso)` → `{from, to}` (`addDays` of WR-128; «всё время» →
no `from`); `costRows(answer)` → priced rows in the server's order plus the count of unpriced ones. Test (new file):
the three periods across a month and a year boundary; cost rows with nulls. Root `View` `testID="screen"` once
loaded.

Screenshot: `cd client && npm run screenshot -- WR-147 --route '/#/wear'` writes
`docs/backlog/reports/screens/WR-147.png`; read it, check it against DESIGN.md and the calendar mockup, commit the PNG
and name it in the PR. The PR says «draft — вид проверяет Aika» and describes the look in words.

### WR-148 — Trip packing screen: plan, depart and come back (вид проверяет Aika)
- status: todo
- depends: WR-022, WR-028, WR-033, WR-035, WR-089, WR-090, WR-102, WR-103, WR-126
- size: S
- paths: `client/src/screens/Trip.tsx`, `client/src/trip.ts`, `client/src/api/trips.ts`, `client/src/screens/routes.tsx`, `client/test/trip.test.mjs`, `docs/backlog/reports/screens/WR-148.png`
- verify: `make client/node_modules/.ci-stamp && cd client && npm test`
- expect: green; the calls, the presets and the shortage texts are tested; the PR says «draft — вид проверяет Aika»

There is no mockup of this screen: build it only from DESIGN.md components with its tokens (no new look; no text
field — choices are `Chip`s). Route `trip` (home tile «Сборы в поездку», wardrobe action «Поездка»). RN core only.

1. `client/src/api/trips.ts` (through `request` of WR-102, items parsed with `parseItems`): `planTrip(baseUrl,
   {days, minTemp, maxTemp, laundry})` (`POST /trips/plan`, WR-089), `depart(baseUrl, ids)` (`POST
   /trips/departures`, WR-090), `returnItems(baseUrl, [{id, status}])` (`POST /trips/returns`); `fetchFn` last.
2. `client/src/trip.ts` (pure): `TRIP_DAYS` [2, 3, 5, 7, 10, 14]; `TRIP_TEMPS` — «мороз» −15…−5, «холодно» −5…5,
   «прохладно» 5…15, «тепло» 15…25, «жара» 25…35 (display presets, not a rule: what to pack is the server's
   `PackTrip`; write them in `DECISIONS.md`); `shortageText(s)` → «Обувь: нужно 2, нашлось 1» (type label of WR-022);
   `planIds(plan)` → every item id of the slots, in slot order, once.
3. Screen: «‹» back; `ScreenTitle` «Сборы в поездку»; `SectionTitle`s with `Chip` rows for days, temperature and
   «есть стирка»; `Button` «Собрать» → the plan: per slot a `SectionTitle` «{type} · нужно N» and a row of
   `ItemThumb`s; shortages as `WhyItFits`-style ✗ lines if `WhyItFits` accepts plain lines (read its props), else
   `ink-soft` text lines. `Button` «Взять в поездку» → `depart(planIds)`; success shows «{countText} в поездке»; a
   409 shows the server's message (`client/src/errors.ts` of WR-112 if it exists). Section «Вернуться»: the items
   of `GET /items` whose status is ON_TRIP (a display filter on the server's status) with two quiet `Button`s «Всё
   чистое» and «Всё в стирку» → `returnItems`. Nothing changes status without a tap.

Test (new file): calls with a fake fetch (method, path, body), `planIds` with an item in two slots, every shortage
type worded. Root `View` `testID="screen"`.

Screenshot: `cd client && npm run screenshot -- WR-148 --route '/#/trip'` writes
`docs/backlog/reports/screens/WR-148.png`; read it, check it against DESIGN.md, commit the PNG and name it in the PR.
The PR says «draft — вид проверяет Aika» and describes the look in words.

### WR-149 — «Нужно ли?»: check a thing before buying it (вид проверяет Aika)
- status: todo
- depends: WR-022, WR-028, WR-033, WR-035, WR-092, WR-102, WR-103, WR-126
- size: S
- paths: `client/src/screens/ShouldBuy.tsx`, `client/src/buy.ts`, `client/src/api/buy.ts`, `client/src/screens/routes.tsx`, `client/test/buy.test.mjs`, `docs/backlog/reports/screens/WR-149.png`
- verify: `make client/node_modules/.ci-stamp && cd client && npm test`
- expect: green; every reason code and answer of the contract has Russian text; the PR says «draft — вид проверяет Aika»

There is no mockup of this screen: build it only from DESIGN.md components with its tokens (no text field —
the candidate is described with `Chip`s). Route `buy` (wardrobe action «Нужно ли?»). RN core only. The verdict is the
server's (WR-092, `ShouldBuy`); the client only words it.

1. `client/src/api/buy.ts`: `checkBuy(baseUrl, candidate, fetchFn = fetch)` (`POST /buy/check`) through `request` of
   WR-102; `similar` and `pairs[].item` parsed with `parseItems`; a body the contract does not allow throws.
2. `client/src/buy.ts` (pure): `ANSWER_TITLE` (BUY «Брать», THINK «Подумать», SKIP «Не брать»); `reasonText(reason,
   answer)` → Russian per code from the answer's own data: NO_SIMILAR «Похожего у тебя нет», ALREADY_HAVE_SIMILAR
   «Похожее уже есть: {names of similar}», OPENS_PAIRS «Есть с чем носить: {countText(pairs.length)}»,
   OPENS_NO_PAIRS «Из чистого не с чем носить», FILLS_GAP «Закроет пробел: {type} для «{formality}»» (the n-th
   FILLS_GAP reason ↔ the n-th of `fills`), FILLS_NO_GAP «Пробелов не закрывает»; `candidateComplete(c)` — every
   field chosen (seasons at least one); UNKNOWN is never offered.
3. Screen: «‹» back; `ScreenTitle` «Нужно ли?»; `SectionTitle`s «Что это», «Цвет», «Узор», «Ткань», «Сезоны»,
   «Строгость» with `Chip` rows of the values (labels of WR-022, UNKNOWN left out; seasons multi-select); `Button`
   «Проверить» (active when `candidateComplete`) → a `PaperCard` with `Tape`: `SectionTitle` of the answer title,
   `WhyItFits` lines of the reasons if it accepts plain lines (read its props) else text lines, «Похожие» and «С чем
   носить» as rows of `ItemThumb`s. Nothing is stored; a 400 shows the server's message.

Test (new file): read the answer and reason-code enums from the `/buy/check` response schema in `../api/openapi.yaml`
(if the codes are not an enum there, use the six above) and check each has text; worded examples; the call with a
fake fetch. Root `View` `testID="screen"`.

Screenshot: `cd client && npm run screenshot -- WR-149 --route '/#/buy'` writes
`docs/backlog/reports/screens/WR-149.png`; read it, check it against DESIGN.md, commit the PNG and name it in the PR.
The PR says «draft — вид проверяет Aika» and describes the look in words.

### WR-150 — Measurements and fit notes beside the doll: MeasureRow, FitNote (вид проверяет Aika)
- status: todo
- depends: WR-021, WR-027, WR-028, WR-035, WR-105, WR-126
- size: S
- paths: `client/src/ui/MeasureRow.tsx`, `client/src/ui/FitNote.tsx`, `client/src/ui/fitNotes.ts`, `client/src/screens/Avatar.tsx`, `client/src/screens/Sheet.tsx`, `client/test/fit-notes.test.mjs`, `docs/backlog/reports/screens/WR-150.png`
- verify: `make client/node_modules/.ci-stamp && cd client && npm test`
- expect: green; every fit-note code of the contract has Russian text; centimetres only from `units.ts`; the PR says «draft — вид проверяет Aika»

Mockup «6 · Кукла для примерки»: beside the doll «Мерки» (Рост 168 см, Грудь 88 см …) and under it «Как сидит»
(«1 Рубашка: рукав короче на 2 см, в груди свободно.», «2 Брюки: по длине и в талии сидят.»).

1. `client/src/ui/MeasureRow.tsx` (DESIGN.md: a measurement name and value in centimetres, mm inside): props
   `{ name, mm }`; value = `mmToCmText(mm) + ' см'` of WR-105 (never divide here); `.measure` look: `sm`, the value
   extra bold, a dashed `rule` line between rows. 
2. `client/src/ui/FitNote.tsx` (DESIGN.md: one line about fit with a small numbered marker): props `{ n, garment,
   text, ok }`; an 18 px circle with `n` in `on-accent` — `mustard` when there is a problem, `sage` when the garment
   fits — then «{garment}: {text}». The doll's matching numbered markers are WR-027's; reuse its numbering.
3. `client/src/ui/fitNotes.ts` (pure): `noteText(note)` → Russian per code of the fit notes in the `POST
   /avatar/dress` answer (WR-021; read the codes and fields from `api/openapi.yaml`): TOO_TIGHT «тесно в {груди /
   талии / бёдрах}», TOO_LOOSE «свободно в …», SLEEVE_SHORT «рукав короче», SLEEVE_LONG «рукав длиннее», LEG_SHORT
   «брюки короче», LEG_LONG «брюки длиннее», plus `diffText` of WR-105 («на 2 см») when the note carries a
   difference; `garmentLine(notes)` → the notes joined by «, » with a capital letter and a full stop, or «сидит.»
   when there are none (marker `ok`); `MEASURE_ROWS` — the measurement names in order (Рост, Плечи, Грудь, Талия,
   Бёдра, Рука, Шаг) mapped to the request's fields.
4. `client/src/screens/Avatar.tsx` (WR-027): add the «Мерки» column of `MeasureRow`s for the invented figure and the
   «Как сидит» `PaperCard` of `FitNote`s, one per garment of the answer, as in the mockup. Keep the doll and its
   scaling as they are; WR-027's tests stay untouched. Append one sample of each component to `SHEET`.

Test (new file): every fit-note code from the contract has text; `garmentLine` with none, one and three notes;
`MEASURE_ROWS` covers every measurement field of the request schema.

Screenshot: `cd client && npm run screenshot -- WR-150 --route '/#/me'` writes
`docs/backlog/reports/screens/WR-150.png`; read it, compare with «6 · Кукла для примерки» in
`docs/design/mockups/mockups.html`, commit the PNG and name it in the PR. The PR says «draft — вид проверяет Aika» and
describes the look in words.

## Follow-ups decided on 26 November

### WR-151 — TextField (вид проверяет Aika)
- status: todo
- depends: WR-028, WR-035, WR-126, WR-129
- size: S
- paths: `client/src/ui/TextField.tsx`, `client/src/ui/textField.ts`, `client/src/screens/Sheet.tsx`, `client/test/text-field.test.mjs`, `docs/backlog/reports/screens/WR-151.png`
- verify: `make client/node_modules/.ci-stamp && cd client && npm test`
- expect: green; the length note and the colour-literal check are tested; the PR says «draft — вид проверяет Aika»

`TextField` (DESIGN.md component table, added 26 November: one-line input on `card`, radius `chip`, label above in
`ink-soft`, placeholder `ink-faint`, error below in `terracotta`; a multiline variant for pasting a saved model
answer; see `.field` in the component sheet of `docs/design/mockups/mockups.html`). Read both first; do not invent
anything the table and the sheet do not show.

`client/src/ui/TextField.tsx`: props `{ label, value, onChange(text), placeholder?, error?: string | null,
multiline?: boolean, maxLength?: number, editable?: boolean }`. React Native `TextInput` (core, no new dependency):
`card` fill, radius `chip`, shadow as the sheet shows, padding `sm`/`md`, text `md` `ink`, placeholder colour
`ink-faint`; the label `xs` bold `ink-soft` above; the error `xs` `terracotta` below (the error is a sentence given
by the screen — this component decides nothing); multiline: at least 6 lines tall, text aligned to the top, scrolls
inside. Tokens only.

`client/src/ui/textField.ts` (pure; erasable TypeScript; imports with `.ts`): `lengthNote(value, max)` → `null`
until the text reaches 90 % of `max`, then «9 500 / 10 000» (groups of three with U+202F: use `numberText` of
`client/src/ui/statTile.ts` (WR-129) for thousands grouping — do not write a second grouping); no `max` → `null`. The component
shows the note right-aligned under the field when not null.

`text-field.test.mjs` (new): `lengthNote` at 0, 89 %, 90 %, 100 %, no max; reads `TextField.tsx` as text and checks
it has no colour literal (`#…`, `rgb(`) and imports `tokens.ts`. Append to `SHEET` the two samples of the mockup
sheet: «Название» filled with «Голубые джинсы», and «Ответ модели» multiline with the placeholder «Вставь
сохранённый ответ…» and the error «Не вижу тип вещи — выбери его».

Screenshot: `cd client && npm run screenshot -- WR-151 --route '/#/sheet'` writes
`docs/backlog/reports/screens/WR-151.png`; read it, compare with `TextField` in `docs/design/mockups/mockups.html`,
commit the PNG and name it in the PR. The PR says «draft — вид проверяет Aika» and describes the look in words.

### WR-152 — Add an item from a pasted saved model answer (вид проверяет Aika)
- status: todo
- depends: WR-022, WR-028, WR-033, WR-035, WR-112, WR-123, WR-125, WR-126, WR-142, WR-151
- size: S
- paths: `client/src/screens/AddItem.tsx`, `client/src/addItem.ts`, `client/src/errors.ts`, `client/src/screens/routes.tsx`, `client/src/screens/Home.tsx`, `client/test/add-item.test.mjs`, `docs/backlog/reports/screens/WR-152.png`
- verify: `make client/node_modules/.ci-stamp && cd client && npm test`
- expect: green; the request body and the field errors are tested; nothing about completeness is decided in the client; the PR says «draft — вид проверяет Aika»

Version 1 has no model calls: the person pastes the text a model answered and confirms the card with corrections
(intent 01, invariant 5 — what the answer lacks stays unknown until the person chooses it). Route `add`. Built only
from DESIGN.md components with its tokens; RN core only; no new dependency.

Screen: «‹» back; `ScreenTitle` «Новая вещь»; a multiline `TextField` (WR-151) «Ответ модели» (`maxLength` 10 000,
as `POST /cards/parse` allows) and `Button` «Разобрать» → `parseCard` of WR-123. Then one block per row of
`draftRows` (WR-123), in its order: `SectionTitle` with the row's title; for `name` a `TextField` prefilled with the
draft's name; for the other keys a wrapping row of `Chip`s with the values of that enum (labels of WR-022; seasons
multi-select; the drafted value selected; `UNKNOWN` never offered as a choice — choosing nothing leaves it unknown);
the row's `doubt` sentence under it in `terracotta` while the person has not chosen. `Button` «Добавить» → `addItem`
of WR-125 with `addBody(result, chosen)`; 201 → `reset` to `{name:'item', param: newId}`. A 400 `INCOMPLETE_CARD`
marks the listed `fields` with the sentence of `client/src/errors.ts` (WR-112) for that code (add the `INCOMPLETE_CARD` entry if missing); other errors show the
server's message on top. The client never pre-checks completeness: the server's `ConfirmCard` decides (one rule in
one place) — «Добавить» is always active once something was parsed.

Entry point: on the home screen (WR-142) a quiet `Button` «Добавить вещь» under the «Недавно добавлены» row → `add`
(no second link in its `SectionTitle`; the central «+» keeps opening the builder — decided 26 November; write it in `DECISIONS.md`).

`client/src/addItem.ts` (pure): `addBody(result, chosen)` → `NewItem`: for each key the person's choice if any,
else the draft's value when it is known, else the key is left out (never `UNKNOWN` invented, never a guess);
`fieldErrors(apiError)` → `Record<key, sentence>` from `ApiError.fields`. Test (new file): a clean draft with no
choices, a garbled draft corrected, an empty draft with only a name; seasons order kept; `fieldErrors` with two
fields and with none.

Screenshot: `cd client && npm run screenshot -- WR-152 --route '/#/add'` writes
`docs/backlog/reports/screens/WR-152.png`; read it, check it against DESIGN.md and the `TextField` sample of the
mockups, commit the PNG and name it in the PR. The PR says «draft — вид проверяет Aika» and describes the look in
words (the empty form; describe the parsed state from running it by hand with an invented answer).

### WR-153 — Name a new collection: NewCard opens a TextField (вид проверяет Aika)
- status: todo
- depends: WR-028, WR-035, WR-112, WR-124, WR-126, WR-143, WR-151
- size: S
- paths: `client/src/screens/NewCollection.tsx`, `client/src/newCollection.ts`, `client/src/errors.ts`, `client/src/screens/Wardrobe.tsx`, `client/src/screens/routes.tsx`, `client/test/new-collection.test.mjs`, `docs/backlog/reports/screens/WR-153.png`
- verify: `make client/node_modules/.ci-stamp && cd client && npm test`
- expect: green; the trimmed name and the error mapping are tested; the PR says «draft — вид проверяет Aika»

The wardrobe screen (WR-143) shows `NewCard` «Новая подборка» without an action. Give it `onPress` → `go({name:
'collection-new'})` — the only change to `Wardrobe.tsx`.

Route `collection-new`: «‹» back; `ScreenTitle` «Новая подборка»; a `TextField` (WR-151) «Название» (placeholder
«Например, Офис»); `Button` «Создать» → `createCollection` of WR-124 with `trimmedName(text)`; success → `back()`
(the wardrobe reloads its collections when it is shown again — check how WR-143 loads them; if it loads only once,
reload on focus by re-running its load when the route becomes current, without changing its look). Errors: 409
`DUPLICATE_NAME` and 400 `BAD_REQUEST` are shown under the field as the sentence of `client/src/errors.ts` (WR-112; add `DUPLICATE_NAME` if missing);
the server decides what a valid or duplicate name is (`CollectionBook`, WR-107) — the client only trims.

`client/src/newCollection.ts` (pure): `trimmedName(text)` (outer spaces removed, inner runs of spaces collapsed;
nothing else); `nameError(apiError)` → a sentence for the field or `null` for errors that belong on top. Test (new
file): trimming cases; 409 and 400 map to the field, a network error does not; the create call with a fake fetch
sends `{name}`.

Screenshot: `cd client && npm run screenshot -- WR-153 --route '/#/collection-new'` writes
`docs/backlog/reports/screens/WR-153.png`; read it, check it against DESIGN.md, commit the PNG and name it in the PR.
The PR says «draft — вид проверяет Aika» and describes the look in words.

### WR-154 — Open a collection: its items, read-only (вид проверяет Aika)
- status: todo
- depends: WR-028, WR-033, WR-035, WR-103, WR-124, WR-126, WR-143, WR-153
- size: S
- paths: `client/src/screens/Collection.tsx`, `client/src/collectionScreen.ts`, `client/src/screens/Wardrobe.tsx`, `client/src/screens/routes.tsx`, `client/test/collection-screen.test.mjs`, `docs/backlog/reports/screens/WR-154.png`
- verify: `make client/node_modules/.ci-stamp && cd client && npm test`
- expect: green; the grid rows and the subtitle are tested; the PR says «draft — вид проверяет Aika»

`CollectionCard`s on the wardrobe screen (WR-143) go nowhere yet. Give them `onPress` → `go({name:'collection',
param: id})` — the only change to `Wardrobe.tsx` (WR-153 changed the `NewCard` line; keep it).

Route `collection` (param = collection id): «‹» back; `ScreenTitle` with the collection's name and the subtitle
`itemsCount` of WR-103 («17 вещей», from the server's items); a grid of three columns of round `ItemThumb`s (WR-033)
with `StatusDot` and the name under each (`xs` bold, one line, ellipsis); a thumb opens `{name:'item', param:id}`.
Data: `getCollection` of WR-124. An empty collection shows «Здесь пока пусто» in `ink-faint`; an unknown id (404
`UNKNOWN_COLLECTION`) shows the server's message and the back mark. Read-only: renaming and adding/removing items
(WR-120) are not part of this task. Built only from DESIGN.md components with its tokens; RN core only.

`client/src/collectionScreen.ts` (pure): `gridRows(items, columns)` → rows of at most `columns` items in the
server's order, the last row padded with `null`; `columns < 1` throws. Test (new file): 0, 1, 3, 4, 17 items.
Root `View` `testID="screen"` once loaded.

Screenshot: `cd client && npm run screenshot -- WR-154 --route '/#/collection/<id>'` (an id from `GET
/collections`, e.g. the seeded «Офис») writes `docs/backlog/reports/screens/WR-154.png`; read it, check it against
DESIGN.md, commit the PNG and name it in the PR. The PR says «draft — вид проверяет Aika» and describes the look in
words.

### WR-155 — «Хочу» opens «Нужно ли?» (вид проверяет Aika)
- status: todo
- depends: WR-028, WR-035, WR-143, WR-147, WR-148, WR-149, WR-153, WR-154
- size: S
- paths: `client/src/screens/Wardrobe.tsx`, `client/src/wardrobe.ts`, `client/test/wardrobe-actions.test.mjs`, `docs/backlog/reports/screens/WR-155.png`
- verify: `make client/node_modules/.ci-stamp && cd client && npm test`
- expect: green; every round action of the wardrobe opens a registered route; the PR says «draft — вид проверяет Aika»

Decided on 26 November: «Хочу» on the wardrobe screen has no wish list behind it; a wish is a thing one is thinking
of buying, so it opens the «Нужно ли?» screen (route `buy`, WR-149). Write the decision in `DECISIONS.md` (reversible).

In `client/src/wardrobe.ts` (WR-143) add `ROUND_ACTIONS` — the four actions in mockup order with their label, icon,
accent and route: «Носка» → `wear`, «Хочу» → `buy`, «Поездка» → `trip`, «Нужно ли?» → `buy` — and make
`Wardrobe.tsx` render the row from it (no other change; keep the `NewCard` and `CollectionCard` lines of WR-153/154
if present). `wardrobe-actions.test.mjs` (new; `client/test/wardrobe.test.mjs` stays untouched): four actions in
order, every one has a route, and each route name appears as a key in `client/src/screens/routes.tsx` (read that
file as text: it imports React components, which Node cannot load).

Screenshot: `cd client && npm run screenshot -- WR-155 --route '/#/wardrobe'` writes
`docs/backlog/reports/screens/WR-155.png`; read it, compare with «2 · Шкаф» in `docs/design/mockups/mockups.html`,
commit the PNG and name it in the PR. The PR says «draft — вид проверяет Aika» and describes the look in words.

### WR-156 — Choose what the doll tries on: CategoryTabs and the item strip (вид проверяет Aika)
- status: todo
- depends: WR-028, WR-035, WR-130, WR-139, WR-150
- size: S
- paths: `client/src/screens/Avatar.tsx`, `client/src/avatar/tryOn.ts`, `client/test/try-on.test.mjs`, `docs/backlog/reports/screens/WR-156.png`
- verify: `make client/node_modules/.ci-stamp && cd client && npm test`
- expect: green; choosing per tab is tested; impossible outfits are the server's answer; the PR says «draft — вид проверяет Aika»

Mockup «6 · Кукла для примерки»: under the fit notes, `CategoryTabs` «Верх · Низ · Платья · Верхняя». Let the person
choose what the doll wears.

`client/src/avatar/tryOn.ts` (pure): `TRY_TABS` — `TOP` «Верх», `BOTTOM` «Низ», `DRESS` «Платья», `OUTERWEAR`
«Верхняя» (the doll's own tabs, as in the mockup; the builder's `TABS` of WR-104 are a different grouping — do not
change them); `itemsForTab(items, key)` keeps the server's order; `choose(chosen, item)` → the new list of chosen ids:
at most one item per tab (choosing another item of the same tab replaces it, choosing the chosen one removes it),
order of tabs kept. Whether a dress goes with a top, or an outfit is possible at all, is the server's: `POST
/avatar/dress` answers 409 with the problems (WR-021) — show them, never pre-check them.

`client/src/screens/Avatar.tsx`: add `CategoryTabs` (WR-130) with `TRY_TABS` under «Как сидит», and an `ItemStrip`
(WR-139) of `itemsForTab(items of GET /items, tab)`; a tap calls `choose` and asks `POST /avatar/dress` again with the
same invented figure and the chosen items (reuse WR-027's call and its scaling; the doll, `MeasureRow`s and
`FitNote`s of WR-150 update from the new answer). Send sizes only for the items WR-027 invented sizes for; a chosen item without
a size gets the FitNote line «размер не указан» (not ok), never «сидит.». A 409 shows the server's problems in
`ink-soft` and keeps the last good drawing. WR-027's and WR-150's tests stay untouched.

Test (new file): each tab's items; replace within a tab; remove; two tabs chosen keep tab order; an unknown type is
in no tab; a chosen item without a size gets «размер не указан» (not ok), never «сидит.».

Screenshot: `cd client && npm run screenshot -- WR-156 --route '/#/me'` writes
`docs/backlog/reports/screens/WR-156.png`; read it, compare with «6 · Кукла для примерки» in
`docs/design/mockups/mockups.html`, commit the PNG and name it in the PR. The PR says «draft — вид проверяет Aika» and
describes the look in words.

### WR-157 — «У тебя уже есть N похожих» after adding an item
- status: todo
- depends: WR-068, WR-109, WR-121
- size: S
- paths: `api/openapi.yaml`, `domain/src/main/java/wardrobe/domain/buy/Candidate.java`, `domain/src/main/java/wardrobe/domain/buy/OwnedLike.java`, `domain/src/test/java/wardrobe/domain/buy/OwnedLikeTest.java`, `app/src/main/java/wardrobe/app/web/SimilarController.java`, `app/src/test/java/wardrobe/app/SimilarApiTest.java`
- verify: `./gradlew :domain:test --tests 'wardrobe.domain.buy.OwnedLikeTest' && ./gradlew :app:test --tests 'wardrobe.app.SimilarApiTest'`
- expect: green; the count comes from `Similar.to` (WR-068) only; the item itself is never counted

The problem behind the app: things are bought three times. What counts as «similar» is decided once, by fragment
B's `Similar.to` (WR-068: same type, same colour family, same formality) — reuse it, never a second rule.

Why not a field in the `POST /items` body: WR-121's `AddItemApiTest` validates the 201 body against `Item`
(`additionalProperties: false`), and existing tests are never changed. So the answer comes from a new read-only
endpoint the client calls right after adding (write this in `DECISIONS.md`).

1. Domain: `Candidate.of(Item)` (static, in `Candidate.java` of WR-068) → the card of an owned item; an item with an
   `UNKNOWN` colour, pattern or fabric → `Optional.empty()` (the candidate refuses unknown fields — we cannot vouch
   for it; never guess). `OwnedLike.of(ItemId id, List<Item> catalogue)` → `Optional<List<Item>>`: the item is looked
   up (absent → `IllegalArgumentException`), then `Similar.to(candidate, catalogue without that item)`; empty when
   the candidate is empty. `OwnedLikeTest` (new; reuse `BuyItems` of WR-068, add a new helper file if you need other
   items): the item itself never counted; two similar; none; UNKNOWN colour → empty; unknown id → IAE.
2. REST, contract first: `GET /items/{id}/similar` → `{ "known": boolean, "count": integer ≥ 0, "items": [Item] }`
   (`known` false and `count` 0 when the domain answered empty; `additionalProperties: false`); unknown id → 404
   `UNKNOWN_ITEM` (throw `UnknownItem`, WR-008's advice answers). New `SimilarController` calls `OwnedLike` with
   `catalog.all()` and maps items with `ItemView.of`; mind the route order next to `/items/{id}` (WR-109).
   `SimilarApiTest` (real HTTP, bodies validated against the contract) on the seed catalogue: work the expected
   count out from the seed and the rule of WR-068 and write the reasoning in a comment; 404.
The client part (the sentence on the add screen) is WR-158.

### WR-158 — The add screen says how many similar items you already have (вид проверяет Aika)
- status: todo
- depends: WR-157, WR-152, WR-035
- size: S
- paths: `client/src/api/similar.ts`, `client/src/screens/AddItem.tsx`, `client/test/similar.test.mjs`, `docs/backlog/reports/screens/WR-158.png`
- verify: `make client/node_modules/.ci-stamp && cd client && npm test`
- expect: green; the client words the server's number and decides nothing about similarity; the PR says «draft — вид проверяет Aika»

The server counts similar items you already own (`GET /items/{id}/similar`, WR-157; the rule is WR-068's
`Similar.to`) — the client only words the answer.
1. `client/src/api/similar.ts` — `getSimilar(baseUrl, id, fetchFn = fetch)` through `request` of WR-102, items
   parsed with `parseItems`; `similarText(answer)` → `null` when `known` is false or `count` is 0, else «У тебя уже
   есть N похожих вещей» with the plural of WR-103 («У тебя уже есть 1 похожая вещь», «… 2 похожие вещи», «… 5
   похожих вещей») — words only.
2. `client/src/screens/AddItem.tsx` (WR-152): after a successful add, call `getSimilar` for the new id and show the
   sentence in `ink-soft` (pass it to the item card's route as a one-time note, or show it on the add screen for a
   moment before moving on — choose the simplest and describe it in the PR). `known: false` → no sentence; a failed
   call shows nothing either and never undoes the add. WR-152's tests stay untouched.
`similar.test.mjs` (new): the call with a fake fetch; the texts for 0, 1, 2, 5, 11, 21 and `known: false`.
Screenshot: `cd client && npm run screenshot -- WR-158 --route '/#/add'` writes
`docs/backlog/reports/screens/WR-158.png`; read it, check it against DESIGN.md, commit the PNG and name it in the PR.
The PR says «draft — вид проверяет Aika» and describes the look in words (the sentence appears only after an add:
describe it from running the add by hand with an invented answer).

## Tech debt of 27 November

### WR-160 — The filter Chip shows a status dot, as on the wardrobe mockup (вид проверяет Aika)
- status: todo
- depends: WR-028, WR-035
- size: S
- paths: `client/src/ui/Chip.tsx`, `client/src/ui/chipStatus.ts`, `client/test/chip-status.test.mjs`, `client/src/filter.ts`, `client/App.tsx`, `client/src/screens/ItemList.tsx`, `docs/backlog/reports/screens/WR-160.png`
- verify: `make client/node_modules/.ci-stamp && cd client && npm test`
- expect: green; a status chip shows the status colour dot before its word, the «Все» chip has no dot; screenshot WR-160.png

Tech debt `docs/backlog/tech-debt/2026-11-27-WR-023-1.md`. The approved mockup (`docs/design/mockups/mockups.html`,
wardrobe screen, «Где вещи сейчас») shows each status chip with a small dot in the status colour before the word —
the status is word **and** colour (DESIGN.md rule). Decision (without Aika, 27 November): this is the mockup's look,
not a new one — add an optional `status` prop to `Chip` that draws `StatusDot` (reuse `client/src/ui/StatusDot.tsx`
and the status → colour table of `client/src/ui/status.ts`; never a second table) before the label. Pure logic
(which chips get which status) in `client/src/ui/chipStatus.ts`, tested in `chip-status.test.mjs`; pass the
status from the list screen's chip row (`client/App.tsx`, or `client/src/screens/ItemList.tsx` after WR-126).
Screenshot: `cd client && npm run screenshot -- WR-160`, compare with the mockup, name it in the PR.

### WR-161 — The item list screen built from the «уютный блокнот» components (вид проверяет Aika)
- status: todo
- depends: WR-126, WR-159, WR-160, WR-033
- size: S
- paths: `client/src/screens/ItemList.tsx`, `client/src/screens/itemListView.ts`, `client/test/item-list-view.test.mjs`, `docs/backlog/reports/screens/WR-161.png`
- verify: `make client/node_modules/.ci-stamp && cd client && npm test`
- expect: green; the screen uses only DESIGN.md components and tokens (no hex colour or pixel size in `ItemList.tsx`); screenshot WR-161.png

Tech debt `docs/backlog/tech-debt/2026-11-27-WR-023-2.md`: the list screen still has its own hex colours, sizes and
plain `Text`. Rebuild it from the components only: `ScreenTitle` («Мой шкаф» and the count line «N вещей · без
ограничений» — plural from WR-103 if merged, else a local helper in `itemListView.ts`), the chip row (WR-160),
`PaperCard` rows with `ItemThumb` (WR-033), the name and a `StatusBadge`. No hex colour, no pixel number in
`ItemList.tsx`: everything from `client/src/ui/tokens.ts`. Pure logic (row models, count line) in
`client/src/screens/itemListView.ts`, tested; the same test reads `client/src/screens/ItemList.tsx` as text and
asserts it has no hex colour (`#[0-9a-fA-F]{3,8}`) and no bare pixel number in a style (`:\s*\d+[,}]`). Keep the behaviour (load, error text, filter) unchanged — the
existing tests of `filter.ts` and `items.ts` stay green. Screenshot WR-161, compare with the wardrobe mockup, name it
in the PR.

## Added by intake

Appended by `scripts/backlog/backlog.py intake`: every proposal, tech-debt file or `tech-debt` issue that passes the filter (small, checked by a command, allowed paths).

### AUTO-20261126-1 — The client reaches the server from an Android emulator by default
- status: todo
- size: S
- paths: `client/src/apiUrl.ts`, `client/src/config.ts`, `client/test/api-url.test.mjs`
- verify: `make client/node_modules/.ci-stamp && cd client && node --test test/api-url.test.mjs && npx tsc --noEmit`
- expect: green; with no `EXPO_PUBLIC_API_URL`, Android gets `http://10.0.2.2:8080`, every other platform `http://localhost:8080`; a set `EXPO_PUBLIC_API_URL` always wins
- source: issue#41#1
- added: 2026-11-26

Why: `client/src/config.ts:2` — the default `http://localhost:8080` points an Android emulator at itself, so the app cannot load items there; found by the independent review of the skeleton, 2026-11-25

Add `client/src/apiUrl.ts` with a pure function `defaultApiUrl(os: string, envUrl: string | undefined): string`:
`envUrl` when it is a non-empty string; otherwise `http://10.0.2.2:8080` for `os === 'android'` (the emulator's
address of the host machine) and `http://localhost:8080` for anything else. No React Native import in this file, so
`node --test` can load it. `client/src/config.ts` keeps exporting `API_URL`, now
`defaultApiUrl(Platform.OS, process.env.EXPO_PUBLIC_API_URL)` (`Platform` from `react-native`). Test the three cases
and the empty-string case in `client/test/api-url.test.mjs`. The screenshot script (web) is not affected.

### AUTO-20261126-2 — Show ItemThumb, ClothingDrawing and PaperDoll on the component sheet
- status: todo
- size: S
- paths: `client/src/screens/Sheet.tsx`
- verify: `make client/node_modules/.ci-stamp && cd client && npm test`
- expect: green; `#/sheet` shows the three components under their names
- source: docs/backlog/tech-debt/2026-11-27-WR-033-1.md#1
- added: 2026-11-26

Why: `client/src/screens/Sheet.tsx:17` — "a component task appends one entry to SHEET", but WR-033's paths did not include `Sheet.tsx`, so its three new components appear on no screen yet; found by WR-033, executor, 2026-11-27

Append three entries to `SHEET`: `ItemThumb` (a few invented items of different types and statuses, one square),
`ClothingDrawing` (every kind of `GARMENT_KINDS` in a different item colour, size 56) and `PaperDoll` (bare and
dressed in WHITE / NAVY, height 260). The WR-033 PR's screenshot was taken with exactly these entries added locally.
