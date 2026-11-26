# Intent: a wardrobe that knows what is clean and what goes together
Author: Aika. Status: draft (version 1).

## Problem
Every morning the same question: what to wear today, from what is clean and goes together. Clothes are forgotten
in the back of the cupboard, things are bought that are already there three times, and the one shirt that fits the
meeting is in the wash. A list of things with a status and a few honest rules about what combines would already
answer most mornings.

## Proposed outcome (version 1, no model calls)
- A **card per item**: type, colour, pattern, fabric, seasons, formality. Later a vision model fills the card from a
  photo and Aika confirms it; in version 1 cards come from code and from **saved** model answers (text fixtures).
- **Statuses**: clean, in the wash, in repair, on a trip. Only clean items take part in outfits.
- **Combination rules** in code, with tests: by formality and by colour. A rule says yes or no and why.
- **REST** `GET /items` and an **Expo screen** with the list of items.
- **Avatar draft**: a flat figure built from proportions (measurements), clothes drawn in layers on it, the fit
  logic (does this item fit these measurements, where its edges fall) under tests. No photos, no 3D.

The look of the screens and of the avatar is checked by Aika by eye after 8 December 2026; cloud agents build it
as a draft and mark it so. The theme is «уютный блокнот» (`docs/design/DESIGN.md`); screens are built only from its
components.

What sets it apart (from the reviews of a popular wardrobe app, where people complain that outfits look random,
recognition is slow and wrong, and only 100 items are free):
- every suggested pair or outfit says **why it fits**, rule by rule, and why not when it does not;
- every item has a **status** (clean, in the wash, in repair, on a trip) and outfits use only clean items;
- **no limit** on the number of items.

## Affected users and systems
Aika (one user). Server: Java 25 + Spring Boot. Client: Expo. No other systems.

## Constraints
- Same stack as the other projects: domain in plain Java (no frameworks), Spring Boot only in the adapter module.
- No model calls in version 1. When they come, the model key lives only on the server (never in the client).
- Aika's own photos are personal data: they stay on her machine, never in the repository, never in the cloud.
- No real brands anywhere (names, logos, fixtures).

## Out of scope (version 1)
Photos and background removal (researched in WR-034; Aika decides), model calls, a weather service (weather is
passed in by the person or the client as plain values), choosing what to buy for the person (the «стоит ли
покупать» check of an item she describes herself is in scope), try-on on a photo, 3D avatar, accounts and sync,
persistence beyond memory.

In scope since 26 November 2026 (Aika's list for the week): the wear log and statistics, statuses over time, trip
packing, the «стоит ли покупать» check, outfit suggestions by occasion and by weather given as values, and the
screens of the approved mockups.

## Open questions (for Aika, not for agents)
- A set of 20–30 photos of real items as a test set, or an open dataset — decided when model calls start.
- Colour model: a fixed palette of named colours (version 1) or measured colours (HSL) later.
- Which proportions the avatar takes (height, chest, waist, hips, inseam … ) and in which units on the screen.

## Invariants
1. Every item has exactly one status; an item is offered for an outfit only when it is clean.
2. Status changes follow the allowed transitions (`ItemStatus`); any other change is rejected, not ignored.
3. A combination rule is symmetric: `a goes with b` ⇔ `b goes with a`, and an item always goes with itself (the
   same item, by id; two different items with an unknown field the rule judges do not go together).
4. Formality is an ordered scale; two items whose formality differs by more than one step never combine.
5. A card parsed from a model answer is a draft until confirmed; a field the answer lacks or garbles stays
   unknown — it is never guessed.
6. Measurements are integers in millimetres; the avatar never uses floating-point for sizes it compares.
7. No personal photo, face or real measurement of Aika is committed; fixtures use invented figures.
8. A verdict on a pair or an outfit always comes with its reasons, one per rule; there is no verdict without one.
9. There is no limit on the number of items.
