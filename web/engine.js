/* Gym Buddy — domain logic: units, 1RM, MET/calories, progression, PRs, streaks, achievements, plates, CSV.
   Pure functions/state — no DOM here. Depends on `state`/`save`/`getEx` from app.js (loaded after this file). */
'use strict';

const LB_PER_KG = 2.2046226218;
const round1 = n => Math.round(n * 10) / 10;

/* ---------- units ---------- */
function unitLabel() { return state.settings.units === 'lb' ? 'lb' : 'kg'; }
function kgToDisp(kg) { if (kg === '' || kg == null) return kg; return state.settings.units === 'lb' ? round1(kg * LB_PER_KG) : round1(kg); }
function dispToKg(v) { if (v === '' || v == null || v === '') return v; return state.settings.units === 'lb' ? round1(v / LB_PER_KG) : round1(+v); }
function roundToStep(kg) { return Math.round(kg / 1.25) * 1.25; } // nearest small-plate increment

/* ---------- 1RM (Epley + Brzycki averaged) ---------- */
function oneRM(w, r) {
  if (!w || !r) return 0;
  const epley = w * (1 + r / 30);
  const brzycki = r < 37 ? w * 36 / (37 - r) : epley;
  return (epley + brzycki) / 2;
}

/* ---------- MET-based calorie estimate ---------- */
function metForTarget(target) {
  const t = (target || '').toLowerCase();
  if (/pector|chest/.test(t)) return 5.0;
  if (/lat|trapez|rhomboid|back/.test(t)) return 5.5;
  if (/delt|shoulder/.test(t)) return 4.5;
  if (/bicep|tricep|forearm/.test(t)) return 3.8;
  if (/quad|hamstring|glute|calv|adductor|abductor/.test(t)) return 6.0;
  if (/abdominal|oblique|waist|core/.test(t)) return 4.5;
  if (/cardio/.test(t)) return 8.0;
  return 4.5;
}
function latestBodyWeight() {
  return state.bodyMetrics.length ? state.bodyMetrics[state.bodyMetrics.length - 1].kg : 75;
}
function sessionCalories(session) {
  const sets = session.log.filter(l => !l.time);
  if (!sets.length) return 0;
  const kg = latestBodyWeight();
  const mets = sets.map(l => metForTarget((getEx(l.ex) || {}).target));
  const avgMet = mets.reduce((a, b) => a + b, 0) / mets.length;
  const minutes = Math.max(1, (session.endedAt - session.startedAt) / 60000);
  return Math.round(avgMet * 3.5 * kg / 200 * minutes);
}

/* ---------- progression engine ---------- */
/* Last logged sets for an exercise across finished sessions (newest first, warm-ups excluded). */
function lastSets(exId) {
  for (let i = state.sessions.length - 1; i >= 0; i--) {
    const sets = (state.sessions[i].log || []).filter(l => l.ex === exId);
    if (sets.length) return sets;
  }
  return null;
}
function progressionFor(item) {
  const prev = lastSets(item.ex);
  if (!prev || item.time) return null;
  const parts = String(item.reps).split('-').map(n => parseInt(n, 10));
  const bottom = parts[0], top = parts[parts.length - 1];
  if (!top) return null;
  const bestW = Math.max(...prev.map(s => s.w || 0));
  if (!bestW) return null;
  const allTop = prev.every(s => s.r >= top);
  const allBelowBottom = bottom && prev.every(s => s.r < bottom);
  const cappedByRpe = state.settings.rpeEnabled && prev.some(s => s.rpe >= 9.5) && allTop;
  if (allTop && !cappedByRpe) {
    return { weight: round1(bestW * 1.025), type: 'up', hint: `Hit ${top}+ reps on every set last time — try more weight today` };
  }
  if (allBelowBottom) {
    return { weight: round1(bestW * 0.9), type: 'deload', hint: `Missed the rep target last time — consider a lighter deload today` };
  }
  return { weight: bestW, type: 'repeat', hint: null };
}

/* ---------- personal records (derived from finished sessions; rebuilt at boot, updated live) ---------- */
let prMap = {};
function rebuildPRs() {
  prMap = {};
  for (const s of state.sessions) for (const l of s.log) updatePRFromSet(l.ex, l.w, l.r, s.endedAt, false);
}
function updatePRFromSet(exId, w, r, ts) {
  if (!w || !r) return false;
  const rm = oneRM(w, r);
  const cur = prMap[exId] || { bestWeight: 0, bestOneRm: 0, achievedAt: 0 };
  let isPR = false;
  if (w > cur.bestWeight) { cur.bestWeight = w; isPR = true; }
  if (rm > cur.bestOneRm) { cur.bestOneRm = rm; isPR = true; }
  if (isPR) cur.achievedAt = ts;
  prMap[exId] = cur;
  return isPR;
}

/* ---------- streaks & weekly goal ---------- */
function currentStreak() {
  const days = new Set(state.sessions.map(s => new Date(s.endedAt).toDateString()));
  let streak = 0;
  let d = new Date();
  if (!days.has(d.toDateString())) d.setDate(d.getDate() - 1); // today not done yet still counts yesterday's chain
  while (days.has(d.toDateString())) { streak++; d.setDate(d.getDate() - 1); }
  return streak;
}
function weekWindowStart() {
  const now = new Date();
  const daysSinceStart = ((now.getDay() - state.startWeekday) % 7 + 7) % 7;
  const start = new Date(now); start.setHours(0, 0, 0, 0); start.setDate(start.getDate() - daysSinceStart);
  return start.getTime();
}
function sessionsThisWeek() { return state.sessions.filter(s => s.endedAt >= weekWindowStart()).length; }
function weeklyGoal() { return state.settings.weeklyGoal ?? state.week.length; }

/* ---------- achievements ---------- */
const ACHIEVEMENTS = [
  { id: 'first_workout', name: 'First Step', emoji: '🎉', check: () => state.sessions.length >= 1 },
  { id: 'workouts_5', name: 'Getting Started', emoji: '🔥', check: () => state.sessions.length >= 5 },
  { id: 'workouts_10', name: 'Committed', emoji: '💪', check: () => state.sessions.length >= 10 },
  { id: 'workouts_25', name: 'Regular', emoji: '⭐', check: () => state.sessions.length >= 25 },
  { id: 'workouts_50', name: 'Half Century', emoji: '🏅', check: () => state.sessions.length >= 50 },
  { id: 'workouts_100', name: 'Centurion', emoji: '👑', check: () => state.sessions.length >= 100 },
  { id: 'streak_3', name: '3-Day Streak', emoji: '🔥', check: () => currentStreak() >= 3 },
  { id: 'streak_7', name: 'Week Warrior', emoji: '🔥', check: () => currentStreak() >= 7 },
  { id: 'streak_14', name: 'Two-Week Streak', emoji: '🔥', check: () => currentStreak() >= 14 },
  { id: 'streak_30', name: 'Iron Will', emoji: '🔥', check: () => currentStreak() >= 30 },
  { id: 'first_pr', name: 'New Record', emoji: '🏆', check: () => Object.keys(prMap).length >= 1 },
  { id: 'pr_10', name: 'Record Breaker', emoji: '🏆', check: () => Object.keys(prMap).length >= 10 },
  { id: 'goal_hit', name: 'Goal Crusher', emoji: '🎯', check: () => sessionsThisWeek() >= weeklyGoal() },
  { id: 'ten_tonne', name: '10-Tonne Club', emoji: '🐘', check: s => s && s.log.filter(l => !l.time).reduce((t, l) => t + l.w * l.r, 0) >= 10000 },
];
function checkAchievements(session) {
  const unlocked = [];
  for (const a of ACHIEVEMENTS) {
    if (state.achievements[a.id]) continue;
    if (a.check(session)) { state.achievements[a.id] = Date.now(); unlocked.push(a); }
  }
  if (unlocked.length) save();
  return unlocked;
}

/* ---------- plate calculator (operates in display units) ---------- */
const PLATE_SETS = { kg: [25, 20, 15, 10, 5, 2.5, 1.25], lb: [45, 35, 25, 10, 5, 2.5] };
function calcPlates(targetDisp, barDisp) {
  const setDisp = PLATE_SETS[unitLabel()];
  let remaining = (targetDisp - barDisp) / 2;
  if (remaining < -1e-6) return { plates: [], warning: 'Target is lighter than the bar alone.' };
  const plates = [];
  for (const p of setDisp) { while (remaining >= p - 1e-6) { plates.push(p); remaining -= p; } }
  const warning = remaining > 0.01 ? `Off by ${(remaining * 2).toFixed(2)} ${unitLabel()} — not exactly loadable with standard plates.` : null;
  return { plates, warning };
}

/* ---------- CSV export ---------- */
function toCSV(session) {
  const rows = [['date', 'day', 'exercise', 'set', `weight_${unitLabel()}`, 'reps', 'rpe', 'est_1rm']];
  state.sessions.forEach(s => {
    const date = new Date(s.endedAt).toISOString().slice(0, 10);
    const perExCount = {};
    s.log.forEach(l => {
      perExCount[l.ex] = (perExCount[l.ex] || 0) + 1;
      const rm = l.time ? '' : Math.round(oneRM(l.w, l.r));
      rows.push([date, s.title, l.label || (getEx(l.ex) || {}).name || l.ex, perExCount[l.ex], l.time ? '' : kgToDisp(l.w), l.r, l.rpe ?? '', rm]);
    });
  });
  return rows.map(r => r.map(v => /[,"\n]/.test(String(v)) ? `"${String(v).replace(/"/g, '""')}"` : v).join(',')).join('\n');
}
