// The drawn line icons of the approved mockups (docs/design/mockups/mockups.html) as data: 24 × 24, stroke only.
// Copied, never redrawn: test/icons.test.mjs fails when a path here differs from the same icon in the mockups.

/** Path data `d` of each icon, by name. */
export const ICONS = {
  outfit: "M4 20 L14 10 M14 4 v3 M19 9 h-3 M17.5 5.5 l-2 2 M10 5 l1 2 M18 13 l-2 -1",
  pairs: "M8 12 a4 4 0 1 1 0 .1 M16 12 a4 4 0 1 1 0 .1",
  suitcase: "M5 8 h14 v11 H5 Z M9 8 V5 h6 v3 M5 13 h14",
  wash: "M5 3 h14 v18 H5 Z M5 7 h14 M12 14 m-4 0 a4 4 0 1 0 8 0 a4 4 0 1 0 -8 0",
  home: "M4 11 L12 4 L20 11 V20 H4 Z",
  wardrobe: "M12 6 a2 2 0 1 1 2 2 L12 9.5 L3 17 H21 L12 9.5",
  plus: "M12 5 V19 M5 12 H19",
  calendar: "M4 6 h16 v14 H4 Z M4 10 h16 M8 3 v4 M16 3 v4",
  me: "M12 4 a3 3 0 1 1 0 .1 M6 21 q0 -8 6 -8 q6 0 6 8",
  wear: "M5 20 V12 M10 20 V6 M15 20 V10 M20 20 V14",
  want: "M12 20 C4 14 4 7 8.5 6 C10.5 5.6 11.5 7 12 8 C12.5 7 13.5 5.6 15.5 6 C20 7 20 14 12 20 Z",
  trip: "M5 8 h14 v11 H5 Z M9 8 V5 h6 v3",
  shouldBuy: "M11 4 a7 7 0 1 0 9 9 M14 4 l1 3 3 1 -3 1 -1 3 -1 -3 -3 -1 3 -1 Z",
  clean: "M5 13 l4 4 L19 7",
  repair: "M6 18 L16 8 M14 6 l4 4 M4 20 l2 -2",
} as const;

export type IconName = keyof typeof ICONS;

/** The stroke style of every icon in the mockups (`svg.ic`). */
export const ICON_STROKE_WIDTH = 1.8;

function escapeAttr(value: string): string {
  return value.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}

/** The complete SVG markup of an icon in the given colour, for `SvgXml` of `react-native-svg`. */
export function iconXml(name: IconName, colour: string): string {
  if (!Object.hasOwn(ICONS, name)) throw new Error(`unknown icon: ${String(name)}`);
  const d = ICONS[name];
  return (
    `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="${escapeAttr(colour)}"` +
    ` stroke-width="${ICON_STROKE_WIDTH}" stroke-linecap="round" stroke-linejoin="round">` +
    `<path d="${escapeAttr(d)}"/></svg>`
  );
}
