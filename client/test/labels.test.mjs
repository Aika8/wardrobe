import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { FORMALITY_HINT, LABELS, label } from '../src/labels.ts';
import { STATUS_LABEL } from '../src/items.ts';

/**
 * The `enum: [...]` lists inside `components.schemas.Item.properties` of an OpenAPI text, by property name.
 * The Item block runs from the line `    Item:` to the next line with the same four-space indentation, or to the
 * end of `components.schemas` (a non-empty line indented less). An enum nested under a property (`items:` of
 * `seasons`) belongs to that property.
 */
function itemEnums(yaml) {
  const lines = yaml.split('\n');
  const start = lines.indexOf('    Item:');
  assert.notEqual(start, -1, 'the contract has a schema `Item`');
  let end = lines.length;
  for (let i = start + 1; i < lines.length; i++) {
    const indent = /^ */.exec(lines[i])[0].length;
    if (lines[i].trim() !== '' && indent <= 4) { end = i; break; }
  }
  const block = lines.slice(start + 1, end);
  const propsAt = block.findIndex((l) => /^      properties:\s*$/.test(l));
  assert.notEqual(propsAt, -1, '`Item` has `properties`');
  const enums = {};
  let property = null;
  for (const line of block.slice(propsAt + 1)) {
    const prop = /^        ([A-Za-z_][\w]*):/.exec(line);
    if (prop) property = prop[1];
    if (/^ {0,7}\S/.test(line)) property = null; // left `properties`
    const m = /\benum:\s*\[([^\]]*)\]/.exec(line);
    if (m) {
      assert.ok(property, `an enum outside a property: ${line.trim()}`);
      enums[property] = m[1].split(',').map((v) => v.trim().replace(/^['"]|['"]$/g, '')).filter(Boolean);
    } else {
      assert.ok(!/\benum:/.test(line), `an enum not written as [A, B, …] cannot be read here: ${line.trim()}`);
    }
  }
  return enums;
}

const contract = readFileSync(new URL('../../api/openapi.yaml', import.meta.url), 'utf8');

test('the contract has the enums of Item this file labels', () => {
  assert.deepEqual(Object.keys(itemEnums(contract)).sort(),
    ['colour', 'fabric', 'formality', 'pattern', 'seasons', 'status', 'type']);
});

test('every enum value of Item in api/openapi.yaml has a non-empty Russian label', () => {
  for (const [property, values] of Object.entries(itemEnums(contract))) {
    const table = LABELS[property];
    assert.ok(table, `no label table for Item.${property}`);
    for (const v of values) {
      assert.equal(typeof table[v], 'string', `Item.${property} = ${v} has no label`);
      assert.ok(table[v].trim() !== '', `Item.${property} = ${v} has an empty label`);
    }
  }
});

test('statuses use the one table of items.ts', () => {
  assert.equal(LABELS.status, STATUS_LABEL);
});

test('the reader stops at the next schema: its enums are ignored', () => {
  const fixture = [
    'components:',
    '  schemas:',
    '    Item:',
    '      type: object',
    '      properties:',
    '        id: { type: string }',
    '        type: { type: string, enum: [TOP, SHOES] }',
    '        seasons:',
    '          type: array',
    '          items: { type: string, enum: [WINTER] }',
    '        colour:',
    '          type: string',
    '          enum: [BLACK, UNKNOWN]',
    '    Error:',
    '      type: object',
    '      properties:',
    '        code: { type: string, enum: [UNKNOWN_ITEM, BAD_REQUEST] }',
    '',
  ].join('\n');
  assert.deepEqual(itemEnums(fixture), { type: ['TOP', 'SHOES'], seasons: ['WINTER'], colour: ['BLACK', 'UNKNOWN'] });
});

test('the reader stops at the end of components.schemas', () => {
  const fixture = [
    'components:',
    '  schemas:',
    '    Item:',
    '      properties:',
    '        pattern: { type: string, enum: [PLAIN] }',
    '  parameters:',
    '    Kind:',
    '      schema: { type: string, enum: [OTHER] }',
  ].join('\n');
  assert.deepEqual(itemEnums(fixture), { pattern: ['PLAIN'] });
});

test('a missing label shows as it came, never a guess', () => {
  assert.equal(label('colour', 'NAVY'), 'тёмно-синий');
  assert.equal(label('colour', 'TEAL'), 'TEAL');
  assert.equal(label('nothing', 'X'), 'X');
});

test('the smart-casual word comes with a hint', () => {
  assert.ok(FORMALITY_HINT.SMART_CASUAL);
});
