param([string]$Id,[int]$Seconds=20)
. $PSScriptRoot/common.ps1
$dir="run/chrome-spike/$Id"
D $Id 'Page.navigate' @{url='https://example.org/'} | Out-Null
Start-Sleep -Seconds 2
$tree=D $Id 'Page.getFrameTree'
$html=@'
<!doctype html><html><body style="margin:0;background:#10151c;color:white;font:20px sans-serif">
<video id="sample" style="width:1280px;height:720px;object-fit:contain" autoplay muted loop playsinline src="https://media.w3.org/2010/05/sintel/trailer.mp4"></video>
<div style="position:fixed;top:8px;left:8px;background:#000b;padding:8px">Perde CDP benchmark: W3C Sintel H.264/AAC sample</div>
</body></html>
'@
D $Id 'Page.setDocumentContent' @{frameId=$tree.frameTree.frame.id;html=$html} | Out-Null
Start-Sleep -Seconds 7
Save "$dir/benchmark-before.json" @(Media $Id)
Save "$dir/benchmark-memory.json" (Memory $Id)
$r=C @{op='stream';id=$Id;name='sintel-720-q80';seconds=$Seconds;width=1280;height=720;quality=80}
$r | Select-Object frames,averageFps,medianIntervalFps,p10IntervalFps,bytes,latencyMs,imageIoMs,stbMs | ConvertTo-Json -Depth 4
Save "$dir/benchmark-after.json" @(Media $Id)
$r=C @{op='stream';id=$Id;name='sintel-480-q55';seconds=12;width=854;height=480;quality=55}
$r | Select-Object frames,averageFps,medianIntervalFps,p10IntervalFps,bytes,latencyMs,imageIoMs,stbMs | ConvertTo-Json -Depth 4
Save "$dir/benchmark-post-memory.json" (Memory $Id)
E $Id "document.querySelector('video').pause()" | Out-Null
