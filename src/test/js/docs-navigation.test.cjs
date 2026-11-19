const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');
const script = fs.readFileSync(path.join(__dirname, '../../main/resources/static/docs-version-switch.js'), 'utf8');

function page(search = '') {
  const navigations = [];
  const listeners = [];
  class Select {
    constructor(value, topbar = true) { this.value = value; this.topbar = topbar; }
    matches(selector) { return selector === '.topbar select' && this.topbar; }
  }
  vm.runInNewContext(script, {
    URLSearchParams, HTMLSelectElement: Select,
    window: { location: { search, assign: value => navigations.push(value), replace: value => navigations.push(value) } },
    document: { addEventListener: (type, callback, capture) => listeners.push({ type, callback, capture }) },
  });
  return { navigations, listeners, Select };
}

test('stock Swagger stays on V1 until V2 is selected', () => {
  const p = page();
  assert.deepEqual(p.navigations, []);
  let stopped = false;
  const selector = new p.Select('/api-docs/v2');
  p.listeners[0].callback({ target: selector, stopImmediatePropagation() { stopped = true; } });
  assert.equal(p.listeners[0].capture, true);
  assert.equal(stopped, true);
  assert.equal(selector.value, '/api-docs/v1');
  assert.deepEqual(p.navigations, ['/docs/v2']);
});

test('changing a request field or keeping V1 does not leave stock Swagger', () => {
  const p = page();
  for (const target of [new p.Select('/api-docs/v1'), new p.Select('/api-docs/v2', false), {}]) {
    p.listeners[0].callback({ target, stopImmediatePropagation() { assert.fail('unexpected navigation'); } });
  }
  assert.deepEqual(p.navigations, []);
});

test('direct links selecting V2 open the historical styled presentation', () => {
  for (const query of ['?url=%2Fapi-docs%2Fv2', '?urls.primaryName=V2%20-%20perguntas%20com%20imagens']) {
    assert.deepEqual(page(query).navigations, ['/docs/v2']);
  }
});

test('styled V2 sends contract two and returns to stock Swagger when V1 is selected', () => {
  const html = fs.readFileSync(path.join(__dirname, '../../main/resources/static/docs-v2.html'), 'utf8');
  const code = [...html.matchAll(/<script>([\s\S]*?)<\/script>/g)].at(-1)[1];
  const calls = [];
  const navigations = [];
  let change;
  const selector = { value: '2', addEventListener(type, callback) { if (type === 'change') change = callback; } };
  vm.runInNewContext(code, {
    document: { getElementById: id => id === 'api-version' ? selector : {} },
    window: { location: { assign: value => navigations.push(value) } },
    SwaggerUIBundle: config => calls.push(config),
  });
  assert.equal(calls.length, 1);
  assert.equal(calls[0].url, '/api-docs/v2');
  const request = { headers: { Accept: 'application/json' } };
  calls[0].requestInterceptor(request);
  assert.equal(request.headers['X-API-Version'], '2');
  assert.equal(request.headers.Accept, 'application/json');
  selector.value = '1';
  change();
  assert.deepEqual(navigations, ['/swagger-ui.html']);
  assert.equal(selector.value, '2');
  assert.equal(calls.length, 1);
});
