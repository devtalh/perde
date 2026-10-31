param([string]$Id,[string]$Name='youtube-60s',[int]$Seconds=60)
. $PSScriptRoot/common.ps1
$rows=@()
for($elapsed=0;$elapsed -le $Seconds;$elapsed+=5){
    $rows+=@{at=[DateTimeOffset]::UtcNow.ToString('o');observation=@(Media $Id)}
    if($elapsed -lt $Seconds){Start-Sleep -Seconds 5}
}
Save "run/chrome-spike/$Id/$Name.json" $rows
$first=@($rows[0].observation | ForEach-Object {$_.result.result.value.media} | Where-Object {$_.width -ge 1280 -and $_.height -eq 720} | Select-Object -First 1)
$last=@($rows[-1].observation | ForEach-Object {$_.result.result.value.media} | Where-Object {$_.width -ge 1280 -and $_.height -eq 720} | Select-Object -First 1)
$passed=$first.Count -gt 0 -and $last.Count -gt 0 -and $last[0].ready -ge 3 -and ($last[0].time-$first[0].time) -ge ($Seconds-5)
Save "run/chrome-spike/$Id/$Name-summary.json" @{seconds=$Seconds;first=$first;last=$last;passed=$passed}
[pscustomobject]@{id=$Id;soak=$Name;passed=$passed;firstTime=$first[0].time;lastTime=$last[0].time} | ConvertTo-Json -Compress
