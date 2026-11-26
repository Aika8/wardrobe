// Tokens of «уютный блокнот»: the JSON block of docs/design/DESIGN.md as a typed constant (same names, same values).
// A token changes in DESIGN.md first; test/ui-tokens.test.mjs fails when this file differs from it.

export const tokens = {
  colour: {
    paper: "#F5EEE1",
    "paper-deep": "#EDE3D2",
    card: "#FCF8F1",
    rule: "#E3D7C3",
    ink: "#3F3630",
    "ink-soft": "#7B6F65",
    "ink-faint": "#A99D91",
    terracotta: "#C47A5C",
    sage: "#8DA48A",
    mustard: "#D7A955",
    "dusty-blue": "#7F98AA",
    plum: "#9A7B8C",
    tape: "#E7C98E",
    "on-accent": "#FFF8EE",
    "doll-skin": "#EBD9C4",
    "doll-hair": "#6E5A4C",
  },
  item: {
    BLACK: "#433A34",
    WHITE: "#F7F2E8",
    GREY: "#B8B0A6",
    NAVY: "#4E5B73",
    BEIGE: "#E2CFB2",
    BROWN: "#8B6A4E",
    RED: "#B9614F",
    PINK: "#D9A3A0",
    ORANGE: "#D98B4E",
    YELLOW: "#D7A955",
    GREEN: "#8DA48A",
    BLUE: "#6F87A3",
    PURPLE: "#9A7B8C",
    UNKNOWN: "#D9CFC0",
  },
  status: {
    CLEAN: "sage",
    IN_WASH: "dusty-blue",
    IN_REPAIR: "mustard",
    ON_TRIP: "terracotta",
  },
  font: {
    heading: "Caveat, handwriting; 600; headings and section titles only",
    body: "Nunito, rounded sans; 400 / 600 / 700",
    size: {
      xs: 11,
      sm: 13,
      md: 15,
      lg: 18,
      section: 24,
      title: 28,
      hero: 34,
    },
  },
  space: {
    xxs: 2,
    xs: 4,
    sm: 8,
    md: 12,
    lg: 16,
    xl: 24,
    xxl: 32,
  },
  radius: {
    chip: 12,
    card: 18,
    sheet: 24,
    pill: 999,
  },
  shadow: {
    card: "0 1px 0 rgba(63,54,48,.06), 0 6px 16px rgba(63,54,48,.07)",
  },
  line: {
    hair: 1,
    dashed: "1.5 dashed rule",
  },
} as const;

export type Tokens = typeof tokens;
export type ColourToken = keyof Tokens['colour'];
export type ItemColour = keyof Tokens['item'];
export type SpaceToken = keyof Tokens['space'];
