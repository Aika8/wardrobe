// Plain Russian for one line of the server's `OutfitVerdict.why` (pure data, no React; tested with `node --test`).
// The server decides whether things go together (one rule in one place); this file only words its answer:
// the mark is the line's `goes`, the sentence comes from one template per `WhyLine.code` of `api/openapi.yaml`.
// It never compares items itself (no steps counted, no seasons intersected). `client/test/why.test.mjs` fails
// when the contract gains a code without a template here.
import { type Item, STATUS_LABEL } from './items.ts';
import { COLOUR_LABEL, FORMALITY_LABEL, PATTERN_LABEL, SEASON_LABEL } from './labels.ts';

export type WhyRule = 'FORMALITY' | 'COLOUR' | 'PATTERN' | 'SEASON' | 'CLEAN';
export type WhyCode =
  | 'SAME_ITEM' | 'CLOSE_ENOUGH' | 'STEPS_APART' | 'NEUTRAL' | 'SAME_COLOUR' | 'FRIENDS' | 'NOT_FRIENDS'
  | 'UNKNOWN_COLOUR' | 'ONE_PATTERN' | 'TWO_PATTERNS' | 'SHARED_SEASON' | 'NO_SHARED_SEASON' | 'ALL_CLEAN'
  | 'NOT_CLEAN' | 'NO_PAIRS' | 'UNCODED';

/** One line of `OutfitVerdict.why` as the contract's `WhyLine` sends it. */
export type WhyLine = {
  rule: string;
  goes: boolean;
  code: string;
  items: string[];
  reason: string;
};

export type WhyText = { mark: '✓' | '✗'; title: string; text: string };

export const RULE_TITLE: Record<WhyRule, string> = {
  FORMALITY: 'Строгость',
  COLOUR: 'Цвет',
  PATTERN: 'Узор',
  SEASON: 'Сезон',
  CLEAN: 'Чистота',
};

/** What a template gets: the line, and its items as the client knows them (an unknown id stays the id). */
type Ctx = {
  line: WhyLine;
  /** Names of the line's items, in the line's order. */
  names: string[];
  /** The line's items the client knows, by position; `undefined` where it does not. */
  items: (Item | undefined)[];
};

function word(table: Readonly<Record<string, string>>, value: string | undefined): string {
  if (value === undefined) return 'не известно';
  return table[value] || value;
}

const a = (c: Ctx) => c.names[0] ?? '—';
const b = (c: Ctx) => c.names[1] ?? '—';
const colour = (c: Ctx, i: number) => word(COLOUR_LABEL, c.items[i]?.colour);
const pattern = (c: Ctx, i: number) => word(PATTERN_LABEL, c.items[i]?.pattern);
const formality = (c: Ctx, i: number) => word(FORMALITY_LABEL, c.items[i]?.formality);
const seasons = (c: Ctx, i: number) => {
  const s = c.items[i]?.seasons;
  return s && s.length > 0 ? s.map((v) => word(SEASON_LABEL, v)).join(', ') : 'сезон не известен';
};

/**
 * A ✓ line is about every item and its code is the first pair's (`Explain`, WR-029): the sentence names that pair,
 * and with more items says the other pairs pass too — which the ✓ itself already means.
 */
function pairSentence(c: Ctx, sentence: string): string {
  return c.line.goes && c.names.length > 2 ? `${sentence}. Остальные пары тоже подходят` : sentence;
}

/** One template per `WhyLine.code` of the contract. */
export const WHY_TEMPLATE: Record<WhyCode, (c: Ctx) => string> = {
  SAME_ITEM: () => 'Одна и та же вещь — с собой она сочетается',
  CLOSE_ENOUGH: (c) => pairSentence(c, `${a(c)} и ${b(c)} близки по строгости`),
  STEPS_APART: (c) =>
    `${a(c)} — ${formality(c, 0)} стиль, ${b(c)} — ${formality(c, 1)}: слишком далеко по строгости`,
  NEUTRAL: (c) => pairSentence(c, `${a(c)} и ${b(c)}: нейтральный цвет подходит к любому`),
  SAME_COLOUR: (c) => pairSentence(c, `${a(c)} и ${b(c)} одного цвета — ${colour(c, 0)}`),
  FRIENDS: (c) => pairSentence(c, `${a(c)} (${colour(c, 0)}) и ${b(c)} (${colour(c, 1)}) — цвета дружат`),
  NOT_FRIENDS: (c) => `${a(c)} (${colour(c, 0)}) и ${b(c)} (${colour(c, 1)}) — цвета не дружат`,
  UNKNOWN_COLOUR: (c) => `${a(c)} и ${b(c)}: у одной из вещей не указан цвет, поручиться нельзя`,
  ONE_PATTERN: (c) => pairSentence(c, `${a(c)} и ${b(c)}: узор не больше чем у одной вещи`),
  TWO_PATTERNS: (c) => `${a(c)} (${pattern(c, 0)}) и ${b(c)} (${pattern(c, 1)}) — два узора спорят`,
  SHARED_SEASON: (c) => pairSentence(c, `${a(c)} и ${b(c)} носят в одно время года`),
  NO_SHARED_SEASON: (c) => `${a(c)} (${seasons(c, 0)}) и ${b(c)} (${seasons(c, 1)}) — нет общего сезона`,
  ALL_CLEAN: () => 'Все вещи чистые',
  NOT_CLEAN: (c) =>
    'Не готово: ' +
    c.names.map((n, i) => `${n} — ${word(STATUS_LABEL, c.items[i]?.status)}`).join(', '),
  NO_PAIRS: () => 'Меньше двух вещей — сравнивать нечего',
  UNCODED: (c) => `Пояснения по-русски нет; сервер пишет: ${c.line.reason}`,
};

/**
 * The Russian wording of one `WhyLine`. The mark is the server's `goes`; nothing is decided here. A code this client
 * does not know (a newer server) is worded like `UNCODED`: the server's own reason, never a guess.
 */
export function whyText(line: WhyLine, itemsById: ReadonlyMap<string, Item>): WhyText {
  const items = line.items.map((id) => itemsById.get(id));
  const names = line.items.map((id, i) => items[i]?.name ?? id);
  const template = WHY_TEMPLATE[line.code as WhyCode] ?? WHY_TEMPLATE.UNCODED;
  return {
    mark: line.goes ? '✓' : '✗',
    title: RULE_TITLE[line.rule as WhyRule] ?? line.rule,
    text: template({ line, names, items }),
  };
}
