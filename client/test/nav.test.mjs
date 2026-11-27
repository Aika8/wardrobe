import { test } from 'node:test';
import assert from 'node:assert/strict';
import { back, current, MAIN_ROUTES, parseHash, push, reset, resolveRoute, sameRoute, toHash } from '../src/nav.ts';

test('parseHash reads a name and a param', () => {
  assert.deepEqual(parseHash('#/item/white-shirt'), { name: 'item', param: 'white-shirt' });
  assert.deepEqual(parseHash('#/list/IN_WASH'), { name: 'list', param: 'IN_WASH' });
  assert.deepEqual(parseHash('#/sheet'), { name: 'sheet' });
  assert.deepEqual(parseHash('#/sheet/'), { name: 'sheet' });
});

test('parseHash decodes the param', () => {
  assert.deepEqual(parseHash('#/item/%D1%80%D1%83%D0%B1%D0%B0%D1%88%D0%BA%D0%B0%20%2F%201'), {
    name: 'item',
    param: 'рубашка / 1',
  });
});

test('parseHash gives null for no route and for a broken escape', () => {
  for (const h of ['', '#', '#/', '#//x', '#/item/%E0%A4%A']) assert.equal(parseHash(h), null, h);
});

test('toHash then parseHash gives the same route: spaces, «/» and Cyrillic in the param', () => {
  const routes = [
    { name: 'list' },
    { name: 'list', param: 'IN_WASH' },
    { name: 'item', param: 'white-shirt' },
    { name: 'item', param: 'белая рубашка' },
    { name: 'item', param: 'a/b/c' },
    { name: 'item', param: '100% хлопок #1?x=y' },
  ];
  for (const r of routes) assert.deepEqual(parseHash(toHash(r)), r, JSON.stringify(r));
  assert.equal(toHash({ name: 'item', param: 'a b/c' }), '#/item/a%20b%2Fc');
  assert.equal(toHash({ name: 'list' }), '#/list');
});

test('sameRoute compares name and param', () => {
  assert.ok(sameRoute({ name: 'list' }, { name: 'list' }));
  assert.ok(sameRoute({ name: 'list', param: '' }, { name: 'list' }));
  assert.ok(!sameRoute({ name: 'list' }, { name: 'list', param: 'IN_WASH' }));
});

test('resolveRoute: an unknown or missing route gives the fallback', () => {
  const known = ['list', 'item', 'sheet'];
  const fallback = { name: 'list' };
  assert.deepEqual(resolveRoute({ name: 'item', param: 'x' }, known, fallback), { name: 'item', param: 'x' });
  assert.equal(resolveRoute({ name: 'nowhere', param: 'x' }, known, fallback), fallback);
  assert.equal(resolveRoute(parseHash('#/nowhere'), known, fallback), fallback);
  assert.equal(resolveRoute(null, known, fallback), fallback);
});

test('push adds on top and does not change the input', () => {
  const h = [{ name: 'list' }];
  const h2 = push(h, { name: 'item', param: 'a' });
  assert.deepEqual(h2, [{ name: 'list' }, { name: 'item', param: 'a' }]);
  assert.deepEqual(h, [{ name: 'list' }]);
  assert.deepEqual(current(h2), { name: 'item', param: 'a' });
});

test('back removes the top; at the root it returns the same history', () => {
  const h = [{ name: 'list' }, { name: 'item', param: 'a' }];
  const b = back(h);
  assert.deepEqual(b, [{ name: 'list' }]);
  assert.equal(h.length, 2);
  assert.equal(back(b), b);
  assert.deepEqual(current(back(back(b))), { name: 'list' });
});

test('reset makes the history one route', () => {
  const h = [{ name: 'list' }, { name: 'item', param: 'a' }];
  assert.deepEqual(reset(h, { name: 'me' }), [{ name: 'me' }]);
  assert.equal(h.length, 2);
});

test('current of an empty history is an error', () => {
  assert.throws(() => current([]));
});

test('MAIN_ROUTES are the four places of BottomNav', () => {
  assert.deepEqual(MAIN_ROUTES, ['home', 'wardrobe', 'calendar', 'me']);
});
