const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const template = fs.readFileSync(path.join(__dirname, '../shared/src/jvmMain/resources/location-map.html'), 'utf8');
const inlineScript = [...template.matchAll(/<script[^>]*>([\s\S]*?)<\/script>/g)].map(match => match[1]).find(text => text.includes("'use strict'"));

function picker({automatic = false, library = true} = {}) {
  const elements = Object.fromEntries(['status', 'confirm', 'cancel', 'locate'].map(id => [id, {textContent:'', disabled:id === 'confirm'}]));
  const events = {}, calls = [];
  let geo = null;
  const map = {setView() {return this;}, on(name, action) {events[name] = action;}};
  const leaflet = {
    map: () => map,
    tileLayer: () => ({addTo() {return this;}, on() {}}),
    latLng: (lat, lng) => ({lat, lng, wrap() {return this;}}),
    marker: point => ({point, addTo() {return this;}, setLatLng(value) {this.point = value;}, getLatLng() {return this.point;}, on() {}}),
    circle: () => ({addTo() {}})
  };
  const context = vm.createContext({
    document:{getElementById:id => elements[id]},
    navigator:{geolocation:{getCurrentPosition(success, failure) {geo = {success, failure};}}},
    fetch:async (url, options) => {calls.push({url, ...options}); return {ok:true};},
    ...(library ? {L:leaflet} : {})
  });
  vm.runInContext(inlineScript.replaceAll('__TOKEN__', 'test-session').replaceAll('__LATITUDE__', '40')
    .replaceAll('__LONGITUDE__', '-4').replaceAll('__HAS_SELECTION__', 'false').replaceAll('__LOCATE__', String(automatic)), context);
  return {elements, calls, events, geo:() => geo};
}

test('manual selection never requests device location and requires a clicked point', () => {
  const page = picker();
  assert.equal(page.geo(), null);
  assert.equal(page.elements.confirm.disabled, true);
  page.events.click({latlng:{lat:38.345, lng:-0.481}});
  assert.equal(page.elements.confirm.disabled, false);
  assert.match(page.elements.status.textContent, /38\.345000/);
});

test('confirmation returns the selected point to the app, without sending a chat', async () => {
  const page = picker();
  page.events.click({latlng:{lat:38.345, lng:-0.481}});
  await page.elements.confirm.onclick();
  // The handler starts an async request; settle its continuation.
  await Promise.resolve();
  assert.equal(page.calls[0].url, 'select');
  assert.equal(page.calls[0].body, '38.345|-0.481');
  assert.equal(page.calls[0].headers['X-Chimera-Selection'], 'test-session');
  assert.match(page.elements.status.textContent, /Vuelve a Chimera/);
});

test('denied geolocation keeps manual map selection available', () => {
  const page = picker({automatic:true});
  page.geo().failure({code:1});
  assert.match(page.elements.status.textContent, /Permiso de ubicación denegado/);
  page.events.click({latlng:{lat:1, lng:2}});
  assert.equal(page.elements.confirm.disabled, false);
});

test('a late location response cannot overwrite a point chosen by the user', async () => {
  const page = picker({automatic:true});
  page.events.click({latlng:{lat:1, lng:2}});
  page.geo().success({coords:{latitude:3, longitude:4, accuracy:10}});
  await page.elements.confirm.onclick();
  assert.equal(page.calls[0].body, '1|2');
});

test('cancel does not turn the initial map viewport into a location', async () => {
  const page = picker();
  await page.elements.cancel.onclick();
  assert.equal(page.calls[0].body, 'cancel');
  assert.match(page.elements.status.textContent, /Selección cancelada/);
});

test('an unavailable map library shows an error and still allows cancelling', async () => {
  const page = picker({library:false});
  assert.match(page.elements.status.textContent, /No se pudo cargar el mapa/);
  assert.equal(page.elements.confirm.disabled, true);
  await page.elements.cancel.onclick();
  assert.equal(page.calls[0].body, 'cancel');
});
