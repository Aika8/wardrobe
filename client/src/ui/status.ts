// Pure logic of the UI kit (no React): which token a status or a rule mark is drawn with.
// The server decides every wardrobe rule; this file only picks colours and words for its answer.
import { type ItemStatus, STATUS_LABEL } from '../items.ts';
import { type ColourToken, tokens } from './tokens.ts';

/** Strength of a status colour behind a `StatusBadge` («status colour at low strength», DESIGN.md). */
export const BADGE_FILL_ALPHA = 0.2;

/** The colour token of a status, as DESIGN.md's `status` block names it. */
export function statusToken(status: ItemStatus): ColourToken {
  return tokens.status[status];
}

/** The hex colour of a status. */
export function statusColour(status: ItemStatus): string {
  return tokens.colour[statusToken(status)];
}

/** The word shown next to a status colour: colour alone is never enough (DESIGN.md). */
export function statusWord(status: ItemStatus): string {
  return STATUS_LABEL[status];
}

/** `#RRGGBB` → `rgba(r,g,b,alpha)`. Throws on anything else, so a broken token fails loudly. */
export function withAlpha(hex: string, alpha: number): string {
  const m = /^#([0-9a-fA-F]{2})([0-9a-fA-F]{2})([0-9a-fA-F]{2})$/.exec(hex);
  if (!m || !(alpha >= 0 && alpha <= 1)) throw new Error(`withAlpha: bad colour '${hex}' or alpha ${alpha}`);
  const [r, g, b] = [m[1], m[2], m[3]].map((h) => parseInt(h ?? '', 16));
  return `rgba(${r},${g},${b},${alpha})`;
}

/** The fill behind a `StatusBadge`. */
export function statusFill(status: ItemStatus): string {
  return withAlpha(statusColour(status), BADGE_FILL_ALPHA);
}

/** The mark of one line of `WhyItFits`: ✓ in sage when the rule says yes, ✗ in terracotta when it says no. */
export function fitMark(fits: boolean): { mark: string; colour: string } {
  return fits ? { mark: '✓', colour: tokens.colour.sage } : { mark: '✗', colour: tokens.colour.terracotta };
}
