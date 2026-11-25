# Design: «уютный блокнот» (cozy notebook)

Status: **approved by Aika on 25 November 2026** (mockups: `docs/design/mockups/mockups.html`, the «cut-out photo»
look of items and the drawn doll included). Screen tasks follow it; the avatar itself waits for her decision (doll or
photo try-on), and ADR-0003 (`react-native-svg`) for her «да».

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
names in `client/src/ui/tokens.ts` (WR-028). A value changes here first.

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
    "tape": "#E7C98E",
    "on-accent": "#FFF8EE",
    "doll-skin": "#EBD9C4",
    "doll-hair": "#6E5A4C"
  },
  "item": {
    "BLACK": "#433A34",
    "WHITE": "#F7F2E8",
    "GREY": "#B8B0A6",
    "NAVY": "#4E5B73",
    "BEIGE": "#E2CFB2",
    "BROWN": "#8B6A4E",
    "RED": "#B9614F",
    "PINK": "#D9A3A0",
    "ORANGE": "#D98B4E",
    "YELLOW": "#D7A955",
    "GREEN": "#8DA48A",
    "BLUE": "#6F87A3",
    "PURPLE": "#9A7B8C",
    "UNKNOWN": "#D9CFC0"
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
    "size": { "xs": 11, "sm": 13, "md": 15, "lg": 18, "section": 24, "title": 28, "hero": 34 }
  },
  "space": { "xxs": 2, "xs": 4, "sm": 8, "md": 12, "lg": 16, "xl": 24, "xxl": 32 },
  "radius": { "chip": 12, "card": 18, "sheet": 24, "pill": 999 },
  "shadow": { "card": "0 1px 0 rgba(63,54,48,.06), 0 6px 16px rgba(63,54,48,.07)" },
  "line": { "hair": 1, "dashed": "1.5 dashed rule" }
}
```

Rules of use:
- Background is always `paper`; content sits on `card`. Never pure white, never black: text is `ink`.
- Handwriting (`heading`) only for screen titles and section titles, never for buttons, labels or numbers.
- A status is always shown with its colour **and** its word (colour alone is not enough).
- Accent colours are for items and statuses; at most one accent per control. Text on an accent fill is `on-accent`.
- A drawn item takes its fill from `item` by the item's `Colour` (the API's enum); `UNKNOWN` is a neutral grey-beige.
- Every size, gap, radius and colour in a screen is a token. Positions and sizes of drawings on a canvas are not.
- The mockup page's own frame (phone bezel, page background, captions, the component sheet's grid) is not part of the
  app and uses no tokens.
- Clothes and the doll look real, not like icons: see «Clothes and the doll» below. Photos never leave the phone.

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
| `PaperDoll` | faceless figure with natural proportions from measurements; clothes drawn over it in layers, following the body | avatar |
| `FitNote` | one line about fit: «рукав короче на 2 см» with a small marker on the doll | avatar |
| `MeasureRow` | a measurement name and value in centimetres for the person (mm inside) | avatar |

## Clothes and the doll

Aika's decision (25 November 2026): clothes and the avatar must look real, not boxy.

- **Items look like cut-out photos** (Aika, 25 November, after the reference app): the item without its
  background on a light backing, in a flat lay; an outfit is a collage of such cut-outs. The theme stays for the
  background, cards and text. Real photos come once background removal is decided (queue WR-034; Aika decides).
  Until then the mockups imitate cut-out photos with drawings (`render_photo`: no outline, light from the top left,
  fabric grain, a soft contact shadow) — never pictures of the reference app or of real items.
- **The drawn garments** (the shapes behind both looks) are fashion flats — the real cut of each garment: collar and placket with buttons, cuffs, yokes,
  waistband with belt loops and fly, pockets and flaps, darts, ribbing, soles and laces, and soft folds. Fill: the
  item's colour from `item`; outline `ink` 1.5; shadow `ink` at 10–30 %; light `card` at 20–40 %; seams and folds
  `ink` lines at 20–60 %; top-stitching dashed. One drawing per garment kind, in a 100 × 100 box.
- **The doll** (a placeholder until Aika decides on the avatar; logic of fit and layers is what it is for) is a faceless figure with natural proportions (head, neck, rounded shoulders, arms with elbows and
  hands, waist and hips, knees, feet) in `doll-skin` with `doll-hair`. Clothes are drawn over it in layer order and
  follow the body (sleeves along the arms, trousers along the legs), with the same shading as the flats. Fit notes
  are numbered markers on the doll.
- The drawings are made by `docs/design/mockups/garments.py` (the source of the shapes); it rewrites the drawings in
  `mockups.html`; `--json` exports the flat drawings, the bare doll and its landmarks for the app. In the app an
  item without a photo shows the flat drawing (`render`); on the phone it needs SVG — ADR-0003 (Proposed).
- Never a photo of a real item or person as a placeholder; never a garment that looks like a box.

## Screens (mockups)

`docs/design/mockups/mockups.html`, phone width 390 px: home, wardrobe, outfit builder, calendar, item card (our
statuses), avatar doll (draft), and the component sheet. Open the file in a browser.

## Open points for Aika

- Fonts on the phone need `expo-font` with the two Google fonts — a new dependency (ADR before the screen tasks).
- Real-looking clothes and the doll on the phone need `react-native-svg` — ADR-0003, Proposed, waits for Aika.
- Currency on the calendar's «цена за носку» is shown in ₸; prices are not in the item model yet.
