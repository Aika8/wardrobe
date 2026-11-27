import { test } from 'node:test';
import assert from 'node:assert/strict';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { GARMENTS_DATA } from '../src/ui/garments.data.ts';
import {
  DEFAULT_GARMENT,
  DOLL_BOX,
  DOLL_LANDMARKS,
  GARMENT_KINDS,
  dollXml,
  garmentKind,
  garmentXml,
  itemFill,
} from '../src/ui/garments.ts';
import { TYPE_LABEL } from '../src/labels.ts';
import { tokens } from '../src/ui/tokens.ts';

function scriptOutput() {
  const script = fileURLToPath(new URL('../../docs/design/mockups/garments.py', import.meta.url));
  return JSON.parse(execFileSync('python3', [script, '--json'], { encoding: 'utf8' }));
}

test('garments.data.ts holds exactly what garments.py --json prints', () => {
  assert.deepEqual(GARMENTS_DATA, scriptOutput());
});

test('every item type has a default garment, and every default is a drawing that exists', () => {
  assert.deepEqual(Object.keys(DEFAULT_GARMENT).sort(), Object.keys(TYPE_LABEL).sort());
  assert.deepEqual(DEFAULT_GARMENT, {
    TOP: 'shirt',
    BOTTOM: 'trousers',
    DRESS: 'dress',
    OUTERWEAR: 'coat',
    SHOES: 'shoes',
    ACCESSORY: 'bag',
  });
  for (const kind of Object.values(DEFAULT_GARMENT)) assert.ok(GARMENT_KINDS.includes(kind), kind);
});

test('garmentKind: by type; a type the contract does not know gets no drawing', () => {
  assert.equal(garmentKind('BOTTOM'), 'trousers');
  assert.equal(garmentKind('HAT'), null);
  assert.equal(garmentKind('toString'), null);
});

test('itemFill: the item token by Colour; unknown values draw as UNKNOWN', () => {
  assert.equal(itemFill('NAVY'), tokens.item.NAVY);
  assert.equal(itemFill('UNKNOWN'), tokens.item.UNKNOWN);
  assert.equal(itemFill('TEAL'), tokens.item.UNKNOWN);
  assert.equal(itemFill('constructor'), tokens.item.UNKNOWN);
});

test('garmentXml: every drawing filled, no placeholder left, a complete svg in the 100 × 100 box', () => {
  for (const kind of GARMENT_KINDS) {
    const xml = garmentXml(kind, '#4E5B73');
    assert.match(xml, /^<svg xmlns="http:\/\/www\.w3\.org\/2000\/svg" viewBox="0 0 100 100">[\s\S]*<\/svg>$/);
    assert.ok(!xml.includes('{'), `${kind}: placeholder left`);
    assert.ok(xml.includes('#4E5B73'), `${kind}: fill missing`);
  }
});

test('garmentXml and dollXml accept only a #RRGGBB fill', () => {
  assert.throws(() => garmentXml('shirt', 'red'), /#RRGGBB/);
  assert.throws(() => garmentXml('shirt', '#fff" onload="x'), /#RRGGBB/);
  assert.throws(() => garmentXml('hat', '#FFFFFF'), /unknown garment/);
  assert.throws(() => dollXml({ shirt: '#FFFFFF', trousers: 'blue' }), /#RRGGBB/);
});

test('dollXml: bare without clothes, dressed in both fills with them', () => {
  const bare = dollXml();
  assert.equal(bare, `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 178 360">${GARMENTS_DATA.doll_bare}</svg>`);
  const dressed = dollXml({ shirt: '#F7F2E8', trousers: '#4E5B73' });
  assert.ok(!dressed.includes('{'));
  assert.ok(dressed.includes('#F7F2E8') && dressed.includes('#4E5B73'));
});

test('the doll box and landmarks come from the script', () => {
  assert.deepEqual(DOLL_BOX, { width: 178, height: 360 });
  assert.equal(DOLL_LANDMARKS, GARMENTS_DATA.doll_landmarks);
});
