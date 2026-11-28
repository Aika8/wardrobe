// The status move of the item screen (AUTO-20261126-5): a changeStatus answer for a move started on an earlier
// item must not replace the item shown now, set a move error on it, or leave it busy.
//
// The harness (deepStub, fakeReact, loadScreen) is copied from screens.test.mjs, which exports nothing and is
// append-only: the real screen source is transpiled to CommonJS and run with a fake React (hooks by call order).
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import ts from 'typescript';

function deepStub() {
  const f = function () {};
  return new Proxy(f, {
    get: (_t, k) => (k === Symbol.toPrimitive ? () => '' : deepStub()),
    apply: () => deepStub(),
    construct: () => deepStub(),
  });
}

// State slots by call order, effects collected per render; useRef keeps a stable holder per slot.
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
      useRef(init) {
        const i = cursor++;
        if (!(i in slots)) slots[i] = { current: init };
        return slots[i];
      },
      useEffect(fn) {
        effects.push(fn);
      },
      useMemo: (fn) => fn(),
    },
    render(component, props) {
      cursor = 0;
      effects = [];
      const tree = component(props);
      return { effects, tree };
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
  const require = (spec) => {
    if (spec === 'react') return fake.react;
    // JSX elements as plain objects, so a test can find a child's props (ItemDetails' onMove).
    if (spec === 'react/jsx-runtime') {
      const jsx = (type, props) => ({ type, props });
      return { jsx, jsxs: jsx, Fragment: 'Fragment' };
    }
    return modules[spec] ?? deepStub();
  };
  const module = { exports: {} };
  new Function('require', 'module', 'exports', outputText)(require, module, module.exports);
  return { component: module.exports[exportName], fake, ItemDetails: modules['./ItemDetails.tsx'].ItemDetails };
}

function deferred() {
  let resolve;
  let reject;
  const promise = new Promise((res, rej) => {
    resolve = res;
    reject = rej;
  });
  return { promise, resolve, reject };
}

const settle = () => new Promise((r) => setImmediate(r));

// Depth-first search of the fake JSX tree for the element of a given type.
function find(tree, type) {
  if (tree === null || typeof tree !== 'object') return undefined;
  if (tree.type === type) return tree;
  const children = tree.props?.children;
  for (const child of Array.isArray(children) ? children : [children]) {
    const hit = find(child, type);
    if (hit) return hit;
  }
  return undefined;
}

const A = { id: 'a', colour: 'WHITE', status: 'CLEAN' };
const B = { id: 'b', colour: 'BLACK', status: 'CLEAN' };

function setup() {
  const loads = [];
  const moves = [];
  const modules = {
    './loadItems.ts': {
      loadItems: () => {
        const d = deferred();
        loads.push(d);
        return d.promise;
      },
      messageOf: (e) => String(e?.message ?? e),
    },
    '../api.ts': {
      changeStatus: (_url, id, status) => {
        const d = deferred();
        moves.push({ id, status, ...d });
        return d.promise;
      },
    },
    './ItemDetails.tsx': { ItemDetails: function ItemDetails() {} },
  };
  const screen = loadScreen('ItemScreen.tsx', 'ItemScreen', modules);
  const props = (param) => ({ route: { name: 'item', param }, back: () => {}, go: () => {} });
  return { ...screen, loads, moves, props };
}

// Show `a`, start a move on it (its answer stays pending), switch to `b` and let `b` load.
async function moveThenSwitch() {
  const s = setup();
  const first = s.fake.render(s.component, s.props('a'));
  const cleanupA = first.effects[0]();
  s.loads[0].resolve([A, B]);
  await settle();
  const shownA = s.fake.render(s.component, s.props('a'));
  const details = find(shownA.tree, s.ItemDetails);
  assert.ok(details, 'item a is shown with ItemDetails');
  details.props.onMove('IN_WASH');
  assert.equal(s.moves.length, 1, 'one move asked');
  assert.equal(s.moves[0].id, 'a');
  if (typeof cleanupA === 'function') cleanupA();
  const second = s.fake.render(s.component, s.props('b'));
  second.effects[0]();
  s.loads[1].resolve([A, B]);
  await settle();
  return s;
}

function shown(s) {
  const { tree } = s.fake.render(s.component, s.props('b'));
  const details = find(tree, s.ItemDetails);
  assert.ok(details, 'an item is shown');
  return details.props;
}

test('item screen: a late move answer for item a does not replace item b', async () => {
  const s = await moveThenSwitch();
  s.moves[0].resolve({ ...A, status: 'IN_WASH' });
  await settle();
  const props = shown(s);
  assert.equal(props.item.id, 'b');
  assert.equal(props.error, null, 'no move error');
  assert.equal(props.busy, false, 'not moving');
});

test('item screen: a late move failure for item a sets no error on item b', async () => {
  const s = await moveThenSwitch();
  s.moves[0].reject(new Error('HTTP 409'));
  await settle();
  const props = shown(s);
  assert.equal(props.item.id, 'b');
  assert.equal(props.error, null, 'no move error');
  assert.equal(props.busy, false, 'not moving');
});

test('item screen: a move answer for the item still shown replaces it', async () => {
  const s = setup();
  const first = s.fake.render(s.component, s.props('a'));
  first.effects[0]();
  s.loads[0].resolve([A, B]);
  await settle();
  find(s.fake.render(s.component, s.props('a')).tree, s.ItemDetails).props.onMove('IN_WASH');
  assert.equal(find(s.fake.render(s.component, s.props('a')).tree, s.ItemDetails).props.busy, true, 'busy while moving');
  s.moves[0].resolve({ ...A, status: 'IN_WASH' });
  await settle();
  const props = find(s.fake.render(s.component, s.props('a')).tree, s.ItemDetails).props;
  assert.equal(props.item.status, 'IN_WASH');
  assert.equal(props.busy, false);
});
