/* Gym Buddy service worker — precache the app shell, serve cache-first (fully offline).
   All exercise media is vendored locally in web/media/ — no runtime dependency on the
   source dataset repo. */
const CACHE = 'gymbuddy-v4';

const ASSETS = [
  './',
  './index.html',
  './styles.css',
  './app.js',
  './engine.js',
  './charts.js',
  './data.js',
  './manifest.webmanifest',
  './icons/icon-192.png',
  './icons/icon-512.png',
  './icons/maskable-512.png',
  './media/0025.gif',
  './media/0027.gif',
  './media/0030.gif',
  './media/0031.gif',
  './media/0043.gif',
  './media/0047.gif',
  './media/0060.gif',
  './media/0085.gif',
  './media/0194.gif',
  './media/0200.gif',
  './media/0227.gif',
  './media/0233.gif',
  './media/0238.gif',
  './media/0251.gif',
  './media/0289.gif',
  './media/0313.gif',
  './media/0314.gif',
  './media/0318.gif',
  './media/0327.gif',
  './media/0334.gif',
  './media/0383.gif',
  './media/0405.gif',
  './media/0447.gif',
  './media/0472.gif',
  './media/0585.gif',
  './media/0586.gif',
  './media/0594.gif',
  './media/0605.gif',
  './media/0606.gif',
  './media/0652.gif',
  './media/0861.gif',
  './media/1463.gif',
  './media/2135.gif',
  './media/2330.gif',
];

self.addEventListener('install', e => {
  e.waitUntil(
    caches.open(CACHE).then(c => c.addAll(ASSETS)).then(() => self.skipWaiting())
  );
});

self.addEventListener('activate', e => {
  e.waitUntil(
    caches.keys()
      .then(keys => Promise.all(keys.filter(k => k !== CACHE).map(k => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', e => {
  if (e.request.method !== 'GET') return;
  e.respondWith(
    caches.match(e.request, { ignoreSearch: true }).then(hit =>
      hit ||
      fetch(e.request).then(res => {
        // Cache same-origin responses not in the precache list (catalog.json, the
        // 1,290 non-curated exercise GIFs) — fetched lazily as the user browses,
        // never precached up front.
        if (res.ok || res.type === 'opaque') {
          const copy = res.clone();
          caches.open(CACHE).then(c => c.put(e.request, copy));
        }
        return res;
      })
    )
  );
});

self.addEventListener('notificationclick', e => {
  e.notification.close();
  e.waitUntil(
    self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then(clients => {
      for (const c of clients) if ('focus' in c) return c.focus();
      if (self.clients.openWindow) return self.clients.openWindow('./');
    })
  );
});
