param([string]$Id,[switch]$AlreadyOpen,[string]$Url='https://www.youtube.com/watch?v=YE7VzlLtp-4')
. $PSScriptRoot/common.ps1
if(-not $AlreadyOpen){D $Id 'Page.navigate' @{url=$Url} | Out-Null;Start-Sleep -Seconds 8}
for($adWait=0;$adWait -lt 18;$adWait++){
    $ad=(E $Id "!!document.querySelector('.html5-video-player.ad-showing')").result.value
    if(-not $ad){break}
    Start-Sleep -Seconds 5
}
if($ad){throw 'Advertisement did not finish within the observation window'}
# Close an earlier settings panel before opening the content video's native quality menu.
D $Id 'Input.dispatchKeyEvent' @{type='keyDown';key='Escape';code='Escape';windowsVirtualKeyCode=27} | Out-Null
D $Id 'Input.dispatchKeyEvent' @{type='keyUp';key='Escape';code='Escape';windowsVirtualKeyCode=27} | Out-Null
Start-Sleep -Milliseconds 300
function RectOf($expression){(E $Id "(()=>{const e=$expression;if(!e)return null;const r=e.getBoundingClientRect();return {x:r.x+r.width/2,y:r.y+r.height/2,w:r.width,h:r.height}})()").result.value}
$quality=RectOf "[...document.querySelectorAll('.ytp-menuitem')].find(e=>/^720p/.test(e.innerText.trim()))"
if(-not $quality -or $quality.w -eq 0){
    $settings=RectOf "document.querySelector('.ytp-settings-button')"
    Click $Id $settings.x $settings.y | Out-Null
    Start-Sleep -Milliseconds 500
    $qualityMenu=RectOf "[...document.querySelectorAll('.ytp-menuitem')].find(e=>/Kalite|Quality/.test(e.innerText)&&e.getBoundingClientRect().width>0)"
    if(-not $qualityMenu){throw 'Quality menu unavailable'}
    Click $Id $qualityMenu.x $qualityMenu.y | Out-Null
    Start-Sleep -Milliseconds 500
    $quality=RectOf "[...document.querySelectorAll('.ytp-menuitem')].find(e=>/^720p/.test(e.innerText.trim())&&e.getBoundingClientRect().width>0)"
}
if(-not $quality){throw 'Native 720p option unavailable'}
Click $Id $quality.x $quality.y | Out-Null
Start-Sleep -Seconds 3
$observations=@()
for($i=0;$i -lt 14;$i++){
    $v=(E $Id "(()=>{const v=document.querySelector('video');return v&&{ready:v.readyState,time:v.currentTime,w:v.videoWidth,h:v.videoHeight,paused:v.paused,error:v.error&&v.error.message}})()").result.value
    $observations+=@{at=[DateTimeOffset]::UtcNow.ToString('o');video=$v}
    if($v.ready -ge 3 -and $v.w -ge 960 -and $v.h -eq 720 -and -not $v.paused){Save "run/chrome-spike/$Id/quality720.json" $observations;$v | ConvertTo-Json -Compress;return}
    Start-Sleep -Seconds 1
}
Save "run/chrome-spike/$Id/quality720-failed.json" $observations
throw '720p playback not observed'
