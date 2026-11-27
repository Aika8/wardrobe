// The load effects of the item and avatar screens (AUTO-20261126-4): when route.param changes while a request is
// in flight, the answer for the earlier param must not overwrite the later one.
//
// Node cannot import a .tsx screen, and there is no React renderer in the tests. So the real screen source is
// transpiled with TypeScript to CommonJS and run with a tiny fake React (hooks only) and stubbed imports; the test
// drives the effect the way React would: run it for `a`, run its cleanup, run it for `b`.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import ts from 'typescript';

// Any property, call or `new` gives another stub: enough for StyleSheet.create, tokens.*, components.
function deepStub() {
  const f = function () {};
  return new Proxy(f, {
    get: (_t, k) => (k === Symbol.toPrimitive ? () => '' : deepStub()),
    apply: () => deepStub(),
    construct: () => deepStub(),
  });
}

// A minimal hooks runtime: state slots by call order, effects collected per render.
function fakeReact() {
  const slots = [];
  let cursor = 0;
  let effects = [];
  return {
    slots,
    react: {
      useState(init) {
        const i = cursor++;
        if (!(i in slots)) slots[i] = init;
        return [slots[i], (v) => (slots[i] = typeof v === 'function' ? v(slots[i]) : v)];
      },
      useEffect(fn) {
        effects.push(fn);
      },
      useMemo: (fn) => fn(),
    },
    render(component, props) {
      cursor = 0;
      effects = [];
      component(props);
      return effects;
    },
  };
}

function loadScreen(file, exportName, modules) {
  const source = readFileSync(new URL(`../src/screens/${file}`, import.meta.url), 'utf8');
  const { outputText } = ts.transpileModule(source, {
    compilerOptions: {
      module: ts.ModuleKind.CommonJS,
      target: ts.ScriptTarget.ES2022,
      jsx: ts.JsxEmit.ReactJSX,
      esModuleInterop: true,
    },
  });
  const fake = fakeReact();
  const require = (spec) => (spec === 'react' ? fake.react : (modules[spec] ?? deepStub()));
  const module = { exports: {} };
  new Function('require', 'module', 'exports', outputText)(require, module, module.exports);
  return { component: module.exports[exportName], fake };
}

function deferred() {
  let resolve;
  const promise = new Promise((r) => (resolve = r));
  return { promise, resolve };
}

const settle = () => new Promise((r) => setImmediate(r));

// Render for `a`, start its load; change to `b` (cleanup of `a`, effect of `b`); then the loads answer
// in the order given. Returns the fake React (its state slots) after everything settled.
async function changeParamMidLoad(file, exportName, modules, pending, order) {
  const { component, fake } = loadScreen(file, exportName, modules);
  const props = (param) => ({ route: { name: 'x', param }, back: () => {}, go: () => {} });
  const [effectA] = fake.render(component, props('a'));
  const cleanupA = effectA();
  if (typeof cleanupA === 'function') cleanupA();
  const [effectB] = fake.render(component, props('b'));
  effectB();
  assert.equal(pending.length, 2, 'one load per param');
  for (const [i, value] of order) {
    pending[i].resolve(value);
    await settle();
  }
  return fake;
}

const ITEMS = [{ id: 'a', colour: 'WHITE' }, { id: 'b', colour: 'BLACK' }];

function itemModules(pending) {
  return {
    './loadItems.ts': {
      loadItems: () => {
        const d = deferred();
        pending.push(d);
        return d.promise;
      },
      messageOf: String,
    },
  };
}

test('item screen: the answer for the first param arriving last does not replace the second', async () => {
  const pending = [];
  // Second request (b) answers first, then the first one (a).
  const fake = await changeParamMidLoad('ItemScreen.tsx', 'ItemScreen', itemModules(pending), pending, [
    [1, ITEMS],
    [0, ITEMS],
  ]);
  assert.equal(fake.slots[0]?.id, 'b'); // slot 0: item
  assert.equal(fake.slots[1], null); // slot 1: error
});

test('item screen: in-order answers still show the second param', async () => {
  const pending = [];
  const fake = await changeParamMidLoad('ItemScreen.tsx', 'ItemScreen', itemModules(pending), pending, [
    [0, ITEMS],
    [1, ITEMS],
  ]);
  assert.equal(fake.slots[0]?.id, 'b');
});

function avatarModules(pending) {
  return {
    ...itemModules(pending),
    '../avatar/api.ts': { dress: (_url, request) => Promise.resolve({ layers: [], for: request.param }) },
    '../avatar/outfit.ts': { outfitRequest: (_items, param) => ({ param }) },
  };
}

test('avatar screen: the answer for the first param arriving last does not replace the second', async () => {
  const pending = [];
  const fake = await changeParamMidLoad('Avatar.tsx', 'Avatar', avatarModules(pending), pending, [
    [1, ITEMS],
    [0, ITEMS],
  ]);
  assert.equal(fake.slots[0]?.answer.for, 'b'); // slot 0: { items, answer }
  assert.equal(fake.slots[1], null); // slot 1: error
});

test('avatar screen: a late failure for the first param does not show an error over the second', async () => {
  const pending = [];
  const modules = avatarModules(pending);
  modules['./loadItems.ts'].loadItems = () => {
    const d = deferred();
    pending.push(d);
    return d.promise.then((v) => (v instanceof Error ? Promise.reject(v) : v));
  };
  const fake = await changeParamMidLoad('Avatar.tsx', 'Avatar', modules, pending, [
    [1, ITEMS],
    [0, new Error('HTTP 500')],
  ]);
  assert.equal(fake.slots[0]?.answer.for, 'b');
  assert.equal(fake.slots[1], null);
});
