param([string]$Id,[string]$Point,[int]$Count=10)
. $PSScriptRoot/common.ps1
$rows=@()
$startup=Get-Content "run/chrome-spike/$Id/startup.json" -Raw | ConvertFrom-Json
$rootMeta=Get-CimInstance Win32_Process -Filter "ProcessId=$([int]$startup.pid)"
$bridgeMeta=Get-CimInstance Win32_Process -Filter "ProcessId=$($rootMeta.ParentProcessId)" -ErrorAction SilentlyContinue
for($sample=0;$sample -lt $Count;$sample++){
    $snapshot=C @{op='snapshot';id=$Id}
    $ownedPids=@($snapshot.processes | ForEach-Object {[int]$_.pid})
    $metadata=@{}
    Get-CimInstance Win32_Process -Filter (($ownedPids | ForEach-Object {"ProcessId=$_"}) -join ' OR ') | ForEach-Object {$metadata[[int]$_.ProcessId]=$_}
    $processes=@{}
    Get-Process -Id $ownedPids -ErrorAction SilentlyContinue | ForEach-Object {$processes[$_.Id]=$_}
    $processRows=@()
    foreach($ownedId in $ownedPids){
        $p=$processes[$ownedId]
        if(-not $p){continue}
        $meta=$metadata[$ownedId]
        $command=$meta.CommandLine
        $type=if($command -match '--type=gpu-process'){'GPU'}elseif($command -match '--utility-sub-type=network\.mojom\.NetworkService'){'network'}elseif($command -match '--type=renderer' -and $command -match '--extension-process'){'extension renderer'}elseif($command -match '--type=renderer'){'renderer'}elseif($command -match 'crashpad-handler'){'crashpad utility'}elseif($command -match '--type=utility'){'utility'}elseif($command -match '--type='){'other helper'}else{'browser'}
        $processRows+=@{pid=$ownedId;type=$type;workingSetBytes=$p.WorkingSet64;privateBytes=$p.PrivateMemorySize64}
    }
    $java=Get-Process -Id ([int]$snapshot.javaPid)
    $bridge=$null
    if($bridgeMeta.CommandLine -match 'chrome-spike[\\/]launch.ps1'){ $bridge=Get-Process -Id $bridgeMeta.ProcessId -ErrorAction SilentlyContinue }
    $media=@(Media $Id | ForEach-Object {$_.result.result.value} | Where-Object {$_} | ForEach-Object {@{url=$_.url;media=$_.media}})
    $chromeWs=($processRows | Measure-Object workingSetBytes -Sum).Sum;$chromePrivate=($processRows | Measure-Object privateBytes -Sum).Sum
    $rows+=@{sample=$sample;at=[DateTimeOffset]::UtcNow.ToString('o');chromeWorkingSetBytes=$chromeWs;chromePrivateBytes=$chromePrivate;chromeProcesses=$processRows;javaWorkingSetBytes=$java.WorkingSet64;javaPrivateBytes=$java.PrivateMemorySize64;javaPid=$java.Id;bridgeWorkingSetBytes=if($bridge){$bridge.WorkingSet64}else{0};bridgePrivateBytes=if($bridge){$bridge.PrivateMemorySize64}else{0};chromePlusJavaWorkingSetBytes=$chromeWs+$java.WorkingSet64;chromePlusJavaPrivateBytes=$chromePrivate+$java.PrivateMemorySize64;media=$media}
    Start-Sleep -Milliseconds 1000
}
$summary=@{id=$Id;point=$Point;count=$rows.Count;processTypes=@($rows.chromeProcesses.type | Sort-Object -Unique)}
foreach($field in @('chromeWorkingSetBytes','chromePrivateBytes','javaWorkingSetBytes','javaPrivateBytes','chromePlusJavaWorkingSetBytes','chromePlusJavaPrivateBytes','bridgeWorkingSetBytes','bridgePrivateBytes')){
    $stats=$rows | Measure-Object $field -Average -Maximum
    $summary[$field]=@{meanMiB=$stats.Average/1MB;maxMiB=$stats.Maximum/1MB}
}
Save "run/chrome-spike/$Id/memory-$Point-samples.json" $rows
Save "run/chrome-spike/$Id/memory-$Point-summary.json" $summary
$summary | ConvertTo-Json -Depth 5 -Compress
