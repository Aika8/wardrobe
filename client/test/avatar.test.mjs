import { test } from 'node:test';
import assert from 'node:assert/strict';
import { DOLL_LANDMARKS } from '../src/ui/garments.ts';
import { GARMENTS_DATA } from '../src/ui/garments.data.ts';
import {
  TEMPLATE_CLOTHES,
  TEMPLATE_HALF_WIDTH,
  avatarXml,
  bands,
  drawnAs,
  markers,
  place,
  warpXml,
} from '../src/avatar/fit.ts';
import { dress, parseDressed } from '../src/avatar/api.ts';
import { fitRows, noteText, outfitRequest, INVENTED_FIGURE } from '../src/avatar/outfit.ts';

const FIGURE = {
  landmarks: { head: 0, neck: 230, shoulders: 300, chest: 420, waist: 560, hips: 700, crotch: 830, knee: 1150, ankle: 1600, floor: 1680 },
  halfWidths: { shoulders: 190, chest: 140, waist: 112, hips: 153 },
};
const WIDE = { ...FIGURE, halfWidths: { ...FIGURE.halfWidths, waist: 170 } };
const LINES = [
  ['head', 'head_top'], ['neck', 'chin'], ['shoulders', 'shoulders'], ['chest', 'chest'], ['waist', 'waist'],
  ['hips', 'hips'], ['crotch', 'crotch'], ['knee', 'knee'], ['ankle', 'ankle'], ['floor', 'floor'],
];

const answer = (layers, figure = FIGURE) => ({ figure, layers });
const shape = { top: 300, bottom: 700, topHalfWidth: 150, bottomHalfWidth: 160, sleeveEnd: 880, clamped: false };

test('every template landmark lands exactly on the figure’s landmark, in whole pixels', () => {
  const b = bands(answer([]), 360);
  for (const [f, t] of LINES) {
    const p = place({ x: DOLL_LANDMARKS.centre_x, y: DOLL_LANDMARKS[t] }, b);
    assert.deepEqual(p, { x: b.centre, y: b.y[f] }, f);
    assert.ok(Number.isInteger(b.y[f]), f);
  }
  // mm → px: the figure's own numbers, not the template's
  assert.equal(b.y.waist, b.mm(560));
  assert.equal(b.y.floor - b.y.head, Math.round(339.5 * 360 / 360) - Math.round(10.5 * 360 / 360));
});

test('the template body edge at a width line lands on the figure’s half-width there', () => {
  const b = bands(answer([]), 360);
  for (const w of ['shoulders', 'chest', 'waist', 'hips']) {
    const p = place({ x: DOLL_LANDMARKS.centre_x - TEMPLATE_HALF_WIDTH[w], y: DOLL_LANDMARKS[w] }, b);
    assert.equal(b.centre - p.x, b.halfWidth[w], w);
  }
});

test('the template half-widths are read off the outline (the waist is an anchor of the body path)', () => {
  assert.ok(Math.abs(TEMPLATE_HALF_WIDTH.waist - (89 - 70.8)) < 1e-9);
  for (const w of ['shoulders', 'chest', 'hips']) assert.ok(TEMPLATE_HALF_WIDTH[w] > TEMPLATE_HALF_WIDTH.waist, w);
});

test('a wider waist widens the drawn waist, and nothing above the shoulders', () => {
  const narrow = bands(answer([]), 360);
  const wide = bands(answer([], WIDE), 360);
  const waistEdge = { x: 70, y: DOLL_LANDMARKS.waist };
  assert.ok(place(waistEdge, wide).x < place(waistEdge, narrow).x);
  const ear = { x: 74, y: 31 };
  assert.deepEqual(place(ear, wide), place(ear, narrow));
});

test('points between two landmarks stay between them; order is kept', () => {
  const b = bands(answer([]), 540);
  for (let k = 1; k < LINES.length; k++) {
    const [fa, ta] = LINES[k - 1];
    const [fb, tb] = LINES[k];
    let prev = -Infinity;
    for (let s = 0; s <= 10; s++) {
      const y = DOLL_LANDMARKS[ta] + ((DOLL_LANDMARKS[tb] - DOLL_LANDMARKS[ta]) * s) / 10;
      const p = place({ x: 60, y }, b);
      assert.ok(p.y >= b.y[fa] && p.y <= b.y[fb], `${fa}–${fb} at ${y}`);
      assert.ok(p.y >= prev);
      prev = p.y;
      assert.ok(Number.isInteger(p.x) && Number.isInteger(p.y));
    }
  }
  // left of the centre stays left, and further left stays further left
  const at = (x) => place({ x, y: 130 }, b).x;
  assert.ok(at(40) < at(60) && at(60) < at(89) && at(89) < at(120));
});

test('warpXml moves every number of paths, circles and ellipses to whole pixels', () => {
  const b = bands(answer([]), 360);
  const out = warpXml(GARMENTS_DATA.doll, b);
  for (const d of out.match(/ d="([^"]*)"/g)) assert.match(d, /^ d="[MLCZ0-9 -]*"$/);
  for (const a of out.match(/ (cx|cy|r|rx|ry)="[^"]*"/g)) assert.match(a, /="-?\d+"$/);
  assert.throws(() => warpXml('<path d="m1 2 h3"/>', b), /not supported/);
});

test('the template clothes split into the trousers and the shirt', () => {
  assert.ok(TEMPLATE_CLOTHES.trousers.includes('{trousers}') && !TEMPLATE_CLOTHES.trousers.includes('{shirt}'));
  assert.ok(TEMPLATE_CLOTHES.shirt.includes('{shirt}') && !TEMPLATE_CLOTHES.shirt.includes('{trousers}'));
  assert.equal(GARMENTS_DATA.doll_bare + TEMPLATE_CLOTHES.trousers + TEMPLATE_CLOTHES.shirt, GARMENTS_DATA.doll);
  assert.equal(drawnAs('TOP'), 'shirt');
  assert.equal(drawnAs('BOTTOM'), 'trousers');
  for (const t of ['DRESS', 'OUTERWEAR', 'SHOES', 'ACCESSORY', 'X']) assert.equal(drawnAs(t), null);
});

test('the drawing keeps the answer’s order and draws only tops and bottoms, in the items’ colours', () => {
  const layers = [
    { itemId: 'shoes', type: 'SHOES', shape, fit: [] },
    { itemId: 'jeans', type: 'BOTTOM', shape, fit: [] },
    { itemId: 'shirt', type: 'TOP', shape, fit: [] },
  ];
  const b = bands(answer(layers), 360);
  const xml = avatarXml(answer(layers), { jeans: 'NAVY', shirt: 'WHITE' }, b);
  assert.match(xml, new RegExp(`viewBox="0 0 ${b.width} ${b.height}"`));
  assert.ok(!xml.includes('{'));
  const trousersAt = xml.indexOf('<path', xml.indexOf(warpXml(GARMENTS_DATA.doll_bare, b)) + warpXml(GARMENTS_DATA.doll_bare, b).length);
  const shirtFirst = warpXml(TEMPLATE_CLOTHES.shirt, b).slice(0, 40);
  assert.ok(xml.indexOf(shirtFirst) > trousersAt);
  // the other order puts the shirt under the trousers
  const swapped = avatarXml(answer([layers[2], layers[1]]), { jeans: 'NAVY', shirt: 'WHITE' }, b);
  const trousersFirst = warpXml(TEMPLATE_CLOTHES.trousers, b).slice(0, 40);
  assert.ok(swapped.indexOf(shirtFirst) < swapped.indexOf(trousersFirst));
});

test('fit markers sit at the sleeve end, the hem, or the side at a girth line, numbered by layer', () => {
  const layers = [
    { itemId: 'jeans', type: 'BOTTOM', shape: { ...shape, top: 560, bottom: 1500 }, fit: [{ kind: 'LEG_SHORT', part: 'INSEAM' }] },
    { itemId: 'shirt', type: 'TOP', shape, fit: [{ kind: 'SLEEVE_SHORT', part: 'SLEEVE' }, { kind: 'TOO_LOOSE', part: 'CHEST' }] },
    { itemId: 'coat', type: 'OUTERWEAR', shape, fit: [{ kind: 'TOO_TIGHT', part: 'CHEST' }] },
  ];
  const b = bands(answer(layers), 360);
  const m = markers(answer(layers), b);
  assert.deepEqual(m, [
    { x: b.centre + Math.round(160 * b.pxPerMm), y: b.mm(1500), n: 1 },
    { x: b.centre - Math.round(150 * b.pxPerMm), y: b.mm(880), n: 2 },
    { x: b.centre + b.halfWidth.chest, y: b.y.chest, n: 2 },
  ]);
  const noSleeve = { ...layers[1], shape: { ...shape, sleeveEnd: undefined } };
  assert.equal(markers(answer([noSleeve]), b).length, 1);
});

test('parseDressed takes the contract’s answer and refuses a garbled one', () => {
  const body = answer([{ itemId: 'a', type: 'TOP', shape, fit: [{ kind: 'TOO_TIGHT', part: 'CHEST' }] }, { itemId: 'b', type: 'ACCESSORY', fit: [] }]);
  assert.deepEqual(parseDressed(body), body);
  assert.throws(() => parseDressed({ ...body, figure: { ...FIGURE, landmarks: { ...FIGURE.landmarks, waist: 1.5 } } }), /waist/);
  assert.throws(() => parseDressed({ figure: FIGURE }), /layers/);
});

test('dress posts the request and returns the server’s message when it refuses', async () => {
  let seen;
  const ok = async (url, init) => {
    seen = { url, init };
    return new Response(JSON.stringify(answer([])), { status: 200 });
  };
  const req = { measurements: INVENTED_FIGURE, items: [{ id: 'a' }] };
  assert.deepEqual(await dress('http://x', req, ok), answer([]));
  assert.equal(seen.url, 'http://x/avatar/dress');
  assert.equal(seen.init.method, 'POST');
  assert.deepEqual(JSON.parse(seen.init.body), req);
  const refused = async () => new Response(JSON.stringify({ code: 'IMPOSSIBLE_OUTFIT', message: 'two tops' }), { status: 409 });
  await assert.rejects(dress('http://x', req, refused), /two tops/);
});

test('the outfit: named ids in order, else the first clean top and bottom; no sizes are sent', () => {
  const item = (id, type, status = 'CLEAN') => ({ id, name: id, type, colour: 'NAVY', pattern: 'PLAIN', fabric: 'COTTON', seasons: [], formality: 'CASUAL', status, next: [] });
  const items = [item('t1', 'TOP', 'IN_WASH'), item('t2', 'TOP'), item('b1', 'BOTTOM'), item('d1', 'DRESS')];
  assert.deepEqual(outfitRequest(items).items, [{ id: 't2' }, { id: 'b1' }]);
  assert.deepEqual(outfitRequest(items, 'd1,nope,t1').items, [{ id: 'd1' }, { id: 't1' }]);
  assert.deepEqual(outfitRequest([item('t1', 'TOP', 'IN_WASH')]).items, [{ id: 't1' }]);
  assert.equal(outfitRequest(items).measurements, INVENTED_FIGURE);
});

test('fit rows: numbered like the markers, worded, undrawn kinds say so; unknown values are not guessed', () => {
  const rows = fitRows(
    [
      { itemId: 'x', type: 'TOP', fit: [{ kind: 'SLEEVE_SHORT', part: 'SLEEVE' }, { kind: 'TOO_LOOSE', part: 'CHEST' }] },
      { itemId: 'y', type: 'DRESS', fit: [] },
    ],
    [{ id: 'x', name: 'Белая рубашка' }],
  );
  assert.deepEqual(rows[0], { n: 1, name: 'Белая рубашка', text: 'рукав короче, в груди свободно', ok: false, drawn: true });
  assert.equal(rows[1].n, 2);
  assert.equal(rows[1].drawn, false);
  assert.match(rows[1].text, /не нарисовано/);
  assert.equal(noteText({ kind: 'NEW', part: 'NECK' }), 'NEW NECK');
});
