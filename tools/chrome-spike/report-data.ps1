param([string]$E1Headless='final-e1-h-memory')
$ErrorActionPreference='Stop'
$destination='docs/spike-chrome/tables'
New-Item -ItemType Directory -Force $destination | Out-Null
function F($n){([double]$n).ToString('F2',[Globalization.CultureInfo]::InvariantCulture)}
function Stats($numbers){$values=@($numbers | ForEach-Object {[double]$_});$m=$values | Measure-Object -Average -Maximum;@{mean=$m.Average;max=$m.Maximum}}
function Q($numbers,[double]$p){$values=@($numbers | Sort-Object);if(-not $values.Count){return 0};$values[[int][Math]::Floor(($values.Count-1)*$p)]}
$memory=@();$roles=@();$rows=@('| Engine / mode | Point (10 samples) | Chrome WS mean / max MiB | Chrome private mean / max MiB | Java WS mean / max MiB | Java private mean / max MiB | Chrome + Java WS mean / max MiB |')
$rows+='| --- | --- | ---: | ---: | ---: | ---: | ---: |'
foreach($case in @(@{id=$E1Headless;label='E1 headless'},@{id='mem-e1-o';label='E1 off-screen'},@{id='mem-e2-h';label='E2 headless'},@{id='mem-e2-o';label='E2 off-screen'})){
    foreach($point in @('blank','youtube720-60s','dizi-failed-no-extension','dizi-without-extension','dizi-with-extension','returned-blank')){
        $filePoint=if($case.id -eq 'mem-e1-o' -and $point -eq 'dizi-with-extension'){'dizi-with-extension-compare'}else{$point}
        $path="run/chrome-spike/$($case.id)/memory-$filePoint-samples.json"
        $samples=@(Get-Content $path -Raw | ConvertFrom-Json)
        if($samples.Count -ne 10){throw "$path is not ten samples"}
        $record=[ordered]@{id=$case.id;engineMode=$case.label;point=$point;count=$samples.Count;processTypes=@($samples.chromeProcesses.type | Sort-Object -Unique)}
        foreach($field in @('chromeWorkingSetBytes','chromePrivateBytes','javaWorkingSetBytes','javaPrivateBytes','bridgeWorkingSetBytes','bridgePrivateBytes')){$record[$field]=Stats @($samples | ForEach-Object {$_.$field/1MB})}
        $record['combinedWorkingSet']=Stats @($samples | ForEach-Object {($_.chromeWorkingSetBytes+$_.javaWorkingSetBytes)/1MB})
        $record['combinedPrivate']=Stats @($samples | ForEach-Object {($_.chromePrivateBytes+$_.javaPrivateBytes)/1MB})
        $record['chromeProcessCount']=Stats @($samples | ForEach-Object {@($_.chromeProcesses).Count})
        $record['playing720Samples']=@($samples | Where-Object {@($_.media | ForEach-Object {$_.media} | Where-Object {$_.height -eq 720 -and $_.ready -ge 3 -and -not $_.paused}).Count -gt 0}).Count
        if($point -eq 'youtube720-60s'){
            $soak=Get-Content "run/chrome-spike/$($case.id)/youtube-60s-summary.json" -Raw | ConvertFrom-Json
            $record['soakPassed']=$soak.passed
            if(-not $soak.passed -or $record.playing720Samples -ne 10){throw "Unverified 720p memory point: $($case.id)"}
        }
        $memory+=,$record
        $label=switch($point){blank{'a: initial blank'};'youtube720-60s'{'b: 720p after 60 s'};'dizi-failed-no-extension'{'c: failed episode page, not playing'};'dizi-without-extension'{'d: same dizi URL, no uBOL'};'dizi-with-extension'{'d: same dizi URL, uBOL'};'returned-blank'{'e: returned to blank'}}
        $cells=@($case.label,$label)
        foreach($key in @('chromeWorkingSetBytes','chromePrivateBytes','javaWorkingSetBytes','javaPrivateBytes','combinedWorkingSet')){$cells+=(F $record[$key].mean)+' / '+(F $record[$key].max)}
        $rows+='| '+($cells -join ' | ')+' |'
        foreach($type in $record.processTypes){
            $parts=@($samples | ForEach-Object {$p=@($_.chromeProcesses | Where-Object type -eq $type);@{ws=($p|Measure-Object workingSetBytes -Sum).Sum/1MB;private=($p|Measure-Object privateBytes -Sum).Sum/1MB;count=$p.Count}})
            $w=Stats $parts.ws;$p=Stats $parts.private;$n=Stats $parts.count
            $roles+=[pscustomobject]@{engineMode=$case.label;point=$point;type=$type;meanCount=$n.mean;maxCount=$n.max;meanWorkingSetMiB=$w.mean;maxWorkingSetMiB=$w.max;meanPrivateMiB=$p.mean;maxPrivateMiB=$p.max}
        }
    }
}
foreach($old in @(@{file='docs/phase2/f2-3-memory.csv';stage='f2-3-full-a-before';label='MCEF, F2 full-profile YouTube'},@{file='docs/phase2/f2-1-memory.csv';stage='audit-video-live';label='MCEF, F2-1 audited YouTube'},@{file='docs/phase2/f2-1-memory.csv';stage='final-A-live';label='MCEF, F2-1 YouTube/ad'})){
    $samples=@(Import-Csv $old.file | Where-Object Stage -eq $old.stage);$stats=Stats @($samples | ForEach-Object {[double]$_.HelperBytes/1MB})
    $rows+='| '+$old.label+' | Historical, 10 samples | '+(F $stats.mean)+' / '+(F $stats.max)+' | n/a | Full Minecraft JVM, not comparable | n/a | n/a |'
}
$rows | Set-Content "$destination/memory.md" -Encoding UTF8
$memory | ConvertTo-Json -Depth 12 | Set-Content "$destination/memory.json" -Encoding UTF8
$roles | Export-Csv "$destination/process-roles.csv" -NoTypeInformation -Encoding UTF8

$streams=@();$streamRows=@('| Engine / case | Transport / video | Frames | Received fps median / p10 (1 s bins) | JPEG bytes median / p90 | Capture-to-receive ms median / p90 | ImageIO / STB median ms | Chrome / Java CPU (one core %) |')
$streamRows+='| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |'
$cases=@(@{id='e1-h';name='sintel-720-q80';label='E1 headless';transport='WS, Sintel 720 Q80'},@{id='e1-h';name='sintel-480-q55';label='E1 headless';transport='WS, Sintel 480 Q55'},@{id='e1-ob';name='sintel-720-q80';label='E1 off-screen';transport='WS, Sintel 720 Q80'},@{id='e2-ob';name='sintel-720-q80';label='E2 off-screen';transport='WS, Sintel 720 Q80'},@{id='mem-e1-o';name='youtube720-memory-stream';label='E1 off-screen, initial pipe';transport='Unbuffered pipe, YouTube 720 Q80'},@{id='mem-e2-h';name='youtube720-memory-stream';label='E2 headless, initial pipe';transport='Unbuffered pipe, YouTube 720 Q80'},@{id='mem-e2-h';name='youtube720-low-q55';label='E2 headless, initial pipe';transport='Unbuffered pipe, YouTube 480 Q55'},@{id=$E1Headless;name='youtube720-memory-stream';label='E1 headless, final';transport='Buffered pipe, YouTube 720 Q80'},@{id=$E1Headless;name='youtube720-low-q55';label='E1 headless, final';transport='Buffered pipe, YouTube 480 Q55'},@{id='final-e2-ws';name='sintel-720-q80';label='E2 headless, final';transport='WS, Sintel 720 Q80'},@{id='final-e2-ws';name='youtube720-q80';label='E2 headless, final';transport='WS, YouTube 720 Q80'},@{id='final-e2-ws';name='youtube480-q55';label='E2 headless, final';transport='WS, YouTube 480 Q55'})
foreach($case in $cases){
    $path="run/chrome-spike/$($case.id)/$($case.name)-summary.json";if(-not (Test-Path $path)){continue}
    $summary=Get-Content $path -Raw | ConvertFrom-Json
    $frames=@(Import-Csv "run/chrome-spike/$($case.id)/$($case.name)-frames.csv")
    $binCount=[Math]::Max(1,[Math]::Floor($summary.seconds-0.3));$bins=New-Object int[] $binCount
    if($frames.Count){$origin=[double]::Parse($frames[0].receiveMonotonicSec,[Globalization.CultureInfo]::InvariantCulture);foreach($frame in $frames){$stamp=[double]::Parse($frame.receiveMonotonicSec,[Globalization.CultureInfo]::InvariantCulture);$bin=[int][Math]::Floor($stamp-$origin);if($bin -ge 0 -and $bin -lt $binCount){$bins[$bin]++}}}
    $chromeCpu=($summary.after.chromeCpuMs-$summary.before.chromeCpuMs)/($summary.seconds*10);$javaCpu=($summary.after.javaCpuMs-$summary.before.javaCpuMs)/($summary.seconds*10)
    $result=@{id=$case.id;name=$case.name;label=$case.label;transport=$case.transport;summary=$summary;fpsBins=$bins;medianFps=(Q $bins 0.5);p10Fps=(Q $bins 0.1);chromeCorePercent=$chromeCpu;javaCorePercent=$javaCpu}
    $streams+=,$result
    $cells=@($case.label,$case.transport,$summary.frames,((F $result.medianFps)+' / '+(F $result.p10Fps)),((F $summary.bytes.median)+' / '+(F $summary.bytes.p90)),((F $summary.latencyMs.median)+' / '+(F $summary.latencyMs.p90)),((F $summary.imageIoMs.median)+' / '+(F $summary.stbMs.median)),((F $chromeCpu)+' / '+(F $javaCpu)))
    $streamRows+='| '+($cells -join ' | ')+' |'
}
$streams | ConvertTo-Json -Depth 30 | Set-Content "$destination/streams.json" -Encoding UTF8
$streamRows | Set-Content "$destination/streams.md" -Encoding UTF8
Write-Output "Validated $($memory.Count) ten-sample memory points and summarized $($streams.Count) streams."
