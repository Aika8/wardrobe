# Item cards from a model answer

Version 1 makes no model calls (intent 01). A vision model **would** look at a photo of an item and answer with a
card; here we fix the format we will ask for and keep saved, invented answers as text fixtures in
`domain/src/test/resources/cards/`. The parser tasks (WR-012, WR-013, WR-014) test against them.

## The format we ask the model for

One `key: value` per line, these seven keys, in this order:

| Key | Value | Domain type |
|---|---|---|
| `type` | one of `top, bottom, dress, outerwear, shoes, accessory` | `ItemType` |
| `colour` | one of `black, white, grey, navy, beige, brown, red, pink, orange, yellow, green, blue, purple` | `Colour` |
| `pattern` | one of `plain, striped, checked, floral, dotted, printed` | `Pattern` |
| `fabric` | one of `cotton, linen, wool, silk, denim, leather, knit, synthetic` | `Fabric` |
| `seasons` | one or more of `winter, spring, summer, autumn`, comma-separated | `Season` |
| `formality` | one of `sport, casual, smart_casual, business, formal` | `Formality` |
| `name` | a short free-text name of the item | text |

Values are the English enum words; keys and values are case-insensitive and whitespace around them is trimmed.
The name is free text and is kept as written (trimmed), in any language.

## What real answers look like around it

A model does not always answer in the format. The fixtures cover what we expect to meet:

- the clean format, with any letter case and extra spaces;
- the card wrapped in a Markdown code fence, with a sentence before and after it;
- values in Russian («синий», «хлопок», «зима, весна») instead of the English words;
- a key left out (no `fabric` line);
- a garbled value (`colour: blueish ?`) — not a word of the list in either language;
- one key given twice with different values;
- keys we did not ask for (`brand: …`, `size: …`, `confidence: …`) — ignored;
- an empty answer.

A value the answer lacks, garbles, or gives twice differently stays **unknown** — never guessed and never "the first
one wins" (intent 01, invariant 5).

## The `.expected` files

Each fixture `NN-short-name.txt` has a sibling `NN-short-name.expected`: the card a correct parser produces. Exactly
seven lines, one `key: value` per key, in the order of the table above, keys in lower case:

- `colour`, `pattern`, `fabric`: the enum word in upper case, or `UNKNOWN`;
- `type`, `formality`: the enum word in upper case (`SMART_CASUAL`), or `?` when unknown (these enums have no
  `UNKNOWN`);
- `seasons`: the enum words in upper case in enum order (`WINTER,SPRING,SUMMER,AUTUMN`), joined by a comma with no
  space; `?` when unknown;
- `name`: the name as written, trimmed; `?` when unknown. A name may be the word "Unknown" if the model wrote that.

Files are UTF-8, end with a newline; the empty answer is an empty file.

## The fixtures

These files are append-only once merged: add new ones, never change these.

| Fixture | What it shows |
|---|---|
| `01-clean-wool-coat.txt` | clean answer, lower case |
| `02-clean-striped-top.txt` | clean answer, keys and values capitalised |
| `03-clean-linen-trousers.txt` | clean answer, extra spaces around keys and values, no space after a colon, seasons out of order |
| `04-fenced-silk-dress.txt` | card inside a Markdown code fence, a sentence before and after |
| `05-russian-cotton-shirt.txt` | Russian values for colour, fabric and seasons; a Russian name |
| `06-missing-fabric-sneakers.txt` | no `fabric` line → `fabric: UNKNOWN` |
| `07-garbled-colour-scarf.txt` | `colour: blueish ?` → `colour: UNKNOWN` |
| `08-doubled-colour-skirt.txt` | `colour` twice (red, then pink) → `colour: UNKNOWN` |
| `09-extra-keys-knit-sweater.txt` | extra keys `brand` (an invented word), `size`, `confidence` — ignored |
| `10-empty-answer.txt` | empty answer → every field unknown |
