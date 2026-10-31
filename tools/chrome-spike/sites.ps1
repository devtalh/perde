param([string]$Id,[string]$Site,[string]$Url,[int]$Wait=10)
. $PSScriptRoot/common.ps1
$dir="run/chrome-spike/$Id"
Save "$dir/$Site-navigation.json" (D $Id 'Page.navigate' @{url=$Url})
Start-Sleep -Seconds $Wait
$samples=@(Media $Id)
Save "$dir/$Site-initial.json" $samples
$actions=@()
for($attempt=0;$attempt -lt 7;$attempt++){
    $samples=@(Media $Id)
    $values=@($samples | Where-Object { $_.result.result.value } | ForEach-Object { [pscustomobject]@{session=$_.session;contextId=$_.contextId;v=$_.result.result.value} })
    if(@($values | Where-Object {$_.v.gates.challenge -or $_.v.gates.login -or $_.v.gates.age}).Count -gt 0){break}
    $mainUrl=(E $Id 'location.href').result.value
    if(([uri]$mainUrl).Host -ne ([uri]$Url).Host){$actions+=@{type='navigationReturnedElsewhere';requested=$Url;observed=$mainUrl};break}
    $adClose=@($values | ForEach-Object {$_.v.controls} | Where-Object {$_.cls -eq 'adx-pov-close' -and $_.rect.y -ge 0 -and $_.rect.y -lt 700})
    if($adClose.Count -gt 0){$r=$adClose[0].rect;Click $Id ($r.x+$r.w/2) ($r.y+$r.h/2) | Out-Null;$actions+=@{type='normalAdClose';class='adx-pov-close'};Start-Sleep -Milliseconds 300;continue}
    $playing=@($values | ForEach-Object { $_.v.media } | Where-Object { $_.ready -ge 3 -and -not $_.paused -and $_.width -ge 320 -and $_.height -ge 180 -and ($null -eq $_.duration -or $_.duration -gt 120) })
    if($playing.Count -gt 0){break}
    $clicked=$false
    foreach($cx in $values){
        foreach($control in @($cx.v.controls)){
            if (($control.label -match '^(Play|Oynat|Bölümü başlat|Bolumu baslat|Bolumu Baslat|Videoyu oynat|Play video|Start watching|Watch live|Oynat \(k\))$' -or $control.cls -match '(^| )(np-play|bp-play-btn|vjs-big-play-button|vp-play)( |$)') -and $control.label -notmatch 'Duraklat|Pause'){
                $x=$control.rect.x+$control.rect.w/2;$y=$control.rect.y+$control.rect.h/2
                if($cx.v.url -ne $Url -and $cx.v.url -notlike 'about:*'){
                    $parentFrames=@($values | ForEach-Object {$_.v.frames} | Where-Object { $_.src -and $cx.v.url.StartsWith($_.src.Split('?')[0]) -and $_.rect.w -gt 0 })
                    if($parentFrames.Count -eq 1){$x+=$parentFrames[0].rect.x;$y+=$parentFrames[0].rect.y}
                    elseif(([uri]$cx.v.url).Host -ne ([uri]$Url).Host){continue}
                }
                if($x -ge 0 -and $x -lt 1280 -and ($y -ge 720 -or $y -lt 0)){
                    D $Id 'Input.dispatchMouseEvent' @{type='mouseWheel';x=640;y=360;deltaX=0;deltaY=($y-300)} | Out-Null
                    $actions+=@{type='scrollToControl';label=$control.label;deltaY=($y-300)};$clicked=$true;break
                }
                if($x -ge 0 -and $x -lt 1280 -and $y -ge 0 -and $y -lt 720){
                    Click $Id $x $y | Out-Null
                    $actions+=@{session=$cx.session;url=$cx.v.url;label=$control.label;class=$control.cls;x=$x;y=$y;at=[DateTimeOffset]::UtcNow.ToString('o')}
                    $clicked=$true;break
                }
            }
        }
        if($clicked){break}
    }
    if(-not $clicked -and $attempt -ge 4){break}
    Start-Sleep -Seconds 7
}
Save "$dir/$Site-actions.json" $actions
$before=@(Media $Id);Save "$dir/$Site-before.json" $before
Start-Sleep -Seconds 6
$after=@(Media $Id);Save "$dir/$Site-after.json" $after
$capture=C @{op='stream';id=$Id;name="$Site-proof";seconds=2;width=1280;height=720;quality=80}
Save "$dir/$Site-memory.json" (Memory $Id)
foreach($cx in $after){$v=$cx.result.result.value;if($v){[pscustomobject]@{site=$Site;url=$v.url;signals=$v.signals;media=$v.media;actions=$actions.Count;streamFrames=$capture.frames} | ConvertTo-Json -Depth 6 -Compress}}
