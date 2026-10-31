(() => {
  const text=(document.body?.innerText||'').slice(0,50000);
  return {url:location.href,title:document.title,visibility:document.visibilityState,youtubeAd:!!document.querySelector('.html5-video-player.ad-showing'),
    gates:{challenge:/verify to continue|confirm that you're a human|verify you are human|gerçek kişi olduğunuzu|checking your browser/i.test(text),consent:/consent|cookie preferences|accept all cookies|tüm çerez/i.test(text),login:/sign in to confirm|login to watch|log in to watch/i.test(text),age:/confirm your age|age.verification/i.test(text)},
    signals:text.match(/.{0,60}(unsupported browser|browser not compatible|headless|player error|error #\d+|not supported|no supported streams|hata|error loading|cloudflare|just a moment).{0,100}/gi)?.slice(0,12)||[],
    media:[...document.querySelectorAll('video,audio')].map(v=>({tag:v.tagName,ready:v.readyState,time:v.currentTime,duration:Number.isFinite(v.duration)?v.duration:null,paused:v.paused,ended:v.ended,muted:v.muted,volume:v.volume,width:v.videoWidth,height:v.videoHeight,src:v.currentSrc?.split('?')[0],error:v.error&&{code:v.error.code,message:v.error.message}})),
    frames:[...document.querySelectorAll('iframe')].map(f=>({src:f.src?.split('?')[0],rect:rect(f)})),
    controls:[...document.querySelectorAll('button,[role=button],.np-play,.bp-play-btn,.adx-pov-close,.adx-sticky-close,[class*=play-button],[class*=playButton]')].filter(e=>{let r=e.getBoundingClientRect();return r.width>0&&r.height>0}).slice(0,100).map(e=>({tag:e.tagName,id:e.id,cls:typeof e.className==='string'?e.className:'',label:(e.getAttribute('aria-label')||e.getAttribute('title')||e.innerText||'').slice(0,100),rect:rect(e)}))};
  function rect(e){const r=e.getBoundingClientRect();return {x:r.x,y:r.y,w:r.width,h:r.height};}
})()
