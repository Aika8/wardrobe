import { test } from 'node:test';
import assert from 'node:assert/strict';
import { countsByStatus, filterByStatus, filterChips, FILTER_STATUSES } from '../src/filter.ts';
import { sortForList, STATUS_LABEL } from '../src/items.ts';

const item = (id, status) => ({
  id, name: id, type: 'TOP', colour: 'WHITE', pattern: 'PLAIN', fabric: 'COTTON',
  seasons: ['SUMMER'], formality: 'CASUAL', status,
});

const items = [item('a', 'CLEAN'), item('b', 'IN_WASH'), item('c', 'CLEAN'), item('d', 'ON_TRIP')];

test('filterByStatus keeps only that status, in the given order', () => {
  assert.deepEqual(filterByStatus(items, 'CLEAN').map((i) => i.id), ['a', 'c']);
  assert.deepEqual(filterByStatus(items, 'IN_WASH').map((i) => i.id), ['b']);
  assert.deepEqual(filterByStatus(items, 'IN_REPAIR'), []);
});

test('filterByStatus ALL gives every item; the input is not changed', () => {
  const before = items.map((i) => i.id);
  const all = filterByStatus(items, 'ALL');
  assert.deepEqual(all.map((i) => i.id), before);
  assert.notEqual(all, items);
  filterByStatus(items, 'CLEAN');
  assert.deepEqual(items.map((i) => i.id), before);
});

test('countsByStatus has every status, zero included', () => {
  assert.deepEqual(countsByStatus(items), { CLEAN: 2, IN_WASH: 1, IN_REPAIR: 0, ON_TRIP: 1 });
  assert.deepEqual(countsByStatus([]), { CLEAN: 0, IN_WASH: 0, IN_REPAIR: 0, ON_TRIP: 0 });
});

test('the counts add up to the list, and each count matches the filter', () => {
  const counts = countsByStatus(items);
  assert.equal(Object.values(counts).reduce((a, b) => a + b, 0), items.length);
  for (const s of FILTER_STATUSES) assert.equal(counts[s], filterByStatus(items, s).length);
});

test('FILTER_STATUSES covers every status the client knows, in list order', () => {
  assert.deepEqual([...FILTER_STATUSES].sort(), Object.keys(STATUS_LABEL).sort());
  const sorted = sortForList(FILTER_STATUSES.map((s) => item('x', s))).map((i) => i.status);
  assert.deepEqual(sorted, [...FILTER_STATUSES]);
});

test('filterChips: «Все» with the total, then every status with its word and count', () => {
  assert.deepEqual(filterChips(items), [
    { key: 'ALL', label: 'Все', count: 4 },
    { key: 'CLEAN', label: 'Чистое', count: 2 },
    { key: 'IN_WASH', label: 'В стирке', count: 1 },
    { key: 'IN_REPAIR', label: 'В ремонте', count: 0 },
    { key: 'ON_TRIP', label: 'В поездке', count: 1 },
  ]);
  assert.equal(filterChips([]).length, 5);
});
