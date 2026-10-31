param([string]$Id,[string]$Label='ad-comparison')
. $PSScriptRoot/common.ps1
$log="run/chrome-spike/$Id/events.jsonl"
$start=[DateTimeOffset]::UtcNow
D $Id 'Page.navigate' @{url='https://dizipalorijinal20.com/bolum/mobland-2-sezon-7-bolum'} | Out-Null
Start-Sleep -Seconds 8
D $Id 'Input.dispatchMouseEvent' @{type='mouseWheel';x=1100;y=500;deltaX=0;deltaY=904} | Out-Null
Start-Sleep -Seconds 2
Save "run/chrome-spike/$Id/$Label-observation.json" @(Media $Id)
C @{op='stream';id=$Id;name=$Label;seconds=2;width=1280;height=720;quality=80} | Out-Null
$end=[DateTimeOffset]::UtcNow
$events=@(Get-Content -LiteralPath $log | ForEach-Object { $_ | ConvertFrom-Json } | Where-Object { [DateTimeOffset]$_.at -ge $start -and [DateTimeOffset]$_.at -le $end })
$blocked=@($events | Where-Object { $_.kind -eq 'Network.loadingFailed' -and $_.data.params.errorText -eq 'net::ERR_BLOCKED_BY_CLIENT' })
$result=@{start=$start.ToString('o');end=$end.ToString('o');blockedRequestCount=$blocked.Count;blocked=$blocked;note='No Fetch interception enabled. Separate fresh navigations, not a deterministic ad auction.'}
Save "run/chrome-spike/$Id/$Label.json" $result
$result | Select-Object start,end,blockedRequestCount | ConvertTo-Json
