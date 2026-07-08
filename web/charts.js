/* Gym Buddy — tiny dependency-free SVG chart builders. Each function returns an HTML string. */
'use strict';

const CHART_W = 320, CHART_H = 120;

function svgBarChart(labels, values, opts = {}) {
  const w = opts.width || CHART_W, h = opts.height || CHART_H;
  const pad = 18;
  const max = Math.max(1, ...values);
  const n = values.length;
  const bw = (w - pad) / n * 0.62;
  const gap = (w - pad) / n;
  const bars = values.map((v, i) => {
    const bh = Math.max(2, (v / max) * (h - pad));
    const x = pad + i * gap + (gap - bw) / 2;
    const y = h - pad - bh;
    return `<rect x="${x.toFixed(1)}" y="${y.toFixed(1)}" width="${bw.toFixed(1)}" height="${bh.toFixed(1)}" rx="3" fill="${opts.color || 'var(--accent)'}"><title>${esc(labels[i])}: ${v}</title></rect>`;
  }).join('');
  const labelEls = labels.map((l, i) => {
    const x = pad + i * gap + gap / 2;
    return `<text x="${x.toFixed(1)}" y="${h - 4}" font-size="8" fill="var(--text-dim)" text-anchor="middle">${esc(l)}</text>`;
  }).join('');
  return `<svg viewBox="0 0 ${w} ${h}" class="chart-svg">${bars}${labelEls}</svg>`;
}

function svgLineChart(points, opts = {}) {
  const w = opts.width || CHART_W, h = opts.height || CHART_H;
  const pad = 20;
  if (!points.length) return `<svg viewBox="0 0 ${w} ${h}" class="chart-svg"></svg>`;
  const ys = points.map(p => p.y);
  const min = Math.min(...ys), max = Math.max(...ys);
  const range = max - min || 1;
  const n = points.length;
  const stepX = n > 1 ? (w - pad * 2) / (n - 1) : 0;
  const coords = points.map((p, i) => {
    const x = pad + i * stepX;
    const y = h - pad - ((p.y - min) / range) * (h - pad * 2);
    return [x, y];
  });
  const path = coords.map(([x, y], i) => `${i === 0 ? 'M' : 'L'}${x.toFixed(1)},${y.toFixed(1)}`).join(' ');
  const dots = coords.map(([x, y], i) => `<circle cx="${x.toFixed(1)}" cy="${y.toFixed(1)}" r="${i === n - 1 ? 4 : 2.5}" fill="${i === n - 1 ? (opts.color || 'var(--accent)') : 'var(--text-dim)'}"><title>${esc(points[i].label)}: ${points[i].y}</title></circle>`).join('');
  return `<svg viewBox="0 0 ${w} ${h}" class="chart-svg">
    <path d="${path}" fill="none" stroke="${opts.color || 'var(--accent)'}" stroke-width="2" stroke-linejoin="round" stroke-linecap="round"/>
    ${dots}
    <text x="${pad}" y="12" font-size="9" fill="var(--text-dim)">${Math.round(max)}</text>
    <text x="${pad}" y="${h - 6}" font-size="9" fill="var(--text-dim)">${Math.round(min)}</text>
  </svg>`;
}

function svgRing(pct, opts = {}) {
  const size = opts.size || 64, stroke = opts.stroke || 7;
  const r = (size - stroke) / 2, c = size / 2;
  const circ = 2 * Math.PI * r;
  const clamped = Math.max(0, Math.min(1, pct));
  return `<svg viewBox="0 0 ${size} ${size}" width="${size}" height="${size}" class="ring-svg">
    <circle cx="${c}" cy="${c}" r="${r}" fill="none" stroke="var(--surface-2)" stroke-width="${stroke}"/>
    <circle cx="${c}" cy="${c}" r="${r}" fill="none" stroke="${opts.color || 'var(--accent)'}" stroke-width="${stroke}"
      stroke-dasharray="${circ.toFixed(1)}" stroke-dashoffset="${(circ * (1 - clamped)).toFixed(1)}"
      stroke-linecap="round" transform="rotate(-90 ${c} ${c})"/>
  </svg>`;
}

function svgHeatmap(weeksCount, dayCounts, opts = {}) {
  const cell = 11, gap = 3;
  const w = weeksCount * (cell + gap), h = 7 * (cell + gap);
  const now = new Date(); now.setHours(0, 0, 0, 0);
  const start = new Date(now); start.setDate(start.getDate() - (weeksCount * 7 - 1));
  let cells = '';
  for (let i = 0; i < weeksCount * 7; i++) {
    const d = new Date(start); d.setDate(start.getDate() + i);
    const key = d.toDateString();
    const count = dayCounts[key] || 0;
    const col = Math.floor(i / 7), row = i % 7;
    const alpha = count === 0 ? 0.08 : Math.min(1, 0.3 + count * 0.35);
    const isFuture = d > now;
    cells += `<rect x="${col * (cell + gap)}" y="${row * (cell + gap)}" width="${cell}" height="${cell}" rx="2.5"
      fill="${isFuture ? 'transparent' : `color-mix(in srgb, ${opts.color || 'var(--accent)'} ${Math.round(alpha * 100)}%, transparent)`}"
      stroke="${isFuture ? 'var(--surface-2)' : 'none'}"><title>${d.toDateString()}: ${count} workout${count === 1 ? '' : 's'}</title></rect>`;
  }
  return `<svg viewBox="0 0 ${w} ${h}" class="chart-svg heatmap-svg">${cells}</svg>`;
}
