param([string]$Id,[string]$Point,[int]$Count=10)
. $PSScriptRoot/common.ps1
$rows=@()
for($sample=0;$sample -lt $Count;$sample++){
    $snapshot=C @{op='snapshot';id=$Id}
    $ownedPids=@($snapshot.processes | ForEach-Object {[int]$_.pid})
    $processRows=@()
    foreach($ownedId in $ownedPids){
        $p=Get-Process -Id $ownedId -ErrorAction SilentlyContinue
        if(-not $p){continue}
        $meta=Get-CimInstance Win32_Process -Filter "ProcessId=$ownedId" -ErrorAction SilentlyContinue
        $command=$meta.CommandLine
        $type=if($command -match '--type=gpu-process'){'GPU'}elseif($command -match '--utility-sub-type=network\.mojom\.NetworkService'){'network'}elseif($command -match '--type=renderer' -and $command -match '--extension-process'){'extension renderer'}elseif($command -match '--type=renderer'){'renderer'}elseif($command -match 'crashpad-handler'){'crashpad utility'}elseif($command -match '--type=utility'){'utility'}elseif($command -match '--type='){'other helper'}else{'browser'}
        $processRows+=@{pid=$ownedId;type=$type;workingSetBytes=$p.WorkingSet64;privateBytes=$p.PrivateMemorySize64}
    }
    $java=Get-Process -Id ([int]$snapshot.javaPid)
    $media=@(Media $Id | ForEach-Object {$_.result.result.value} | Where-Object {$_} | ForEach-Object {@{url=$_.url;media=$_.media}})
    $rows+=@{sample=$sample;at=[DateTimeOffset]::UtcNow.ToString('o');chromeWorkingSetBytes=($processRows | Measure-Object workingSetBytes -Sum).Sum;chromePrivateBytes=($processRows | Measure-Object privateBytes -Sum).Sum;chromeProcesses=$processRows;javaWorkingSetBytes=$java.WorkingSet64;javaPrivateBytes=$java.PrivateMemorySize64;javaPid=$java.Id;media=$media}
    Start-Sleep -Milliseconds 1000
}
$summary=@{id=$Id;point=$Point;count=$rows.Count;processTypes=@($rows.chromeProcesses.type | Sort-Object -Unique)}
foreach($field in @('chromeWorkingSetBytes','chromePrivateBytes','javaWorkingSetBytes','javaPrivateBytes')){
    $stats=$rows | Measure-Object $field -Average -Maximum
    $summary[$field]=@{meanMiB=$stats.Average/1MB;maxMiB=$stats.Maximum/1MB}
}
Save "run/chrome-spike/$Id/memory-$Point-samples.json" $rows
Save "run/chrome-spike/$Id/memory-$Point-summary.json" $summary
$summary | ConvertTo-Json -Depth 5 -Compress
