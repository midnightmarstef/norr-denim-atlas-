// Keeps the atlas shell available offline; map tiles and routing still need a connection.
const SHELL = 'norr-atlas-v1';
self.addEventListener('install', e => { e.waitUntil(caches.open(SHELL).then(c => c.addAll(['./index.html', './manifest.webmanifest', './icon-192.png', './icon-512.png'])).then(() => self.skipWaiting())); });
self.addEventListener('activate', e => { e.waitUntil(caches.keys().then(ks => Promise.all(ks.filter(k => k !== SHELL).map(k => caches.delete(k)))).then(() => self.clients.claim())); });
self.addEventListener('fetch', e => {
  const url = new URL(e.request.url);
  if (url.origin !== location.origin) return;                       // tiles, fonts, CDN scripts, OSRM: straight to network
  e.respondWith(fetch(e.request).then(r => { const copy = r.clone(); caches.open(SHELL).then(c => c.put(e.request, copy)); return r; }).catch(() => caches.match(e.request, { ignoreSearch: true }).then(r => r || caches.match('./index.html'))));
});
