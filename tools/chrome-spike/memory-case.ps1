param([string]$Id,[string]$Executable,[string]$Mode,[switch]$Existing)
. $PSScriptRoot/common.ps1
try {
if(-not $Existing){
    $launched=C @{op='start';id=$Id;executable=$Executable;mode=$Mode;unsafe=$true;pipe=$true}
    if($Mode -eq 'offscreen'){& $PSScriptRoot/windows.ps1 -OwnedPid $launched.pid | Set-Content "run/chrome-spike/$Id/window-bounds.txt"}
    Save "run/chrome-spike/$Id/codecs.json" (E $Id (Get-Content tools/chrome-spike/codecs.js -Raw))
    & $PSScriptRoot/memory.ps1 -Id $Id -Point blank
    try{& $PSScriptRoot/youtube720.ps1 -Id $Id -Url 'https://www.youtube.com/watch?v=dQw4w9WgXcQ'}catch{Save "run/chrome-spike/$Id/quality-error.json" @{error=$_.Exception.Message}}
}
Save "run/chrome-spike/$Id/youtube720-short-before.json" @(Media $Id)
C @{op='stream';id=$Id;name='youtube720-short';seconds=15;width=1280;height=720;quality=80} | Out-Null
Save "run/chrome-spike/$Id/youtube720-short-after.json" @(Media $Id)
& $PSScriptRoot/soak.ps1 -Id $Id
# Capture and both decoders are active while sampling the Java and browser memory at the video point.
$client=[Net.Http.HttpClient]::new();$client.Timeout=[TimeSpan]::FromSeconds(90);$client.DefaultRequestHeaders.Add('X-Perde-Spike-Token',$controller.token)
$content=[Net.Http.StringContent]::new((@{op='stream';id=$Id;name='youtube720-memory-stream';seconds=35;width=1280;height=720;quality=80} | ConvertTo-Json -Compress),[Text.Encoding]::UTF8,'application/json')
$capture=$client.PostAsync($script:Endpoint,$content)
& $PSScriptRoot/memory.ps1 -Id $Id -Point youtube720-60s
$response=$capture.GetAwaiter().GetResult();if(-not $response.IsSuccessStatusCode){throw $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()};$client.Dispose()
C @{op='stream';id=$Id;name='youtube720-low-q55';seconds=12;width=854;height=480;quality=55} | Out-Null
& $PSScriptRoot/sites.ps1 -Id $Id -Site dizipal-memory -Url 'https://dizipalorijinal20.com/bolum/mobland-2-sezon-7-bolum' -Wait 8
& $PSScriptRoot/soak.ps1 -Id $Id -Name dizi-attempt-60s
& $PSScriptRoot/memory.ps1 -Id $Id -Point dizi-failed-no-extension
& $PSScriptRoot/ads.ps1 -Id $Id -Label ad-no-extension
& $PSScriptRoot/memory.ps1 -Id $Id -Point dizi-without-extension
D $Id 'Extensions.loadUnpacked' @{path=(Resolve-Path run/chrome-spike/ubol).Path} -Browser | Out-Null
Save "run/chrome-spike/$Id/extensions.json" (D $Id 'Extensions.getExtensions' @{} -Browser)
& $PSScriptRoot/ads.ps1 -Id $Id -Label ad-with-extension
& $PSScriptRoot/memory.ps1 -Id $Id -Point dizi-with-extension
D $Id 'Page.navigate' @{url='about:blank'} | Out-Null
Start-Sleep -Seconds 5
& $PSScriptRoot/memory.ps1 -Id $Id -Point returned-blank
$extensions=D $Id 'Extensions.getExtensions' @{} -Browser
foreach($extension in $extensions.extensions){if($extension.name -eq 'uBlock Origin Lite'){D $Id 'Extensions.uninstall' @{id=$extension.id} -Browser | Out-Null}}
& $PSScriptRoot/control.ps1 -Id $Id | Out-Null
} finally {
    C @{op='stop';id=$Id} | Select-Object remaining | ConvertTo-Json -Compress
}
