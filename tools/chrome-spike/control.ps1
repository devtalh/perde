param([string]$Id,[switch]$Popup)
. $PSScriptRoot/common.ps1
$dir="run/chrome-spike/$Id"
$results=@{}
$results.navigation=D $Id 'Page.navigate' @{url='https://example.org/'}
Start-Sleep -Seconds 2
$tree=D $Id 'Page.getFrameTree'
$html=@'
<!doctype html><html><body style="margin:0;height:3000px">
<input id="typed" style="position:absolute;left:20px;top:20px;width:200px;height:30px">
<button id="click" style="position:absolute;left:20px;top:80px;width:200px;height:40px" onclick="this.dataset.clicks=String(Number(this.dataset.clicks||0)+1);perdeSpike(JSON.stringify({kind:'click',clicks:this.dataset.clicks}))">Test click</button>
<iframe src="https://example.com/" style="position:absolute;left:300px;top:0;width:500px;height:300px"></iframe>
</body></html>
'@
D $Id 'Page.setDocumentContent' @{frameId=$tree.frameTree.frame.id;html=$html} | Out-Null
Start-Sleep -Seconds 3
Click $Id 90 38 | Out-Null
$results.type=D $Id 'Input.insertText' @{text='Perde CDP 25'}
Click $Id 95 98 | Out-Null
$results.inputReadback=E $Id "({typed:document.querySelector('#typed').value,clicks:document.querySelector('#click').dataset.clicks})"
$results.scroll=D $Id 'Input.dispatchMouseEvent' @{type='mouseWheel';x=900;y=500;deltaX=0;deltaY=400}
Start-Sleep -Milliseconds 500
$results.scrollReadback=E $Id 'scrollY'
foreach($name in @('geolocation','microphone','camera','notifications')) { $results["deny-$name"]=D $Id 'Browser.setPermission' @{permission=@{name=$name};setting='denied';origin='https://example.org'} -Browser }
$results.permissionReadback=E $Id "Promise.all(['geolocation','microphone','camera','notifications'].map(async name=>({name,state:(await navigator.permissions.query({name})).state})))"
$results.frames=@()
foreach($cx in @(C @{op='contexts';id=$Id})) { try { $results.frames+=C @{op='eval';id=$Id;session=$cx.session;contextId=$cx.context.id;expression="(()=>{perdeSpike(JSON.stringify({kind:'frame',origin:location.origin}));return {origin:location.origin,binding:typeof perdeSpike}})()"} } catch { $results.frames+=@{error=$_.Exception.Message} } }
$results.fetchEnable=D $Id 'Fetch.enable' @{patterns=@(@{urlPattern='*://pagead2.googlesyndication.com/*';requestStage='Request'})}
$results.fetchProbe=E $Id "fetch('https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js').then(()=>({unexpected:true})).catch(e=>({blocked:true,error:e.message}))"
$results.fetchDisable=D $Id 'Fetch.disable'
if($Popup){$results.popup=E $Id "(()=>{window.open('about:blank','spike-popup');return 'requested'})()";Start-Sleep -Seconds 1;$results.targets=D $Id 'Target.getTargets' @{} -Browser}
Save "$dir/control.json" $results
$results | ConvertTo-Json -Depth 7
