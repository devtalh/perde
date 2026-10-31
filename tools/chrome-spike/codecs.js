(() => {
 const v=document.createElement('video');
 const types={h264baseline:'video/mp4; codecs="avc1.42E01E"',h264high:'video/mp4; codecs="avc1.640028"',aac:'audio/mp4; codecs="mp4a.40.2"',hevc:'video/mp4; codecs="hvc1.1.6.L93.B0"',vp9:'video/webm; codecs="vp09.00.10.08"',av1:'video/mp4; codecs="av01.0.05M.08"',opus:'audio/webm; codecs="opus"'};
 return Object.fromEntries(Object.entries(types).map(([k,mime])=>[k,{mime,mse:MediaSource.isTypeSupported(mime),canPlayType:v.canPlayType(mime)}]));
})()
