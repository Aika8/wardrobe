import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { tokens } from '../src/ui/tokens.ts';
import { BADGE_FILL_ALPHA, fitMark, statusColour, statusFill, statusToken, statusWord, withAlpha } from '../src/ui/status.ts';

const STATUSES = ['CLEAN', 'IN_WASH', 'IN_REPAIR', 'ON_TRIP'];

function designTokens() {
  const md = readFileSync(new URL('../../docs/design/DESIGN.md', import.meta.url), 'utf8');
  const m = /```json\n([\s\S]*?)```/.exec(md);
  assert.ok(m, 'DESIGN.md has a ```json block');
  return JSON.parse(m[1]);
}

test('tokens.ts is the JSON block of DESIGN.md: same names, same values', () => {
  assert.deepEqual(tokens, designTokens());
});

test('every status names an existing colour token', () => {
  for (const s of STATUSES) assert.ok(tokens.colour[statusToken(s)], s);
});

test('status → colour follows the status block', () => {
  assert.equal(statusColour('CLEAN'), tokens.colour.sage);
  assert.equal(statusColour('IN_WASH'), tokens.colour['dusty-blue']);
  assert.equal(statusColour('IN_REPAIR'), tokens.colour.mustard);
  assert.equal(statusColour('ON_TRIP'), tokens.colour.terracotta);
});

test('a status always has a word as well as a colour', () => {
  for (const s of STATUSES) assert.ok(statusWord(s).length > 0, s);
  assert.equal(statusWord('IN_WASH'), 'в стирке');
});

test('withAlpha turns a hex token into rgba and refuses anything else', () => {
  assert.equal(withAlpha('#8DA48A', 0.2), 'rgba(141,164,138,0.2)');
  assert.equal(withAlpha('#ffffff', 1), 'rgba(255,255,255,1)');
  assert.throws(() => withAlpha('sage', 0.2), /bad colour/);
  assert.throws(() => withAlpha('#8DA48A', 1.5), /bad colour/);
});

test('the badge fill is the status colour at low strength', () => {
  assert.equal(statusFill('CLEAN'), `rgba(141,164,138,${BADGE_FILL_ALPHA})`);
  assert.ok(BADGE_FILL_ALPHA > 0 && BADGE_FILL_ALPHA < 0.5);
});

test('fitMark: ✓ in sage for yes, ✗ in terracotta for no', () => {
  assert.deepEqual(fitMark(true), { mark: '✓', colour: tokens.colour.sage });
  assert.deepEqual(fitMark(false), { mark: '✗', colour: tokens.colour.terracotta });
});
