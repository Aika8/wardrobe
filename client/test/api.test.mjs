import { test } from 'node:test';
import assert from 'node:assert/strict';
import { changeStatus } from '../src/api.ts';
import { parseItem, parseItems } from '../src/items.ts';

const item = (status, next) => ({
  id: 'jeans 1', name: 'Jeans', type: 'BOTTOM', colour: 'BLUE', pattern: 'PLAIN', fabric: 'DENIM',
  seasons: ['SPRING'], formality: 'CASUAL', status, ...(next === undefined ? {} : { next }),
});

/** A fake `fetch` that records its calls and answers with one prepared response. */
function fakeFetch(status, body) {
  const calls = [];
  const fn = async (url, init) => {
    calls.push({ url, init });
    const text = typeof body === 'string' ? body : JSON.stringify(body);
    return new Response(text, { status, headers: { 'Content-Type': 'application/json' } });
  };
  return { fn, calls };
}

test('changeStatus sends PUT /items/{id}/status with the status and returns the updated item', async () => {
  const { fn, calls } = fakeFetch(200, item('IN_WASH', ['CLEAN', 'IN_REPAIR']));
  const updated = await changeStatus('http://srv', 'jeans 1', 'IN_WASH', fn);
  assert.equal(calls.length, 1);
  assert.equal(calls[0].url, 'http://srv/items/jeans%201/status');
  assert.equal(calls[0].init.method, 'PUT');
  assert.deepEqual(JSON.parse(calls[0].init.body), { status: 'IN_WASH' });
  assert.equal(updated.status, 'IN_WASH');
  assert.deepEqual(updated.next, ['CLEAN', 'IN_REPAIR']);
});

test('changeStatus throws the server Error.message as is', async () => {
  for (const code of [400, 404, 409]) {
    const message = `Cannot move it (${code}).`;
    const { fn } = fakeFetch(code, { code: 'ILLEGAL_STATUS_CHANGE', message });
    await assert.rejects(changeStatus('http://srv', 'a', 'ON_TRIP', fn), (e) => e.message === message);
  }
});

test('changeStatus without an Error body says the HTTP status', async () => {
  const { fn } = fakeFetch(502, 'Bad gateway');
  await assert.rejects(changeStatus('http://srv', 'a', 'CLEAN', fn), (e) => e.message === 'HTTP 502');
});

test('changeStatus refuses a broken 200 answer instead of showing half an item', async () => {
  const { fn } = fakeFetch(200, { id: 'a' });
  await assert.rejects(changeStatus('http://srv', 'a', 'CLEAN', fn), /'name'/);
});

test('parseItems reads next; a server without next gives no moves', () => {
  const [withNext, without] = parseItems([item('CLEAN', ['IN_WASH', 'ON_TRIP']), item('CLEAN')]);
  assert.deepEqual(withNext.next, ['IN_WASH', 'ON_TRIP']);
  assert.deepEqual(without.next, []);
});

test('parseItem refuses a garbled next', () => {
  assert.throws(() => parseItem(item('CLEAN', 'IN_WASH')), /'next'/);
  assert.throws(() => parseItem(item('CLEAN', ['LOST'])), /unknown status 'LOST' in 'next'/);
});
