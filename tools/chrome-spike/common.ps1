$ErrorActionPreference = 'Stop'
$controller = Get-Content run/chrome-spike/controller.json -Raw | ConvertFrom-Json
$script:Endpoint = "http://127.0.0.1:$($controller.port)/cmd"
$script:Headers = @{'X-Perde-Spike-Token'=$controller.token}
function C($q) { (Invoke-RestMethod -Uri $script:Endpoint -Headers $script:Headers -Method Post -ContentType 'application/json; charset=utf-8' -Body ([Text.Encoding]::UTF8.GetBytes(($q | ConvertTo-Json -Depth 50 -Compress))) -TimeoutSec 90).result }
function E([string]$id,[string]$js) { C @{op='eval';id=$id;expression=$js} }
function D([string]$id,[string]$method,$p=@{},[switch]$Browser) { $q=@{op='cdp';id=$id;method=$method;params=$p};if($Browser){$q.session=''}; C $q }
function Save($path,$value) { $value | ConvertTo-Json -Depth 60 | Set-Content -LiteralPath $path -Encoding UTF8 }
function Memory($id) { $snapshot=C @{op='snapshot';id=$id};$pids=@($snapshot.processes | ForEach-Object {[int]$_.pid});$ownedProcesses=Get-Process -Id $pids -ErrorAction SilentlyContinue;[pscustomobject]@{at=[DateTimeOffset]::UtcNow.ToString('o');count=$ownedProcesses.Count;workingSetMiB=($ownedProcesses | Measure-Object WorkingSet64 -Sum).Sum/1MB;privateMiB=($ownedProcesses | Measure-Object PrivateMemorySize64 -Sum).Sum/1MB;snapshot=$snapshot} }
function Media($id) {
    $js = Get-Content tools/chrome-spike/observe.js -Raw
    $contexts=C @{op='contexts';id=$id}
    $tree=D $id 'Page.getFrameTree'
    $frameIds=@{}
    function CollectFrames($node) { $frameIds[$node.frame.id]=$true;foreach($child in $node.childFrames){CollectFrames $child} }
    CollectFrames $tree.frameTree
    foreach($cx in $contexts){if(-not $frameIds.ContainsKey([string]$cx.context.auxData.frameId)){continue};try{$r=C @{op='eval';id=$id;session=$cx.session;contextId=$cx.context.id;expression=$js};[pscustomobject]@{session=$cx.session;contextId=$cx.context.id;result=$r}}catch{[pscustomobject]@{session=$cx.session;error=$_.Exception.Message}}}
}
function Click($id,[double]$x,[double]$y) { D $id 'Input.dispatchMouseEvent' @{type='mousePressed';x=$x;y=$y;button='left';clickCount=1} | Out-Null;D $id 'Input.dispatchMouseEvent' @{type='mouseReleased';x=$x;y=$y;button='left';clickCount=1} }
