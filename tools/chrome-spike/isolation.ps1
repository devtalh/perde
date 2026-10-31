param([string]$Prefix,[string]$Executable)
. $PSScriptRoot/common.ps1
$a="$Prefix-a";$b="$Prefix-b";$restart="$Prefix-restart"
$results=@{}
function OpenFixture($id){D $id 'Page.navigate' @{url='https://example.org/'} | Out-Null;for($n=0;$n -lt 30;$n++){if((E $id 'location.origin').result.value -eq 'https://example.org'){return};Start-Sleep -Milliseconds 200};throw 'Fixture origin not ready'}
try{
    $results.a=C @{op='start';id=$a;executable=$Executable;mode='headless'}
    $results.b=C @{op='start';id=$b;executable=$Executable;mode='headless'}
    if($results.a.profile -eq $results.b.profile){throw 'Profiles are not independent'}
    OpenFixture $a;OpenFixture $b
    E $a "localStorage.setItem('perdeSpikeIsolation','A')" | Out-Null
    $results.bInitially=(E $b "localStorage.getItem('perdeSpikeIsolation')").result.value
    if($null -ne $results.bInitially){throw 'Storage leaked between profiles'}
    E $b "localStorage.setItem('perdeSpikeIsolation','B')" | Out-Null
    $results.aValue=(E $a "localStorage.getItem('perdeSpikeIsolation')").result.value
    $results.bValue=(E $b "localStorage.getItem('perdeSpikeIsolation')").result.value
    if($results.aValue -ne 'A' -or $results.bValue -ne 'B'){throw 'Storage isolation failed'}
    $results.crash=C @{op='crash';id=$a}
    $results.bAfterCrash=(E $b "localStorage.getItem('perdeSpikeIsolation')").result.value
    if($results.bAfterCrash -ne 'B'){throw 'Other instance was affected by the crash'}
    $results.cleanupA=C @{op='stop';id=$a}
    $results.restart=C @{op='start';id=$restart;executable=$Executable;mode='headless'}
    OpenFixture $restart
    $results.restartStorage=(E $restart "localStorage.getItem('perdeSpikeIsolation')").result.value
    if($null -ne $results.restartStorage){throw 'Fresh restart reused old profile storage'}
    $results.passed=$true
}finally{
    foreach($id in @($a,$b,$restart)){if($id -eq $a -and $results.ContainsKey('cleanupA')){continue};try{$results["cleanup-$id"]=C @{op='stop';id=$id}}catch{$results["cleanup-$id"]=$_.Exception.Message}}
    Save "run/chrome-spike/isolation-$Prefix.json" $results
}
$results | Select-Object passed,aValue,bValue,bAfterCrash,restartStorage | ConvertTo-Json
