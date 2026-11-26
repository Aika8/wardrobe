import { test } from 'node:test';
import assert from 'node:assert/strict';
import { join } from 'node:path';
import { DEFAULT_API_PORT, parseScreenshotArgs } from '../scripts/screenshot.mjs';

const ROOT = join('/', 'repo');

test('an id alone: root route, default API port, PNG named after the id in the screens folder', () => {
  assert.deepEqual(parseScreenshotArgs(['WR-035'], ROOT), {
    id: 'WR-035',
    route: '/',
    apiPort: DEFAULT_API_PORT,
    out: join(ROOT, 'docs', 'backlog', 'reports', 'screens', 'WR-035.png'),
  });
  assert.equal(DEFAULT_API_PORT, 8091);
});

test('--route is taken before or after the id', () => {
  assert.equal(parseScreenshotArgs(['WR-040', '--route', '/items/a'], ROOT).route, '/items/a');
  assert.equal(parseScreenshotArgs(['--route', '/outfit', 'WR-041'], ROOT).id, 'WR-041');
});

test('a missing or malformed id is refused, so the PNG can never land outside the screens folder', () => {
  assert.throws(() => parseScreenshotArgs([], ROOT), /task id is required/);
  assert.throws(() => parseScreenshotArgs(['../../x'], ROOT), /not a task id/);
  assert.throws(() => parseScreenshotArgs(['wr-035'], ROOT), /not a task id/);
  assert.throws(() => parseScreenshotArgs(['WR-035/a'], ROOT), /not a task id/);
});

test('bad options are refused with the usage line', () => {
  assert.throws(() => parseScreenshotArgs(['WR-035', '--route'], ROOT), /--route needs a path/);
  assert.throws(() => parseScreenshotArgs(['WR-035', '--route', 'items'], ROOT), /--route needs a path/);
  assert.throws(() => parseScreenshotArgs(['WR-035', '--port', '1'], ROOT), /unknown option '--port'/);
  assert.throws(() => parseScreenshotArgs(['WR-035', 'WR-036'], ROOT), /one task id only/);
});
