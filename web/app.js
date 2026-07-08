/* Gym Buddy — vanilla JS PWA. State lives in localStorage. */
'use strict';

const STORE_KEY = 'gymbuddy.v1';
const REST_DAY = {
  key: 'sun', title: 'Rest & Recovery', weekday: 0, accent: '#3DDBD9',
  tips: ['Complete rest', 'Light walk or yoga', 'Focus on mobility', 'Hydrate & eat clean', 'Prepare for next week'],
};

/* ---------- store ---------- */
function load() {
  try {
    const s = JSON.parse(localStorage.getItem(STORE_KEY));
    if (s && s.v === 1) return s;
  } catch (e) { /* corrupted → start fresh */ }
  return { v: 1, sessions: [], active: null, settings: { restSec: 90 } };
}
function save() { localStorage.setItem(STORE_KEY, JSON.stringify(state)); }
let state = load();

const $ = (sel, el) => (el || document).querySelector(sel);
const $$ = (sel, el) => [...(el || document).querySelectorAll(sel)];
const view = $('#view');
const dayByKey = k => SPLIT.find(d => d.key === k);
const esc = s => String(s).replace(/[&<>"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));

function todayDay() {
  const wd = new Date().getDay();
  return SPLIT.find(d => d.weekday === wd) || null; // null → rest day
}

/* Last logged sets for an exercise across finished sessions (newest first).
   Session logs only ever contain completed sets. */
function lastSets(exId) {
  for (let i = state.sessions.length - 1; i >= 0; i--) {
    const sets = (state.sessions[i].log || []).filter(l => l.ex === exId);
    if (sets.length) return sets;
  }
  return null;
}

/* Progression hint per the split's rule: top of rep range on all sets → +2.5%. */
function progressionHint(item) {
  const prev = lastSets(item.ex);
  if (!prev || item.time) return null;
  const top = parseInt(String(item.reps).split('-').pop(), 10);
  if (!top) return null;
  const allTop = prev.every(s => s.r >= top);
  const w = Math.max(...prev.map(s => s.w || 0));
  if (allTop && w > 0) {
    const next = Math.round(w * 1.025 * 2) / 2;
    return `Hit ${top}+ reps on every set last time — try ${next} kg today`;
  }
  return null;
}

/* ---------- router ---------- */
window.addEventListener('hashchange', render);
window.addEventListener('DOMContentLoaded', render);

function render() {
  stopTicker();
  const hash = location.hash.replace(/^#\/?/, '');
  const [route, arg] = hash.split('/');
  let nav = 'today';
  if (route === 'week') { renderWeek(); nav = 'week'; }
  else if (route === 'history') { renderHistory(); nav = 'history'; }
  else if (route === 'settings') { renderSettings(); nav = 'settings'; }
  else if (route === 'ex') { renderExercise(arg); nav = ''; }
  else if (route === 'session') { renderSession(); nav = ''; }
  else if (route === 'day') { renderDay(dayByKey(arg)); nav = 'week'; }
  else { renderDay(todayDay(), true); }
  $$('#bottom-nav a').forEach(a => a.classList.toggle('active', a.dataset.nav === nav));
  view.scrollTop = 0; window.scrollTo(0, 0);
}

/* ---------- day (today / week detail) ---------- */
function renderDay(day, isToday) {
  if (!day) return renderRest(isToday);
  const dayName = { mon: 'Monday', tue: 'Tuesday', wed: 'Wednesday', thu: 'Thursday', fri: 'Friday', sat: 'Saturday' }[day.key];
  const active = state.active && state.active.day === day.key;
  let lastSection = null;
  const cards = day.items.map((it, i) => {
    const ex = EXERCISES[it.ex];
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
      <div class="eyebrow" style="color:${day.accent}">${isToday ? 'Today · ' : ''}${dayName}</div>
      <h1>${esc(day.title)}</h1>
      <div class="sub">${esc(day.focus)} · ${day.items.length} exercises</div>
    </div>
    ${cards}
    <button class="btn" id="start" style="background:${day.accent}">${active ? 'Resume workout' : 'Start workout'}</button>`;
  $('#start').onclick = () => {
    if (!active) startSession(day.key);
    location.hash = '#/session';
  };
}

function renderRest(isToday) {
  view.innerHTML = `
    <div class="screen-head">
      <div class="eyebrow" style="color:${REST_DAY.accent}">${isToday ? 'Today · ' : ''}Sunday</div>
      <h1>Rest & Recovery</h1>
    </div>
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
  const names = { mon: 'Mon', tue: 'Tue', wed: 'Wed', thu: 'Thu', fri: 'Fri', sat: 'Sat', sun: 'Sun' };
  const wd = new Date().getDay();
  const tiles = [...SPLIT, REST_DAY].map(d => `
    <a class="day-tile" href="${d.key === 'sun' ? '#/day/sun' : `#/day/${d.key}`}" style="--day-accent:${d.accent}">
      <div class="day-dot">${names[d.key]}</div>
      <div class="card-body">
        <div class="card-title">${esc(d.title)}</div>
        <div class="card-meta">${d.items ? d.items.length + ' exercises' : 'Recovery'}</div>
      </div>
      ${d.weekday === wd ? '<span class="today-badge">TODAY</span>' : '<span class="chev">›</span>'}
    </a>`).join('');
  view.innerHTML = `
    <div class="screen-head"><h1>6-Day Split</h1>
    <div class="sub">Build muscle · Lose fat · Stay consistent</div></div>${tiles}`;
}

/* ---------- exercise detail ---------- */
function renderExercise(id) {
  const ex = EXERCISES[id];
  if (!ex) { location.hash = '#/'; return; }
  view.innerHTML = `
    <a class="back-link" href="javascript:history.back()">‹ Back</a>
    <img class="detail-gif" src="${ex.gif}" alt="${esc(ex.name)}">
    <div class="chips">
      <span class="chip">${esc(ex.equipment)}</span>
      <span class="chip">${esc(ex.target)}</span>
      ${[...new Set(ex.secondary)].slice(0, 3).map(m => `<span class="chip">${esc(m)}</span>`).join('')}
    </div>
    <div class="screen-head" style="text-align:center;margin-top:6px">
      <h1 style="font-size:20px;text-transform:capitalize">${esc(ex.name)}</h1>
    </div>
    <div class="steps">
      ${ex.steps.map((s, i) => `<div class="step"><b>${i + 1}</b><span>${esc(s)}</span></div>`).join('')}
    </div>`;
}

/* ---------- session tracking ---------- */
function startSession(dayKey) {
  const day = dayByKey(dayKey);
  const sets = day.items.map(it => {
    const prev = lastSets(it.ex);
    return Array.from({ length: it.sets }, (_, i) => ({
      w: prev && prev[i] ? prev[i].w : (prev ? prev[prev.length - 1].w : ''),
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
  const day = dayByKey(a.day);
  requestWakeLock();

  const blocks = day.items.map((it, i) => {
    const ex = EXERCISES[it.ex];
    const hint = progressionHint(it);
    const rows = a.sets[i].map((s, j) => `
      <div class="set-row ${s.done ? 'done' : ''}" data-i="${i}" data-j="${j}">
        <div class="set-num">${j + 1}</div>
        ${it.time
          ? `<input class="set-input" data-f="r" type="number" inputmode="numeric" placeholder="sec" value="${s.r}">
             <div class="set-num">—</div>`
          : `<input class="set-input" data-f="w" type="number" inputmode="decimal" step="0.5" placeholder="kg" value="${s.w}">
             <input class="set-input" data-f="r" type="number" inputmode="numeric" placeholder="${esc(it.reps)}" value="${s.r}">`}
        <button class="set-check ${s.done ? 'on' : ''}" aria-label="done">✓</button>
      </div>`).join('');
    return `
      <div class="session-ex">
        <div class="session-ex-head" data-ex="${it.ex}">
          <img class="thumb" src="${ex.gif}" alt="">
          <div class="card-body">
            <div class="card-title">${esc(it.label)}</div>
            <div class="card-meta">${it.sets} × ${esc(it.reps)}${it.time ? '' : ' · tap for form'}</div>
          </div>
          <span class="chev">›</span>
        </div>
        ${hint ? `<div class="hint">📈 ${hint}</div>` : ''}
        <div class="set-rows">
          <div class="set-head-row"><span>#</span><span>${it.time ? 'seconds' : 'kg'}</span><span>${it.time ? '' : 'reps'}</span><span>done</span></div>
          ${rows}
        </div>
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
    s[inp.dataset.f] = inp.value === '' ? '' : +inp.value;
    save();
  });
  $$('.set-check').forEach(btn => btn.onclick = () => {
    const row = btn.closest('.set-row');
    const i = +row.dataset.i, j = +row.dataset.j;
    const s = a.sets[i][j];
    s.done = !s.done;
    if (s.done) {
      // default reps to bottom of target range if left empty
      if (s.r === '' || s.r == null) {
        const it = day.items[i];
        s.r = parseInt(String(it.reps), 10) || 0;
        row.querySelector('[data-f="r"]').value = s.r;
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
      state.active = null; save(); stopRest(); location.hash = '#/';
    }
  };
}

function finishSession() {
  const a = state.active;
  const day = dayByKey(a.day);
  const log = [];
  day.items.forEach((it, i) => a.sets[i].forEach(s => {
    if (s.done) log.push({ ex: it.ex, label: it.label, w: +s.w || 0, r: +s.r || 0, time: !!it.time });
  }));
  if (!log.length && !confirm('No sets completed — finish anyway?')) return;
  const session = {
    day: a.day, title: day.title,
    startedAt: a.startedAt, endedAt: Date.now(), log,
  };
  state.sessions.push(session);
  state.active = null;
  save(); stopRest(); releaseWakeLock();
  showSummary(session, day);
}

function showSummary(s, day) {
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
        <div><b>${Math.round(volume)}</b><span>kg volume</span></div>
        <div><b>${mins}</b><span>min</span></div>
      </div>
      <button class="btn" id="close-sum">Done</button>
    </div>`;
  document.body.appendChild(el);
  $('#close-sum', el).onclick = () => { el.remove(); location.hash = '#/history'; };
  if (navigator.vibrate) navigator.vibrate([60, 60, 120]);
}

/* ---------- history ---------- */
function renderHistory() {
  if (!state.sessions.length) {
    view.innerHTML = `<div class="screen-head"><h1>History</h1></div>
      <div class="empty"><span class="emoji">🏋️</span>No workouts yet.<br>Finish your first session and it lands here.</div>`;
    return;
  }
  const fmt = ts => new Date(ts).toLocaleDateString(undefined, { weekday: 'short', month: 'short', day: 'numeric' });
  const cards = [...state.sessions].reverse().map(s => {
    const volume = s.log.filter(l => !l.time).reduce((t, l) => t + l.w * l.r, 0);
    const mins = Math.max(1, Math.round((s.endedAt - s.startedAt) / 60000));
    const d = dayByKey(s.day);
    return `
      <div class="card hist-card" style="border-left:4px solid ${d ? d.accent : '#666'}">
        <div class="card-body">
          <div class="hist-top">
            <span class="hist-title">${esc(s.title)}</span>
            <span class="hist-date">${fmt(s.endedAt)}</span>
          </div>
          <div class="hist-stats">
            <span><b>${s.log.length}</b> sets</span>
            <span><b>${Math.round(volume)}</b> kg volume</span>
            <span><b>${mins}</b> min</span>
          </div>
        </div>
      </div>`;
  }).join('');
  const week = state.sessions.filter(s => Date.now() - s.endedAt < 7 * 864e5).length;
  view.innerHTML = `
    <div class="screen-head"><h1>History</h1>
    <div class="sub">${state.sessions.length} workouts · ${week} in the last 7 days</div></div>${cards}`;
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
    <button class="btn ghost" id="export">Export data (JSON backup)</button>
    <button class="btn ghost" id="import">Import backup</button>
    <input type="file" id="import-file" accept=".json,application/json" hidden>
    <button class="btn danger" id="wipe">Clear all data</button>
    <p class="about">
      <b>Gym Buddy</b> — your 6-day split, offline.<br>
      Exercise illustrations & instructions from
      <a href="https://github.com/hasaneyldrm/exercises-dataset" target="_blank" rel="noopener">hasaneyldrm/exercises-dataset</a>,
      used for educational, non-commercial purposes.<br>
      Progression: hit the top of the rep range on all sets with good form → increase weight 2.5–5% next session.
    </p>`;
  const bump = d => {
    state.settings.restSec = Math.min(300, Math.max(15, state.settings.restSec + d));
    save(); $('#rest-val').textContent = state.settings.restSec + 's';
  };
  $('#rest-minus').onclick = () => bump(-15);
  $('#rest-plus-set').onclick = () => bump(15);
  $('#export').onclick = () => {
    const blob = new Blob([JSON.stringify(state, null, 2)], { type: 'application/json' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = `gymbuddy-backup-${new Date().toISOString().slice(0, 10)}.json`;
    a.click(); URL.revokeObjectURL(a.href);
  };
  $('#import').onclick = () => $('#import-file').click();
  $('#import-file').onchange = e => {
    const file = e.target.files[0];
    if (!file) return;
    file.text().then(t => {
      const s = JSON.parse(t);
      if (s && s.v === 1 && Array.isArray(s.sessions)) {
        state = s; save(); alert('Backup restored ✔'); render();
      } else alert('Not a valid Gym Buddy backup.');
    }).catch(() => alert('Could not read that file.'));
  };
  $('#wipe').onclick = () => {
    if (confirm('Delete ALL workout history and settings?')) {
      localStorage.removeItem(STORE_KEY); state = load(); render();
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
function stopTicker() { /* view changed — rest bar persists across views on purpose */ }

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
