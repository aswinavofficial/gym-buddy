/* Gym Buddy service worker — precache everything, serve cache-first (fully offline). */
const CACHE = 'gymbuddy-v1';

const ASSETS = [
  './',
  './index.html',
  './styles.css',
  './app.js',
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
        if (res.ok && new URL(e.request.url).origin === location.origin) {
          const copy = res.clone();
          caches.open(CACHE).then(c => c.put(e.request, copy));
        }
        return res;
      })
    )
  );
});
