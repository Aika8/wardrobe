import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { RULE_TITLE, WHY_TEMPLATE, whyText } from '../src/why.ts';

/**
 * One `enum: [...]` (possibly over several lines) of a property of `components.schemas.WhyLine` in an OpenAPI text.
 * Only the `WhyLine` block is read: from the line `    WhyLine:` to the next line indented four spaces or less.
 */
function whyLineEnum(yaml, property) {
  const lines = yaml.split('\n');
  const start = lines.indexOf('    WhyLine:');
  assert.notEqual(start, -1, 'the contract has a schema `WhyLine`');
  let end = lines.length;
  for (let i = start + 1; i < lines.length; i++) {
    if (lines[i].trim() !== '' && /^ */.exec(lines[i])[0].length <= 4) { end = i; break; }
  }
  const block = lines.slice(start + 1, end).join('\n');
  const at = block.search(new RegExp(`^        ${property}:`, 'm'));
  assert.notEqual(at, -1, `\`WhyLine\` has a property \`${property}\``);
  const m = /enum:\s*\[([^\]]*)\]/.exec(block.slice(at));
  assert.ok(m, `\`WhyLine.${property}\` has an enum written as [A, B, …]`);
  return m[1].split(',').map((v) => v.trim().replace(/^['"]|['"]$/g, '')).filter(Boolean);
}

const contract = readFileSync(new URL('../../api/openapi.yaml', import.meta.url), 'utf8');

const item = (id, name, over = {}) => ({
  id, name, type: 'TOP', colour: 'BLACK', pattern: 'PLAIN', fabric: 'COTTON', seasons: ['WINTER'],
  formality: 'CASUAL', status: 'CLEAN', next: [], ...over,
});
const byId = (...items) => new Map(items.map((i) => [i.id, i]));

test('every WhyLine.code of api/openapi.yaml has a Russian sentence', () => {
  const codes = whyLineEnum(contract, 'code');
  assert.ok(codes.length > 0);
  const items = byId(item('a', 'Пиджак'), item('b', 'Джинсы'));
  for (const code of codes) {
    assert.equal(typeof WHY_TEMPLATE[code], 'function', `no template for ${code}`);
    const { text } = whyText({ rule: 'COLOUR', goes: true, code, items: ['a', 'b'], reason: 'r' }, items);
    assert.ok(text.trim().length > 0, `empty sentence for ${code}`);
    assert.match(text, /[а-яё]/i, `the sentence for ${code} is not Russian`);
  }
});

test('every WhyLine.rule of api/openapi.yaml has a Russian title', () => {
  for (const rule of whyLineEnum(contract, 'rule')) {
    assert.ok(RULE_TITLE[rule]?.trim(), `no title for ${rule}`);
  }
});

test('STEPS_APART names both items and their formality, marked ✗', () => {
  const items = byId(item('j', 'Пиджак', { formality: 'BUSINESS' }), item('s', 'Худи', { formality: 'SPORT' }));
  const w = whyText({ rule: 'FORMALITY', goes: false, code: 'STEPS_APART', items: ['j', 's'], reason: 'x' }, items);
  assert.deepEqual(w, {
    mark: '✗',
    title: 'Строгость',
    text: 'Пиджак — деловой стиль, Худи — спортивный: слишком далеко по строгости',
  });
});

test('NOT_FRIENDS words the colours of the pair', () => {
  const items = byId(item('r', 'Юбка', { colour: 'RED' }), item('o', 'Блузка', { colour: 'ORANGE' }));
  const w = whyText({ rule: 'COLOUR', goes: false, code: 'NOT_FRIENDS', items: ['r', 'o'], reason: 'x' }, items);
  assert.deepEqual(w, {
    mark: '✗',
    title: 'Цвет',
    text: 'Юбка (красный) и Блузка (оранжевый) — цвета не дружат',
  });
});

test('NOT_CLEAN lists each item with its status; ✓ ALL_CLEAN needs no names', () => {
  const items = byId(item('s', 'Свитер', { status: 'IN_WASH' }));
  assert.deepEqual(
    whyText({ rule: 'CLEAN', goes: false, code: 'NOT_CLEAN', items: ['s'], reason: 'x' }, items),
    { mark: '✗', title: 'Чистота', text: 'Не готово: Свитер — в стирке' },
  );
  assert.equal(
    whyText({ rule: 'CLEAN', goes: true, code: 'ALL_CLEAN', items: ['s'], reason: 'x' }, items).text,
    'Все вещи чистые',
  );
});

test('the mark is the server\'s goes, whatever the code', () => {
  const items = byId(item('a', 'А'), item('b', 'Б'));
  assert.equal(whyText({ rule: 'COLOUR', goes: false, code: 'FRIENDS', items: ['a', 'b'], reason: 'x' }, items).mark, '✗');
  assert.equal(whyText({ rule: 'COLOUR', goes: true, code: 'NOT_FRIENDS', items: ['a', 'b'], reason: 'x' }, items).mark, '✓');
});

test('a ✓ line over more than two items says the other pairs pass too', () => {
  const items = byId(item('a', 'А'), item('b', 'Б'), item('c', 'В'));
  const w = whyText({ rule: 'SEASON', goes: true, code: 'SHARED_SEASON', items: ['a', 'b', 'c'], reason: 'x' }, items);
  assert.equal(w.text, 'А и Б носят в одно время года. Остальные пары тоже подходят');
});

test('an unknown item stays its id; an unknown code shows the server reason, never a guess', () => {
  const w = whyText({ rule: 'NEW_RULE', goes: false, code: 'NEW_CODE', items: ['x1', 'x2'], reason: 'too shiny' }, new Map());
  assert.deepEqual(w, { mark: '✗', title: 'NEW_RULE', text: 'Пояснения по-русски нет; сервер пишет: too shiny' });
  const u = whyText({ rule: 'COLOUR', goes: false, code: 'NOT_FRIENDS', items: ['x1', 'x2'], reason: 'x' }, new Map());
  assert.equal(u.text, 'x1 (не известно) и x2 (не известно) — цвета не дружат');
});
