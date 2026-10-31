param([string]$Id,[string]$Executable)
. $PSScriptRoot/common.ps1
C @{op='start';id=$Id;executable=$Executable;mode='headless'} | Out-Null
& $PSScriptRoot/control.ps1 -Id $Id -Popup | Out-Null
& $PSScriptRoot/benchmark.ps1 -Id $Id -Seconds 15
& $PSScriptRoot/youtube720.ps1 -Id $Id -Url 'https://www.youtube.com/watch?v=dQw4w9WgXcQ'
Save "run/chrome-spike/$Id/youtube720-before.json" @(Media $Id)
C @{op='stream';id=$Id;name='youtube720-q80';seconds=15;width=1280;height=720;quality=80} | Out-Null
Save "run/chrome-spike/$Id/youtube720-after.json" @(Media $Id)
C @{op='stream';id=$Id;name='youtube480-q55';seconds=12;width=854;height=480;quality=55} | Out-Null
Save "run/chrome-spike/$Id/performance-ready.json" @{passed=$true;at=[DateTimeOffset]::UtcNow.ToString('o')}
