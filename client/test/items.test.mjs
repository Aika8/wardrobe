import { test } from 'node:test';
import assert from 'node:assert/strict';
import { parseItems, sortForList, STATUS_LABEL } from '../src/items.ts';

const item = (id, name, status) => ({
  id, name, type: 'TOP', colour: 'WHITE', pattern: 'PLAIN', fabric: 'COTTON',
  seasons: ['SUMMER'], formality: 'CASUAL', status,
});

test('parseItems reads the contract shape', () => {
  const items = parseItems([item('a', 'Shirt', 'CLEAN')]);
  assert.equal(items.length, 1);
  assert.deepEqual(items[0].seasons, ['SUMMER']);
});

test('parseItems refuses a broken answer instead of showing half an item', () => {
  assert.throws(() => parseItems({}), /array/);
  assert.throws(() => parseItems([{ ...item('a', 'Shirt', 'CLEAN'), name: 3 }]), /'name'/);
  assert.throws(() => parseItems([{ ...item('a', 'Shirt', 'CLEAN'), seasons: 'SUMMER' }]), /'seasons'/);
  assert.throws(() => parseItems([item('a', 'Shirt', 'LOST')]), /unknown status 'LOST'/);
});

test('sortForList: clean first, then wash, repair, trip; by name within a status; input untouched', () => {
  const input = [
    item('1', 'Coat', 'IN_REPAIR'),
    item('2', 'Jeans', 'IN_WASH'),
    item('3', 'Tee', 'CLEAN'),
    item('4', 'Dress', 'ON_TRIP'),
    item('5', 'Blouse', 'CLEAN'),
  ];
  assert.deepEqual(sortForList(input).map((i) => i.id), ['5', '3', '2', '1', '4']);
  assert.equal(input[0].id, '1');
});

test('every status has a Russian label', () => {
  for (const s of ['CLEAN', 'IN_WASH', 'IN_REPAIR', 'ON_TRIP']) assert.ok(STATUS_LABEL[s], s);
});
