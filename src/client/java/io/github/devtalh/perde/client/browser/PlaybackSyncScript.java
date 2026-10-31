package io.github.devtalh.perde.client.browser;

/** Runs separately in each renderer frame. Captured element and callback never cross DOM origins. */
final class PlaybackSyncScript {
    private PlaybackSyncScript() { }
    static String sample(String token, long poll) {
        // Token is generated as hex in Java, never taken from page input.
        return """
                (() => {
                  const key = '__perde_%s', token = '%s', poll = %d;
                  let state = window[key];
                  if (!state) {
                    state = { ids: new WeakMap(), next: 1, rejected: false };
                    Object.defineProperty(window, key, { value: state });
                  }
                  const candidates = Array.from(document.querySelectorAll('video')).map(v => {
                    const r = v.getBoundingClientRect(), s = getComputedStyle(v);
                    const area = Math.max(0, Math.min(r.right, innerWidth) - Math.max(r.left, 0)) *
                                 Math.max(0, Math.min(r.bottom, innerHeight) - Math.max(r.top, 0));
                    return {v, area: s.display !== 'none' && s.visibility === 'visible' && +s.opacity > 0 ? area : 0};
                  }).filter(c => c.area > 0).sort((a,b) => Number(a.v.paused)-Number(b.v.paused) || b.area-a.area);
                  const chosen = candidates[0], v = chosen?.v;
                  let id = 0;
                  if (v) {
                    id = state.ids.get(v);
                    if (!id) { id = state.next++; state.ids.set(v, id); }
                  }
                  const duration = v && Number.isFinite(v.duration) ? v.duration * 1000 : 'live';
                  const live = !!v && (!Number.isFinite(v.duration) || v.getAttribute('data-is-live') === 'true');
                  const request = ['p1', token, poll, id, chosen?.area || 0, v?.currentTime * 1000 || 0,
                    duration, !v || v.paused ? 1:0, v?.readyState || 0, live ? 1:0, state.rejected ? 1:0, 'end'].join('|');
                  state.rejected = false;
                  window.cefQuery({request, persistent:false, onSuccess: response => {
                    if (!v || !v.isConnected) return;
                    const action = JSON.parse(response);
                    if (action.seek !== null) v.currentTime = action.seek / 1000;
                    if (action.transport === 'PAUSE') v.pause();
                    if (action.transport === 'PLAY') {
                      try { Promise.resolve(v.play()).catch(() => {state.rejected = true;}); }
                      catch (_) { state.rejected = true; }
                    }
                  }, onFailure: () => {}});
                })();
                """.formatted(token, token, poll);
    }
}
