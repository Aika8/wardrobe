// Plain Russian words for every value the API sends in an Item (pure data, no React; tested with `node --test`).
// One table per enum of `Item` in `api/openapi.yaml`; statuses reuse `STATUS_LABEL` of `items.ts`.
// `client/test/labels.test.mjs` fails when the contract gains a value without a word here.
import { STATUS_LABEL } from './items.ts';

export type ItemType = 'TOP' | 'BOTTOM' | 'DRESS' | 'OUTERWEAR' | 'SHOES' | 'ACCESSORY';
export type Colour =
  | 'BLACK' | 'WHITE' | 'GREY' | 'NAVY' | 'BEIGE' | 'BROWN' | 'RED' | 'PINK' | 'ORANGE' | 'YELLOW'
  | 'GREEN' | 'BLUE' | 'PURPLE' | 'UNKNOWN';
export type Pattern = 'PLAIN' | 'STRIPED' | 'CHECKED' | 'FLORAL' | 'DOTTED' | 'PRINTED' | 'UNKNOWN';
export type Fabric = 'COTTON' | 'LINEN' | 'WOOL' | 'SILK' | 'DENIM' | 'LEATHER' | 'KNIT' | 'SYNTHETIC' | 'UNKNOWN';
export type Season = 'WINTER' | 'SPRING' | 'SUMMER' | 'AUTUMN';
export type Formality = 'SPORT' | 'CASUAL' | 'SMART_CASUAL' | 'BUSINESS' | 'FORMAL';

export const TYPE_LABEL: Record<ItemType, string> = {
  TOP: 'верх',
  BOTTOM: 'низ',
  DRESS: 'платье',
  OUTERWEAR: 'верхняя одежда',
  SHOES: 'обувь',
  ACCESSORY: 'аксессуар',
};

export const COLOUR_LABEL: Record<Colour, string> = {
  BLACK: 'чёрный',
  WHITE: 'белый',
  GREY: 'серый',
  NAVY: 'тёмно-синий',
  BEIGE: 'бежевый',
  BROWN: 'коричневый',
  RED: 'красный',
  PINK: 'розовый',
  ORANGE: 'оранжевый',
  YELLOW: 'жёлтый',
  GREEN: 'зелёный',
  BLUE: 'синий',
  PURPLE: 'фиолетовый',

  UNKNOWN: 'цвет не указан',
};

export const PATTERN_LABEL: Record<Pattern, string> = {
  PLAIN: 'однотонный',
  STRIPED: 'в полоску',
  CHECKED: 'в клетку',
  FLORAL: 'в цветочек',
  DOTTED: 'в горошек',
  PRINTED: 'с принтом',
  UNKNOWN: 'узор не указан',
};

export const FABRIC_LABEL: Record<Fabric, string> = {
  COTTON: 'хлопок',
  LINEN: 'лён',
  WOOL: 'шерсть',
  SILK: 'шёлк',
  DENIM: 'джинса',
  LEATHER: 'кожа',
  KNIT: 'трикотаж',
  SYNTHETIC: 'синтетика',
  UNKNOWN: 'ткань не указана',
};

export const SEASON_LABEL: Record<Season, string> = {
  WINTER: 'зима',
  SPRING: 'весна',
  SUMMER: 'лето',
  AUTUMN: 'осень',
};

export const FORMALITY_LABEL: Record<Formality, string> = {
  SPORT: 'спортивный',
  CASUAL: 'повседневный',
  SMART_CASUAL: 'смарт-кэжуал',
  BUSINESS: 'деловой',
  FORMAL: 'торжественный',
};

/** A short explanation for a formality word that is not plain on its own; the others need none. */
export const FORMALITY_HINT: Partial<Record<Formality, string>> = {
  SMART_CASUAL: 'аккуратно, но не строго: между повседневным и деловым',
};

/** Every table, keyed by the name of the `Item` property whose values it labels. */
export const LABELS: Readonly<Record<string, Readonly<Record<string, string>>>> = {
  type: TYPE_LABEL,
  colour: COLOUR_LABEL,
  pattern: PATTERN_LABEL,
  fabric: FABRIC_LABEL,
  seasons: SEASON_LABEL,
  formality: FORMALITY_LABEL,
  status: STATUS_LABEL,
};

/** The word for a value of an `Item` property; a value the table lacks is shown as it came, never guessed. */
export function label(property: string, value: string): string {
  return LABELS[property]?.[value] || value;
}
