// Opt-in metadata-only codec probe. Does not save content or manipulate playback/consent.
(() => {
  if (window.__perdeCodecObserve) return;
  const tag = 'perde-codec-LEA-200';
  const observations = new WeakMap();
  const totals = { frameHost: location.hostname, top: window === window.top, videos: [], requests: [], controls: [] };
  window.__perdeCodecObserve = totals;
  if (window === window.top) {
    window.__perdeCodecFrames = {};
    addEventListener('message', event => {
      if (event.data?.tag === tag && typeof event.data.frameHost === 'string')
        window.__perdeCodecFrames[event.data.frameHost + ':' + event.data.top] = event.data;
    });
  }
  const mime = {
    h264: 'video/mp4; codecs="avc1.42E01E"', aac: 'audio/mp4; codecs="mp4a.40.2"',
    vp9: 'video/webm; codecs="vp9"', av1: 'video/mp4; codecs="av01.0.05M.08"', opus: 'audio/webm; codecs="opus"'
  };
  totals.support = Object.fromEntries(Object.entries(mime).map(([name, type]) =>
    [name, { mime:type, mediaSource: typeof MediaSource !== 'undefined' && MediaSource.isTypeSupported(type), canPlay:document.createElement('video').canPlayType(type) }]));
  const visible = e => { const r=e.getBoundingClientRect();return r.width>0 && r.height>0 && getComputedStyle(e).visibility==='visible'; };
  const update = () => {
    totals.now = Date.now();
    totals.videos = Array.from(document.querySelectorAll('video')).map((v,index) => {
      let data = observations.get(v);
      if (!data) { data={first:v.currentTime,previous:v.currentTime,readyMax:0,advancingSamples:0};observations.set(v,data); }
      if (v.readyState>=3 && !v.paused && v.currentTime-data.previous>0.02 && v.currentTime-data.previous<1) data.advancingSamples++;
      data.previous=v.currentTime;data.readyMax=Math.max(data.readyMax,v.readyState);
      const src=v.currentSrc || v.src;
      let sourceHost='';try{sourceHost=new URL(src,location.href).hostname;}catch(_){}
      return {index,time:v.currentTime,firstTime:data.first,ready:v.readyState,readyMax:data.readyMax,advancingSamples:data.advancingSamples,
        paused:v.paused,visible:visible(v),duration:Number.isFinite(v.duration)?v.duration:null,volume:v.volume,muted:v.muted,
        error:v.error?{code:v.error.code,message:v.error.message.slice(0,240)}:null,
        sourceHost,sourceKind:/^blob:/.test(src)?'blob':(/\.(m3u8|mpd|mp4|webm)(?:[?#]|$)/i.exec(src)?.[1] || 'unknown'),
        declaredTypes:Array.from(v.querySelectorAll('source')).map(s=>s.type).filter(Boolean)};
    });
    totals.requests = Array.from(new Set(performance.getEntriesByType('resource').flatMap(r => {
      const type=/\.(m3u8|mpd|mp4|webm)(?:[?#]|$)/i.exec(r.name)?.[1];
      if (!type) return [];
      try{return [new URL(r.name).hostname+':'+type.toLowerCase()];}catch(_){return [];}
    }))).slice(0,30);
    totals.controls = Array.from(document.querySelectorAll('button,[role="button"],input[type="button"]')).filter(visible)
      .map(e=>({tag:e.tagName,aria:e.getAttribute('aria-label'),title:e.getAttribute('title'),test:e.getAttribute('data-testid'),classes:String(e.className).slice(0,120)}))
      .filter(c=>/play|oynat|başlat/i.test([c.aria,c.title,c.test,c.classes].join(' '))).slice(0,12);
    const dialogs=Array.from(document.querySelectorAll('[role="dialog"],#onetrust-banner-sdk,.qc-cmp2-container,[id*="consent"],[class*="cookie-banner"]')).filter(visible);
    totals.gate = dialogs.some(d=>/consent|cookies|çerez|privacy|gizlilik|accept|agree|izin/i.test(d.innerText));
    totals.challenge = /just a moment|verify you are human|checking your browser|erişim engellendi|access denied/i.test(document.title);
    window.top.postMessage({tag,...totals}, '*');
  };
  update();setInterval(update,250);
})();
