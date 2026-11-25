# Design: «уютный блокнот» (cozy notebook)

Status: **draft — waits for Aika's «да»** (mockups: `docs/design/mockups/mockups.html`). Until then every task that
changes how a screen looks is `waiting` in `docs/backlog/QUEUE.md`; logic tasks go on.

## Where it comes from

- **Structure of the screens** follows a wardrobe app Aika chose as a reference: a home with "outfit of the day"
  and helper tiles, a wardrobe of collections, an outfit builder (canvas above, item strip below), an outfit
  calendar, a bottom bar with a central «+». Only the arrangement is borrowed: no logo, pictures, texts, colours or
  exact look of that app, and no screenshots of it in this repository. Its name is not used in code or copy.
- **Theme** is ours: a warm paper notebook — cream paper, soft rounded cards with a strip of washi tape, calm muted
  colours, handwriting only in headings. The reference is cold white with black accents; we use neither.
- **What we add** (our difference, see the queue): every suggestion says *why it fits* by explicit rules; every item
  shows its status (clean / in the wash / in repair / on a trip); no limit on the number of items.

## Tokens

The single source of the theme. Mockups use them as CSS variables (`--paper` …); the Expo client will get the same
names in `client/src/ui/tokens.ts` (a task after Aika's «да»). A value changes here first.

```json
{
  "colour": {
    "paper": "#F5EEE1",
    "paper-deep": "#EDE3D2",
    "card": "#FCF8F1",
    "rule": "#E3D7C3",
    "ink": "#3F3630",
    "ink-soft": "#7B6F65",
    "ink-faint": "#A99D91",
    "terracotta": "#C47A5C",
    "sage": "#8DA48A",
    "mustard": "#D7A955",
    "dusty-blue": "#7F98AA",
    "plum": "#9A7B8C",
    "tape": "#E7C98E"
  },
  "status": {
    "CLEAN": "sage",
    "IN_WASH": "dusty-blue",
    "IN_REPAIR": "mustard",
    "ON_TRIP": "terracotta"
  },
  "font": {
    "heading": "Caveat, handwriting; 600; headings and section titles only",
    "body": "Nunito, rounded sans; 400 / 600 / 700",
    "size": { "xs": 11, "sm": 13, "md": 15, "lg": 18, "title": 28, "hero": 34 }
  },
  "space": { "xs": 4, "sm": 8, "md": 12, "lg": 16, "xl": 24, "xxl": 32 },
  "radius": { "chip": 12, "card": 18, "sheet": 24, "pill": 999 },
  "shadow": { "card": "0 1px 0 rgba(63,54,48,.06), 0 6px 16px rgba(63,54,48,.07)" },
  "line": { "hair": 1, "dashed": "1.5 dashed rule" }
}
```

Rules of use:
- Background is always `paper`; content sits on `card`. Never pure white, never black: text is `ink`.
- Handwriting (`heading`) only for screen titles and section titles, never for buttons, labels or numbers.
- A status is always shown with its colour **and** its word (colour alone is not enough).
- Accent colours are for items and statuses; at most one accent per control.
- Clothes are flat drawings in item colours with an `ink` outline until photos exist; photos never leave the phone.

## Components

Screens are built **only** from these. An agent never invents a new component or a new look of an existing one:
if a screen needs something missing, the task is parked with a proposal under "For Aika" (what, why, which screen),
and Aika decides. Each component in the mockups has a `data-component="…"` attribute with its name.

| Component | What it is | Used on |
|---|---|---|
| `ScreenTitle` | handwritten title, optional subtitle in body font | all |
| `WeatherStrip` | date · city · temperature · a word for the weather | home, builder, calendar |
| `PaperCard` | card on paper: `card` colour, radius `card`, shadow, optional `Tape` | all |
| `Tape` | a washi-tape strip on a card's top edge, slightly turned | cards that hold an outfit |
| `SectionTitle` | handwritten section title with an optional link on the right | all |
| `HelperTile` | square tile: drawn icon, two-line label, optional counter | home |
| `RoundAction` | round button with a drawn icon and a word under it | wardrobe |
| `ItemThumb` | an item drawing on a small card (round or square), optional `StatusDot` | everywhere |
| `StatusDot` | small dot in the status colour, with the word next to it where there is room | thumbs, lists |
| `StatusBadge` | pill: status colour at low strength + status word | item card, lists |
| `StatusSwitch` | four-way control of an item's status; only the moves the server allows are active | item card |
| `Chip` | filter or attribute pill; selected = ink outline | wardrobe, item card |
| `CollectionCard` | 2×2 collage of `ItemThumb`s, «+N», name, count | wardrobe |
| `NewCard` | dashed card with «+» and a label | wardrobe |
| `OutfitCanvas` | ruled paper sheet where items lie as a flat lay | home, builder |
| `WhyItFits` | list of rules with ✓ / ✗ and a one-line reason each | home, builder, item card |
| `CategoryTabs` | text tabs with an underline in `terracotta` | builder |
| `ItemStrip` | horizontal row of `ItemThumb`s; items not clean are faded with their status word | builder |
| `CalendarMonth` | month grid; a day holds the thumb of that day's outfit | calendar |
| `StatTile` | number + label on a `PaperCard` | calendar, wardrobe |
| `Button` | primary (`terracotta` fill) and quiet (ink outline) | all |
| `BottomNav` | five places; the middle one is a raised round «+» | all main screens |
| `PaperDoll` | flat faceless figure from measurements; clothes as layers with paper-doll tabs | avatar |
| `FitNote` | one line about fit: «рукав короче на 2 см» with a small marker on the doll | avatar |
| `MeasureRow` | a measurement name and value in centimetres for the person (mm inside) | avatar |

## Screens (mockups)

`docs/design/mockups/mockups.html`, phone width 390 px: home, wardrobe, outfit builder, calendar, item card (our
statuses), avatar doll (draft), and the component sheet. Open the file in a browser.

## Open points for Aika

- Fonts on the phone need `expo-font` with the two Google fonts — a new dependency (ADR before the screen tasks).
- Currency on the calendar's «цена за носку» is shown in ₸; prices are not in the item model yet.
