import { test } from 'node:test';
import assert from 'node:assert/strict';
import { mkdirSync, mkdtempSync, readdirSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { basename, extname, join, relative, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

/**
 * Version 1 keeps no model key and no photo in the client (CLAUDE.md, "Don'ts"). This test walks `client/` and
 * fails on a key-like string in any text file or on any photo file. It skips its own file, whose patterns would
 * otherwise match themselves.
 */

const CLIENT = resolve(fileURLToPath(new URL('..', import.meta.url)));
const SELF = fileURLToPath(import.meta.url);
const SKIP_DIRS = new Set(['node_modules', '.expo', 'dist']);
const PHOTO_EXT = new Set(['.jpg', '.jpeg', '.heic']);

const KEY_PATTERNS = [
  /sk-ant-/i,
  /sk-[A-Za-z0-9]{20,}/i,
  /AIza[0-9A-Za-z_-]{30,}/i,
  /api[_-]?key\s*[:=]/i,
  /Authorization:\s*Bearer\s+[A-Za-z0-9]/i,
];

/** The problems of one file: its path, extension and content. An empty list means the file is clean. */
function problems(path, content) {
  if (PHOTO_EXT.has(extname(path).toLowerCase())) return [`photo file: ${path}`];
  if (content.includes(0)) return []; // binary (an icon): no text key to find
  const text = content.toString('utf8');
  return KEY_PATTERNS.filter((p) => p.test(text)).map((p) => `key-like string ${p} in ${path}`);
}

/** Every file under `dir`, skipping the build and dependency folders and this test file. */
function walk(dir) {
  const files = [];
  for (const entry of readdirSync(dir, { withFileTypes: true })) {
    const path = join(dir, entry.name);
    if (entry.isDirectory()) {
      if (!SKIP_DIRS.has(entry.name)) files.push(...walk(path));
    } else if (entry.isFile() && path !== SELF) {
      files.push(path);
    }
  }
  return files;
}

function scan(dir) {
  return walk(dir).flatMap((path) => problems(relative(dir, path), readFileSync(path)));
}

test('no file under client/ holds a key-like string or a photo', () => {
  const files = walk(CLIENT);
  assert.ok(files.some((f) => basename(f) === 'package.json'), 'the walk reaches the client files');
  assert.ok(!files.some((f) => f.split(/[\\/]/).includes('node_modules')), 'node_modules is skipped');
  assert.ok(!files.includes(SELF), 'the test skips its own file');
  assert.deepEqual(scan(CLIENT), []);
});

test('the matcher catches each key pattern in a planted file', () => {
  // Built from parts so this source never holds a whole key-like string.
  const planted = {
    'anthropic.ts': `const k = "${'sk-' + 'ant-'}abc";`,
    'generic.ts': `const k = "${'sk-' + 'A1b2C3d4E5f6G7h8I9j0K'}";`,
    'google.ts': `const k = "${'AI' + 'za' + 'A'.repeat(30)}";`,
    'config.ts': `export const ${'api' + '_key'} = process.env.X;`,
    'camel.ts': `{ ${'Api' + 'Key'}: x }`,
    'fetch.ts': `headers: "${'Author' + 'ization'}: ${'Bear' + 'er'} abc"`,
    'lower.ts': `${'author' + 'ization'}: ${'bear' + 'er'} xyz`,
  };
  const dir = mkdtempSync(join(tmpdir(), 'wr-no-secrets-'));
  try {
    for (const [name, text] of Object.entries(planted)) {
      writeFileSync(join(dir, name), text);
      assert.notDeepEqual(problems(name, readFileSync(join(dir, name))), [], `${name} is caught`);
    }
    assert.equal(scan(dir).length, Object.keys(planted).length);
  } finally {
    rmSync(dir, { recursive: true, force: true });
  }
});

test('the matcher catches a photo file, whatever the case of its extension', () => {
  const dir = mkdtempSync(join(tmpdir(), 'wr-no-secrets-'));
  try {
    for (const name of ['a.jpg', 'b.JPEG', 'c.heic']) writeFileSync(join(dir, name), 'not really a photo');
    writeFileSync(join(dir, 'icon.png'), Buffer.from([0x89, 0x50, 0x4e, 0x47, 0, 1, 2]));
    assert.deepEqual(scan(dir).sort(), ['photo file: a.jpg', 'photo file: b.JPEG', 'photo file: c.heic']);
  } finally {
    rmSync(dir, { recursive: true, force: true });
  }
});

test('the matcher leaves ordinary code alone', () => {
  const clean = [
    'const task = "ask-me";',
    'const apiUrl = process.env.EXPO_PUBLIC_API_URL;',
    'headers: { Accept: "application/json" }',
  ].join('\n');
  assert.deepEqual(problems('clean.ts', Buffer.from(clean)), []);
});

test('the walk skips node_modules, .expo and dist', () => {
  const dir = mkdtempSync(join(tmpdir(), 'wr-no-secrets-'));
  try {
    for (const name of [...SKIP_DIRS, 'src']) {
      mkdirSync(join(dir, name));
      writeFileSync(join(dir, name, 'photo.jpg'), 'not really a photo');
    }
    assert.deepEqual(scan(dir), [`photo file: ${join('src', 'photo.jpg')}`]);
  } finally {
    rmSync(dir, { recursive: true, force: true });
  }
});
