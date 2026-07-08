/* Gym Buddy — vanilla JS PWA. State lives in localStorage; full exercise catalog lives in IndexedDB (lazy).
   Domain logic lives in engine.js, chart builders in charts.js (both loaded before this file). */
'use strict';

const STORE_KEY = 'gymbuddy.v1';
const REST_DAY = { accent: '#3DDBD9', tips: ['Complete rest', 'Light walk or yoga', 'Focus on mobility', 'Hydrate & eat clean', 'Prepare for next week'] };
const WD_SHORT = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
const WD_FULL = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];
const GROUP_ORDER = ['chest', 'back', 'shoulders', 'upper arms', 'lower arms', 'upper legs', 'lower legs', 'waist', 'cardio', 'neck'];
const DEFAULT_WEEK = ['mon', 'tue', 'wed', 'thu', 'fri', 'sat'];

/* ---------- store ---------- */
function defaultSettings() { return { restSec: 90, units: 'kg', weeklyGoal: null, rpeEnabled: false, restNotify: false, barKg: 20 }; }
function load() {
  try {
    const s = JSON.parse(localStorage.getItem(STORE_KEY));
    if (s && s.v === 1) {
      if (!s.week) s.week = [...DEFAULT_WEEK];
      if (s.startWeekday == null) s.startWeekday = 1;
      if (!s.customDays) s.customDays = {};
      if (!s.customDayDefs) s.customDayDefs = {};
      if (!s.customExercises) s.customExercises = {};
      if (!s.nextDayId) s.nextDayId = 1;
      if (s.installDismissedAt === undefined) s.installDismissedAt = null;
      if (!s.settings) s.settings = defaultSettings();
      const ds = defaultSettings();
      for (const k in ds) if (s.settings[k] === undefined) s.settings[k] = ds[k];
      if (!s.bodyMetrics) s.bodyMetrics = [];
      if (!s.achievements) s.achievements = {};
      return s;
    }
  } catch (e) { /* corrupted → start fresh */ }
  return {
    v: 1, sessions: [], active: null, settings: defaultSettings(),
    week: [...DEFAULT_WEEK], startWeekday: 1,
    customDays: {}, customDayDefs: {}, customExercises: {}, nextDayId: 1,
    installDismissedAt: null, bodyMetrics: [], achievements: {},
  };
}
function save() { localStorage.setItem(STORE_KEY, JSON.stringify(state)); }
let state = load();
rebuildPRs();

const $ = (sel, el) => (el || document).querySelector(sel);
const $$ = (sel, el) => [...(el || document).querySelectorAll(sel)];
const view = $('#view');
const esc = s => String(s).replace(/[&<>"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));
const titleCase = s => String(s).replace(/\b\w/g, c => c.toUpperCase());

/* ---------- plan resolution (built-in templates + custom overrides) ---------- */
const dayByKey = k => SPLIT.find(d => d.key === k); // built-in template lookup only

function planDayDef(key) {
  const custom = state.customDayDefs[key];
  const base = custom || dayByKey(key);
  if (!base) return null;
  const items = state.customDays[key] || base.items;
  return { key, title: base.title, focus: base.focus, accent: base.accent, items };
}

function todayKey() {
  const wd = new Date().getDay();
  const idx = ((wd - state.startWeekday) % 7 + 7) % 7;
  return idx < state.week.length ? state.week[idx] : null;
}
function weekdayLabelFor(key) {
  const idx = state.week.indexOf(key);
  if (idx === -1) return null;
  return WD_FULL[(state.startWeekday + idx) % 7];
}
function newDayKey() { const k = 'c' + state.nextDayId; state.nextDayId++; return k; }

/* ---------- exercise lookup: curated (offline-guaranteed) → plan overrides → full catalog ---------- */
function getEx(id) { return EXERCISES[id] || state.customExercises[id] || catalogMap.get(id) || null; }

/* ---------- IndexedDB: full 1,324-exercise catalog, loaded lazily on first use ---------- */
const DB_NAME = 'gymbuddy', DB_STORE = 'exercises';
let catalogMap = new Map();
let catalogLoading = null;

function openDB() {
  return new Promise((resolve, reject) => {
    const req = indexedDB.open(DB_NAME, 1);
    req.onupgradeneeded = () => {
      if (!req.result.objectStoreNames.contains(DB_STORE)) req.result.createObjectStore(DB_STORE, { keyPath: 'id' });
    };
    req.onsuccess = () => resolve(req.result);
    req.onerror = () => reject(req.error);
  });
}
function idbGetAll(db) {
  return new Promise((resolve, reject) => {
    const req = db.transaction(DB_STORE, 'readonly').objectStore(DB_STORE).getAll();
    req.onsuccess = () => resolve(req.result);
    req.onerror = () => reject(req.error);
  });
}
function idbPutAll(db, rows) {
  return new Promise((resolve, reject) => {
    const tx = db.transaction(DB_STORE, 'readwrite');
    const os = tx.objectStore(DB_STORE);
    rows.forEach(r => os.put(r));
    tx.oncomplete = resolve; tx.onerror = () => reject(tx.error);
  });
}
/* Only called when the Library tab or an exercise picker is opened — never at startup. */
function ensureCatalog() {
  if (catalogMap.size) return Promise.resolve(catalogMap);
  if (catalogLoading) return catalogLoading;
  catalogLoading = (async () => {
    try {
      const db = await openDB();
      const rows = await idbGetAll(db);
      if (rows.length) { rows.forEach(r => catalogMap.set(r.id, r)); return catalogMap; }
      const data = await (await fetch('catalog.json')).json();
      await idbPutAll(db, data);
      data.forEach(r => catalogMap.set(r.id, r));
    } catch (e) { /* offline on first-ever load with no cached catalog — Library shows an error state */ }
    return catalogMap;
  })();
  return catalogLoading;
}

/* ---------- fuzzy search (pure JS, no deps) ---------- */
function fuzzyScore(text, query) {
  if (!text) return -1;
  text = text.toLowerCase();
  if (text === query) return 1000;
  if (text.startsWith(query)) return 900;
  if (text.split(/\s+/).some(w => w.startsWith(query))) return 800;
  const idx = text.indexOf(query);
  if (idx !== -1) return Math.max(1, 700 - idx);
  let ti = 0, first = -1, last = -1;
  for (let qi = 0; qi < query.length; qi++) {
    const found = text.indexOf(query[qi], ti);
    if (found === -1) return -1;
    if (first === -1) first = found;
    last = found; ti = found + 1;
  }
  return Math.max(1, 500 - (last - first + 1));
}
function fuzzySearch(items, query) {
  const q = query.trim().toLowerCase();
  if (!q) return items;
  const scored = [];
  for (const it of items) {
    const hay = `${it.name} ${it.target} ${it.equipment} ${it.group}`;
    let best = fuzzyScore(hay, q);
    const nameScore = fuzzyScore(it.name, q);
    if (nameScore > -1) best = Math.max(best, nameScore + 50);
    if (best > -1) scored.push([best, it]);
  }
  scored.sort((a, b) => b[0] - a[0]);
  return scored.map(x => x[1]);
}

/* ---------- router ---------- */
window.addEventListener('hashchange', render);
window.addEventListener('DOMContentLoaded', render);
let pickCtx = null; // { dayKey, index, dayTitle, returnHash } while the Library is acting as an exercise picker

function render() {
  if (document.startViewTransition) document.startViewTransition(() => renderRoute());
  else renderRoute();
  updateAppBadge();
}

function renderRoute() {
  const hash = location.hash.replace(/^#\/?/, '');
  const [route, arg] = hash.split('/');
  let nav = 'today';
  if (route === 'week') { renderWeek(); nav = 'week'; }
  else if (route === 'plan') { renderPlanEditor(); nav = 'week'; }
  else if (route === 'library') { pickCtx = null; renderLibrary(); nav = 'library'; }
  else if (route === 'pick') { if (!pickCtx) { location.hash = '#/library'; return; } renderLibrary(); nav = ''; }
  else if (route === 'progress') { renderProgress(); nav = 'progress'; }
  else if (route === 'settings') { renderSettings(); nav = 'settings'; }
  else if (route === 'ex') { renderExercise(arg); nav = ''; }
  else if (route === 'session') { renderSession(); nav = ''; }
  else if (route === 'edit') { renderDayEditor(arg); nav = ''; }
  else if (route === 'day') { renderDay(arg, arg === todayKey()); nav = 'week'; }
  else if (route === 'rest') { renderRest(todayKey() === null); nav = 'today'; }
  else if (route === 'today-start') {
    const k = todayKey();
    if (k) { if (!(state.active && state.active.day === k)) startSession(k); renderSession(); }
    else renderRest(true);
    nav = '';
  }
  else { const k = todayKey(); if (k) renderDay(k, true); else renderRest(true); nav = 'today'; }
  $$('#bottom-nav a').forEach(a => a.classList.toggle('active', a.dataset.nav === nav));
  view.scrollTop = 0; window.scrollTo(0, 0);
}

/* ---------- stats strip (Today view header) ---------- */
function statsStripHtml() {
  const streak = currentStreak();
  const done = sessionsThisWeek(), goal = weeklyGoal();
  const pct = goal ? done / goal : 0;
  return `<div class="stats-strip">
    <div class="stat-pill">🔥 <b>${streak}</b><span>day streak</span></div>
    <a class="stat-pill ring-pill" href="#/progress">
      ${svgRing(pct, { size: 34, stroke: 4 })}
      <span>${done}/${goal}<br>this week</span>
    </a>
  </div>`;
}

/* ---------- day (today / week detail) ---------- */
function renderDay(key, isToday) {
  const day = planDayDef(key);
  if (!day) { location.hash = '#/week'; return; }
  const wdLabel = weekdayLabelFor(key);
  const active = state.active && state.active.day === key;
  const customized = !!state.customDays[key];
  let lastSection = null;
  const cards = day.items.map((it, i) => {
    const ex = getEx(it.ex) || { gif: '', equipment: '', target: '' };
    const sec = it.section && it.section !== lastSection ? `<div class="section-label">${esc(it.section)}</div>` : '';
    lastSection = it.section || lastSection;
    return `${sec}
      <a class="card" href="#/ex/${it.ex}">
        <img class="thumb" src="${ex.gif}" alt="" loading="lazy">
        <div class="card-body">
          <div class="card-title">${esc(it.label)}</div>
          <div class="card-meta">${esc(ex.equipment)} · ${esc(ex.target)}</div>
        </div>
        <div class="card-target">${it.sets} × ${esc(it.reps)}</div>
      </a>`;
  }).join('');

  view.innerHTML = `
    <div class="screen-head" style="--accent:${day.accent}">
      <div class="eyebrow" style="color:${day.accent}">${isToday ? 'Today · ' : ''}${wdLabel || ''}${customized ? ' <span class="edited-badge">edited</span>' : ''}</div>
      <h1>${esc(day.title)}</h1>
      <div class="sub">${esc(day.focus)} · ${day.items.length} exercises</div>
    </div>
    ${isToday ? statsStripHtml() : ''}
    ${cards || '<div class="empty"><span class="emoji">🗒️</span>No exercises yet — customize this day to add some.</div>'}
    <a class="btn ghost" href="#/edit/${key}" style="text-align:center;text-decoration:none;color:var(--text);display:block">Customize workout</a>
    <button class="btn" id="start" style="background:${day.accent}" ${day.items.length ? '' : 'disabled'}>${active ? 'Resume workout' : 'Start workout'}</button>`;
  $('#start').onclick = () => {
    if (!active) startSession(key);
    location.hash = '#/session';
  };
}

function renderRest(isToday) {
  view.innerHTML = `
    <div class="screen-head">
      <div class="eyebrow" style="color:${REST_DAY.accent}">${isToday ? 'Today · ' : ''}Rest Day</div>
      <h1>Rest & Recovery</h1>
    </div>
    ${isToday ? statsStripHtml() : ''}
    <div class="rest-hero">
      <div class="emoji">🌴</div>
      <h2>Recharge day</h2>
      <div class="sub">Discipline today, strength tomorrow.</div>
      <ul class="rest-list">${REST_DAY.tips.map(t => `<li>${t}</li>`).join('')}</ul>
    </div>
    <a class="btn ghost" href="#/week" style="text-align:center;text-decoration:none;color:var(--text)">Browse the week</a>`;
}

/* ---------- week ---------- */
function renderWeek() {
  const tk = todayKey();
  const rows = state.week.map((key, i) => {
    const d = planDayDef(key);
    if (!d) return '';
    const wd = (state.startWeekday + i) % 7;
    const customized = !!state.customDays[key];
    return `<a class="day-tile" href="#/day/${key}" style="--day-accent:${d.accent}">
      <div class="day-dot">${WD_SHORT[wd]}</div>
      <div class="card-body">
        <div class="card-title">${esc(d.title)}${customized ? ' <span class="edited-badge">edited</span>' : ''}</div>
        <div class="card-meta">${d.items.length} exercises</div>
      </div>
      ${key === tk ? '<span class="today-badge">TODAY</span>' : '<span class="chev">›</span>'}
    </a>`;
  }).join('');
  const restRows = [];
  for (let i = state.week.length; i < 7; i++) {
    const wd = (state.startWeekday + i) % 7;
    const isTodayRest = tk === null && wd === new Date().getDay();
    restRows.push(`<a class="day-tile" href="#/rest" style="--day-accent:${REST_DAY.accent}">
      <div class="day-dot">${WD_SHORT[wd]}</div>
      <div class="card-body"><div class="card-title">Rest & Recovery</div><div class="card-meta">Recovery</div></div>
      ${isTodayRest ? '<span class="today-badge">TODAY</span>' : '<span class="chev">›</span>'}
    </a>`);
  }
  view.innerHTML = `
    <div class="screen-head"><h1>${state.week.length}-Day Split</h1>
    <div class="sub">Build muscle · Lose fat · Stay consistent</div></div>
    ${rows}${restRows.join('')}
    <a class="btn ghost" href="#/plan" style="text-align:center;text-decoration:none;color:var(--text);display:block">Edit plan</a>`;
}

/* ---------- plan structure editor (days count + start weekday) ---------- */
function renderPlanEditor() {
  const restCount = 7 - state.week.length;
  const rows = state.week.map((key, i) => {
    const d = planDayDef(key) || { title: '(missing)', accent: '#666' };
    const wd = (state.startWeekday + i) % 7;
    return `<div class="plan-row" style="--day-accent:${d.accent}">
      <div class="day-dot">${WD_SHORT[wd]}</div>
      <div class="card-body"><div class="card-title">${esc(d.title)}</div></div>
      <div class="plan-actions">
        <button class="icon-btn up-btn" data-i="${i}" ${i === 0 ? 'disabled' : ''} aria-label="Move up">↑</button>
        <button class="icon-btn down-btn" data-i="${i}" ${i === state.week.length - 1 ? 'disabled' : ''} aria-label="Move down">↓</button>
        <button class="icon-btn remove-day-btn" data-i="${i}" ${state.week.length <= 1 ? 'disabled' : ''} aria-label="Remove">✕</button>
      </div>
    </div>`;
  }).join('');
  view.innerHTML = `
    <a class="back-link" href="#/week">‹ Back</a>
    <div class="screen-head"><h1>Edit Plan</h1><div class="sub">${state.week.length} training days · ${restCount} rest day${restCount === 1 ? '' : 's'}</div></div>
    <div class="section-label">Start day</div>
    <div class="chip-row">${WD_SHORT.map((n, i) => `<button class="wd-chip ${i === state.startWeekday ? 'on' : ''}" data-wd="${i}">${n}</button>`).join('')}</div>
    <div class="section-label">Training days (in order)</div>
    ${rows}
    <button class="btn ghost" id="add-day">+ Add day</button>`;

  $$('.wd-chip').forEach(b => b.onclick = () => { state.startWeekday = +b.dataset.wd; save(); renderPlanEditor(); });
  $$('.up-btn').forEach(b => b.onclick = () => {
    const i = +b.dataset.i; [state.week[i - 1], state.week[i]] = [state.week[i], state.week[i - 1]];
    save(); renderPlanEditor();
  });
  $$('.down-btn').forEach(b => b.onclick = () => {
    const i = +b.dataset.i; [state.week[i + 1], state.week[i]] = [state.week[i], state.week[i + 1]];
    save(); renderPlanEditor();
  });
  $$('.remove-day-btn').forEach(b => b.onclick = () => {
    if (state.week.length <= 1) return;
    state.week.splice(+b.dataset.i, 1); save(); renderPlanEditor();
  });
  $('#add-day').onclick = showAddDayMenu;
}

function showAddDayMenu() {
  const el = document.createElement('div');
  el.className = 'overlay';
  el.innerHTML = `<div class="modal" style="text-align:left">
    <h2 style="text-align:center">Add a day</h2>
    <div class="section-label" style="text-align:center">Templates</div>
    ${SPLIT.map(d => `<button class="btn ghost tmpl-btn">${esc(d.title)}</button>`).join('')}
    <button class="btn ghost" id="blank-day">+ Blank day</button>
    <button class="btn danger" id="cancel-add">Cancel</button>
  </div>`;
  document.body.appendChild(el);
  $$('.tmpl-btn', el).forEach((b, i) => b.onclick = () => { addDayFromTemplate(SPLIT[i].key); el.remove(); });
  $('#blank-day', el).onclick = () => { addBlankDay(); el.remove(); };
  $('#cancel-add', el).onclick = () => el.remove();
}
function addDayFromTemplate(tKey) {
  const tmpl = dayByKey(tKey);
  const key = newDayKey();
  state.customDayDefs[key] = { title: tmpl.title, focus: tmpl.focus, accent: tmpl.accent, items: tmpl.items.map(it => ({ ...it })) };
  state.week.push(key);
  save(); renderPlanEditor();
}
function addBlankDay() {
  const title = prompt('Day name', 'Custom Day') || 'Custom Day';
  const palette = ['#4FA3FF', '#5BD168', '#FFA94D', '#FF6B6B', '#B786F5', '#FFD43B', '#3DDBD9'];
  const key = newDayKey();
  state.customDayDefs[key] = { title, focus: 'Custom workout', accent: palette[state.week.length % palette.length], items: [] };
  state.week.push(key);
  save(); renderPlanEditor();
}

/* ---------- day exercise editor (swap/add/remove/tune) ---------- */
function renderDayEditor(key) {
  const day = planDayDef(key);
  if (!day) { location.hash = '#/week'; return; }
  const customized = !!state.customDays[key];
  const rows = day.items.map((it, i) => {
    const ex = getEx(it.ex) || {};
    return `
    <div class="edit-row">
      <img class="thumb" src="${ex.gif || ''}" alt="">
      <div class="card-body">
        <div class="card-title">${esc(it.label)}</div>
        <div class="edit-controls">
          <div class="stepper small"><button class="sets-minus" data-i="${i}">−</button><span>${it.sets} sets</span><button class="sets-plus" data-i="${i}">+</button></div>
          <input class="reps-input" data-i="${i}" value="${esc(it.reps)}" placeholder="reps">
        </div>
      </div>
      <div class="edit-actions">
        <button class="icon-btn swap-btn" data-i="${i}" title="Swap exercise">⇄</button>
        <button class="icon-btn remove-btn" data-i="${i}" title="Remove">✕</button>
      </div>
    </div>`;
  }).join('');
  view.innerHTML = `
    <a class="back-link" href="#/day/${key}">‹ Back</a>
    <div class="screen-head"><h1>Customize ${esc(day.title)}</h1><div class="sub">Changes save automatically</div></div>
    ${rows || '<div class="empty"><span class="emoji">🗒️</span>No exercises yet — add one below.</div>'}
    <button class="btn ghost" id="add-ex">+ Add exercise</button>
    ${customized ? '<button class="btn danger" id="reset-day">Reset to default</button>' : ''}
    <button class="btn" id="done-edit" style="background:${day.accent}">Done</button>`;

  const mutate = fn => {
    const items = state.customDays[key] ? state.customDays[key] : day.items.map(it => ({ ...it }));
    fn(items);
    state.customDays[key] = items;
    save(); renderDayEditor(key);
  };

  $$('.sets-minus').forEach(b => b.onclick = () => mutate(items => { items[+b.dataset.i].sets = Math.max(1, items[+b.dataset.i].sets - 1); }));
  $$('.sets-plus').forEach(b => b.onclick = () => mutate(items => { items[+b.dataset.i].sets = Math.min(10, items[+b.dataset.i].sets + 1); }));
  $$('.reps-input').forEach(inp => inp.onchange = () => mutate(items => { items[+inp.dataset.i].reps = inp.value || items[+inp.dataset.i].reps; }));
  $$('.remove-btn').forEach(b => b.onclick = () => mutate(items => { items.splice(+b.dataset.i, 1); }));
  $$('.swap-btn').forEach(b => b.onclick = () => {
    pickCtx = { dayKey: key, index: +b.dataset.i, dayTitle: day.title, returnHash: `#/edit/${key}` };
    location.hash = '#/pick';
  });
  $('#add-ex').onclick = () => {
    pickCtx = { dayKey: key, index: null, dayTitle: day.title, returnHash: `#/edit/${key}` };
    location.hash = '#/pick';
  };
  const resetBtn = $('#reset-day');
  if (resetBtn) resetBtn.onclick = () => {
    if (confirm('Reset this day to its default exercises?')) { delete state.customDays[key]; save(); renderDayEditor(key); }
  };
  $('#done-edit').onclick = () => { location.hash = `#/day/${key}`; };
}

function applyPick(ex) {
  if (!ex || !pickCtx) return;
  const { dayKey, index, returnHash } = pickCtx;
  if (!EXERCISES[ex.id]) {
    state.customExercises[ex.id] = { name: ex.name, equipment: ex.equipment, target: ex.target, secondary: ex.secondary, steps: ex.steps, gif: ex.gif };
  }
  const day = planDayDef(dayKey);
  const items = state.customDays[dayKey] ? state.customDays[dayKey] : day.items.map(it => ({ ...it }));
  if (index == null) {
    items.push({ label: titleCase(ex.name), ex: ex.id, sets: 3, reps: '10-12' });
  } else {
    items[index] = { label: titleCase(ex.name), ex: ex.id, sets: items[index].sets, reps: items[index].reps };
  }
  state.customDays[dayKey] = items;
  save();
  pickCtx = null;
  location.hash = returnHash;
}

/* ---------- library tab (all exercises, grouped by muscle) + fuzzy search + picker mode ---------- */
let libQuery = '';
function renderLibrary() {
  const picking = !!pickCtx;
  view.innerHTML = `
    ${picking
      ? `<div class="pick-banner">Choose exercise for <b>${esc(pickCtx.dayTitle)}</b><button id="pick-cancel" class="chip-btn">Cancel</button></div>`
      : `<div class="screen-head"><h1>Library</h1><div class="sub">Every exercise in the dataset — grouped by muscle</div></div>`}
    <div class="search-wrap"><input id="lib-search" class="search-input" type="search" inputmode="search" placeholder="Search exercises…" value="${esc(libQuery)}" autocomplete="off"></div>
    <div id="lib-body"><div class="empty"><span class="emoji">⏳</span>Loading exercise library…</div></div>`;
  if (picking) $('#pick-cancel').onclick = () => { const h = pickCtx.returnHash; pickCtx = null; location.hash = h; };
  const input = $('#lib-search');
  let deb;
  input.oninput = () => { libQuery = input.value; clearTimeout(deb); deb = setTimeout(renderLibraryBody, 100); };
  ensureCatalog().then(renderLibraryBody);
}

function renderLibraryBody() {
  const body = $('#lib-body');
  if (!body) return; // navigated away already
  const all = [...catalogMap.values()];
  if (!all.length) {
    body.innerHTML = `<div class="empty"><span class="emoji">📡</span>Couldn't load the library. Check your connection and reopen this tab.</div>`;
    return;
  }
  const q = libQuery.trim();
  if (q) {
    const results = fuzzySearch(all, q).slice(0, 50);
    body.innerHTML = results.length ? results.map(rowHtml).join('') : `<div class="empty"><span class="emoji">🔍</span>No matches for "${esc(q)}".</div>`;
  } else {
    const groups = {};
    all.forEach(e => { (groups[e.group] || (groups[e.group] = [])).push(e); });
    const order = [...GROUP_ORDER.filter(g => groups[g]), ...Object.keys(groups).filter(g => !GROUP_ORDER.includes(g)).sort()];
    body.innerHTML = order.map(g => `
      <details class="lib-group">
        <summary>${titleCase(g)} <span class="count">${groups[g].length}</span></summary>
        ${groups[g].map(rowHtml).join('')}
      </details>`).join('');
  }
  $$('.lib-row', body).forEach(row => row.onclick = () => {
    const id = row.dataset.id;
    if (pickCtx) applyPick(catalogMap.get(id));
    else location.hash = `#/ex/${id}`;
  });
}
function rowHtml(e) {
  return `<div class="card lib-row" data-id="${e.id}">
    <img class="thumb" src="${e.gif}" alt="" loading="lazy">
    <div class="card-body">
      <div class="card-title" style="text-transform:capitalize">${esc(e.name)}</div>
      <div class="card-meta">${esc(e.equipment)} · ${esc(e.target)}</div>
    </div>
    <span class="chev">›</span>
  </div>`;
}

/* ---------- exercise detail ---------- */
function renderExercise(id) {
  const ex = getEx(id);
  if (ex) { renderExerciseDetail(ex, id); return; }
  view.innerHTML = `<a class="back-link" href="javascript:history.back()">‹ Back</a><div class="empty"><span class="emoji">⏳</span>Loading exercise…</div>`;
  ensureCatalog().then(() => {
    if (!location.hash.endsWith('/ex/' + id)) return; // navigated away while loading
    const ex2 = getEx(id);
    if (ex2) renderExerciseDetail(ex2, id);
    else view.innerHTML = `<a class="back-link" href="javascript:history.back()">‹ Back</a><div class="empty"><span class="emoji">🤷</span>Exercise not found.</div>`;
  });
}
function renderExerciseDetail(ex, id) {
  view.innerHTML = `
    <a class="back-link" href="javascript:history.back()">‹ Back</a>
    <img class="detail-gif" src="${ex.gif}" alt="${esc(ex.name)}">
    <div class="chips">
      <span class="chip">${esc(ex.equipment)}</span>
      <span class="chip">${esc(ex.target)}</span>
      ${[...new Set(ex.secondary || [])].slice(0, 3).map(m => `<span class="chip">${esc(m)}</span>`).join('')}
    </div>
    <div class="screen-head" style="text-align:center;margin-top:6px">
      <h1 style="font-size:20px;text-transform:capitalize">${esc(ex.name)}</h1>
    </div>
    <div class="steps">
      ${(ex.steps || []).map((s, i) => `<div class="step"><b>${i + 1}</b><span>${esc(s)}</span></div>`).join('')}
    </div>
    <div id="similar-slot"></div>`;
  if (id) ensureCatalog().then(() => {
    if (!location.hash.endsWith('/ex/' + id)) return;
    const all = [...catalogMap.values()];
    const similar = all.filter(e => e.id !== id && e.target === ex.target).slice(0, 6);
    const slot = $('#similar-slot');
    if (slot && similar.length) {
      slot.innerHTML = `<div class="section-label">Similar exercises</div>${similar.map(rowHtml).join('')}`;
      $$('.lib-row', slot).forEach(row => row.onclick = () => { location.hash = `#/ex/${row.dataset.id}`; });
    }
  });
}

/* ---------- session tracking ---------- */
function startSession(dayKey) {
  const day = planDayDef(dayKey);
  const sets = day.items.map(it => {
    const prog = progressionFor(it);
    const prev = lastSets(it.ex);
    return Array.from({ length: it.sets }, (_, i) => ({
      w: prog ? prog.weight : (prev && prev[i] ? prev[i].w : (prev ? prev[prev.length - 1].w : '')),
      r: '',
      done: false,
    }));
  });
  state.active = { day: dayKey, startedAt: Date.now(), sets };
  save();
}

function renderSession() {
  const a = state.active;
  if (!a) { location.hash = '#/'; return; }
  const day = planDayDef(a.day);
  requestWakeLock();

  const blocks = day.items.map((it, i) => {
    const ex = getEx(it.ex) || {};
    const prog = progressionFor(it);
    const hasWarmup = a.sets[i].some(s => s.warmup);
    const showRpe = state.settings.rpeEnabled && !it.time;
    const rows = a.sets[i].map((s, j) => `
      <div class="set-row ${showRpe ? 'has-rpe' : ''} ${s.done ? 'done' : ''} ${s.warmup ? 'warmup' : ''}" data-i="${i}" data-j="${j}">
        <div class="set-num">${s.warmup ? 'W' : j - a.sets[i].filter(x => x.warmup).length + 1}</div>
        ${it.time
          ? `<input class="set-input" data-f="r" type="number" inputmode="numeric" placeholder="sec" value="${s.r}">
             <div class="set-num">—</div>`
          : `<div class="w-input-wrap"><input class="set-input" data-f="w" type="number" inputmode="decimal" step="0.5" placeholder="${unitLabel()}" value="${kgToDisp(s.w)}"><button class="plate-btn" data-i="${i}" data-j="${j}" title="Plate calculator">🏋️</button></div>
             <input class="set-input" data-f="r" type="number" inputmode="numeric" placeholder="${esc(it.reps)}" value="${s.r}">`}
        ${showRpe ? `<select class="rpe-select" data-i="${i}" data-j="${j}"><option value="">RPE</option>${[6, 6.5, 7, 7.5, 8, 8.5, 9, 9.5, 10].map(v => `<option value="${v}" ${s.rpe === v ? 'selected' : ''}>${v}</option>`).join('')}</select>` : ''}
        <button class="set-check ${s.done ? 'on' : ''}" aria-label="done">✓</button>
      </div>`).join('');
    const hint = prog && prog.hint;
    return `
      <div class="session-ex">
        <div class="session-ex-head" data-ex="${it.ex}">
          <img class="thumb" src="${ex.gif || ''}" alt="">
          <div class="card-body">
            <div class="card-title">${esc(it.label)}</div>
            <div class="card-meta">${it.sets} × ${esc(it.reps)}${it.time ? '' : ' · tap for form'}</div>
          </div>
          <span class="chev">›</span>
        </div>
        ${hint ? `<div class="hint ${prog.type === 'deload' ? 'hint-deload' : ''}">${prog.type === 'deload' ? '⚠️' : '📈'} ${esc(hint)}</div>` : ''}
        <div class="set-rows">
          <div class="set-head-row ${showRpe ? 'has-rpe' : ''}"><span>#</span><span>${it.time ? 'seconds' : unitLabel()}</span><span>${it.time ? '' : 'reps'}</span>${showRpe ? '<span>rpe</span>' : ''}<span>done</span></div>
          ${rows}
        </div>
        ${!it.time && !hasWarmup ? `<button class="btn ghost warmup-btn" data-i="${i}" style="margin:0 14px 12px;width:calc(100% - 28px)">+ Warm-up sets</button>` : ''}
      </div>`;
  }).join('');

  const total = a.sets.flat().length;
  const done = a.sets.flat().filter(s => s.done).length;

  view.innerHTML = `
    <div class="progress-track">
      <div class="screen-head" style="margin-bottom:10px">
        <div class="eyebrow" style="color:${day.accent}">Workout in progress</div>
        <h1 style="font-size:20px">${esc(day.title)}</h1>
      </div>
      <div class="progress-bar"><div class="progress-fill" id="pfill" style="width:${(done / total) * 100}%"></div></div>
    </div>
    ${blocks}
    <button class="btn" id="finish" style="background:${day.accent}">Finish workout</button>
    <button class="btn danger" id="discard">Discard</button>`;

  $$('.session-ex-head').forEach(el => el.onclick = () => { location.hash = `#/ex/${el.dataset.ex}`; });
  $$('.set-input').forEach(inp => inp.onchange = () => {
    const row = inp.closest('.set-row');
    const s = a.sets[+row.dataset.i][+row.dataset.j];
    if (inp.dataset.f === 'w') s.w = inp.value === '' ? '' : dispToKg(+inp.value);
    else s.r = inp.value === '' ? '' : +inp.value;
    save();
  });
  $$('.rpe-select').forEach(sel => sel.onchange = () => {
    a.sets[+sel.dataset.i][+sel.dataset.j].rpe = sel.value === '' ? undefined : +sel.value;
    save();
  });
  $$('.plate-btn').forEach(btn => btn.onclick = () => {
    const row = btn.closest('.set-row');
    const inp = row.querySelector('[data-f="w"]');
    showPlateModal(+inp.value || 0);
  });
  $$('.warmup-btn').forEach(btn => btn.onclick = () => {
    const i = +btn.dataset.i;
    const working = a.sets[i].find(s => s.w) || {};
    if (!working.w) { alert('Enter a working weight first, then add warm-ups.'); return; }
    const pct = [[0.4, 10], [0.6, 6], [0.8, 3]];
    const warm = pct.map(([p, reps]) => ({ w: roundToStep(working.w * p), r: reps, done: false, warmup: true }));
    a.sets[i] = [...warm, ...a.sets[i]];
    save(); renderSession();
  });
  $$('.set-check').forEach(btn => btn.onclick = () => {
    const row = btn.closest('.set-row');
    const i = +row.dataset.i, j = +row.dataset.j;
    const s = a.sets[i][j];
    s.done = !s.done;
    if (s.done) {
      if (s.r === '' || s.r == null) {
        const it = day.items[i];
        s.r = parseInt(String(it.reps), 10) || 0;
        row.querySelector('[data-f="r"]').value = s.r;
      }
      if (!s.warmup && s.w && updatePRFromSet(day.items[i].ex, s.w, s.r, Date.now())) {
        celebratePR();
      }
      startRest();
      if (navigator.vibrate) navigator.vibrate(30);
    }
    save();
    btn.classList.toggle('on', s.done);
    row.classList.toggle('done', s.done);
    const flat = a.sets.flat();
    $('#pfill').style.width = (flat.filter(x => x.done).length / flat.length) * 100 + '%';
  });
  $('#finish').onclick = finishSession;
  $('#discard').onclick = () => {
    if (confirm('Discard this workout? Logged sets will be lost.')) {
      state.active = null; save(); stopRest(); rebuildPRs(); location.hash = '#/';
    }
  };
}

function finishSession() {
  const a = state.active;
  const day = planDayDef(a.day);
  const log = [];
  day.items.forEach((it, i) => a.sets[i].forEach(s => {
    if (s.done && !s.warmup) log.push({ ex: it.ex, label: it.label, w: +s.w || 0, r: +s.r || 0, time: !!it.time, rpe: s.rpe });
  }));
  if (!log.length && !confirm('No sets completed — finish anyway?')) return;
  const session = { day: a.day, title: day.title, startedAt: a.startedAt, endedAt: Date.now(), log };
  session.calories = sessionCalories(session);
  state.sessions.push(session);
  state.active = null;
  save(); stopRest(); releaseWakeLock();
  const unlocked = checkAchievements(session);
  showSummary(session, day, unlocked);
}

function showSummary(s, day, unlocked) {
  const volume = s.log.filter(l => !l.time).reduce((t, l) => t + l.w * l.r, 0);
  const mins = Math.max(1, Math.round((s.endedAt - s.startedAt) / 60000));
  const el = document.createElement('div');
  el.className = 'overlay';
  el.innerHTML = `
    <div class="modal">
      <div class="emoji">💪</div>
      <h2>Workout complete!</h2>
      <div class="sub">${esc(day.title)}</div>
      <div class="stats">
        <div><b>${s.log.length}</b><span>sets</span></div>
        <div><b>${Math.round(kgToDisp(volume))}</b><span>${unitLabel()} volume</span></div>
        <div><b>${s.calories || 0}</b><span>kcal</span></div>
        <div><b>${mins}</b><span>min</span></div>
      </div>
      ${unlocked && unlocked.length ? `<div class="unlock-list">${unlocked.map(u => `<div class="unlock-row">${u.emoji} Achievement unlocked: <b>${esc(u.name)}</b></div>`).join('')}</div>` : ''}
      <button class="btn" id="share-sum">Share</button>
      <button class="btn ghost" id="close-sum">Done</button>
    </div>`;
  document.body.appendChild(el);
  $('#close-sum', el).onclick = () => { el.remove(); location.hash = '#/progress'; };
  $('#share-sum', el).onclick = () => shareSummary(s, day, mins);
  if (navigator.vibrate) navigator.vibrate([60, 60, 120]);
  confettiBurst();
  unlocked.forEach(u => queueToast(`Achievement: ${u.name}`, u.emoji));
}

async function shareSummary(s, day, mins) {
  const volume = Math.round(kgToDisp(s.log.filter(l => !l.time).reduce((t, l) => t + l.w * l.r, 0)));
  const text = `💪 ${day.title}\n${s.log.length} sets · ${volume} ${unitLabel()} volume · ${s.calories || 0} kcal · ${mins} min\n🔥 ${currentStreak()} day streak — Gym Buddy`;
  try {
    if (navigator.share) { await navigator.share({ title: 'Gym Buddy workout', text }); return; }
  } catch (e) { /* user cancelled or unsupported — fall through to clipboard */ }
  try { await navigator.clipboard.writeText(text); queueToast('Summary copied to clipboard', '📋'); }
  catch (e) { alert(text); }
}

/* ---------- progress tab (charts, PRs, achievements, body weight, session history) ---------- */
function renderProgress() {
  const streak = currentStreak();
  const doneWk = sessionsThisWeek(), goal = weeklyGoal();

  // weekly volume, last 8 weeks
  const weekLabels = [], weekVols = [];
  for (let i = 7; i >= 0; i--) {
    const start = weekWindowStart() - i * 7 * 864e5;
    const end = start + 7 * 864e5;
    const vol = state.sessions.filter(s => s.endedAt >= start && s.endedAt < end)
      .reduce((t, s) => t + s.log.filter(l => !l.time).reduce((tt, l) => tt + l.w * l.r, 0), 0);
    weekVols.push(Math.round(kgToDisp(vol)));
    weekLabels.push(i === 0 ? 'This' : `-${i}w`);
  }

  // muscle balance this week
  const start = weekWindowStart();
  const muscleCounts = {};
  state.sessions.filter(s => s.endedAt >= start).forEach(s => s.log.forEach(l => {
    const g = (getEx(l.ex) || {}).target || 'other';
    muscleCounts[g] = (muscleCounts[g] || 0) + 1;
  }));
  const muscleEntries = Object.entries(muscleCounts).sort((a, b) => b[1] - a[1]).slice(0, 6);

  // streak heatmap
  const dayCounts = {};
  state.sessions.forEach(s => { const k = new Date(s.endedAt).toDateString(); dayCounts[k] = (dayCounts[k] || 0) + 1; });

  // 1RM trend for most-logged exercise
  const exCounts = {};
  state.sessions.forEach(s => s.log.forEach(l => { if (!l.time) exCounts[l.ex] = (exCounts[l.ex] || 0) + 1; }));
  const topExId = Object.entries(exCounts).sort((a, b) => b[1] - a[1])[0]?.[0];
  let rmChartHtml = '<div class="empty small"><span class="emoji">📉</span>Log a few sessions to see 1RM trends.</div>';
  if (topExId) {
    const points = state.sessions
      .filter(s => s.log.some(l => l.ex === topExId))
      .map(s => {
        const best = Math.max(...s.log.filter(l => l.ex === topExId).map(l => oneRM(l.w, l.r)));
        return { y: Math.round(kgToDisp(best)), label: new Date(s.endedAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric' }) };
      });
    const exName = (getEx(topExId) || {}).name || topExId;
    rmChartHtml = `<div class="chart-title">${esc(titleCase(exName))} — est. 1RM (${unitLabel()})</div>${svgLineChart(points)}`;
  }

  // PR list
  const prEntries = Object.entries(prMap).sort((a, b) => b[1].achievedAt - a[1].achievedAt).slice(0, 12);
  const prHtml = prEntries.length ? prEntries.map(([id, pr]) => `
    <div class="card">
      <div class="card-body">
        <div class="card-title" style="text-transform:capitalize">${esc(titleCase((getEx(id) || {}).name || id))}</div>
        <div class="card-meta">Best ${round1(kgToDisp(pr.bestWeight))} ${unitLabel()} · est. 1RM ${Math.round(kgToDisp(pr.bestOneRm))} ${unitLabel()}</div>
      </div>
    </div>`).join('') : `<div class="empty small"><span class="emoji">🏆</span>No PRs yet — log a session to start tracking.</div>`;

  // achievements grid
  const achHtml = ACHIEVEMENTS.map(a => {
    const unlocked = !!state.achievements[a.id];
    return `<div class="badge ${unlocked ? '' : 'locked'}" title="${esc(a.name)}"><span class="badge-emoji">${a.emoji}</span><span class="badge-name">${esc(a.name)}</span></div>`;
  }).join('');

  // body weight trend
  const bwPoints = state.bodyMetrics.slice(-12).map(m => ({ y: round1(kgToDisp(m.kg)), label: new Date(m.ts).toLocaleDateString() }));
  const bwChart = bwPoints.length >= 2 ? svgLineChart(bwPoints, { color: 'var(--success)' }) : `<div class="empty small"><span class="emoji">⚖️</span>Log your weight to see a trend.</div>`;

  // session history
  const fmt = ts => new Date(ts).toLocaleDateString(undefined, { weekday: 'short', month: 'short', day: 'numeric' });
  const histHtml = state.sessions.length ? [...state.sessions].reverse().slice(0, 30).map(s => {
    const volume = s.log.filter(l => !l.time).reduce((t, l) => t + l.w * l.r, 0);
    const mins = Math.max(1, Math.round((s.endedAt - s.startedAt) / 60000));
    const d = planDayDef(s.day);
    return `<div class="card hist-card" style="border-left:4px solid ${d ? d.accent : '#666'}">
      <div class="card-body">
        <div class="hist-top"><span class="hist-title">${esc(s.title)}</span><span class="hist-date">${fmt(s.endedAt)}</span></div>
        <div class="hist-stats">
          <span><b>${s.log.length}</b> sets</span>
          <span><b>${Math.round(kgToDisp(volume))}</b> ${unitLabel()}</span>
          <span><b>${s.calories || 0}</b> kcal</span>
          <span><b>${mins}</b> min</span>
        </div>
      </div>
    </div>`;
  }).join('') : `<div class="empty small"><span class="emoji">🏋️</span>No workouts yet.</div>`;

  view.innerHTML = `
    <div class="screen-head"><h1>Progress</h1></div>
    <div class="stats-strip">
      <div class="stat-pill">🔥 <b>${streak}</b><span>day streak</span></div>
      <div class="stat-pill ring-pill">${svgRing(goal ? doneWk / goal : 0, { size: 34, stroke: 4 })}<span>${doneWk}/${goal}<br>this week</span></div>
    </div>

    <div class="section-label">Weekly volume (${unitLabel()})</div>
    <div class="chart-card">${svgBarChart(weekLabels, weekVols)}</div>

    <div class="section-label">Est. 1RM trend</div>
    <div class="chart-card">${rmChartHtml}</div>

    ${muscleEntries.length ? `<div class="section-label">Muscle balance this week</div>
    <div class="chart-card">${svgBarChart(muscleEntries.map(e => titleCase(e[0]).slice(0, 6)), muscleEntries.map(e => e[1]))}</div>` : ''}

    <div class="section-label">Consistency (12 weeks)</div>
    <div class="chart-card heatmap-wrap">${svgHeatmap(12, dayCounts)}</div>

    <div class="section-label">Body weight</div>
    <div class="chart-card">${bwChart}</div>
    <button class="btn ghost" id="log-weight">+ Log weight</button>

    <div class="section-label">Personal records</div>
    ${prHtml}

    <div class="section-label">Achievements</div>
    <div class="badge-grid">${achHtml}</div>

    <div class="section-label">History</div>
    ${histHtml}`;

  $('#log-weight').onclick = showLogWeightModal;
}

function showLogWeightModal() {
  const el = document.createElement('div');
  el.className = 'overlay';
  const current = state.bodyMetrics.length ? round1(kgToDisp(state.bodyMetrics[state.bodyMetrics.length - 1].kg)) : '';
  el.innerHTML = `<div class="modal">
    <h2>Log body weight</h2>
    <input id="weight-input" class="search-input" type="number" inputmode="decimal" step="0.1" placeholder="Weight in ${unitLabel()}" value="${current}" style="text-align:center;margin:16px 0">
    <button class="btn" id="save-weight">Save</button>
    <button class="btn ghost" id="cancel-weight">Cancel</button>
  </div>`;
  document.body.appendChild(el);
  $('#weight-input', el).focus();
  $('#cancel-weight', el).onclick = () => el.remove();
  $('#save-weight', el).onclick = () => {
    const v = +$('#weight-input', el).value;
    if (!v || v <= 0) { alert('Enter a valid weight.'); return; }
    state.bodyMetrics.push({ ts: Date.now(), kg: dispToKg(v) });
    save(); el.remove(); renderProgress();
  };
}

/* ---------- plate calculator ---------- */
function showPlateModal(currentDisp) {
  const el = document.createElement('div');
  el.className = 'overlay';
  const renderPlateResult = () => {
    const bar = +$('#bar-input', el).value || state.settings.barKg;
    const target = +$('#target-input', el).value || currentDisp;
    const { plates, warning } = calcPlates(target, bar);
    $('#plate-result', el).innerHTML = plates.length
      ? `<div class="plate-list">${plates.map(p => `<span class="plate-chip">${p}</span>`).join(' + ')}</div><div class="plate-note">per side</div>`
      : `<div class="plate-note">Just the bar.</div>`;
    $('#plate-warning', el).textContent = warning || '';
  };
  el.innerHTML = `<div class="modal">
    <h2>🏋️ Plate calculator</h2>
    <div class="plate-row"><label>Target (${unitLabel()})</label><input id="target-input" class="search-input" type="number" value="${currentDisp || ''}"></div>
    <div class="plate-row"><label>Bar (${unitLabel()})</label><input id="bar-input" class="search-input" type="number" value="${kgToDisp(state.settings.barKg)}"></div>
    <div id="plate-result" style="margin:14px 0"></div>
    <div id="plate-warning" class="hint-deload" style="text-align:center"></div>
    <button class="btn ghost" id="close-plate">Close</button>
  </div>`;
  document.body.appendChild(el);
  $('#target-input', el).oninput = renderPlateResult;
  $('#bar-input', el).oninput = () => { state.settings.barKg = dispToKg(+$('#bar-input', el).value || 20); save(); renderPlateResult(); };
  $('#close-plate', el).onclick = () => el.remove();
  renderPlateResult();
}

/* ---------- settings ---------- */
function renderSettings() {
  view.innerHTML = `
    <div class="screen-head"><h1>Settings</h1></div>
    <div class="setting-row">
      <div><div class="lab">Rest timer</div><div class="desc">Countdown after each completed set</div></div>
      <div class="stepper">
        <button id="rest-minus">−</button>
        <span id="rest-val">${state.settings.restSec}s</span>
        <button id="rest-plus-set">+</button>
      </div>
    </div>
    <div class="setting-row">
      <div><div class="lab">Units</div><div class="desc">Weight display &amp; input</div></div>
      <div class="chip-row" style="margin:0"><button class="wd-chip unit-chip ${state.settings.units === 'kg' ? 'on' : ''}" data-u="kg">kg</button><button class="wd-chip unit-chip ${state.settings.units === 'lb' ? 'on' : ''}" data-u="lb">lb</button></div>
    </div>
    <div class="setting-row">
      <div><div class="lab">Weekly goal</div><div class="desc">Sessions per week (default: plan's training days)</div></div>
      <div class="stepper">
        <button id="goal-minus">−</button>
        <span id="goal-val">${weeklyGoal()}</span>
        <button id="goal-plus">+</button>
      </div>
    </div>
    <div class="setting-row">
      <div><div class="lab">Log RPE</div><div class="desc">Rate of perceived exertion per set</div></div>
      <label class="switch"><input type="checkbox" id="rpe-toggle" ${state.settings.rpeEnabled ? 'checked' : ''}><span class="switch-track"></span></label>
    </div>
    <div class="setting-row">
      <div><div class="lab">Rest-end notification</div><div class="desc">Notify when the timer ends (tab in background)</div></div>
      <label class="switch"><input type="checkbox" id="notify-toggle" ${state.settings.restNotify ? 'checked' : ''}><span class="switch-track"></span></label>
    </div>
    ${deferredInstallPrompt ? '<button class="btn ghost" id="install-settings-btn">📲 Install app</button>' : ''}
    <button class="btn ghost" id="export">Export data (JSON backup)</button>
    <button class="btn ghost" id="export-csv">Export history (CSV)</button>
    <button class="btn ghost" id="import">Import backup</button>
    <input type="file" id="import-file" accept=".json,application/json" hidden>
    <button class="btn danger" id="wipe">Clear all data</button>
    <p class="about">
      <b>Gym Buddy</b> — your workout plan, offline.<br>
      Exercise illustrations & instructions are used for educational, non-commercial purposes.<br>
      Progression: hit the top of the rep range on all sets with good form → increase weight ~2.5% next session; miss the bottom of the range → deload ~10%.<br>
      Calorie figures are MET-based estimates.
    </p>`;
  const bump = d => {
    state.settings.restSec = Math.min(300, Math.max(15, state.settings.restSec + d));
    save(); $('#rest-val').textContent = state.settings.restSec + 's';
  };
  $('#rest-minus').onclick = () => bump(-15);
  $('#rest-plus-set').onclick = () => bump(15);
  $$('.unit-chip').forEach(b => b.onclick = () => { state.settings.units = b.dataset.u; save(); renderSettings(); });
  $('#goal-minus').onclick = () => { state.settings.weeklyGoal = Math.max(1, weeklyGoal() - 1); save(); $('#goal-val').textContent = weeklyGoal(); };
  $('#goal-plus').onclick = () => { state.settings.weeklyGoal = Math.min(14, weeklyGoal() + 1); save(); $('#goal-val').textContent = weeklyGoal(); };
  $('#rpe-toggle').onchange = e => { state.settings.rpeEnabled = e.target.checked; save(); };
  $('#notify-toggle').onchange = async e => {
    if (e.target.checked && 'Notification' in window) {
      const perm = await Notification.requestPermission();
      state.settings.restNotify = perm === 'granted';
      e.target.checked = state.settings.restNotify;
    } else state.settings.restNotify = false;
    save();
  };
  const installBtn = $('#install-settings-btn');
  if (installBtn) installBtn.onclick = doInstall;
  $('#export').onclick = () => {
    const blob = new Blob([JSON.stringify(state, null, 2)], { type: 'application/json' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = `gymbuddy-backup-${new Date().toISOString().slice(0, 10)}.json`;
    a.click(); URL.revokeObjectURL(a.href);
  };
  $('#export-csv').onclick = () => {
    const blob = new Blob([toCSV()], { type: 'text/csv' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = `gymbuddy-history-${new Date().toISOString().slice(0, 10)}.csv`;
    a.click(); URL.revokeObjectURL(a.href);
  };
  $('#import').onclick = () => $('#import-file').click();
  $('#import-file').onchange = e => {
    const file = e.target.files[0];
    if (!file) return;
    file.text().then(t => {
      const s = JSON.parse(t);
      if (s && s.v === 1 && Array.isArray(s.sessions)) {
        localStorage.setItem(STORE_KEY, JSON.stringify(s));
        state = load(); // backfills defaults for any fields missing from an older backup
        rebuildPRs();
        alert('Backup restored ✔'); render();
      } else alert('Not a valid Gym Buddy backup.');
    }).catch(() => alert('Could not read that file.'));
  };
  $('#wipe').onclick = () => {
    if (confirm('Delete ALL workout history and settings?')) {
      localStorage.removeItem(STORE_KEY); state = load(); rebuildPRs(); render();
    }
  };
}

/* ---------- rest timer ---------- */
let restEnd = 0, restTotal = 0, restTimer = null;
const restBar = $('#rest-bar');

function startRest() {
  restTotal = state.settings.restSec;
  restEnd = Date.now() + restTotal * 1000;
  restBar.classList.remove('hidden');
  if (!restTimer) restTimer = setInterval(tickRest, 250);
  tickRest();
}
function stopRest() {
  clearInterval(restTimer); restTimer = null;
  restBar.classList.add('hidden');
}
function tickRest() {
  const left = Math.max(0, restEnd - Date.now());
  const s = Math.ceil(left / 1000);
  $('#rest-time').textContent = `${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')}`;
  $('#rest-progress').style.transform = `scaleX(${left / (restTotal * 1000)})`;
  if (left <= 0) {
    stopRest();
    if (navigator.vibrate) navigator.vibrate([120, 80, 120]);
    beep();
    if (state.settings.restNotify && document.visibilityState === 'hidden' && 'serviceWorker' in navigator) {
      navigator.serviceWorker.ready.then(reg => reg.showNotification('Rest over 💪', {
        body: 'Back to it — next set is ready.', icon: 'icons/icon-192.png', tag: 'gymbuddy-rest',
      })).catch(() => {});
    }
  }
}
$('#rest-skip').onclick = stopRest;
$('#rest-plus').onclick = () => { restEnd += 30000; restTotal += 30; };

function beep() {
  try {
    const ctx = new (window.AudioContext || window.webkitAudioContext)();
    const osc = ctx.createOscillator(), gain = ctx.createGain();
    osc.connect(gain); gain.connect(ctx.destination);
    osc.frequency.value = 880; gain.gain.value = 0.15;
    osc.start(); osc.stop(ctx.currentTime + 0.25);
    osc.onended = () => ctx.close();
  } catch (e) { /* audio not available */ }
}

/* ---------- wake lock (screen stays on during a session) ---------- */
let wakeLock = null;
async function requestWakeLock() {
  try { wakeLock = await navigator.wakeLock.request('screen'); } catch (e) { /* unsupported */ }
}
function releaseWakeLock() {
  if (wakeLock) { wakeLock.release().catch(() => {}); wakeLock = null; }
}
document.addEventListener('visibilitychange', () => {
  if (document.visibilityState === 'visible' && state.active && location.hash.startsWith('#/session')) requestWakeLock();
});

/* ---------- toasts + confetti ---------- */
let toastQueue = [], toastBusy = false;
function queueToast(msg, emoji) {
  toastQueue.push({ msg, emoji });
  if (!toastBusy) processToastQueue();
}
function processToastQueue() {
  if (!toastQueue.length) { toastBusy = false; return; }
  toastBusy = true;
  const { msg, emoji } = toastQueue.shift();
  const el = document.createElement('div');
  el.className = 'toast';
  el.innerHTML = `<span>${emoji}</span> ${esc(msg)}`;
  document.body.appendChild(el);
  requestAnimationFrame(() => el.classList.add('show'));
  setTimeout(() => { el.classList.remove('show'); setTimeout(() => el.remove(), 300); }, 2200);
  setTimeout(processToastQueue, 1800);
}
function celebratePR() {
  queueToast('New PR!', '🏆');
  confettiBurst();
  if (navigator.vibrate) navigator.vibrate([40, 40, 40, 40, 80]);
}
function confettiBurst() {
  const canvas = $('#confetti-canvas');
  if (!canvas) return;
  const ctx = canvas.getContext('2d');
  const dpr = window.devicePixelRatio || 1;
  canvas.width = innerWidth * dpr; canvas.height = innerHeight * dpr;
  canvas.style.width = innerWidth + 'px'; canvas.style.height = innerHeight + 'px';
  ctx.scale(dpr, dpr);
  const colors = ['#4FA3FF', '#5BD168', '#FFA94D', '#FF6B6B', '#B786F5', '#FFD43B'];
  const particles = Array.from({ length: 60 }, () => ({
    x: innerWidth / 2, y: innerHeight / 3,
    vx: (Math.random() - 0.5) * 12, vy: (Math.random() - 1.4) * 10,
    size: 4 + Math.random() * 5, color: colors[Math.floor(Math.random() * colors.length)],
    rot: Math.random() * Math.PI, vr: (Math.random() - 0.5) * 0.3, life: 1,
  }));
  const start = performance.now();
  function tick(now) {
    const dt = Math.min(32, now - (tick.last || now)); tick.last = now;
    ctx.clearRect(0, 0, innerWidth, innerHeight);
    let alive = false;
    for (const p of particles) {
      if (p.life <= 0) continue;
      p.vy += 0.35; p.x += p.vx * (dt / 16); p.y += p.vy * (dt / 16); p.rot += p.vr;
      p.life -= 0.012;
      if (p.life > 0) {
        alive = true;
        ctx.save(); ctx.globalAlpha = Math.max(0, p.life); ctx.translate(p.x, p.y); ctx.rotate(p.rot);
        ctx.fillStyle = p.color; ctx.fillRect(-p.size / 2, -p.size / 2, p.size, p.size);
        ctx.restore();
      }
    }
    if (alive && now - start < 2000) requestAnimationFrame(tick);
    else ctx.clearRect(0, 0, innerWidth, innerHeight);
  }
  requestAnimationFrame(tick);
}

/* ---------- app badge (best-effort, Chromium) ---------- */
function updateAppBadge() {
  if (!navigator.setAppBadge) return;
  const k = todayKey();
  if (!k) { navigator.clearAppBadge().catch(() => {}); return; }
  const doneToday = state.sessions.some(s => new Date(s.endedAt).toDateString() === new Date().toDateString());
  if (doneToday) navigator.clearAppBadge().catch(() => {});
  else navigator.setAppBadge(1).catch(() => {});
}

/* ---------- install prompt (native-app behavior) ---------- */
let deferredInstallPrompt = null;
function isStandalone() { return window.matchMedia('(display-mode: standalone)').matches || window.navigator.standalone === true; }
function isIOS() { return /iphone|ipad|ipod/i.test(navigator.userAgent) && !window.MSStream; }

window.addEventListener('beforeinstallprompt', e => {
  e.preventDefault();
  deferredInstallPrompt = e;
  maybeShowInstallBanner();
});
window.addEventListener('appinstalled', () => {
  deferredInstallPrompt = null;
  hideInstallBanner();
  state.installDismissedAt = 'installed'; save();
});
window.addEventListener('load', () => setTimeout(maybeShowInstallBanner, 1500));

function maybeShowInstallBanner() {
  if (isStandalone()) return;
  if (state.installDismissedAt === 'installed') return;
  if (typeof state.installDismissedAt === 'number' && Date.now() - state.installDismissedAt < 14 * 864e5) return;
  if (!deferredInstallPrompt && !isIOS()) return;
  showInstallBanner();
}
function showInstallBanner() {
  if ($('#install-banner')) return;
  const el = document.createElement('div');
  el.id = 'install-banner';
  el.className = 'install-banner';
  if (deferredInstallPrompt) {
    el.innerHTML = `<span>📲 Install Gym Buddy for the full app experience</span>
      <div class="install-actions"><button id="install-go" class="chip-btn accent">Install</button><button id="install-dismiss" class="chip-btn">✕</button></div>`;
  } else if (isIOS()) {
    el.innerHTML = `<span>📲 Install: tap Share, then "Add to Home Screen"</span>
      <div class="install-actions"><button id="install-dismiss" class="chip-btn">✕</button></div>`;
  } else return;
  document.body.appendChild(el);
  $('#install-dismiss', el).onclick = () => { state.installDismissedAt = Date.now(); save(); el.remove(); };
  const goBtn = $('#install-go', el);
  if (goBtn) goBtn.onclick = () => { el.remove(); doInstall(); };
}
function hideInstallBanner() { const el = $('#install-banner'); if (el) el.remove(); }
async function doInstall() {
  if (!deferredInstallPrompt) return;
  deferredInstallPrompt.prompt();
  const choice = await deferredInstallPrompt.userChoice;
  deferredInstallPrompt = null;
  if (choice.outcome !== 'accepted') { state.installDismissedAt = Date.now(); save(); }
}
