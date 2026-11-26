import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { ICONS, iconXml } from '../src/ui/icons.ts';

// Each icon by the label that follows it in the mockups (the «+» of the bottom bar sits inside class="add").
const LABELS = {
  outfit: ['Собрать образ'],
  pairs: ['Что с чем носить'],
  suitcase: ['Сборы в поездку'],
  wash: ['Сейчас в стирке', 'В стирке'],
  home: ['Главная'],
  wardrobe: ['Шкаф'],
  calendar: ['Календарь'],
  me: ['Я'],
  wear: ['Носка'],
  want: ['Хочу'],
  trip: ['Поездка', 'В поездку'],
  shouldBuy: ['Нужно ли?'],
  clean: ['Чистое'],
  repair: ['В ремонт'],
};

const SVG = /<svg class="ic"[^>]*>\s*<path d="([^"]*)"\s*\/>\s*<\/svg>/g;

/** Every `svg.ic` of the mockups with its label: closing tags and `<span>` skipped, `<br>` read as a space. */
function mockupIcons() {
  const html = readFileSync(new URL('../../docs/design/mockups/mockups.html', import.meta.url), 'utf8');
  const found = [];
  for (const m of html.matchAll(SVG)) {
    const before = html.slice(Math.max(0, m.index - 40), m.index);
    const inAdd = /<span class="add">\s*$/.test(before);
    let rest = html.slice(m.index + m[0].length);
    rest = rest.replace(/^(?:\s|<\/[a-z]+>|<span>)*/, '');
    const text = /^(?:[^<]|<br\s*\/?>)*/.exec(rest)[0].replace(/<br\s*\/?>/g, ' ').replace(/\s+/g, ' ').trim();
    found.push({ d: m[1], label: text, inAdd });
  }
  return found;
}

const icons = mockupIcons();

for (const [name, labels] of Object.entries(LABELS)) {
  for (const label of labels) {
    test(`ICONS.${name} is the mockup icon «${label}»`, () => {
      const hits = icons.filter((i) => i.label === label);
      assert.ok(hits.length > 0, `no icon labelled «${label}» in the mockups`);
      for (const h of hits) assert.equal(ICONS[name], h.d);
    });
  }
}

test('ICONS.plus is the icon inside class="add" of the bottom bar', () => {
  const hits = icons.filter((i) => i.inAdd);
  assert.ok(hits.length > 0, 'no icon inside class="add"');
  for (const h of hits) assert.equal(ICONS.plus, h.d);
});

test('ICONS holds exactly the named icons', () => {
  assert.deepEqual(Object.keys(ICONS).sort(), [...Object.keys(LABELS), 'plus'].sort());
});

test('iconXml is the full svg with the path, the colour and the mockup stroke style', () => {
  const xml = iconXml('wear', '#9A7B8C');
  assert.ok(xml.startsWith('<svg') && xml.endsWith('</svg>'));
  assert.ok(xml.includes('viewBox="0 0 24 24"'));
  assert.ok(xml.includes(`d="${ICONS.wear}"`));
  assert.ok(xml.includes('stroke="#9A7B8C"'));
  assert.ok(xml.includes('fill="none"'));
  assert.ok(xml.includes('stroke-width="1.8"'));
  assert.ok(xml.includes('stroke-linecap="round"'));
  assert.ok(xml.includes('stroke-linejoin="round"'));
});

test('iconXml keeps every path verbatim and escapes only what breaks an attribute', () => {
  for (const name of Object.keys(ICONS)) assert.ok(iconXml(name, '#000').includes(`d="${ICONS[name]}"`), name);
  assert.ok(iconXml('home', 'rgba(1, 2, 3, 0.5)').includes('stroke="rgba(1, 2, 3, 0.5)"'));
  const odd = iconXml('home', '"><x a="&');
  assert.ok(odd.includes('stroke="&quot;&gt;&lt;x a=&quot;&amp;"'));
  assert.ok(!odd.includes('<x'));
});

test('iconXml throws on an unknown name', () => {
  assert.throws(() => iconXml('nope', '#000'), /unknown icon/);
  assert.throws(() => iconXml('toString', '#000'), /unknown icon/);
});
