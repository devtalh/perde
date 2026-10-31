param([string[]]$Ids)
$ErrorActionPreference='Stop'
$repo=(Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$target=Join-Path $repo 'docs/spike-chrome/evidence'
New-Item -ItemType Directory -Force $target | Out-Null
$provenance=@()
function Redact($v){
    if($null -eq $v){return $null}
    if($v -is [string]){
        $s=$v.Replace($repo,'<repo>').Replace($repo.Replace('\','/'),'<repo>')
        # Public page URLs are listed in the report; expiring CDN/ad query strings are unnecessary evidence.
        return [regex]::Replace($s,'(https?://[^?\s"''<>]+)\?[^\s"''<>]*','$1?redacted')
    }
    if($v -is [System.Management.Automation.PSCustomObject]){$o=[ordered]@{};foreach($p in $v.PSObject.Properties){$o[$p.Name]=Redact $p.Value};return $o}
    if($v -is [System.Collections.IDictionary]){$o=[ordered]@{};foreach($key in $v.Keys){$o[$key]=Redact $v[$key]};return $o}
    if($v -is [System.Collections.IEnumerable] -and $v -isnot [string]){return ,@($v | ForEach-Object {Redact $_})}
    return $v
}
function Record($source,$destination){
    if([IO.Path]::GetExtension($destination) -ne '.png'){
        $text=[IO.File]::ReadAllText($destination).Replace("`r`n","`n")
        [IO.File]::WriteAllText($destination,$text,[Text.UTF8Encoding]::new($false))
    }
    $script:provenance+=@{source=[IO.Path]::GetRelativePath($repo,$source);published=[IO.Path]::GetRelativePath($repo,$destination);originalSha256=(Get-FileHash -LiteralPath $source -Algorithm SHA256).Hash;publishedSha256=(Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash}
}
foreach($id in $Ids){
    $dir=Join-Path $repo "run/chrome-spike/$id";$out=Join-Path $target $id
    New-Item -ItemType Directory -Force $out | Out-Null
    foreach($file in Get-ChildItem -LiteralPath $dir -File){
        if($file.Name -in @('launch.json','protocol.json','bridge.log') -or $file.Name -like 'pipe-*.json'){continue}
        $destination=Join-Path $out $file.Name
        if($file.Extension -eq '.json'){
            $value=Get-Content -LiteralPath $file.FullName -Raw | ConvertFrom-Json
            Redact $value | ConvertTo-Json -Depth 100 | Set-Content -LiteralPath $destination -Encoding UTF8
        }elseif($file.Extension -in @('.csv','.txt')){Copy-Item -LiteralPath $file.FullName -Destination $destination}
        elseif($file.Extension -eq '.png'){
            # One selected streamed frame per proof and two per size/quality comparison; no desktop captures.
            if($file.Name -match '(sintel|youtube720|youtube-720|youtube-480|ad-).*frame30\.png$' -or $file.Name -match 'ad-.*frame1\.png$' -or $file.Name -match '(twitch|kick|vimeo|dailymotion|dizilla|dizipal).*(frame1)\.png$'){
                Copy-Item -LiteralPath $file.FullName -Destination $destination
            }else{continue}
        }else{continue}
        Record $file.FullName $destination
    }
    $log=Join-Path $dir 'events.jsonl'
    if(Test-Path -LiteralPath $log){
        $published=Join-Path $out 'diagnostics.jsonl'
        $writer=[IO.StreamWriter]::new($published,$false,[Text.UTF8Encoding]::new($false))
        try{foreach($line in [IO.File]::ReadLines($log)){
            if(-not $line){continue};$event=$line | ConvertFrom-Json
            if($event.kind -match '^(cleanup|startup|stream|commandFailure|popupClosed|fetchBlocked|Runtime.bindingCalled|Network.loadingFailed|Media.playerErrorsRaised|Page.frameNavigated|Page.frameRequestedNavigation)$'){
                $writer.WriteLine(((Redact $event) | ConvertTo-Json -Depth 100 -Compress))
            }elseif($event.kind -eq 'Log.entryAdded' -and $event.data.params.entry.level -eq 'error'){
                $writer.WriteLine(((Redact $event) | ConvertTo-Json -Depth 100 -Compress))
            }elseif($event.kind -eq 'Media.playerPropertiesChanged'){
                $event.data.params.properties=@($event.data.params.properties | Where-Object name -match 'k(VideoTracks|AudioTracks|VideoDecoderName|AudioDecoderName|FrameUrl|FrameTitle|Resolution|IsPlatformVideoDecoder|IsPlatformAudioDecoder)$')
                if($event.data.params.properties.Count){$writer.WriteLine(((Redact $event) | ConvertTo-Json -Depth 100 -Compress))}
            }
        }}finally{$writer.Dispose()}
        Record $log $published
    }
}
foreach($name in @('system.json','baseline-chrome.json','baseline-survival.json','twitch-listing.json','kick-listing2.json','isolation-isolate-e1.json','isolation-isolate-e2.json','final-cleanup.json','downloads.json','verification.json')){
    $source=Join-Path $repo "run/chrome-spike/$name";if(Test-Path $source){$destination=Join-Path $target $name;Redact (Get-Content $source -Raw | ConvertFrom-Json) | ConvertTo-Json -Depth 100 | Set-Content $destination -Encoding UTF8;Record $source $destination}
}
$provenance | ConvertTo-Json -Depth 5 | Set-Content (Join-Path $target 'provenance.json') -Encoding UTF8
[IO.File]::WriteAllText((Join-Path $target 'provenance.json'),[IO.File]::ReadAllText((Join-Path $target 'provenance.json')).Replace("`r`n","`n"),[Text.UTF8Encoding]::new($false))
Write-Output "Exported $($provenance.Count) evidence files. Query strings and repository paths redacted in JSON. PNG/CSV copies retain source hashes."
