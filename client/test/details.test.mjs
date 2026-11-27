import { test } from 'node:test';
import assert from 'node:assert/strict';
import { describeItem, SEASON_ORDER } from '../src/details.ts';
import { COLOUR_LABEL, FABRIC_LABEL, FORMALITY_HINT, FORMALITY_LABEL, PATTERN_LABEL, TYPE_LABEL } from '../src/labels.ts';

const item = (over = {}) => ({
  id: 'jeans-1', name: 'Голубые джинсы', type: 'BOTTOM', colour: 'BLUE', pattern: 'PLAIN', fabric: 'DENIM',
  seasons: ['SPRING', 'AUTUMN'], formality: 'CASUAL', status: 'IN_WASH', ...over,
});

const value = (rows, l) => rows.find((r) => r.label === l)?.value;

test('describeItem gives the rows in a fixed order, in Russian', () => {
  assert.deepEqual(describeItem(item()), [
    { label: 'Тип', value: 'низ' },
    { label: 'Цвет', value: 'синий' },
    { label: 'Узор', value: 'однотонный' },
    { label: 'Ткань', value: 'джинса' },
    { label: 'Сезоны', value: 'весна, осень' },
    { label: 'Стиль', value: 'повседневный' },
  ]);
});

test('every type, colour, pattern, fabric and formality gets its word from labels.ts', () => {
  for (const [k, v] of Object.entries(TYPE_LABEL)) assert.equal(value(describeItem(item({ type: k })), 'Тип'), v);
  for (const [k, v] of Object.entries(COLOUR_LABEL)) assert.equal(value(describeItem(item({ colour: k })), 'Цвет'), v);
  for (const [k, v] of Object.entries(PATTERN_LABEL)) assert.equal(value(describeItem(item({ pattern: k })), 'Узор'), v);
  for (const [k, v] of Object.entries(FABRIC_LABEL)) assert.equal(value(describeItem(item({ fabric: k })), 'Ткань'), v);
  for (const [k, v] of Object.entries(FORMALITY_LABEL)) {
    assert.equal(value(describeItem(item({ formality: k })), 'Стиль'), v);
  }
});

test('seasons come in calendar order, whatever order the server sent', () => {
  assert.deepEqual(SEASON_ORDER, ['WINTER', 'SPRING', 'SUMMER', 'AUTUMN']);
  assert.equal(value(describeItem(item({ seasons: ['AUTUMN', 'SUMMER', 'WINTER', 'SPRING'] })), 'Сезоны'),
    'зима, весна, лето, осень');
  assert.equal(value(describeItem(item({ seasons: ['SUMMER'] })), 'Сезоны'), 'лето');
});

test('a formality that needs it carries a hint; the others carry none', () => {
  const smart = describeItem(item({ formality: 'SMART_CASUAL' })).find((r) => r.label === 'Стиль');
  assert.equal(smart.hint, FORMALITY_HINT.SMART_CASUAL);
  for (const r of describeItem(item({ formality: 'BUSINESS' }))) assert.equal('hint' in r, false);
});

test('a value the tables lack is shown as it came, never guessed', () => {
  const rows = describeItem(item({ colour: 'TEAL', seasons: ['WINTER', 'MONSOON'], formality: 'GALA' }));
  assert.equal(value(rows, 'Цвет'), 'TEAL');
  assert.equal(value(rows, 'Сезоны'), 'зима, MONSOON');
  assert.equal(value(rows, 'Стиль'), 'GALA');
});

test('id, name and status are not rows (title and badge show the last two); the item is not changed', () => {
  const it = item();
  const before = JSON.stringify(it);
  const text = JSON.stringify(describeItem(it));
  for (const s of ['jeans-1', 'Голубые джинсы', 'IN_WASH', 'в стирке']) assert.ok(!text.includes(s), s);
  assert.equal(JSON.stringify(it), before);
});
