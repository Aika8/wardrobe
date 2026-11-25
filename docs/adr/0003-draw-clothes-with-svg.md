# 0003. Draw clothes and the doll with react-native-svg

- Status: Accepted (Aika, 25 November 2026; implemented by WR-033)
- Date: 2026-11-25
- Pattern(s): none

## Context
Aika approved the «уютный блокнот» theme with one change: clothes and the avatar must look real, not boxy
(`docs/design/DESIGN.md`, «Clothes and the doll»). Real-looking garments are curves, shading and seams; the doll's
clothes follow the body. Plain React Native `View`s draw only boxes and rounded boxes. Version 1 has no photos.

## Decision
Add `react-native-svg` to the Expo client (installed with `npx expo install react-native-svg`, the version Expo
pins). The garment shapes of `docs/design/mockups/garments.py` become path data in the client
(`client/src/ui/garments.data.ts`, generated from that script, never hand-drawn twice) and one component
`ClothingDrawing` renders them in an item's colour; `PaperDoll` renders the doll the same way and places the
garments by the landmarks the server computes (`POST /avatar/dress`). Works on iOS, Android and the Expo web build.

## Why this and not the alternatives
| Alternative | Why not (now) |
|---|---|
| Plain `View`s (rectangles, trapezoids) | Exactly the boxy look Aika rejected |
| PNG drawings per garment and colour | 10 garments × 14 colours of files; no recolouring; blurry when scaled |
| Skia (`@shopify/react-native-skia`) | Heavier, more than flat drawings need; a dev build for some features |
| Photos of real items | Version 1 has none; photos are personal data and stay on the phone |

## Pros
- One source of shapes for mockups and app; any colour from the palette; sharp at every size.
- An Expo-supported library, no native build step for Expo Go.

## Risks and costs
- A new runtime dependency (size, upgrades with each Expo SDK). Noticed by `npm ci` and the type check in CI.
- Path data must stay in sync with `garments.py`: the client test compares the generated file with the script's
  output, so a drawing changes in one place.

## How to explain it (teaching note)
"Clothes are vector drawings from one script: the same shapes in the design mockups and in the app, recoloured per
item. SVG was the smallest dependency that draws curves and shading on every platform Expo targets."
